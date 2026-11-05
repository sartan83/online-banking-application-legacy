package org.thothlab.devilsvault.dao.userauthentication;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import jakarta.mail.MessagingException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.thothlab.devilsvault.config.DaoContext;
import org.thothlab.devilsvault.dao.MongoCollections;
import org.thothlab.devilsvault.dao.customer.CustomerAccountsDAO;
import org.thothlab.devilsvault.dao.employee.InternalUserDaoImpl;
import org.thothlab.devilsvault.dao.employee.Validator;
import org.thothlab.devilsvault.db.model.UserAuthentication;

@Repository("userAuthenticationDao")
public class UserAuthenticationDaoImpl implements UserAuthenticationDao {

	private final MongoTemplate mongoTemplate;

	public UserAuthenticationDaoImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public void updatePassword(String newPassword, String username) {
		mongoTemplate.updateFirst(Query.query(where("_id").is(username)), new Update().set("password", newPassword),
				UserAuthentication.class);
	}

	@Override
	public String changePassword(String oldPassword, String newPassword, String confirmPassword, Integer userID,
			String role) {
		String message = "";
		DaoContext ctx = DaoContext.open();
		Validator validate = ctx.getBean("Validator", Validator.class);
		if (newPassword.equals(confirmPassword)) {
			if (validate.validatePassword(newPassword)) {
				CustomerAccountsDAO customeraccDao = ctx.getBean("CustomerAccountsDAO", CustomerAccountsDAO.class);
				InternalUserDaoImpl internaluserdao = ctx.getBean("EmployeeDAOForInternal", InternalUserDaoImpl.class);
				String email;
				if (role.equals("ROLE_CUSTOMER") || role.equals("ROLE_MERCHANT")) {
					email = customeraccDao.getEmailID(userID);
				} else {
					email = internaluserdao.getEmailID(userID);
				}
				UserAuthentication userdetails = getUserDetails(email);
				BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
				if (passwordEncoder.matches(oldPassword, userdetails.getPassword())) {
					updatePassword(passwordEncoder.encode(newPassword), email);
					message = "Password changed successfully";
				} else {
					message = "Old password entered is incorrect";
				}
			} else {
				message = "Password not meeting minimum criteria";
			}
		} else {
			message = "confirm password doesn't match with the new password";
		}
		ctx.close();
		return message;
	}

	@Override
	public UserAuthentication getUserDetails(String email) {
		UserAuthentication user = mongoTemplate.findById(email, UserAuthentication.class);
		if (user == null) {
			throw new IndexOutOfBoundsException("No user " + email);
		}
		return user;
	}

	public Boolean validateuserDetails(String username, BigInteger phone, String table) {
		Query sameContact = Query.query(new Criteria().orOperator(where("email").is(username), where("phone").is(phone)));
		boolean profileExists = mongoTemplate.exists(sameContact, MongoCollections.forTable(table));
		boolean userExists = mongoTemplate.exists(Query.query(where("_id").is(username)), UserAuthentication.class);
		return !(profileExists || userExists);
	}

	public HashMap<String, String> randomPasswordGenerator() {
		HashMap<String, String> passwords = new HashMap<String, String>();
		Random rand = new Random();
		Long rawPassword = (long) (rand.nextInt(999999 - 100000) + 100000);
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String hashedPassword = passwordEncoder.encode(Long.toString(rawPassword));
		passwords.put("rawPassword", Long.toString(rawPassword));
		passwords.put("hashedPassword", hashedPassword);
		return passwords;
	}

	public UserAuthentication setNewUsers(String username, String password, String role) {
		UserAuthentication userdetails = new UserAuthentication();
		userdetails.setUsername(username);
		userdetails.setPassword(password);
		userdetails.setRole(role);
		userdetails.setAccountNonExpired(1);
		userdetails.setAccountNonLocked(1);
		userdetails.setCredentialsNonExpired(1);
		userdetails.setEnabled(1);
		userdetails.setOtpNonLocked(1);
		return userdetails;
	}

	@Override
	public Boolean save(UserAuthentication userdetails) {
		try {
			mongoTemplate.insert(userdetails);
			return true;
		} catch (DuplicateKeyException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String changeForgotPassword(String newPassword, String confirmPassword, String username) {
		String message = "";
		DaoContext ctx = DaoContext.open();
		Validator validate = ctx.getBean("Validator", Validator.class);
		if (newPassword.equals(confirmPassword)) {
			if (validate.validatePassword(newPassword)) {
				BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
				updatePassword(passwordEncoder.encode(newPassword), username);
				message = "Password changed successfully";
			} else {
				message = "Password not meeting minimum criteria!!";
			}
		} else {
			message = "confirm password doesn't match with the new password";
		}
		ctx.close();
		return message;
	}

	public void sendEmailToUser(String email, String pass) {
		Properties settings;
		try {
			settings = MailSupport.settings();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return;
		}
		try {
			MailSupport.send(settings, email, "Welcome to Devils Vault!", "Your Username: " + email + "\n\nPassword : "
					+ pass + "\n\nPlease do not share this password with anyone");
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}
}
