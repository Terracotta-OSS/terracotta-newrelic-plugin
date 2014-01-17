Terracotta New Relic Plugin
========================================

This plugin is _not officially supported by Terracotta_, and has been so far tested with Terracotta EE 3.7.x version.

Prerequisites
-------------

1. A New Relic account. Signup for a free account at http://newrelic.com
2. A configured Java Developer Kit (JDK) - version 1.6 or better
3. The Ant build tool - version 1.8 or better
4. Git
5. Terracotta product installed
	
Building/Installing the Plugin Agent
-----------------------------------------

1. Download the latest release tag from the https://github.com/lanimall/terracotta-newrelic-plugin/tags
2. Extract the archive to the location of choice (easiest is to have Terracotta product installed on that box for building/running)
3. Copy `template-build.properties` to `build.properties`
4. Edit `build.properties` and update `terracotta.home` value to be the root install path of Terracotta (edit `plugin.deploy` with the path of your choice)
3. From your shell run: `ant` to build the plugin (optional: `ant deploy` will also extract the dist package to the `plugin.deploy` location)
4. A tar archive will be placed in the `dist` folder with the pattern `terracotta-newrelic-plugin-X.Y.Z.tar.gz`.
5. Extract the tar file to a location where you want to run the plugin agent from (`ant deploy` would have done this automatically)
6. The extracted package should have 3 subfolder: "bin", "config", "libs" (each folder's usage is hopefully self-explanatory)
7. Almost there...now you need to configure it.

Configuring/Starting the Java plugin agent
--------------------------------------------

1. Copy `config/template_newrelic.properties` to `config/newrelic.properties`
2. Edit `config/newrelic.properties` and replace `YOUR_LICENSE_KEY_HERE` with your New Relic license key
3. Copy `config/template-com.newrelic.plugins.terracotta.json` to `config/com.newrelic.plugins.terracotta.json`
4. Edit `config/com.newrelic.plugins.terracotta.json` and update it with all the nodes you want to monitor in your Terracotta Cluster.
5. Optional: Copy `config/template-logging.properties` to `config/logging.properties`
6. Optional: Edit `config/logging.properties` and update it appropriately based on the level of "New Relic"-specific logging you need.
7. If the plugin _is not_ installed on a server where Terracotta is installed, you'll need to edit the startup script at `bin/start-terracotta-newrelic-plugin.sh` and fix the paths to the needed Terracotta libs.
8. If the plugin _is_ installed on a server where Terracotta is installed, check that the `TC_HOME` value is accurate in the startup script at `bin/start-terracotta-newrelic-plugin.sh` 
9. From your shell, navigate to `bin/` folder and run: `sh start-terracotta-newrelic-plugin.sh`
10. Look for error-free startup messages on stdout (also check the log files in the `bin/` folder: terracotta-newrelic-plugin.log, newrelic.log)
11. Wait a few minutes for New Relic to start processing the data sent from your agent.
12. Sign in to your New Relic account.
13. From the New Relic menu bar, look for the "Terracotta" item.
14. To view your plugin's summary page, click "Terracotta".

Source Code
-----------

This plugin can be found at https://github.com/Terracotta-OSS/terracotta-newrelic-plugin