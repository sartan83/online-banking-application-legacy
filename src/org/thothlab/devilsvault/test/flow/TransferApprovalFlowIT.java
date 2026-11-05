package org.thothlab.devilsvault.test.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockHttpSession;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.test.support.TestData;

class TransferApprovalFlowIT extends FlowTestSupport {

	private Transaction customerTransfersToBob(String amount) throws Exception {
		MockHttpSession alice = login("alice@devilsvault.com");
		mvc.perform(get("/customer/externalFundTransfer").session(alice))
				.andExpect(model().attribute("payeeAccounts", hasItem("Bob Bianchi:1004")))
				.andExpect(model().attribute("userAccounts", hasItem("1001:CHECKING")));
		mvc.perform(post("/customer/ExternalTransferSubmit").session(alice).with(csrf())
				.param("etpselectPayerAccount", "1001:CHECKING").param("etpselectPayeeAccount", "Bob Bianchi:1004")
				.param("etpinputAmount", amount).param("etpTextArea", "Rent share"))
				.andExpect(view().name("customerPages/transferConfirmation"))
				.andExpect(model().attribute("success", true));
		List<Transaction> pending = mongo.find(Query.query(where("stage").is("pending")), Transaction.class);
		assertThat(pending).hasSize(1);
		assertThat(TestData.hold(mongo, 1001)).isEqualByComparingTo(amount);
		return pending.get(0);
	}

	@Test
	void customerTransferIsApprovedByManager() throws Exception {
		Transaction pending = customerTransfersToBob("250");
		assertThat(pending.getId()).isEqualTo(2);
		assertThat(pending.isCritical()).isFalse();

		MockHttpSession regular = login("regular@devilsvault.com");
		mvc.perform(get("/employee/home").session(regular)).andExpect(model().attribute("transaction_count", 1));

		MockHttpSession manager = login("manager@devilsvault.com");
		mvc.perform(get("/employee/transaction").session(manager))
				.andExpect(model().attribute("pending_list", hasSize(1)));
		mvc.perform(post("/employee/processtransaction").session(manager).with(csrf())
				.param("transactionID", "" + pending.getId()).param("requestType", "approve").param("extUserID", "1"))
				.andExpect(redirectedUrl("/employee/transaction")).andExpect(flash().attribute("success", true));

		assertThat(TestData.balance(mongo, 1001)).isEqualByComparingTo("4750");
		assertThat(TestData.balance(mongo, 1004)).isEqualByComparingTo("1450");
		assertThat(TestData.hold(mongo, 1001)).isEqualByComparingTo("0");
		Transaction completed = mongo.findOne(Query.query(where("stage").is("completed").and("id").is(pending.getId())),
				Transaction.class);
		assertThat(completed.getStatus()).isEqualTo("approve");
		assertThat(completed.getApprover()).isEqualTo("2");
		assertThat(mongo.count(Query.query(where("stage").is("pending")), Transaction.class)).isZero();
	}

	@Test
	void rejectedTransferReleasesTheHold() throws Exception {
		Transaction pending = customerTransfersToBob("100");
		MockHttpSession manager = login("manager@devilsvault.com");
		mvc.perform(post("/employee/processtransaction").session(manager).with(csrf())
				.param("transactionID", "" + pending.getId()).param("requestType", "reject").param("extUserID", "1"))
				.andExpect(redirectedUrl("/employee/transaction"));
		assertThat(TestData.balance(mongo, 1001)).isEqualByComparingTo("5000");
		assertThat(TestData.hold(mongo, 1001)).isEqualByComparingTo("0");
	}

	@Test
	void overdraftIsRefused() throws Exception {
		MockHttpSession alice = login("alice@devilsvault.com");
		mvc.perform(get("/customer/externalFundTransfer").session(alice));
		mvc.perform(post("/customer/ExternalTransferSubmit").session(alice).with(csrf())
				.param("etpselectPayerAccount", "1002:SAVINGS").param("etpselectPayeeAccount", "Bob Bianchi:1004")
				.param("etpinputAmount", "2600").param("etpTextArea", "too much"))
				.andExpect(model().attribute("error_msg", "Insufficient balance!"));
	}
}
