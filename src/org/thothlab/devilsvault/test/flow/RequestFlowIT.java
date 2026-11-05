package org.thothlab.devilsvault.test.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.thothlab.devilsvault.db.model.Customer;

class RequestFlowIT extends FlowTestSupport {

	@Test
	void customerChangeRequestIsApprovedByEmployee() throws Exception {
		MockHttpSession alice = login("alice@devilsvault.com");
		mvc.perform(post("/customer/addrequest").session(alice).with(csrf()).param("requestType", "address")
				.param("newValue", "5 New St")).andExpect(redirectedUrl("/customer/userdetails"))
				.andExpect(flash().attribute("error_msg", "Request raised!!"));
		mvc.perform(post("/customer/addrequest").session(alice).with(csrf()).param("requestType", "address")
				.param("newValue", "6 New St")).andExpect(flash().attribute("error_msg", "Request denied as already exists!!"));

		MockHttpSession regular = login("regular@devilsvault.com");
		mvc.perform(get("/employee/pendingrequest").session(regular))
				.andExpect(model().attribute("external_list", hasSize(2)));
		mvc.perform(post("/employee/processrequestexternal").session(regular).with(csrf()).param("requestID", "2")
				.param("action", "approve")).andExpect(redirectedUrl("/employee/pendingrequest"))
				.andExpect(flash().attribute("error_msg", "Request Approved!"));
		mvc.perform(post("/employee/processrequestexternal").session(regular).with(csrf()).param("requestID", "1")
				.param("action", "reject")).andExpect(flash().attribute("error_msg", "Request Rejected!"));

		assertThat(mongo.findById(1, Customer.class).getAddress()).isEqualTo("5 New St");
		assertThat(mongo.findById(2, Customer.class).getAddress()).isEqualTo("2 Palm Walk");
		mvc.perform(get("/employee/completedrequest").session(regular))
				.andExpect(model().attribute("external_list", hasSize(2)));
	}
}
