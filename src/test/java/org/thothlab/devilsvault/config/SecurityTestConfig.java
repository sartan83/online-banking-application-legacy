package org.thothlab.devilsvault.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thothlab.devilsvault.controllers.security.CustomAuthenticationSuccessHandler;
import org.thothlab.devilsvault.support.TestDatabase;

/** Minimal web context: the production {@link SecurityConfig} plus test stand-ins for the XML beans it needs. */
@Configuration
@EnableWebMvc
@Import(SecurityConfig.class)
public class SecurityTestConfig {

	@Bean
	public DataSource securitydataSource() {
		DataSource dataSource = TestDatabase.dataSource();
		TestDatabase.reset(dataSource);
		return dataSource;
	}

	@Bean
	public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
		return new CustomAuthenticationSuccessHandler();
	}

	/** Registered as a member component of this configuration class. */
	@RestController
	public static class ProbeController {
		@GetMapping({ "/login", "/employee/**", "/customer/**", "/public/**" })
		public String ok() {
			return "ok";
		}
	}
}
