#!/bin/bash
#
# This script erases the database, completely.
# 
# The system must NOT be running when you do this!
#

# The usual junk about finding out where we are...

case $0 in
         /*)  SHELLFILE=$0 ;;
        ./*)  SHELLFILE=${PWD}${0#.} ;;
        ../*) SHELLFILE=${PWD%/*}${0#..} ;;
          *)  SHELLFILE=$(type -P $0) ; if [ ${SHELLFILE:0:1} != "/" ]; then SHELLFILE=${PWD}/$SHELLFILE ; fi ;;
esac
SHELLDIR=${SHELLFILE%/*}

# So, SHELLDIR ought to be NISGS_HOME/dsm/bin...
DSM_HOME=${SHELLDIR}/..
NISGS_HOME=${DSM_HOME}/..

# Go get the database passwords...
. ${SHELLDIR}/dbpasswords.sh

echo drop database if exists 'DSM;' \
| mysql --user=root --password=$ROOTPASSWORD
