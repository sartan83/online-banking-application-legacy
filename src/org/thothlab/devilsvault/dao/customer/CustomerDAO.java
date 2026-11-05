package org.thothlab.devilsvault.dao.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.Customer;

@Repository("customerDAO")
public class CustomerDAO {

	protected MongoTemplate mongoTemplate;

	@Autowired
	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public Customer getCustomer(int id) {
		return mongoTemplate.findById(id, Customer.class);
	}
}
