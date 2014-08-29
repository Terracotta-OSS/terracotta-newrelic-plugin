Terracotta New Relic Plugin
========================================

This plugin is _not officially supported by Terracotta_, and has been so far tested with Terracotta EE 4.1.1 version.

Prerequisites
-------------

1. A New Relic account. Signup for a free account at http://newrelic.com
2. A configured Java Developer Kit (JDK) - version 1.6 or better
3. Maven 3.x
4. Git
5. Access to a running Terracotta Management Console (TMC)
	
Building/Installing the Plugin Agent
-----------------------------------------

1. Clone the latest release tag from the https://github.com/lanimall/terracotta-newrelic-plugin/tags to $PLUGIN_SOURCE_DIR.
2. Run 'mvn package' from $PLUGIN_SOURCE_DIR.
3. An archive will be created at $PLUGIN_ROOT/target/tc-nr-plugin-<version>.tar.gz.
4. Extract the archive to $PLUGIN_EXECUTION_DIR.

Starting the Java plugin agent
--------------------------------------------

1. Configure $PLUGIN_EXECUTION_DIR/conf/application.properties:
    a. com.saggs.terracotta.nrplugin.tmc.url - this can either point at a TC instance or a TMC
    b. com.saggs.terracotta.nrplugin.nr.name - unique name for your project
    c. com.saggs.terracotta.nrplugin.nr.guid - unique guid for your project
    d. com.saggs.terracotta.nrplugin.nr.licenseKey - your NewRelic license key
2. Optionally configure logging in $PLUGIN_EXECUTION_DIR/conf/logback.xml
3. Run start-terracotta-newrelic-plugin.bat or start-terracotta-newrelic-plugin.sh

Source Code
-----------

This plugin can be found at https://github.com/Terracotta-OSS/terracotta-newrelic-plugin