package org.thothlab.devilsvault.dao.request;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.thothlab.devilsvault.dao.MongoCollections.COMPLETED;
import static org.thothlab.devilsvault.dao.MongoCollections.PENDING;

import java.math.BigInteger;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.Request;

/**
 * {@code internal|external_request_pending|completed} -> {@code requests} with {@code scope} and {@code stage}.
 * The {@code type} arguments are the legacy scope prefix ("internal" / "external").
 */
public class RequestDaoImpl implements RequestDao {

	protected MongoTemplate mongoTemplate;
	protected SequenceGenerator sequences;

	@Autowired
	public void setMongo(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	/** Archives the request in {@code <type>_request_completed}, keeping its id. */
	@Override
	public Boolean save(Request request, String type) {
		return insert(request, type, COMPLETED);
	}

	@Override
	public List<Request> getById(int id, String status) {
		return null;
	}

	/**
	 * Applies an approved change to a profile: {@code UPDATE <table> SET <set> = newValue WHERE <where> = oldValue}.
	 */
	@Override
	public void update(String table, String set, String newValue, String where, String oldValue) {
		String collection = MongoCollections.forTable(table);
		String key = "id".equals(where) ? "_id" : where;
		Object match = "_id".equals(key) ? (Object) Integer.valueOf(oldValue) : oldValue;
		Object value = "phone".equalsIgnoreCase(set) ? (Object) new BigInteger(newValue.trim()) : newValue;
		mongoTemplate.updateFirst(Query.query(where(key).is(match)), new Update().set(set.toLowerCase(), value), collection);
	}

	@Override
	public void deleteById(int id, String type) {
		mongoTemplate.remove(byId(id, type, PENDING), Request.class);
	}

	@Override
	public List<Request> getByUserId(int requesterid, String status) {
		return null;
	}

	@Override
	public void approveRequest(int id, String type, int approver) {
		Request request = mongoTemplate.find(byId(id, type, PENDING), Request.class).get(0);
		request.setStatus("approved");
		request.setApprover(Integer.toString(approver));
		String table;
		if (type.equalsIgnoreCase("internal"))
			table = "internal_user";
		else
			table = "external_users";
		update(table, request.getRequest_type(), request.getRequested_value(), "id",
				Integer.toString(request.getRequesterid()));
		save(request, type);
		deleteById(id, type);
	}

	@Override
	public void rejectRequest(int id, String type, int approver) {
		Request request = mongoTemplate.find(byId(id, type, PENDING), Request.class).get(0);
		request.setStatus("rejected");
		request.setApprover(Integer.toString(approver));
		save(request, type);
		deleteById(id, type);
	}

	/** INSERT into {@code internal_request_pending} / {@code external_request_pending} with a fresh id. */
	public Boolean saveRaisedRequest(Request request, String type) {
		String scope = type.startsWith(MongoCollections.INTERNAL) ? MongoCollections.INTERNAL : MongoCollections.EXTERNAL;
		request.setId(sequences.next(scope + "_requests"));
		return insert(request, scope, MongoCollections.stageOf(type));
	}

	protected Query byId(int id, String scope, String stage) {
		return Query.query(where("scope").is(scope).and("stage").is(stage).and("id").is(id));
	}

	protected List<Request> find(String scope, String stage) {
		return mongoTemplate.find(Query.query(where("scope").is(scope).and("stage").is(stage)), Request.class);
	}

	private Boolean insert(Request request, String scope, String stage) {
		Request copy = new Request();
		copy.setId(request.getId());
		copy.setRequesterid(request.getRequesterid());
		copy.setRequest_type(request.getRequest_type());
		copy.setCurrent_value(request.getCurrent_value());
		copy.setRequested_value(request.getRequested_value());
		copy.setStatus(request.getStatus());
		copy.setApprover(request.getApprover());
		copy.setDescription(request.getDescription());
		copy.setRole(request.getRole());
		copy.setTimestamp_created(request.getTimestamp_created());
		copy.setTimestamp_updated(request.getTimestamp_updated());
		copy.setScope(scope);
		copy.setStage(stage);
		try {
			mongoTemplate.insert(copy);
			return true;
		} catch (DuplicateKeyException e) {
			e.printStackTrace();
			return false;
		}
	}

}
