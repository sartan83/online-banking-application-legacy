# Secure Banking System

Technology Stack: **Java, SpringMVC, MySQL**

## About

A secure banking system (SBS) is a software system developed primarily to facilitate secure banking transactions and user account management through the Internet. A banking organization often needs to track various operations performed by both the internal and external users using the organization’s banking infrastructure. The focus of this project is to develop a SBS to facilitate secure banking transactions and account management required by any banking organization.

## Requirements

| Component | Version |
|-----------|---------|
| JDK | 21 (LTS) |
| Maven | 3.9+ |
| Servlet container | Apache Tomcat 10.1+ (Jakarta EE 10, `jakarta.servlet` 6.0) |
| Database | MySQL 8.x (driver `com.mysql.cj.jdbc.Driver`, Connector/J 8.4) |

Stack: Spring Framework 6.1, Spring Security 6.3, Jakarta Servlet 6.0 / JSTL 3.0,
Jakarta Mail 2.1 (Eclipse Angus), Jakarta JSON-P 2.1 (Eclipse Parsson).

> Tomcat 9.x and older are **not** supported: they implement `javax.servlet`, while the
> application now uses the `jakarta.*` namespace.

## Installation

1. Clone the repo to your machine.
2. Run the database script(s) in `database_scripts/` against a MySQL 8 instance.
3. Update your database config in `src/jdbc/config/database.properties`, e.g.
   ```properties
   database.driver=com.mysql.cj.jdbc.Driver
   database.url=jdbc:mysql://localhost:3306/devilsvault?serverTimezone=UTC
   database.user=...
   database.password=...
   ```
4. Update your SMTP server details (to send OTP) in `src/main/resources/smtp.properties`.
5. Add your keystore file which stores your certificate and private key: `keystore.jks`.
6. Build with JDK 21: `mvn clean verify` (compiles, runs the test suite, produces `target/CSE545-SS.war`).
7. Deploy on Tomcat 10.1+: copy `target/CSE545-SS.war` to `$CATALINA_HOME/webapps/`
   (the app expects the `/CSE545-SS` context path) and start Tomcat with JDK 21
   (`JAVA_HOME=/path/to/jdk-21 $CATALINA_HOME/bin/startup.sh`).
8. Open `http://localhost:8080/CSE545-SS/login`.

## Security configuration

Spring Security is configured in Java (`org.thothlab.devilsvault.config.SecurityConfig`),
registered in the root context through `WEB-INF/spring-security.xml`. The
`springSecurityFilterChain` `DelegatingFilterProxy` in `web.xml` resolves to it.

## Tests

`mvn clean verify` runs JUnit 5 tests (Spring MVC Test + spring-security-test, H2 in-memory
database in MySQL mode, Mockito for the mail transport). No MySQL or SMTP server is needed.

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D
