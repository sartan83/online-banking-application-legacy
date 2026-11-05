package org.thothlab.devilsvault.dao.user;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thothlab.devilsvault.db.model.UserAuthentication;

/** Loads users (with accountNonLocked / enabled flags and their single role) from the {@code users} collection. */
@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

	private final MongoTemplate mongoTemplate;

	public CustomUserDetailsService(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAuthentication user = username == null ? null : mongoTemplate.findById(username, UserAuthentication.class);
		if (user == null || user.getRole() == null) {
			throw new UsernameNotFoundException("Username " + username + " not found");
		}
		return new User(user.getUsername(), user.getPassword(), user.getEnabled() == 1, user.getAccountNonExpired() == 1,
				user.getCredentialsNonExpired() == 1, user.getAccountNonLocked() == 1,
				List.of(new SimpleGrantedAuthority(user.getRole())));
	}
}
