/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;



/**
 * The Origin is defined according to the JPSS/NPOESS specifications.  
 * 
 *
 */
public enum Origin {
	navo("NAVO IDP"),
	afwa("AFWA IDP"),
	fnmc("FNMOC IDP"),
	dod("NAVO, AFWA, and FNMOC IDPs"),  // dod-
	noaa("NOAA IDP"),
	devl("Development Factory (includes the ISF)"),
	ftn0("Field Terminal Number -- default", 0),  // define more as needed...
	nfts("The old field terminal name which now breaks this"),

	all("All Field Terminals"), // all-
	c3s("C3S"), // c3s-
	larc("NASA Langley Research Center"),
	asf("Algorithm Support Function"), // asf-
	unkn("Unknown");  // Catch all for technically invalid names that we want to process anyway


	private String description;
	private int fieldTerminalNumber;

	private Origin(String description) {
		this.description = description;
		this.fieldTerminalNumber = 0;  // only for the ftn items
	}
	
	private Origin(String description, int fieldTerminalNumber) {
		this.description = description;
		this.fieldTerminalNumber = fieldTerminalNumber;  // only for the ftn items
	}
	
	/**
	 * Return the field terminal number.  Defaults to zero for all items except defined
	 * ftn0...n above.
	 * @return an integer containing the field terminal number
	 */
	public int getFieldTerminalNumber() {
		return fieldTerminalNumber;
	}
	/**
	 * Return the description
	 * @return a String containing the description
	 */
	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		String ret = this.name();
		
		if (this == Origin.dod) {
			ret = "dod-";
		} else if (this.name().startsWith("ftn")) {
			ret = String.format("%04x", this.getFieldTerminalNumber());
		} else if (this == Origin.all) {
			ret = "all-";
		} else if (this == Origin.c3s) {
			ret = "c3s-";
		} else if (this == Origin.asf) {
			ret = "asf-";
		}
		
		return ret;
	}
}
