package org.thothlab.devilsvault.dao.user;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.Date;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.UserAttempts;
import org.thothlab.devilsvault.db.model.UserAuthentication;

/** Failed-login tracking, stored as the embedded {@code users.login_attempts} sub-document. */
@Repository("userDetailsDao")
public class UserDetailsDaoImpl implements UserDetailsDao {

	private static final int MAX_ATTEMPTS = 3;

	private final MongoTemplate mongoTemplate;

	public UserDetailsDaoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public void updateFailAttempts(String username) {
		UserAttempts user = getUserAttempts(username);
		Query byUser = Query.query(where("_id").is(username));
		if (user == null) {
			if (isUserExists(username)) {
				mongoTemplate.updateFirst(byUser,
						new Update().set("login_attempts.attempts", 1).set("login_attempts.lastModified", new Date()),
						UserAuthentication.class);
			}
		} else {
			if (isUserExists(username)) {
				mongoTemplate.updateFirst(byUser,
						new Update().inc("login_attempts.attempts", 1).set("login_attempts.lastModified", new Date()),
						UserAuthentication.class);
			}
			if (user.getAttempts() + 1 >= MAX_ATTEMPTS) {
				mongoTemplate.updateFirst(byUser, new Update().set("accountNonLocked", 0), UserAuthentication.class);
				throw new LockedException("User Account is locked!");
			}
		}
	}

	@Override
	public UserAttempts getUserAttempts(String username) {
		if (username == null) {
			return null;
		}
		UserAuthentication user = mongoTemplate.findById(username, UserAuthentication.class);
		if (user == null || user.getLoginAttempts() == null) {
			return null;
		}
		UserAttempts attempts = user.getLoginAttempts();
		attempts.setUsername(username);
		return attempts;
	}

	@Override
	public void resetFailAttempts(String username) {
		mongoTemplate.updateFirst(Query.query(where("_id").is(username).and("login_attempts").exists(true)),
				new Update().set("login_attempts.attempts", 0).set("login_attempts.lastModified", new Date()),
				UserAuthentication.class);
	}

	private boolean isUserExists(String username) {
		return mongoTemplate.exists(Query.query(where("_id").is(username)), UserAuthentication.class);
	}
}
