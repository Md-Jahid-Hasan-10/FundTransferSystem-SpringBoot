package com.bank.fundtransfer.service;

import com.bank.fundtransfer.config.BankProperties;
import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.AccountRole;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.entity.RequestStatus;
import com.bank.fundtransfer.entity.Transaction;
import com.bank.fundtransfer.entity.TransactionRequest;
import com.bank.fundtransfer.entity.TransactionStatus;
import com.bank.fundtransfer.entity.TransactionType;
import com.bank.fundtransfer.exception.AccountFrozenException;
import com.bank.fundtransfer.exception.AccountNotFoundException;
import com.bank.fundtransfer.exception.BankException;
import com.bank.fundtransfer.exception.InsufficientFundsException;
import com.bank.fundtransfer.exception.InvalidAmountException;
import com.bank.fundtransfer.exception.InvalidOtpException;
import com.bank.fundtransfer.exception.InvalidTransferException;
import com.bank.fundtransfer.exception.OtpExpiredException;
import com.bank.fundtransfer.policy.TransactionLimitPolicy;
import com.bank.fundtransfer.policy.TransferFeePolicy;
import com.bank.fundtransfer.repository.AccountRepository;
import com.bank.fundtransfer.repository.TransactionRepository;
import com.bank.fundtransfer.repository.TransactionRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Main entry point for money operations.
 * Flow for withdrawals and transfers: request -> verifyOtp -> confirm (or cancel before confirm).
 *
 * Every public method runs inside ONE database transaction (@Transactional). If something unexpected
 * fails, the database rolls everything back, so a transfer can never be half done.
 */
@Service
@Transactional
public class FundTransferService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final TransactionRequestRepository requests;
    private final CodeGeneratorService codes;
    private final TransferFeePolicy standardFeePolicy;      // transfers to other customers
    private final TransferFeePolicy ownAccountFeePolicy;    // transfers between a customer's own accounts
    private final TransactionLimitPolicy limitPolicy;
    private final OtpService otpService;
    private final NotificationService notifications;
    private final BankProperties properties;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    public FundTransferService(AccountRepository accounts,
                               TransactionRepository transactions,
                               TransactionRequestRepository requests,
                               CodeGeneratorService codes,
                               @Qualifier("standardTransferFeePolicy") TransferFeePolicy standardFeePolicy,
                               @Qualifier("ownAccountTransferFeePolicy") TransferFeePolicy ownAccountFeePolicy,
                               TransactionLimitPolicy limitPolicy,
                               OtpService otpService,
                               NotificationService notifications,
                               BankProperties properties,
                               Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.requests = requests;
        this.codes = codes;
        this.standardFeePolicy = standardFeePolicy;
        this.ownAccountFeePolicy = ownAccountFeePolicy;
        this.limitPolicy = limitPolicy;
        this.otpService = otpService;
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
    }

    // ---------- deposit (no OTP needed) ----------
    public Transaction deposit(Customer customer, String accountNumber, BigDecimal amount) {
        BigDecimal value = normalizeAmount(amount);
        Account account = findOwnedAccount(customer, accountNumber);
        lock(account);
        requireActive(account);
        account.deposit(value);
        Transaction tx = transactions.save(new Transaction(codes.nextTransactionCode(), TransactionType.DEPOSIT,
                null, account, value, ZERO, TransactionStatus.SUCCESS, Instant.now(clock)));
        notifications.notifySuccess(tx);
        return tx;
    }

    // ---------- step 1: create a request and send the OTP ----------
    public TransactionRequest requestWithdrawal(Customer customer, String accountNumber, BigDecimal amount) {
        BigDecimal value = normalizeAmount(amount);
        Account source = findOwnedAccount(customer, accountNumber);
        requireActive(source);
        checkLimits(customer, TransactionType.WITHDRAWAL, source, null, value);
        requireCanSpend(source, value);
        return startRequest(customer, TransactionType.WITHDRAWAL, source, null, value, ZERO);
    }

    public TransactionRequest requestTransfer(Customer customer, String fromNumber, String toNumber,
                                              BigDecimal amount) {
        BigDecimal value = normalizeAmount(amount);
        Account source = findOwnedAccount(customer, fromNumber);
        Account destination = findAccount(toNumber);
        if (destination.getRole() != AccountRole.CUSTOMER) {
            throw new InvalidTransferException("You cannot transfer to this account.");
        }
        if (source.getAccountNumber().equals(destination.getAccountNumber())) {
            throw new InvalidTransferException("Sender and receiver accounts must be different.");
        }
        requireActive(source);
        requireActive(destination);
        checkLimits(customer, TransactionType.TRANSFER, source, destination, value);
        TransferFeePolicy policy = source.hasSameOwnerAs(destination) ? ownAccountFeePolicy : standardFeePolicy;
        BigDecimal fee = policy.calculateFee(value);
        requireCanSpend(source, value.add(fee));
        return startRequest(customer, TransactionType.TRANSFER, source, destination, value, fee);
    }

    // ---------- step 2: verify the OTP ----------
    // noRollbackFor: a wrong OTP throws an exception, but the attempt counter (and a FAILED status
    // after too many attempts) must still be saved. Without this the database would roll it back
    // and an attacker could try unlimited OTPs.
    @Transactional(noRollbackFor = BankException.class)
    public void verifyOtp(Customer customer, String requestCode, String submittedOtp) {
        TransactionRequest request = findOwnedRequestForUpdate(customer, requestCode);
        requireStatus(request, RequestStatus.PENDING_OTP);
        failIfExpired(request);
        try {
            otpService.verify(request, submittedOtp);
            request.setStatus(RequestStatus.PENDING_CONFIRMATION);
        } catch (OtpExpiredException e) {
            failRequest(request, e.getMessage());
            throw e;
        } catch (InvalidOtpException e) {
            if (e.getRemainingAttempts() == 0) {
                failRequest(request, e.getMessage());
            }
            throw e;
        }
    }

    // ---------- step 3: customer confirms (or cancels) ----------
    // noRollbackFor: a failed confirmation must still save the FAILED request and its record.
    // This is safe because execute() checks everything BEFORE it changes any balance.
    @Transactional(noRollbackFor = BankException.class)
    public Transaction confirm(Customer customer, String requestCode) {
        TransactionRequest request = findOwnedRequestForUpdate(customer, requestCode);
        requireStatus(request, RequestStatus.PENDING_CONFIRMATION);
        failIfExpired(request);
        try {
            Transaction tx = execute(request);
            request.setStatus(RequestStatus.SUCCESS);
            notifications.notifySuccess(tx);
            return tx;
        } catch (BankException e) {
            failRequest(request, e.getMessage());
            throw e;
        }
    }

    public void cancel(Customer customer, String requestCode) {
        TransactionRequest request = findOwnedRequestForUpdate(customer, requestCode);
        if (request.getStatus() != RequestStatus.PENDING_OTP
                && request.getStatus() != RequestStatus.PENDING_CONFIRMATION) {
            throw new BankException("This request can no longer be cancelled.");
        }
        request.setStatus(RequestStatus.CANCELLED);
        transactions.save(buildRecord(request, TransactionStatus.CANCELLED));
        notifications.notifyCancelled(request);
    }

    // ---------- reports ----------
    @Transactional(readOnly = true)
    public List<Transaction> getHistory(Customer customer, String accountNumber) {
        Account account = findOwnedAccount(customer, accountNumber);
        return transactions.findBySourceOrDestinationOrderByCreatedAtDesc(account, account);
    }

    @Transactional(readOnly = true)
    public Account getBankFeeAccount() {
        return accounts.findFirstByRole(AccountRole.BANK_FEE_COLLECTION)
                .orElseThrow(() -> new BankException("The bank fee account is missing."));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getFeeTransactions() {
        return transactions.findByTypeAndStatusAndFeeGreaterThanOrderByCreatedAtDesc(
                TransactionType.TRANSFER, TransactionStatus.SUCCESS, ZERO);
    }

    /** What this customer has already sent out today (withdrawals + transfers to other customers). */
    @Transactional(readOnly = true)
    public BigDecimal getTodaysOutgoingTotal(Customer customer) {
        ZonedDateTime start = LocalDate.now(clock).atStartOfDay(clock.getZone());
        List<BigDecimal> amounts = transactions.outgoingAmountsBetween(
                customer, start.toInstant(), start.plusDays(1).toInstant());
        BigDecimal total = ZERO;
        for (BigDecimal amount : amounts) {
            total = total.add(amount);
        }
        return total;
    }

    // ---------- private helpers ----------
    private TransactionRequest startRequest(Customer customer, TransactionType type, Account source,
                                            Account destination, BigDecimal amount, BigDecimal fee) {
        Instant now = Instant.now(clock);
        TransactionRequest request = requests.save(new TransactionRequest(codes.nextRequestCode(), customer,
                type, source, destination, amount, fee, now, now.plusSeconds(properties.requestExpirySeconds())));
        String otp = otpService.generateOtp(request);
        notifications.sendOtp(customer, request, otp);
        return request;
    }

    /**
     * Moves the money. Locks are taken first, then everything is checked, and only then are balances changed,
     * so a failed check can never leave a half-finished transfer.
     */
    private Transaction execute(TransactionRequest request) {
        Account source = request.getSource();
        Account destination = request.getDestination();
        boolean hasFee = request.getType() == TransactionType.TRANSFER && request.getFee().signum() > 0;
        Account feeAccount = hasFee ? getBankFeeAccount() : null;

        // Lock the customer first (so two requests from the same customer run one after the other and
        // the daily limit is exact), then the accounts in a fixed order (so two transfers can never deadlock).
        lockRow(request.getCustomer());
        lock(source, destination, feeAccount);

        requireActive(source);
        if (destination != null) {
            requireActive(destination);
        }
        checkLimits(request.getCustomer(), request.getType(), source, destination, request.getAmount());
        BigDecimal total = request.getTotalDeduction();
        requireCanSpend(source, total);

        // all checks passed: change the balances
        source.withdraw(total);
        if (request.getType() == TransactionType.TRANSFER) {
            destination.deposit(request.getAmount());
            if (hasFee) {
                feeAccount.deposit(request.getFee());
            }
        }
        return transactions.save(buildRecord(request, TransactionStatus.SUCCESS));
    }

    /**
     * Locks the rows in the database (SELECT ... FOR UPDATE) and re-reads their current values.
     * Always lock in the same order (lowest id first) to avoid deadlocks.
     */
    private void lock(Account... toLock) {
        List<Account> sorted = Arrays.stream(toLock)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Account::getId))
                .toList();
        for (Account account : sorted) {
            lockRow(account);
        }
    }

    /**
     * Saves pending changes, then locks the row and re-reads it from the database.
     * The flush matters: refresh() throws away unsaved changes, so they must be written first.
     */
    private void lockRow(Object entity) {
        entityManager.flush();
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
    }

    private void checkLimits(Customer customer, TransactionType type, Account source,
                             Account destination, BigDecimal amount) {
        if (type == TransactionType.TRANSFER && source.hasSameOwnerAs(destination)) {
            return; // money stays with the same customer, so limits do not apply
        }
        limitPolicy.check(amount, getTodaysOutgoingTotal(customer));
    }

    private void requireActive(Account account) {
        if (account.isFrozen()) {
            throw new AccountFrozenException("Account " + account.getAccountNumber()
                    + " is frozen and cannot be used for transactions.");
        }
    }

    private void requireCanSpend(Account account, BigDecimal total) {
        if (!account.hasSufficientFunds(total)) {
            String message = "Insufficient funds. This needs " + total.toPlainString()
                    + " (including any fee), but you can spend at most "
                    + account.getAvailableToSpend().toPlainString();
            if (account.getMinimumBalance().signum() > 0) {
                message += ", because a minimum balance of " + account.getMinimumBalance().toPlainString()
                        + " must remain";
            }
            throw new InsufficientFundsException(message + ".");
        }
    }

    private Transaction buildRecord(TransactionRequest request, TransactionStatus status) {
        return new Transaction(codes.nextTransactionCode(), request.getType(), request.getSource(),
                request.getDestination(), request.getAmount(), request.getFee(), status, Instant.now(clock));
    }

    private void failRequest(TransactionRequest request, String reason) {
        if (request.getStatus() == RequestStatus.FAILED || request.getStatus() == RequestStatus.SUCCESS
                || request.getStatus() == RequestStatus.CANCELLED) {
            return;
        }
        request.setStatus(RequestStatus.FAILED);
        transactions.save(buildRecord(request, TransactionStatus.FAILED));
        notifications.notifyFailure(request, reason);
    }

    private void failIfExpired(TransactionRequest request) {
        if (request.isExpired(Instant.now(clock))) {
            failRequest(request, "The request expired.");
            throw new BankException("This request has expired. Please start again.");
        }
    }

    private void requireStatus(TransactionRequest request, RequestStatus expected) {
        if (request.getStatus() != expected) {
            throw new BankException("This request is not waiting for that step (status: "
                    + request.getStatus() + ").");
        }
    }

    private Account findAccount(String accountNumber) {
        return accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " was not found."));
    }

    private Account findOwnedAccount(Customer customer, String accountNumber) {
        Account account = findAccount(accountNumber);
        if (!account.isOwnedBy(customer)) {
            throw new BankException("Account " + accountNumber + " does not belong to you.");
        }
        return account;
    }

    /**
     * Finds the request and locks its row, so two clicks on "confirm" (or two threads) cannot both
     * use the same request. The status is re-read after the lock, so the second one sees the new status.
     */
    private TransactionRequest findOwnedRequestForUpdate(Customer customer, String requestCode) {
        TransactionRequest request = requests.findByRequestCode(requestCode)
                .orElseThrow(() -> new BankException("Request not found."));
        if (!request.getCustomer().getCustomerCode().equals(customer.getCustomerCode())) {
            throw new BankException("Request not found.");
        }
        lockRow(request);
        return request;
    }
    @Transactional(readOnly = true)
    public TransactionRequest getRequest(Customer customer, String requestCode) {
        TransactionRequest request = requests.findByRequestCode(requestCode)
                .orElseThrow(() -> new BankException("Request not found."));
        if (!request.getCustomer().getCustomerCode().equals(customer.getCustomerCode())) {
            throw new BankException("Request not found.");
        }
        return request;
    }
    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount can have at most 2 decimal places.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}