#!/usr/bin/env python3
"""DevilsVault MySQL -> MongoDB 8 migration.

Reads the relational schema (migration/sql/01_devilsvault_schema.sql) from MySQL and writes the
document model used by the Spring Data MongoDB application (see MAPPING.md), then verifies that
every source row landed in the expected collection/filter.

    python migrate.py --mysql mysql://root:root@localhost:3306/devilsvault \
                      --mongo "mongodb://localhost:27017/devilsvault?replicaSet=rs0&directConnection=true" --drop
"""
import argparse
import datetime as dt
import decimal
import sys
from urllib.parse import unquote, urlparse

import pymysql
import pymysql.cursors
from bson.decimal128 import Decimal128
from pymongo import ASCENDING, MongoClient

TRANSACTION_TABLES = ("transaction_pending", "transaction_completed")
REQUEST_TABLES = ("internal_request_pending", "internal_request_completed",
                  "external_request_pending", "external_request_completed")
AUTHORIZATION_TABLES = ("authorization_pending", "authorization_completed")
LOG_TABLES = ("internal_log", "external_log")

# Spring Data entity classes are not used for queries (no _class needed), but keep it for type info parity.
CLASS = {
    "users": "org.thothlab.devilsvault.db.model.UserAuthentication",
    "external_users": "org.thothlab.devilsvault.db.model.Customer",
    "internal_users": "org.thothlab.devilsvault.db.model.InternalUser",
    "bank_accounts": "org.thothlab.devilsvault.db.model.BankAccountDB",
    "transactions": "org.thothlab.devilsvault.db.model.Transaction",
    "requests": "org.thothlab.devilsvault.db.model.Request",
    "authorizations": "org.thothlab.devilsvault.db.model.Authorization",
    "pending_registrations": "org.thothlab.devilsvault.db.model.PendingRegistration",
    "otp": "org.thothlab.devilsvault.db.model.OTP",
    "system_logs": "org.thothlab.devilsvault.db.model.DatabaseLog",
}


def bson_value(v):
    if isinstance(v, decimal.Decimal):
        return Decimal128(v)
    if isinstance(v, dt.datetime):
        return v
    if isinstance(v, dt.date):
        return dt.datetime(v.year, v.month, v.day)
    return v


def clean(row):
    return {k: bson_value(v) for k, v in row.items() if v is not None}


def stage_of(table):
    return "pending" if table.endswith("_pending") else "completed"


def rows(cur, table):
    cur.execute(f"SELECT * FROM `{table}`")
    return cur.fetchall()


def max_id(cur, table, column):
    cur.execute(f"SELECT COALESCE(MAX(`{column}`), 0) AS m FROM `{table}`")
    return int(cur.fetchone()["m"])


def migrate(cur, db):
    plan = []  # (source table, source count, collection, filter)
    counters = {}

    attempts = {r["username"]: r for r in rows(cur, "user_attempts")}
    users = []
    for r in rows(cur, "users"):
        doc = clean(r)
        doc["_id"] = doc.pop("username")
        a = attempts.get(doc["_id"])
        if a:
            doc["login_attempts"] = clean({"attempts": a["attempts"], "lastModified": a["lastModified"]})
        doc["_class"] = CLASS["users"]
        users.append(doc)
    insert(db, "users", users)
    plan.append(("users", len(users), "users", {}))
    plan.append(("user_attempts", len(attempts), "users", {"login_attempts": {"$exists": True}}))

    merchants = {}
    for r in rows(cur, "authorized_merchants_users"):
        merchants.setdefault(r["external_user_id"], []).append(r["merchant_id"])
    external = []
    for r in rows(cur, "external_users"):
        doc = clean(r)
        doc["_id"] = doc.pop("id")
        if doc["_id"] in merchants:
            doc["authorized_merchants"] = sorted(merchants[doc["_id"]])
        doc["_class"] = CLASS["external_users"]
        external.append(doc)
    insert(db, "external_users", external)
    plan.append(("external_users", len(external), "external_users", {}))
    links = sum(len(v) for v in merchants.values())
    plan.append(("authorized_merchants_users", links, "external_users.authorized_merchants", None))
    counters["external_users"] = max_id(cur, "external_users", "id")

    internal = []
    for r in rows(cur, "internal_user"):
        doc = clean(r)
        doc["_id"] = doc.pop("id")
        doc["_class"] = CLASS["internal_users"]
        internal.append(doc)
    insert(db, "internal_users", internal)
    plan.append(("internal_user", len(internal), "internal_users", {}))
    counters["internal_users"] = max_id(cur, "internal_user", "id")

    cards = {}
    for r in rows(cur, "credit_card_account_details"):
        cards[r["account_number"]] = clean(r)
    accounts = []
    for r in rows(cur, "bank_accounts"):
        doc = clean(r)
        doc["_id"] = doc.pop("account_number")
        if doc["_id"] in cards:
            doc["credit_card"] = cards[doc["_id"]]
        doc["_class"] = CLASS["bank_accounts"]
        accounts.append(doc)
    insert(db, "bank_accounts", accounts)
    plan.append(("bank_accounts", len(accounts), "bank_accounts", {}))
    plan.append(("credit_card_account_details", len(cards), "bank_accounts", {"credit_card": {"$exists": True}}))
    counters["bank_accounts"] = max_id(cur, "bank_accounts", "account_number")
    counters["credit_card_account_details"] = max_id(cur, "credit_card_account_details", "id")

    for table in TRANSACTION_TABLES:
        docs = [dict(clean(r), stage=stage_of(table), critical=bool(r["critical"]), _class=CLASS["transactions"])
                for r in rows(cur, table)]
        insert(db, "transactions", docs)
        plan.append((table, len(docs), "transactions", {"stage": stage_of(table)}))
        counters["transactions"] = max(counters.get("transactions", 0), max_id(cur, table, "id"))

    for table in REQUEST_TABLES:
        scope = table.split("_")[0]
        docs = [dict(clean(r), scope=scope, stage=stage_of(table), _class=CLASS["requests"]) for r in rows(cur, table)]
        insert(db, "requests", docs)
        plan.append((table, len(docs), "requests", {"scope": scope, "stage": stage_of(table)}))
        key = scope + "_requests"
        counters[key] = max(counters.get(key, 0), max_id(cur, table, "id"))

    for table in AUTHORIZATION_TABLES:
        docs = [dict(clean(r), stage=stage_of(table), _class=CLASS["authorizations"]) for r in rows(cur, table)]
        insert(db, "authorizations", docs)
        plan.append((table, len(docs), "authorizations", {"stage": stage_of(table)}))
        counters["authorizations"] = max(counters.get("authorizations", 0), max_id(cur, table, "auth_id"))

    pending = []
    for r in rows(cur, "user_pending"):
        doc = clean(r)
        doc["_id"] = doc.pop("id")
        doc["_class"] = CLASS["pending_registrations"]
        pending.append(doc)
    insert(db, "pending_registrations", pending)
    plan.append(("user_pending", len(pending), "pending_registrations", {}))
    counters["pending_registrations"] = max_id(cur, "user_pending", "id")

    otps = []
    for r in rows(cur, "otp_table"):
        doc = clean(r)
        doc["_id"] = doc.pop("userEmail")
        doc["_class"] = CLASS["otp"]
        otps.append(doc)
    insert(db, "otp", otps)
    plan.append(("otp_table", len(otps), "otp", {}))

    for table in LOG_TABLES:
        scope = table.split("_")[0]
        docs = [dict(clean(r), scope=scope, _class=CLASS["system_logs"]) for r in rows(cur, table)]
        insert(db, "system_logs", docs)
        plan.append((table, len(docs), "system_logs", {"scope": scope}))

    cur.execute("SELECT COUNT(*) AS c FROM account")
    plan.append(("account", cur.fetchone()["c"], "(dropped: unused id table)", None))

    for name, seq in counters.items():
        db.counters.replace_one({"_id": name}, {"_id": name, "seq": seq}, upsert=True)
    return plan, counters


def insert(db, collection, docs):
    if docs:
        db[collection].insert_many(docs)


def create_indexes(db):
    db.transactions.create_index([("stage", ASCENDING), ("id", ASCENDING)], unique=True, name="stage_id")
    db.requests.create_index([("scope", ASCENDING), ("stage", ASCENDING), ("id", ASCENDING)], unique=True,
                             name="scope_stage_id")
    db.authorizations.create_index([("stage", ASCENDING), ("auth_id", ASCENDING)], unique=True, name="stage_auth_id")
    db.bank_accounts.create_index("external_users_id")
    db.external_users.create_index("email", unique=True)
    db.internal_users.create_index("email", unique=True)


def verify(db, plan):
    ok = True
    print(f"{'MySQL table':32} {'rows':>6}  {'MongoDB target':38} {'docs':>6}  check")
    for table, expected, collection, flt in plan:
        if flt is None:
            if collection.startswith("external_users."):
                actual = sum(len(d.get("authorized_merchants", [])) for d in db.external_users.find())
            else:
                actual = expected
        else:
            actual = db[collection].count_documents(flt)
        good = actual == expected
        ok &= good
        target = collection + ("" if not flt else " " + ",".join(f"{k}={v}" for k, v in flt.items()
                                                                  if not isinstance(v, dict)))
        print(f"{table:32} {expected:>6}  {target:38} {actual:>6}  {'OK' if good else 'MISMATCH'}")
    return ok


def mysql_connect(url):
    u = urlparse(url)
    return pymysql.connect(host=u.hostname, port=u.port or 3306, user=unquote(u.username or "root"),
                           password=unquote(u.password or ""), database=u.path.lstrip("/"),
                           cursorclass=pymysql.cursors.DictCursor)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--mysql", required=True, help="mysql://user:pass@host:port/database")
    p.add_argument("--mongo", required=True, help="MongoDB connection string including the database name")
    p.add_argument("--drop", action="store_true", help="drop the target collections first")
    p.add_argument("--if-empty", action="store_true", help="skip when the target database already has users")
    args = p.parse_args()

    client = MongoClient(args.mongo)
    db = client.get_default_database()
    if args.if_empty and db.users.estimated_document_count() > 0:
        print(f"MongoDB database '{db.name}' already populated, nothing to do")
        return 0
    if args.drop:
        for name in list(CLASS) + ["counters"]:
            db.drop_collection(name)
    with mysql_connect(args.mysql) as conn, conn.cursor() as cur:
        plan, counters = migrate(cur, db)
    create_indexes(db)
    ok = verify(db, plan)
    print("counters: " + ", ".join(f"{k}={v}" for k, v in sorted(counters.items())))
    print("VERIFICATION " + ("PASSED" if ok else "FAILED"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
