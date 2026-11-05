package org.thothlab.devilsvault.test.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.thothlab.devilsvault.config.DaoContext;
import org.thothlab.devilsvault.config.MongoConfig;
import org.thothlab.devilsvault.config.SequenceGenerator;

/** Adds the Mongo-backed DAOs (legacy bean names) to the {@code @DataMongoTest} slice. */
@TestConfiguration
@ComponentScan("org.thothlab.devilsvault.dao")
@Import({ MongoConfig.class, SequenceGenerator.class, DaoContext.class })
public class DaoTestConfig {
}
