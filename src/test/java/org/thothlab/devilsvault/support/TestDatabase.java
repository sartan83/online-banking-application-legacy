package org.thothlab.devilsvault.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Shared H2 (MySQL mode) database used by every test; same URL as the test database.properties. */
public final class TestDatabase {

	public static final String PASSWORD = "Passw0rd!";

	public static final String REGULAR = "regular@bank.test";
	public static final String MANAGER = "manager@bank.test";
	public static final String ADMIN = "admin@bank.test";
	public static final String CUSTOMER = "customer@bank.test";
	public static final String MERCHANT = "merchant@bank.test";
	public static final String LOCKOUT = "lockme@bank.test";
	public static final String SESSION = "session@bank.test";
	public static final String OTP_LOCKED = "otplocked@bank.test";

	private TestDatabase() {
	}

	public static DataSource dataSource() {
		Properties props = new Properties();
		try (InputStream in = new ClassPathResource("jdbc/config/database.properties").getInputStream()) {
			props.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		DriverManagerDataSource ds = new DriverManagerDataSource(props.getProperty("database.url"),
				props.getProperty("database.user"), props.getProperty("database.password"));
		ds.setDriverClassName(props.getProperty("database.driver"));
		return ds;
	}

	public static void reset(DataSource dataSource) {
		new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql"), new ClassPathResource("db/data.sql"))
				.execute(dataSource);
		String hash = new BCryptPasswordEncoder(4).encode(PASSWORD);
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		String sql = "INSERT INTO users (username, password, role, otpNonLocked) VALUES (?,?,?,?)";
		jdbc.update(sql, REGULAR, hash, "ROLE_REGULAR", 1);
		jdbc.update(sql, MANAGER, hash, "ROLE_MANAGER", 1);
		jdbc.update(sql, ADMIN, hash, "ROLE_ADMIN", 1);
		jdbc.update(sql, CUSTOMER, hash, "ROLE_CUSTOMER", 1);
		jdbc.update(sql, MERCHANT, hash, "ROLE_MERCHANT", 1);
		jdbc.update(sql, LOCKOUT, hash, "ROLE_CUSTOMER", 1);
		jdbc.update(sql, SESSION, hash, "ROLE_CUSTOMER", 1);
		jdbc.update(sql, OTP_LOCKED, hash, "ROLE_CUSTOMER", 0);
	}
}
