Terracotta New Relic Plugin
========================================

This plug-in pulls metrics from a running Terracotta Server Array (TSA) at scheduled intervals, 
and makes use of New Relic’s RESTful metric publishing API (at https://platform-api.newrelic.com/platform/v1/metrics) 
to push aggregated metrics to the NewRelic platform.

This plugin is _not officially supported by Terracotta_, and has been tested with various versions of Terracotta 4.x, both Enterprise and OSS.

Note: this plugin will *not* work with Terracotta 3.x and below. 

Please report bugs or feature enhancements directly on this github project.

Prerequisites
-------------

1. A New Relic account. Signup for a free account at http://newrelic.com
2. A configured Java Developer Kit (JDK) - version 1.6 or better
3. Maven 3.x
4. Git
5. Access to a running Terracotta Server (version 4.x)
6. (Only if using Terracotta EE) Access to Terracotta Management Console (TMC)
7. Plugin must be able to connect via HTTP REST to Terracotta or TMC (based on mode of connection chosen)

Building/Installing/Starting the Plugin Agent
-----------------------------------------

1. Clone the latest release tag from the https://github.com/Terracotta-OSS/terracotta-newrelic-plugin to $PLUGIN_SOURCE_DIR.
2. Run 'mvn package' from $PLUGIN_SOURCE_DIR.
3. An archive will be created at $PLUGIN_ROOT/target/tc-nr-plugin-<version>.tar.gz.
4. Extract the archive to $PLUGIN_EXECUTION_DIR.
5. Update permissions on SH / BAT executable script in $PLUGIN_EXECUTION_DIR/bin
6. Configure plugin properties in $PLUGIN_EXECUTION_DIR/conf/application.properties
7. Run start.bat or start.sh

Plugin Properties
--------------------------------------------

While most of the properties have sensible defaults values, 2 properties *must* be updated before starting the plugin:
 - restapi.url (tmc or terracotta rest api url)
 - nr.agent.licenseKey (the license key of the account you want to use to look at the stats in newrelic)
 - nr.environment.prefix (can be left blank, but might be good to specify something qualifying your environment. E.g. PROD_ , TEST_ , DEV_ , etc...)

NOTE: The property prefix "com.saggs.terracotta.nrplugin" is omitted in the table below for clarity.

| Property Name       | Description             | Default Value      |
| ------------------- |:-----------------------:| ------------------:|
| version | Version of this plugin.   |  2.0.2  |
| restapi.url |  Terracotta REST API base url. This can either point at a Terracotta node (only option for Terracotta OSS), or a TMC instance for Terracotta EE  |  CHANGEME  |
| restapi.executor.fixedDelay.milliseconds |  This is the interval for the stats to be pulled from Terracotta  |  60000  |
| restapi.authentication.enabled |  Enable if REST API url is the Terracotta Management Console with authentication  |  false  |
| restapi.authentication.username |  If REST API requires authentication, this is the username that will be used  |    |
| restapi.authentication.password |  If REST API requires authentication, this is the password that will be used  |    |
| restapi.numRelogAttempts |  Number of times the plugin will attempt to authenticate into the TMC before giving up  |  3  |
| restapi.agents.idsPrefix.enabled |  (*TMC access only*) If multiple connections in TMC, you need to enable idsPrefix so the plugin can connect to the right Terracotta connection  |  false  |
| restapi.agents.idsPrefix.value |  (*TMC access only*) If idsPrefix is enabled , this is where you specify what Terracotta TMC connection to attach to  |    |
| nr.executor.fixedDelay.milliseconds |  This is the interval for the stats to be pushed to NewRelic cloud API  |  60000  |
| nr.scheme | HTTP scheme of the NR endpoint   |  https  |
| nr.host |  HTTP hostname of the NR endpoint  |  platform-api.newrelic.com  |
| nr.port |  HTTP port of the NR endpoint  |  443  |
| nr.path |  HTTP path of the NR endpoint  |  /platform/v1/metrics  |
| nr.proxy.enabled | Set to true if the plugin must use a proxy to reach the NR endpoint, false otherwise.   |  false  |
| nr.proxy.scheme |  HTTP scheme of the proxy being used. Ignored if nr.proxy.enabled is set to false.  |    |
| nr.proxy.host |  HTTP host of the proxy being used. Ignored if nr.proxy.enabled is set to false.  |    |
| nr.proxy.port |  HTTP port of the proxy being used. Ignored if nr.proxy.enabled is set to false.  |    |
| nr.environment.prefix | Create an environment prefix when reprting metrics to NewRelic (Especially useful in case of monitoring multiple environments in the same newrelic account)   |  CHANGEME  |
| nr.agent.terracotta.guid |  Terracotta NewRelic Agent GUID as registered in NewRelic plugin central directory   |  com.saggs.terracotta.Terracotta  |
| nr.agent.ehcache.guid |  Ehcache NewRelic Agent GUID as registered in NewRelic plugin central directory  |  com.saggs.terracotta.Ehcache  |
| nr.agent.licenseKey |  New Relic license key so stats are published to the right NewRelic account  |  CHANGEME  |
| data.windowSize |  Maximum number of values per metric that the plug-in will record.  |  100  |

Debugging
--------------------------------------------
This plugin uses SLF4J with Logback binding. To update logging, you can update the logback config file in $PLUGIN_EXECUTION_DIR/conf/logback.xml. 
By defaults, 3 logs are setup with different purpose:

1. tc-response.*.log - Contains the raw stats data in JSON format from Terracotta (or TMC) 
2. nr-request.*.log - Contains the raw stats data in JSON format that gets sent to NewRelic
3. tc-nr-plugin.*.log - Contains the application logs...

Please report bugs or feature enhancements directly on this github project.

Source Code
-----------

This plugin can be found at https://github.com/Terracotta-OSS/terracotta-newrelic-plugin