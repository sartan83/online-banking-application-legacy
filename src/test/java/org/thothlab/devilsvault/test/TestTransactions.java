package org.thothlab.devilsvault.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.thothlab.devilsvault.dao.transaction.InternalTransactionDaoImpl;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.support.TestDatabase;

/** Ported to JUnit 5; runs the production DaoDetails.xml against the H2 test database. */
class TestTransactions {

	private ClassPathXmlApplicationContext ctx;
	private InternalTransactionDaoImpl transactionDAO;

	@BeforeEach
	void setUp() {
		TestDatabase.reset(TestDatabase.dataSource());
		ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		transactionDAO = ctx.getBean("TransactionSpecificDao", InternalTransactionDaoImpl.class);
	}

	@AfterEach
	void tearDown() {
		ctx.close();
	}

	@Test
	void testCreate() {
		assertThat(transactionDAO.createTransaction()).isEqualTo(1);
		assertThat(transactionDAO.getAllPending()).hasSize(2);
	}

	@Test
	void testgetAllPendingTransactions() {
		assertThat(transactionDAO.getAllPending()).singleElement()
				.satisfies(t -> assertThat(t.getAmount()).isEqualByComparingTo("25.00"));
	}

	@Test
	void testgetAllCompletedTransactions() {
		assertThat(transactionDAO.getAllCompleted()).isEmpty();
	}

	@Test
	void testgetByUserId() {
		assertThat(transactionDAO.getByUserId(11, "transaction_pending")).singleElement()
				.extracting(Transaction::getPayer_id).isEqualTo(10);
		assertThat(transactionDAO.getByUserId(99, "transaction_pending")).isEmpty();
	}
}
