package org.thothlab.devilsvault.dao.customer;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.BankAccount;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.Transaction;

@Repository("CustomerAccountsDAO")
public class CustomerAccountsDAO {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;
	private final TransactionTemplate transactionTemplate;

	public CustomerAccountsDAO(MongoTemplate mongoTemplate, SequenceGenerator sequences,
			MongoTransactionManager transactionManager) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	/** Pending then completed transactions of the account within the last {@code interval} months, newest first. */
	public List<Transaction> getTransactionLines(int account_num, int interval) {
		Date now = new Date();
		Date from = Date.from(ZonedDateTime.now().minusMonths(interval).toInstant());
		List<Transaction> TransactionLines = new ArrayList<Transaction>();
		for (String stage : new String[] { MongoCollections.PENDING, MongoCollections.COMPLETED }) {
			Criteria criteria = new Criteria().andOperator(where("stage").is(stage),
					new Criteria().orOperator(where("payer_id").is(account_num), where("payee_id").is(account_num)),
					where("timestamp_updated").gte(from).lte(now));
			for (Transaction row : mongoTemplate.find(
					Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "timestamp_updated")), Transaction.class)) {
				Transaction TransactionLine = new Transaction();
				TransactionLine.setDescription(row.getDescription());
				TransactionLine.setStatus(row.getStatus());
				TransactionLine.setApprover(row.getApprover());
				TransactionLine.setAmount(row.getAmount());
				TransactionLine.setPayee_id(row.getPayee_id());
				TransactionLine.setPayer_id(row.getPayer_id());
				TransactionLine.setTimestamp_updated(row.getTimestamp_updated());
				TransactionLines.add(TransactionLine);
			}
		}
		return TransactionLines;
	}

	public BankAccount getAccount(int accountNumber) {
		BankAccountDB row = mongoTemplate.findById(accountNumber, BankAccountDB.class);
		if (row == null) {
			throw new IndexOutOfBoundsException("No bank account " + accountNumber);
		}
		BankAccount account = new BankAccount();
		account.setAccountNumber(row.getAccount_number());
		account.setAccountType(accountType(row.getAccount_type()));
		account.setOwner(mongoTemplate.findById(row.getExternal_user_id(), Customer.class));
		return account;
	}

	public String getEmailID(Integer userID) {
		Customer customer = mongoTemplate.findById(userID, Customer.class);
		if (customer == null) {
			throw new IndexOutOfBoundsException("No external user " + userID);
		}
		return customer.getEmail();
	}

	public boolean addMoney(Customer user, BankAccountDB account, Transaction transaction, BigDecimal amount,
			String accountType) {
		try {
			transactionTemplate.executeWithoutResult(status -> insertPending(account, transaction, amount,
					"creditfunds", "Deposit money to account"));
			return true;
		} catch (RuntimeException e) {
			e.printStackTrace();
			return false;
		}
	}

	/** Pending debit + hold on the account, atomically (legacy: JDBC transaction, here a MongoDB transaction). */
	public boolean withdrawMoney(Customer user, BankAccountDB account, Transaction transaction, BigDecimal amount,
			String accountType) {
		try {
			transactionTemplate.executeWithoutResult(status -> {
				insertPending(account, transaction, amount, "debitfunds", "Withdrawal of money from account");
				mongoTemplate.updateFirst(
						Query.query(where("external_users_id").is(user.getId()).and("account_type").is(accountType)),
						new Update().inc("hold", amount), BankAccountDB.class);
			});
			return true;
		} catch (RuntimeException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void insertPending(BankAccountDB account, Transaction transaction, BigDecimal amount, String type,
			String description) {
		Timestamp createdDateTime = new Timestamp(System.currentTimeMillis());
		Transaction pending = new Transaction();
		pending.setId(sequences.next(MongoCollections.TRANSACTIONS));
		pending.setPayer_id(account.getAccount_number());
		pending.setPayee_id(account.getAccount_number());
		pending.setAmount(amount);
		pending.setHashvalue("hasvalue");
		pending.setTransaction_type(type);
		pending.setDescription(description);
		pending.setStatus("pending");
		pending.setApprover("External user");
		pending.setCritical(false);
		pending.setTimestamp_created(createdDateTime);
		pending.setTimestamp_updated(createdDateTime);
		pending.setStage(MongoCollections.PENDING);
		mongoTemplate.insert(pending);
		transaction.setId(pending.getId());
	}

	private static BankAccount.AccountType accountType(String type) {
		if (type == null) {
			return null;
		}
		String t = type.toUpperCase();
		if (t.startsWith("CHECKING")) {
			return BankAccount.AccountType.CHECKING;
		}
		try {
			return BankAccount.AccountType.valueOf(t);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
