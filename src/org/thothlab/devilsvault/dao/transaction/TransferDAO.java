package org.thothlab.devilsvault.dao.transaction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.db.model.UserAuthentication;

@Repository("transferDAO")
public class TransferDAO {

	private final MongoTemplate mongoTemplate;

	public TransferDAO(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public String getUserNames(int userID) {
		return customer(userID).getName();
	}

	public void getPayerAccounts(int accountNumber, List<String> userAccounts) {
		userAccounts.add(accountNumber + ":" + account(accountNumber).getAccount_type());
	}

	public List<Integer> getMultipleAccounts(Integer payerID) {
		return accountNumbers(Query.query(where("external_users_id").is(payerID).and("account_type").ne("CREDIT")));
	}

	/** Payee accounts (Transfer to): every account of users this payer already sent external transfers to. */
	public void getRelatedAccounts(int payerID, List<String> populatedPayeeAccounts) {
		Query query = Query.query(where("stage").is(MongoCollections.COMPLETED).and("transaction_type")
				.is("externalFundTfr").and("payer_id").is(payerID));
		Set<Integer> relatedAccounts = new LinkedHashSet<>();
		for (Transaction transaction : mongoTemplate.find(query, Transaction.class)) {
			relatedAccounts.add(transaction.getPayee_id());
		}
		for (Integer listAccountElement : relatedAccounts) {
			int external_user_id = account(listAccountElement).getExternal_user_id();
			String userName = getUserNames(external_user_id);
			String username_account = userName + ":" + listAccountElement;
			if (populatedPayeeAccounts.contains(username_account))
				continue;
			for (Integer bankAccountElements : accountNumbers(Query.query(where("external_users_id").is(external_user_id)))) {
				populatedPayeeAccounts.add(userName + ":" + bankAccountElements);
			}
		}
	}

	public boolean validateAmount(int payerAccountNumber, BigDecimal amount) {
		BankAccountDB ba = account(payerAccountNumber);
		if (amount.compareTo((ba.getBalance().subtract(ba.getHold()))) == 1
				|| amount.compareTo(new BigDecimal("0")) == -1) {
			return false;
		}
		return true;
	}

	public int fetchAccountNumber(String modeOfTransfer, String inputMode) {
		try {
			Query byContact;
			if (modeOfTransfer.equalsIgnoreCase("EMAIL")) {
				byContact = Query.query(where("email").is(inputMode));
			} else {
				byContact = Query.query(where("phone").is(new BigInteger(inputMode.trim())));
			}
			Customer customer = mongoTemplate.findOne(byContact, Customer.class);
			if (customer == null) {
				return -1;
			}
			return accountNumbers(Query.query(where("external_users_id").is(customer.getId()).and("account_type")
					.is("CHECKING"))).get(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public boolean checkAccountExists(int account_number) {
		return mongoTemplate.exists(Query.query(where("_id").is(account_number)), BankAccountDB.class);
	}

	public void updateHold(int payerAccountNumber, BigDecimal amount) {
		mongoTemplate.updateFirst(Query.query(where("_id").is(payerAccountNumber)), new Update().inc("hold", amount),
				BankAccountDB.class);
	}

	/** Customers that authorised the given merchant ({@code authorized_merchants_users}). */
	public List<String> getUserAccounts(int merchantID) {
		List<String> userAccounts = new ArrayList<>();
		for (Customer customer : mongoTemplate.find(Query.query(where("authorized_merchants").is(merchantID)),
				Customer.class)) {
			userAccounts.add(customer.getName() + ":" + customer.getId());
		}
		return userAccounts;
	}

	public HashMap<String, String> processPayment(int merchantID, int sentAmount, int userID) {
		HashMap<String, String> sendToController = new HashMap<>();
		BankAccountDB credit = mongoTemplate.findOne(
				Query.query(where("external_users_id").is(userID).and("account_type").is("CREDIT")), BankAccountDB.class);
		int accountNumber = credit == null ? -1 : credit.getAccount_number();
		sendToController.put("userAccount", "" + accountNumber);
		int minimumBalance = -1;
		if (credit != null && credit.getCredit_card() != null) {
			minimumBalance = value(credit.getCredit_card().getCredit_limit())
					- value(credit.getCredit_card().getAvailable_balance());
		}
		if (sentAmount > minimumBalance) {
			sendToController.put("accepted", "-1");
		} else {
			sendToController.put("accepted", "1");
		}
		List<Integer> merchantAccounts = accountNumbers(Query.query(
				where("external_users_id").is(merchantID).and("account_type").in("CHECKINGS", "CHECKING")));
		sendToController.put("merchantCheckingAccount", merchantAccounts.isEmpty() ? "-1" : "" + merchantAccounts.get(0));
		return sendToController;
	}

	public List<String> getNewMerchantAccounts(int userID) {
		List<Integer> authorized = authorizedMerchants(userID);
		List<String> merchantAccounts = new ArrayList<>();
		for (Customer merchant : merchants()) {
			if (!authorized.contains(merchant.getId())) {
				merchantAccounts.add(merchant.getName() + ":" + merchant.getId());
			}
		}
		return merchantAccounts;
	}

	public boolean addMerchantToUser(int merchantID, int userID) {
		return mongoTemplate.updateFirst(Query.query(where("_id").is(userID)),
				new Update().addToSet("authorized_merchants", merchantID), Customer.class).getMatchedCount() == 1;
	}

	public boolean deleteMerchantConnection(int userID, int merchantID) {
		return mongoTemplate.updateFirst(Query.query(where("_id").is(userID).and("authorized_merchants").is(merchantID)),
				new Update().pull("authorized_merchants", merchantID), Customer.class).getModifiedCount() == 1;
	}

	public List<String> getExistingMerchantAccounts(int userID) {
		List<Integer> authorized = authorizedMerchants(userID);
		List<String> merchantAccounts = new ArrayList<>();
		for (Customer merchant : merchants()) {
			if (authorized.contains(merchant.getId())) {
				merchantAccounts.add(merchant.getName() + ":" + merchant.getId());
			}
		}
		return merchantAccounts;
	}

	public Boolean updateAvailableBalance(BigDecimal amount, int payerAccount) {
		int delta = amount.setScale(0, RoundingMode.HALF_UP).intValue();
		return mongoTemplate.updateFirst(Query.query(where("_id").is(payerAccount).and("credit_card").exists(true)),
				new Update().inc("credit_card.available_balance", delta), BankAccountDB.class).getModifiedCount() == 1;
	}

	private List<Customer> merchants() {
		List<String> merchantEmails = new ArrayList<>();
		for (UserAuthentication user : mongoTemplate.find(Query.query(where("role").is("ROLE_MERCHANT")),
				UserAuthentication.class)) {
			merchantEmails.add(user.getUsername());
		}
		return mongoTemplate.find(Query.query(where("email").in(merchantEmails)), Customer.class);
	}

	private List<Integer> authorizedMerchants(int userID) {
		Customer customer = mongoTemplate.findById(userID, Customer.class);
		return customer == null || customer.getAuthorized_merchants() == null ? List.of()
				: customer.getAuthorized_merchants();
	}

	private Customer customer(int id) {
		Customer customer = mongoTemplate.findById(id, Customer.class);
		if (customer == null) {
			throw new IndexOutOfBoundsException("No external user " + id);
		}
		return customer;
	}

	private BankAccountDB account(int accountNumber) {
		BankAccountDB account = mongoTemplate.findById(accountNumber, BankAccountDB.class);
		if (account == null) {
			throw new IndexOutOfBoundsException("No bank account " + accountNumber);
		}
		return account;
	}

	private List<Integer> accountNumbers(Query query) {
		List<Integer> numbers = new ArrayList<>();
		for (BankAccountDB account : mongoTemplate.find(query, BankAccountDB.class)) {
			numbers.add(account.getAccount_number());
		}
		return numbers;
	}

	private static int value(Integer v) {
		return v == null ? 0 : v;
	}
}
