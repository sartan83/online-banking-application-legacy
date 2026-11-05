package org.thothlab.devilsvault.dao.employee;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigInteger;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.InternalUser;

@Repository("EmployeeDAOForInternal")
public class InternalUserDaoImpl implements InternalUserDao {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;

	public InternalUserDaoImpl(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	public Integer getUserId(String email) {
		List<InternalUser> users = mongoTemplate.find(Query.query(where("email").is(email)), InternalUser.class);
		return users.get(0).getId();
	}

	public InternalUser getUserById(int user_id) {
		InternalUser user = mongoTemplate.findById(user_id, InternalUser.class);
		return user == null ? new InternalUser() : user;
	}

	public String getEmailID(Integer userID) {
		InternalUser user = mongoTemplate.findById(userID, InternalUser.class);
		if (user == null) {
			throw new IndexOutOfBoundsException("No internal user " + userID);
		}
		return user.getEmail();
	}

	public InternalUser setInternalUser(String name, String designation, String address, BigInteger phone, String email,
			String date_of_birth, String ssn) {
		InternalUser userDetails = new InternalUser();
		userDetails.setName(name);
		userDetails.setDesignation(designation);
		userDetails.setAddress(address);
		userDetails.setDate_of_birth(date_of_birth);
		userDetails.setEmail(email);
		userDetails.setPhone(phone);
		userDetails.setSsn(ssn);
		return userDetails;
	}

	public Boolean save(InternalUser userdetails) {
		if (userdetails.getId() == 0) {
			userdetails.setId(sequences.next(MongoCollections.INTERNAL_USERS));
		}
		try {
			mongoTemplate.insert(userdetails);
			return true;
		} catch (DuplicateKeyException e) {
			e.printStackTrace();
			return false;
		}
	}
}
