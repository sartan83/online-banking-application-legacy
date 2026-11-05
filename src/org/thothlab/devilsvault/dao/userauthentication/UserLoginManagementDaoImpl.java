package org.thothlab.devilsvault.dao.userauthentication;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.UserAuthentication;

@Repository("UserLoginManagementDao")
public class UserLoginManagementDaoImpl {

	private final MongoTemplate mongoTemplate;

	public UserLoginManagementDaoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<UserAuthentication> getLockedUsers(List<String> roles) {
		return mongoTemplate.find(Query.query(where("role").in(roles).and("accountNonLocked").is(0)),
				UserAuthentication.class);
	}

	public List<UserAuthentication> getOtpLockedUsers(List<String> roles) {
		return mongoTemplate.find(Query.query(where("role").in(roles).and("otpNonLocked").is(0)),
				UserAuthentication.class);
	}

	public Boolean unlockUserAccount(String username, String requestType) {
		String flag = requestType.toLowerCase() + "NonLocked";
		mongoTemplate.updateFirst(Query.query(where("_id").is(username)),
				new Update().set(flag, 1).unset("login_attempts"), UserAuthentication.class);
		return null;
	}
}
