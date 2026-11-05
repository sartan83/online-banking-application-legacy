package org.thothlab.devilsvault.test.flow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.thothlab.devilsvault.test.support.MongoContainerSupport;
import org.thothlab.devilsvault.test.support.TestData;

@SpringBootTest
@AutoConfigureMockMvc
abstract class FlowTestSupport extends MongoContainerSupport {

	@Autowired
	protected MockMvc mvc;
	@Autowired
	protected MongoTemplate mongo;
	@Autowired
	SessionRegistry sessionRegistry;

	@BeforeEach
	void resetData() {
		TestData.load(mongo);
		// MockMvc never fires HttpSessionDestroyedEvent, so forget sessions of the previous test (max-sessions=1)
		for (Object principal : sessionRegistry.getAllPrincipals()) {
			sessionRegistry.getAllSessions(principal, true)
					.forEach(s -> sessionRegistry.removeSessionInformation(s.getSessionId()));
		}
	}

	protected MvcResult loginResult(String username, String password) throws Exception {
		return mvc.perform(formLogin("/j_spring_security_check").user(username).password(password)).andReturn();
	}

	protected MockHttpSession login(String username) throws Exception {
		MvcResult result = loginResult(username, TestData.PASSWORD);
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
