package org.thothlab.devilsvault.dao.userauthentication;

import org.thothlab.devilsvault.db.model.UserAuthentication;

public interface UserAuthenticationDao {
	public Boolean save(UserAuthentication userdetails);
	public String changePassword(String oldPassword,String newPassword,String confirmPassword,Integer userID,String role);
	public UserAuthentication getUserDetails(String email);
}
