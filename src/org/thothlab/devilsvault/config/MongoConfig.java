package org.thothlab.devilsvault.config;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions.BigDecimalRepresentation;

@Configuration
public class MongoConfig {

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
		return MongoCustomConversions.create(adapter -> adapter.bigDecimal(BigDecimalRepresentation.DECIMAL128)
			.registerConverters(List.of(new TimestampToDate(), new SqlDateToDate(), new DateToTimestamp(),
					new DateToSqlDate(), new BigIntegerToLong(), new NumberToBigInteger())));
	}

	@Bean
	public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
		return new MongoTransactionManager(factory);
	}

	/** Phone and card numbers (MySQL BIGINT) are stored as Int64. */
	@WritingConverter
	static class BigIntegerToLong implements Converter<BigInteger, Long> {
		@Override
		public Long convert(BigInteger source) {
			return source.longValueExact();
		}
	}

	@ReadingConverter
	static class NumberToBigInteger implements Converter<Long, BigInteger> {
		@Override
		public BigInteger convert(Long source) {
			return BigInteger.valueOf(source);
		}
	}

	@WritingConverter
	static class TimestampToDate implements Converter<Timestamp, Date> {
		@Override
		public Date convert(Timestamp source) {
			return new Date(source.getTime());
		}
	}

	@WritingConverter
	static class SqlDateToDate implements Converter<java.sql.Date, Date> {
		@Override
		public Date convert(java.sql.Date source) {
			return new Date(source.getTime());
		}
	}

	@ReadingConverter
	static class DateToTimestamp implements Converter<Date, Timestamp> {
		@Override
		public Timestamp convert(Date source) {
			return new Timestamp(source.getTime());
		}
	}

	@ReadingConverter
	static class DateToSqlDate implements Converter<Date, java.sql.Date> {
		@Override
		public java.sql.Date convert(Date source) {
			return new java.sql.Date(source.getTime());
		}
	}
}
