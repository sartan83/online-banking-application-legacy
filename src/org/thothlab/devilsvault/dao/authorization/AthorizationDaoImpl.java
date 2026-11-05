package org.thothlab.devilsvault.dao.authorization;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.thothlab.devilsvault.dao.MongoCollections.COMPLETED;
import static org.thothlab.devilsvault.dao.MongoCollections.PENDING;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.Authorization;

/** {@code authorization_pending} / {@code authorization_completed} -> {@code authorizations} with {@code stage}. */
@Repository("AuthorizationDao")
public class AthorizationDaoImpl implements AthorizationDao {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;

	public AthorizationDaoImpl(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	@Override
	public List<Authorization> getAllPendingAuthorization(int id) {
		return mongoTemplate.find(Query.query(where("stage").is(PENDING).and("internal_userID").is(id)), Authorization.class);
	}

	public List<Authorization> getAllPendingAuthorizationManager() {
		return mongoTemplate.find(Query.query(where("stage").is(PENDING)), Authorization.class);
	}

	public List<Authorization> getAllCompleteAuthorizationManager() {
		return mongoTemplate.find(Query.query(where("stage").is(COMPLETED)), Authorization.class);
	}

	@Override
	public List<Authorization> getAllCompleteAuthorization(int id) {
		return mongoTemplate.find(Query.query(where("stage").is(COMPLETED).and("internal_userID").is(id)), Authorization.class);
	}

	@Override
	public boolean isExist(int internal_userID, int external_userID, String auth_Type) {
		return mongoTemplate.exists(Query.query(where("internal_userID").is(internal_userID).and("external_userID")
				.is(external_userID).and("auth_Type").is(auth_Type)), Authorization.class);
	}

	@Override
	public Boolean save(int internal_userID, int external_userID, String auth_Type) {
		Authorization authorization = new Authorization();
		authorization.setInternal_userID(internal_userID);
		authorization.setExternal_userID(external_userID);
		authorization.setAuth_Type(auth_Type);
		return insert(authorization, PENDING);
	}

	@Override
	public boolean deleteByID(int auth_id, String table) {
		return mongoTemplate.remove(Query.query(where("auth_id").is(auth_id).and("stage").is(MongoCollections.stageOf(table))),
				Authorization.class).getDeletedCount() != 0;
	}

	@Override
	public boolean addByID(Authorization request) {
		Authorization completed = new Authorization();
		completed.setInternal_userID(request.getInternal_userID());
		completed.setExternal_userID(request.getExternal_userID());
		completed.setAuth_Type(request.getAuth_Type());
		return insert(completed, COMPLETED);
	}

	@Override
	public Authorization getByID(int auth_id) {
		List<Authorization> authorizationList = mongoTemplate
				.find(Query.query(where("auth_id").is(auth_id).and("stage").is(PENDING)), Authorization.class);
		return authorizationList.get(0);
	}

	private boolean insert(Authorization authorization, String stage) {
		authorization.setAuth_id(sequences.next(MongoCollections.AUTHORIZATIONS));
		authorization.setStage(stage);
		mongoTemplate.insert(authorization);
		return true;
	}
}
