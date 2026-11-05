package org.thothlab.devilsvault.test.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * One single-node replica-set MongoDB (Testcontainers) per JVM, so multi-document transactions work.
 * The image defaults to mongo:8.0 and can be overridden with -Dmongodb.image=...
 */
public abstract class MongoContainerSupport {

	protected static final MongoDBContainer MONGO = new MongoDBContainer(
			DockerImageName.parse(System.getProperty("mongodb.image", "mongo:8.0")).asCompatibleSubstituteFor("mongo"));

	static {
		MONGO.start();
	}

	@DynamicPropertySource
	static void mongoProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("devilsvault_test"));
	}
}
