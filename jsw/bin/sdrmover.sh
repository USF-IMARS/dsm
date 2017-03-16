#! /bin/bash

#
# Skeleton sh script suitable for starting and stopping 
# wrapped Java apps on the Linux platform. 
#
# Make sure that PIDFILE points to the correct location,
# if you have changed the default location set in the 
# wrapper configuration file.
#
# The usual junk about finding out where we are...

case $0 in
         /*)  SHELLFILE=$0 ;;
        ./*)  SHELLFILE=${PWD}${0#.} ;;
        ../*) SHELLFILE=${PWD%/*}${0#..} ;;
          *)  SHELLFILE=$(type -P $0) ; if [ ${SHELLFILE:0:1} != "/" ]; then SHELLFILE=${PWD}/$SHELLFILE ; fi ;;
esac
SHELLDIR=${SHELLFILE%/*}

#-----------------------------------------------------------------------------
# These settings can be modified to fit the needs of your application

# Application
APP_NAME="SDRMover"
APP_LONG_NAME="DSM NPP SDR Mover"

# Wrapper
WRAPPER_CMD="./wrapper"
WRAPPER_CONF="../conf/sdrmover.conf"

# Priority at which to run the wrapper.  See "man nice" for valid priorities.
#  nice is only used if a priority is specified.
PRIORITY=

# Location of the pid file.
case $0 in
    /*) PIDDIR="`dirname $0`/../pids" ;;
     *) PWD=`pwd`; PIDDIR="`dirname $PWD/$0`/../pids" ;;
esac

# If uncommented, causes the Wrapper to be shutdown using an anchor file.
#  When launched with the 'start' command, it will also ignore all INT and
#  TERM signals.
#IGNORE_SIGNALS=true

# Wrapper will start the JVM asynchronously. Your application may have some
#  initialization tasks and it may be desirable to wait a few seconds
#  before returning.  For example, to delay the invocation of following
#  startup scripts.  Setting WAIT_AFTER_STARTUP to a positive number will
#  cause the start command to delay for the indicated period of time 
#  (in seconds).
# 
WAIT_AFTER_STARTUP=0

# If set, the status, start_msg and stop_msg commands will print out detailed
#   state information on the Wrapper and Java processes.
#DETAIL_STATUS=true

# If specified, the Wrapper will be run as the specified user when the 'start'
#  command is passed to this script.  When running with the 'console' command
#  the current user will be used.
# IMPORTANT - Make sure that the user has the required privileges to write
#  the PID file and wrapper.log files.  Failure to be able to write the log
#  file will cause the Wrapper to exit without any way to write out an error
#  message.
# NOTE - This will set the user which is used to run the Wrapper as well as
#  the JVM and is not useful in situations where a privileged resource or
#  port needs to be allocated prior to the user being changed.
#RUN_AS_USER=nisgs

# The following two lines are used by the chkconfig command. Change as is
#  appropriate for your application.  They should remain commented.
# chkconfig: 2345 20 80
# description: NSLS Server

# Go get the rest of it from the common tail
. $SHELLDIR/wrapper-tail
