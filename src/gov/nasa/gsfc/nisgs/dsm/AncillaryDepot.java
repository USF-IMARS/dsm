/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.File;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.text.SimpleDateFormat;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * This class handles ancillary files for the DSM library. It exists to reduce
 * the size of the DSM class.
 * AncillaryDepot marks an ancillary as published if the site is the IS.
 * @version 3.18 Added "published" database field.
 * @version 3.20 Added "delete" flag database field.
 */
final class AncillaryDepot
{
    private java.sql.Connection connection;
    private String mysite;
    private File rootDataDirectory;
    private String informationServicesSite;
    static String IS = "IS";   //when site==IS, ancillary.published is true.

    static void setInformationServicesSite(String is)
    {
        IS = is;
    }

    AncillaryDepot(String mysite, java.sql.Connection connection, File rootDataDirectory,
            String is_site)
    {
        this.mysite = mysite;
        this.connection = connection;
        this.rootDataDirectory = rootDataDirectory;
        informationServicesSite = is_site;
    }

    /**
     * Store a new static ancillary file into the DSM database.
     * @return the database ID
     */
    String storeStaticAncillary(String label, String path, String description)
            throws Exception
    {
	return storeAncillary(label, path,
			      "StaticAncillaries", "StaticAncillarySites",
			      (description == null ?
			       null :
			       " description= " + Utility.quote(description)));
    }

    /**
     * Store a new timed ancillary file into the DSM database.
     * @return the database ID
     */
    String storeTimedAncillary(String key, java.util.Date time, String path)
            throws Exception
    {
	return storeAncillary(key, path,
			      "TimeAncillaries", "TimeAncillarySites",
			      " time= " + Utility.quote(Utility.format(time)));
    }

    /**
     * Store a new spacecraft-time ancillary file into the DSM database.
     * @return the database ID
     */
    String storeSatTimeAncillary(String key, String spacecraft, java.util.Date time,
            String path) throws Exception
    {	
	return storeAncillary(key, path,
			      "SatTimeAncillaries", "SatTimeAncillarySites",
			      " time= " + Utility.quote(Utility.format(time))
			      +" , spacecraft= " + Utility.quote(spacecraft));
    }

    /**
     * Get a spacecraft-time ancillary file for the keyword, spacecraft, and
     * time nearest to the passed time.
     */
    /*
    File getSatTimeAncillary(String key, java.util.Date time, String spacecraft)
            throws Exception
    {
	return getSatTimeAncillary(key, time, spacecraft, TimeWindow.all());
	}*/

    /**
     * Get a spacecraft-time ancillary file for the keyword, spacecraft, and
     * time that fits the time window
     */
    File getSatTimeAncillary(String key, java.util.Date time, String spacecraft,
			     TimeWindow tw)
            throws Exception
    {
	String bquery = "SELECT id,path,time FROM SatTimeAncillaries"
	    + " WHERE spacecraft="+ Utility.quote(spacecraft.toUpperCase())
	    + " AND akey=" + Utility.quote(key);

	return getBestFile(bquery, time, tw,
			   "SatTimeAncillarySites", "SatTimeAncillaries");
    }

    /**
     * Get a time ancillary data file for time nearest to the passed time.
     */
    /*
    File getTimedAncillary(String mysite, String key, java.util.Date time)
            throws Exception
    {
	return getTimedAncillary(mysite, key, time, TimeWindow.all());
	}*/

    /**
     * Get a time ancillary data file for time nearest to the passed time.
     */
    File getTimedAncillary(String mysite, String key, java.util.Date time,
			   TimeWindow tw)
            throws Exception
    {
        String sql = "SELECT id, path, time FROM TimeAncillaries WHERE akey=" + Utility.quote(key);
        return getBestFile(sql, time, tw,
			   "TimeAncillarySites","TimeAncillaries");
    }

    // Useful SimpleDateFormat string for yanking times out of Date objects
    // Quoted and ready-to-go for SQL
    private static String timeonlySDF = "''HH:mm:ss''";
    /**
     * This generates a medium hairy SQL statement to pick the best ancillary
     * file fitting certain date and time criteria.  The resulting SQL is
     * of the form:
     *
     * SELECT id, path, time  FROM TimeAncillaries WHERE akey="ancillary-type"
     * [ AND spacecraft="satellite" ]
     * [ AND time >= t0.preDate() AND time <= t0.postDate() ]
     * [ AND TIME(time) >= t0.preTime() AND TIME(time) <= t0.postTime() ]
     * ORDER BY ABS(TIMEDIFF(time, t0))
     *
     */
    private File getBestFile(String sql, Date t0date, TimeWindow tw,
			     String siteTable, String table)
            throws Exception
    {
	// If we do any date/time manipulation, this is going to be a
	// popular number...
	long t0 = t0date.getTime();

	// If we have a date window, tack it on
	if(tw.getPreDate() != Long.MAX_VALUE) {
	    Date predate = new Date(t0 - tw.getPreDate());
	    sql += " AND time >= " + Utility.quote(Utility.format(predate));
	}
	if(tw.getPostDate() != Long.MAX_VALUE) {
	    Date postdate = new Date(t0 + tw.getPostDate());
	    sql += " AND time <= " + Utility.quote(Utility.format(postdate));
	}

	// Time windows are potentially weird because they might wrap outside
	// the current time of day - if t0 is (say) 0200 and we have a
	// +/- three hour window, the time window we really want is:
	//         [0000 - 0500] || [2300 - 2399]
	//
	// We are assured by the TimeWindow constructor that time windows
	// are properly bounded - either they're both infinite, or both
	// set and the total width is less than 24 hours, so the only case
	// we care about is:
	if(tw.getPreTime() != Long.MAX_VALUE
	   || tw.getPostTime() != Long.MAX_VALUE) {

	    // We first compute the t0's milliseconds since midnight
	    Calendar t0midnight = new GregorianCalendar();
	    t0midnight.setTime(t0date);
	    t0midnight.set(Calendar.HOUR_OF_DAY, 0);
	    t0midnight.set(Calendar.MINUTE, 0);
	    t0midnight.set(Calendar.SECOND, 0);
	    t0midnight.set(Calendar.MILLISECOND, 0);
	    long millitod = t0 - t0midnight.getTimeInMillis();
	    Date pretime;
	    Date posttime;
	    SimpleDateFormat localSDF =  new SimpleDateFormat(timeonlySDF);

	    // OK, so there are three possible cases here, which degenerate
	    // into two cases, really:
	    
	    if ((tw.getPreTime() > millitod)
		|| (millitod + tw.getPostTime() > 86400 * 1000)) {
		// pre or post time land outside the 24-hour window
		// (time <= posttime OR time >= pretime)
		// This works because the SimpleDateFormat.format operation
		// is inherently modulo 24-hours
		pretime = new Date(t0 - tw.getPreTime());
		posttime = new Date(t0 + tw.getPostTime());

	    sql += " AND ( TIME(time) >= " + localSDF.format(pretime)
		+ " OR TIME(time) <= " + localSDF.format(posttime) + ")";
	    }
	    else {
		// pre and post time fit in 24 hour window
		// (time >= pretime AND time <= posttime)
		pretime = new Date(t0 - tw.getPreTime());
		posttime = new Date(t0 + tw.getPostTime());
	    sql += " AND TIME(time) >= " + localSDF.format(pretime)
		+ " AND TIME(time) <= " + localSDF.format(posttime);
	    }

	}
	// Tack on the sort
	sql += " ORDER BY ABS(TIMEDIFF(time, "
	    + Utility.quote(Utility.format(t0date))
	    + "))";

	// Do the query, loop down the results, and return the first one
	// that getFile() approves
        Statement statement = connection.createStatement();
	File result = null;
	try {
	    ResultSet r = Utility.executeQuery(statement, sql);
	    while (r.next())
		{
		    String id = r.getString("id");
		    String filename = r.getString("path");
		    result = getFile(siteTable, table, id, filename);
		    if(result != null) break;
		}
	}
	finally {
        statement.close();
	}

        return result;
    }

    /**
     * Get a static ancillary data file associated with a unique label.
     */
    File getStaticAncillary(String mysite, String akey) throws Exception
    {
        String fileName = null;
        String aid = null;
        Statement statement = connection.createStatement();
        String sql = "SELECT * FROM StaticAncillaries WHERE akey=" +
                Utility.quote(akey);
        ResultSet r = Utility.executeQuery(statement, sql);

        if (r.next())
        {
            fileName = r.getString("path");
            aid = r.getString("id");
        }
        statement.close();

        File file = null;
        if (aid != null)
        {
            file = getFile("StaticAncillarySites","StaticAncillaries",aid,fileName);
        }
        return file;
    }

    private File getFile(String siteTable, String ancillaryTable,
            String ancillaryId, String fileName) throws Exception
    {
        File file = null;
        Statement s = connection.createStatement();
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT path FROM Directories,");
        sb.append(siteTable);
        sb.append(" WHERE aid=");
        sb.append(ancillaryId);
        sb.append(" AND site=");
        sb.append(Utility.quote(mysite));
        sb.append(" AND id=directory");
        String sql = sb.toString();
        ResultSet r = Utility.executeQuery(s, sql);
        boolean isLocal = r.next();
        if (isLocal)
        {
            file = new File(r.getString(1),fileName);
            if (!file.isAbsolute())
            {
                //this happens on the IS where files are relative.
                //I make it absolute.
                file = new File(rootDataDirectory,file.getPath());
            }
            isLocal = file.exists();
            //if isLocal==false, it means the file is supposed to exist locally,
            //but it does not. Someone deleted it without telling the database.
        }

        //If the file is not local, I need to tell DSMR to go get it from the IS.
        //Except I don't bother if I am on the IS already.
        if (!isLocal && !mysite.equals(informationServicesSite) &&
            copy(ancillaryTable,ancillaryId))
        {
            r = Utility.executeQuery(s, sql);
            if (r.next())
            {
                file = new File(r.getString(1),fileName);
            }
        }
        s.close();
        return file;
    }

    /**
     * Store an ancillary file into the database and its site and directory
     * information in other related tables.
     */
    /*
    private String storeFile(String sql, String siteTable, String directoryId)
            throws SQLException
    {
        Statement statement = connection.createStatement();
        String aid = null;
        try
        {
            Utility.executeUpdate(statement, sql);
            aid = Utility.getLastAutoIncrementedValue(statement);
            storeSiteTable(statement,siteTable,aid,directoryId);
            Utility.commitConnection(connection);
        }
        catch (SQLException e)
        {
            connection.rollback();
            throw e;
        }
        finally
        {
            statement.close();
        }

        return aid;
    }
    */

    /**
     * Insert a row into one of the ancillary sites tables.
     */
    /*
    private void storeSiteTable(Statement statement, String siteTable,
            String ancillaryId, String directoryId) throws SQLException
    {
        StringBuffer sb = new StringBuffer(256);
        sb.append("INSERT INTO ");
        sb.append(siteTable);
        sb.append(" (aid,site,directory) VALUES (");
        sb.append(ancillaryId);
        sb.append(Utility.COMMA);
        sb.append(Utility.quoteComma(mysite));
        sb.append(directoryId);
        sb.append(")");
        Utility.executeUpdate(statement, sb.toString());
    }
    */
    /**
     * Copy an ancillary file from another site (computer) to this site (computer).
     * It waits units the operation is complete.
     */
    boolean copy(String ancillaryTable, String ancillaryId) throws Exception
    {
        Statement s = connection.createStatement();
        String today = Utility.format(new java.util.Date());
        String sql = "INSERT LOW_PRIORITY INTO TransferCommands VALUES (DEFAULT," +
                Utility.quoteComma(ancillaryTable) +
                ancillaryId + Utility.COMMA + Utility.quoteComma(mysite) +
                Utility.quoteComma(today) + "0)";
        Utility.executeUpdate(s, sql);
        Utility.commitConnection(connection);
        String id = Utility.getLastAutoIncrementedValue(s);
        int complete = 0;
        do
        {
            try { Thread.sleep(15000L); } catch ( InterruptedException e) {}
            ResultSet r = Utility.executeQuery(s, "SELECT complete FROM TransferCommands WHERE id=" + id);
            if (r.next())
            {
                complete = r.getInt(1);
            }
        }
        while (complete == 0);
        s.close();
        return complete == 1;
    }

    /**
     * Common code for storing ancillary files.  It checks to see if there
     * are already entries for this file, and if so just updates them.
     * ACTUALLY, it shouldn't do that check - there are places in the system
     * that *require* the ability to have multiple table entries pointing at
     * the same file.  *sigh*...
     */
    String storeAncillary(String type, String path,
			  String mainTable, String siteTable,
			  String otherFields)
	throws Exception
    {
	String aid = null;

	File file = new File(path);
	String filename = file.getName();
	String filepath = file.getParent();
	String directoryID = Utility.getDirectoryId(connection, filepath, true);
	// NOTE - The query below does a join on the siteTable to get the
	// exact entry pair that should represent this file.  It used to
	// cheat and only query the mainTable on filename and type, making
	// the rash assumption that (type X filename) was guaranteed unique.
	// This *is* guaranteed for products, not for ancillaries
	// (and that is a bug in itself).
	String checkmain =
	    "SELECT id FROM " + mainTable
	    + " LEFT JOIN " + siteTable
	    + " ON " + mainTable + ".id = " + siteTable + ".aid"
	    + " WHERE " + mainTable + ".path = " + Utility.quote(filename)
	    + " AND "+ siteTable + ".directory = " + directoryID
	    + " AND " + siteTable + ".site = " + Utility.quote(mysite)
	    + " AND " + mainTable + ".akey = " + Utility.quote(type);

	Statement statement = connection.createStatement();
	try {/*
	    ResultSet checkrs = Utility.executeQuery(statement, checkmain);
	    boolean needSite = false;
	    if(checkrs.next()) {
		// We already have one of these - update its other
		// fields (if any) and check for the site entry
		aid = checkrs.getString(1);
		if(otherFields != null) {
		    
		    String updatemain =
			"UPDATE " + mainTable + " SET "
			+ otherFields + " WHERE id=" + aid ;
		    Utility.executeUpdate(statement, updatemain);
		}
	    }
	    else {*/
		// It's a new one - just shove it in
		String insertmain =
		    "INSERT INTO " + mainTable + " SET"
		    + " akey= " + Utility.quote(type)
		    + ", path= " + Utility.quote(filename)
		    + ", published= " + (mysite.equals(IS)? "1" : "0");
		if(otherFields != null) {
		    insertmain += ", " + otherFields;
		}
		Utility.executeUpdate(statement, insertmain);
		aid = Utility.getLastAutoIncrementedValue(statement);
		// We definitely need a new site entry...
		String insertsite =
		    "INSERT INTO " + siteTable + " SET"
		    + " aid= " + aid
		    + ", site=" + Utility.quote(mysite)
		    + ", directory= " + directoryID;
		Utility.executeUpdate(statement, insertsite);
		// }
	    // And commit everything
	    Utility.commitConnection(connection);
	}
        catch (SQLException e) {
            connection.rollback();
            throw e;
        }
	finally {
	    statement.close();
	}
	return aid;
    }
}
