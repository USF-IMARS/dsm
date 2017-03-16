#!/bin/bash
#
# This script rebuilds the DSM database.  It should be run whenever
# errors containing the word "deadlock" appear in the NSLS logger.
# 
# The system must NOT be running when you do this!
#
# It is apparently possible for a MySQL database to corrupt its structure
# on disk enough to foster deadlocks.  We have seen this occur after one
# or more MySQL crash/recovery cycles.  The following procedure preserves
# the information in the database, but destroys and rebuilds the on disk
# structures.

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

# Back up the MySQL database:

mysqldump --opt --user=$DSMUSER --password=$DSMPASSWORD DSM > /tmp/mysqldump.sql
# Log into mysql as root, destroy and recreate the database tables:

mysql --user=root --password=$ROOTPASSWORD < $DSM_HOME/database/make_dsm_database.sql

# Reload the DSM database contents:

mysql --user=$DSMUSER --password=$DSMPASSWORD DSM < /tmp/mysqldump.sql
