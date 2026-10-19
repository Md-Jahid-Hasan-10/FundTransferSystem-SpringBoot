-- The bank's own account that collects transfer fees (no owner, no minimum balance)
INSERT INTO accounts (account_number, owner_id, role, status, balance, minimum_balance, version)
VALUES ('BANK-FEE-001', NULL, 'BANK_FEE_COLLECTION', 'ACTIVE', 0, 0, 0);