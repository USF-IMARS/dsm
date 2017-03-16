/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

/**
 * The Temporary and Test Domain is part of Domain description (@link DomainDescription}. This class
 * supports the proper creation of these fields.  It is currently unused in this package.
 * 
 *
 */
public class TemporaryAndTestDomain {
	private String name;
	private String description;
	
	/**
	 * Create a temporary and test domain with a test number and description
	 * @param test the test number, modulo 100
	 * @param description the description
	 */
	public TemporaryAndTestDomain(int test, String description) {
		this.name = "t" + (test%100);
		this.description = description;
	}
	
	/**
	 * Create a temporary and test domain using two fields and a description
	 * @param field1 the first field
	 * @param field2 the second field
	 * @param description the description 
	 */
	public TemporaryAndTestDomain(char field1, char field2, String description) {
		this.name = "t" + field1 + field2;
		this.description = description;
	}
	
	/**
	 * Create a temporary and test domain using two fields, where the first is a value modulo 10, and a description
	 * @param field1 a value, calculated modulo 10
	 * @param field2 the second field
	 * @param description the description
	 */
	public TemporaryAndTestDomain(int field1, char field2, String description) {
		this.name = "t" + (field1%10) + field2;
		this.description = description;
	}
	
	/**
	 * Create a temporary and test domain using two fields, where the second is a value modulo 10, and a description
	 * @param field1 the first field
	 * @param field2 a value, calculated modulo 10 
	 * @param description the description
	 */
	public TemporaryAndTestDomain(char field1, int field2, String description) {
		this.name = "t" + field1 + (field2%10);
		this.description = description;
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
		return name;
	}
}
