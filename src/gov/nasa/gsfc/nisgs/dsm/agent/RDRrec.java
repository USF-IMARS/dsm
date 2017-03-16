/*
Copyright (c) 1999-2012, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;

import gov.nasa.gsfc.nisgs.dsm.ProductType;
import gov.nasa.gsfc.nisgs.dsm.agent.rdr.NPOESSFilename;
//import gov.nasa.gsfc.nisgs.dsm.agent.rdr.Origin;
import gov.nasa.gsfc.nisgs.dsm.agent.rdr.ProductIdentifiers;
import gov.nasa.gsfc.nisgs.dsm.agent.rdr.SpacecraftId;

import java.util.Date;
import java.util.List;

// RDRrec is a record of some of the attributes of a particular RDR
public class RDRrec {


	private Date startTime;
	private Date stopTime;
	private String fileName;
	private String spacecraft;
	private ProductType productType;
	private List<ProductIdentifiers> productIndentifiers;  	// from NPOESS file name
	private SpacecraftId spacecraftId; 						// from NPOESS file name
	private int orbit; 										// from NPOESS file name
	//private DomainDescription domain; 						// from NPOESS file name
	private String domain;									// from NPOESS file name
	private String origin; 									// from NPOESS file name
	//int packetCount = 0;
	
	RDRrec(NPOESSFilename npoessFilename) throws MoverException {
		startTime = npoessFilename.getStartDateTime();
		stopTime = npoessFilename.getStopTime();
		fileName = npoessFilename.toString();
		spacecraftId = npoessFilename.getSpacecraftId();
		orbit = npoessFilename.getOrbit();
		origin = npoessFilename.getOriginString();
		
		crack(fileName);
		
		spacecraft = spacecraftId.toString();
	
		
		
		String sensors = npoessFilename.productIndentifierstoString();
		String description = "NPOESS science RDR from [" + domain.toString() + "] for sensors [" + sensors + "] on orbit[" + orbit + "]";
		
		
		productType = new ProductType(spacecraft + "_" + sensors, spacecraft, sensors, description, "1");
	}
	
	// FIXME this is just to get the domain string.  The original DLR NPOESSFilename class hard codes it to 'drl'...
	// and this is probably more general for outside files.  So... temp fix here. 
	private void crack(String filename) throws MoverException {
		// first split across the "." to separate the extension from the rest of the filename
		String[] fields = filename.split("\\.");
		
		if (fields.length != 2) {
			throw new MoverException("Filename" + filename);
		}
		
		// then split the file name portion into its pieces...
		String[] subfields = fields[0].split("_");
		
		if (subfields.length != 9) {
			throw new MoverException("Filename" + filename);
		}
		
		domain = subfields[8];
	}

	Date getStartTime() {
		return startTime;
	}

	Date getStopTime() {
		return stopTime;
	}

	String getFileName() {
		return fileName;
	}

	String getSpacecraft() {
		return spacecraft;
	}

	ProductType getProductType() {
		return productType;
	}

	List<ProductIdentifiers> getProductIndentifiers() {
		return productIndentifiers;
	}

	SpacecraftId getSpacecraftId() {
		return spacecraftId;
	}

	int getOrbit() {
		return orbit;
	}

	String getDomain() {
		return domain;
	}
}
