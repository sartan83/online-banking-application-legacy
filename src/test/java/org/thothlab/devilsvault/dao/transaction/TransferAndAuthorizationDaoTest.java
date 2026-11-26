package org.thothlab.devilsvault.dao.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.thothlab.devilsvault.dao.authorization.AthorizationDaoImpl;
import org.thothlab.devilsvault.db.model.Authorization;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.support.TestDatabase;

/** Transfers, balances/holds, merchant links and employee access authorizations against H2. */
class TransferAndAuthorizationDaoTest {

	private ClassPathXmlApplicationContext ctx;
	private TransferDAO transfer;
	private TransactionDaoImpl transactions;
	private AthorizationDaoImpl authorizations;
	private JdbcTemplate jdbc;

	@BeforeEach
	void setUp() {
		TestDatabase.reset(TestDatabase.dataSource());
		ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		transfer = ctx.getBean("transferDAO", TransferDAO.class);
		transactions = ctx.getBean("TransactionDao", TransactionDaoImpl.class);
		authorizations = ctx.getBean("AuthorizationDao", AthorizationDaoImpl.class);
		jdbc = new JdbcTemplate(TestDatabase.dataSource());
	}

	@AfterEach
	void tearDown() {
		ctx.close();
	}

	private BigDecimal balance(int account) {
		return jdbc.queryForObject("select balance from bank_accounts where account_number = ?", BigDecimal.class, account);
	}

	private BigDecimal hold(int account) {
		return jdbc.queryForObject("select hold from bank_accounts where account_number = ?", BigDecimal.class, account);
	}

	@Test
	void validateAmountUsesBalanceMinusHoldAndRejectsNegative() {
		assertThat(transfer.validateAmount(100011, new BigDecimal("2400.00"))).isTrue();
		assertThat(transfer.validateAmount(100011, new BigDecimal("2400.01"))).isFalse();
		assertThat(transfer.validateAmount(100011, new BigDecimal("-1"))).isFalse();
	}

	@Test
	void updateHoldReservesFunds() {
		transfer.updateHold(100010, new BigDecimal("750.00"));
		assertThat(hold(100010)).isEqualByComparingTo("750.00");
		assertThat(transfer.validateAmount(100010, new BigDecimal("4300"))).isFalse();
	}

	@Test
	void accountLookups() {
		assertThat(transfer.checkAccountExists(100010)).isTrue();
		assertThat(transfer.checkAccountExists(999999)).isFalse();
		assertThat(transfer.getMultipleAccounts(10)).containsExactlyInAnyOrder(100010, 100011);
		assertThat(transfer.getUserNames(10)).isEqualTo("Customer One");
	}

	@Test
	void externalTransactionAbove1000IsCritical() throws Exception {
		Transaction small = transactions.createExternalTransaction(100010, new BigDecimal("1000"), 100021, "rent", "debit");
		Transaction big = transactions.createExternalTransaction(100010, new BigDecimal("1000.01"), 100021, "car", "debit");

		assertThat(small.isCritical()).isFalse();
		assertThat(big.isCritical()).isTrue();
		assertThat(big.getStatus()).isEqualTo("pending");
	}

	@Test
	void approveTransactionMovesItToCompletedAndUpdatesBalances() throws Exception {
		Transaction t = transactions.createExternalTransaction(100010, new BigDecimal("300.00"), 100021, "invoice", "debit");
		assertThat(transactions.save(t, "transaction_pending")).isTrue();
		assertThat(t.getId()).isPositive();
		assertThat(transactions.getById(t.getId(), "transaction_pending")).hasSize(1);

		t.setStatus("approved");
		assertThat(transactions.saveToCompleted(t, "transaction_completed")).isTrue();
		assertThat(transactions.deleteById(t.getId(), "transaction_pending")).isTrue();
		transactions.updatePayerBalance(100010, t.getAmount());
		transactions.updatePayeeBalance(100021, t.getAmount());

		assertThat(transactions.getById(t.getId(), "transaction_pending")).isEmpty();
		assertThat(transactions.getById(t.getId(), "transaction_completed")).singleElement()
				.extracting(Transaction::getStatus).isEqualTo("approved");
		assertThat(balance(100010)).isEqualByComparingTo("4700.00");
		assertThat(balance(100021)).isEqualByComparingTo("1100.00");
	}

	@Test
	void merchantLinkCanBeAddedAndRemoved() throws Exception {
		assertThat(transfer.addMerchantToUser(11, 10)).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from authorized_merchants_users where merchant_id = 11 and external_user_id = 10", Integer.class)).isEqualTo(1);
		assertThat(transfer.deleteMerchantConnection(10, 11)).isTrue();
		assertThat(transfer.deleteMerchantConnection(10, 11)).isFalse();
	}

	@Test
	void employeeAuthorizationRequestLifecycle() {
		assertThat(authorizations.isExist(1, 10, "viewAccount")).isTrue();
		assertThat(authorizations.isExist(3, 10, "viewAccount")).isFalse();

		assertThat(authorizations.save(3, 10, "viewAccount")).isTrue();
		assertThat(authorizations.isExist(3, 10, "viewAccount")).isTrue();
		Authorization pending = authorizations.getAllPendingAuthorization(3).get(0);
		assertThat(authorizations.getAllPendingAuthorizationManager()).hasSize(2);

		assertThat(authorizations.addByID(pending)).isTrue();
		assertThat(authorizations.deleteByID(pending.getAuth_id(), "authorization_pending")).isTrue();

		assertThat(authorizations.getAllPendingAuthorization(3)).isEmpty();
		assertThat(authorizations.getAllCompleteAuthorization(3)).singleElement()
				.extracting(Authorization::getExternal_userID).isEqualTo(10);
	}
}
