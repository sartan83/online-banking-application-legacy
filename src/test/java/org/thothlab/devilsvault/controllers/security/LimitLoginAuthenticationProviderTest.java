package org.thothlab.devilsvault.controllers.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thothlab.devilsvault.dao.user.UserDetailsDao;
import org.thothlab.devilsvault.db.model.UserAttempts;

@ExtendWith(MockitoExtension.class)
class LimitLoginAuthenticationProviderTest {

	private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
	private static final String HASH = ENCODER.encode("secret");

	@Mock
	UserDetailsService userDetailsService;

	@Mock
	UserDetailsDao userDetailsDao;

	LimitLoginAuthenticationProvider provider;

	@BeforeEach
	void setUp() {
		provider = new LimitLoginAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService);
		provider.setPasswordEncoder(ENCODER);
		provider.setUserDetailsDao(userDetailsDao);
	}

	private static User user(boolean nonLocked) {
		return new User("alice", HASH, true, true, true, nonLocked, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
	}

	@Test
	void successfulLoginResetsFailedAttempts() {
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(user(true));

		Authentication auth = provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret"));

		assertThat(auth.isAuthenticated()).isTrue();
		assertThat(AuthorityUtils.authorityListToSet(auth.getAuthorities())).containsExactly("ROLE_CUSTOMER");
		verify(userDetailsDao).resetFailAttempts("alice");
		verify(userDetailsDao, never()).updateFailAttempts(anyString());
	}

	@Test
	void badPasswordIncrementsFailedAttempts() {
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(user(true));

		assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "wrong")))
				.isInstanceOf(BadCredentialsException.class);
		verify(userDetailsDao).updateFailAttempts("alice");
		verify(userDetailsDao, never()).resetFailAttempts(anyString());
	}

	@Test
	void reachingMaxAttemptsPropagatesLockedException() {
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(user(true));
		doThrow(new LockedException("User Account is locked!")).when(userDetailsDao).updateFailAttempts("alice");

		assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "wrong")))
				.isInstanceOf(LockedException.class).hasMessage("User Account is locked!");
	}

	@Test
	void lockedAccountReportsLastAttempt() {
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(user(false));
		UserAttempts attempts = new UserAttempts();
		attempts.setUsername("alice");
		attempts.setAttempts(3);
		Date last = new Date(0);
		attempts.setLastModified(last);
		when(userDetailsDao.getUserAttempts("alice")).thenReturn(attempts);

		assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret")))
				.isInstanceOf(LockedException.class)
				.hasMessage("User account is locked! Username : alice Last Attempts : " + last
						+ " Please contact the administrator for further assistance");
		verify(userDetailsDao, never()).resetFailAttempts(anyString());
	}

	@Test
	void lockedAccountWithoutAttemptRecordKeepsOriginalMessage() {
		when(userDetailsService.loadUserByUsername("alice")).thenReturn(user(false));
		when(userDetailsDao.getUserAttempts("alice")).thenReturn(null);

		assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret")))
				.isInstanceOf(LockedException.class).hasMessage("User account is locked");
	}
}
