# Functional baseline and migration comparison

This document records how the application behaved **before** the migration (master, Java 7 sources,
Spring 4.3 / Spring Security 4.0, `javax.*`) and compares it with the migrated build (Java 21,
Spring 6.2 / Spring Security 6.5, Jakarta EE 10).

## Environments

| | Baseline | Target |
|---|---|---|
| Source | `master` | this branch |
| JDK (build + run) | Temurin 1.8.0_514 | OpenJDK 21.0.13 |
| Container | Tomcat 9.0.120 | Tomcat 10.1.62 and Tomcat 11.0.29 |
| Database | MySQL 5.7.44 | MySQL 8.0 |
| JDBC driver | Connector/J 5.1.49 (see note) | Connector/J 8.4.0 |
| SMTP | Mailpit (local capture) | Mailpit (local capture) |

Both used `database_scripts/devilsvault_mysql.sql` + `devilsvault_seed.sql` and were deployed with
`scripts/deploy-local.sh`.

## Baseline findings (master)

- `mvn package` on JDK 8 succeeds (`CSE545-SS-0.0.1-SNAPSHOT.war`), but the WAR is **not deployable as built**:
  `src/jdbc/config/DaoDetails.xml` and `database.properties` are not packaged (only Java sources were
  compiled from `src/`), so every DAO lookup fails. The baseline run copied them into `WEB-INF/classes`
  by hand, as the original Eclipse/WTP setup did. The migrated `pom.xml` packages them.
- The bundled Connector/J **5.1.9** cannot authenticate against MySQL 5.5, 5.6, 5.7 or 8.0
  (`Client does not support authentication protocol requested by server`). The baseline run replaced
  it with 5.1.49, the last 5.1.x release, without other changes.
- `devilsvault_internal.sql` does not match the tables used by the DAOs (`users`, `user_attempts`,
  `otp_table`, `bank_accounts`, `transaction_pending`, ...); `devilsvault_mysql.sql` was written
  from the DAO SQL.
- `src/main/resources/mykeystore.jks` is an empty file, so `Encryption.Encode/Decode` returns
  `"encoding error"` until a real AES keystore is supplied.
- The reCAPTCHA result is read in `CustomAuthenticationSuccessHandler` but not enforced (unchanged).

## Access-control matrix

Form login (`/j_spring_security_check`, CSRF token from `/login`), then `GET` of each URL with the
session. `302` = redirect to `/login`, `405` = URL is POST-only and the role is allowed.

| User | Login redirect | customer/home | employee/home | employee/transaction | employee/systemlogs | employee/internalregistrationform | employee/externalregistrationform |
|---|---|---|---|---|---|---|---|
| anonymous | – | 302 | 302 | 302 | 302 | 302 | 302 |
| customer | /customer/home | 200 | 403 | 403 | 403 | 403 | 403 |
| merchant | /customer/home | 200 | 403 | 403 | 403 | 403 | 403 |
| regular | /employee/home | 403 | 200 | 403 | 403 | 403 | 405 |
| manager | /employee/home | 403 | 200 | 200 | 403 | 403 | 405 |
| admin | /employee/home | 403 | 200 | 403 | 200 | 405 | 403 |

Result: **identical** on Tomcat 9 (baseline), Tomcat 10.1 and Tomcat 11 (target).

The same rules (24 role/URL cases), lockout after 3 failures, single concurrent session, CSRF on
login/logout, OTP generation and e-mail, transactions, merchant links, employee authorizations,
AES encryption and reCAPTCHA parsing are covered by the JUnit suite (`mvn clean verify`).
