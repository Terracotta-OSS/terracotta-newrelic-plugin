Terracotta New Relic Plugin
========================================

This plugin is _not officially supported by Terracotta_, and has been so far tested with Terracotta 4.x Enterprise and OSS versions.

Prerequisites
-------------

1. A New Relic account. Signup for a free account at http://newrelic.com
2. A configured Java Developer Kit (JDK) - version 1.6 or better
3. Maven 3.x
4. Git
5. Access to a running Terracotta Server
6. (Only if using Terracotta EE) Access to Terracotta Management Console (TMC)
	
Building/Installing the Plugin Agent
-----------------------------------------

1. Clone the latest release tag from the https://github.com/Terracotta-OSS/terracotta-newrelic-plugin to $PLUGIN_SOURCE_DIR.
2. Run 'mvn package' from $PLUGIN_SOURCE_DIR.
3. An archive will be created at $PLUGIN_ROOT/target/tc-nr-plugin-<version>.tar.gz.
4. Extract the archive to $PLUGIN_EXECUTION_DIR.

Starting the Java plugin agent
--------------------------------------------

1. Configure mandatory properties in $PLUGIN_EXECUTION_DIR/conf/application.properties:
    1. com.saggs.terracotta.nrplugin.restapi.url - This can either point at a Terracotta node (only option for Terracotta OSS), or a TMC instance for Terracotta EE
    2. com.saggs.terracotta.nrplugin.nr.agent.licenseKey - Your NewRelic license key
2. Configure optional properties specific to environment
    1. com.saggs.terracotta.nrplugin.nr.environment.prefix - Create an environment prefix (Especially useful in case of monitoring multiple environments in the same newrelic account)
    2. com.saggs.terracotta.nrplugin.restapi.executor.fixedDelay.milliseconds - This is the interval for stats to be pulled out of Terracotta
    3. com.saggs.terracotta.nrplugin.restapi.executor.fixedDelay.milliseconds - This is the interval for stats to be pulled out of Terracotta
    4. com.saggs.terracotta.nrplugin.nr.executor.fixedDelay.milliseconds - This is the interval for the stats to be pushed to NewRelic cloud API
    5. com.saggs.terracotta.nrplugin.restapi.authentication.enabled - Enable if REST API url is the Terracotta Management Console with authentication
    6. com.saggs.terracotta.nrplugin.restapi.authentication.username - If REST API requires authentication, this is the username
    7. com.saggs.terracotta.nrplugin.restapi.authentication.password - If REST API requires authentication, this is the password
    8. com.saggs.terracotta.nrplugin.nr.proxy.enabled - Set to true if your environment requires proxy to get out to the public internet (and if set to true, set the other "com.saggs.terracotta.nrplugin.nr.proxy.*" configs...) 
3. Optionally configure logging in $PLUGIN_EXECUTION_DIR/conf/logback.xml. 3 logs are setup:
    1. tc-response.*.log - Contains the raw stats data in JSON format from Terracotta (or TMC) 
    2. nr-request.*.log - Contains the raw stats data in JSON format that gets sent to NewRelic
    3. tc-nr-plugin.*.log - Contains the application logs...
4. Run start.bat or start.sh

Source Code
-----------

This plugin can be found at https://github.com/Terracotta-OSS/terracotta-newrelic-plugin