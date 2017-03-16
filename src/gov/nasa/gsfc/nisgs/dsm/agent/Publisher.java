/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.sql.ResultSet;
import java.io.File;
import gov.nasa.gsfc.nisgs.dsm.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * Publisher sends resources to the IS computer.
 * @version 3.18 Added "published" database field to Resource and Ancillaries.
 *      Made sleep time a -D field. Added ancillary publishing.
 */
public class Publisher
{
    private static final long ONE_SECOND = 1000L;
    private DsmLog logger = null;
    private DsmProperties config;
    private File is_dropbox;
    private String is_site;

    public static void main(String[] args) throws Exception
    {
        int n = Integer.getInteger("sleepSeconds",5).intValue();
        if (n < 1) n = 1;
        long sleepyTime = (long)n * ONE_SECOND;

        Publisher publisher = new Publisher();

        while (true)
        {
            try
            {
                publisher.run();
            }
            catch (Exception re)
            {
                publisher.logger.report("Publisher error",re);
            }
            try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
        }
    }

    /**
     * Constructor that only fails when it sees a condition that is
     * unlikely to heal itself - database connection failures get
     * retried, bogus config files and directory structures crap out.
     */

    Publisher() throws Exception
    {
	long sleepMillis = 1000;
	while(logger == null) {
	    try {
		config = new DsmProperties();
		logger = new DsmLog("Publisher", config);
	    }
	    catch (Exception e) {
		System.err.println("Publisher error in initialization!");
		e.printStackTrace();
		try {
		    Thread.sleep(sleepMillis);
		}
		catch (Exception ee) {};
		if(sleepMillis < 1000000)
		    sleepMillis = 2 * sleepMillis;
	    }
	}
        is_dropbox = new File(config.getProperty("IS_dropbox","/"));

	// Complain NOW if the local data directory is hosed somehow...
	try {
	    Utility.isWritableDirectory(config.getLocalDataDirectory());
	}
	catch (Exception e) {
	    logger.report("LocalDataDirectory", e);
	    System.exit(-1);
	}

        is_site = config.getIS_Site();
        if (config.getSite().equals(is_site))
	    {
		throw new Exception("Publisher cannot run on the IS.");
	    }
        logger.report("Publisher ready.");
    }

    void run() throws Exception
    {
        String is_host = config.getIS_Host();
        String is_user = config.getProperty("IS_ftpWriterUser");
        String is_password = config.getProperty("IS_ftpWriterPassword");
        DSMAdministrator dsm = new DSMAdministrator("publisher","publisher");
        FileMover fm = null;
	Exception primaryE = null;
        try
        {
            //I ask for unpublished product resources on this site.
            java.util.List<PResource> resources = getUnpublishedProductResources(dsm);

            //I ask for unpublished non-static ancillaries on this site.
            java.util.List<Ancillary> ancillaries = getUnpublishedAncillaries(dsm);

            if (!resources.isEmpty() || !ancillaries.isEmpty()) {
                final boolean passive = true;
                fm = FileMover.newMover(config.getSite(), is_site, config);
                for (PResource pr : resources) {
		    try {
			publishResource(pr, dsm, fm);
		    }
		    catch (Exception e) {
			logger.report("Resource publisher error", e);
		    }
                }
                for (Ancillary a : ancillaries) {
		    try {
			publishAncillary(a, dsm, fm);
		    }
		    catch (Exception e) {
			logger.report("Ancillary publisher error", e);
		    }
                }
            }
        }
        catch (Exception e)
        {
	    primaryE = e;
	    // Report this error as it passes, since we may get
	    // another error from the finally below
	    logger.report(e);
            throw e;
        }
        finally
        {
            dsm.dispose();
            if (fm != null)
		fm.quit(primaryE);
        }
    }

    private java.util.List<PResource>
            getUnpublishedProductResources(DSMAdministrator dsm) throws Exception
    {
        java.util.List<PResource> list = new java.util.LinkedList<PResource>();
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT Resources.id,Resources.path,Directories.path,Products.productType ");
        sb.append("FROM Resources,Directories,Products,ResourceSites WHERE ");
        sb.append("Resources.published=0 AND ResourceSites.site=");
        sb.append(Utility.quote(dsm.getSite()));
        sb.append(" AND Resources.id=ResourceSites.resource AND ");
        sb.append("Directories.id=ResourceSites.directory AND Resources.product=Products.id");

        ResultSet r = dsm.query(sb.toString());
        while (r.next())
        {
            PResource pr = new PResource();
            pr.resourceId = r.getString(1);
            String filename = r.getString(2);
            String directory = r.getString(3);
            pr.file = new java.io.File(directory,filename);
            pr.productType = r.getString(4);
            list.add(pr);
        }
        return list;
    }

    private void publishResource(PResource pr, DSMAdministrator dsm, FileMover fm)
            throws Exception
    {
        File remoteDirectory = is_dropbox;
        ProductType productType = dsm.getProductType(pr.productType);
        if (productType != null && productType.getISdirectory() != null)
	    {
		remoteDirectory = productType.getISdirectory();
	    }
	// See if we can access/move the local file, and throw a fit
	// if we can't
	try {
	    fm.testLocalAccess(pr.file);
	}
	catch (Exception e) {
	    dsm.update("UPDATE Resources SET published=2 WHERE id=" + pr.resourceId);
	    dsm.commit();
	    throw e;
	}
	// Errors from here on might be temporary, so don't mark the database
	// unless we succeed
	fm.moveFile(pr.file,
		    new File(remoteDirectory, pr.file.getName()));
	final boolean doCommit = false;
	dsm.insertResourceSiteUpdate(is_site,remoteDirectory.getPath(),
				     pr.resourceId,doCommit);
	dsm.update("UPDATE Resources SET published=1 WHERE id=" + pr.resourceId);
	dsm.commit();
	logger.report("Publisher sends to IS " + pr.file.getName());
    }

    private java.util.List<Ancillary>
            getUnpublishedAncillaries(DSMAdministrator dsm) throws Exception
    {
        java.util.List<Ancillary> list = new java.util.LinkedList<Ancillary>();
        getUnpublishedAncillaries(list,dsm,"TimeAncillaries","TimeAncillarySites");
        getUnpublishedAncillaries(list,dsm,"SatTimeAncillaries","SatTimeAncillarySites");
        return list;
    }

    private void getUnpublishedAncillaries(java.util.List<Ancillary> list,
            DSMAdministrator dsm, String atable, String siteTable) throws Exception
    {
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT ");
        sb.append(atable);
        sb.append(".id,");
        sb.append(atable);
        sb.append(".path,Directories.path FROM ");
        sb.append(atable);
        sb.append(",Directories,");
        sb.append(siteTable);
        sb.append(" WHERE ");
        sb.append(atable);
        sb.append(".published=0 AND ");
        sb.append(siteTable);
        sb.append(".site=");
        sb.append(Utility.quote(dsm.getSite()));
        sb.append(" AND ");
        sb.append(atable);
        sb.append(".id=");
        sb.append(siteTable);
        sb.append(".aid AND Directories.id=");
        sb.append(siteTable);
        sb.append(".directory");

        ResultSet r = dsm.query(sb.toString());
        while (r.next())
        {
            Ancillary a = new Ancillary();
            a.id = r.getString(1);
            String filename = r.getString(2);
            String directory = r.getString(3);
            a.file = new java.io.File(directory,filename);
            a.table = atable;
            a.siteTable = siteTable;
            list.add(a);
        }
    }

    private void publishAncillary(Ancillary a, DSMAdministrator dsm, FileMover fm)
            throws Exception
    {
        File remoteDirectory = is_dropbox;
	// See if we can access/move the local file, and mark it to give up
	// if we can't
	try {
	    fm.testLocalAccess(a.file);
	}
	catch (Exception e) {
	    dsm.update("UPDATE " + a.table + " SET published=2 WHERE id=" + a.id);
	    dsm.commit();
	    throw e;
	}
	// Errors from here on might be temporary, so don't mark the database
	// unless we succeed
	fm.moveFile(a.file,
		    new File(remoteDirectory, a.file.getName()));
	final boolean doCommit = false;
	dsm.insertAncillarySiteUpdate(a.siteTable,is_site,remoteDirectory.getPath(),
				      a.id,doCommit);
	dsm.update("UPDATE " + a.table + " SET published=1 WHERE id=" + a.id);
	dsm.commit();
	logger.report("Publisher sends to IS " + a.file.getName());
    }

    // Silly little local classes to package file/resource/ancillary info

    private class PFR
    {
	File file;
    }

    private class PResource extends PFR
    {
        String resourceId;
        String productType;
    }

    private class Ancillary extends PFR
    {
        String id;
        String siteTable;
        String table;
    }
}

