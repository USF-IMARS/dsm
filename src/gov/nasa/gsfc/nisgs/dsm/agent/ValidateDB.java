/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.ProductFactory;
import gov.nasa.gsfc.nisgs.properties.Utility;

import java.util.HashSet;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.File;

/**
 * ValidateDB checks the database resources for this site and removes any
 * resources without a corresponding file.
 * @version 1.0
 */
public class ValidateDB
{
    private static final long ONE_SECOND = 1000L;
    private DsmLog logger;
    private boolean dontDelete;
    private DsmProperties config;
    private String qsite;

    public static void main(String[] args) throws Exception
    {
        int n = Integer.getInteger("sleepSeconds",86400).intValue();
        if (n < 1) n = 1;
        long sleepyTime = (long)n * ONE_SECOND;

        ValidateDB validator = new ValidateDB();

        while (true)
        {
            try
            {
                validator.run();
            }
            catch (Exception re)
            {
                validator.logger.report("ValidateDB error",re);
            }
            try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
        }
    }

    /**
     * Constructor that refuses to fail.  If any errors occur during
     * initialization, we print a message and sleep for a gradually
     * extending time.
     */
    ValidateDB()
    {
	boolean OK = false;
	long sleepMillis = 1000;
	while(!OK) {
	    try {
		config = new DsmProperties();
		logger = new DsmLog("ValidateDB", config);
		logger.setverboseLogging(Boolean.getBoolean("verboseLogging"));
		dontDelete = Boolean.getBoolean("dontDelete");
		OK = true;
	    }
	    catch (Exception e) {
		System.err.println("ValidateDB error in initialization!");
		e.printStackTrace();
		try {
		    Thread.sleep(sleepMillis);
		}
		catch (Exception ee) {};
		if(sleepMillis < 1000000)
		    sleepMillis = 2 * sleepMillis;
	    }
	}

        logger.report("ValidateDB ready");
    }

    void run() throws Exception
    {
	logger.reportVerbose("Validator starting run");
        DSMAdministrator dsm = new DSMAdministrator("ValidateDB","ValidateDB");
        try
        {
	    qsite = Utility.quote(dsm.getSite());

	    int pcount = validateProducts(dsm);
	    int acount = validateStaticAncillaries(dsm);
	    acount += validateTimeAncillaries(dsm);
	    acount += validateSatTimeAncillaries(dsm);

	    if(pcount + acount > 0) {
		logger.warn("Removed " + pcount + " product and " + acount
			      + " ancillary records due to missing files");
	    }
        }
        finally
        {
            dsm.dispose();
        }
	logger.reportVerbose("Validator ending run");
    }

    private HashSet<String> validateCore(String whichFiles,
					 DSMAdministrator dsm, String getter)
	throws Exception
    {
	File siteroot = new File(config.getLocalDataDirectory());
	ResultSet rs = dsm.query(getter);
	HashSet<String> result = new HashSet<String>();
	int rowcount = 0;
	while(rs.next()) {
	    ++rowcount;
	    String thingID = rs.getString(1);
	    String pathstring = rs.getString(2);
	    String namestring = rs.getString(3);
	    File filepath = new File(pathstring);
	    if(!filepath.isAbsolute())
		filepath = new File(siteroot, pathstring);
	    File checkfile = new File(filepath, namestring);
	    if(!checkfile.exists()) {
		if(dontDelete)
		    logger.reportVerbose("Can't find " + checkfile.getPath()
					 + " in database");
		else {
		    logger.warn("Deleting " + checkfile.getPath()
				+ " from database; file is gone");
		    result.add(thingID);
		}
	    }
	}
	logger.reportVerbose(rowcount + " " + whichFiles+ " checked, " + result.size() + " missing files");
	return result;
    }
    /**
     * checks all Product ResourceSite entries on this site and validates them
     */
    private int validateProducts(DSMAdministrator dsm)
	throws Exception
    {
	String getter = "SELECT ResourceSites.resource, Directories.path, Resources.path FROM ResourceSites"
	    + " LEFT JOIN Resources ON Resources.id = ResourceSites.resource"
	    + " JOIN Directories ON ResourceSites.directory = Directories.id"
	    + " WHERE ResourceSites.site = " + qsite;

	HashSet<String> hs = validateCore("Products", dsm, getter);
	HashSet<String> productID = new HashSet<String>();

	for (String rid : hs) {
	    // Look up the product ID associated with this resource, and
	    // stash it in the productID set
	    String pquery = "SELECT product from Resources where id = " + Utility.quote(rid);
	    ResultSet rs = dsm.query(pquery);
	    while(rs.next())
		productID.add(rs.getString(1));

	    // Delete the resource record, since the file is missing
	    String deleter = "DELETE FROM ResourceSites"
		+ " WHERE resource = " + rid
		+ " AND site = " + qsite;
	    dsm.update(deleter);
	}

	// Before we leave, check and see if we can delete any products
	Statement stmt = dsm.getConnection().createStatement();
	for (String pid : productID) {
	    ProductFactory.checkProductResources(stmt, pid);
	}
	dsm.commit();

	return hs.size();
    }

    /**
     * checks all Ancillary entries on this site and validates them
     */
    private int validateAncillaries(DSMAdministrator dsm,
				    String mainTable, String siteTable)
	throws Exception
    {
	String getter = "SELECT " + siteTable + ".aid, Directories.path, " + mainTable + ".path FROM " + siteTable + ""
	    + " LEFT JOIN " + mainTable + " ON " + mainTable + ".id = " + siteTable + ".aid"
	    + " JOIN Directories ON " + siteTable + ".directory = Directories.id"
	    + " WHERE " + siteTable + ".site = " + qsite;

	HashSet<String> hs = validateCore(mainTable, dsm, getter);

	for (String rid : hs) {
	    String deleter = "DELETE FROM " + siteTable + ""
		+ " WHERE aid = " + rid
		+ " AND site = " + qsite;
	    dsm.update(deleter);
	}
	dsm.commit();
	return hs.size();
    }

    /**
     * checks all Product AncillarySite entries on this site and validates them
     */

    private int validateStaticAncillaries(DSMAdministrator dsm)
	throws Exception
    {
	return validateAncillaries(dsm,
				   "StaticAncillaries",
				   "StaticAncillarySites");
    }

    /**
     * checks all Product AncillarySite entries on this site and validates them
     */

    private int validateTimeAncillaries(DSMAdministrator dsm)
	throws Exception
    {
	return validateAncillaries(dsm,
				   "TimeAncillaries",
				   "TimeAncillarySites");
    }

    /**
     * checks all Product AncillarySite entries on this site and validates them
     */
    private int validateSatTimeAncillaries(DSMAdministrator dsm)
	throws Exception
    {
	return validateAncillaries(dsm,
				   "SatTimeAncillaries",
				   "SatTimeAncillarySites");
    }
}
