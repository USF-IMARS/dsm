/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

/**
 * The CalValDomain is used by the DomainDescription class, CalVals are specified
 * by the mission documentation and have a specific format.
 * 
 *
 */
public class CalValDomain {
	private String name;
	private String description;
	
	/**
	 * Create an instance of a CalValDomain using a test number and description
	 * @param test the test number
	 * @param description a description of the test
	 */
	public CalValDomain(int test, String description) {
		this.name = "cv" + (test%10);
		this.description = description;
	}
	
	/**
	 * Create an instance of a CalValDomain using a character test designation and description
	 * @param test the test character
	 * @param description the description of the test
	 */
	public CalValDomain(char test, String description) {
		this.name = "cv" + test;
		this.description = description;
	}
	
	/**
	 * Get the description of CalValDomain
	 * @return a String containing the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Return the constructed internal name
	 * @return a String consisting of the internal name
	 */
	@Override
	public String toString() {
		return name;
	}
}
