package org.thothlab.devilsvault.dao.userauthentication;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * SMTP settings come from {@code smtp.properties} (defaults: Gmail on 587 with STARTTLS, as in the legacy code);
 * SMTP_HOST, SMTP_PORT, SMTP_AUTH, SMTP_STARTTLS, SMTP_USERNAME, SMTP_PASSWORD and SMTP_FROM env vars override them.
 */
final class MailSupport {

	private MailSupport() {
	}

	static Properties settings() throws IOException {
		Properties file = new Properties();
		try (InputStream in = MailSupport.class.getClassLoader().getResourceAsStream("smtp.properties")) {
			if (in == null) {
				throw new IOException("smtp.properties not found");
			}
			file.load(in);
		}
		Properties s = new Properties();
		s.setProperty("username", value("SMTP_USERNAME", file.getProperty("username", "")));
		s.setProperty("password", value("SMTP_PASSWORD", file.getProperty("password", "")));
		s.setProperty("host", value("SMTP_HOST", file.getProperty("host", "smtp.gmail.com")));
		s.setProperty("port", value("SMTP_PORT", file.getProperty("port", "587")));
		s.setProperty("auth", value("SMTP_AUTH", file.getProperty("auth", "true")));
		s.setProperty("starttls", value("SMTP_STARTTLS", file.getProperty("starttls", "true")));
		s.setProperty("from", value("SMTP_FROM", file.getProperty("from", "securebanking.ss4@gmail.com")));
		return s;
	}

	private static String value(String env, String fallback) {
		String v = System.getenv(env);
		if (v == null) {
			v = System.getProperty(env);
		}
		return v == null || v.isEmpty() ? fallback : v;
	}

	static void send(Properties settings, String to, String subject, String text) throws MessagingException {
		final String username = settings.getProperty("username");
		final String password = settings.getProperty("password");
		Properties props = new Properties();
		props.put("mail.smtp.auth", settings.getProperty("auth"));
		props.put("mail.smtp.starttls.enable", settings.getProperty("starttls"));
		props.put("mail.smtp.host", settings.getProperty("host"));
		props.put("mail.smtp.port", settings.getProperty("port"));
		Session session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(settings.getProperty("from")));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		message.setText(text);
		Transport.send(message);
	}
}
