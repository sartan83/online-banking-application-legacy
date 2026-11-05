package org.thothlab.devilsvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.thothlab.devilsvault.controllers.security.CustomAuthenticationSuccessHandler;
import org.thothlab.devilsvault.controllers.security.LimitLoginAuthenticationProvider;
import org.thothlab.devilsvault.dao.user.CustomUserDetailsService;
import org.thothlab.devilsvault.dao.user.UserDetailsDao;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public BCryptPasswordEncoder encoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
		return new CustomAuthenticationSuccessHandler();
	}

	@Bean
	public LimitLoginAuthenticationProvider authenticationProvider(CustomUserDetailsService customUserDetailsService,
			UserDetailsDao userDetailsDao, BCryptPasswordEncoder encoder) {
		LimitLoginAuthenticationProvider provider = new LimitLoginAuthenticationProvider(customUserDetailsService);
		provider.setUserDetailsDao(userDetailsDao);
		provider.setPasswordEncoder(encoder);
		return provider;
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, LimitLoginAuthenticationProvider authenticationProvider,
			CustomAuthenticationSuccessHandler successHandler, SessionRegistry sessionRegistry) throws Exception {
		http.authenticationProvider(authenticationProvider)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/employee/externalregistrationform").hasAnyAuthority("ROLE_REGULAR", "ROLE_MANAGER")
				.requestMatchers("/employee/internalregistrationform").hasAuthority("ROLE_ADMIN")
				.requestMatchers("/employee/internalregister").hasAuthority("ROLE_ADMIN")
				.requestMatchers("/employee/externalregister").hasAnyAuthority("ROLE_REGULAR", "ROLE_MANAGER")
				.requestMatchers("/employee/transaction").hasAuthority("ROLE_MANAGER")
				.requestMatchers("/employee/systemlogs").hasAuthority("ROLE_ADMIN")
				.requestMatchers("/employee/**").hasAnyAuthority("ROLE_REGULAR", "ROLE_MANAGER", "ROLE_ADMIN")
				.requestMatchers("/customer/**").hasAnyAuthority("ROLE_CUSTOMER", "ROLE_MERCHANT")
				.anyRequest().permitAll())
			.formLogin(form -> form
				.loginPage("/login")
				.loginProcessingUrl("/j_spring_security_check")
				.failureUrl("/login?error=")
				.usernameParameter("username")
				.passwordParameter("password")
				.successHandler(successHandler))
			.sessionManagement(session -> session
				.invalidSessionUrl("/login")
				.sessionFixation(fixation -> fixation.newSession())
				.maximumSessions(1)
				.maxSessionsPreventsLogin(true)
				.sessionRegistry(sessionRegistry))
			.logout(logout -> logout
				.logoutSuccessUrl("/login")
				.deleteCookies("JSESSIONID"));
		return http.build();
	}
}
