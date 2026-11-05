package org.thothlab.devilsvault.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.sql.Timestamp;

@Document("otp")
public class OTP {
	@Id
	private String userEmail;
	private String otp;
	private Timestamp timestamp;
	private int attempts;

	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
	public String getOtp() {
		return otp;
	}
	public void setOtp(String otp) {
		this.otp = otp;
	}
	public Timestamp getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
	
	public int getAttempts() {
		return attempts;
	}
	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}
}