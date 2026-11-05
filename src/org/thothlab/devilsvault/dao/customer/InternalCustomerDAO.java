package org.thothlab.devilsvault.dao.customer;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.DaoContext;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.dao.employee.InternalUserDaoImpl;
import org.thothlab.devilsvault.db.model.Authorization;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.DatabaseLog;
import org.thothlab.devilsvault.db.model.InternalUser;
import org.thothlab.devilsvault.db.model.OTP;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.db.model.UserAuthentication;

@Repository("CustomerDAOForInternal")
public class InternalCustomerDAO extends CustomerDAO {

	public List<Integer> getAccNos(List<Integer> externalUserID) {
		List<Integer> accNos = new ArrayList<>();
		for (BankAccountDB account : mongoTemplate.find(Query.query(where("external_users_id").in(externalUserID)),
				BankAccountDB.class)) {
			accNos.add(account.getAccount_number());
		}
		return accNos;
	}

	public List<Integer> getExternalUserIds(Integer internalUserID, String authType) {
		List<Integer> externalIds = new ArrayList<>();
		for (Authorization authorization : mongoTemplate.find(Query.query(where("stage").is(MongoCollections.COMPLETED)
				.and("internal_userID").is(internalUserID).and("auth_Type").is(authType)), Authorization.class)) {
			externalIds.add(authorization.getExternal_userID());
		}
		return externalIds;
	}

	public Integer getUserId(String email) {
		List<Customer> customers = mongoTemplate.find(Query.query(where("email").is(email)), Customer.class);
		return customers.get(0).getId();
	}

	public void deleteCustomer(String extUserID) {
		int id = Integer.parseInt(extUserID);
		Customer customer = getCustomer(id);
		mongoTemplate.remove(Query.query(where("external_userID").is(id)), Authorization.class);
		mongoTemplate.remove(Query.query(where("scope").is(MongoCollections.EXTERNAL).and("userid").is(id)), DatabaseLog.class);
		mongoTemplate.remove(Query.query(where("stage").is(MongoCollections.PENDING)
				.orOperator(where("payer_id").is(id), where("payee_id").is(id))), Transaction.class);
		if (customer != null) {
			mongoTemplate.remove(Query.query(where("_id").is(customer.getEmail())), OTP.class);
			mongoTemplate.remove(Query.query(where("_id").is(customer.getEmail())), UserAuthentication.class);
		}
		mongoTemplate.remove(Query.query(where("_id").is(id)), Customer.class);
		mongoTemplate.remove(Query.query(where("external_users_id").is(id)), BankAccountDB.class);
	}

	public void deleteEmployee(String userID) {
		DaoContext ctx = DaoContext.open();
		InternalUserDaoImpl internalCustomer = ctx.getBean("EmployeeDAOForInternal", InternalUserDaoImpl.class);
		int id = Integer.parseInt(userID);
		InternalUser employee = internalCustomer.getUserById(id);
		mongoTemplate.remove(Query.query(where("internal_userID").is(id)), Authorization.class);
		mongoTemplate.remove(Query.query(new Criteria().andOperator(where("scope").is(MongoCollections.INTERNAL),
				where("userid").is(id))), DatabaseLog.class);
		mongoTemplate.remove(Query.query(where("_id").is(employee.getEmail())), OTP.class);
		mongoTemplate.remove(Query.query(where("_id").is(employee.getEmail())), UserAuthentication.class);
		mongoTemplate.remove(Query.query(where("_id").is(id)), InternalUser.class);
		ctx.close();
	}
}
