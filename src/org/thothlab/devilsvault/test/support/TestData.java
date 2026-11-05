package org.thothlab.devilsvault.test.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.db.model.Authorization;
import org.thothlab.devilsvault.db.model.BankAccountDB;
import org.thothlab.devilsvault.db.model.CreditAccountDB;
import org.thothlab.devilsvault.db.model.Customer;
import org.thothlab.devilsvault.db.model.InternalUser;
import org.thothlab.devilsvault.db.model.Request;
import org.thothlab.devilsvault.db.model.Transaction;
import org.thothlab.devilsvault.db.model.UserAuthentication;

/** Same fixture as migration/sql/02_sample_data.sql (subset), written through the mapped entities. */
public final class TestData {

	public static final String PASSWORD = "Passw0rd@";

	private TestData() {
	}

	public static void load(MongoTemplate mongo) {
		mongo.getDb().drop();
		String hash = new BCryptPasswordEncoder(10).encode(PASSWORD);
		user(mongo, "alice@devilsvault.com", hash, "ROLE_CUSTOMER");
		user(mongo, "bob@devilsvault.com", hash, "ROLE_CUSTOMER");
		user(mongo, "shop@devilsvault.com", hash, "ROLE_MERCHANT");
		user(mongo, "regular@devilsvault.com", hash, "ROLE_REGULAR");
		user(mongo, "manager@devilsvault.com", hash, "ROLE_MANAGER");
		user(mongo, "admin@devilsvault.com", hash, "ROLE_ADMIN");

		customer(mongo, 1, "Alice Rossi", "alice@devilsvault.com", 4805550101L, List.of(3));
		customer(mongo, 2, "Bob Bianchi", "bob@devilsvault.com", 4805550102L, null);
		customer(mongo, 3, "Sun Devil Shop", "shop@devilsvault.com", 4805550103L, null);

		employee(mongo, 1, "Rita Regular", "REGULAR", "regular@devilsvault.com");
		employee(mongo, 2, "Marco Manager", "MANAGER", "manager@devilsvault.com");
		employee(mongo, 3, "Ada Admin", "ADMIN", "admin@devilsvault.com");

		int account = 1001;
		long[] cards = { 5412303716559819L, 5523335147124165L, 5134614022235713L };
		String[][] balances = { { "5000.00", "2500.00" }, { "1200.00", "800.00" }, { "10000.00", "0.00" } };
		for (int user = 1; user <= 3; user++) {
			account(mongo, account++, user, "CHECKING", balances[user - 1][0], null);
			account(mongo, account++, user, "SAVINGS", balances[user - 1][1], null);
			account(mongo, account++, user, "CREDIT", "0", card(user, account - 1, cards[user - 1]));
		}

		authorization(mongo, 1, 1, 1, "completed");
		authorization(mongo, 2, 1, 2, "completed");

		Transaction t = new Transaction();
		t.setId(1);
		t.setPayer_id(1001);
		t.setPayee_id(1004);
		t.setAmount(new BigDecimal("150.00"));
		t.setHashvalue("");
		t.setTransaction_type("externalFundTfr");
		t.setDescription("Dinner split");
		t.setStatus("approve");
		t.setApprover("1");
		Timestamp now = new Timestamp(System.currentTimeMillis() - 86_400_000L);
		t.setTimestamp_created(now);
		t.setTimestamp_updated(now);
		t.setStage("completed");
		mongo.insert(t);

		Request r = new Request();
		r.setId(1);
		r.setRequesterid(2);
		r.setRequest_type("address");
		r.setCurrent_value("22 University Dr");
		r.setRequested_value("77 Rural Rd");
		r.setStatus("Pending");
		r.setApprover("0");
		r.setDescription("Request to change address");
		r.setRole("ROLE_CUSTOMER");
		r.setTimestamp_created(new java.sql.Date(System.currentTimeMillis()));
		r.setTimestamp_updated(new java.sql.Date(System.currentTimeMillis()));
		r.setScope("external");
		r.setStage("pending");
		mongo.insert(r);

		counter(mongo, "external_users", 3);
		counter(mongo, "internal_users", 3);
		counter(mongo, "bank_accounts", 1009);
		counter(mongo, "credit_card_account_details", 3);
		counter(mongo, "transactions", 1);
		counter(mongo, "external_requests", 1);
		counter(mongo, "authorizations", 2);
	}

	private static void user(MongoTemplate mongo, String username, String hash, String role) {
		UserAuthentication u = new UserAuthentication();
		u.setUsername(username);
		u.setPassword(hash);
		u.setRole(role);
		u.setEnabled(1);
		u.setAccountNonExpired(1);
		u.setAccountNonLocked(1);
		u.setCredentialsNonExpired(1);
		u.setOtpNonLocked(1);
		mongo.insert(u);
	}

	private static void customer(MongoTemplate mongo, int id, String name, String email, long phone,
			List<Integer> merchants) {
		Customer c = new Customer();
		c.setId(id);
		c.setName(name);
		c.setEmail(email);
		c.setAddress(id + " Palm Walk");
		c.setCity("Tempe");
		c.setState("AZ");
		c.setCountry("USA");
		c.setPincode(85281);
		c.setPhone(BigInteger.valueOf(phone));
		c.setDate_of_birth("1990-01-01");
		c.setSsn("123-45-678" + id);
		c.setAuthorized_merchants(merchants);
		mongo.insert(c);
	}

	private static void employee(MongoTemplate mongo, int id, String name, String designation, String email) {
		InternalUser u = new InternalUser();
		u.setId(id);
		u.setName(name);
		u.setDesignation(designation);
		u.setEmail(email);
		u.setAddress(id + " Bank St");
		u.setPhone(BigInteger.valueOf(4805550200L + id));
		u.setDate_of_birth("1980-01-01");
		u.setSsn("567-89-012" + id);
		mongo.insert(u);
	}

	private static CreditAccountDB card(int id, int accountNumber, long number) {
		CreditAccountDB cc = new CreditAccountDB();
		cc.setId(id);
		cc.setAccount_number(accountNumber);
		cc.setCredit_card_number(BigInteger.valueOf(number));
		cc.setInterest(5);
		cc.setApr(2.2f);
		cc.setAvailable_balance(0);
		cc.setLast_bill_amount(0);
		cc.setCurrent_due_amount(0);
		cc.setCredit_limit(800);
		cc.setPayment(0);
		cc.setDue_date(java.sql.Date.valueOf("2016-11-15"));
		cc.setCycle_date(java.sql.Date.valueOf("2016-11-25"));
		return cc;
	}

	private static void account(MongoTemplate mongo, int number, int user, String type, String balance,
			CreditAccountDB card) {
		BankAccountDB a = new BankAccountDB();
		a.setAccount_number(number);
		a.setExternal_user_id(user);
		a.setAccount_type(type);
		a.setBalance(new BigDecimal(balance));
		a.setHold(new BigDecimal("0.00"));
		a.setCredit_card(card);
		mongo.insert(a);
	}

	private static void authorization(MongoTemplate mongo, int id, int internal, int external, String stage) {
		Authorization a = new Authorization();
		a.setAuth_id(id);
		a.setInternal_userID(internal);
		a.setExternal_userID(external);
		a.setAuth_Type("transaction");
		a.setStage(stage);
		mongo.insert(a);
	}

	private static void counter(MongoTemplate mongo, String name, int value) {
		mongo.getCollection(SequenceGenerator.COUNTERS).insertOne(new Document("_id", name).append("seq", value));
	}

	public static BigDecimal balance(MongoTemplate mongo, int account) {
		return mongo.findById(account, BankAccountDB.class).getBalance();
	}

	public static BigDecimal hold(MongoTemplate mongo, int account) {
		return mongo.findById(account, BankAccountDB.class).getHold();
	}
}
