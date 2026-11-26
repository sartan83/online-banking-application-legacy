package org.thothlab.devilsvault.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.thothlab.devilsvault.dao.dashboard.PendingStatisticsDao;
import org.thothlab.devilsvault.support.TestDatabase;

/** Ported to JUnit 5; runs the production DaoDetails.xml against the H2 test database. */
class TestPendingStatistics {

	private ClassPathXmlApplicationContext ctx;
	private PendingStatisticsDao pendingStatisticsDao;

	@BeforeEach
	void setUp() {
		TestDatabase.reset(TestDatabase.dataSource());
		ctx = new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml");
		pendingStatisticsDao = ctx.getBean("pendingStatistics", PendingStatisticsDao.class);
	}

	@AfterEach
	void tearDown() {
		ctx.close();
	}

	@Test
	void testGetPendingInternalRequests() {
		assertThat(pendingStatisticsDao.getPendingInternalRequests("admin")).hasSize(1);
		assertThat(pendingStatisticsDao.getPendingInternalRequests("manager")).isEmpty();
	}

	@Test
	void testGetPendingExternalRequests() {
		assertThat(pendingStatisticsDao.getPendingExternalRequests()).singleElement()
				.satisfies(r -> assertThat(r.getApprover()).isEqualTo("0"));
	}

	@Test
	void testGetPendingTransactions() {
		assertThat(pendingStatisticsDao.getPendingTransactions()).singleElement()
				.satisfies(t -> assertThat(t.getDescription()).isEqualTo("coffee"));
	}

	@Test
	void testGetPendingStatistics() {
		Map<String, Integer> items = pendingStatisticsDao.getPendingStatistics("admin");
		assertThat(items).isNotEmpty();
		assertThat(items.values()).allSatisfy(v -> assertThat(v).isNotNegative());
	}
}
