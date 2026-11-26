INSERT INTO internal_user (id, name, designation, email) VALUES
  (1, 'Regular Employee', 'regular', 'regular@bank.test'),
  (2, 'Manager Employee', 'manager', 'manager@bank.test'),
  (3, 'Admin Employee', 'admin', 'admin@bank.test');

INSERT INTO external_users (id, name, email) VALUES
  (10, 'Customer One', 'customer@bank.test'),
  (11, 'Merchant One', 'merchant@bank.test'),
  (12, 'Lockout Victim', 'lockme@bank.test'),
  (13, 'Session Holder', 'session@bank.test');

INSERT INTO external_request_pending (requesterid, request_type, current_value, requested_value, status, approver, description, timestamp_created, timestamp_updated) VALUES
  (10, 'phone change', '111', '222', 'pending', '0', 'unassigned request', '2016-10-10', '2016-10-10'),
  (11, 'email change', 'a@x', 'b@x', 'pending', '2', 'assigned to manager', '2016-10-10', '2016-10-10');

INSERT INTO internal_request_pending (requesterid, request_type, current_value, requested_value, status, approver, description, role, timestamp_created, timestamp_updated) VALUES
  (1, 'address change', 'old', 'new', 'pending', '0', 'regular asks admin', 'admin', '2016-10-10', '2016-10-10');

INSERT INTO transaction_pending (payer_id, payee_id, amount, hashvalue, transaction_type, description, status, approver, critical, timestamp_created, timestamp_updated) VALUES
  (10, 11, 25.00, 'h1', 'debit', 'coffee', 'pending', '0', FALSE, '2016-10-10 10:00:00', '2016-10-10 10:00:00');

INSERT INTO bank_accounts (account_number, external_users_id, account_type, balance, hold) VALUES
  (100010, 10, 'SAVINGS', 5000.00, 0), (100011, 10, 'CHECKINGS', 2500.00, 100.00),
  (100021, 11, 'CHECKINGS', 800.00, 0);

INSERT INTO authorization_completed (internal_userID, external_userID, auth_Type) VALUES (1, 10, 'viewAccount');
INSERT INTO authorization_pending (internal_userID, external_userID, auth_Type) VALUES (2, 11, 'viewAccount');
