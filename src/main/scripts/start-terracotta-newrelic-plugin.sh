#!/bin/sh

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx512m"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

#if test \! -d "${TC_HOME}"; then
#  echo "$0: the TC_HOME environment variable is not defined correctly"
#  exit 2
#fi

if [ ! -d "log" ]; then
    echo "Creating log directory."
    mkdir -p "log";
fi

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx512m -Djavax.net.ssl.keyStore=conf/geotrust.jks \
-Djavax.net.ssl.keyStorePassword=password -Djavax.net.ssl.trustStore=conf/geotrust.jks \
 -Djavax.net.ssl.trustStorePassword=password"
JAVA_EXEC="${JAVA_HOME}/bin/java ${JAVA_OPTS} -Djava.library.path=bin -cp lib/*:conf com.terracotta.nrplugin.app.Main"

echo "Starting Terracotta New Relic Plug-in..."
nohup $JAVA_EXEC > log/tc-nr-plugin.out 2>&1 log/tc-nr-plugin.err &
echo $! > tc-nr-plugin.pid