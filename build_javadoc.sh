#!/bin/bash
# The massive -classpath below is needed to shut javadoc up about
# classes it can't find.  It also means those other packages have
# to be available and built to completely shut it up.
javadoc -version -public -d ./javadoc \
	-sourcepath ./src \
	-classpath ../nsls/lib/nsls.jar:../properties/lib/properties.jar:../interp/lib/interp.jar:../geo/lib/geo.jar:./lib/ftp.jar \
	-subpackages gov.nasa.gsfc.nisgs.dsm:gov.nasa.gsfc.nisgs.dsm.agent:gov.nasa.gsfc.nisgs.dsm.admin
