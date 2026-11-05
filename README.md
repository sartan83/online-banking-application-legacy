# Secure Banking System

Technology Stack: **Java 21, Spring Boot 3, MongoDB 8**

## About

A secure banking system (SBS) is a software system developed primarily to facilitate secure banking transactions and user account management through the Internet. A banking organization often needs to track various operations performed by both the internal and external users using the organization’s banking infrastructure. The focus of this project is to develop a SBS to facilitate secure banking transactions and account management required by any banking organization.

## Stack

Java 21, Spring Boot 3.5 (Spring MVC + JSP/JSTL on embedded Tomcat, Spring Security 6), Spring Data MongoDB on
MongoDB 8.0 (single-node replica set, used for multi-document transactions). The original Java 7 / Spring 4 /
JdbcTemplate / MySQL version is in the git history; the table -> collection mapping is in
[`migration/MAPPING.md`](migration/MAPPING.md).

## Run with Docker Compose

```bash
docker compose up --build
```

- App: http://localhost:8080/CSE545-SS/login
- Mail (OTP / notifications, Mailpit): http://localhost:8025
- Sample data: `legacy-mysql` loads the relational schema and sample rows from `migration/sql/`, then the one-shot
  `migrator` service copies them into MongoDB with `migration/migrate.py` and verifies row/document counts
  (skipped when MongoDB already has users; `docker compose down -v` resets).
- Behind a Maven Central rate limit: `MAVEN_MIRROR_URL=<mirror> docker compose up --build`.
  On Linux kernels >= 6.19 use `MONGO_IMAGE=mongo:8.2`.

Sample logins (password `Passw0rd@` for all):

| user | role |
|------|------|
| alice@devilsvault.com, bob@devilsvault.com | ROLE_CUSTOMER |
| shop@devilsvault.com | ROLE_MERCHANT |
| carol@devilsvault.com | ROLE_CUSTOMER (locked after 3 failed logins) |
| regular@devilsvault.com | ROLE_REGULAR (authorised for Alice and Bob transactions) |
| manager@devilsvault.com | ROLE_MANAGER |
| admin@devilsvault.com | ROLE_ADMIN |

## Run from sources

```bash
docker compose up -d mongo mailpit legacy-mysql migrator   # MongoDB + sample data
SMTP_HOST=localhost SMTP_PORT=1025 SMTP_AUTH=false SMTP_STARTTLS=false mvn spring-boot:run
```

`MONGODB_URI` defaults to `mongodb://localhost:27017/devilsvault?replicaSet=rs0&directConnection=true`.
SMTP settings come from `src/main/resources/smtp.properties` (Gmail defaults) and can be overridden with
`SMTP_HOST`, `SMTP_PORT`, `SMTP_AUTH`, `SMTP_STARTTLS`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`.

## Tests

```bash
mvn verify
```

JUnit 5 + Testcontainers (`mongo:8.0` replica set, override with `-Dmongodb.image=...`): `@DataMongoTest` DAO
tests (`*Test`, surefire) and MockMvc flow tests for login/lockout, transfer + approval/rejection and change
requests (`*IT`, failsafe), under `src/org/thothlab/devilsvault/test`.

## Data migration (MySQL -> MongoDB)

```bash
pip install -r migration/requirements.txt
python migration/migrate.py --mysql mysql://root:root@localhost:3306/devilsvault \
  --mongo "mongodb://localhost:27017/devilsvault?replicaSet=rs0&directConnection=true" --drop
```

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D
