/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.GregorianCalendar;
import java.sql.ResultSet;
import java.io.File;

/**
 * Scrubber is based on Hoover, and doesn't ask a lot of questions 
 * about cleaning the database
 * @version 1.0 Initial version 
 * 
 */
public class Scrubber
{
    
    private DsmLog log;
    private DsmProperties config;

    public static void main(String[] args) throws Exception
    {
        try {
        	Scrubber scrubber = new Scrubber();
        } catch (Exception e) {
			e.printStackTrace();
        }
    }

    Scrubber() throws Exception
    {
        config = new DsmProperties();
    	log = new DsmLog("Scrubber", config);

	// Complain NOW if the local data directory is hosed somehow...
	try {
	    Utility.isWritableDirectory(config.getLocalDataDirectory());
	}
	catch (Exception e) {
	    log.report("LocalDataDirectory", e);
	    System.exit(-1);
	}

        DSMAdministrator dsm = new DSMAdministrator("Scrubber","Scrubber");
 
        log.report("Scrubbing starting...");
        
        cleanProductResources(dsm);
        //cleanSatTimeAncillaries(dsm);
        //cleanTimeAncillaries(dsm);

        String dbScrubber = config.getProperty("DSM_MasterCleaner");
        if (dsm.getSite().equals(dbScrubber)) {

	    log.report("Scrubber: Master scrubbing starting...");
	    cleanDB(dsm);
	    cleanTransferCommands(dsm);
	    cleanPasses(dsm);
	    log.report("Scrubber: Master scrubbing ending.");
	}
  
        dsm.dispose();
        
        log.report("Scrubbing complete.");

    }

    /**
     * Delete product resources on this site and delete rows in ResourceSites.
     * It does not delete from the Resources table.
     */
    private void cleanProductResources(DSMAdministrator dsm) throws Exception
    {
        String mysite = dsm.getSite();
        String qmysite = Utility.quote(mysite);
        
        log.report("Cleaning Product Resources for site " + qmysite);
        
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT Resources.id,Resources.path,Directories.path FROM ");
        sb.append("Resources,ResourceSites,Directories,Products WHERE Resources.id=ResourceSites.resource");
        sb.append(" AND Products.id=Resources.product AND Products.deleteProtected=0 AND ");
        sb.append("ResourceSites.directory=Directories.id AND ResourceSites.site=");
        sb.append(qmysite);
        sb.append(" OR Products.deleteMark!=0");
        String sql = sb.toString();
        ResultSet r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String rid = r.getString(1);
            String filename = r.getString(2);
            String directory = r.getString(3);
            String sql2 = "DELETE FROM ResourceSites WHERE resource=" + rid + " AND site=" + qmysite;
            dsm.update(sql2);
            log.report("Issued delete: [" + sql2 + "]");
            deleteFile(directory,filename);
            dsm.commit();
        }
    }

    /**
     * Delete spacecraft-time files on this site and rows in SatTimeAncillarySites.
     * It does not delete from the SatTimeAncillaries table.
     */
    private void cleanSatTimeAncillaries(DSMAdministrator dsm) throws Exception
    {
  
    	log.report("Cleaning SatTimeAncillaries");
    	
    	String sql = "SELECT DISTINCT akey FROM SatTimeAncillaries";
        ResultSet r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String rkey = r.getString(1);
            String sql2 = getSatTimeAncillariesKeyedDeletionCandidates(dsm,rkey);
            if (sql2 != null)
            {
            	log.report("Issuing query: [" + sql2 + "]");
                cleanSatTimeAncillaries(dsm,sql2);
            }
        }
        String dsql = getSatTimeAncillariesMarkDeleted(dsm);
        cleanSatTimeAncillaries(dsm,dsql);
    }

    private String getSatTimeAncillariesKeyedDeletionCandidates(DSMAdministrator dsm, String keyword)
            throws Exception
    {
        String sql = null;
 
            String qmysite = Utility.quote(dsm.getSite());
           
            StringBuffer sb = new StringBuffer(512);
            sb.append("SELECT SatTimeAncillaries.id,SatTimeAncillaries.path,Directories.path");
            sb.append(" FROM SatTimeAncillaries,SatTimeAncillarySites,Directories WHERE ");
            sb.append("SatTimeAncillaries.id=SatTimeAncillarySites.aid AND ");
            sb.append("SatTimeAncillarySites.directory=Directories.id AND ");
            sb.append("SatTimeAncillaries.deleteProtected=0 AND SatTimeAncillarySites.site=");
            sb.append(qmysite);
            sb.append(" AND SatTimeAncillaries.akey=");
            sb.append(Utility.quote(keyword));
            
            sql = sb.toString();
        
        return sql;
    }

    private String getSatTimeAncillariesMarkDeleted(DSMAdministrator dsm)
    {
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT SatTimeAncillaries.id,SatTimeAncillaries.path,Directories.path");
        sb.append(" FROM SatTimeAncillaries,SatTimeAncillarySites,Directories WHERE ");
        sb.append("SatTimeAncillaries.id=SatTimeAncillarySites.aid AND ");
        sb.append("SatTimeAncillarySites.directory=Directories.id AND ");
        sb.append("SatTimeAncillaries.deleteProtected=0 AND SatTimeAncillaries.deleteMark!=0 ");
        sb.append("AND SatTimeAncillarySites.site=");
        sb.append(Utility.quote(dsm.getSite()));
        return sb.toString();
    }

    /**
     * Delete spacecraft-time files on this site and rows in SatTimeAncillarySites.
     * It does not delete from the SatTimeAncillaries table.
     */
    private void cleanSatTimeAncillaries(DSMAdministrator dsm, String sql) throws Exception
    {
        String qmysite = Utility.quote(dsm.getSite());
        
        log.report("Cleaning SatTimeAncillaries for site: " + qmysite);
        
        ResultSet r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String id = r.getString(1);
            String filename = r.getString(2);
            String directory = r.getString(3);
            dsm.update("DELETE FROM SatTimeAncillarySites WHERE aid=" + id + " AND site=" + qmysite);
            deleteFile(directory,filename);
            dsm.commit();
        }
    }

    /**
     * Delete time ancillary files on this site and rows in TimeAncillarySites. .
     * It does not delete from TimeAncillaries.
     */
    private void cleanTimeAncillaries(DSMAdministrator dsm) throws Exception
    {
    	log.report("Cleaning TimeAncillaries");
    	
    	String sql = "SELECT DISTINCT akey FROM TimeAncillaries";
    	
        ResultSet r = dsm.query(sql);
        
        log.report("Issued query: [" + sql + "]");
        
        while (r.next())
        {
            String rkey = r.getString(1);
            String sql2 = getTimeAncillariesKeyedDeletionCandidates(dsm,rkey);
            
            log.report("Issued query: [" + sql2 + "]");
            
            if (sql2 != null)
            {
                cleanTimeAncillaries(dsm,sql2);
            }
        }
        String dsql = getTimeAncillariesMarkDeleted(dsm);
        cleanTimeAncillaries(dsm,dsql);
    }

    private String getTimeAncillariesKeyedDeletionCandidates(DSMAdministrator dsm, String keyword)
            throws Exception
    {
        String sql = null;

            String qmysite = Utility.quote(dsm.getSite());
            
            StringBuffer sb = new StringBuffer(512);
            sb.append("SELECT TimeAncillaries.id,TimeAncillaries.path,Directories.path FROM ");
            sb.append("TimeAncillaries,TimeAncillarySites,Directories WHERE ");
            sb.append("TimeAncillaries.id=TimeAncillarySites.aid AND ");
            sb.append("TimeAncillaries.deleteProtected=0 AND ");
            sb.append("TimeAncillarySites.directory=Directories.id AND TimeAncillarySites.site=");
            sb.append(qmysite);
            sb.append(" AND TimeAncillaries.akey=");
            sb.append(Utility.quote(keyword));
            sql = sb.toString();
        
        return sql;
    }

    private String getTimeAncillariesMarkDeleted(DSMAdministrator dsm)
    {
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT TimeAncillaries.id,TimeAncillaries.path,Directories.path");
        sb.append(" FROM TimeAncillaries,TimeAncillarySites,Directories WHERE ");
        sb.append("TimeAncillaries.id=TimeAncillarySites.aid AND ");
        sb.append("TimeAncillarySites.directory=Directories.id AND ");
        sb.append("TimeAncillaries.deleteProtected=0 AND TimeAncillaries.deleteMark!=0 ");
        sb.append("AND TimeAncillarySites.site=");
        sb.append(Utility.quote(dsm.getSite()));
        return sb.toString();
    }

    /**
     * Delete time ancillary files on this site and rows in TimeAncillarySites. .
     * It does not delete from TimeAncillaries.
     */
    private void cleanTimeAncillaries(DSMAdministrator dsm, String sql) throws Exception
    {
        String qmysite = Utility.quote(dsm.getSite());
        
        log.report("Cleaning TimeAncillaries from site: " + qmysite);
        
        ResultSet r = dsm.query(sql);
        
        log.report("Issued query: [" + sql + "]");
        
        while (r.next())
        {
            String id = r.getString(1);
            String filename = r.getString(2);
            String directory = r.getString(3);
            dsm.update("DELETE FROM TimeAncillarySites WHERE aid=" + id + " AND site=" + qmysite);
            deleteFile(directory,filename);
            dsm.commit();
        }
    }

    /**
     * Delete DB rows that have no resources or files.
     */
    private void cleanDB(DSMAdministrator dsm) throws Exception
    {
        int ancillaries = 0;
        int products = 0;
        int resources = 0;
        
        log.report("Cleaning DB...");

        String sql = "SELECT SatTimeAncillaries.id FROM SatTimeAncillaries LEFT JOIN SatTimeAncillarySites ON SatTimeAncillaries.id=SatTimeAncillarySites.aid WHERE SatTimeAncillarySites.aid is NULL";
        ResultSet r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM SatTimeAncillaries WHERE id=" + id);
            dsm.commit();
            ++ancillaries;
        }

        sql = "SELECT TimeAncillaries.id FROM TimeAncillaries LEFT JOIN TimeAncillarySites ON TimeAncillaries.id=TimeAncillarySites.aid WHERE TimeAncillarySites.aid is NULL";
        r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM TimeAncillaries WHERE id=" + id);
            dsm.commit();
            ++ancillaries;
        }

        sql = "SELECT Resources.id FROM Resources LEFT JOIN ResourceSites ON Resources.id=ResourceSites.resource WHERE ResourceSites.resource is NULL";
        r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
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
        log.report("Issued query: [" + sql + "]");
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

        //I search Markers for completed=2, which Reservation sets when a transfer command fails.
        //This means there is something wrong with the product, and 99% of the time the files
        //were deleted, but the database was not updated.
        //I should bullet-proof the meaning of completed=2 to ensure it means files were deleted.
        sql = "SELECT DISTINCT Products.id,Products.subproduct FROM Markers,Products WHERE Products.id=Markers.product and Markers.completed=2";
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

        sql = "SELECT Contributors.id FROM Contributors LEFT JOIN ProductContributors ON Contributors.id=ProductContributors.contributor WHERE ProductContributors.product is NULL";
        r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM Contributors WHERE id=" + id);
            dsm.commit();
        }

        sql = "SELECT Thumbnails.id FROM Thumbnails LEFT JOIN ProductThumbnails ON Thumbnails.id=ProductThumbnails.thumbnail WHERE ProductThumbnails.product is NULL";
        r = dsm.query(sql);
        log.report("Issued query: [" + sql + "]");
        while (r.next())
        {
            String id = r.getString(1);
            dsm.update("DELETE FROM Thumbnails WHERE id=" + id);
            dsm.commit();
        }
        if (ancillaries > 0 || products > 0)
        {
            log.report("Scrubber purged " + ancillaries + " ancillary records, " +
                    products + " product records, and " + resources + " resource records from the database.");
        } else {
        	log.report("Scrubber product, ancillaries, etc..., empty already.");
        }
    }

    /**
     * Delete rows from the TransferCommands table.
     */
    private void cleanTransferCommands(DSMAdministrator dsm) throws Exception
    {
    	log.report("Cleaning TransferCommands...");
        String sql = "DELETE FROM TransferCommands WHERE complete >= 0";
        dsm.update(sql);
        dsm.commit();
    }

    /**
     * Delete rows from the Passes table.
     */
    private void cleanPasses(DSMAdministrator dsm) throws Exception
    {
    
    	log.report("Cleaning Passes ... ");
        dsm.update("DELETE FROM Passes WHERE deleteProtected >= 0");
        dsm.commit();
    }

 

    private void deleteFile(String directory, String filename)
    {
        File file = new File(directory,filename);
        if (!file.isAbsolute())
        {
            String dataRoot = config.getLocalDataDirectory();
            file = new File(dataRoot,file.getPath());
        }
        if (file.exists())
        {
            boolean ok = file.delete();
            if (ok)
            {
                log.report("Scrubber deleted " + file.getName());
            }
            else
            {
                log.report("Scrubber cannot delete " + file.getName() + " -- stricken from database");
            }
        }
        else
        {
            log.report("File " + file.getName() + " does not exist");
        }
    }
}
