-- Full DevilsVault relational schema (MySQL 8), reconstructed from every SQL statement issued by the
-- legacy JdbcTemplate DAOs (database_scripts/devilsvault_internal.sql only covers 9 of these tables).
-- Used as the source side of migration/migrate.py.

CREATE TABLE users (
  username VARCHAR(100) NOT NULL PRIMARY KEY,
  password VARCHAR(100) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  role VARCHAR(45) NOT NULL,
  accountNonExpired TINYINT NOT NULL DEFAULT 1,
  accountNonLocked TINYINT NOT NULL DEFAULT 1,
  credentialsNonExpired TINYINT NOT NULL DEFAULT 1,
  otpNonLocked TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE user_attempts (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  attempts INT NOT NULL,
  lastModified DATETIME
) ENGINE=InnoDB;

CREATE TABLE external_users (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  address VARCHAR(100),
  city VARCHAR(20),
  state VARCHAR(20),
  country VARCHAR(20),
  pincode INT,
  phone BIGINT,
  email VARCHAR(100) NOT NULL UNIQUE,
  date_of_birth VARCHAR(20),
  ssn VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE internal_user (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  designation VARCHAR(15) NOT NULL,
  address VARCHAR(100),
  phone BIGINT,
  email VARCHAR(100) NOT NULL UNIQUE,
  date_of_birth VARCHAR(20),
  ssn VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE authorized_merchants_users (
  merchant_id INT NOT NULL,
  external_user_id INT NOT NULL,
  PRIMARY KEY (merchant_id, external_user_id)
) ENGINE=InnoDB;

CREATE TABLE bank_accounts (
  account_number INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  external_users_id INT NOT NULL,
  account_type VARCHAR(20) NOT NULL,
  balance DECIMAL(15,2) NOT NULL DEFAULT 0,
  hold DECIMAL(15,2) NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE credit_card_account_details (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  interest INT,
  credit_card_no BIGINT NOT NULL UNIQUE,
  available_balance INT,
  last_bill_amount INT,
  due_date DATE,
  apr FLOAT,
  account_number INT NOT NULL,
  cycle_date DATE,
  current_due_amt INT,
  credit_limit INT,
  payment INT
) ENGINE=InnoDB;

CREATE TABLE transaction_pending (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  payer_id INT NOT NULL,
  payee_id INT NOT NULL,
  amount DECIMAL(15,2) NOT NULL,
  hashvalue VARCHAR(255) NOT NULL,
  transaction_type VARCHAR(20) NOT NULL,
  description VARCHAR(255),
  status VARCHAR(20) NOT NULL,
  approver VARCHAR(50) NOT NULL,
  critical BOOLEAN NOT NULL,
  timestamp_created TIMESTAMP NULL,
  timestamp_updated TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE transaction_completed LIKE transaction_pending;

CREATE TABLE internal_request_pending (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  requesterid INT NOT NULL,
  request_type VARCHAR(20) NOT NULL,
  current_value VARCHAR(100) NOT NULL,
  requested_value VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL,
  approver VARCHAR(50) NOT NULL,
  description VARCHAR(100),
  timestamp_created DATE,
  timestamp_updated DATE,
  role VARCHAR(45)
) ENGINE=InnoDB;

CREATE TABLE internal_request_completed LIKE internal_request_pending;
CREATE TABLE external_request_pending LIKE internal_request_pending;
CREATE TABLE external_request_completed LIKE internal_request_pending;

CREATE TABLE authorization_pending (
  auth_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  internal_userID INT NOT NULL,
  external_userID INT NOT NULL,
  auth_Type VARCHAR(20) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE authorization_completed LIKE authorization_pending;

CREATE TABLE user_pending (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  firstName VARCHAR(50),
  lastName VARCHAR(50),
  userEmail VARCHAR(100),
  userSsn VARCHAR(20),
  userPassword VARCHAR(100),
  country VARCHAR(20),
  city VARCHAR(20),
  street VARCHAR(50),
  house VARCHAR(20),
  state VARCHAR(20),
  pincode INT,
  userPhonenumber VARCHAR(20),
  timestamp_created DATE
) ENGINE=InnoDB;

CREATE TABLE otp_table (
  userEmail VARCHAR(100) NOT NULL PRIMARY KEY,
  otp VARCHAR(10) NOT NULL,
  timestamp TIMESTAMP NULL,
  attempts INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE internal_log (
  activity VARCHAR(255) NOT NULL,
  userid INT NOT NULL,
  details VARCHAR(255),
  timestamp TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE external_log LIKE internal_log;

CREATE TABLE account (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY
) ENGINE=InnoDB;
