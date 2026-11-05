package org.thothlab.devilsvault.dao.request;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.thothlab.devilsvault.dao.MongoCollections.COMPLETED;
import static org.thothlab.devilsvault.dao.MongoCollections.INTERNAL;
import static org.thothlab.devilsvault.dao.MongoCollections.PENDING;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.InternalUser;
import org.thothlab.devilsvault.db.model.Request;

@Repository("internalRequestDao")
public class InternalRequestDaoImpl extends RequestDaoImpl {

	public int createRequest() {
		Request request = new Request();
		request.setRequesterid(1);
		request.setRequest_type("phone change");
		request.setCurrent_value("12345");
		request.setRequested_value("54321");
		request.setStatus("pending");
		request.setApprover("ajay");
		request.setDescription("Chnage phone number");
		request.setTimestamp_created(java.sql.Date.valueOf("2016-10-10"));
		request.setTimestamp_updated(java.sql.Date.valueOf("2016-10-10"));
		return saveRaisedRequest(request, "internal_request_pending") ? 1 : 0;
	}

	public List<Request> getAllPending(String role) {
		return mongoTemplate.find(Query.query(where("scope").is(INTERNAL).and("stage").is(PENDING).and("role").is(role)),
				Request.class);
	}

	public List<Request> getAllCompleted(String role) {
		return mongoTemplate.find(Query.query(where("scope").is(INTERNAL).and("stage").is(COMPLETED).and("role").is(role)),
				Request.class);
	}

	@Override
	public Boolean save(Request request, String type) {
		return super.save(request, type);
	}

	@Override
	public List<Request> getById(int id, String status) {
		return mongoTemplate.find(byId(id, INTERNAL, status), Request.class);
	}

	@Override
	public List<Request> getByUserId(int requesterid, String status) {
		return mongoTemplate.find(
				Query.query(where("scope").is(INTERNAL).and("stage").is(status).and("requesterid").is(requesterid)),
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

	private static Request newRequest(int requesterId, String requestType, String currentValue, String newValue,
			String approver, String role) {
		Request request = new Request();
		request.setRequesterid(requesterId);
		request.setRequest_type(requestType);
		request.setCurrent_value(currentValue);
		request.setRequested_value(newValue);
		request.setStatus("Pending");
		request.setApprover(approver);
		request.setDescription("Request to change " + requestType);
		java.sql.Date sqlDate = new java.sql.Date(new Date().getTime());
		request.setTimestamp_created(sqlDate);
		request.setTimestamp_updated(sqlDate);
		request.setRole(role);
		return request;
	}

	private static String currentValue(String requestType, String email, Object phone, String address) {
		if (requestType.equalsIgnoreCase("email"))
			return email;
		else if (requestType.equalsIgnoreCase("phone"))
			return phone.toString();
		else
			return address;
	}

	public void raiseExternalRequest(Customer customer, String requestType, String newValue, Integer internalUserID) {
		Request request = newRequest(internalUserID, requestType,
				currentValue(requestType, customer.getEmail(), customer.getPhone(), customer.getAddress()), newValue,
				Integer.toString(customer.getId()), "ROLE_CUSTOMER");
		super.saveRaisedRequest(request, "external_request_pending");
	}

	public void raiseInternalPersonalRequest(InternalUser internaluser, String requestType, String newValue,
			Integer internalUserID, String role) {
		Request request = newRequest(internalUserID, requestType,
				currentValue(requestType, internaluser.getEmail(), internaluser.getPhone(), internaluser.getAddress()),
				newValue, Integer.toString(0), role);
		super.saveRaisedRequest(request, "internal_request_pending");
	}

	public void raiseInternalRequest(Customer customer, String requestType, String newValue, Integer internalUserID) {
		Request request = newRequest(customer.getId(), requestType,
				currentValue(requestType, customer.getEmail(), customer.getPhone(), customer.getAddress()), newValue,
				Integer.toString(0), "ROLE_CUSTOMER");
		super.saveRaisedRequest(request, "external_request_pending");
	}

	/** false when the user already has a request of this type in the given legacy table. */
	public Boolean validateRequest(Integer userID, String requesttype, String table) {
		String scope = table.startsWith(INTERNAL) ? INTERNAL : MongoCollections.EXTERNAL;
		return !mongoTemplate.exists(Query.query(where("scope").is(scope).and("stage").is(MongoCollections.stageOf(table))
				.and("requesterid").is(userID).and("request_type").is(requesttype)), Request.class);
	}
}
