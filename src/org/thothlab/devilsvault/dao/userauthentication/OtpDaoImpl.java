package org.thothlab.devilsvault.dao.userauthentication;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.OTP;
import org.thothlab.devilsvault.db.model.UserAuthentication;

@Repository("OtpDaoImpl")
public class OtpDaoImpl implements OtpDao {

	String role;
	int userID;
	String username;

	private final MongoTemplate mongoTemplate;

	public OtpDaoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public void setGlobals(HttpServletRequest request) {
		role = (String) request.getSession().getAttribute("role");
		userID = (int) request.getSession().getAttribute("userID");
		username = (String) request.getSession().getAttribute("username");
	}

	@Override
	public String verifyEmail(String email) {
		try {
			InternetAddress emailAddress = new InternetAddress(email);
			emailAddress.validate();
		} catch (AddressException ae) {
			return "Invalid User";
		}
		UserAuthentication user = mongoTemplate.findById(email, UserAuthentication.class);
		if (user == null)
			return "Invalid User";
		if (user.getOtpNonLocked() == 0)
			return "Cannot generate more OTP, Contact Support !!";
		return processOTP(user.getUsername());
	}

	@Override
	public String processOTP(String email) {
		OTP existing = mongoTemplate.findById(email, OTP.class);
		String otp;
		if (existing == null) {
			otp = generateOTP(email);
			OTP record = new OTP();
			record.setUserEmail(email);
			record.setOtp(otp);
			record.setTimestamp(new Timestamp(System.currentTimeMillis()));
			record.setAttempts(1);
			mongoTemplate.insert(record);
			return "User Verified";
		}
		if (existing.getAttempts() >= 3) {
			mongoTemplate.updateFirst(Query.query(where("_id").is(email)), new Update().set("otpNonLocked", 0),
					UserAuthentication.class);
			mongoTemplate.remove(Query.query(where("_id").is(email)), OTP.class);
			return "Account Locked";
		}
		otp = generateOTP(email);
		mongoTemplate.updateFirst(Query.query(where("_id").is(email)),
				new Update().set("attempts", existing.getAttempts() + 1)
						.set("timestamp", new Timestamp(System.currentTimeMillis())).set("otp", otp),
				OTP.class);
		return "User Verified";
	}

	@Override
	public String updatePassword(String email, String password) {
		mongoTemplate.updateFirst(Query.query(where("_id").is(email)), new Update().set("password", password),
				UserAuthentication.class);
		return "Password Changed";
	}

	@Override
	public String generateOTP(String email) {
		OTPDelivery otpDelivery = new OTPDelivery();
		return otpDelivery.send(email);
	}

	@Override
	public String verifyOTP(String otp, String email) {
		OTP record = mongoTemplate.findById(email, OTP.class);
		if (record == null)
			return "Error in verifying OTP";
		long databaseDate = record.getTimestamp().getTime();
		long currentDate = System.currentTimeMillis();
		if (record.getOtp().equals(otp) == false)
			return "Incorrect OTP";
		if ((currentDate - databaseDate > 180000))
			return "OTP Expired";
		mongoTemplate.remove(Query.query(where("_id").is(email)), OTP.class);
		return "OTP Validated";
	}

	@Override
	public String getEmailFromPayerID(int payerid) {
		Customer customer = mongoTemplate.findById(payerid, Customer.class);
		if (customer == null)
			return "User Not Found";
		return customer.getEmail();
	}

	@Override
	public String getOTPFromEmail(String email) {
		OTP record = mongoTemplate.findById(email, OTP.class);
		if (record == null)
			return "OTP Not Found";
		return record.getUserEmail();
	}

	@Override
	public void sendEmailToUser(String email, String payeeid, BigDecimal amount) {
		Properties settings;
		try {
			settings = MailSupport.settings();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}
		try {
			MailSupport.send(settings, email, "A critical transaction has been initiated",
					"Transfer amount to: " + payeeid + "\n\nAmount: " + amount);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
