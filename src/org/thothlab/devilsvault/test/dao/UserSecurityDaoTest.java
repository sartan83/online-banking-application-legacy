package org.thothlab.devilsvault.test.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.thothlab.devilsvault.dao.user.CustomUserDetailsService;
import org.thothlab.devilsvault.dao.user.UserDetailsDao;
import org.thothlab.devilsvault.dao.userauthentication.OtpDaoImpl;
import org.thothlab.devilsvault.dao.userauthentication.UserAuthenticationDaoImpl;
import org.thothlab.devilsvault.dao.userauthentication.UserLoginManagementDaoImpl;
import org.thothlab.devilsvault.db.model.OTP;
import org.thothlab.devilsvault.db.model.UserAuthentication;
import org.thothlab.devilsvault.test.support.DaoTestConfig;
import org.thothlab.devilsvault.test.support.MongoContainerSupport;
import org.thothlab.devilsvault.test.support.TestData;

@DataMongoTest
@Import(DaoTestConfig.class)
class UserSecurityDaoTest extends MongoContainerSupport {

	static final String ALICE = "alice@devilsvault.com";

	@Autowired
	MongoTemplate mongo;
	@Autowired
	UserDetailsDao userDetailsDao;
	@Autowired
	CustomUserDetailsService userDetailsService;
	@Autowired
	UserLoginManagementDaoImpl loginManagementDao;
	@Autowired
	OtpDaoImpl otpDao;
	@Autowired
	UserAuthenticationDaoImpl userAuthenticationDao;

	@BeforeEach
	void seed() {
		TestData.load(mongo);
	}

	@Test
	void userDetailsCarryRoleAndLockFlag() {
		UserDetails alice = userDetailsService.loadUserByUsername(ALICE);
		assertThat(alice.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTOMER");
		assertThat(alice.isAccountNonLocked()).isTrue();
	}

	@Test
	void thirdFailedAttemptLocksTheAccountAndAdminUnlocks() {
		userDetailsDao.updateFailAttempts(ALICE);
		userDetailsDao.updateFailAttempts(ALICE);
		assertThat(userDetailsDao.getUserAttempts(ALICE).getAttempts()).isEqualTo(2);
		assertThatThrownBy(() -> userDetailsDao.updateFailAttempts(ALICE)).isInstanceOf(LockedException.class);

		assertThat(userDetailsService.loadUserByUsername(ALICE).isAccountNonLocked()).isFalse();
		assertThat(loginManagementDao.getLockedUsers(List.of("ROLE_CUSTOMER", "ROLE_MERCHANT")))
				.extracting(UserAuthentication::getUsername).containsExactly(ALICE);

		loginManagementDao.unlockUserAccount(ALICE, "account");
		assertThat(userDetailsService.loadUserByUsername(ALICE).isAccountNonLocked()).isTrue();
		assertThat(userDetailsDao.getUserAttempts(ALICE)).isNull();
	}

	@Test
	void successfulLoginResetsAttempts() {
		userDetailsDao.updateFailAttempts(ALICE);
		userDetailsDao.resetFailAttempts(ALICE);
		assertThat(userDetailsDao.getUserAttempts(ALICE).getAttempts()).isZero();
	}

	@Test
	void otpVerification() {
		OTP otp = new OTP();
		otp.setUserEmail(ALICE);
		otp.setOtp("123456");
		otp.setAttempts(1);
		otp.setTimestamp(new Timestamp(System.currentTimeMillis()));
		mongo.insert(otp);
		assertThat(otpDao.verifyOTP("000000", ALICE)).isEqualTo("Incorrect OTP");
		assertThat(otpDao.verifyOTP("123456", ALICE)).isEqualTo("OTP Validated");
		assertThat(otpDao.verifyOTP("123456", ALICE)).isEqualTo("Error in verifying OTP");

		otp.setTimestamp(new Timestamp(System.currentTimeMillis() - 200_000));
		mongo.insert(otp);
		assertThat(otpDao.verifyOTP("123456", ALICE)).isEqualTo("OTP Expired");
	}

	@Test
	void fourthOtpRequestLocksOtpGeneration() {
		OTP otp = new OTP();
		otp.setUserEmail(ALICE);
		otp.setOtp("123456");
		otp.setAttempts(3);
		otp.setTimestamp(new Timestamp(System.currentTimeMillis()));
		mongo.insert(otp);
		assertThat(otpDao.processOTP(ALICE)).isEqualTo("Account Locked");
		assertThat(otpDao.verifyEmail(ALICE)).isEqualTo("Cannot generate more OTP, Contact Support !!");
		assertThat(loginManagementDao.getOtpLockedUsers(List.of("ROLE_CUSTOMER"))).hasSize(1);
	}

	@Test
	void changePasswordValidatesOldPasswordAndPolicy() {
		assertThat(userAuthenticationDao.changePassword("wrong", "N3w@pass", "N3w@pass", 1, "ROLE_CUSTOMER"))
				.isEqualTo("Old password entered is incorrect");
		assertThat(userAuthenticationDao.changePassword(TestData.PASSWORD, "weak", "weak", 1, "ROLE_CUSTOMER"))
				.isEqualTo("Password not meeting minimum criteria");
		assertThat(userAuthenticationDao.changePassword(TestData.PASSWORD, "N3w@pass", "N3w@pass", 1, "ROLE_CUSTOMER"))
				.isEqualTo("Password changed successfully");
		assertThat(userAuthenticationDao.validateuserDetails(ALICE, java.math.BigInteger.ONE, "external_users")).isFalse();
		assertThat(userAuthenticationDao.validateuserDetails("new@devilsvault.com", java.math.BigInteger.ONE,
				"external_users")).isTrue();
	}
}
