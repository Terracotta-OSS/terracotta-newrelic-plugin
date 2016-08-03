#!/bin/sh
#
# Stops Terracotta newrelic plugin
#


getPID() {
    PIDGREP="com.terracotta.nrplugin.app.Main"
    PID_CMD=`ps -elf | grep ${PIDGREP} | grep -v grep | awk '{print $4}'`
    echo $PID_CMD
}

PID=`getPID`
echo "Shutting down Terracotta Newrelic plug-in with process id: $PID"
kill $PID