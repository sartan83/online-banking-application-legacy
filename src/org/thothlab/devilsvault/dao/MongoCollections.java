package org.thothlab.devilsvault.dao;

/** Maps the legacy MySQL table names (still passed around by controllers) to MongoDB collections and stages. */
public final class MongoCollections {

	public static final String USERS = "users";
	public static final String EXTERNAL_USERS = "external_users";
	public static final String INTERNAL_USERS = "internal_users";
	public static final String BANK_ACCOUNTS = "bank_accounts";
	public static final String TRANSACTIONS = "transactions";
	public static final String REQUESTS = "requests";
	public static final String AUTHORIZATIONS = "authorizations";
	public static final String PENDING_REGISTRATIONS = "pending_registrations";
	public static final String OTP = "otp";
	public static final String SYSTEM_LOGS = "system_logs";

	public static final String PENDING = "pending";
	public static final String COMPLETED = "completed";
	public static final String INTERNAL = "internal";
	public static final String EXTERNAL = "external";

	private MongoCollections() {
	}

	public static String forTable(String table) {
		switch (table) {
		case "internal_user":
			return INTERNAL_USERS;
		case "external_users":
			return EXTERNAL_USERS;
		case "users":
			return USERS;
		case "bank_accounts":
			return BANK_ACCOUNTS;
		default:
			throw new IllegalArgumentException("Unknown legacy table " + table);
		}
	}

	/** {@code transaction_pending} / {@code authorization_completed} / ... -> {@code pending} / {@code completed}. */
	public static String stageOf(String table) {
		if (table.endsWith("_pending") || table.equals(PENDING)) {
			return PENDING;
		}
		if (table.endsWith("_completed") || table.equals(COMPLETED)) {
			return COMPLETED;
		}
		throw new IllegalArgumentException("Unknown legacy table " + table);
	}
}
