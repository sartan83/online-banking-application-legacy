# Secure Banking System

Technology Stack: **Java, SpringMVC, MySQL**

## About

A secure banking system (SBS) is a software system developed primarily to facilitate secure banking transactions and user account management through the Internet. A banking organization often needs to track various operations performed by both the internal and external users using the organization’s banking infrastructure. The focus of this project is to develop a SBS to facilitate secure banking transactions and account management required by any banking organization.

## Requirements

| Component | Version |
|-----------|---------|
| JDK | 21 (LTS) |
| Maven | 3.9+ |
| Servlet container | Apache Tomcat 10.1 (Jakarta EE 10) or Tomcat 11 (Jakarta EE 11) |
| Database | MySQL 8.x (driver `com.mysql.cj.jdbc.Driver`, Connector/J 8.4) |

Stack: Spring Framework 6.2, Spring Security 6.5, Jakarta Servlet 6.0 / JSTL 3.0,
Jakarta Mail 2.1 (Eclipse Angus), Jakarta JSON-P 2.1 (Eclipse Parsson), Hibernate Validator 8.

> Tomcat 9.x and older are **not** supported: they implement `javax.servlet`, while the
> application now uses the `jakarta.*` namespace.

## Installation

1. Create the schema and (optionally) demo data on MySQL 8:
   ```sh
   mysql -uroot -p < database_scripts/devilsvault_mysql.sql
   mysql -uroot -p < database_scripts/devilsvault_seed.sql   # demo users, password Passw0rd!
   ```
   `devilsvault_internal.sql` is the historical script and does not match the tables used by the DAOs.
2. Build with JDK 21: `mvn clean verify` (compiles with `--release 21`, runs the test suite,
   produces `target/CSE545-SS.war`).
3. Deploy on Tomcat 10.1/11 with runtime configuration injected into `WEB-INF/classes`
   (no credentials are stored in the repository):
   ```sh
   DB_URL=jdbc:mysql://localhost:3306/devilsvault DB_USER=devilsvault DB_PASSWORD=... \
   SMTP_USERNAME=... SMTP_PASSWORD=... KEYSTORE_FILE=/secure/mykeystore.p12 \
   scripts/deploy-local.sh target/CSE545-SS.war $CATALINA_HOME
   JAVA_HOME=/path/to/jdk-21 $CATALINA_HOME/bin/startup.sh
   ```
   The app must run under the `/CSE545-SS` context path (redirects are hard-coded).
4. Open `http://localhost:8080/CSE545-SS/login`.

### Runtime configuration

| File (in `WEB-INF/classes`) | Keys |
|-----------------------------|------|
| `jdbc/config/database.properties` | `database.driver`, `database.url`, `database.user`, `database.password` |
| `smtp.properties` | `username`, `password`; optional `host` (smtp.gmail.com), `port` (587), `auth` (true), `starttls` (true), `from` |
| `mykeystore.jks` | AES secret key, alias `mykey`, used by `Encryption` for SSN/DOB fields |

`Encryption` reads the keystore as **PKCS12** (the JDK default since 9) and falls back to the
legacy **JCEKS** format, so existing keystores keep working on JDK 21. To create or convert one:
```sh
keytool -genseckey -alias mykey -keyalg AES -keysize 128 -storetype PKCS12 -keystore mykeystore.jks
keytool -importkeystore -srckeystore old.jceks -srcstoretype JCEKS -destkeystore mykeystore.jks -deststoretype PKCS12
```
The key must stay the same across the migration, otherwise previously encrypted data cannot be decrypted.
The repository copy of `src/main/resources/mykeystore.jks` is empty; supply the real one at deploy time.

### TLS

HTTPS is terminated by Tomcat. Configure a PKCS12 certificate keystore in `conf/server.xml`
(Tomcat 10.1+/11 syntax):
```xml
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol" SSLEnabled="true">
  <SSLHostConfig protocols="TLSv1.2+TLSv1.3">
    <Certificate certificateKeystoreFile="conf/keystore.p12" certificateKeystoreType="PKCS12"
                 certificateKeystorePassword="..." />
  </SSLHostConfig>
</Connector>
```
Convert an old `keystore.jks` with `keytool -importkeystore -srckeystore keystore.jks -destkeystore keystore.p12 -deststoretype PKCS12`.

## Security configuration

Spring Security is configured in Java (`org.thothlab.devilsvault.config.SecurityConfig`,
`SecurityFilterChain` with `PathPatternRequestMatcher`), registered in the root context through
`WEB-INF/spring-security.xml`. The `springSecurityFilterChain` `DelegatingFilterProxy` in `web.xml`
resolves to it. Login form, URL rules, lockout, single-session and CSRF behaviour are unchanged;
see [docs/BASELINE.md](docs/BASELINE.md) for the before/after access-control comparison.

## Tests

`mvn clean verify` runs the JUnit 5 suite (Spring MVC Test + spring-security-test, H2 in-memory
database in MySQL mode, Mockito for the mail transport, a local fake reCAPTCHA endpoint). No MySQL,
SMTP server or Internet access is needed. CI runs the same build on JDK 21 (`.github/workflows/build.yml`).

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D
