/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;

/** This class packages up two time intervals, expressed as millisecond
 * offsets from a presumed center time.
 */

public final class TimeWindow
{
    private long pretime;
    private long posttime;
    private long predate;
    private long postdate;

    public TimeWindow (long pret, long postt, long pred, long postd)
    {
	pretime = pret;
	posttime = postt;
	predate = pred;
	postdate = postd;
    }

    // The usual damned boilerplate accessors

    public long getPreTime()
    {
	return pretime;
    }
    public long getPostTime()
    {
	return posttime;
    }
    public long getPreDate()
    {
	return predate;
    }
    public long getPostDate()
    {
	return postdate;
    }

    // storage for the default time window (infinite on both intervals)
    static private TimeWindow defaultWindow = null;

    public static TimeWindow all()
    {
	if (defaultWindow == null)
	    defaultWindow = new TimeWindow(Long.MAX_VALUE ,Long.MAX_VALUE ,Long.MAX_VALUE ,Long.MAX_VALUE);
	return defaultWindow;
    }

    // Stupid little parser function with mindless error checking
    long parse(String s, long millifactor)
	throws Exception
    {
	long result = Long.MAX_VALUE;
	if(!s.equals("")) {
	    double dresult = Double.parseDouble(s);
	    if(dresult < 0)
		throw new Exception("negative value " + s + " not allowed for TimeWindow");
	    result = (long)(dresult * millifactor);
	}
	return result;
    }

    /**
     * The convenience constructor that takes four strings (fresh from the XML
     * TimeWindow description).  The strings contain decimal offsets in
     * hours or days (depending on time or day offset).  Time intervals
     * greater than 24 hours are illegal, as are one-sided time intervals -
     * both pre-and-post time must be set, or neither.
     */

    public TimeWindow(String pret, String postt, String pred, String postd)
	throws Exception
    {
	pretime = parse(pret, 3600 * 1000);
	posttime = parse(postt, 3600 * 1000);

	if (pretime == Long.MAX_VALUE) {
	    if(posttime != Long.MAX_VALUE)
		throw new Exception("Post-time set without pre-time");
	}
	else if(posttime == Long.MAX_VALUE)
	    throw new Exception("Pre-time set without post-time");

	if (pretime != Long.MAX_VALUE
	    && posttime != Long.MAX_VALUE
	    && pretime + posttime > 86400 * 1000)
	    throw new Exception("TimeWindow time interval (" + pret + " , " + postt + ") greater than one day");

	predate = parse(pred, 86400 * 1000);
	postdate = parse(postd, 86400 * 1000);
    }

    /**
     * What do you need to say about a toString() method ?
     */
    public String toString()
    {
	return "TimeWindow("
	    + (pretime ==  Long.MAX_VALUE ? "" : " pretime=" + (pretime / (3600.0 * 1000.0)))
	    + (posttime ==  Long.MAX_VALUE ? "" : " posttime=" + (posttime / (3600.0 * 1000.0)))
	    + (predate ==  Long.MAX_VALUE ? "" : " predate=" + (predate / (86400.0 * 1000.0)))
	    + (postdate ==  Long.MAX_VALUE ? "" : " postdate=" + (postdate / (86400.0 * 1000.0)))
	    + ")";
    }
}
