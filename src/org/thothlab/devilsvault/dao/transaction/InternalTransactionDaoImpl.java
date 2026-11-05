package org.thothlab.devilsvault.dao.transaction;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.thothlab.devilsvault.dao.MongoCollections.COMPLETED;
import static org.thothlab.devilsvault.dao.MongoCollections.PENDING;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.Transaction;

@Repository("TransactionSpecificDao")
public class InternalTransactionDaoImpl extends TransactionDaoImpl {

	public int createTransaction() {
		Transaction transaction = new Transaction();
		transaction.setPayer_id(1);
		transaction.setPayee_id(1);
		transaction.setAmount(new BigDecimal(500));
		transaction.setHashvalue("pending");
		transaction.setTransaction_type("Add money");
		transaction.setDescription("Add 1500USD");
		transaction.setStatus("sds");
		transaction.setApprover("sdsds");
		transaction.setCritical(true);
		Timestamp date = Timestamp.valueOf("2016-10-10 00:00:00");
		transaction.setTimestamp_created(date);
		transaction.setTimestamp_updated(date);
		return save(transaction, "transaction_pending") ? 1 : 0;
	}

	public List<Transaction> getAllPending() {
		return mongoTemplate.find(Query.query(where("stage").is(PENDING)), Transaction.class);
	}

	public List<Transaction> getAllCompleted() {
		return mongoTemplate.find(Query.query(where("stage").is(COMPLETED)), Transaction.class);
	}

	@Override
	public Boolean save(Transaction transaction, String type) {
		return super.save(transaction, type);
	}

	public List<Transaction> getByUserId(int id, String table) {
		return mongoTemplate.find(Query.query(where("stage").is(MongoCollections.stageOf(table))
				.orOperator(where("payer_id").is(id), where("payee_id").is(id))), Transaction.class);
	}

	public List<Transaction> getTransactionById(int id, String table) {
		return getById(id, table);
	}

	public void approveTransaction(int id, String type) {
		Transaction transaction = getById(id, type).get(0);
		transaction.setStatus("approved");
		save(transaction, "transaction_completed");
		deleteById(id, type);
	}

	public void rejectTransaction(int id, String type) {
		Transaction transaction = getById(id, type).get(0);
		transaction.setStatus("rejected");
		save(transaction, "transaction_completed");
		deleteById(id, type);
	}

	public List<Transaction> getAllPendingTransactionByAccountNo(List<Integer> accoNos) {
		Criteria criteria = where("stage").is(PENDING).and("payer_id").in(accoNos).and("critical").is(false);
		return mongoTemplate.find(Query.query(criteria), Transaction.class);
	}

	public void updateHold(int payerAccountNumber, BigDecimal amount) {
		incAccount(payerAccountNumber, "hold", amount.negate());
	}

	public void updateCC_AvailableBalance(int amount, int account_number) {
		incCreditCard(account_number, "available_balance", -amount);
	}

	public void updateCC_CurrentDueAmt(int amount, int account_number) {
		incCreditCard(account_number, "current_due_amt", -amount);
	}

	public void updateCC_Payment(int amount, int account_number) {
		incCreditCard(account_number, "payment", amount);
	}
}
