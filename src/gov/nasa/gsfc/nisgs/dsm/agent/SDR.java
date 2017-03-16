/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.dsm.Product;
import gov.nasa.gsfc.nisgs.dsm.ProductType;
import gov.nasa.gsfc.nisgs.dsm.agent.rdr.NPOESSFilename;
import gov.nasa.gsfc.nisgs.dsm.agent.rdr.ProductIdentifiers;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * SDR represents a SDR product. SDRMover uses it.
 * @version 3.0.1 Added SDR subtable
 */
class SDR implements MoverRawProduct
{
	private static String CREATOR = "Unavailable";
	private static String CREATOR_VERSION = "Not used";
	private String taskName;
	private Pass pass;
	private String spacecraft;
	private Date startTime;
	private Date stopTime;
	private ProductType productType;
	private String fault = null;
        private DsmLog log;
	
	private File sdrFile;
	private NPOESSFilename npoessFilename;

	SDR(String taskName, File sdrFile, DSMAdministrator dsm)
	{
		
		//System.out.println("SDR: " + taskName + " sdrFile: " + sdrFile);
		this.taskName = taskName;
		this.sdrFile = sdrFile;
		
		try {
			npoessFilename = new NPOESSFilename(sdrFile.getName(),sdrFile.getAbsolutePath());
		} catch (MoverException e) {
			fault = e.getMessage();
			return;
		}
		
		spacecraft = npoessFilename.getSpacecraftId().toString();

		Date sdrStartDateTime = npoessFilename.getStartDateTime();
		
		Date sdrStopTimeOnly = npoessFilename.getStopTime();
		
		// throw the two together to make the stop date+time
		// note: this seems like it never works right, we get time zone shifts or savings time issues...
		// but I am going to try it again anyway...
		
		// includes both start date and time but only care about the date to make the stop date+time
		Calendar startDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		startDateTime.setTime(sdrStartDateTime);  
		Calendar stopDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		stopDateTime.setTime(sdrStopTimeOnly);
                //Subtracted a second to make pass disjoint
                //stopDateTime.add(Calendar.SECOND, -1);
		
		// get the hour of the start date+time... and the hour of the stop time
		// if the hour of stop time is less than that of start date+time, then it must be
		// the next day when the stop occurred.  If that's the case, make the date the next day.
		int startDateHour = startDateTime.get(Calendar.HOUR_OF_DAY);
		int stopTimeHour = stopDateTime.get(Calendar.HOUR_OF_DAY);
		
		// roll the date forward if needed ...
		if (stopTimeHour < startDateHour) {
			startDateTime.add(Calendar.DAY_OF_YEAR, 1);  // this should roll higher fields...
		}
		
		// now take the date value and put them with stopTime, this gets us the stop date+time
		stopDateTime.set(Calendar.YEAR, startDateTime.get(Calendar.YEAR));
		stopDateTime.set(Calendar.DAY_OF_YEAR, startDateTime.get(Calendar.DAY_OF_YEAR));
		
		this.startTime = sdrStartDateTime;
		this.stopTime = stopDateTime.getTime();
		
		String sensor = getPrimaryScienceSensor(npoessFilename);
		String domain = npoessFilename.getDomainDescriptionString();
		
		
		// Look up product type in SDRType
		String productTypeName = SDRType.findProductType(npoessFilename.toString());
		if(productTypeName == null) {
		    fault = "Filename " + npoessFilename + " has no product type";
		    return;
		}
		try
		{
			productType = dsm.getProductType(productTypeName);
		}
		catch (Exception e2)
		{
			fault = e2.getMessage();
			return;
		}

		// Crap out now if there is no ProductType registered
		if(productType == null) {
		    fault = "Product type " + productTypeName + " does not exist for file " + sdrFile.getName();
		    return;
		}
		
		//System.out.println("SDR ProductType -- " + productType);

		try
		{
		    
		    pass = dsm.getPass(spacecraft,startTime, stopTime);

		    if (pass == null)
			{
			    fault = "SDR has no pass for " + toString();
			    return;
			}
		    
		    // For VIIRS SDRs, if the pass already contains
		    // a product of this type, it's an error
		    Product oldProd = dsm.getProduct(productTypeName, pass);
		    if(oldProd != null) {
			fault = "Pass " + pass.getId() + " already has a "
			    + productTypeName;
			return;
		    }
		}
		catch (Exception e1)
		{
			fault = e1.getMessage();
			return;
		}
		
		//System.out.println("SDR pass -- " + pass);
		//System.out.println("SDR productType string --" + productTypeName);

		
	}

	private String getPrimaryScienceSensor(NPOESSFilename npoessFilename) {
		String pids = npoessFilename.productIndentifierstoString();
		
		String result = "";
		if (pids.contains("RVIRS")) {
			result = "viirs";
		} else if (pids.contains("CRIS")) {
			result = "cris";
		} else if (pids.contains("ATMS")) {
			result = "atms";
		} else if (pids.contains("ONPS")) {
			result = "ronps";
		} else if (pids.contains("OLPS")) {
			result = "rolps";
		} else if (pids.contains("OTCS")) {
			result = "rotcs";
		}
		return result;
	}

	/**
	 * Get an error message if there was a problem creating this SDR.
	 * @return error message or null if no problem exists.
	 */
	public String getFault()
	{
		return fault;
	}

	public final ProductType getProductType()
	{
		return productType;
	}

	final public Pass getPass()
	{
		return pass;
	}

	final public Date getStartTime()
	{
		return startTime;
	}

	final public Date getStopTime()
	{
		return stopTime;
	}

	public final File getDataFile()
	{
		return sdrFile;
	}

	public final File getRecordFile()
	{
		return sdrFile;
	}

        public String getFileNames()
        {
	    return sdrFile.getName();
        }

	public void delete() throws MoverException
	{
		if (sdrFile.exists())
		{
			sdrFile.delete();
		}
	}

	public void move(File directory)
	{
		if (sdrFile.exists())
		{
			File newcrec = new File(directory,sdrFile.getName());
			sdrFile.renameTo(newcrec);
		}
	}

	public  Product createProduct(File targetDirectory) throws MoverException
	{
		
		//System.out.println("StarTme -- " + startTime);
		//System.out.println("StopTime -- " + stopTime);
		//System.out.println("TaskName -- " + taskName);
		//System.out.println("productType -- " + productType.getName());
		//System.out.println("Pass -- " + pass);
		
		
		Product p = new Product(startTime,stopTime,taskName,productType.getName(),pass);
		float e = pass.getEastLongitude();
		float w = pass.getWestLongitude();
		float n = pass.getNorthLatitude();
		float s = pass.getSouthLatitude();
		p.setCorners(n,s,e,w);
		float clat = pass.getCenterLatitude();
		float clong = pass.getCenterLongitude();
		p.setCenter(clat,clong);
		p.setAlgorithm(CREATOR,CREATOR_VERSION);
		File record = new File(targetDirectory,sdrFile.getName());
		try {
			p.addResource("DATA",record.getPath());

		} catch (Exception e1) {
			throw new MoverException(e1);
		}

		return p;
	}



	/**cd sr		
	 * Two SDR products are equal if they are the same type, have the same start time, and both
	 * reference the same pass. Object may also be a Product.
	 */
	public boolean equals(Object o)
	{
		boolean match = false;
		if (o != null)
		{
			if (o instanceof SDR)
			{
				SDR p = (SDR)o;
				match = p.getProductType().equals(getProductType()) &&
				p.getStartTime().equals(getStartTime()) &&
				p.getPass().equals(getPass());
			}
			else if (o instanceof Product)
			{
				Product p = (Product)o;
				match = p.getProductType().equals(getProductType()) &&
				p.getStartTime().equals(getStartTime()) &&
				p.getPass().equals(getPass());
			}
		}
		return match;
	}

}
