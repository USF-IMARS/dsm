/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.io.*;
import java.sql.*;
import java.util.Properties;
import java.util.Enumeration;

import gov.nasa.gsfc.nisgs.properties.NisgsProperties;

/**
 * This class holds DSM properties from many sources (local init files,
 * database tables), courtesy of its superclass.  It insists that
 * the <code>DSM_DATA_DIRECTORY</code> property be set after everything is loaded.
 * It defines abstract accessors to hide many property names.
 * Everything is found relative to the Java property <code>MODULE_HOME</code>.
 * <p> The following properties must be set in the
 * <code>nisgs.properties</code> file:
 * <ul>
 * <li><code>NISGS_SITE_NAME</code> - IS, NISFES, NISDS1, etc.
 * <li><code>DSM_DATABASE_HOST</code> - host name of the DSM database
 * <li><code>DSM_DATA_DIRECTORY</code> - root directory managed by DSM on this site
 * </ul>
 * <p>
 * Other properties that can usefully be set in the <code>nisgs.properties</code> file:
 * <ul>
 * <li><code>DSM_INCOMING_DIRECTORY</code> - root directory scanned by (@link PdsMover} (if different from <code>DSM_DATA_DIRECTORY</code>)
 * <li><code>DSM_DATABASE_USER</code> - user name for DSM database (defaults to "dsm")
 * <li><code>DSM_DATABASE_PASSWORD</code> - password for DSM database (defaults to "b28c935")
 * </ul>
 * <p>
 * Other properties that are typically set globally in the
 * <code>NisgsProperties</code> database table.
 * <p>
 * These properties allow NISGS services to find each other:
 * <ul>
 * <li><code>NSLS_SERVER_HOST</code> - Host name of the NSLS logger
 * <li><code>NSLS_SERVER_PORT</code> - Port where the NSLS logger is listening (typically 3500)
 * <li><code>NSLS_SERVER_DIRECTORY</code> - local directory where the NSLS logger will write log event files if NSLS is not available
 * <li><code>IS_SITE</code> - Site name of the Information Services site (leave set to "IS")
 * <li><code>IS_SERVER_HOST</code> - Host name of the Information Services machine
 * <li><code>IS_ftpReaderUser</code> - User name for read-only FTP access to the IS machine (typically "anonymous")
 * <li><code>IS_ftpReaderPassword</code> - Password for read-only FTP access to the IS machine
 * <li><code>IS_ftpWriterUser</code> - User name for read-write FTPaccess to the IS machine (typically "nisgsftp")
 * <li><code>IS_ftpWriterPassword</code> - Password for read-write FTP access to the IS machine
 * <li><code>IS_dropbox</code> - Directory on the IS machine where products with unregistered types are stored
 * </ul>
 * <p>
 * These properties control retention and deletion of products (times are specified in days):
 * <ul>
 * <li><code>DSM_TLE_DaysRetention</code>
 * <li><code>DSM_UTCPOLE_DaysRetention</code>
 * <li><code>DSM_LEAPSEC_DaysRetention</code>
 * <li><code>DSM_ProductResourcesDaysRetention</code>
 * <li><code>DSM_PassesDaysRetention</code>
 * <li><code>DSM_TransferCommandsDaysRetention</code>
 * <li><code>DSM_MasterCleaner</code> - This is set to the site NISDS1 running the master cleaner agent.
 * </ul>
 */
public final class DsmProperties extends NisgsProperties
{
    private static final long serialVersionUID = 1L;

    private String moduleHome = null;
    private String nisgsHome = null;

    /**
     * Constructor loads all property files and database tables, in the
     * appropriate order.
     */
    public DsmProperties() throws Exception
    {

	moduleHome = System.getProperty("MODULE_HOME");

	nisgsHome = new File(moduleHome, "..").getCanonicalPath();

	// Also, defaultDataDirectory must be sanely set here
	mustBeSet("DSM_DATA_DIRECTORY");
    }

    /**
     * Constructor for test code where we don't want to read files
     */
    public DsmProperties(String mySite, String dbhost,
		  String dbuser, String dbpassword)
	throws Exception
    {
	throw new Exception("DSM without files no longer supported");
    }


    // And a bunch of convenience getter methods
    /**
     * Get the nisgs home directory (up from MODULE_HOME property value)
     */
    public final String getNisgsHome()
    {
        return nisgsHome;
    }

    /**
     * Get the local site name.
     */
    public final String getSite()
    {
        return getProperty("NISGS_SITE_NAME");
    }

    /**
     * Get the local host name (set by superclass)
     */
    public final String getHost()
    {
        return getProperty("HOST_NAME");
    }

    /**
     * Get the NSLS server host name.
     */
    public final String getNslsHost()
    {
        return getProperty("NSLS_SERVER_HOST");
    }

    /**
     * Get the NSLS server host port
     */
    public final int getNslsPort()
    {
        int port = 0;
        String p = getProperty("NSLS_SERVER_PORT","4005");
        try
	    {
		port = Integer.parseInt(p);
	    }
        catch (NumberFormatException e)
	    {
		port = 0;
	    }
        return port;
    }

    /**
     * Get the NSLS server log directory.
     * It is used only if the NSLS is unavailable.
     */
    public final String getNslsDirectory()
    {
        String p = getProperty("NSLS_SERVER_DIRECTORY",".");
        java.io.File f = new java.io.File(p);
        if (!f.isAbsolute())
	    {
		f = new java.io.File(nisgsHome,f.getPath());
	    }
        return f.getAbsolutePath();
    }

    /**
     * Get our local data directory root.
     */
    public final String getLocalDataDirectory()
    {
        return getProperty("DSM_DATA_DIRECTORY");
    }

    /**
     * Get the IS site name (this is hard-coded to "IS" in other code,
     * which we have to fix sometime
     */
    public final String getIS_Site()
    {
        return getProperty("IS_SITE");
    }

    /**
     * Get the host name of the IS machine
     */
    public final String getIS_Host()
    {
        return getProperty("IS_SERVER_HOST");
    }
    
    /**
     * Get the ingest Mode
     */
    public final String getIngest_Mode()
    {
    	String IngestMode=getProperty("PROCESSING_MODE");
    	if(IngestMode==null)
     	   IngestMode="multi-granule";
        return IngestMode;
    }
    /**
     * Return true if Ingest Mode is GRANULE otherwise return false
     */
    public final Boolean isGranuleIngestMode()
    {
    	Boolean IsGranule=false;
    	if((getIngest_Mode()).equals("cross-granule"))
    		IsGranule=true;
    	return IsGranule;
    }

    /**
     * Force granule ingest mode.  Used by SDRMover to insure that common
     * RDR code works in granule mode for SDRs.
     */
    public final void forceGranuleIngestMode()
    {
	setProperty("PROCESSING_MODE", "cross-granule");
    }
    
}
