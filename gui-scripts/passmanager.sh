#!/bin/bash
# Runs PassManager, the DSM database Pass/Product/Resource browser
# Takes one undocumented argument, -powerUser

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

java -DMODULE_HOME=$DSM_HOME -cp $DSM_HOME/lib/passmanager.jar:$DSM_HOME/lib/dsm.jar:$DSM_HOME/lib/mysql-jdbc.jar:$NISGS_HOME/properties/lib/properties.jar gov.nasa.gsfc.nisgs.dsm.admin.PassManager $*
