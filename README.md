Terracotta New Relic Plugin
========================================

This plugin is *not* officially supported by Terracotta, and has been so far tested with Terracotta EE 3.7.x version.

Prerequisites
-------------

1. A New Relic account. Signup for a free account at http://newrelic.com
2. A configured Java Developer Kit (JDK) - version 1.6 or better
3. The Ant build tool - version 1.8 or better
4. Git
	
Building/Installing the Plugin Agent
-----------------------------------------

1. Download the latest release tag from the https://github.com/lanimall/terracotta-newrelic-plugin/tags
2. Extract the archive to the location of choice
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
4. Edit `config/com.newrelic.plugins.terracotta.json` and update it with all the nodes in your Terracotta Cluster.
5. Optional: Copy `config/template-logging.properties` to `config/logging.properties`
6. Optional: Edit `config/logging.properties` and update it appropriately based on the level of "New Relic"-specific logging you need.
7. From your shell, navigate to `bin/` folder and run: `sh start-terracotta-newrelic-plugin.sh`
8. Look for error-free startup messages on stdout (also check the log files in the `bin/` folder: terracotta-newrelic-plugin.log, newrelic.log)
9. Wait a few minutes for New Relic to start processing the data sent from your agent.
10. Sign in to your New Relic account.
11. From the New Relic menu bar, look for the "Terracotta" item.
12. To view your plugin's summary page, click "Terracotta".

Source Code
-----------

This plugin can be found at https://github.com/lanimall/terracotta-newrelic-plugin