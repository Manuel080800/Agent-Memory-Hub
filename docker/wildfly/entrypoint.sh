#!/bin/sh
set -eu

configuration_marker="/opt/jboss/wildfly/standalone/configuration/.agent-memory-hub-configured"
if [ ! -f "$configuration_marker" ]; then
    /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/wildfly/configure.cli
    touch "$configuration_marker"
fi

exec /opt/jboss/wildfly/bin/standalone.sh --server-config=standalone.xml -b=0.0.0.0 -bmanagement=127.0.0.1
