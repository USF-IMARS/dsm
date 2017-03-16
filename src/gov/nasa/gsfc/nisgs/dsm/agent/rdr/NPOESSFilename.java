/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;


import gov.nasa.gsfc.nisgs.dsm.agent.MoverException;
import gov.nasa.gsfc.nisgs.dsm.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.*;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

/**
 * Creates legal NPOESS (JPSS) filenames.  The format is to mission specification. 
 * This is taken from RT-STPS, modified slightly to provide just the functionality needed by RDRMover.
 *
 */
public class NPOESSFilename {
	private static FilenameTimefieldFormat eTime = new FilenameTimefieldFormat(StartStopFieldId.e);
	private static FilenameTimefieldFormat tTime = new FilenameTimefieldFormat(StartStopFieldId.t);
	private static FileCreationDateFormat dfCreation = new FileCreationDateFormat();
	
	private List<ProductIdentifiers> productIds = new LinkedList<ProductIdentifiers>();
	private Date startDateTime;
	private Date stopTime;
	private Date creationDateAndTime;
	private String startDateStr;
	private String startTimeStr;
	private String stopTimeStr;
	private String creationDateTimeStr;
	private SpacecraftId spacecraftId;
	private int orbit;
	private String originString;
	private String orbitStr;
	private String filename;
	private String path;
	private String domainDescriptionString;
	private DsmProperties config;
	
	private final static String delimiter = "_";
	private final static String productNameDelimiter = "\\-";
	private final static DomainDescription drlDomain = new DomainDescription(FixedDomainDescription.drl);
	private final static String extension = ".h5";
	private final static char creationDateCode = 'c';
	private final static char orbitCode = 'b';
	private final static char startDateCode = 'd';
	
	
	private final static String usageStr = " format error -- should be 9 fields seperated by underscore: " +
										"ProductsId(s), SpaceCraftId, StartDate, StartTime, StopTime, Orbit, CreationDate, Domain" +
										" -- then followed by an " +
										extension + 
										" extension, not -->> ";
	
	


	/**
	 * De-construct a string into it constituent parts that is supposed to have a valid NPOESS filename.
	 * @param filename the string containing the NPOESS filename
	 * @throws ParseException throws except for certain failures in parsing the name
	 * @throws MoverException 
	 */
	public NPOESSFilename(String filename, String path) throws MoverException {
		
		try{
			config = new DsmProperties();
		} catch (Exception e1) {
			throw new MoverException(e1);
		}
		
		this.filename = filename;
		this.path = path;
	
		// first split across the "." to separate the extension from the rest of the filename
		String[] fields = filename.split("\\.");
		
		if (fields.length != 2) {
			throw new MoverException("Filename" + usageStr + filename + " -- failed to split on '.', fields found: " + fields.length);
		}
		
		// then split the file name portion into its pieces...
		String[] subfields = fields[0].split(delimiter);
		
		if (subfields.length != 9) {
			throw new MoverException("Filename" + usageStr + filename + " -- only contains " + subfields.length + " subfields");
		}

		// and then parse each one, or at least try to do so.
		parseSpacecraftId(subfields[1]); /// MUST BE FIRST
		parseProductIds(subfields[0]);
		
		parseStartDateTime(subfields[2], subfields[3], path);
		parseStopTime(subfields[4], path);
		parseOrbitNumber(subfields[5]);
		parseCreationDate(subfields[6]);
		parseOrigin(subfields[7]);
		parseDomainDescription(subfields[8]);
		parseExtension(fields[1]);
	}
	

	/**
	 * Once the filename is constructed it can be retrieved as a string. Or it will return the name
	 * passed into to the constructor which de-constructs filename strings.  Calling this method
	 * recomputes the name if that's appropriate.
	 * @return the string containing the filename.
	 */
	@Override
	public String toString() {
		//buildFilename();
		return filename;
	}

	public String productIndentifierstoString() {
		return productIdsToString();
	}
	
	/**
	 * Get the product identifiers associated with this filename
	 * @return a List of {@link ProductIdentifiers}
	 */
	public List<ProductIdentifiers> getProductIdentifiers() {
		return productIds;
	}
	
	/**
	 * Return the start date and time
	 * @return the start date and time as a Date
	 */
	public Date getStartDateTime() {
		return startDateTime;
	}
	
	/**
	 * Return the stop date and time
	 * @return the stop date and time as a Date
	 */
	public Date getStopTime() {
		return stopTime;
	}
	
	/**
	 * Return the clock creation date and time
	 * @return the creation date and time as a Date
	 */
	public Date getCreationDateAndTime() {
		return creationDateAndTime;
	}
	
	/**
	 * Return the spacecraft identifier
	 * @return the {@link SpacecraftId}
	 */
	public SpacecraftId getSpacecraftId() {
		return spacecraftId;
	}
	
	/**
	 * Return the orbit of the pass this file is associated with
	 * @return the orbit as an <code>int</code>
	 */
	public int getOrbit() {
		return orbit;
	}
	
	/**
	 * Return the origin of the packets associated with this file
	 * @return the {@link Origin}
	 */
	public String getOriginString() {
		return originString;
	}
	
	/** 
	 * Return the domain description field
	 * @return a String
	 */
	public String getDomainDescriptionString() {
		return this.domainDescriptionString;
	}
	/**
	 * Parse the extension (e.g. ".h5")
	 * @param extensionStr
	 * @throws ParseException
	 */
	private void parseExtension(String extensionStr) throws MoverException {
		if (!extensionStr.equals(extension.substring(1))) {
			throw new MoverException("Extension" + usageStr + filename);
		}
	}

	/**
	 * Parse the domain (e.g. "DRL")
	 * @param domainStr
	 * @throws ParseException
	 */
	private void parseDomainDescription(String domainStr) throws MoverException {
		//if (!domainStr.equals(drlDomain.toString())) {
		//	throw new MoverException("Domain must be [" + drlDomain + "] not [ " + domainStr + "] -- " + usageStr + filename);
		//}
		this.domainDescriptionString = domainStr;
	}

	/**
	 * Parse the origin (e.g. "nfts")
	 * @param originStr
	 * @throws ParseException
	 */
	private void parseOrigin(String originStr) throws MoverException {
	    this.originString = originStr;
	}

	/**
	 * Parse the creation date 
	 * @param creationDateStr
	 * @throws ParseException
	 * @throws MoverException 
	 */
	private void parseCreationDate(String creationDateStr) throws MoverException {
		if (creationDateStr.charAt(0) != creationDateCode) {
			throw new MoverException("Creation date [" + creationDateStr + "] -- must start with a \'" + creationDateCode + "\' -- " + usageStr + filename);			
		}
		this.creationDateAndTime = dfCreation.parse(creationDateStr);
	}

	/**
	 * Parse the orbit sub-field
	 * @param orbitStr
	 * @throws ParseException
	 */
	private void parseOrbitNumber(String orbitStr) throws MoverException {
		if (orbitStr.charAt(0) != orbitCode) {
			throw new MoverException("Orbit [" + orbitStr + "] -- must start with a \'" + orbitCode + "\' -- " + usageStr + filename);			
		}
		this.orbit = Integer.valueOf(orbitStr.substring(1));
	}

	/**
	 * Parse the stop time
	 * @param stopTimeStr
	 * @throws ParseException
	 */
	private void parseStopTime(String stopTimeStr, String path) throws MoverException {
		if (stopTimeStr.charAt(0) != StartStopFieldId.e.name().charAt(0)) {
			throw new MoverException("StopTime [" + stopTimeStr + "] -- must start with a \'" + StartStopFieldId.e + "\' -- " + usageStr + filename);			
		}
		
		//String granulemode = System.getenv("RDRMOVERMODE");
		/*String IngestMode=null;
		try{
			   DsmProperties config = new DsmProperties();
			   IngestMode=config.getIngest_Mode();
	           if(IngestMode==null)
	        	   IngestMode="NORMAL";
			} catch (Exception e1) {
				throw new MoverException(e1);
			}*/
		
		
		if(!config.isGranuleIngestMode())	
		{
			try 
			{
				int file = H5.H5Fopen(path,HDF5Constants.H5F_ACC_RDONLY,HDF5Constants.H5P_DEFAULT);
				int group_id = H5.H5Gopen(file,"Data_Products",HDF5Constants.H5P_DEFAULT);
				int sub_group_id = H5.H5Gopen(group_id,"SPACECRAFT-DIARY-RDR", HDF5Constants.H5P_DEFAULT);
				int dataset_id = H5.H5Dopen(sub_group_id, "SPACECRAFT-DIARY-RDR_Aggr", HDF5Constants.H5P_DEFAULT);
				int ending_attribute_id = H5.H5Aopen(dataset_id,"AggregateEndingTime", HDF5Constants.H5P_DEFAULT);
				String[] endingTimeString = new String[1];
				int ending_dataspace_id = H5.H5Aget_space(ending_attribute_id);
				int ending_dataset_type_id = H5.H5Aget_type(ending_attribute_id);
				int ending_read_id =
						H5.H5AreadVL(ending_attribute_id,ending_dataset_type_id,endingTimeString);
				String endTimeString = endingTimeString[0];
				endTimeString = endTimeString.replace(".","");
				endTimeString = endTimeString.replace("Z","");
				endTimeString = endTimeString.substring(0,7);
				stopTimeStr = "t" + endTimeString;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		try {
			this.stopTime = eTime.parse(stopTimeStr);
			if(!config.isGranuleIngestMode())	
			{
				Calendar startTimeCal = Calendar.getInstance();
				Calendar stopTimeCal = Calendar.getInstance();
				startTimeCal.setTime(this.startDateTime);
				stopTimeCal.setTime(this.stopTime);
				if(stopTimeCal.get(Calendar.HOUR_OF_DAY) < 
						startTimeCal.get(Calendar.HOUR_OF_DAY)) {
					if(stopTimeCal.get(Calendar.DAY_OF_MONTH) ==
							startTimeCal.get(Calendar.DAY_OF_MONTH)) {
						stopTimeCal.add(Calendar.DATE,1);
						this.stopTime = stopTimeCal.getTime();
					}
				}
			}

		} catch (ParseException e) {
			throw new MoverException(e);
		}
	}


	/**
	 * Parse the date and time
	 * @param startDateStr
	 * @param startTimeStr
	 * @throws ParseException
	 */
	private void parseStartDateTime(String startDateStr, String startTimeStr, String path) throws MoverException {
		if (startDateStr.charAt(0) != startDateCode) {
			throw new MoverException("StartDate [" + startDateStr + "] -- must start with a \'" + startDateCode + "\' -- " + usageStr + filename);			
		}
		
		// use ThreadLocal to ensure no race conditions occur
		ThreadLocal<SimpleDateFormat> createDateFormat = new ThreadLocal<SimpleDateFormat>()
		{
		
			@Override
			protected SimpleDateFormat initialValue()
			{
				return new SimpleDateFormat("yyyyMMdd");
			}

		};

		createDateFormat.get().setTimeZone(TimeZone.getTimeZone("UTC"));
		
		Date startDate = null;
		try {
			startDate = createDateFormat.get().parse(startDateStr.substring(1));
		} catch (ParseException e) {
			throw new MoverException(e);
		}

		if (startTimeStr.charAt(0) != StartStopFieldId.t.name().charAt(0)) {
			throw new MoverException("StartTime [" + startTimeStr + "] -- must start with a \'" + StartStopFieldId.t + "\' -- " + usageStr + filename);			
		}
		
		Date startTime = null;

		//String granulemode = System.getenv("RDRMOVERMODE");
		/*String IngestMode;
		try{
			DsmProperties config = new DsmProperties();
			IngestMode=getIngest_Mode();			
		} catch (Exception e1) {
			throw new MoverException(e1);
		}*/
		
		if(!config.isGranuleIngestMode())
		{
			try
			{	
				int file = H5.H5Fopen(path,HDF5Constants.H5F_ACC_RDONLY,HDF5Constants.H5P_DEFAULT);
				int group_id = H5.H5Gopen(file,"Data_Products",HDF5Constants.H5P_DEFAULT);
				int sub_group_id = H5.H5Gopen(group_id,"SPACECRAFT-DIARY-RDR", HDF5Constants.H5P_DEFAULT);
				int dataset_id = H5.H5Dopen(sub_group_id, "SPACECRAFT-DIARY-RDR_Aggr", HDF5Constants.H5P_DEFAULT);
				int beginning_attribute_id = H5.H5Aopen(dataset_id,"AggregateBeginningTime", HDF5Constants.H5P_DEFAULT);
				String[] beginningTimeString = new String[1];
				int beginning_dataspace_id = H5.H5Aget_space(beginning_attribute_id);
				int beginning_dataset_type_id = H5.H5Aget_type(beginning_attribute_id);
				int beginning_read_id =
						H5.H5AreadVL(beginning_attribute_id,beginning_dataset_type_id,beginningTimeString);
				startTimeStr = beginningTimeString[0];
				startTimeStr = startTimeStr.replace(".","");
				startTimeStr = startTimeStr.replace("Z","");
				startTimeStr = startTimeStr.substring(0,7);
				startTimeStr = "t" + startTimeStr;

				H5.H5close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		try {
			startTime = tTime.parse(startTimeStr);
		} catch (ParseException e) {
			throw new MoverException(e);
		}
		
		// throw the two together to make the start date+time
		// note: this seems like it never works right, we get time zone shifts or savings time issues...
		// but I am going to try it again anyway...
		
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		time.setTime(startTime);
		Calendar date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		date.setTime(startDate);
		
		date.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
		date.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
		date.set(Calendar.SECOND, time.get(Calendar.SECOND));
		date.set(Calendar.MILLISECOND, time.get(Calendar.MILLISECOND) * 100);
		
		this.startDateTime = date.getTime();

	}

	/**
	 * Parse the spacecraft identifier (e.g. "NPP")
	 * @param spacecraftIdStr
	 * @throws ParseException
	 */
	private void parseSpacecraftId(String spacecraftIdStr) throws MoverException {
		SpacecraftId spacecraftId = SpacecraftId.valueOf(spacecraftIdStr);
		if (spacecraftId == null) {
			throw new MoverException("Unknown SpaceCraftId [" + spacecraftIdStr + "]" + usageStr + filename);
		}
		this.spacecraftId = spacecraftId;
	}

	/**
	 * Parse the product identifier(s) (e.g. "RATMS")
	 * @param productIdsStr
	 * @throws ParseException
	 */
	private void parseProductIds(String productIdsStr) throws MoverException {
		String[] products = productIdsStr.split(productNameDelimiter);
		
		for (int i = 0; i < products.length; i++) {
			System.out.println("Products[" + i + "]" + products[i]);
			ProductIdentifiers productId = ProductIdentifiers.fromNameString(products[i], this.getSpacecraftId().toString());
			if (productId == null) {
				throw new MoverException("Unknown ProductId [" + productIdsStr + "]" + usageStr + filename);
			}
			this.productIds.add(productId);
		}
		
		// doesn't seem likely this will trip...
		if (this.productIds.size() <= 0) {
			throw new MoverException("No ProductIds found [" + productIdsStr + "]" + usageStr + filename);
		}
	}


	
	/**
	 * make the list of product IDs into a ID0-ID1 style string
	 * maybe there's a better way to do this...?
	 */
	private String productIdsToString() {
		int count = 0;  String productStr = "";
		
		Collections.sort(productIds, new ProductIdentifiersComparator()); // FIXME nice if this had no side effects but I guess it does not matter.
		
		for (ProductIdentifiers productID : productIds) {
			if (count > 0) {
				productStr += "-";
			}
			productStr += productID.toString();
			++count;
		}
		return productStr;
	}
	
	


}
