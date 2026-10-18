#!/bin/sh
set -eu

config_dir="$(dirname "$0")/../config-local"
mkdir -p "$config_dir"
config_dir="$(cd "$config_dir" && pwd)"
tls_dir="$config_dir/tls"
runtime_config="$config_dir/runtime.env"

mkdir -p "$tls_dir"
chmod 700 "$config_dir" "$tls_dir"

if [ ! -f "$runtime_config" ]; then
    database_password="$(openssl rand -hex 32)"
    keystore_password="$(openssl rand -hex 24)"
    truststore_password="$(openssl rand -hex 24)"
    cat > "$runtime_config" <<EOF
POSTGRES_DB=agent_memory_hub
POSTGRES_USER=amh_app
POSTGRES_PASSWORD=$database_password
AMH_DB_USERNAME=amh_app
AMH_DB_PASSWORD=$database_password
AMH_TLS_KEYSTORE=/run/amh/tls/localhost.p12
AMH_TLS_KEYSTORE_PASSWORD=$keystore_password
AMH_TLS_TRUSTSTORE=/run/amh/tls/truststore.p12
AMH_TLS_TRUSTSTORE_PASSWORD=$truststore_password
EOF
    chmod 600 "$runtime_config"
fi

set -a
. "$runtime_config"
set +a

if [ ! -f "$tls_dir/localhost.p12" ] || [ ! -f "$tls_dir/truststore.p12" ]; then
    openssl req -x509 -newkey rsa:3072 -sha256 -days 825 -nodes \
        -keyout "$tls_dir/localhost.key" \
        -out "$tls_dir/localhost.crt" \
        -subj "/CN=localhost" \
        -addext "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1"
    openssl pkcs12 -export \
        -out "$tls_dir/localhost.p12" \
        -inkey "$tls_dir/localhost.key" \
        -in "$tls_dir/localhost.crt" \
        -name agent-memory-hub-local \
        -passout "pass:$AMH_TLS_KEYSTORE_PASSWORD"
    keytool -importcert -noprompt \
        -alias agent-memory-hub-local \
        -file "$tls_dir/localhost.crt" \
        -keystore "$tls_dir/truststore.p12" \
        -storetype PKCS12 \
        -storepass "$AMH_TLS_TRUSTSTORE_PASSWORD"
    chmod 600 "$tls_dir/localhost.key" "$tls_dir/localhost.p12" "$tls_dir/truststore.p12"
    chmod 644 "$tls_dir/localhost.crt"
fi

printf '%s\n' "Local config and TLS files are ready in $config_dir." \
    "Execute sql/001_schema.sql manually before starting WildFly." \
    "Then run: docker compose up --build -d"
