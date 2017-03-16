/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
 */
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.util.*;
import java.io.*;
import gov.nasa.gsfc.nisgs.dsm.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * PDS represents a PDS product. PdsMover uses it.
 * @version 3.0.1 Added PDS subtable
 */
class PDS implements MoverRawProduct
{
	private static String CREATOR = "rtstps";
	private static String CREATOR_VERSION = "3.05";
	private String taskName;
	private Pass pass;
	private String spacecraft;
	private Date startTime;
	private Date stopTime;
	private ProductType productType;
	private int packetCount = 0;
	private int gapCount = 0;
	private int missingPacketCount = 0;
	private String fault = null;

	//The data and crec absolute paths are local to the fep.
	//P1540064AAAAAAAAAAAAAA03199153407000.PDS
	private File constructionRecord;
	//P1540064AAAAAAAAAAAAAA03199153407001.PDS
	private File data;

	//I don't throw an exception out of the constructor because then I
	//need the file names even if the constructor fails.
	PDS(String taskName, File crec, DSMAdministrator dsm)
	{
		this.taskName = taskName;
		this.constructionRecord = crec;

		String filename = constructionRecord.getName();
		if (constructionRecord.length() == 0)
		{
			fault = constructionRecord.getName() +
			" Zero-length construction record. Check disk space.";
			return;
		}

		spacecraft = (filename.startsWith("P154"))? "AQUA" : "TERRA";

		String dataName = filename.substring(0,35) + "1.PDS";
		data = new File(crec.getParent(),dataName);
		if(!data.exists()) {
			fault = data.getName() + " does not exist.";
			return;
		}
		if (data.length() == 0)
		{
			fault = data.getName() + " Zero-length data file. Check disk space.";
			return;
		}

		try
		{
			readConstructionRecord(constructionRecord);
			pass = dsm.getPass(spacecraft,stopTime);
			if (pass == null)
			{
				pass = dsm.getPass(spacecraft,startTime);
				if (pass == null)
				{
					long t = (startTime.getTime() + stopTime.getTime()) / 2L;
					pass = dsm.getPass(spacecraft,new Date(t));
				}
			}
			if (pass == null)
			{
				fault = "PDS has no pass for " + toString();
				return;
			}
		}
		catch (Exception e1)
		{
			fault = e1.getMessage();
			return;
		}

		String productTypeName = null;
		String satellite = spacecraft.toLowerCase();
		String appid = filename.substring(4,8);
		if (appid.equals("0064") && (spacecraft.equals("TERRA") || spacecraft.equals("AQUA")))
		{
			productTypeName = satellite + ".modis.pds";
		}
		else if (appid.equals("0957") && spacecraft.equals("AQUA"))
		{
			productTypeName = satellite + ".gbad.pds";
		}
		else
		{
			productTypeName = satellite + "." + appid + ".pds";
		}
		// All productTypeNames created by PdsMover begin with 'drl.'
		productTypeName = "drl." + productTypeName;

		try
		{
			productType = dsm.getProductType(productTypeName);
		}
		catch (Exception e2)
		{
			fault = e2.getMessage();
			return;
		}
	}

	/**
	 * Get an error message if there was a problem creating this PDS.
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

	final int getPacketCount()
	{
		return packetCount;
	}

	final int getGapCount()
	{
		return gapCount;
	}

	final int getMissingPacketCount()
	{
		return missingPacketCount;
	}

	public final File getDataFile()
	{
		return data;
	}

        public String getFileNames()
        {
	    return constructionRecord.getName() + " " + data.getName();
        }

	public final File getRecordFile()
	{
		return constructionRecord;
	}

	public void delete() throws MoverException
	{
		if (constructionRecord.exists())
		{
			constructionRecord.delete();
		}
		if (data.exists())
		{
			data.delete();
		}
	}

	public void move(File directory)
	{
		if (constructionRecord.exists())
		{
			File newcrec = new File(directory,constructionRecord.getName());
			constructionRecord.renameTo(newcrec);
		}
		if (data.exists())
		{
			File newdata = new File(directory,data.getName());
			data.renameTo(newdata);
		}
	}

	public Product createProduct(File targetDirectory) throws MoverException
	{
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
		File fcrec = new File(targetDirectory,constructionRecord.getName());
		File fdata = new File(targetDirectory,data.getName());
		try {
			p.addResource("CREC",fcrec.getPath(),"Construction Record");

			p.addResource("DATA",fdata.getPath());
			p.setSubproduct("ProductionDataSets");
			p.addAttribute("packetCount",Integer.toString(packetCount));
			p.addAttribute("gapCount",Integer.toString(gapCount));
			p.addAttribute("missingPacketCount",Integer.toString(missingPacketCount));

		} catch (Exception e1) {
			throw new MoverException(e1);
		}
		return p;
	}

	private void readConstructionRecord(File fcrec) throws java.io.IOException
	{
		FileInputStream fis = new FileInputStream(fcrec);
		BufferedInputStream bis = new BufferedInputStream(fis);
		DataInputStream di = null;

		try {
			di = new DataInputStream(new BufferedInputStream(new FileInputStream(fcrec)));

			GregorianCalendar c0 = new GregorianCalendar(1958,Calendar.JANUARY,1);
			c0.setTimeZone(TimeZone.getTimeZone("GMT+0"));

			di.skipBytes(50);
			int scspairs = (int)di.readShort();
			byte[] scs = new byte[16];
			for (int n = 0; n < scspairs; n++)
			{
				di.read(scs);
			}
			di.skipBytes(12);

			short days0 = di.readShort();
			long msday0 = (long)di.readInt();
			@SuppressWarnings("unused") short micro0 = di.readShort();
			long x0 = days0 * 86400000L + msday0;
			long t0 = c0.getTimeInMillis() + x0;
			startTime = new Date(t0);

			short days1 = di.readShort();
			long msday1 = (long)di.readInt();
			@SuppressWarnings("unused") short micro1 = di.readShort();
			long x1 = days1 * 86400000L + msday1;
			long t1 = c0.getTimeInMillis() + x1;
			stopTime = new Date(t1);

			/**
	       long esh0,esh1 16
	       int rscorrpkts 4
	       int packets    4
	       long pktbytes  8
	       int gaps       4
	       long comple    8
	       int skip       4
	       int appids     4
	       int apidspid   4
	       long offset    8
	       int vcids      4
	       int vcidspid   4
	       int pgaps      4

	       for each pgap
	       int gseq       4
	       long goffset   8
	       int missing    4
	       long t0        8
	       long t1        8
	       long pre       8
	       long post      8
			 */
			di.skipBytes(20);
			packetCount = di.readInt();
			di.skipBytes(8);
			gapCount = di.readInt();
			di.skipBytes(36);
			int pgaps = di.readInt();
			missingPacketCount = 0;
			for (int n = 0; n < pgaps; n++)
			{
				di.skipBytes(12);
				int hole = di.readInt();
				missingPacketCount += hole;
				di.skipBytes(32);
			}
		}
		finally {
			if(di != null) di.close();
		}
	}

	public String toString()
	{
		return constructionRecord.getName() + "("+
		Utility.format(startTime) + "," +
		Utility.format(stopTime) + ")";
	}

	/**
	 * Two PDS products are equal if they are the same type, have the same start time, and both
	 * reference the same pass. Object may also be a Product.
	 */
	public boolean equals(Object o)
	{
		boolean match = false;
		if (o != null)
		{
			if (o instanceof PDS)
			{
				PDS p = (PDS)o;
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
