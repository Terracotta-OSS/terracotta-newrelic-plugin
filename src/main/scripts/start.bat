@echo off

setlocal

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit \b 1
)

set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_COMMAND="%JAVA_HOME%\bin\java"
set JAVA_OPTS=-Xms128m -Xmx512m -Djavax.net.ssl.trustStore=conf/geotrust.jks -Djavax.net.ssl.trustStorePassword=password

%JAVA_COMMAND% %JAVA_OPTS% -Djava.library.path=bin -cp "lib\*;conf" com.terracotta.nrplugin.app.Main 

pause

exit \b %ERRORLEVEL%
endlocal