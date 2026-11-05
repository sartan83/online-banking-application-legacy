package org.thothlab.devilsvault.dao.customer;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.bankaccount.CreditCardGenerator;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.CreditAccountDB;

@Repository("CreditCardDao")
public class CreditCardDaoImpl {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;

	public CreditCardDaoImpl(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	public void createCreditAccount(Integer accountNumber) {
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		cal.set(year, month, 25);
		java.sql.Date sqldateCycle = new java.sql.Date(cal.getTime().getTime());
		cal.set(year, month, 15);
		java.sql.Date sqldateDue = new java.sql.Date(cal.getTime().getTime());
		BigInteger credit_card_number = new BigInteger(CreditCardGenerator.generateMasterCardNumber());
		while (mongoTemplate.exists(Query.query(where("credit_card.credit_card_no").is(credit_card_number)),
				BankAccountDB.class)) {
			credit_card_number = new BigInteger(CreditCardGenerator.generateMasterCardNumber());
		}
		CreditAccountDB creditaccount = new CreditAccountDB();
		creditaccount.setAccount_number(accountNumber);
		creditaccount.setApr((float) 2.2);
		creditaccount.setAvailable_balance(0);
		creditaccount.setCredit_card_number(credit_card_number);
		creditaccount.setCredit_limit(800);
		creditaccount.setCurrent_due_amount(0);
		creditaccount.setCycle_date(sqldateCycle);
		creditaccount.setDue_date(sqldateDue);
		creditaccount.setInterest(5);
		creditaccount.setLast_bill_amount(0);
		creditaccount.setPayment(0);
		save(creditaccount);
	}

	/** Embeds the card ({@code credit_card_account_details} row) in its CREDIT bank account document. */
	public Boolean save(CreditAccountDB creditaccount) {
		if (creditaccount.getId() == null) {
			creditaccount.setId(sequences.next("credit_card_account_details"));
		}
		return mongoTemplate.updateFirst(Query.query(where("_id").is(creditaccount.getAccount_number())),
				new Update().set("credit_card", creditaccount), BankAccountDB.class).getMatchedCount() != 0;
	}
}
