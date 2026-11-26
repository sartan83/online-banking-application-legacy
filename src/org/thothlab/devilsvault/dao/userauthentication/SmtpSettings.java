package org.thothlab.devilsvault.dao.userauthentication;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

/**
 * Builds the Jakarta Mail session from classpath {@code smtp.properties}.
 * Only {@code username}/{@code password} are required; host, port, auth, STARTTLS and
 * sender default to the original Gmail settings.
 */
public final class SmtpSettings {

	public static final String DEFAULT_HOST = "smtp.gmail.com";
	public static final String DEFAULT_PORT = "587";
	public static final String DEFAULT_FROM = "securebanking.ss4@gmail.com";

	private final Properties props;

	private SmtpSettings(Properties props) {
		this.props = props;
	}

	public static SmtpSettings load() {
		Properties props = new Properties();
		try (InputStream in = SmtpSettings.class.getClassLoader().getResourceAsStream("smtp.properties")) {
			if (in != null) {
				props.load(in);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return new SmtpSettings(props);
	}

	public String from() {
		return props.getProperty("from", DEFAULT_FROM);
	}

	public Session session() {
		boolean auth = Boolean.parseBoolean(props.getProperty("auth", "true"));
		Properties mail = new Properties();
		mail.put("mail.smtp.auth", String.valueOf(auth));
		mail.put("mail.smtp.starttls.enable", props.getProperty("starttls", "true"));
		mail.put("mail.smtp.host", props.getProperty("host", DEFAULT_HOST));
		mail.put("mail.smtp.port", props.getProperty("port", DEFAULT_PORT));
		if (!auth) {
			return Session.getInstance(mail);
		}
		final String username = props.getProperty("username");
		final String password = props.getProperty("password");
		return Session.getInstance(mail, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}
}
