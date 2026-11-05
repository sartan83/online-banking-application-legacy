package org.thothlab.devilsvault.dao.log;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.DatabaseLog;

/** {@code internal_log} / {@code external_log} -> {@code system_logs} with {@code scope}. */
@Repository("DatabaseLogDao")
public class LogDaoImpl implements LogDao {

	private final MongoTemplate mongoTemplate;

	public LogDaoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Boolean save(DatabaseLog log, String tablename) {
		log.setScope(tablename.replace("_log", ""));
		mongoTemplate.insert(log);
		return true;
	}

	@Override
	public List<DatabaseLog> getByUserId(int userid, String type) {
		return mongoTemplate.find(Query.query(where("scope").is(type).and("userid").is(userid)), DatabaseLog.class);
	}

	@Override
	public List<DatabaseLog> getLogs(String type) {
		return mongoTemplate.find(
				Query.query(where("scope").is(type)).with(Sort.by(Sort.Direction.DESC, "timestamp")).limit(50),
				DatabaseLog.class);
	}
}
