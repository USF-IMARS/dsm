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
 * RDR represents a RDR product. RDRMover uses it.
 * @version 3.0.1 Added RDR subtable
 */
class RDR implements MoverRawProduct
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
	
	private File rdrFile;
	private NPOESSFilename npoessFilename;

	RDR(String taskName, File rdrFile, DSMAdministrator dsm)
	{
		
		//System.out.println("RDR: " + taskName + " rdrFile: " + rdrFile);
		this.taskName = taskName;
		this.rdrFile = rdrFile;
		
		try {
			npoessFilename = new NPOESSFilename(rdrFile.getName(),rdrFile.getAbsolutePath());
		} catch (MoverException e) {
			fault = e.getMessage();
			return;
		}
		
		spacecraft = npoessFilename.getSpacecraftId().toString();

		Date rdrStartDateTime = npoessFilename.getStartDateTime();
		
		Date rdrStopTimeOnly = npoessFilename.getStopTime();
		
		// throw the two together to make the stop date+time
		// note: this seems like it never works right, we get time zone shifts or savings time issues...
		// but I am going to try it again anyway...
		
		// includes both start date and time but only care about the date to make the stop date+time
		Calendar startDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		startDateTime.setTime(rdrStartDateTime);  
		Calendar stopDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		stopDateTime.setTime(rdrStopTimeOnly);
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
		
		this.startTime = rdrStartDateTime;
		this.stopTime = stopDateTime.getTime();
		
		String sensor = getPrimaryScienceSensor(npoessFilename);
		String domain = npoessFilename.getDomainDescriptionString();
		
		
		// All productTypeNames created by RDRMover begin with 'drl.'
		String productTypeName = "drl." + spacecraft.toLowerCase() + "." + sensor + ".rdr";
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
		    fault = "Product type " + productTypeName + " does not exist for file " + rdrFile.getName();
		    return;
		}
		
		//System.out.println("RDR ProductType -- " + productType);

		try
		{
		    pass = dsm.getPass(spacecraft, startTime, stopTime);
		    if (pass == null) {
			pass = dsm.getPass(spacecraft,stopTime);
			if (pass == null) {
			    pass = dsm.getPass(spacecraft,startTime);
			    if (pass == null) {
				long t = (startTime.getTime() + stopTime.getTime()) / 2L;
				pass = dsm.getPass(spacecraft,new Date(t));
			    }
			}
		    }
		    if (pass == null)
			{
			    fault = "RDR has no pass for " + toString();
			    return;
			}

		    // For VIIRS RDRs, if the pass already contains
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
		
		//System.out.println("RDR pass -- " + pass);
		//System.out.println("RDR productType string --" + productTypeName);

		
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
	 * Get an error message if there was a problem creating this RDR.
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
		return rdrFile;
	}

	public final File getRecordFile()
	{
		return rdrFile;
	}

        public String getFileNames()
        {
	    return rdrFile.getName();
        }

	public void delete() throws MoverException
	{
		if (rdrFile.exists())
		{
			rdrFile.delete();
		}
	}

	public void move(File directory)
	{
		if (rdrFile.exists())
		{
			File newcrec = new File(directory,rdrFile.getName());
			rdrFile.renameTo(newcrec);
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
		File record = new File(targetDirectory,rdrFile.getName());
		try {

			p.addResource("DATA",record.getPath());
			p.setSubproduct("ProductionDataSets");
			
			
			
			for (ProductIdentifiers pi : npoessFilename.getProductIdentifiers()) {
				if ((pi == ProductIdentifiers.RATMS) || (pi == ProductIdentifiers.RCRIS) || (pi == ProductIdentifiers.RVIRS) || (pi == ProductIdentifiers.RONPS) ||
				    (pi == ProductIdentifiers.ROLPS) || (pi == ProductIdentifiers.ROTCS))
				{
					p.addAttribute("packetCount", "0");
					break; // we only support ONE sensor in the name, not include A&E
				}
			}
		} catch (Exception e1) {
			throw new MoverException(e1);
		}
		return p;
	}



	/**cd sr		
	 * Two RDR products are equal if they are the same type, have the same start time, and both
	 * reference the same pass. Object may also be a Product.
	 */
	public boolean equals(Object o)
	{
		boolean match = false;
		if (o != null)
		{
			if (o instanceof RDR)
			{
				RDR p = (RDR)o;
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
