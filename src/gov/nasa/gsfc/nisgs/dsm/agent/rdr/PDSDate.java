/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;



import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;



/**
 * Manipulate a date given a packet time in the following format, calculate year,
 * day, month, day of year, hour, minute and seconds.
 * The 10 lines of code which calculate year, day, month were taken from getCal
 * in TimeDate in the checker package.
 * The day of year tables were taken from http://disc.gsfc.nasa.gov/julian_calendar.shtml
 * Note: much of this was taken from an earlier implementation in CRECBuilder
 *
 */
public class PDSDate implements Comparable<PDSDate> {
	private static final double EPOCH_DATE = 2436205.0; //2436204.5;
	private static final long MillisPerDay = 86400000L; // I am led to believe this is not strictly true
	private static final long MicrosPerDay = MillisPerDay * 1000L;
	private static final long DaysBetweenEpochs = 4383L;  // From Jan 1, 1958 to Jan 1, 1970 (not including this one)
	private static final long MillisBetweenEpochs = DaysBetweenEpochs * MillisPerDay;
	// from -- http://disc.gsfc.nasa.gov/julian_calendar.shtml#non-leap_year
	private int[][] dayInPerpetual = { /* 31 x 12 */
		//Day Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec 
		{  1, 32, 60,  91, 121, 152, 182, 213, 244, 274, 305, 335 },
		{  2, 33, 61,  92, 122, 153, 183, 214, 245, 275, 306, 336 },
		{  3, 34, 62,  93, 123, 154, 184, 215, 246, 276, 307, 337 },
		{  4, 35, 63,  94, 124, 155, 185, 216, 247, 277, 308, 338 },
		{  5, 36, 64,  95, 125, 156, 186, 217, 248, 278, 309, 339 },
		{  6, 37, 65,  96, 126, 157, 187, 218, 249, 279, 310, 340 },
		{  7, 38, 66,  97, 127, 158, 188, 219, 250, 280, 311, 341 },
		{  8, 39, 67,  98, 128, 159, 189, 220, 251, 281, 312, 342 },
		{  9, 40, 68,  99, 129, 160, 190, 221, 252, 282, 313, 343 },
		{ 10, 41, 69, 100, 130, 161, 191, 222, 253, 283, 314, 344 },
		{ 11, 42, 70, 101, 131, 162, 192, 223, 254, 284, 315, 345 },
		{ 12, 43, 71, 102, 132, 163, 193, 224, 255, 285, 316, 346 },
		{ 13, 44, 72, 103, 133, 164, 194, 225, 256, 286, 317, 347 },
		{ 14, 45, 73, 104, 134, 165, 195, 226, 257, 287, 318, 348 },
		{ 15, 46, 74, 105, 135, 166, 196, 227, 258, 288, 319, 349 },
		{ 16, 47, 75, 106, 136, 167, 197, 228, 259, 289, 320, 350 },
		{ 17, 48, 76, 107, 137, 168, 198, 229, 260, 290, 321, 351 },
		{ 18, 49, 77, 108, 138, 169, 199, 230, 261, 291, 322, 352 },
		{ 19, 50, 78, 109, 139, 170, 200, 231, 262, 292, 323, 353 },
		{ 20, 51, 79, 110, 140, 171, 201, 232, 263, 293, 324, 354 },
		{ 21, 52, 80, 111, 141, 172, 202, 233, 264, 294, 325, 355 },
		{ 22, 53, 81, 112, 142, 173, 203, 234, 265, 295, 326, 356 },
		{ 23, 54, 82, 113, 143, 174, 204, 235, 266, 296, 327, 357 },
		{ 24, 55, 83, 114, 144, 175, 205, 236, 267, 297, 328, 358 },
		{ 25, 56, 84, 115, 145, 176, 206, 237, 268, 298, 329, 359 },
		{ 26, 57, 85, 116, 146, 177, 207, 238, 269, 299, 330, 360 },
		{ 27, 58, 86, 117, 147, 178, 208, 239, 270, 300, 331, 361 },
		{ 28, 59, 87, 118, 148, 179, 209, 240, 271, 301, 332, 362 },
		{ 29,  0, 88, 119, 149, 180, 210, 241, 272, 302, 333, 363 },
		{ 30,  0, 89, 120, 150, 181, 211, 242, 273, 303, 334, 364 },
		{ 31,  0, 90,   0, 151,   0, 212, 243,   0, 304,   0, 365 }
	};

	// from http://disc.gsfc.nasa.gov/julian_calendar.shtml#leap_year
	private int[][] dayInLeapYear = { /* 31 x 12 */
		// Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec 
		{  1, 32, 61,  92, 122, 153, 183, 214, 245, 275, 306, 336 },
		{  2, 33, 62,  93, 123, 154, 184, 215, 246, 276, 307, 337 },
		{  3, 34, 63,  94, 124, 155, 185, 216, 247, 277, 308, 338 },
		{  4, 35, 64,  95, 125, 156, 186, 217, 248, 278, 309, 339 },
		{  5, 36, 65,  96, 126, 157, 187, 218, 249, 279, 310, 340 },
		{  6, 37, 66,  97, 127, 158, 188, 219, 250, 280, 311, 341 },
		{  7, 38, 67,  98, 128, 159, 189, 220, 251, 281, 312, 342 },
		{  8, 39, 68,  99, 129, 160, 190, 221, 252, 282, 313, 343 },
		{  9, 40, 69, 100, 130, 161, 191, 222, 253, 283, 314, 344 },
		{ 10, 41, 70, 101, 131, 162, 192, 223, 254, 284, 315, 345 },
		{ 11, 42, 71, 102, 132, 163, 193, 224, 255, 285, 316, 346 },
		{ 12, 43, 72, 103, 133, 164, 194, 225, 256, 286, 317, 347 },
		{ 13, 44, 73, 104, 134, 165, 195, 226, 257, 287, 318, 348 },
		{ 14, 45, 74, 105, 135, 166, 196, 227, 258, 288, 319, 349 },
		{ 15, 46, 75, 106, 136, 167, 197, 228, 259, 289, 320, 350 },
		{ 16, 47, 76, 107, 137, 168, 198, 229, 260, 290, 321, 351 },
		{ 17, 48, 77, 108, 138, 169, 199, 230, 261, 291, 322, 352 },
		{ 18, 49, 78, 109, 139, 170, 200, 231, 262, 292, 323, 353 },
		{ 19, 50, 79, 110, 140, 171, 201, 232, 263, 293, 324, 354 },
		{ 20, 51, 80, 111, 141, 172, 202, 233, 264, 294, 325, 355 },
		{ 21, 52, 81, 112, 142, 173, 203, 234, 265, 295, 326, 356 },
		{ 22, 53, 82, 113, 143, 174, 204, 235, 266, 296, 327, 357 },
		{ 23, 54, 83, 114, 144, 175, 205, 236, 267, 297, 328, 358 },
		{ 24, 55, 84, 115, 145, 176, 206, 237, 268, 298, 329, 359 },
		{ 25, 56, 85, 116, 146, 177, 207, 238, 269, 299, 330, 360 },
		{ 26, 57, 86, 117, 147, 178, 208, 239, 270, 300, 331, 361 },
		{ 27, 58, 87, 118, 148, 179, 209, 240, 271, 301, 332, 362 },
		{ 28, 59, 88, 119, 149, 180, 210, 241, 272, 302, 333, 363 },
		{ 29, 60, 89, 120, 150, 181, 211, 242, 273, 303, 334, 364 },
		{ 30,  0, 90, 121, 151, 182, 212, 243, 274, 304, 335, 365 },
		{ 31,  0, 91,   0, 152,   0, 213, 244,   0, 305,   0, 366 }
	};
	
	
	
	private long rawDay;
	private long rawMillis;
	private long rawMicros;
	private long year;
	private long month;
	private long day;
	private long hours;
	private long minutes;
	private long seconds;
	private long milliseconds;
	private long dayOfYear;
	
	private long packetTime;
	
	/**
	 * Constructor, supply a packet time in the following format from epoch 1/1/58.
	 * <pre>
	 *    Uint16 day since 1/1/1958
	 *    Uint32 millisecond of day
	 *    Uint16 microsecond of millisecond
	 * </pre>
	 * @param packetTime
	 */
	public PDSDate(long packetTime) {
		this.packetTime = packetTime;
		rawDay = (packetTime >> 48) & 0x0ffffL;
		rawMillis = (packetTime >> 16) & 0x0ffffffffL;
		rawMicros = packetTime & 0x0ffffL;
		
		calculate();
	}
	

	
	/**
	 * Rough approximation into Mission Epoch date/time.  
	 * @param year the year
	 * @param month the month, starts at 0 for January
	 * @param day the day of month, starts at 1
	 * @param hour the hour of day
	 * @param minute the minute of the hour
	 * @param second the seconds in the minute
	 * @param milliseconds the milliseconds of the second
	 * @param microseconds of the millis
	 */
	
	public PDSDate(int year, int month, int day, int hour, int minute, int second, int milliseconds, int microseconds) { 
		
		
	
		// add up all the days since the epoch
		int days = this.daysSinceEpoch(year);
		int dayOfYear = this.dayOfYear(year, month, day-1);
		
		//System.out.println("Days from 1/1/1958 to " + year + " -- " + days + " days of year: " + dayOfYear);
		
		days += (dayOfYear-1);  // subtract one day since a full day has not elapsed on the specified month/day/time
		
		
		// total hrs since epoch
	//	int hours = days * 24 + hour;
		
		int hours = hour;
		
		int minutes = hours * 60 + minute;
		
		int seconds = minutes * 60 + second;
		
		long t_milliseconds = seconds * 1000L + milliseconds;
		
		long t_microseconds = t_milliseconds * 1000L + microseconds;
	      
		//double cDaysSinceEpoch = (year - 1958) * 365.25;
		/**
		System.out.println("Calc days since epoch[" + cDaysSinceEpoch + "]" +
						   " Days since epoch [" + days + "]" +
				           " dayOfYear [" + dayOfYear + "]" +
				           " hours[" + hours + "]" +
				           " minutes total[" + minutes + "]" +
				           " seconds total[" + seconds + "]" +
				           " t_milliseconds[" + t_milliseconds + "]" +
				           " t_microseconds[" + t_microseconds + "]");
		**/
		this.rawDay = days;
		this.rawMillis = t_milliseconds;
		this.rawMicros =  microseconds;
		
		this.calculate();
		
		/**
		System.out.println("before Year: " + year);
		System.out.println("before Month: " + month);
		System.out.println("before Day: " + day);
		System.out.println("before Hours: " + hour);
		System.out.println("before Minutes: " + minute);
		System.out.println("before Seconds: " + second);
		System.out.println("before milliseconds: " + milliseconds);
		
		
		// NOTE: setting the time zone here DOES make a difference when the time in millis
		// is retrieve below.  This may be a hack... not sure.  
		//
		Calendar pdsCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		pdsCal.set(Calendar.YEAR, year);
		pdsCal.set(Calendar.MONTH, month);
		pdsCal.set(Calendar.DAY_OF_MONTH, day);
		pdsCal.set(Calendar.HOUR_OF_DAY, hour);
		pdsCal.set(Calendar.MINUTE, minute);
		pdsCal.set(Calendar.SECOND, second);
		pdsCal.set(Calendar.MILLISECOND, milliseconds);
		
		this.year = pdsCal.get(Calendar.YEAR);
		this.month = pdsCal.get(Calendar.MONTH);
		this.day = pdsCal.get(Calendar.DAY_OF_MONTH);
		this.hours = pdsCal.get(Calendar.HOUR_OF_DAY);
		this.minutes = pdsCal.get(Calendar.MINUTE);
		this.seconds = pdsCal.get(Calendar.SECOND);
		this.milliseconds = pdsCal.get(Calendar.MILLISECOND);
		this.dayOfYear = pdsCal.get(Calendar.DAY_OF_YEAR);

		/**
		System.out.println("after Year: " + this.year);
		System.out.println("after Month: " + this.month);
		System.out.println("after Day: " + this.day);
		System.out.println("after Hours: " + this.hours);
		System.out.println("after Minutes: " + this.minutes);
		System.out.println("after Seconds: " + this.seconds);
		System.out.println("after milliseconds: " + this.milliseconds);
		**
		
		//SimpleDateFormat df = new SimpleDateFormat();
		
		//System.out.println("New date: " + df.format(pdsCal.getTime()));
		// calculate the long 64-bit mission style time
		// now get the milliseconds total since the java epoch 1/1/1970...
		
		
		//NOTE: beware the timezone here.  If it set in UTC above then everything
		// seems to work... if not it will be shifted by YOUR timezone.
		//
		long millisSinceJavaEpoch = pdsCal.getTimeInMillis();
		//System.out.println("Millis in JavaEpoch: " + millisSinceJavaEpoch);
		
		// and add in the fixed amount from 1970 to 1958
		long millisSinceMissionEpoch = millisSinceJavaEpoch + MillisBetweenEpochs;
		
		//System.out.println("Millis between Epochs:" + MillisBetweenEpochs);
		
		//System.out.println("Millis since 1958: " + millisSinceMissionEpoch);
		
		// get the days
		rawDay = millisSinceMissionEpoch / MillisPerDay;
		
		rawMillis = millisSinceMissionEpoch - (rawDay * MillisPerDay);
		
		rawMicros = 0L;
		
		**/
		packetTime =   ((rawDay << 48)    & 0xffff000000000000L);
		packetTime |= ((rawMillis << 16)  & 0x0000ffffffff0000L);
		packetTime |=  (rawMicros         & 0x000000000000ffffL);
		
		//System.out.println("Packe ttime: " + packetTime);
		
	}

	
	/**
	 * Given a time in Java-Epoch in Date, put into the mission format in mission epoch.
	 * The epoch is converted although the timezone is set to UTC.
	 * @param dateTime the Date/Time of interest
	 */
/**
	public PDSDate(Date dateTime) {
		Calendar pdsCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	
		pdsCal.setTime(dateTime);
		year = pdsCal.get(Calendar.YEAR);
		month = pdsCal.get(Calendar.MONTH);
		day = pdsCal.get(Calendar.DAY_OF_MONTH);
		hours = pdsCal.get(Calendar.HOUR_OF_DAY);
		minutes = pdsCal.get(Calendar.MINUTE);
		seconds = pdsCal.get(Calendar.SECOND);
		milliseconds = pdsCal.get(Calendar.MILLISECOND);
		dayOfYear = pdsCal.get(Calendar.DAY_OF_YEAR);
		// calculate the long 64-bit mission style time
		// now get the milliseconds total since the java epoch 1/1/1970...
		
		long millisSinceJavaEpoch = pdsCal.getTimeInMillis();
		
		// and add in the fixed amount from 1970 to 1958
		long millisSinceMissionEpoch = millisSinceJavaEpoch + MillisBetweenEpochs;
		
		// get the days
		rawDay = millisSinceMissionEpoch / MillisPerDay;
		
		rawMillis = millisSinceMissionEpoch - (rawDay * MillisPerDay);
		
		rawMicros = 0L;
		
		packetTime =   ((rawDay << 48)    & 0xffff000000000000L);
		packetTime |= ((rawMillis << 16) & 0x0000ffffffff0000L);
		packetTime |=  (rawMicros        & 0x000000000000ffffL);
		
	}
**/
	/**
	 * Return the 64-bit timestamp
	 * @return long
	 */
	public long getPacketTime() {
		return this.packetTime;
	}
	
	/**
	 * Get the raw day from the packet time
	 * @return a long
	 */
	public long getRawDay() {
		return rawDay;
	}
	/**
	 * Get the raw milliseconds from the packet time
	 * @return a long
	 */
	public long getRawMillis() {
		return rawMillis;
	}
	/**
	 * Get the raw microseconds from the packet time
	 * @return a long
	 */
	public long getRawMicros() {
		return rawMicros;
	}
	

	/**
	 * Get the calculated month of the year
	 * @return a long
	 */
	public long getMonth() {
		return month;
	}
	/**
	 * Get the calculated year, four digits
	 * @return a long
	 */
	public long getYear() {
		return year;
	}
	/**
	 * Get the calculated day of month
	 * @return an integer
	 */
	public long getDayOfMonth() {
		return day;
	}
	/**
	 * Get the calculated day of the year
	 * @return an integer
	 */
	public long getDayOfYear() {
		return dayOfYear;
	}
	/**
	 * Get the calculated milliseconds of second
	 * @return a long
	 */
	public long getMilliseconds() {
		return milliseconds;
	}
	
	/**
	 * Get any remaining microseconds of millisecond
	 * @return a long
	 */
	public long getMicroseconds() {
		return rawMicros;
	}
	/**
	 * Get the calculated seconds of minute
	 * @return a long
	 */
	public long getSeconds() {
		return seconds;
	}
	/**
	 * Get the calculated minutes of hour
	 * @return a long
	 */
	public long getMinutes() {
		return minutes;
	}
	/**
	 * Get the calculated hours of day
	 * @return a long
	 */
	public long getHours() {
		return hours;
	}
	
	/**
	 * Return the micros since the mission epoch
	 * @return 64 bits of microseconds, signed
	 */
	public long getMicrosSinceEpoch() {
		long millis = (this.rawDay * MillisPerDay) + this.rawMillis;
		
		long micros = (millis * 1000L) + this.rawMicros;
		
		return micros;
	}
	
	/**
	 * Static variation given a 64-bit mission time (segmented)
	 * @param packetTime 48 bits of time, 16 bits millis and 16 bits of micros
	 * @return the micros since epoch in a signed 64-bit quantity
	 */
	public static long getMicrosSinceEpoch(long packetTime) {
		long rawDay = (packetTime >> 48) & 0x0ffffL;
		long rawMillis = (packetTime >> 16) & 0x0ffffffffL;
		long rawMicros = packetTime & 0x0ffffL;
		
		long millis = (rawDay * MillisPerDay) + rawMillis;

		long micros = (millis * 1000L) + rawMicros;

		return micros;
	}
	
	/**
	 * Determine if this is a leapyear or not
	 * @param year
	 * @return true if 
	 */
	public boolean isLeapYear (int year) {
		//return ((((year % 4) == 0) && ((year % 100) != 0)) || ((year % 400) == 0));
		
		boolean leapYear = false;
		if ((year % 400) ==  0)
	       leapYear = true;
		else if ((year % 100) == 0)
	       leapYear = false;
	    else if ((year % 4) == 0)
	       leapYear = true;
	    else
	       leapYear = false;

		return leapYear;
	}
	
	/**
	 * Count up the days since the epoch per year.  The leaps year days
	 * are added based on the leap year calculation... no provision is 
	 * made to check that the year given is on or before the epoch year
	 * @param year a year since 1958
	 * @return the total days including leap year days since that year
	 */
	private int daysSinceEpoch(int year) {
		// add up all the days since the epoch
		int days = 0;
		for (int y = 1958; y < year; y++) {
			if (isLeapYear(y)) {
				days += 366;
			} else {
				days += 365;
			}
		}
		return days;
	}
	
	/**
	 * Calculate the day of the year using the calendar tables.
	 * @param month the month of the year starting at zero for January
	 * @param day the day of the month starting at zero for the 1st day
	 * @return the day of the year
	 */
	private int dayOfYear(int year, int month, int day) {
		int dayOfYear = 0;
		if (isLeapYear(year)) {
			dayOfYear = dayInLeapYear[day][month];
		} else {
			dayOfYear = dayInPerpetual[day][month]; 
		}
		return dayOfYear;
	}
	/**
	 * Return the Java Date, the epoch is converted and the timezone is UTC
	 * The returned Date if simply printed will be relative this your timezone.  To convert to UTC,
	 * use the DateFormat classes.
	 * @return the Date in the local time zone, specify the UTC to display it in proper mission time timezone
	 */
	public Date getDate() {

		//System.out.println(this.toString());
		
		//Calendar pdsCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		
		Calendar pdsCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		//pdsCal.set(Calendar.ZONE_OFFSET, 0);
		//pdsCal.set(Calendar.DST_OFFSET, 0);
		
		pdsCal.set(Calendar.YEAR,(int)this.getYear());
		
		//System.out.println("Time?  " + pdsCal.getTime());
		pdsCal.set(Calendar.MONTH, (int)this.getMonth());
		//System.out.println("Time?  " + pdsCal.getTime());
		pdsCal.set(Calendar.DAY_OF_YEAR, (int)this.getDayOfYear());
		//System.out.println("Time?  " + pdsCal.getTime());
		pdsCal.set(Calendar.HOUR_OF_DAY,(int)this.getHours());
		//System.out.println("Time?  " + pdsCal.getTime());
		pdsCal.set(Calendar.MINUTE,(int)this.getMinutes());
		//System.out.println("Time?  " + pdsCal.getTime());
		pdsCal.set(Calendar.SECOND,(int)this.getSeconds());
		//System.out.println("Time?  " + pdsCal.getTime());
		pdsCal.set(Calendar.MILLISECOND, (int)this.getMilliseconds());
		//System.out.println("Time?  " + pdsCal.getTime());

		//Calendar output = Calendar.getInstance();   
		//output.set(Calendar.YEAR, pdsCal.get(Calendar.YEAR));   
		//output.set(Calendar.MONTH, pdsCal.get(Calendar.MONTH));   
		//output.set(Calendar.DAY_OF_MONTH, pdsCal.get(Calendar.DAY_OF_MONTH));   
		//output.set(Calendar.HOUR_OF_DAY, pdsCal.get(Calendar.HOUR_OF_DAY));   
		//output.set(Calendar.MINUTE, pdsCal.get(Calendar.MINUTE));   
		//output.set(Calendar.SECOND, pdsCal.get(Calendar.SECOND));   
		//output.set(Calendar.MILLISECOND, pdsCal.get(Calendar.MILLISECOND)); 
		
		return pdsCal.getTime();
		//return output.getTime();
	}

	
	/**
	 * Return just the raw fields
	 */
	public String toRawFields() {
		
		return("RawDay[" + rawDay + "]" +
		          				" RawMillis[" + rawMillis + "]" +
		        				" RawMicros[" + rawMicros + "]");
	}
	
	/**
	 * Return just the raw fields, static version
	 */
	public static String toRawFields(long packetTime) {
		
		long rawDay = (packetTime >> 48) & 0x0ffffL;
		long rawMillis = (packetTime >> 16) & 0x0ffffffffL;
		long rawMicros = packetTime & 0x0ffffL;
		
		return("RawDay[" + rawDay + "]" +
		          				" RawMillis[" + rawMillis + "]" +
		        				" RawMicros[" + rawMicros + "]");
	}
	
	/**
	 * Convert the results to String
	 */
	@Override
	public String toString() {
		return "Year[" + this.getYear() + "]" + 
				" Month[" + this.getMonth() + "]" +
				" Day[" + this.getDayOfMonth() + "]" +
				" (DayOfYear[" + this.getDayOfYear() + "])" +
				" Hours[" + this.getHours() + "]" +
				" Minutes[" + this.getMinutes() + "]" +
				" Seconds[" + this.getSeconds() + "]" +
				" Milliseconds[" + this.getMilliseconds() + "]" +
				" Microseconds[" + this.getMicroseconds() + "]" +
				" -- RawDay[" + this.getRawDay() + "]" +
				" RawMillis[" + this.getRawMillis() + "]" +
				" RawMicros[" + this.getRawMicros() + "]";
		
	}
		 
	/**
	 * Calculate the new year, month, days of year, hours, minutes, seconds, milliseconds...
	 */
	private void calculate() {
		
		//System.out.println( 
		//		" rawDay: " + rawDay + 
		//		" rawMillis: " + rawMillis + " rawMicros: " + rawMicros);
		
		double jdUTC = rawDay + EPOCH_DATE;
		
	      long l,n;
	      
	      // alternative
	      /**
	      long t_day = rawDay;
	      int yearCount = 0;
	      int yearSum = 1958;
	      boolean notDone = true;
	      while (notDone) {
	    	  
	    	  	int days = 0;
				if (isLeapYear(yearSum)) {
					days = dayInLeapYear[30][11];
				} else {
					days = dayInPerpetual[30][11]; 
				}
				
				t_day -= days;
				++yearCount;
				++yearSum;
				
				if (t_day < 365) {
					notDone = false;
				}
				
	      }
	      **/
	     // System.out.println("Loop Year = " + yearSum + " Days Left = " + t_day);
	      
	      // original
	      l = (long)(jdUTC + 0.5) + 68569L;
	      n = 4L*l/146097L;
	      l = l - (146097L*n+3L)/4L;
	      year = (4000L*(l+1L)/1461001L);
	      l = l - 1461L*year/4L + 31L;
	      month = (80L*l/2447L);
	      day = (l-2447L*month/80L);
	      l = month/11L;
	      month = month + 2L - 12L*l;
	      year = (100L*(n-49L) + year + l);
	      
	      
	    //  System.out.println(" year = " + year);
	      
	      month = month - 1;
	      
	     // System.out.println(" month = " + month);
	      
	    //  System.out.println(" Day = " + day);
	      
	      if (isLeapYear((int) year)) {
	    	  dayOfYear = dayInLeapYear[(int)day-1][(int)month]; //-1];
	      } else {
	    	  dayOfYear = dayInPerpetual[(int)day-1][(int)month]; //-1];
	      }
	     // dayOfYear = 1;
	     // System.out.println(" dayOfYear = " + dayOfYear);
	      
	      
	      long tsecs = (rawMillis / 1000L);
	     // System.out.println(" tsecs = " + tsecs);
	      
	      long tmins = tsecs/60L;
	      
	      hours = (tmins / 60L);
	     // hours = 0;
	     //System.out.println(" Hours = " + hours);
	     
	      
	      minutes = tmins - (hours * 60L);
	     // minutes = 0;
	     // System.out.println(" minutes = " + minutes);
	      
	      seconds = tsecs - ((minutes * 60L) + (hours * 60L * 60L));
	      
	      //seconds = 0;
	      //System.out.println(" seconds = " + seconds);
	      
	      milliseconds = rawMillis - (tsecs * 1000L);
	      
	      
	}	
	
/**
	public static PDSDate createTimeDate(String dateStr, DateFormat dateFormat, String timeStr, DateFormat timeFormat) throws RtStpsException {
		
		//System.out.println("The date strings: " + dateStr + " time:" + timeStr);
		
		Calendar dateCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Date date = dateCal.getTime();
		date.setTime(0L);
		
		try {
			
			// NOTE: If the timezone is set to UTC, this seems to add 5 hrs to the value being parsed
			// So leaving it off, seems to retrieve the hours as they are specified in the string
			// 
			date = dateFormat.parse(dateStr);
		} catch (ParseException e) {
			throw new RtStpsException(e);
		}
	
		dateCal.setTime(date);

		Calendar timeCal = Calendar.getInstance(TimeZone.getTimeZone("UTC") );
		Date time = timeCal.getTime();
		time.setTime(0L);
		
		try {
			// NOTE: If the timezone is set to UTC, this seems to add 5 hrs to the value being parsed
			// So leaving it off, seems to retrieve the hours as they are specified in the string
			// 
			time = timeFormat.parse(timeStr);
		} catch (ParseException e) {
			throw new RtStpsException(e);
		}
		
		
		timeCal.setTime(time);
		
		// 	PDSDate(int year, int month, int day, int hour, int minute, int second, int milliseconds) { 
		int year = dateCal.get(Calendar.YEAR);
		int month = dateCal.get(Calendar.MONTH);
		int dayOfMonth  = dateCal.get(Calendar.DAY_OF_MONTH);
		int hourOfDay = timeCal.get(Calendar.HOUR_OF_DAY);
		int minute = timeCal.get(Calendar.MINUTE);
		int second = timeCal.get(Calendar.SECOND);
		int milliSecond = timeCal.get(Calendar.MILLISECOND);
		
		// 	PDSDate(int year, int month, int day, int hour, int minute, int second, int milliseconds) {
		/**
		System.out.println("year: " + year);
		System.out.println("month: " + month);
		System.out.println("dayOfMonth: " + dayOfMonth);
		System.out.println("hourOfDay: " + hourOfDay);
		System.out.println("minute: " + minute);
		System.out.println("second: " + second);
		System.out.println("milliSecond: " + milliSecond);
		
	//	PDSDate dateTime = new PDSDate(year, month, dayOfMonth, hourOfDay, minute, second, milliSecond);
		
		//SimpleDateFormat df = new SimpleDateFormat();
		
		//System.out.println("Created date/time: " + df.format(dateTime.getDate()));
		
	//	return dateTime;
	//}

**/

	@Override
	public int compareTo(PDSDate dateTime) {
		if (this.rawDay < dateTime.rawDay) {
			return -1;
		} else if (this.rawDay > dateTime.rawDay) {
			return 1;
		}
		// ok same day... check the time fields
		if (this.rawMillis < dateTime.rawMillis){
			return -1;
		} else if (this.rawMillis > dateTime.rawMillis) {
			return 1;
		}
		// ok same day and millis ... check the micros
		if (this.rawMicros < dateTime.rawMicros){
			return -1;
		} else if (this.rawMicros > dateTime.rawMicros) {
			return 1;
		}
		// if it gets this far, they are the same...
		return 0;
	}
	
}
