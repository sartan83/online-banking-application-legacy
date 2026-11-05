package org.thothlab.devilsvault.dao.userauthentication;

import java.io.IOException;
import java.util.Properties;

import jakarta.mail.MessagingException;

public class OTPDelivery {

	public String send(String email) {
		Properties settings;
		try {
			settings = MailSupport.settings();
		} catch (IOException ioe) {
			return "Failed OTP Generation ";
		}
		String OTP = OneTimePassword.getOTP();
		try {
			MailSupport.send(settings, email, "Please do not share this with anyone", "Your One Time Password is " + OTP);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		return OTP;
	}
}
