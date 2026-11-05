package org.thothlab.devilsvault.db.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Document("bank_accounts")
public class BankAccountDB {
	@Id
	private int account_number;
	private String account_type;
	@Field("external_users_id")
	private int external_user_id;
	private BigDecimal balance;
	private BigDecimal hold;
	public int getAccount_number() {
		return account_number;
	}
	public void setAccount_number(int account_number) {
		this.account_number = account_number;
	}
	public String getAccount_type() {
		return account_type;
	}
	public void setAccount_type(String account_type) {
		this.account_type = account_type;
	}
	public int getExternal_user_id() {
		return external_user_id;
	}
	public void setExternal_user_id(int external_user_id) {
		this.external_user_id = external_user_id;
	}
	public BigDecimal getBalance() {
		return balance;
	}
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
	public BigDecimal getHold() {
		return hold;
	}
	public void setHold(BigDecimal hold) {
		this.hold = hold;
	}	

	private CreditAccountDB credit_card;
	public CreditAccountDB getCredit_card() {
		return credit_card;
	}
	public void setCredit_card(CreditAccountDB credit_card) {
		this.credit_card = credit_card;
	}
}
