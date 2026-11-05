package org.thothlab.devilsvault.dao.creditcard;

import org.thothlab.devilsvault.config.DaoContext;
import org.thothlab.devilsvault.dao.customer.CustomerAccountsDAO;
import org.thothlab.devilsvault.dao.customer.CustomerDAO;
import org.thothlab.devilsvault.dao.transaction.TransferDAO;

public class CustomerDAOHelper {

	public static CreditCardDOA creditCardDAO() {
		return DaoContext.open().getBean("creditCardDOA", CreditCardDOA.class);
	}

	public static CustomerDAO customerDAO() {
		return DaoContext.open().getBean("customerDAO", CustomerDAO.class);
	}

	public static CustomerAccountsDAO customerAccountsDAO() {
		return DaoContext.open().getBean("CustomerAccountsDAO", CustomerAccountsDAO.class);
	}

	public static TransferDAO transferDAO() {
		return DaoContext.open().getBean("transferDAO", TransferDAO.class);
	}
}
