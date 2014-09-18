#!/bin/bash
# chkconfig: 345 80 05
# description: Starts / Stops the Terracotta Newrelic plugin
## processname: tc_nrplugin
##
## user: $RUN_AS_USER
##

#configurable variables
RUN_AS_USER="@some_system_user@"
LOGS_HOME="@some_log_path@"
APP_HOME="@home_path@"

# Source function library.
if [ -f /etc/init.d/functions ]; then
  . /etc/init.d/functions
elif [ -f /etc/rc.d/init.d/functions ] ; then
  . /etc/rc.d/init.d/functions
else
  exit 0
fi

# For SELinux we need to use 'runuser' not 'su'
if [ -x "/sbin/runuser" ]; then
    SU="/sbin/runuser"
else
    SU="/bin/su"
fi

#specify nohup command path
NOHUP="/usr/bin/nohup"

RETVAL=$?
CURRENT_USER=`id -nu`

function getPID() {
    PIDGREP="com.terracotta.nrplugin.app.Main"
    PID_CMD=`ps -elf | grep ${PIDGREP} | grep -v grep | awk '{print $4}'`
    echo $PID_CMD
}

function start() {
    RETVAL=1

    echo "Starting Terracotta Newrelic plugin..."
    if [ -f $APP_HOME/bin/start.sh ]; then
        PID=`getPID`
        if [ "x$PID" != "x" ]; then
            echo "Terracotta Newrelic plugin is already running. PID=$PID"
        else
            cd $APP_HOME/bin
            if [ "x$CURRENT_USER" != "x$RUN_AS_USER" ]; then
                $SU $RUN_AS_USER -c "$NOHUP $APP_HOME/bin/start.sh >> $LOGS_HOME/nohup-tcnrplugin.out 2>&1" &
            else
                $NOHUP $APP_HOME/bin/start.sh >> $LOGS_HOME/nohup-tcnrplugin.out 2>&1 &
            fi
            RETVAL=$?
        fi
    fi
    
    echo -n "Terracotta Newrelic plugin startup:"
    if [ $RETVAL -eq 0 ]; then
        echo_success
    else
        echo_failure
    fi
    
    echo
    return $RETVAL
}

function stop() {
    RETVAL=1

    echo "Stopping Terracotta Newrelic plugin..."
    if [ -f $APP_HOME/bin/stop.sh ]; then
        cd $APP_HOME/bin
        if [ "x$CURRENT_USER" != "x$RUN_AS_USER" ]; then
            $SU $RUN_AS_USER -c "$NOHUP $APP_HOME/bin/stop.sh >> $LOGS_HOME/nohup-tcnrplugin.out 2>&1" &
        else
            $NOHUP $APP_HOME/bin/stop.sh >> $LOGS_HOME/nohup-tcnrplugin.out 2>&1 &
        fi
        RETVAL=$?
    fi

    echo -n "Terracotta Newrelic plugin shutdown:"
    if [ $RETVAL -eq 0 ]; then
        echo_success
    else
        echo_failure
    fi

    echo
    return $RETVAL
}

function info() {
    RETVAL=1
    echo -n "Status for Terracotta Newrelic plugin --> "
    PID=`getPID`
    if [ "x$PID" != "x" ]; then
        echo -n " $PID "
        RETVAL=0
    fi

    if [ $RETVAL -eq 0 ]; then
        echo_success
    else
        echo_failure
    fi

    echo
    return $RETVAL
}

case "$1" in
 start)
    start
     ;;

 stop)
    stop
     ;;
 restart)
    stop
    echo "Waiting 5 seconds before restart..."
    sleep 5
    start
    ;;
 info)
    info
     ;;
 *)
    echo "Usage: $0 {start|stop|restart|info}"
    RETVAL="2"
    ;;
esac

exit $RETVAL