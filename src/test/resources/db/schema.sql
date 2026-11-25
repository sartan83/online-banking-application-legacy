DROP ALL OBJECTS;

CREATE TABLE users (
  username VARCHAR(100) PRIMARY KEY,
  password VARCHAR(100) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  role VARCHAR(30) NOT NULL,
  accountNonExpired TINYINT NOT NULL DEFAULT 1,
  accountNonLocked TINYINT NOT NULL DEFAULT 1,
  credentialsNonExpired TINYINT NOT NULL DEFAULT 1,
  otpNonLocked TINYINT NOT NULL DEFAULT 1
);

CREATE TABLE user_attempts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  attempts INT NOT NULL,
  lastModified TIMESTAMP
);

CREATE TABLE internal_user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100), designation VARCHAR(30), address VARCHAR(200), phone BIGINT,
  email VARCHAR(100), date_of_birth VARCHAR(20), ssn VARCHAR(20)
);

CREATE TABLE external_users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100), address VARCHAR(200), city VARCHAR(50), state VARCHAR(50), country VARCHAR(50),
  pincode INT, phone BIGINT, email VARCHAR(100), date_of_birth VARCHAR(20), ssn VARCHAR(20)
);

CREATE TABLE external_request_pending (
  id INT AUTO_INCREMENT PRIMARY KEY,
  requesterid INT, request_type VARCHAR(50), current_value VARCHAR(100), requested_value VARCHAR(100),
  status VARCHAR(20), approver VARCHAR(50), description VARCHAR(200), role VARCHAR(30),
  timestamp_created DATE, timestamp_updated DATE
);
CREATE TABLE external_request_completed (
  id INT AUTO_INCREMENT PRIMARY KEY,
  requesterid INT, request_type VARCHAR(50), current_value VARCHAR(100), requested_value VARCHAR(100),
  status VARCHAR(20), approver VARCHAR(50), description VARCHAR(200), role VARCHAR(30),
  timestamp_created DATE, timestamp_updated DATE
);
CREATE TABLE internal_request_pending (
  id INT AUTO_INCREMENT PRIMARY KEY,
  requesterid INT, request_type VARCHAR(50), current_value VARCHAR(100), requested_value VARCHAR(100),
  status VARCHAR(20), approver VARCHAR(50), description VARCHAR(200), role VARCHAR(30),
  timestamp_created DATE, timestamp_updated DATE
);
CREATE TABLE internal_request_completed (
  id INT AUTO_INCREMENT PRIMARY KEY,
  requesterid INT, request_type VARCHAR(50), current_value VARCHAR(100), requested_value VARCHAR(100),
  status VARCHAR(20), approver VARCHAR(50), description VARCHAR(200), role VARCHAR(30),
  timestamp_created DATE, timestamp_updated DATE
);

CREATE TABLE transaction_pending (
  id INT AUTO_INCREMENT PRIMARY KEY,
  payer_id INT, payee_id INT, amount DECIMAL(15,2), hashvalue VARCHAR(200), transaction_type VARCHAR(50),
  description VARCHAR(200), status VARCHAR(20), approver VARCHAR(50), critical BOOLEAN,
  timestamp_created TIMESTAMP, timestamp_updated TIMESTAMP
);
CREATE TABLE transaction_completed (
  id INT AUTO_INCREMENT PRIMARY KEY,
  payer_id INT, payee_id INT, amount DECIMAL(15,2), hashvalue VARCHAR(200), transaction_type VARCHAR(50),
  description VARCHAR(200), status VARCHAR(20), approver VARCHAR(50), critical BOOLEAN,
  timestamp_created TIMESTAMP, timestamp_updated TIMESTAMP
);

CREATE TABLE otp_table (
  id INT AUTO_INCREMENT PRIMARY KEY,
  userEmail VARCHAR(100) NOT NULL,
  otp VARCHAR(20),
  timestamp TIMESTAMP,
  attempts INT
);
