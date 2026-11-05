package org.thothlab.devilsvault.dao.pendingregistration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.PendingRegistration;

@Repository("PendingRegistrationDao")
public class PendingRegistrationDaoImpl implements PendingRegistrationDao {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;

	public PendingRegistrationDaoImpl(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	@Override
	public PendingRegistration getById(int id, String table) {
		List<PendingRegistration> requestList = mongoTemplate.find(Query.query(where("_id").is(id)), PendingRegistration.class);
		return requestList.get(0);
	}

	@Override
	public boolean deleteById(int id) {
		return mongoTemplate.remove(Query.query(where("_id").is(id)), PendingRegistration.class).getDeletedCount() != 0;
	}

	@Override
	public Boolean save(PendingRegistration user, String type) {
		user.setId(sequences.next(MongoCollections.PENDING_REGISTRATIONS));
		mongoTemplate.insert(user);
		return true;
	}

	@Override
	public boolean update(PendingRegistration user, String type) {
		if (!mongoTemplate.exists(Query.query(where("_id").is(user.getId())), PendingRegistration.class)) {
			return false;
		}
		mongoTemplate.save(user);
		return true;
	}

	@Override
	public List<PendingRegistration> getAllPending() {
		return mongoTemplate.findAll(PendingRegistration.class);
	}
}
