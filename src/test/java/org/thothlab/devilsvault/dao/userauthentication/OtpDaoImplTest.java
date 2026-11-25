package org.thothlab.devilsvault.dao.userauthentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thothlab.devilsvault.support.TestDatabase;

import jakarta.mail.Message;
import jakarta.mail.Transport;

class OtpDaoImplTest {

	DataSource dataSource = TestDatabase.dataSource();
	JdbcTemplate jdbc = new JdbcTemplate(dataSource);
	OtpDaoImpl dao = new OtpDaoImpl();
	MockedStatic<Transport> transport;

	@BeforeEach
	void setUp() {
		TestDatabase.reset(dataSource);
		dao.setDataSource(dataSource);
		transport = mockStatic(Transport.class);
	}

	@AfterEach
	void tearDown() {
		transport.close();
	}

	private List<Message> sentMessages(int expected) {
		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		transport.verify(() -> Transport.send(captor.capture()), times(expected));
		return captor.getAllValues();
	}

	private static String otpIn(Message message) throws Exception {
		return message.getContent().toString().replace("Your One Time Password is ", "");
	}

	@Test
	void rejectsMalformedOrUnknownEmail() {
		assertThat(dao.verifyEmail("not an email")).isEqualTo("Invalid User");
		assertThat(dao.verifyEmail("ghost@bank.test")).isEqualTo("Invalid User");
		transport.verify(() -> Transport.send(any(Message.class)), never());
	}

	@Test
	void refusesOtpForOtpLockedUser() {
		assertThat(dao.verifyEmail(TestDatabase.OTP_LOCKED)).isEqualTo("Cannot generate more OTP, Contact Support !!");
		transport.verify(() -> Transport.send(any(Message.class)), never());
	}

	@Test
	void generatesStoresAndValidatesOtp() throws Exception {
		assertThat(dao.verifyEmail(TestDatabase.CUSTOMER)).isEqualTo("User Verified");
		String otp = otpIn(sentMessages(1).get(0));

		assertThat(jdbc.queryForObject("SELECT otp FROM otp_table WHERE userEmail = ?", String.class,
				TestDatabase.CUSTOMER)).isEqualTo(otp);
		assertThat(dao.verifyOTP("000000".equals(otp) ? "111111" : "000000", TestDatabase.CUSTOMER))
				.isEqualTo("Incorrect OTP");
		assertThat(dao.verifyOTP(otp, TestDatabase.CUSTOMER)).isEqualTo("OTP Validated");
		assertThat(dao.verifyOTP(otp, TestDatabase.CUSTOMER)).isEqualTo("Error in verifying OTP");
	}

	@Test
	void expiredOtpIsRejected() throws Exception {
		dao.verifyEmail(TestDatabase.CUSTOMER);
		String otp = otpIn(sentMessages(1).get(0));
		jdbc.update("UPDATE otp_table SET timestamp = DATEADD('MINUTE', -4, CURRENT_TIMESTAMP) WHERE userEmail = ?",
				TestDatabase.CUSTOMER);

		assertThat(dao.verifyOTP(otp, TestDatabase.CUSTOMER)).isEqualTo("OTP Expired");
	}

	@Test
	void locksOtpAfterThreeRequests() {
		for (int i = 0; i < 3; i++) {
			assertThat(dao.verifyEmail(TestDatabase.CUSTOMER)).isEqualTo("User Verified");
		}
		assertThat(dao.verifyEmail(TestDatabase.CUSTOMER)).isEqualTo("Account Locked");
		sentMessages(3);
		assertThat(jdbc.queryForObject("SELECT otpNonLocked FROM users WHERE username = ?", Integer.class,
				TestDatabase.CUSTOMER)).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM otp_table", Integer.class)).isZero();
		assertThat(dao.verifyEmail(TestDatabase.CUSTOMER)).isEqualTo("Cannot generate more OTP, Contact Support !!");
	}

	@Test
	void notifiesUserOfCriticalTransaction() throws Exception {
		dao.sendEmailToUser(TestDatabase.CUSTOMER, "11", new BigDecimal("1500.00"));

		Message message = sentMessages(1).get(0);
		assertThat(message.getAllRecipients()[0].toString()).isEqualTo(TestDatabase.CUSTOMER);
		assertThat(message.getContent().toString()).contains("1500.00");
	}
}
