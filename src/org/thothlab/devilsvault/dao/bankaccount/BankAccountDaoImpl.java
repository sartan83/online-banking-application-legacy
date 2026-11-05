package org.thothlab.devilsvault.dao.bankaccount;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.SequenceGenerator;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.db.model.BankAccountDB;

@Repository("bankAccountDao")
public class BankAccountDaoImpl implements BankAccountDao {

	private final MongoTemplate mongoTemplate;
	private final SequenceGenerator sequences;

	public BankAccountDaoImpl(MongoTemplate mongoTemplate, SequenceGenerator sequences) {
		this.mongoTemplate = mongoTemplate;
		this.sequences = sequences;
	}

	@Override
	public Boolean save(BankAccountDB bankDetails) {
		if (bankDetails.getAccount_number() == 0) {
			bankDetails.setAccount_number(sequences.next(MongoCollections.BANK_ACCOUNTS));
		}
		try {
			mongoTemplate.insert(bankDetails);
			return true;
		} catch (DuplicateKeyException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public List<BankAccountDB> getAccountDetailsById(int userId) {
		return mongoTemplate.find(Query.query(where("external_users_id").is(userId)), BankAccountDB.class);
	}

	public Integer CreateAndGetCreditAccountNo(Integer UserID) {
		String[] type = new String[] { "CHECKING", "CREDIT", "SAVINGS" };
		Integer creditAccount = null;
		for (int i = 0; i < type.length; i++) {
			BankAccountDB bankDetails = new BankAccountDB();
			bankDetails.setExternal_user_id(UserID);
			bankDetails.setAccount_type(type[i]);
			bankDetails.setBalance(new BigDecimal("0"));
			bankDetails.setHold(new BigDecimal("0"));
			save(bankDetails);
			if ("CREDIT".equals(type[i])) {
				creditAccount = bankDetails.getAccount_number();
			}
		}
		return creditAccount;
	}
}
