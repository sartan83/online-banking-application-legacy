package org.thothlab.devilsvault.test.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.thothlab.devilsvault.dao.authorization.AthorizationDaoImpl;
import org.thothlab.devilsvault.dao.bankaccount.BankAccountDaoImpl;
import org.thothlab.devilsvault.dao.creditcard.CreditCardDOA;
import org.thothlab.devilsvault.dao.customer.CreditCardDaoImpl;
import org.thothlab.devilsvault.dao.customer.CustomerDAO;
import org.thothlab.devilsvault.dao.customer.ExtUserDaoImpl;
import org.thothlab.devilsvault.dao.customer.InternalCustomerDAO;
import org.thothlab.devilsvault.dao.transaction.TransferDAO;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.CreditAccount;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.test.support.DaoTestConfig;
import org.thothlab.devilsvault.test.support.MongoContainerSupport;
import org.thothlab.devilsvault.test.support.TestData;

@DataMongoTest
@Import(DaoTestConfig.class)
class AccountsDaoTest extends MongoContainerSupport {

	@Autowired
	MongoTemplate mongo;
	@Autowired
	TransferDAO transferDAO;
	@Autowired
	ExtUserDaoImpl extUserDao;
	@Autowired
	CustomerDAO customerDAO;
	@Autowired
	InternalCustomerDAO internalCustomerDAO;
	@Autowired
	BankAccountDaoImpl bankAccountDao;
	@Autowired
	CreditCardDaoImpl creditCardDao;
	@Autowired
	CreditCardDOA creditCardDOA;
	@Autowired
	AthorizationDaoImpl authorizationDao;

	@BeforeEach
	void seed() {
		TestData.load(mongo);
	}

	@Test
	void transferLookups() {
		assertThat(transferDAO.getMultipleAccounts(1)).containsExactly(1001, 1002);
		List<String> payees = new ArrayList<>();
		transferDAO.getRelatedAccounts(1001, payees);
		assertThat(payees).containsExactly("Bob Bianchi:1004", "Bob Bianchi:1005", "Bob Bianchi:1006");
		assertThat(transferDAO.fetchAccountNumber("EMAIL", "bob@devilsvault.com")).isEqualTo(1004);
		assertThat(transferDAO.fetchAccountNumber("PHONE", "4805550102")).isEqualTo(1004);
		assertThat(transferDAO.fetchAccountNumber("EMAIL", "nobody@x.com")).isEqualTo(-1);
		assertThat(transferDAO.validateAmount(1001, new BigDecimal("5000"))).isTrue();
		assertThat(transferDAO.validateAmount(1001, new BigDecimal("5000.01"))).isFalse();
		assertThat(transferDAO.checkAccountExists(1009)).isTrue();
		assertThat(transferDAO.checkAccountExists(4242)).isFalse();
	}

	@Test
	void merchantAuthorisations() {
		assertThat(transferDAO.getExistingMerchantAccounts(1)).containsExactly("Sun Devil Shop:3");
		assertThat(transferDAO.getNewMerchantAccounts(2)).containsExactly("Sun Devil Shop:3");
		assertThat(transferDAO.addMerchantToUser(3, 2)).isTrue();
		assertThat(transferDAO.getUserAccounts(3)).containsExactlyInAnyOrder("Alice Rossi:1", "Bob Bianchi:2");
		assertThat(transferDAO.deleteMerchantConnection(1, 3)).isTrue();
		assertThat(transferDAO.getUserAccounts(3)).containsExactly("Bob Bianchi:2");
		HashMap<String, String> payment = transferDAO.processPayment(3, 100, 2);
		assertThat(payment).containsEntry("userAccount", "1006").containsEntry("accepted", "1")
				.containsEntry("merchantCheckingAccount", "1007");
		assertThat(transferDAO.processPayment(3, 900, 2)).containsEntry("accepted", "-1");
	}

	@Test
	void availableBalanceIsBalanceMinusHold() {
		transferDAO.updateHold(1001, new BigDecimal("1000"));
		BankAccountDB checking = extUserDao.getAccount(customerDAO.getCustomer(1), new BankAccountDB(), "CHECKING");
		assertThat(checking.getBalance()).isEqualByComparingTo("4000");
		assertThat(extUserDao.getSavingsBalance(customerDAO.getCustomer(1))).isEqualTo(2500.0);
	}

	@Test
	void newCustomerGetsThreeAccountsAndACard() {
		Customer dario = extUserDao.setExternalUser("Dario Neri", "4 Lemon St", BigInteger.valueOf(4805550105L),
				"dario@devilsvault.com", "1992-02-02", "890-12-3456");
		Integer id = extUserDao.createUser(dario);
		assertThat(id).isEqualTo(4);
		Integer credit = bankAccountDao.CreateAndGetCreditAccountNo(id);
		assertThat(credit).isEqualTo(1011);
		creditCardDao.createCreditAccount(credit);
		assertThat(bankAccountDao.getAccountDetailsById(id)).extracting(BankAccountDB::getAccount_type)
				.containsExactly("CHECKING", "CREDIT", "SAVINGS");
		CreditAccount card = creditCardDOA.getCreditAccount(customerDAO.getCustomer(id));
		assertThat(card.getCreditLimit()).isEqualTo(800);
		assertThat(card.getCredit_card_no().toString()).hasSize(16).startsWith("5");
		assertThat(creditCardDOA.getAccount(card.getCredit_card_no().toString(), "", "", "").getOwner().getEmail())
				.isEqualTo("dario@devilsvault.com");

		Document raw = mongo.getCollection("external_users").find(new Document("_id", id)).first();
		assertThat(raw.get("phone")).isInstanceOf(Long.class);
	}

	@Test
	void authorizationsAndCustomerDeletion() {
		assertThat(internalCustomerDAO.getExternalUserIds(1, "transaction")).containsExactly(1, 2);
		assertThat(internalCustomerDAO.getAccNos(List.of(1))).containsExactly(1001, 1002, 1003);
		assertThat(authorizationDao.isExist(1, 3, "transaction")).isFalse();
		authorizationDao.save(1, 3, "transaction");
		int pendingId = authorizationDao.getAllPendingAuthorizationManager().get(0).getAuth_id();
		assertThat(authorizationDao.addByID(authorizationDao.getByID(pendingId))).isTrue();
		assertThat(authorizationDao.deleteByID(pendingId, "authorization_pending")).isTrue();
		assertThat(authorizationDao.getAllCompleteAuthorization(1)).hasSize(3);

		internalCustomerDAO.deleteCustomer("2");
		assertThat(customerDAO.getCustomer(2)).isNull();
		assertThat(bankAccountDao.getAccountDetailsById(2)).isEmpty();
		assertThat(authorizationDao.getAllCompleteAuthorization(1)).hasSize(2);
	}
}
