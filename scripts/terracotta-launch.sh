#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx512m -XX:MaxDirectMemorySize=10G"
TC_HOME="/Applications/terracotta/terracotta-ee-3.7.6"

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

CLASSPATH="${PLUGIN_DIR}/libs/terracotta-1.0.2.jar:${PLUGIN_DIR}/libs/metrics_publish-1.2.0.jar:${PLUGIN_DIR}/libs/terracotta-jmxremote-3.6-runtime-1.0.0.jar:${TC_HOME}/lib/tc.jar:${TC_HOME}/lib/slf4j-api-1.6.1.jar"

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
[ -n "$PLUGIN_DIR" ] && PLUGIN_DIR=`cygpath -d "$PLUGIN_DIR"`
fi

echo ${CLASSPATH}

exec "${JAVA_HOME}/bin/java" \
${JAVA_OPTS} -Dnewrelic.platform.config.dir="${PLUGIN_DIR}/config" \
-cp ${CLASSPATH} \
com.newrelic.plugins.terracotta.TCL2MonLauncher "$@"