package com.bank.fundtransfer;

import com.bank.fundtransfer.entity.Account;
import com.bank.fundtransfer.entity.Customer;
import com.bank.fundtransfer.entity.RequestStatus;
import com.bank.fundtransfer.entity.TransactionRequest;
import com.bank.fundtransfer.exception.AccountFrozenException;
import com.bank.fundtransfer.exception.BankException;
import com.bank.fundtransfer.exception.InsufficientFundsException;
import com.bank.fundtransfer.exception.InvalidOtpException;
import com.bank.fundtransfer.exception.TransactionLimitExceededException;
import com.bank.fundtransfer.service.AdminService;
import com.bank.fundtransfer.service.CustomerService;
import com.bank.fundtransfer.service.FundTransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-flow tests against the real test database. Each test runs in a transaction that is
 * rolled back at the end, so the database stays clean. The OTP is fixed to 111111 in application-test.yml.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FundTransferServiceTest {
    private static final String OTP = "111111";

    @Autowired
    private FundTransferService service;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AdminService adminService;

    private Customer alice;
    private Customer bob;
    private Account aliceAccount;
    private Account bobAccount;

    @BeforeEach
    void setUp() {
        alice = customerService.register("Alice", "alice@test.com", "01700000001", "1234");
        bob = customerService.register("Bob", "bob@test.com", "01700000002", "5678");
        aliceAccount = customerService.getAccounts(alice).get(0);
        bobAccount = customerService.getAccounts(bob).get(0);
        service.deposit(alice, aliceAccount.getAccountNumber(), money("1000.00"));
        service.deposit(bob, bobAccount.getAccountNumber(), money("100.00"));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, money(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private TransactionRequest transferRequest(String amount) {
        return service.requestTransfer(alice, aliceAccount.getAccountNumber(), bobAccount.getAccountNumber(), money(amount));
    }

    private void completeTransfer(TransactionRequest request) {
        service.verifyOtp(alice, request.getRequestCode(), OTP);
        service.confirm(alice, request.getRequestCode());
    }

    // ---------- fees ----------
    @Test
    void successfulTransferMovesAmountAndFee() {
        BigDecimal feeBefore = service.getBankFeeAccount().getBalance();
        TransactionRequest request = transferRequest("500");
        completeTransfer(request);

        assertMoney("490.75", aliceAccount.getBalance());                 // 1000 - 500 - 9.25
        assertMoney("600.00", bobAccount.getBalance());                   // 100 + 500
        assertMoney("9.25", service.getBankFeeAccount().getBalance().subtract(feeBefore));
        assertEquals(RequestStatus.SUCCESS, request.getStatus());
    }

    @Test
    void transferBetweenOwnAccountsHasNoFee() {
        Account second = customerService.openAccount(alice);
        TransactionRequest request = service.requestTransfer(alice, aliceAccount.getAccountNumber(),
                second.getAccountNumber(), money("500"));
        assertMoney("0.00", request.getFee());
        completeTransfer(request);

        assertMoney("500.00", aliceAccount.getBalance());
        assertMoney("500.00", second.getBalance());
    }

    // ---------- OTP and confirmation ----------
    @Test
    void wrongOtpThenCorrectOtpWorks() {
        TransactionRequest request = transferRequest("500");
        InvalidOtpException e = assertThrows(InvalidOtpException.class,
                () -> service.verifyOtp(alice, request.getRequestCode(), "000000"));
        assertEquals(2, e.getRemainingAttempts());

        service.verifyOtp(alice, request.getRequestCode(), OTP);          // must not throw
        assertEquals(RequestStatus.PENDING_CONFIRMATION, request.getStatus());
    }

    @Test
    void requestFailsAfterThreeWrongOtps() {
        TransactionRequest request = transferRequest("500");
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidOtpException.class,
                    () -> service.verifyOtp(alice, request.getRequestCode(), "000000"));
        }
        assertEquals(RequestStatus.FAILED, request.getStatus());
        // even the correct OTP no longer works
        assertThrows(BankException.class, () -> service.verifyOtp(alice, request.getRequestCode(), OTP));
        assertMoney("1000.00", aliceAccount.getBalance());
    }

    @Test
    void cannotConfirmBeforeOtpIsVerified() {
        TransactionRequest request = transferRequest("500");
        assertThrows(BankException.class, () -> service.confirm(alice, request.getRequestCode()));
        assertMoney("1000.00", aliceAccount.getBalance());
    }

    @Test
    void anotherCustomerCannotUseMyRequest() {
        TransactionRequest request = transferRequest("500");
        assertThrows(BankException.class, () -> service.verifyOtp(bob, request.getRequestCode(), OTP));
    }

    @Test
    void cancelledTransferChangesNoBalances() {
        TransactionRequest request = transferRequest("500");
        service.verifyOtp(alice, request.getRequestCode(), OTP);
        service.cancel(alice, request.getRequestCode());

        assertMoney("1000.00", aliceAccount.getBalance());
        assertMoney("100.00", bobAccount.getBalance());
        assertEquals(RequestStatus.CANCELLED, request.getStatus());
    }

    @Test
    void withdrawalFlowWorks() {
        TransactionRequest request = service.requestWithdrawal(alice, aliceAccount.getAccountNumber(), money("200"));
        service.verifyOtp(alice, request.getRequestCode(), OTP);
        service.confirm(alice, request.getRequestCode());
        assertMoney("800.00", aliceAccount.getBalance());
    }

    // ---------- limits ----------
    @Test
    void transferAbovePerTransactionLimitIsRejected() {
        service.deposit(alice, aliceAccount.getAccountNumber(), money("5000.00"));   // balance 6000
        assertThrows(TransactionLimitExceededException.class, () -> transferRequest("2500"));
    }

    @Test
    void dailyLimitIsEnforcedAcrossTransactions() {
        service.deposit(alice, aliceAccount.getAccountNumber(), money("5000.00"));   // balance 6000
        completeTransfer(transferRequest("1900"));
        assertMoney("1900.00", service.getTodaysOutgoingTotal(alice));

        // 1900 + 1900 = 3800, above the daily limit of 3000
        assertThrows(TransactionLimitExceededException.class, () -> transferRequest("1900"));
    }

    @Test
    void ownAccountTransfersIgnoreLimits() {
        service.deposit(alice, aliceAccount.getAccountNumber(), money("5000.00"));   // balance 6000
        Account second = customerService.openAccount(alice);
        TransactionRequest request = service.requestTransfer(alice, aliceAccount.getAccountNumber(),
                second.getAccountNumber(), money("3500"));    // above both limits, but it is her own account
        completeTransfer(request);

        assertMoney("3500.00", second.getBalance());
        assertMoney("0.00", service.getTodaysOutgoingTotal(alice));
    }

    // ---------- minimum balance ----------
    @Test
    void withdrawalCannotBreakMinimumBalance() {
        // Alice has 1000.00 and must keep 100.00
        assertThrows(InsufficientFundsException.class,
                () -> service.requestWithdrawal(alice, aliceAccount.getAccountNumber(), money("950")));

        TransactionRequest ok = service.requestWithdrawal(alice, aliceAccount.getAccountNumber(), money("900"));
        service.verifyOtp(alice, ok.getRequestCode(), OTP);
        service.confirm(alice, ok.getRequestCode());
        assertMoney("100.00", aliceAccount.getBalance());
    }

    @Test
    void transferPlusFeeCannotBreakMinimumBalance() {
        // 900 + 9.25 fee would leave 90.75, below the minimum of 100.00
        assertThrows(InsufficientFundsException.class, () -> transferRequest("900"));
    }

    // ---------- frozen accounts and admin ----------
    @Test
    void frozenAccountCannotSendOrReceive() {
        service.deposit(bob, bobAccount.getAccountNumber(), money("500.00"));        // bob has 600
        adminService.freezeAccount(aliceAccount.getAccountNumber());

        assertThrows(AccountFrozenException.class, () -> transferRequest("100"));
        assertThrows(AccountFrozenException.class, () -> service.requestTransfer(bob,
                bobAccount.getAccountNumber(), aliceAccount.getAccountNumber(), money("100")));
        assertThrows(AccountFrozenException.class,
                () -> service.deposit(alice, aliceAccount.getAccountNumber(), money("10")));

        adminService.unfreezeAccount(aliceAccount.getAccountNumber());
        assertFalse(aliceAccount.isFrozen());
    }

    @Test
    void freezingAfterTheRequestStopsConfirmation() {
        TransactionRequest request = transferRequest("500");
        service.verifyOtp(alice, request.getRequestCode(), OTP);
        adminService.freezeAccount(aliceAccount.getAccountNumber());

        assertThrows(AccountFrozenException.class, () -> service.confirm(alice, request.getRequestCode()));
        assertMoney("1000.00", aliceAccount.getBalance());
        assertEquals(RequestStatus.FAILED, request.getStatus());
    }

    @Test
    void adminLoginAndFreezeRules() {
        assertThrows(BankException.class, () -> adminService.login("admin", "wrong"));
        adminService.login("admin", "admin123");                                      // must not throw

        adminService.freezeAccount(aliceAccount.getAccountNumber());
        assertTrue(aliceAccount.isFrozen());
        assertThrows(BankException.class, () -> adminService.freezeAccount(aliceAccount.getAccountNumber()));
        assertThrows(BankException.class, () -> adminService.freezeAccount("BANK-FEE-001"));
    }
}