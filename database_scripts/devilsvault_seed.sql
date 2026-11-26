-- Demo data for local/staging runs. Every user's password is: Passw0rd!
USE devilsvault;
SET @pw = '$2a$10$s50giW4gcNwjiYyoTg3Eze/3WIjtSb38ZRwa4WQM5RenNrYcYmlXK';

INSERT INTO users (username, password, role) VALUES
  ('regular@bank.test',  @pw, 'ROLE_REGULAR'),
  ('manager@bank.test',  @pw, 'ROLE_MANAGER'),
  ('admin@bank.test',    @pw, 'ROLE_ADMIN'),
  ('customer@bank.test', @pw, 'ROLE_CUSTOMER'),
  ('customer2@bank.test',@pw, 'ROLE_CUSTOMER'),
  ('merchant@bank.test', @pw, 'ROLE_MERCHANT');

INSERT INTO internal_user (id, name, designation, address, phone, email) VALUES
  (1, 'Rita Regular', 'ROLE_REGULAR', '1 Bank St', 4805550001, 'regular@bank.test'),
  (2, 'Mario Manager', 'ROLE_MANAGER', '2 Bank St', 4805550002, 'manager@bank.test'),
  (3, 'Ada Admin', 'ROLE_ADMIN', '3 Bank St', 4805550003, 'admin@bank.test');

INSERT INTO external_users (id, name, address, city, state, country, pincode, phone, email, merchant) VALUES
  (10, 'Carla Customer', '10 Main St', 'Tempe', 'AZ', 'USA', 85281, 4805550010, 'customer@bank.test', 0),
  (11, 'Bruno Customer', '11 Main St', 'Tempe', 'AZ', 'USA', 85281, 4805550011, 'customer2@bank.test', 0),
  (12, 'Mega Merchant', '12 Market St', 'Phoenix', 'AZ', 'USA', 85001, 4805550012, 'merchant@bank.test', 1);

INSERT INTO bank_accounts (account_number, external_users_id, account_type, balance, hold) VALUES
  (100010, 10, 'SAVINGS', 5000.00, 0), (100011, 10, 'CHECKING', 2500.00, 0), (100012, 10, 'CREDIT', 0, 0),
  (100020, 11, 'SAVINGS', 1000.00, 0), (100021, 11, 'CHECKING', 800.00, 0),
  (100031, 12, 'CHECKING', 10000.00, 0);

INSERT INTO credit_card_account_details (interest, credit_card_no, available_balance, last_bill_amount, due_date, apr, account_number, cycle_date, current_due_amt, credit_limit, payment) VALUES
  (0, 4532015112830366, 0, 0, DATE_ADD(CURDATE(), INTERVAL 20 DAY), 18.5, 100012, CURDATE(), 0, 5000, 0);

INSERT INTO authorization_completed (internal_userID, external_userID, auth_Type) VALUES
  (1, 10, 'viewAccount'), (2, 10, 'viewAccount');
