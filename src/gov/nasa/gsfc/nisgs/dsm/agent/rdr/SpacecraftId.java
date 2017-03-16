/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

/**
 * The spacecraft identifier is defined by the JPSS/NPOESS specification
 * 
 *
 */
public enum SpacecraftId {
	npp("NPP 2230 Orbit"),
	n01("NPOESS Launch 1"),
	n02("NPOESS Launch 2"),
	n03("NPOESS Launch 3");
	
	private String description;

	private SpacecraftId(String description) {
		this.description = description;
	}

	/**
	 * Return the description of the identifier
	 * @return the description in a String
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Return the enum label as an all upper case
	 * @return the label as an upper case String
	 */
	@Override
	public String toString() {
		return this.name().toUpperCase();
	}
	
	/**
	 * Return the label as a lower case String
	 * @return the label as a lower case String
	 */
	public String toStringLower() {
		return this.name();
	}
}
