package org.thothlab.devilsvault.test.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MvcResult;
import org.thothlab.devilsvault.db.model.UserAuthentication;

class LoginFlowIT extends FlowTestSupport {

	@Test
	void customerLoginLandsOnCustomerHomeWithSessionAttributes() throws Exception {
		MvcResult result = loginResult("alice@devilsvault.com", "Passw0rd@");
		assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/customer/home");
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session.getAttribute("userID")).isEqualTo(1);
		assertThat(session.getAttribute("role")).isEqualTo("ROLE_CUSTOMER");
		mvc.perform(get("/customer/home").session(session)).andExpect(status().isOk());
		mvc.perform(get("/employee/home").session(session)).andExpect(status().isForbidden());
	}

	@Test
	void employeesLandOnEmployeeHome() throws Exception {
		for (String user : new String[] { "regular@devilsvault.com", "manager@devilsvault.com", "admin@devilsvault.com" }) {
			assertThat(loginResult(user, "Passw0rd@").getResponse().getRedirectedUrl()).isEqualTo("/employee/home");
		}
		MockHttpSession regular = (MockHttpSession) loginResult("bob@devilsvault.com", "Passw0rd@").getRequest()
				.getSession(false);
		mvc.perform(get("/employee/systemlogs").session(regular)).andExpect(status().isForbidden());
	}

	@Test
	void anonymousUserIsSentToLogin() throws Exception {
		mvc.perform(get("/customer/home")).andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	void threeWrongPasswordsLockTheAccount() throws Exception {
		for (int i = 0; i < 3; i++) {
			assertThat(loginResult("bob@devilsvault.com", "bad").getResponse().getRedirectedUrl())
					.isEqualTo("/login?error=");
		}
		assertThat(mongo.findById("bob@devilsvault.com", UserAuthentication.class).getAccountNonLocked()).isZero();
		MvcResult locked = loginResult("bob@devilsvault.com", "Passw0rd@");
		assertThat(locked.getResponse().getRedirectedUrl()).isEqualTo("/login?error=");
		Object error = locked.getRequest().getSession().getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
		assertThat(error).isInstanceOf(LockedException.class);
		assertThat(((LockedException) error).getMessage()).startsWith("User account is locked! Username : bob@");
	}

	@Test
	void logoutRedirectsToLogin() throws Exception {
		MockHttpSession session = login("alice@devilsvault.com");
		mvc.perform(get("/logout").session(session)).andExpect(redirectedUrl("/login"));
	}
}
