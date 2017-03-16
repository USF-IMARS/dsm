/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * ArchiveWindow takes two or three arguments:
 *
 * date - formatted as yyyy-MM-dd (see SimpleDateFormat), this is the date
 *     of the last tarball in the archives
 * windowSize - a positive integer number of days
 * extension - a positive integer number of days [ optional, defaults to 7 ]
 *
 * The program prints out a list of dates between the the input date and
 * the current date such that they are greater than the input date and less
 * than (current date - windowSize - extension), meaning the dates represent
 * tarballs that currently do not exist and are safe to create now.
 *
 * @version 1.0 and we know what that means...
 */
public class ArchiveWindow
{

    public static void main(String[] args) throws Exception
    {
	if(args.length < 2 || args.length > 3) {
	    System.err.println("ARGS: ArchiveWindow date windowSize [ extension ]");
	    System.exit(-1);
	}

	try {
	    Date startDate;
	    Date lastSafeDate;
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	    startDate = sdf.parse(args[0]);
	    if(startDate == null)
		throw new Exception("SimpleDateFormat(\"yyyy-MM-dd\" failed to parse: " + args[0]);

	    long windowDays = Long.parseLong(args[1]);
	    if(windowDays < 1)
		throw new Exception("windowSize must be positive, not " + args[1]);
	    windowDays = windowDays * 86400 * 1000;

	    long windowExtension = 7;
	    if(args.length == 3)
		windowExtension = Long.parseLong(args[2]);
	    if(windowExtension < 1)
		throw new Exception("windowExtension must be positive, not " + args[2]);
	    windowExtension = windowExtension * 86400 * 1000;

	    startDate = new Date(startDate.getTime() + windowDays);
	    lastSafeDate = new Date( new Date().getTime() - windowDays - windowExtension);

	    String result = "";

	    while(startDate.compareTo(lastSafeDate) < 0) {
		if(result.length() != 0) result = result + " ";
		result = result + sdf.format(startDate);
		startDate = new Date(startDate.getTime() + windowDays);
	    }
	
	    System.out.println(result);
	}
	catch (Exception re)
            {
		System.err.println("ArchiveWindow error:");
		re.printStackTrace();
            }
    }
}
