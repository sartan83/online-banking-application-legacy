package org.thothlab.devilsvault.dao.request;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.thothlab.devilsvault.dao.MongoCollections.COMPLETED;
import static org.thothlab.devilsvault.dao.MongoCollections.EXTERNAL;
import static org.thothlab.devilsvault.dao.MongoCollections.PENDING;

import java.sql.Date;
import java.util.List;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.Request;

@Repository("externalRequestDao")
public class ExternalRequestDaoImpl extends RequestDaoImpl {

	public int createRequest() {
		Request request = new Request();
		request.setRequesterid(1);
		request.setRequest_type("phone change");
		request.setCurrent_value("12345");
		request.setRequested_value("54321");
		request.setStatus("pending");
		request.setApprover("ajay");
		request.setDescription("Chnage phone number");
		request.setTimestamp_created(Date.valueOf("2016-10-10"));
		request.setTimestamp_updated(Date.valueOf("2016-10-10"));
		return saveRaisedRequest(request, "external_request_pending") ? 1 : 0;
	}

	public List<Request> getAllPending() {
		return mongoTemplate.find(Query.query(where("scope").is(EXTERNAL).and("stage").is(PENDING).and("approver").is("0")),
				Request.class);
	}

	public List<Request> getAllCompleted() {
		return find(EXTERNAL, COMPLETED);
	}

	@Override
	public Boolean save(Request request, String type) {
		return super.save(request, type);
	}

	@Override
	public List<Request> getById(int id, String status) {
		return mongoTemplate.find(byId(id, EXTERNAL, status), Request.class);
	}

	@Override
	public List<Request> getByUserId(int requesterid, String status) {
		return mongoTemplate.find(
				Query.query(where("scope").is(EXTERNAL).and("stage").is(status).and("requesterid").is(requesterid)),
				Request.class);
	}

	@Override
	public void approveRequest(int id, String type, int approver) {
		super.approveRequest(id, type, approver);
	}

	@Override
	public void rejectRequest(int id, String type, int approver) {
		super.rejectRequest(id, type, approver);
	}

	public List<Request> getAllRequestToApprove(Integer userId) {
		return mongoTemplate.find(
				Query.query(where("scope").is(EXTERNAL).and("stage").is(PENDING).and("approver").is("" + userId)),
				Request.class);
	}
}
