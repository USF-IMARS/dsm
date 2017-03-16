#!/bin/bash -e
#
# This script installs all SPAs that are currently unpacked
# in ~/drl/SPA.
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

cd $NISGS_HOME

for spadir in `ls SPA`
do
    if [ -d ./SPA/$spadir ]; then
	echo installing $spadir "..."
	./SPA/$spadir/NISGSinstall.sh
	# echo done - NISGSinstall.sh prints "done"
	echo
    fi
done
