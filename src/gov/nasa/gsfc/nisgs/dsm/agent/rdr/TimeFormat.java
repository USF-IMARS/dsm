/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;


import gov.nasa.gsfc.nisgs.dsm.agent.MoverException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
 * Provides parse or format methods for certain time field attributes such as Granule's N_CreationTime field.
 * In some cases the method loses the microseconds if dealing with Java Date as microseconds are not supported.
 * 
 *
 */
public class TimeFormat {

	
	/**
	 * Parse an input DateTime String in the format <code>yyyyMMddHHmmss.SSSuuuZ</code> and return a Date.
	 * The uuuZ is ignored by this method as microseconds are not supported by Java Data.
	 * @return a Date in UTC.   Use DateFormat to view it in UTC
	 * @exception throws MoverException on an input format error
	 */
	public static Date parseDateTime(String dateTime) throws MoverException {
		
		//This only supports milliseconds, so micros are added zeroes below
		ThreadLocal<SimpleDateFormat> dateTimeformatter = new ThreadLocal<SimpleDateFormat>()
		{
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("yyyyMMddHHmmss.SSS");
			}

		};

		dateTimeformatter.get().setTimeZone(TimeZone.getTimeZone("UTC"));
		
		String parseThis = dateTime.substring(0, dateTime.length()-4);
		//System.out.println("Parse this!? " + parseThis);
		
		Date date;
		try {
			date = dateTimeformatter.get().parse(parseThis);
		} catch (ParseException e) {
			throw new MoverException(e);
		}
		
		Calendar timeCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		timeCal.setTime(date);
		
		//int hourOfDay = timeCal.get(Calendar.HOUR_OF_DAY);
		//int minute = timeCal.get(Calendar.MINUTE);
		//int second = timeCal.get(Calendar.SECOND);
		//int milliSecond = timeCal.get(Calendar.MILLISECOND);
		
		//System.out.println("Hour of Day: " + hourOfDay + " minute: " + minute + " second: " + second + " milliSecond: " + milliSecond);
		
		return timeCal.getTime();  
	}
	
	/**
	 * Parse an input DateTime String in the format <code>yyyyMMddHHmmss.SSSuuuZ</code> and return a PDSDate.
	 * 
	 * @return a PDSDate
	 * @exception throws MoverException on an input format error
	 */
	public static PDSDate parsePDSDateTime(String dateTime) throws MoverException {
		if (dateTime.length() != 22) {
			throw new MoverException("Date (yyyyMMddHHmmss.SSSuuuZ) string not 22 chars, has [" + dateTime.length() + "] chars");
		}

		int year = Integer.parseInt(dateTime.substring(0, 4));  // year
		int month = Integer.parseInt(dateTime.substring(4, 6)) - 1;  // month, subtract 1 for PDSDate
		int day = Integer.parseInt(dateTime.substring(6, 8));  // date of month
		
		int hour = Integer.parseInt(dateTime.substring(0, 2));  // hour
		int minute = Integer.parseInt(dateTime.substring(2, 4));  // minute
		int seconds = Integer.parseInt(dateTime.substring(4, 6));  // seconds
		int millis = Integer.parseInt(dateTime.substring(7, 10));  // millis
		int micros = Integer.parseInt(dateTime.substring(10, 13));  // micros
		
		
		return new PDSDate(year, month, day, hour, minute, seconds, millis, micros);
	}
	
	
	
	/**
	 * Return a StringBuffer of formatted DateTime <code>yyyyMMddHHmmss.SSSuuuZ</code>.  
	 * Microseconds are currently appended as "000" as Date does not support it.
	 * @param date the input date and time
	 * @return a formatted StringBuffer
	 */
	public static StringBuffer formatDateTime(Date date) {

		//This only supports milliseconds, so micros are added zeroes below
		ThreadLocal<SimpleDateFormat> dateTimeformatter = new ThreadLocal<SimpleDateFormat>()
		{
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("yyyyMMddHHmmss.SSS");
			}
		};

		dateTimeformatter.get().setTimeZone(TimeZone.getTimeZone("UTC"));

		StringBuffer temp = new StringBuffer(dateTimeformatter.get().format(date));
		return temp.append("000").append("Z");
	}
	
	/**
	 * Return a StringBuffer of formatted DateTime <code>yyyyMMdd</code>.  
	 * 
	 * @param date the input Date
	 * @return a formatted StringBuffer
	 */
	public static StringBuffer formatDate(Date date) {

		ThreadLocal<SimpleDateFormat> dateTimeformatter = new ThreadLocal<SimpleDateFormat>()
		{
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("yyyyMMdd");
			}
		};

		dateTimeformatter.get().setTimeZone(TimeZone.getTimeZone("UTC"));

		StringBuffer temp = new StringBuffer(dateTimeformatter.get().format(date));
		return temp;
	}
	
	/**
	 * Return a StringBuffer of formatted DateTime <code>HHmmss.SSSuuuZ</code>.  
	 * Microseconds are currently appended as "000" as Date does not support it.
	 * @param date the input date and time
	 * @return a formatted StringBuffer
	 */
	public static StringBuffer formatTime(Date date) {
		//This only supports milliseconds, so micros are added zeroes below
		ThreadLocal<SimpleDateFormat> dateTimeformatter = new ThreadLocal<SimpleDateFormat>()
		{
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("HHmmss.SSS");
			}
		};
		dateTimeformatter.get().setTimeZone(TimeZone.getTimeZone("UTC"));

		StringBuffer temp = new StringBuffer(dateTimeformatter.get().format(date));
		return temp.append("000").append("Z");
	}
	
	/**
	 * Return a StringBuffer of formatted PDSDateTime <code>yyyyMMddHHmmss.SSSuuuZ</code>.  
	 *
	 * @param date the input date and time
	 * @return a formatted StringBuffer
	 */
	public static StringBuffer formatPDSDateTime(PDSDate date) {
		StringBuffer sb = new StringBuffer();
		sb.append(formatPDSDate(date));
		sb.append(formatPDSTime(date));
		return sb;
	}
	
	
	/**
	 * Return a StringBuffer of formatted PDSDate <code>yyyyMMdd</code>.  
	 *
	 * @param date the input date and time
	 * @return a formatted StringBuffer
	 */
	public static StringBuffer formatPDSDate(PDSDate date) {
		StringBuffer sb = new StringBuffer();
		
		sb.append(String.format("%04d", date.getYear()));
		sb.append(String.format("%02d", date.getMonth()+1));  // add 1 to the month as DateFormat used elsewhere expect 1-12
		sb.append(String.format("%02d", date.getDayOfMonth()));
		
		return sb;
	}
	
	
	/**
	 * Return a StringBuffer of formatted PDSDateTime <code>HHmmss.SSSuuuZ</code>.  
	 *
	 * @param date the input date and time
	 * @return a formatted StringBuffer
	 */
	public static StringBuffer formatPDSTime(PDSDate date) {
		StringBuffer sb = new StringBuffer();
	
		sb.append(String.format("%02d", date.getHours()));
		sb.append(String.format("%02d", date.getMinutes()));
		sb.append(String.format("%02d", date.getSeconds()));
		sb.append('.');
		sb.append(String.format("%03d", date.getMilliseconds()));
		sb.append(String.format("%03d", date.getMicroseconds()));
		sb.append('Z');
	
		return sb;
	}
	
	/**
	 * Take values from string formated as <code>yyyyMMdd</code> and <code>HHmmss.SSSuuuZ</code> and creates a Date
	 * but loses the microseconds since Java does not support them in the Date.
	 * @param dateStr  A string in the format of: <code>yyyyMMdd</code>
	 * @param timeStr  A string in the format of: <code>HHmmss.SSSuuuZ</code>
	 * @return the Date constructed from the string values of each field except for microseconds
	 * @throws MoverException 
	 */
	public static Date createDateTime(String dateStr, String timeStr) throws MoverException {
		// somewhat gross... bu re-uses code... not sure if this the best.
		return createPDSDateTime(dateStr, timeStr).getDate();
	}

	/**
	 * Take values from string formated as <code>yyyyMMdd</code> and <code>HHmmss.SSSuuuZ</code> and creates a PDSDate
	 * @param dateStr  A string in the format of: <code>yyyyMMdd</code>
	 * @param timeStr  A string in the format of: <code>HHmmss.SSSuuuZ</code>
	 * @return the PDSDate constructed from the string values of each field
	 */
	public static PDSDate createPDSDateTime(String dateStr, String timeStr) throws MoverException {
		if (dateStr.length() != 8) {
			throw new MoverException("Date (yyyyMMdd) string not 8 chars, has [" + dateStr.length() + "] chars");
		}
		if (timeStr.length() != 14) {
			throw new MoverException("Time (HHmmss.SSSuuuZ) string not 14 chars, has [" + timeStr.length() + "] chars -- [" + timeStr + "]");
		}
		int year = Integer.parseInt(dateStr.substring(0, 4));  // year
		
		//System.out.println("year -- " + year);
		
		int month = Integer.parseInt(dateStr.substring(4, 6))  - 1;  // month, subtract 1 for month
		
		//System.out.println("month -- " + month);
		
		int day = Integer.parseInt(dateStr.substring(6, 8));  // date of month
		
		//System.out.println("day -- " + day);
		
		int hour = Integer.parseInt(timeStr.substring(0, 2));  // hour
		
		//System.out.println("hour -- " + hour);
		
		int minute = Integer.parseInt(timeStr.substring(2, 4));  // minute
		
		//System.out.println("minute -- " + minute);
		
		int seconds = Integer.parseInt(timeStr.substring(4, 6));  // seconds
		
		//System.out.println("seconds -- " + seconds);
		
		int millis = Integer.parseInt(timeStr.substring(7, 10));  // millis
		
		//System.out.println("millis -- " + millis);
		
		int micros = Integer.parseInt(timeStr.substring(10, 13));  // micros
		
		//System.out.println("micros -- " + micros);
		
		return new PDSDate(year, month, day, hour, minute, seconds, millis, micros);
	}
}
