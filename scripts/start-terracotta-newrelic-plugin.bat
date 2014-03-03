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

set JAVA_OPTS=-Dlog4j.configuration=%PLUGIN_DIR%\config\log4j.properties -Dplugin.config.path=%PLUGIN_DIR%\config\plugin.properties -Dnewrelic.platform.config.dir=%PLUGIN_DIR%\config
rem JAVA_OPTS=%JAVA_OPTS% -Dcom.newrelic.plugins.terracotta.learningmode=true -Xms128m -Xmx512m
echo JAVA_OPTS=
echo %JAVA_OPTS%

echo JAVA_COMMAND=
echo %JAVA_COMMAND%

pause

set CLASSPATH=%PLUGIN_DIR%\libs\@plugin_jar_file@;%PLUGIN_DIR%\libs\@newrelic_plugin_sdk_jar@;%PLUGIN_DIR%\libs\@terracotta_jmxremot_jar@;%PLUGIN_DIR%\libs\@commons_math3_jar@;%TC_HOME%\ehcache\lib\ehcache-core-ee-2.6.7.jar;%TC_HOME%\lib\tc.jar;%TC_HOME%\lib\slf4j-api-1.6.1.jar;%TC_HOME%\lib\slf4j-log4j12-1.6.1.jar

:START_TCSERVER
%JAVA_COMMAND% %JAVA_OPTS% -cp %CLASSPATH% com.newrelic.plugins.terracotta.TCL2MonLauncher %*

pause
exit \b %ERRORLEVEL%
endlocal
