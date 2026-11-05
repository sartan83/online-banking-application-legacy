# DevilsVault: relational schema -> MongoDB document model

Integer ids are preserved; new ids come from the `counters` collection
(`{_id: <sequence>, seq: <int>}`, the AUTO_INCREMENT equivalent), so URLs, JSP forms and `int`
DAO signatures keep working unchanged. Field names keep the original
column names.

Where several tables were merged into one collection (transactions, requests,
authorizations) the old tables had independent id sequences, so `_id` is an ObjectId and the
legacy id is kept in `id` / `auth_id`, unique per `stage` (and `scope`) through a compound
unique index. Single-table collections use the legacy key as `_id`.

Sequences: `external_users`, `internal_users`, `bank_accounts`, `credit_card_account_details`,
`pending_registrations`, and one shared sequence per merged pair: `transactions`,
`internal_requests`, `external_requests`, `authorizations` (seeded with the max id of both
legacy tables). Sharing it matters: approval copies a pending row into the completed set *keeping
its id* (`saveToCompleted`, `RequestDaoImpl.save`); with the two independent MySQL
AUTO_INCREMENTs that insert failed with a duplicate key as soon as a completed row with the same
id already existed.

| MySQL table(s)                                        | MongoDB collection        | Notes |
|-------------------------------------------------------|---------------------------|-------|
| `users`                                               | `users` (`_id` = username)| auth flags (`enabled`, `accountNonLocked`, `otpNonLocked`, ...) |
| `user_attempts`                                       | embedded `users.login_attempts` `{attempts, lastModified}` | 1:1 with users |
| `external_users`                                      | `external_users`          | customers + merchants |
| `authorized_merchants_users`                          | embedded `external_users.authorized_merchants: [merchant_id]` | N:M stored on the customer side |
| `internal_user`                                       | `internal_users`          | employees |
| `bank_accounts`                                       | `bank_accounts` (`_id` = account_number) | ref `external_users_id` |
| `credit_card_account_details`                         | embedded `bank_accounts.credit_card` on the CREDIT account | 1:1 with account_number |
| `transaction_pending`, `transaction_completed`        | `transactions` + `stage: pending|completed` | approval = new completed doc, pending removed (as before) |
| `internal_request_pending/_completed`, `external_request_pending/_completed` | `requests` + `scope: internal|external`, `stage: pending|completed` | |
| `authorization_pending`, `authorization_completed`    | `authorizations` + `stage` | `_id` = auth_id |
| `user_pending`                                        | `pending_registrations`   | |
| `otp_table`                                           | `otp` (`_id` = userEmail) | |
| `internal_log`, `external_log`                        | `system_logs` + `scope: internal|external` | |
| `account`                                             | (dropped)                 | unused id table |
