#!/bin/bash

java -DNISGS_HOME=/home/nisgs/drl/dsm -classpath ../lib/dsm.jar:../lib/mysql-jdbc.jar:../lib/ftp.jar:../lib/agent.jar:../../nsls/lib/nsls.jar gov.nasa.gsfc.nisgs.dsm.agent.Scrubber
