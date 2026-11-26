package org.thothlab.devilsvault.dao.userauthentication;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class OTPDelivery {
	public String send(String email) {

		String OTP = "";
		SmtpSettings smtp = SmtpSettings.load();
		Session session = smtp.session();

		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(smtp.from()));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
			message.setSubject("Please do not share this with anyone");
			{
				OTP = OneTimePassword.getOTP();
				message.setText("Your One Time Password is " + OTP);
			}
			Transport.send(message);

			System.out.println("Done");

		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		return OTP;
	}

}