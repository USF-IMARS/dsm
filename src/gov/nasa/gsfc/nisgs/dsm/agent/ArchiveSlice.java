/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.ProductType;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.TimeWindow;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.ResultSet;

/**
 * ArchiveSlice takes two or three arguments:
 *
 * date - formatted as yyyy-MM-dd (see SimpleDateFormat)
 * windowSize - a positive integer number of days
 * offset - an integer number of days [ optional ]
 *
 * A date window is computed as either
 *
 * (date, date + windowSize)
 * (date + offset, date + windowSize + offset)
 *
 * and the program then prints out a list of ancillary path names where
 * the freshness date of the ancillary is inside the date window.
 * A few additional files will be added to insure that every type in the
 * database has at least one file in the result list.
 *
 * @version 1.0 and we know what that means...
 */
public class ArchiveSlice
{
    Date startDate;
    Date endDate;
    TreeSet<String> result;
    DSMAdministrator dsma;
    String localDataRoot;
    String thisSite;
    String excludedType[];
    String excludedTypeQuery;

    public static void main(String[] args) throws Exception
    {
	if(args.length < 3) {
	    System.err.println("ARGS: ArchiveSlice date windowSize offset [types-to-exclude]");
	    System.exit(-1);
	}

	try
            {
                (new ArchiveSlice(args)).run();
            }
	catch (Exception re)
            {
		System.err.println("ArchiveSlice error:");
		re.printStackTrace();
            }
    }

    ArchiveSlice(String args[]) throws Exception
    {
	// properties = new DsmProperties();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	startDate = sdf.parse(args[0]);
	if(startDate == null)
	    throw new Exception("SimpleDateFormat(\"yyyy-MM-dd\" failed to parse: " + args[0]);

	long windowDays = Long.parseLong(args[1]);
	if(windowDays < 1)
	    throw new Exception("windowSize must be positive, not " + args[1]);
	windowDays = windowDays * 86400 * 1000;

	long windowOffset = 0;
	windowOffset = Long.parseLong(args[2]);
	if(windowOffset < 0)
	    throw new Exception("windowOffset must be non-negative, not " + args[2]);
	windowOffset = windowOffset * 86400 * 1000;

	// Anything after the offset is excluded types
	if(args.length > 3) {
	    int nxt = args.length - 3;
	    excludedType = new String[nxt];
	    for(int i = 0; i < nxt; i++)
		excludedType[i] = args[i + 3];
	}
	else
	    excludedType = new String[0];

	// Create the excludedTypeQuery, which is of the form
	// akey <> "type1" AND akey <> "type2" ...
	if (excludedType.length == 0)
	    excludedTypeQuery = "";
	else {
	    excludedTypeQuery = "akey <> \"" + excludedType[0] + "\"";
	    for(int i = 1; i < excludedType.length; i++)
		excludedTypeQuery +=
		    " AND akey <> \"" + excludedType[i] + "\"";
	}

	startDate = new Date(startDate.getTime() + windowOffset);
	endDate = new Date(startDate.getTime() + windowDays);

	result = new TreeSet<String>();

        dsma = new DSMAdministrator("ArchiveSlice","ArchiveSlice");

	// Dredge up the file system root path and our site name
	DsmProperties dsmp = new DsmProperties();
	try {
	    localDataRoot = dsmp.getLocalDataDirectory();
	    thisSite = dsmp.getSite();
	}
	finally {
	    dsmp.dispose();
	}	
	// Print a formatted version of the start date to stderr
	// (useful for file naming purposes)
	System.err.println(sdf.format(startDate));
    }

    void run() throws Exception
    {

	System.err.println("Date range is " + startDate + " to " + endDate);

	mashTables("TimeAncillaries", "TimeAncillarySites");
	mashTables("SatTimeAncillaries", "SatTimeAncillarySites");

	// And now to insure that every type has at least one entry
	// (to catch the LUTs), we query for each type the best file
	// before the endDate.  We get to do this twice in-line here
	// because the DSM API isn't as orthogonal as we'd like.

	// We may need an appropriate TimeWindow.
	// For now we'll use the *everything* window...
	TimeWindow archiveTW = TimeWindow.all();


	// First the timed ancillaries
	TreeSet<String> extraFiles = new TreeSet<String>();
	String qs;
	qs = "select distinct akey from TimeAncillaries where " + excludedTypeQuery;
	ResultSet rs = dsma.query(qs);
	ArrayList<String> akeys = new ArrayList<String>();
	while(rs.next())
	    akeys.add(rs.getString(1));

	for (String akey : akeys) {
	    String extraFile = dsma.getTimedAncillary(akey, endDate, archiveTW);
	    if(extraFile == null)
		System.err.println("Couldn't find any " + akey
				   + " in date range");
	    else {
		// Chop off the localDataRoot if it happens to be there
		if(extraFile.startsWith(localDataRoot))
		    extraFile = extraFile.substring(localDataRoot.length() + 1);
		extraFiles.add(extraFile);
	    }
	}

	// Then the satellite timed ancillaries
	akeys.clear();
	ArrayList<String> satellites = new ArrayList<String>();
	qs = "select distinct akey, spacecraft from SatTimeAncillaries where " + excludedTypeQuery;
	rs = dsma.query(qs);
	while(rs.next()) {
	    akeys.add(rs.getString(1));
	    satellites.add(rs.getString(2));
	}
	// Wouldn't a syntax for stepping arrays in parallel like
	// for(String akey : akeys, String sat : satellites)
	// be cool?
	Iterator<String> satit = satellites.iterator();
	for(String akey : akeys) {
	    String sat = satit.next();
	    String extraFile = dsma.getSatTimedAncillary(akey, endDate, sat, archiveTW);
	    if(extraFile == null)
		System.err.println("Couldn't find any " + akey
				   + " in date range");
	    else {
		// Chop off the localDataRoot if it happens to be there
		if(extraFile.startsWith(localDataRoot))
		    extraFile = extraFile.substring(localDataRoot.length() + 1);
		extraFiles.add(extraFile);
	    }
	}

	// Hose the extraFiles into the result
	result.addAll(extraFiles);

	// And print everything
	for (String p : result)
	    System.out.println(p);
    }

    /**
     * Slogs through mainTable and siteTable and friends, generating
     * the list of paths needed
     */
    void mashTables(String mainTable, String siteTable)
	throws Exception
    {
	// First get the main stuff out of the way
	String qs;
	qs = "select Directories.path, "
	    + mainTable + ".path from " + mainTable
	    + " LEFT JOIN " + siteTable
	    + " on " + mainTable + ".id = " + siteTable + ".aid"
	    + " LEFT JOIN Directories"
	    + " on " + siteTable + ".directory = Directories.id"
	    + " WHERE " + siteTable + ".site = " + Utility.quote(thisSite)
	    + " and " + mainTable + ".time >= " + Utility.quote(Utility.format(startDate))
	    + " and " + mainTable + ".time < " + Utility.quote(Utility.format(endDate))
	    + " and " + excludedTypeQuery;
	
	ResultSet rs = dsma.query(qs);
	while(rs.next()) {
	    result.add(rs.getString(1) + "/" + rs.getString(2));
	}
    }
}
