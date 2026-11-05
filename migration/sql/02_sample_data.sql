-- Sample data. Every login uses the password  Passw0rd@  (BCrypt, strength 10).

INSERT INTO users (username, password, enabled, role, accountNonExpired, accountNonLocked, credentialsNonExpired, otpNonLocked) VALUES
 ('alice@devilsvault.com',   '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_CUSTOMER', 1, 1, 1, 1),
 ('bob@devilsvault.com',     '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_CUSTOMER', 1, 1, 1, 1),
 ('shop@devilsvault.com',    '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_MERCHANT', 1, 1, 1, 1),
 ('carol@devilsvault.com',   '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_CUSTOMER', 1, 0, 1, 1),
 ('regular@devilsvault.com', '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_REGULAR',  1, 1, 1, 1),
 ('manager@devilsvault.com', '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_MANAGER',  1, 1, 1, 1),
 ('admin@devilsvault.com',   '$2a$10$u.n97pWniQ.cBnotrcYfaerkMa315Ie52mGZO2aZyDQZYqAaS/kIC', 1, 'ROLE_ADMIN',    1, 1, 1, 1);

INSERT INTO user_attempts (username, attempts, lastModified) VALUES
 ('carol@devilsvault.com', 3, NOW() - INTERVAL 1 DAY);

INSERT INTO external_users (id, name, address, city, state, country, pincode, phone, email, date_of_birth, ssn) VALUES
 (1, 'Alice Rossi',    '1 Palm Walk',      'Tempe', 'AZ', 'USA', 85281, 4805550101, 'alice@devilsvault.com', '1990-04-12', '123-45-6789'),
 (2, 'Bob Bianchi',    '22 University Dr', 'Tempe', 'AZ', 'USA', 85281, 4805550102, 'bob@devilsvault.com',   '1988-11-03', '234-56-7890'),
 (3, 'Sun Devil Shop', '500 Mill Ave',     'Tempe', 'AZ', 'USA', 85281, 4805550103, 'shop@devilsvault.com',  '1980-01-01', '345-67-8901'),
 (4, 'Carol Verdi',    '9 Apache Blvd',    'Tempe', 'AZ', 'USA', 85281, 4805550104, 'carol@devilsvault.com', '1995-07-21', '456-78-9012');

INSERT INTO internal_user (id, name, designation, address, phone, email, date_of_birth, ssn) VALUES
 (1, 'Rita Regular', 'REGULAR', '10 Bank St', 4805550201, 'regular@devilsvault.com', '1985-02-02', '567-89-0123'),
 (2, 'Marco Manager','MANAGER', '11 Bank St', 4805550202, 'manager@devilsvault.com', '1979-03-03', '678-90-1234'),
 (3, 'Ada Admin',    'ADMIN',   '12 Bank St', 4805550203, 'admin@devilsvault.com',   '1975-04-04', '789-01-2345');

INSERT INTO authorized_merchants_users (merchant_id, external_user_id) VALUES (3, 1);

INSERT INTO bank_accounts (account_number, external_users_id, account_type, balance, hold) VALUES
 (1001, 1, 'CHECKING', 5000.00, 0), (1002, 1, 'SAVINGS', 2500.00, 0), (1003, 1, 'CREDIT', 0, 0),
 (1004, 2, 'CHECKING', 1200.00, 0), (1005, 2, 'SAVINGS',  800.00, 0), (1006, 2, 'CREDIT', 0, 0),
 (1007, 3, 'CHECKING', 10000.00, 0), (1008, 3, 'SAVINGS',   0.00, 0), (1009, 3, 'CREDIT', 0, 0),
 (1010, 4, 'CHECKING', 300.00, 0), (1011, 4, 'SAVINGS',   50.00, 0), (1012, 4, 'CREDIT', 0, 0);

INSERT INTO credit_card_account_details (id, interest, credit_card_no, available_balance, last_bill_amount, due_date, apr, account_number, cycle_date, current_due_amt, credit_limit, payment) VALUES
 (1, 5, 5412303716559819, 0, 0, '2016-11-15', 2.2, 1003, '2016-11-25', 0, 800, 0),
 (2, 5, 5523335147124165, 0, 0, '2016-11-15', 2.2, 1006, '2016-11-25', 0, 800, 0),
 (3, 5, 5134614022235713, 0, 0, '2016-11-15', 2.2, 1009, '2016-11-25', 0, 800, 0),
 (4, 5, 5412303716551113, 0, 0, '2016-11-15', 2.2, 1012, '2016-11-25', 0, 800, 0);

INSERT INTO authorization_completed (auth_id, internal_userID, external_userID, auth_Type) VALUES
 (1, 1, 1, 'transaction'), (2, 1, 2, 'transaction');
INSERT INTO authorization_pending (auth_id, internal_userID, external_userID, auth_Type) VALUES
 (1, 1, 3, 'transaction');

INSERT INTO transaction_completed (id, payer_id, payee_id, amount, hashvalue, transaction_type, description, status, approver, critical, timestamp_created, timestamp_updated) VALUES
 (1, 1001, 1004, 150.00, '', 'externalFundTfr', 'Dinner split', 'approve', '1', 0, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 9 DAY),
 (2, 1004, 1001,  40.00, '', 'externalFundTfr', 'Movie tickets', 'approve', '1', 0, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 4 DAY);

INSERT INTO external_request_pending (id, requesterid, request_type, current_value, requested_value, status, approver, description, timestamp_created, timestamp_updated, role) VALUES
 (1, 2, 'address', '22 University Dr', '77 Rural Rd', 'Pending', '0', 'Request to change address', CURDATE(), CURDATE(), 'ROLE_CUSTOMER');

INSERT INTO internal_request_completed (id, requesterid, request_type, current_value, requested_value, status, approver, description, timestamp_created, timestamp_updated, role) VALUES
 (1, 1, 'address', '9 Bank St', '10 Bank St', 'approved', '2', 'Request to change address', CURDATE() - INTERVAL 30 DAY, CURDATE() - INTERVAL 29 DAY, 'ROLE_REGULAR');

INSERT INTO user_pending (id, firstName, lastName, userEmail, userSsn, userPassword, country, city, street, house, state, pincode, userPhonenumber, timestamp_created) VALUES
 (1, 'Dario', 'Neri', 'dario@devilsvault.com', '890-12-3456', '', 'USA', 'Tempe', 'Lemon St', '4', 'AZ', 85281, '4805550105', CURDATE());

INSERT INTO internal_log (activity, userid, details, timestamp) VALUES
 ('Entered in /employee/home...', 1, 'Populate pending statistics of external users', NOW() - INTERVAL 2 DAY),
 ('Entered in /employee/transaction...', 2, 'Fetch all completed and pending transactions', NOW() - INTERVAL 1 DAY);
INSERT INTO external_log (activity, userid, details, timestamp) VALUES
 ('Entered in /customer/home...', 1, 'Customer dashboard', NOW() - INTERVAL 1 DAY);

INSERT INTO account (id) VALUES (1001), (1004);
