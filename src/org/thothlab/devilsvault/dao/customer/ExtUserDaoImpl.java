package org.thothlab.devilsvault.dao.customer;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.BankAccountExternal;
import org.thothlab.devilsvault.db.model.Customer;

@Repository("ExtUserDaoImpl")
public class ExtUserDaoImpl {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;

	public ExtUserDaoImpl(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	private List<BankAccountDB> accounts(Customer user, String accountType) {
		return mongoTemplate.find(
				Query.query(where("external_users_id").is(user.getId()).and("account_type").is(accountType)),
				BankAccountDB.class);
	}

	/** Available balance is reported as balance - hold, as in the legacy DAO. */
	public BankAccountDB getAccount(Customer user, BankAccountDB bankAccount, String accountType) {
		for (BankAccountDB row : accounts(user, accountType)) {
			bankAccount.setAccount_number(row.getAccount_number());
			bankAccount.setAccount_type(row.getAccount_type());
			bankAccount.setHold(row.getHold());
			bankAccount.setBalance(row.getBalance().subtract(row.getHold()));
			bankAccount.setExternal_user_id(row.getExternal_user_id());
		}
		return bankAccount;
	}

	public Double getSavingsBalance(Customer user) {
		Double balance = 0.0d;
		for (BankAccountDB row : accounts(user, "SAVINGS")) {
			balance = row.getBalance().doubleValue();
		}
		return balance;
	}

	public Double getCheckingBalance(Customer user) {
		Double balance = 0.0d;
		for (BankAccountDB row : accounts(user, "CHECKINGS")) {
			balance = row.getBalance().doubleValue();
		}
		return balance;
	}

	public Customer setExternalUser(String name, String address, BigInteger phone, String email, String date_of_birth,
			String ssn) {
		Customer userDetails = new Customer();
		userDetails.setName(name);
		userDetails.setAddress(address);
		userDetails.setDate_of_birth(date_of_birth);
		userDetails.setEmail(email);
		userDetails.setPhone(phone);
		userDetails.setSsn(ssn);
		return userDetails;
	}

	public Integer createUser(Customer userdetails) {
		if (userdetails.getId() == 0) {
			userdetails.setId(sequences.next("external_users"));
		}
		userdetails.setCity("sdsdsad");
		userdetails.setState("sdsdsad");
		userdetails.setCountry("sdsdsad");
		userdetails.setPincode(123);
		try {
			mongoTemplate.insert(userdetails);
		} catch (DuplicateKeyException e) {
			e.printStackTrace();
			return 0;
		}
		return mongoTemplate.find(Query.query(where("email").is(userdetails.getEmail())), Customer.class).get(0).getId();
	}

	public BankAccountDB getCreditCardBalance(BankAccountDB account) {
		BankAccountDB row = mongoTemplate.findById(account.getAccount_number(), BankAccountDB.class);
		if (row != null && row.getCredit_card() != null && row.getCredit_card().getAvailable_balance() != null) {
			account.setBalance(new BigDecimal(row.getCredit_card().getAvailable_balance()));
		}
		return account;
	}

	public BankAccountExternal getAccount(Customer user, BankAccountExternal bankAccount, String accountType) {
		for (BankAccountDB row : accounts(user, accountType)) {
			bankAccount.setAccount_number(row.getAccount_number());
			bankAccount.setAccount_type(row.getAccount_type());
			bankAccount.setBalance(row.getBalance().floatValue());
			bankAccount.setExternal_users_id(row.getExternal_user_id());
		}
		return bankAccount;
	}
}
