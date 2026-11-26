package org.thothlab.devilsvault.dao.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.LockedException;
import org.thothlab.devilsvault.support.TestDatabase;

class UserDetailsDaoImplTest {

	DataSource dataSource = TestDatabase.dataSource();
	UserDetailsDaoImpl dao = new UserDetailsDaoImpl();
	JdbcTemplate jdbc = new JdbcTemplate(dataSource);

	@BeforeEach
	void setUp() {
		TestDatabase.reset(dataSource);
		dao.setDataSource(dataSource);
	}

	private int accountNonLocked(String user) {
		return jdbc.queryForObject("SELECT accountNonLocked FROM users WHERE username = ?", Integer.class, user);
	}

	@Test
	void firstFailureCreatesAttemptRecord() {
		assertThat(dao.getUserAttempts(TestDatabase.CUSTOMER)).isNull();
		dao.updateFailAttempts(TestDatabase.CUSTOMER);
		assertThat(dao.getUserAttempts(TestDatabase.CUSTOMER).getAttempts()).isEqualTo(1);
	}

	@Test
	void thirdFailureLocksAccount() {
		dao.updateFailAttempts(TestDatabase.CUSTOMER);
		dao.updateFailAttempts(TestDatabase.CUSTOMER);
		assertThat(accountNonLocked(TestDatabase.CUSTOMER)).isEqualTo(1);

		assertThatThrownBy(() -> dao.updateFailAttempts(TestDatabase.CUSTOMER)).isInstanceOf(LockedException.class);
		assertThat(dao.getUserAttempts(TestDatabase.CUSTOMER).getAttempts()).isEqualTo(3);
		assertThat(accountNonLocked(TestDatabase.CUSTOMER)).isZero();
	}

	@Test
	void resetClearsAttempts() {
		dao.updateFailAttempts(TestDatabase.CUSTOMER);
		dao.updateFailAttempts(TestDatabase.CUSTOMER);
		dao.resetFailAttempts(TestDatabase.CUSTOMER);
		assertThat(dao.getUserAttempts(TestDatabase.CUSTOMER).getAttempts()).isZero();
	}

	@Test
	void unknownUserIsIgnored() {
		dao.updateFailAttempts("ghost@bank.test");
		assertThat(dao.getUserAttempts("ghost@bank.test")).isNull();
	}
}
