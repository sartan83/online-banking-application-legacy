-- Full DevilsVault schema for MySQL 5.7 / 8.x, derived from the SQL issued by the DAOs.
-- Usage: mysql -u root -p < database_scripts/devilsvault_mysql.sql
CREATE DATABASE IF NOT EXISTS devilsvault CHARACTER SET utf8mb4;
USE devilsvault;

CREATE TABLE IF NOT EXISTS users (
  username VARCHAR(100) NOT NULL PRIMARY KEY,
  password VARCHAR(100) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  role VARCHAR(30) NOT NULL,
  accountNonExpired TINYINT NOT NULL DEFAULT 1,
  accountNonLocked TINYINT NOT NULL DEFAULT 1,
  credentialsNonExpired TINYINT NOT NULL DEFAULT 1,
  otpNonLocked TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_attempts (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  attempts INT NOT NULL,
  lastModified DATETIME
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS otp_table (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  userEmail VARCHAR(100) NOT NULL,
  otp VARCHAR(20),
  timestamp DATETIME,
  attempts INT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS internal_user (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  designation VARCHAR(30),
  address VARCHAR(200),
  phone BIGINT,
  email VARCHAR(100),
  date_of_birth VARCHAR(255),
  ssn VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS external_users (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100),
  address VARCHAR(200),
  city VARCHAR(50),
  state VARCHAR(50),
  country VARCHAR(50),
  pincode INT,
  phone BIGINT,
  email VARCHAR(100),
  date_of_birth VARCHAR(255),
  ssn VARCHAR(255),
  merchant TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_pending (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  firstName VARCHAR(50), lastName VARCHAR(50), userEmail VARCHAR(100), userSsn VARCHAR(255),
  userPassword VARCHAR(100), country VARCHAR(50), city VARCHAR(50), street VARCHAR(100),
  house VARCHAR(50), state VARCHAR(50), pincode INT, userPhonenumber VARCHAR(20),
  timestamp_created DATETIME
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bank_accounts (
  account_number INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  external_users_id INT NOT NULL,
  account_type VARCHAR(20) NOT NULL,
  balance DECIMAL(15,2) NOT NULL DEFAULT 0,
  hold DECIMAL(15,2) NOT NULL DEFAULT 0
) ENGINE=InnoDB AUTO_INCREMENT=200000;

CREATE TABLE IF NOT EXISTS credit_card_account_details (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  interest INT,
  credit_card_no BIGINT,
  available_balance INT,
  last_bill_amount INT,
  due_date DATE,
  apr FLOAT,
  account_number INT,
  cycle_date DATE,
  current_due_amt INT,
  credit_limit INT,
  payment INT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorized_merchants_users (
  merchant_id INT NOT NULL,
  external_user_id INT NOT NULL,
  PRIMARY KEY (merchant_id, external_user_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorization_pending (
  auth_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  internal_userID INT NOT NULL,
  external_userID INT NOT NULL,
  auth_Type VARCHAR(30) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS authorization_completed (
  auth_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  internal_userID INT NOT NULL,
  external_userID INT NOT NULL,
  auth_Type VARCHAR(30) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transaction_pending (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  payer_id INT, payee_id INT, amount DECIMAL(15,2), hashvalue VARCHAR(255), transaction_type VARCHAR(50),
  description VARCHAR(255), status VARCHAR(20), approver VARCHAR(50), critical BOOLEAN,
  timestamp_created DATETIME, timestamp_updated DATETIME
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transaction_completed LIKE transaction_pending;

CREATE TABLE IF NOT EXISTS internal_request_pending (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  requesterid INT, request_type VARCHAR(50), current_value VARCHAR(255), requested_value VARCHAR(255),
  status VARCHAR(20), approver VARCHAR(50), description VARCHAR(255), role VARCHAR(30),
  timestamp_created DATETIME, timestamp_updated DATETIME
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS internal_request_completed LIKE internal_request_pending;
CREATE TABLE IF NOT EXISTS external_request_pending LIKE internal_request_pending;
CREATE TABLE IF NOT EXISTS external_request_completed LIKE internal_request_pending;

CREATE TABLE IF NOT EXISTS internal_log (
  activity VARCHAR(255) NOT NULL, userid INT NOT NULL, details VARCHAR(255), timestamp DATETIME NULL
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS external_log LIKE internal_log;
