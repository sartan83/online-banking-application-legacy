package org.thothlab.devilsvault.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.thothlab.devilsvault.support.TestDatabase;

@SpringJUnitWebConfig(SecurityTestConfig.class)
class SecurityConfigTest {

	@Autowired
	WebApplicationContext context;

	@Autowired
	@Qualifier("securitydataSource")
	DataSource dataSource;

	MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Nested
	class UnauthenticatedAccess {

		@ParameterizedTest
		@ValueSource(strings = { "/employee/home", "/employee/transaction", "/employee/systemlogs",
				"/customer/home", "/customer/transfer" })
		void protectedUrlsRedirectToLogin(String url) throws Exception {
			mvc.perform(get(url)).andExpect(status().isFound()).andExpect(redirectedUrlPattern("**/login"));
		}

		@ParameterizedTest
		@ValueSource(strings = { "/login", "/public/anything" })
		void publicUrlsArePermitted(String url) throws Exception {
			mvc.perform(get(url)).andExpect(status().isOk());
		}
	}

	@Nested
	class RoleBasedAccess {

		@ParameterizedTest(name = "{0} as {1} -> {2}")
		@CsvSource({
				"/employee/home, REGULAR, 200", "/employee/home, MANAGER, 200", "/employee/home, ADMIN, 200",
				"/employee/home, CUSTOMER, 403", "/employee/home, MERCHANT, 403",
				"/employee/externalregistrationform, REGULAR, 200",
				"/employee/externalregistrationform, MANAGER, 200",
				"/employee/externalregistrationform, ADMIN, 403",
				"/employee/externalregister, REGULAR, 200", "/employee/externalregister, ADMIN, 403",
				"/employee/internalregistrationform, ADMIN, 200",
				"/employee/internalregistrationform, MANAGER, 403",
				"/employee/internalregister, ADMIN, 200", "/employee/internalregister, REGULAR, 403",
				"/employee/transaction, MANAGER, 200", "/employee/transaction, REGULAR, 403",
				"/employee/transaction, ADMIN, 403",
				"/employee/systemlogs, ADMIN, 200", "/employee/systemlogs, MANAGER, 403",
				"/customer/home, CUSTOMER, 200", "/customer/home, MERCHANT, 200",
				"/customer/home, REGULAR, 403", "/customer/home, MANAGER, 403", "/customer/home, ADMIN, 403" })
		void urlRules(String url, String role, int expectedStatus) throws Exception {
			mvc.perform(get(url).with(user("someone").roles(role))).andExpect(status().is(expectedStatus));
		}
	}

	@Nested
	class FormLogin {

		@Test
		void customerLoginUsesLegacyProcessingUrlAndSuccessHandler() throws Exception {
			MvcResult result = mvc
					.perform(formLogin("/j_spring_security_check").user(TestDatabase.CUSTOMER)
							.password(TestDatabase.PASSWORD))
					.andExpect(authenticated().withUsername(TestDatabase.CUSTOMER).withRoles("CUSTOMER"))
					.andExpect(redirectedUrl("/CSE545-SS/customer/home")).andReturn();
			MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
			assertThat(session.getAttribute("userID")).isEqualTo(10);
			assertThat(session.getAttribute("role")).isEqualTo("ROLE_CUSTOMER");
		}

		@Test
		void employeeLoginRedirectsToEmployeeHome() throws Exception {
			mvc.perform(formLogin("/j_spring_security_check").user(TestDatabase.MANAGER)
					.password(TestDatabase.PASSWORD))
					.andExpect(authenticated().withRoles("MANAGER"))
					.andExpect(redirectedUrl("/CSE545-SS/employee/home"));
		}

		@Test
		void wrongPasswordRedirectsToLoginError() throws Exception {
			mvc.perform(formLogin("/j_spring_security_check").user(TestDatabase.MERCHANT).password("nope"))
					.andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error="));
		}

		@Test
		void loginWithoutCsrfTokenIsRejected() throws Exception {
			// missing token is routed to the configured invalid-session strategy (redirect to the login page)
			mvc.perform(post("/j_spring_security_check").param("username", TestDatabase.MERCHANT)
					.param("password", TestDatabase.PASSWORD)).andExpect(unauthenticated())
					.andExpect(redirectedUrl("/login"));
			// wrong token is a plain 403
			mvc.perform(post("/j_spring_security_check").with(csrf().useInvalidToken())
					.param("username", TestDatabase.MERCHANT).param("password", TestDatabase.PASSWORD))
					.andExpect(unauthenticated()).andExpect(status().isForbidden());
		}

		@Test
		void threeFailedLoginsLockTheAccount() throws Exception {
			for (int i = 0; i < 2; i++) {
				mvc.perform(formLogin("/j_spring_security_check").user(TestDatabase.LOCKOUT).password("bad"))
						.andExpect(redirectedUrl("/login?error="));
			}
			MvcResult third = mvc
					.perform(formLogin("/j_spring_security_check").user(TestDatabase.LOCKOUT).password("bad"))
					.andExpect(redirectedUrl("/login?error=")).andReturn();
			assertThat(lastException(third)).isInstanceOf(LockedException.class);

			MvcResult correctPassword = mvc
					.perform(formLogin("/j_spring_security_check").user(TestDatabase.LOCKOUT)
							.password(TestDatabase.PASSWORD))
					.andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=")).andReturn();
			assertThat(lastException(correctPassword)).isInstanceOf(LockedException.class)
					.hasMessageStartingWith("User account is locked! Username : " + TestDatabase.LOCKOUT);
			assertThat(new JdbcTemplate(dataSource).queryForObject(
					"SELECT accountNonLocked FROM users WHERE username = ?", Integer.class, TestDatabase.LOCKOUT))
					.isZero();
		}

		@Test
		void secondConcurrentLoginIsRejected() throws Exception {
			mvc.perform(formLogin("/j_spring_security_check").user(TestDatabase.SESSION)
					.password(TestDatabase.PASSWORD)).andExpect(authenticated());

			MvcResult second = mvc
					.perform(formLogin("/j_spring_security_check").user(TestDatabase.SESSION)
							.password(TestDatabase.PASSWORD))
					.andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error=")).andReturn();
			assertThat(lastException(second)).isInstanceOf(SessionAuthenticationException.class);
		}

		private Throwable lastException(MvcResult result) {
			return (Throwable) result.getRequest().getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		}
	}

	@Test
	void logoutRedirectsToLogin() throws Exception {
		mvc.perform(post("/logout").with(user("someone").roles("CUSTOMER")).with(csrf()))
				.andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
	}
}
