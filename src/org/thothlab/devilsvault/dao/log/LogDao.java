package org.thothlab.devilsvault.dao.log;

import java.util.List;

import org.thothlab.devilsvault.db.model.DatabaseLog;

public interface LogDao {
	public Boolean save(DatabaseLog log, String tablename);
	public List<DatabaseLog> getByUserId(int id, String tablename);
	List<DatabaseLog> getLogs(String type);
}
