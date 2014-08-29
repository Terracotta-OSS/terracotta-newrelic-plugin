#!/bin/sh

TC_HOME=/opt/terracotta/bigmemory-max-4.1.3
nohup $TC_HOME/server/bin/start-tc-server.sh > $TC_HOME/server/logs/stdout.log 2>&1 $TC_HOME/server/logs/stderr.log &
nohup $TC_HOME/tools/management-console/bin/start-tmc.sh > $TC_HOME/tools/management-console/logs/stdout.log 2> $TC_HOME/tools/management-console/logs/stderr.log &
