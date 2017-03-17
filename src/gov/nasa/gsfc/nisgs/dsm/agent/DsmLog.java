/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.properties.Utility;
import gov.nasa.gsfc.nisgs.nsls.Log;
import gov.nasa.gsfc.nisgs.nsls.LoggableThrowable;
import java.util.Date;

/**
 * DsmLog is a facade to the NSLS logger.
 */
class DsmLog
{
    public Log log;
    private String mysite;
    private boolean verboseLogging;

    DsmLog(String program, DsmProperties dsmp) throws Exception
    {
	constructor_guts(program, dsmp);
    }

    // Constructor that creates (and throws away) a DsmProperties

    DsmLog(String program) throws Exception
    {
	DsmProperties dsmp = new DsmProperties();
	constructor_guts(program, dsmp);
	dsmp.dispose();
    }

    // Stupid local function to do the guts of DsmLog construction
    // This is to avoid the simple-looking constructor code:
    //
    // DsmLog(String program)
    // {
    //   DsmProperties dsmp = new DsmProperties();
    //	 super(program, dsmp);
    //   dsmp.dispose();
    // }
    //
    // which is *illegal* in Java - apparently super() must be the
    // first line in a constructor.

    void constructor_guts(String program, DsmProperties dsmp) throws Exception
    {
        String logHost = dsmp.getNslsHost();
        int logPort = dsmp.getNslsPort();
        mysite = dsmp.getSite();
        String logDir = dsmp.getNslsDirectory();
        log = new Log(logHost,logPort,logDir);
        log.setDefaultSource(new gov.nasa.gsfc.nisgs.nsls.NSLS.DSM(program));
	verboseLogging = false;
    }

    void setverboseLogging(boolean v)
    {
	verboseLogging = v;
    }

    void report(String text)
    {
        System.out.println(Utility.format(new Date()) + " " + text);
        log.info(mysite + " " + text);
    }

    void reportVerbose(String text)
    {
	if(verboseLogging)
	    report(text);
    }

    // Changed to avoid sending potentially unserializable Exceptions
    void report(String text, Exception e)
    {
        System.out.println(Utility.format(new Date()) + " " + text + " " + e.getMessage());

	// We use LoggableThrowable to create a safe exception here
        // We may lose information, but get something guaranteed Serializable
        log.error(mysite + " " + text, LoggableThrowable.create(e));
    }

    void report(Exception e)
    {
        System.out.println(Utility.format(new Date()) + " " + e.getMessage());

	// We use LoggableThrowable to create a safe exception here
        // We may lose information, but get something guaranteed Serializable

        log.error(mysite, LoggableThrowable.create(e));
    }

    void warn(String text)
    {
        System.out.println(Utility.format(new Date()) + " WARNING:" + text);
        log.warning(mysite + " " + text);
    }

    void verboseWarn(String text)
    {
	if(verboseLogging)
	    warn(text);
    }

    // Changed to avoid sending potentially unserializable Exceptions
    void warn(String text, Exception e)
    {
        System.out.println(Utility.format(new Date()) + " " + text + " " + e.getMessage());

	// We use LoggableThrowable to create a safe exception here
        // We may lose information, but get something guaranteed Serializable
        log.warning(mysite + " " + text, LoggableThrowable.create(e));
    }

    void warn(Exception e)
    {
        System.out.println(Utility.format(new Date()) + " " + e.getMessage());

	// We use LoggableThrowable to create a safe exception here
        // We may lose information, but get something guaranteed Serializable

        log.warning(mysite, LoggableThrowable.create(e));
    }

    void close(){
        log.close();
    }
}
