#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

PLUGIN_DIR=`dirname "$0"`/..

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
[ -n "$PLUGIN_DIR" ] && PLUGIN_DIR=`cygpath -d "$PLUGIN_DIR"`
fi

if [ -f "${PLUGIN_DIR}/bin/setenv.sh" ]; then
  . "${PLUGIN_DIR}/bin/setenv.sh"
fi

if test \! -d "${JAVA_HOME}"; then
echo "$0: the JAVA_HOME environment variable is not defined correctly"
exit 2
fi

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx128m"
JAVA_OPTS="${JAVA_OPTS} -Dlog4j.configuration=file:${PLUGIN_DIR}/config/log4j.properties -Dlogfile=${PLUGIN_DIR}/logs/terracotta-newrelic-plugin -Dplugin.config.path=file:${PLUGIN_DIR}/config/plugin.properties -Dnewrelic.platform.config.dir=${PLUGIN_DIR}/config"
JAVA_OPTS="${JAVA_OPTS} -Dplugin.config.path=file:${PLUGIN_DIR}/config/plugin.properties -Dnewrelic.platform.config.dir=${PLUGIN_DIR}/config"
JAVA_OPTS="${JAVA_OPTS} ${PLUGIN_OPTS}"

CLASSPATH="${PLUGIN_DIR}/libs/*:${EHCACHE_CORE}:${TC_HOME}/lib/tc.jar"

exec "${JAVA_HOME}/bin/java" \
${JAVA_OPTS} \
-cp ${CLASSPATH} \
com.newrelic.plugins.terracotta.TCL2MonLauncher "$@"