package org.thothlab.devilsvault.dao.transaction;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.Transaction;

/** {@code transaction_pending} / {@code transaction_completed} -> {@code transactions} with {@code stage}. */
@Repository("TransactionDao")
public class TransactionDaoImpl implements TransactionDao {

	protected MongoTemplate mongoTemplate;
	protected SequenceGenerator sequences;

	@Autowired
	public void setMongo(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	public Transaction createExternalTransaction(int payerAccountNumber, BigDecimal amount, int payeeAccountNumber,
			String description, String transactionType) throws ParseException {
		Transaction extTransfer = new Transaction();
		extTransfer.setPayer_id(payerAccountNumber);
		extTransfer.setPayee_id(payeeAccountNumber);
		extTransfer.setAmount(amount);
		extTransfer.setDescription(description);
		extTransfer.setTransaction_type(transactionType);
		extTransfer.setHashvalue("");
		extTransfer.setStatus("pending");
		extTransfer.setApprover("");
		if (amount.compareTo(new BigDecimal("1000")) == 1) {
			extTransfer.setCritical(true);
		} else {
			extTransfer.setCritical(false);
		}
		java.sql.Timestamp createdDateTime = new java.sql.Timestamp(new java.util.Date().getTime());
		extTransfer.setTimestamp_created(createdDateTime);
		extTransfer.setTimestamp_updated(createdDateTime);
		return extTransfer;
	}

	/**
	 * INSERT with a fresh id (set on the argument). Pending and completed share one sequence so that
	 * {@link #saveToCompleted} can keep the pending id without colliding with an existing completed one.
	 */
	@Override
	public Boolean save(Transaction transaction, String type) {
		transaction.setId(sequences.next(MongoCollections.TRANSACTIONS));
		return insert(transaction, type);
	}

	@Override
	public List<Transaction> getById(int id, String table) {
		return mongoTemplate.find(byId(id, table), Transaction.class);
	}

	@Override
	public void update(Transaction employer) {
	}

	@Override
	public boolean deleteById(int id, String type) {
		return mongoTemplate.remove(byId(id, type), Transaction.class).getDeletedCount() > 0;
	}

	/** INSERT keeping the transaction id (pending -> completed move). */
	public Boolean saveToCompleted(Transaction transaction, String type) {
		return insert(transaction, type);
	}

	public void updatePayerBalance(int payerAccountNumber, BigDecimal amount) {
		incAccount(payerAccountNumber, "balance", amount.negate());
	}

	public void updatePayeeBalance(int payeeAccountNumber, BigDecimal amount) {
		incAccount(payeeAccountNumber, "balance", amount);
	}

	protected Query byId(int id, String table) {
		return Query.query(where("stage").is(MongoCollections.stageOf(table)).and("id").is(id));
	}

	protected Boolean insert(Transaction transaction, String type) {
		Transaction copy = copyOf(transaction);
		copy.setStage(MongoCollections.stageOf(type));
		try {
			mongoTemplate.insert(copy);
			return true;
		} catch (DuplicateKeyException e) {
			e.printStackTrace();
			return false;
		}
	}

	protected void incAccount(int accountNumber, String field, BigDecimal amount) {
		mongoTemplate.updateFirst(Query.query(where("_id").is(accountNumber)), new Update().inc(field, amount),
				BankAccountDB.class);
	}

	protected void incCreditCard(int accountNumber, String field, int amount) {
		mongoTemplate.updateFirst(Query.query(where("_id").is(accountNumber).and("credit_card").exists(true)),
				new Update().inc("credit_card." + field, amount), BankAccountDB.class);
	}

	private static Transaction copyOf(Transaction t) {
		Transaction c = new Transaction();
		c.setId(t.getId());
		c.setPayer_id(t.getPayer_id());
		c.setPayee_id(t.getPayee_id());
		c.setAmount(t.getAmount());
		c.setHashvalue(t.getHashvalue());
		c.setTransaction_type(t.getTransaction_type());
		c.setDescription(t.getDescription());
		c.setStatus(t.getStatus());
		c.setApprover(t.getApprover());
		c.setCritical(t.isCritical());
		c.setTimestamp_created(t.getTimestamp_created());
		c.setTimestamp_updated(t.getTimestamp_updated());
		return c;
	}
}
