package org.thothlab.devilsvault.dao.transaction;

import java.util.List;

import org.thothlab.devilsvault.db.model.Transaction;

public interface TransactionDao {
	public Boolean save(Transaction transaction, String type);
	public List<Transaction> getById(int id,String table);
	public void update(Transaction employer);
	public boolean deleteById(int id,String type);
}
