/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.GregorianCalendar;
import java.sql.ResultSet;
import java.io.File;

/**
 * GCDatabase Cleans the database.
 * @version 1.0 Derived from Hoover (by deleting a bunch of stuff)
 */
public class GCDatabase
{
    private static final long ONE_SECOND = 1000L;
    private DsmProperties config;

    /**
     * Deprecated, as this stuff is now getting called from
     * ISDeleter as part of its cycle.
     */
    public static void main(String[] args) throws Exception
    {
        int n = Integer.getInteger("sleepSeconds",1860).intValue();
        if (n < 1) n = 1;
        long sleepyTime = (long)n * ONE_SECOND;

        GCDatabase gcd = new GCDatabase();

        while (true)
        {
            try
            {
                gcd.run();
            }
            catch (Exception re)
            {
                //gcd.logger.report("GCDatabase error",re);
            }
            try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
        }
    }

    public GCDatabase() throws Exception
    {
        config = new DsmProperties();
    }

    public void run() throws Exception
    {
        DSMAdministrator dsm = new DSMAdministrator("GCDatabase","GCDatabase");
        try
        {
	    cleanTransferCommands(dsm);
	    cleanProducts(dsm);
	    cleanPasses(dsm);
        }
        finally
        {
            dsm.dispose();
        }
    }

    /**
     * Delete DB rows that have no resources or files.
     */
    private void cleanProducts(DSMAdministrator dsm) throws Exception
    {
        int ancillaries = 0;
        int products = 0;
        int resources = 0;

	// I'm leaving these three in, though they should never find anything
        String sql = "SELECT SatTimeAncillaries.id FROM SatTimeAncillaries LEFT JOIN SatTimeAncillarySites ON SatTimeAncillaries.id=SatTimeAncillarySites.aid WHERE SatTimeAncillarySites.aid is NULL";
        ResultSet r = dsm.query(sql);
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM SatTimeAncillaries WHERE id=" + id);
            dsm.commit();
            ++ancillaries;
        }

        sql = "SELECT TimeAncillaries.id FROM TimeAncillaries LEFT JOIN TimeAncillarySites ON TimeAncillaries.id=TimeAncillarySites.aid WHERE TimeAncillarySites.aid is NULL";
        r = dsm.query(sql);
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM TimeAncillaries WHERE id=" + id);
            dsm.commit();
            ++ancillaries;
        }

        sql = "SELECT Resources.id FROM Resources LEFT JOIN ResourceSites ON Resources.id=ResourceSites.resource WHERE ResourceSites.resource is NULL";
        r = dsm.query(sql);
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM Resources WHERE id=" + id);
            dsm.commit();
            ++resources;
        }

        //Delete products with no resources.
        sql = "SELECT Products.id,Products.subproduct FROM Products LEFT JOIN Resources ON Products.id=Resources.product WHERE Resources.product is NULL";
        r = dsm.query(sql);
        while (r.next())
        {
            String id = r.getString(1);
            String subproduct = r.getString(2);
            dsm.update("DELETE FROM Products WHERE id=" + id);
            dsm.update("DELETE FROM Markers WHERE product=" + id);
            dsm.update("DELETE FROM Ancestors WHERE product=" + id + " OR ancestor=" + id);
            dsm.update("DELETE FROM ProductContributors WHERE product=" + id);
            if (subproduct != null)
            {
                dsm.update("DELETE FROM " + subproduct + " WHERE product=" + id);
            }
            dsm.commit();
            ++products;
        }

	// There used to be code here that searched the Markers table
	// for status >=2, and used that to delete products, using a
	// query like SELECT DISTINCT Products.id,Products.subproduct
	// FROM Markers,Products WHERE Products.id=Markers.product
	// AND Markers.status>=2.  This idea was bogus, because the
	// product in the Markers table is an INPUT product, and may
	// or may not need to die.

        sql = "SELECT Contributors.id FROM Contributors LEFT JOIN ProductContributors ON Contributors.id=ProductContributors.contributor WHERE ProductContributors.product is NULL";
        r = dsm.query(sql);
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM Contributors WHERE id=" + id);
            dsm.commit();
        }

        sql = "SELECT Thumbnails.id FROM Thumbnails LEFT JOIN ProductThumbnails ON Thumbnails.id=ProductThumbnails.thumbnail WHERE ProductThumbnails.product is NULL";
        r = dsm.query(sql);
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM Thumbnails WHERE id=" + id);
            dsm.commit();
        }
    }

    /**
     * Delete rows from the TransferCommands table.
     */
    private void cleanTransferCommands(DSMAdministrator dsm) throws Exception
    {
        String cutoff = computeCutoff(config.getProperty("DSM_TransferCommandsDaysRetention","5"));
        String sql = "DELETE FROM TransferCommands WHERE creation<" +
                Utility.quote(cutoff) + " AND complete>0";
        dsm.update(sql);
        dsm.commit();
    }

    /**
     * Delete rows from the Passes table.
     */
    private void cleanPasses(DSMAdministrator dsm) throws Exception
    {
        // This method deletes passes that have no products linked to them.
	// (and are not deleteProtected)

        int deleted = dsm.update("DELETE Passes FROM Passes LEFT JOIN Products"
				 + " ON Passes.id=Products.pass"
				 + " WHERE Products.pass IS NULL"
				 + " AND Passes.deleteProtected=0");
        dsm.commit();
    }

    /**
     * Compute cutoff date as a sql string.
     */
    public static String computeCutoff(String daysRetention) throws Exception
    {
        int days = Integer.parseInt(daysRetention);
        GregorianCalendar today = new GregorianCalendar();
        today.add(GregorianCalendar.DAY_OF_MONTH,-days);
        return Utility.format(today.getTime());
    }
}
