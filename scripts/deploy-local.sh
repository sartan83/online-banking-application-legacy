#!/usr/bin/env bash
# Deploys a CSE545-SS WAR into a local Tomcat with runtime config (DB, SMTP, AES keystore)
# injected into WEB-INF/classes. Nothing secret is written back into the repository.
#
#   DB_URL=jdbc:mysql://localhost:3306/devilsvault DB_USER=devilsvault DB_PASSWORD=... \
#   SMTP_HOST=localhost SMTP_PORT=1025 SMTP_AUTH=false SMTP_STARTTLS=false \
#   scripts/deploy-local.sh target/CSE545-SS.war /opt/tomcat
set -euo pipefail

WAR=${1:?usage: deploy-local.sh <war> <catalina_home>}
CATALINA_HOME=${2:?usage: deploy-local.sh <war> <catalina_home>}
APP_DIR="$CATALINA_HOME/webapps/CSE545-SS"
CLASSES="$APP_DIR/WEB-INF/classes"

rm -rf "$APP_DIR" "$CATALINA_HOME/webapps/CSE545-SS.war"
mkdir -p "$APP_DIR"
(cd "$APP_DIR" && jar xf "$(realpath "$OLDPWD/$WAR" 2>/dev/null || realpath "$WAR")")

mkdir -p "$CLASSES/jdbc/config"
cat > "$CLASSES/jdbc/config/database.properties" <<PROPS
database.driver=${DB_DRIVER:-com.mysql.cj.jdbc.Driver}
database.url=${DB_URL:?DB_URL is required}
database.user=${DB_USER:?DB_USER is required}
database.password=${DB_PASSWORD:?DB_PASSWORD is required}
PROPS

cat > "$CLASSES/smtp.properties" <<PROPS
host=${SMTP_HOST:-smtp.gmail.com}
port=${SMTP_PORT:-587}
auth=${SMTP_AUTH:-true}
starttls=${SMTP_STARTTLS:-true}
from=${SMTP_FROM:-securebanking.ss4@gmail.com}
username=${SMTP_USERNAME:-}
password=${SMTP_PASSWORD:-}
PROPS

if [ -n "${KEYSTORE_FILE:-}" ]; then
  cp "$KEYSTORE_FILE" "$CLASSES/mykeystore.jks"
elif [ ! -s "$CLASSES/mykeystore.jks" ]; then
  echo "No AES keystore supplied; generating a throwaway PKCS12 key for this deployment"
  rm -f "$CLASSES/mykeystore.jks"
  keytool -genseckey -alias mykey -keyalg AES -keysize 128 -storetype PKCS12 \
    -keystore "$CLASSES/mykeystore.jks" -storepass password -keypass password
fi

echo "Deployed to $APP_DIR"
