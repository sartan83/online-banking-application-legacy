package org.thothlab.devilsvault.test.dao;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.thothlab.devilsvault.dao.customer.CustomerDAO;
import org.thothlab.devilsvault.dao.dashboard.PendingStatisticsDao;
import org.thothlab.devilsvault.dao.employee.InternalUserDaoImpl;
import org.thothlab.devilsvault.dao.request.ExternalRequestDaoImpl;
import org.thothlab.devilsvault.dao.request.InternalRequestDaoImpl;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.InternalUser;
import org.thothlab.devilsvault.db.model.Request;
import org.thothlab.devilsvault.test.support.DaoTestConfig;
import org.thothlab.devilsvault.test.support.MongoContainerSupport;
import org.thothlab.devilsvault.test.support.TestData;

@DataMongoTest
@Import(DaoTestConfig.class)
class RequestDaoTest extends MongoContainerSupport {

	@Autowired
	MongoTemplate mongo;
	@Autowired
	InternalRequestDaoImpl internalRequestDao;
	@Autowired
	ExternalRequestDaoImpl externalRequestDao;
	@Autowired
	CustomerDAO customerDAO;
	@Autowired
	InternalUserDaoImpl internalUserDao;
	@Autowired
	PendingStatisticsDao pendingStatisticsDao;

	@BeforeEach
	void seed() {
		TestData.load(mongo);
	}

	@Test
	void customerRequestIsRaisedOnceAndApprovalUpdatesProfile() {
		Customer alice = customerDAO.getCustomer(1);
		assertThat(internalRequestDao.validateRequest(1, "address", "external_request_pending")).isTrue();
		internalRequestDao.raiseInternalRequest(alice, "address", "5 New St", 1);
		assertThat(internalRequestDao.validateRequest(1, "address", "external_request_pending")).isFalse();

		Request raised = externalRequestDao.getByUserId(1, "pending").get(0);
		assertThat(raised.getId()).isEqualTo(2);
		assertThat(raised.getCurrent_value()).isEqualTo("1 Palm Walk");
		assertThat(externalRequestDao.getAllPending()).hasSize(2);
		assertThat(pendingStatisticsDao.getPendingStatistics("ROLE_REGULAR")).containsEntry("external", 2);

		externalRequestDao.approveRequest(raised.getId(), "external", 1);

		assertThat(customerDAO.getCustomer(1).getAddress()).isEqualTo("5 New St");
		assertThat(externalRequestDao.getById(raised.getId(), "pending")).isEmpty();
		assertThat(externalRequestDao.getById(raised.getId(), "completed")).singleElement().satisfies(r -> {
			assertThat(r.getStatus()).isEqualTo("approved");
			assertThat(r.getApprover()).isEqualTo("1");
		});
	}

	@Test
	void rejectKeepsProfileAndArchivesRequest() {
		externalRequestDao.rejectRequest(1, "external", 2);
		assertThat(customerDAO.getCustomer(2).getAddress()).isEqualTo("2 Palm Walk");
		assertThat(externalRequestDao.getAllCompleted()).singleElement()
				.satisfies(r -> assertThat(r.getStatus()).isEqualTo("rejected"));
	}

	@Test
	void internalPhoneRequestApprovalUpdatesEmployee() {
		InternalUser rita = internalUserDao.getUserById(1);
		internalRequestDao.raiseInternalPersonalRequest(rita, "phone", "4805559999", 1, "ROLE_REGULAR");
		Request pending = internalRequestDao.getAllPending("ROLE_REGULAR").get(0);
		internalRequestDao.approveRequest(pending.getId(), "internal", 2);
		assertThat(internalUserDao.getUserById(1).getPhone()).hasToString("4805559999");
		assertThat(internalRequestDao.getAllCompleted("ROLE_REGULAR")).hasSize(1);
	}
}
