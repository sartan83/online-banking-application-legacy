package org.thothlab.devilsvault.dao.dashboard;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.thothlab.devilsvault.dao.MongoCollections.EXTERNAL;
import static org.thothlab.devilsvault.dao.MongoCollections.INTERNAL;
import static org.thothlab.devilsvault.dao.MongoCollections.PENDING;

import java.util.HashMap;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.Request;
import org.thothlab.devilsvault.db.model.Transaction;

@Repository("pendingStatistics")
public class PendingStatisticsDao {

	private final MongoTemplate mongoTemplate;

	public PendingStatisticsDao(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Request> getPendingInternalRequests(String role) {
		return mongoTemplate.find(
				Query.query(where("scope").is(INTERNAL).and("stage").is(PENDING).and("role").is(role)), Request.class);
	}

	public List<Request> getPendingExternalRequests() {
		return mongoTemplate.find(
				Query.query(where("scope").is(EXTERNAL).and("stage").is(PENDING).and("approver").is("0")), Request.class);
	}

	public List<Transaction> getPendingTransactions() {
		return mongoTemplate.find(Query.query(where("stage").is(PENDING)), Transaction.class);
	}

	public HashMap<String, Integer> getPendingStatistics(String role) {
		HashMap<String, Integer> pendingStatisticsMap = new HashMap<String, Integer>();
		pendingStatisticsMap.put("internal", getPendingInternalRequests(role).size());
		pendingStatisticsMap.put("external", getPendingExternalRequests().size());
		pendingStatisticsMap.put("transaction", getPendingTransactions().size());
		return pendingStatisticsMap;
	}
}
