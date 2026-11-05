package org.thothlab.devilsvault.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Replaces the per-request {@code new ClassPathXmlApplicationContext("jdbc/config/DaoDetails.xml")}
 * lookups of the legacy code: DAOs are resolved by their original bean names from the Boot context.
 */
@Component
public class DaoContext implements ApplicationContextAware, AutoCloseable {

	private static ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		DaoContext.context = applicationContext;
	}

	public static DaoContext open() {
		if (context == null) {
			throw new IllegalStateException("Spring application context not initialised");
		}
		return context.getBean(DaoContext.class);
	}

	public <T> T getBean(String name, Class<T> type) {
		return context.getBean(name, type);
	}

	public <T> T getBean(Class<T> type) {
		return context.getBean(type);
	}

	@Override
	public void close() {
		// beans are container managed; kept for source compatibility with the legacy ctx.close() calls
	}
}
