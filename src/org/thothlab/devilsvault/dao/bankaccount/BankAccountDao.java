package org.thothlab.devilsvault.dao.bankaccount;

import java.util.List;

import org.thothlab.devilsvault.db.model.BankAccountDB;

public interface BankAccountDao {
	public Boolean save(BankAccountDB bankDetails);
	public List<BankAccountDB> getAccountDetailsById(int userId);
}
