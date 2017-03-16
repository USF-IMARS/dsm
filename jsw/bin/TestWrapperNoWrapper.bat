rem Copyright (c) 1999, 2009 Tanuki Software, Ltd.
rem http://www.tanukisoftware.com
rem All rights reserved.
rem
rem This software is the proprietary information of Tanuki Software.
rem You shall use it only in accordance with the terms of the
rem license agreement you entered into with Tanuki Software.
rem http://wrapper.tanukisoftware.org/doc/english/licenseOverview.html
rem
rem This script is an example of how to run your application without the Wrapper, but with the
rem  Wrapper helper classes.  You can obtain the actual command generated by the wrapper for
rem  your application by running the Wrapper with the wrapper.java.command.loglevel=INFO
rem  property set.
rem
rem The wrapper.key property MUST be removed from the resulting command or it will fail to
rem  run correctly.
rem
java -Xms16m -Xmx64m -Djava.library.path="../lib" -Djava.class.path="../lib/wrapper.jar;../lib/wrappertest.jar" -Dwrapper.native_library="wrapper" -Dwrapper.debug="TRUE" org.tanukisoftware.wrapper.test.Main

