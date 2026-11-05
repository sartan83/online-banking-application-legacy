package org.thothlab.devilsvault.dao.transaction;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.db.model.Transaction;

@Repository("extTransactionDAO")
public class ExternalTransactionDAO extends TransactionDaoImpl {

	@Override
	public Transaction createExternalTransaction(int payerAccountNumber, BigDecimal amount, int payeeAccountNumber,
			String description, String transactionType) {
		Transaction extTransfer = new Transaction();
		extTransfer.setPayer_id(payerAccountNumber);
		extTransfer.setPayee_id(payeeAccountNumber);
		extTransfer.setAmount(amount);
		extTransfer.setDescription(description);
		extTransfer.setTransaction_type(transactionType);
		extTransfer.setHashvalue("");
		extTransfer.setStatus("pending");
		extTransfer.setApprover("");
		extTransfer.setCritical(true);
		extTransfer.setTimestamp_created(null);
		extTransfer.setTimestamp_updated(null);
		return extTransfer;
	}

	/** Same as the parent but, like the legacy {@code NOW(), NOW()} insert, stamps both timestamps. */
	@Override
	public Boolean save(Transaction transaction, String type) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		transaction.setTimestamp_created(now);
		transaction.setTimestamp_updated(now);
		return super.save(transaction, type);
	}
}
