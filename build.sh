#!/bin/bash -e

# For paranoia's sake, recompile everything every time
find classes -name "*.class" -delete

HDF="./lib/hdf-java-2.7/linux64/hdf-java/lib"
HDF_LIB="linux"

# build dsm lib first
echo "Compiling dsm library"

javac -d ./classes/classes.dsm \
    -classpath ./src:./classes/classes.dsm:../properties/lib/properties.jar:../interp/lib/interp.jar:./lib/ftp.jar \
    -Xlint:deprecation \
    src/gov/nasa/gsfc/nisgs/dsm/*.java 
echo "Building dsm.jar"
jar -cf ./lib/dsm.jar -C ./classes/classes.dsm .

# then the agent jar
echo "Compiling agents"

javac -d ./classes/classes.agent \
    -classpath \
    ./classes/classes.dsm:./classes/classes.agent:./lib/dsm.jar:./lib/ftp.jar:./lib/mysql-jdbc.jar:../geo/lib/geo.jar:../nsls/lib/nsls.jar:../properties/lib/properties.jar:../interp/lib/interp.jar:$HDF/jhdf5.jar:$HDF/jhdf.jar \
    -Xlint:deprecation \
    src/gov/nasa/gsfc/nisgs/dsm/agent/{*.java,rdr/*.java}

echo "Building agent.jar"
jar -cf ./lib/agent.jar -C ./classes/classes.agent .

# then the test jar
echo "Compiling tests"

javac -d ./classes/classes.test \
    -classpath ./classes/classes.dsm:./classes/classes.test:./lib/dsm.jar:./lib/ftp.jar:./lib/mysql-jdbc.jar:../geo/lib/geo.jar:../nsls/lib/nsls.jar:../properties/lib/properties.jar \
    -Xlint:deprecation \
    src/gov/nasa/gsfc/nisgs/dsm/test/*.java 

echo "Building test.jar"
jar -cf ./lib/test.jar -C ./classes/classes.test .

# and then the kinpin gui

echo "Compiling passmanager GUI"

javac -d ./classes/classes.passmanager \
    -classpath ./classes/classes.dsm:./classes/classes.passmanager:./classes/classes.agent:./lib/dsm.jar:./lib/ftp.jar:./lib/mysql-jdbc.jar:../geo/lib/geo.jar:../nsls/lib/nsls.jar:../properties/lib/properties.jar \
    -Xlint:deprecation \
    src/gov/nasa/gsfc/nisgs/dsm/admin/*.java

echo "Building passmanager.jar GUI"
jar -cf ./lib/passmanager.jar -C ./classes/classes.passmanager .

echo "Done DSM building..."
