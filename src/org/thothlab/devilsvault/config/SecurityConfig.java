package org.thothlab.devilsvault.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.thothlab.devilsvault.controllers.security.LimitLoginAuthenticationProvider;
import org.thothlab.devilsvault.dao.user.CustomUserDetailsService;
import org.thothlab.devilsvault.dao.user.UserDetailsDaoImpl;

/**
 * Spring Security 6 replacement for the former XML namespace configuration
 * (WEB-INF/spring-security.xml). URL rules, login form and session policy are
 * kept identical so the existing JSPs keep working unchanged.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	public static final String LOGIN_PAGE = "/login";
	public static final String LOGIN_PROCESSING_URL = "/j_spring_security_check";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager,
			AuthenticationSuccessHandler customAuthenticationSuccessHandler) throws Exception {
		http
			.authenticationManager(authenticationManager)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(antMatcher("/employee/externalregistrationform")).hasAnyRole("REGULAR", "MANAGER")
				.requestMatchers(antMatcher("/employee/internalregistrationform")).hasRole("ADMIN")
				.requestMatchers(antMatcher("/employee/internalregister")).hasRole("ADMIN")
				.requestMatchers(antMatcher("/employee/externalregister")).hasAnyRole("REGULAR", "MANAGER")
				.requestMatchers(antMatcher("/employee/transaction")).hasRole("MANAGER")
				.requestMatchers(antMatcher("/employee/systemlogs")).hasRole("ADMIN")
				.requestMatchers(antMatcher("/employee/**")).hasAnyRole("REGULAR", "MANAGER", "ADMIN")
				.requestMatchers(antMatcher("/customer/**")).hasAnyRole("CUSTOMER", "MERCHANT")
				.anyRequest().permitAll())
			.formLogin(form -> form
				.loginPage(LOGIN_PAGE)
				.loginProcessingUrl(LOGIN_PROCESSING_URL)
				.failureUrl(LOGIN_PAGE + "?error=")
				.usernameParameter("username")
				.passwordParameter("password")
				.successHandler(customAuthenticationSuccessHandler))
			.sessionManagement(session -> session
				.invalidSessionUrl(LOGIN_PAGE)
				.sessionFixation(fixation -> fixation.newSession())
				.maximumSessions(1)
				.maxSessionsPreventsLogin(true))
			.logout(logout -> logout
				.logoutSuccessUrl(LOGIN_PAGE)
				.deleteCookies("JSESSIONID"))
			.csrf(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	public CustomUserDetailsService customUserDetailsService(@Qualifier("securitydataSource") DataSource dataSource) {
		CustomUserDetailsService service = new CustomUserDetailsService();
		service.setUsersByUsernameQuery("select * from users where username = ?");
		service.setAuthoritiesByUsernameQuery("select username, role from users where username =?");
		service.setDataSource(dataSource);
		return service;
	}

	@Bean
	public UserDetailsDaoImpl userDetailsDao(@Qualifier("securitydataSource") DataSource dataSource) {
		UserDetailsDaoImpl dao = new UserDetailsDaoImpl();
		dao.setDataSource(dataSource);
		return dao;
	}

	@Bean
	public BCryptPasswordEncoder encoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	public LimitLoginAuthenticationProvider authenticationProvider(CustomUserDetailsService customUserDetailsService,
			UserDetailsDaoImpl userDetailsDao, BCryptPasswordEncoder encoder) {
		LimitLoginAuthenticationProvider provider = new LimitLoginAuthenticationProvider();
		provider.setUserDetailsService(customUserDetailsService);
		provider.setUserDetailsDao(userDetailsDao);
		provider.setPasswordEncoder(encoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(LimitLoginAuthenticationProvider authenticationProvider) {
		return new ProviderManager(authenticationProvider);
	}
}
