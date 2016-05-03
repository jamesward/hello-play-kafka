#!/usr/bin/env bash

set -e

client_key="KAFKA_CLIENT_CERT_KEY"
client_cert="KAFKA_CLIENT_CERT"
trusted_cert="KAFKA_TRUSTED_CERT"

[ -z $TRUSTSTORE_PASSWORD ] && {
  echo "TRUSTSTORE_PASSWORD is missing" >&2
  exit 1
}

[ -z $KEYSTORE_PASSWORD ] && {
  echo "KEYSTORE_PASSWORD is missing" >&2
  exit 1
}

rm -f .{keystore,truststore}.{pem,pkcs12,jks}

echo -n "${!client_key}" >> .keystore.pem
echo -n "${!client_cert}" >> .keystore.pem
echo -n "${!trusted_cert}" > .truststore.pem

keytool -importcert -file .truststore.pem -keystore .truststore.jks -deststorepass $TRUSTSTORE_PASSWORD -noprompt

openssl pkcs12 -export -in .keystore.pem -out .keystore.pkcs12 -password pass:$KEYSTORE_PASSWORD
keytool -importkeystore -srcstoretype PKCS12 \
    -destkeystore .keystore.jks -deststorepass $KEYSTORE_PASSWORD \
    -srckeystore .keystore.pkcs12 -srcstorepass $KEYSTORE_PASSWORD

rm -f .{keystore,truststore}.{pem,pkcs12}
