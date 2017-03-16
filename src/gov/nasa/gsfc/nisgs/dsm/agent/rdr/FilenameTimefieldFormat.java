/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Enforce certain time field formats for the the RDR filename. Time zone is UTC.  Uses {@link SimpleDateFormat}.
 * 
 *
 */
public class FilenameTimefieldFormat {
	private static ThreadLocal<SimpleDateFormat> dfTime = initializeTime1();
	private static ThreadLocal<SimpleDateFormat> mfTime = initializeTime2();
	private StartStopFieldId fieldId;
	
	// initialize the time formats and set the time zone to UTC.
	// initialize SimpleDateFormat using ThreadLocal
	// if we used the default SimpleDateFormat pattern, we wouldn't have to call initialValue()
	// but because we use non-standard date/time patterns, we override the constructor to provide a new pattern
	private static ThreadLocal<SimpleDateFormat> initializeTime1() {
		ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>()
		{
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("HHmmss");
			}
		};
	
		sdf.get().setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf;
	}
	private static ThreadLocal<SimpleDateFormat> initializeTime2() {
		ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>()
		{
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("SSS");
			}
		};

		sdf.get().setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf;
	}
	
	/**
	 * Construct a new FileTimefieldFormat using the {link@ StartStopFieldId} as an input.  Time zone is UTC.
	 * @param fieldId a StartStopFieldId which is used in the official name
	 */
	public FilenameTimefieldFormat(StartStopFieldId fieldId) {
		this.fieldId = fieldId;
	}
	
	/**
	 * Format the given Date into proper FilenameTimeField
	 * @param timeDate the time/date of interest
	 * @return a String that enforces the format
	 */
	public String format(Date timeDate) {
		String tmpMilliStr = mfTime.get().format(timeDate);
		int tmpTenths = Integer.parseInt(tmpMilliStr);
		tmpTenths = tmpTenths / 100;
		
		String timeStr = fieldId.toString() + dfTime.get().format(timeDate)  + String.format("%1d", tmpTenths) ;
		
		return timeStr;
	}
	
	/**
	 * Parse the given string according to the FilenameTimefieldFormat and return it as a Date
	 * @param timeStr the string containing the time and date
	 * @return a Date with the time and date from the string encoded in it
	 * @throws ParseException throws ParseException if the string cannot be parsed according to the format
	 */
	public Date parse(String timeStr) throws ParseException {
		if ((timeStr.charAt(0) != StartStopFieldId.e.toString().charAt(0)) && 
				(timeStr.charAt(0) != StartStopFieldId.t.toString().charAt(0))) {
			throw new ParseException("Time string [" + timeStr + "] must start with a \'" + StartStopFieldId.t + 
										"\' or " + StartStopFieldId.e + "\'" , 0);			
		}
		
		
		return dfTime.get().parse(timeStr.substring(1,7));
	}
}
