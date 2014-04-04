@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

setlocal
set PLUGIN_DIR=%~d0%~p0..
set PLUGIN_DIR=%PLUGIN_DIR:"=%

set TC_HOME=@terracotta_home@

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit \b 1
)

set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_COMMAND="%JAVA_HOME%\bin\java"

set JAVA_OPTS=%JAVA_OPTS% -Xms128m -Xmx128m
set JAVA_OPTS=%JAVA_OPTS% -Dlog4j.configuration=%PLUGIN_DIR%\config\log4j.properties -Dlogfile=%PLUGIN_DIR%/logs/terracotta-newrelic-plugin -Dplugin.config.path=%PLUGIN_DIR%\config\plugin.properties -Dnewrelic.platform.config.dir=%PLUGIN_DIR%\config
rem JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.trustStore=some_newlic_cert.jks -Djavax.net.ssl.trustStorePassword=some_password
rem JAVA_OPTS=%JAVA_OPTS% -Dhttp.proxyHost=some_proxy_ip -Dhttp.proxyPort=some_proxy_port
rem JAVA_OPTS=%JAVA_OPTS% -Dcom.newrelic.plugins.terracotta.learningmode=true

echo JAVA_OPTS=
echo %JAVA_OPTS%

echo JAVA_COMMAND=
echo %JAVA_COMMAND%

pause

set CLASSPATH=%PLUGIN_DIR%\libs\*;%EHCACHE_CORE%;%TC_HOME%\lib\tc.jar

:START_TCSERVER
%JAVA_COMMAND% %JAVA_OPTS% -cp %CLASSPATH% com.newrelic.plugins.terracotta.TCL2MonLauncher %*

pause
exit \b %ERRORLEVEL%
endlocal
