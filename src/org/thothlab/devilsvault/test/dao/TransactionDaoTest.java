package org.thothlab.devilsvault.test.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.thothlab.devilsvault.dao.customer.CustomerAccountsDAO;
import org.thothlab.devilsvault.dao.customer.CustomerDAO;
import org.thothlab.devilsvault.dao.transaction.InternalTransactionDaoImpl;
import org.thothlab.devilsvault.dao.transaction.TransactionDaoImpl;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.test.support.DaoTestConfig;
import org.thothlab.devilsvault.test.support.MongoContainerSupport;
import org.thothlab.devilsvault.test.support.TestData;

@DataMongoTest
@Import(DaoTestConfig.class)
class TransactionDaoTest extends MongoContainerSupport {

	@Autowired
	MongoTemplate mongo;
	@Autowired
	@Qualifier("TransactionDao")
	TransactionDaoImpl transactionDao;
	@Autowired
	InternalTransactionDaoImpl internalTransactionDao;
	@Autowired
	CustomerAccountsDAO customerAccountsDAO;
	@Autowired
	CustomerDAO customerDAO;

	@BeforeEach
	void seed() {
		TestData.load(mongo);
	}

	private Transaction transfer(String amount) throws Exception {
		return transactionDao.createExternalTransaction(1001, new BigDecimal(amount), 1004, "test", "externalFundTfr");
	}

	@Test
	void savePendingAssignsSharedSequenceIdAndCriticalFlag() throws Exception {
		Transaction small = transfer("250");
		Transaction big = transfer("1500");
		assertThat(transactionDao.save(small, "transaction_pending")).isTrue();
		assertThat(transactionDao.save(big, "transaction_pending")).isTrue();

		assertThat(small.getId()).isEqualTo(2);
		assertThat(big.getId()).isEqualTo(3);
		assertThat(big.isCritical()).isTrue();
		assertThat(transactionDao.getById(2, "transaction_pending")).singleElement()
				.satisfies(t -> assertThat(t.getAmount()).isEqualByComparingTo("250"));
		assertThat(internalTransactionDao.getAllPendingTransactionByAccountNo(List.of(1001)))
				.extracting(Transaction::getId).containsExactly(2);
	}

	@Test
	void approvalMovesPendingToCompletedKeepingIdAndUpdatesBalances() throws Exception {
		Transaction t = transfer("250");
		transactionDao.save(t, "transaction_pending");
		internalTransactionDao.updateHold(1001, new BigDecimal("-250"));
		assertThat(TestData.hold(mongo, 1001)).isEqualByComparingTo("250");

		t.setStatus("approve");
		assertThat(transactionDao.saveToCompleted(t, "transaction_completed")).isTrue();
		transactionDao.updatePayerBalance(1001, t.getAmount());
		transactionDao.updatePayeeBalance(1004, t.getAmount());
		internalTransactionDao.updateHold(1001, t.getAmount());
		assertThat(transactionDao.deleteById(t.getId(), "transaction_pending")).isTrue();

		assertThat(internalTransactionDao.getAllPending()).isEmpty();
		assertThat(internalTransactionDao.getTransactionById(t.getId(), "transaction_completed")).singleElement()
				.satisfies(c -> assertThat(c.getStatus()).isEqualTo("approve"));
		assertThat(TestData.balance(mongo, 1001)).isEqualByComparingTo("4750");
		assertThat(TestData.balance(mongo, 1004)).isEqualByComparingTo("1450");
		assertThat(TestData.hold(mongo, 1001)).isEqualByComparingTo("0");
	}

	@Test
	void approveAndRejectTransactionHelpers() throws Exception {
		Transaction a = transfer("10");
		Transaction b = transfer("20");
		internalTransactionDao.save(a, "transaction_pending");
		internalTransactionDao.save(b, "transaction_pending");
		internalTransactionDao.approveTransaction(a.getId(), "transaction_pending");
		internalTransactionDao.rejectTransaction(b.getId(), "transaction_pending");
		assertThat(internalTransactionDao.getAllPending()).isEmpty();
		assertThat(internalTransactionDao.getAllCompleted()).extracting(Transaction::getStatus)
				.containsExactlyInAnyOrder("approve", "approved", "rejected");
		assertThat(internalTransactionDao.getByUserId(1001, "transaction_completed")).hasSize(3);
	}

	@Test
	void withdrawMoneyIsAtomicPendingPlusHold() {
		BankAccountDB account = mongo.findById(1002, BankAccountDB.class);
		Transaction t = new Transaction();
		assertThat(customerAccountsDAO.withdrawMoney(customerDAO.getCustomer(1), account, t, new BigDecimal("100"),
				"SAVINGS")).isTrue();
		assertThat(t.getId()).isPositive();
		assertThat(TestData.hold(mongo, 1002)).isEqualByComparingTo("100");
		assertThat(mongo.count(Query.query(where("stage").is("pending").and("transaction_type").is("debitfunds")),
				Transaction.class)).isEqualTo(1);
		assertThat(customerAccountsDAO.getTransactionLines(1002, 1)).hasSize(1);
	}

	@Test
	void creditCardUpdatesTargetEmbeddedCard() {
		internalTransactionDao.updateCC_Payment(30, 1003);
		internalTransactionDao.updateCC_AvailableBalance(-50, 1003);
		BankAccountDB credit = mongo.findById(1003, BankAccountDB.class);
		assertThat(credit.getCredit_card().getPayment()).isEqualTo(30);
		assertThat(credit.getCredit_card().getAvailable_balance()).isEqualTo(50);
	}
}
