#!/bin/bash
# Runs command-line database file deleter
# Takes one argument - full path to file to be deleted
# Only removes database record - DOES NOT DELETE FILE FROM FILE SYSTEM

# First, find out where we are

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

java -cp $DSM_HOME/lib/test.jar:$DSM_HOME/lib/dsm.jar:$DSM_HOME/lib/agent.jar:$DSM_HOME/lib/mysql-jdbc.jar:$DSM_HOME/lib/ftp.jar:$NISGS_HOME/nsls/lib/nsls.jar:$NISGS_HOME/properties/lib/properties.jar -DMODULE_HOME=$DSM_HOME gov.nasa.gsfc.nisgs.dsm.test.TestProduct $1
