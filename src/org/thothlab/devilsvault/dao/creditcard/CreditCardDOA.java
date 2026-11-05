package org.thothlab.devilsvault.dao.creditcard;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.dao.customer.CustomerAccountsDAO;
import org.thothlab.devilsvault.db.model.BankAccount;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.CreditAccount;
import org.thothlab.devilsvault.db.model.CreditAccountDB;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.db.model.TransactionModel;

/** Credit card details live in the {@code credit_card} sub-document of the customer's CREDIT bank account. */
@Repository("creditCardDOA")
public class CreditCardDOA {

	private final MongoTemplate mongoTemplate;

	public CreditCardDOA(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/** Get the credit account details for the user. */
	public CreditAccount getCreditAccount(Customer customer) {
		List<BankAccountDB> accounts = mongoTemplate.find(Query.query(where("external_users_id").is(customer.getId())
				.and("account_type").is("CREDIT").and("credit_card").exists(true)), BankAccountDB.class);
		return toCreditAccount(accounts.get(0));
	}

	/** Returns one month (up to the 15th of {@code month + 1}, as in the legacy query) of card transactions. */
	public List<TransactionModel> getTransactionForMonth(CreditAccount account, int month) {
		month += 1;
		LocalDate end = LocalDate.of(2016, month, 15);
		Date to = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date from = Date.from(end.minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
		Criteria criteria = new Criteria().andOperator(
				new Criteria().orOperator(where("payee_id").is(account.getAccountNumber()),
						where("payer_id").is(account.getAccountNumber())),
				where("timestamp_updated").gte(from).lte(to));
		List<TransactionModel> transactionList = new ArrayList<>();
		for (Transaction t : mongoTemplate.find(Query.query(criteria), Transaction.class)) {
			TransactionModel obj = new TransactionModel();
			obj.setId(t.getId());
			obj.setPayee_id(t.getPayee_id());
			obj.setPayer_id(t.getPayer_id());
			obj.setAmount(t.getAmount() == null ? 0 : t.getAmount().intValue());
			obj.setStatus(t.getStatus());
			obj.setTimestamp_updated(t.getTimestamp_updated() == null ? null
					: new java.sql.Date(t.getTimestamp_updated().getTime()));
			obj.setIsPending(MongoCollections.PENDING.equals(t.getStage()) ? 1 : 0);
			obj.setDescription(t.getDescription());
			transactionList.add(obj);
		}
		transactionList.sort(Comparator.comparing(TransactionModel::getTimestamp_updated,
				Comparator.nullsLast(Comparator.<Date>naturalOrder())).reversed());
		return transactionList;
	}

	public CreditAccount getAccount(String creditCardNumber, String cvv, String month, String year) {
		List<BankAccountDB> accounts = mongoTemplate.find(
				Query.query(where("credit_card.credit_card_no").is(new BigInteger(creditCardNumber.trim()))),
				BankAccountDB.class);
		CreditAccount creditaccount = toCreditAccount(accounts.get(0));
		CustomerAccountsDAO customerAccDao = CustomerDAOHelper.customerAccountsDAO();
		BankAccount bankAcc = customerAccDao.getAccount(creditaccount.getAccountNumber());
		creditaccount.setAccountNumber(bankAcc.getAccountNumber());
		creditaccount.setOwner(bankAcc.getOwner());
		creditaccount.setAccountType(bankAcc.getAccountType());
		return creditaccount;
	}

	private static CreditAccount toCreditAccount(BankAccountDB account) {
		CreditAccountDB cc = account.getCredit_card();
		CreditAccount obj = new CreditAccount();
		obj.setId(cc.getId() == null ? 0 : cc.getId());
		obj.setCredit_card_no(cc.getCredit_card_number());
		obj.setAvailBalance(value(cc.getAvailable_balance()));
		obj.setLastBillAmount(value(cc.getLast_bill_amount()));
		obj.setDueDateTimestamp(cc.getDue_date());
		obj.setApr(cc.getApr());
		obj.setAccountNumber(account.getAccount_number());
		obj.setCycleDate(cc.getCycle_date());
		obj.setCurrentDueAmount(value(cc.getCurrent_due_amount()));
		obj.setCreditLimit(value(cc.getCredit_limit()));
		return obj;
	}

	private static int value(Integer v) {
		return v == null ? 0 : v;
	}
}
