/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;



import gov.nasa.gsfc.nisgs.dsm.agent.MoverException;
import gov.nasa.gsfc.nisgs.dsm.agent.MoverRuntimeException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;



/**
 * Create proper NPOESS filename CreationDate fields, or parse pre-existing ones back into a Date object.
 * Because Java only supports times to milliseconds resolution, the microseconds are not fully reflected.
 * The format is follows:
 * <code>cyyyyMMddHHmmssSSSSSS</code>
 * The subfields are:
 * <table>
 * <tr>yyyy -- year</tr>
 * <tr>MM -- month</tr>
 * <tr>dd -- day of month</tr>
 * <tr>HH -- hour of day</tr>
 * <tr>mm -- minutes of hour</tr>
 * <tr>ss -- seconds of minutes</tr>
 * <tr>SSSSSS -- microseconds</tr>
 *</td>
 *</table>
 *
 * 
 *
 */
public class FileCreationDateFormat {
	private ThreadLocal<SimpleDateFormat> sdf =  new ThreadLocal<SimpleDateFormat>()
	{
		@Override 
		protected SimpleDateFormat initialValue()
		{
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	public FileCreationDateFormat() {
		sdf.get().setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	/**
	 * Return a CreationDate string in the format: <code>cyyyyMMddHHmmssSSSSSS</code>
	 * @param date a Date object
	 * @return the formatted String
	 */
	public String format(Date date) {
		StringBuffer sb = new StringBuffer();
		
		sb.append('c');
		sb.append(sdf.get().format(date));
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
	
		sb.append(String.format("%06d", cal.get(Calendar.MILLISECOND) * 1000));
	
		return sb.toString();
	}
	
	/**
	 * Parse a Date object into the NPOESS file CreationDate field.  The field has the format of:
	 * <code>cyyyyMMddHHmmssSSSSSS</code>, although a String with the preceding 'c' is accepted as well.
	 * The subfields are:
	 * <table>
	 * <tr>yyyy -- year</tr>
	 * <tr>MM -- month</tr>
	 * <tr>dd -- day of month</tr>
	 * <tr>HH -- hour of day</tr>
	 * <tr>mm -- minutes of hour</tr>
	 * <tr>ss -- seconds of minutes</tr>
	 * <tr>SSSSSS -- microseconds</tr>
	 *</td>
	 *</table>
	 * @param fileCreationDateString the date/time String from the file, may include the preceding 'c' or not
	 * @return the Date object
	 * @throws MoverException 
	 */
	public Date parse(String fileCreationDateString) throws MoverException {
		String longDateToParse = fileCreationDateString;
		if (fileCreationDateString.length() == 21) {
			if (fileCreationDateString.charAt(0) != 'c') {
				throw new MoverRuntimeException("File CreationDate format is the wrong format [" +  fileCreationDateString + "], should be: cyyyyMMddHHmmssSSSSSS");
			}
			longDateToParse = fileCreationDateString.substring(1);
		} else if (fileCreationDateString.length() != 20) {
			throw new MoverRuntimeException("File CreationDate format is the wrong format [" +  fileCreationDateString + "], should be: cyyyyMMddHHmmssSSSSSS");			
		}
		
		String dateToParse = longDateToParse.substring(0, 14);
		Date tmp = null;
		try {
			tmp = sdf.get().parse(dateToParse);
		} catch (ParseException e) {
			throw new MoverException(e);
		}
		
		int millis = Integer.parseInt(longDateToParse.substring(14, 20)) / 1000;
		
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(tmp);
		
		cal.set(Calendar.MILLISECOND, millis);
	
		return cal.getTime();
	}
	
	/****
	public static void main(String[] args) {
		try {
			FileCreationDateFormat fcd = new FileCreationDateFormat();

			Date d1 = new Date();

			String t1 = fcd.format(d1);

			System.out.println("First date = " + d1 + " filename: " + t1);

			Date d2 = fcd.parse(t1);
			String t2 = fcd.format(d2);
			
			System.out.println("Second date = " + d2 + " filename: " + t2);
			
			if (d1.equals(d2)) {
				System.out.println("They are equal");
			} else {
				System.out.println("They are not equal");
			}
		
		} catch (MoverException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	****/
	
}
