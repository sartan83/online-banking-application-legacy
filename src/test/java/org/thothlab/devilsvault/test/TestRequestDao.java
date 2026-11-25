package org.thothlab.devilsvault.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.thothlab.devilsvault.dao.request.ExternalRequestDaoImpl;
import org.thothlab.devilsvault.db.model.Request;
import org.thothlab.devilsvault.support.TestDatabase;

/** Ported to JUnit 5; runs the production DaoDetails.xml against the H2 test database. */
class TestRequestDao {

	private ClassPathXmlApplicationContext ctx;
	private ExternalRequestDaoImpl externalRequest;

	@BeforeEach
	void setUp() {
		TestDatabase.reset(TestDatabase.dataSource());
		ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		externalRequest = ctx.getBean("externalRequestDao", ExternalRequestDaoImpl.class);
	}

	@AfterEach
	void tearDown() {
		ctx.close();
	}

	@Test
	void testCreateRequest() {
		assertThat(externalRequest.createRequest()).isEqualTo(1);
		assertThat(externalRequest.getAllRequestToApprove(0)).hasSize(1);
		assertThat(externalRequest.getByUserId(1, "pending")).singleElement()
				.satisfies(r -> assertThat(r.getRequest_type()).isEqualTo("phone change"));
	}

	@Test
	void testGetAllPending() {
		List<Request> requestList = externalRequest.getAllPending();
		assertThat(requestList).singleElement().satisfies(r -> assertThat(r.getRequesterid()).isEqualTo(10));
	}

	@Test
	void testGetAllCompleted() {
		assertThat(externalRequest.getAllCompleted()).isEmpty();
	}

	@Test
	void testGetById() {
		Request pending = externalRequest.getAllPending().get(0);
		assertThat(externalRequest.getById(pending.getId(), "pending")).singleElement()
				.satisfies(r -> assertThat(r.getDescription()).isEqualTo("unassigned request"));
	}

	@Test
	void testSave() {
		Request pending = externalRequest.getAllPending().get(0);
		assertThat(externalRequest.save(pending, "external")).isTrue();
		assertThat(externalRequest.getAllCompleted()).singleElement()
				.satisfies(r -> assertThat(r.getId()).isEqualTo(pending.getId()));
	}
}
