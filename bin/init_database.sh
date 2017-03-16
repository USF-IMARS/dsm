#!/bin/bash
#
# This script creates an empty DSM database, deleteing and
# dropping the current database if it exists.
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

cd ${DSM_HOME}/database
echo Initializing database...
mysql -u root -p${ROOTPASSWORD} < make_dsm.sql
echo done
echo

cd $NISGS_HOME
echo Installing root product types...
dsm/bin/NISGSinstall.sh dsm/database/modis_PDS_product_types.xml
dsm/bin/NISGSinstall.sh dsm/database/npp_rdr_product_types.xml
echo done
echo
