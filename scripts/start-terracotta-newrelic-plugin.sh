#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

TC_HOME="@terracotta_home@"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
echo "$0: the JAVA_HOME environment variable is not defined correctly"
exit 2
fi

PLUGIN_DIR=`dirname "$0"`/..

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx128m"
JAVA_OPTS="${JAVA_OPTS} -Dlog4j.configuration=file:${PLUGIN_DIR}/config/log4j.properties -Dplugin-log-path=${PLUGIN_DIR}/logs -Dplugin.config.path=file:${PLUGIN_DIR}/config/plugin.properties -Dnewrelic.platform.config.dir=${PLUGIN_DIR}/config"
#JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=some_newlic_cert.jks -Djavax.net.ssl.trustStorePassword=some_password"
#JAVA_OPTS="${JAVA_OPTS} -Dhttp.proxyHost=some_proxy_ip -Dhttp.proxyPort=some_proxy_port"
#JAVA_OPTS="${JAVA_OPTS} -Dcom.newrelic.plugins.terracotta.learningmode=true"

CLASSPATH="${PLUGIN_DIR}/libs/*:${TC_HOME}/ehcache/lib/ehcache-core-ee-2.6.8.jar:${TC_HOME}/lib/tc.jar"

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
[ -n "$PLUGIN_DIR" ] && PLUGIN_DIR=`cygpath -d "$PLUGIN_DIR"`
fi

echo ${CLASSPATH}

exec "${JAVA_HOME}/bin/java" \
${JAVA_OPTS} \
-cp ${CLASSPATH} \
com.newrelic.plugins.terracotta.TCL2MonLauncher "$@"