package org.thothlab.devilsvault.config;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/** Auto-increment integer ids (MySQL AUTO_INCREMENT equivalent) backed by the {@code counters} collection. */
@Component
public class SequenceGenerator {

	public static final String COUNTERS = "counters";

	private final MongoTemplate mongoTemplate;

	public SequenceGenerator(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public int next(String sequence) {
		Document counter = mongoTemplate.findAndModify(Query.query(where("_id").is(sequence)),
				new Update().inc("seq", 1), FindAndModifyOptions.options().returnNew(true).upsert(true),
				Document.class, COUNTERS);
		return ((Number) counter.get("seq")).intValue();
	}
}
