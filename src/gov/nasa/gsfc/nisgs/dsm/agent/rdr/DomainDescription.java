/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

/**
 * Creates a domain description from one of three types of domains: Fixed, CalVal or TemporaryAndTest.
 * This package only used the {@link FixedDomainDescription}.
 * 
 *
 */
public class DomainDescription {
	private String domain;
	private String description;
	
	/**
	 * Construct a DomainDescription from a FixedDomainDescription.
	 * @param domain the FixedDomainDescription
	 */
	public DomainDescription(FixedDomainDescription domain) {
		this.domain = domain.toString();
		this.description = domain.getDescription();
	}
	
	/**
	 * Construct a DomainDescription from a CalValDomain.
	 * @param calValDomain the CalValDomain
	 */
	public DomainDescription(CalValDomain calValDomain) {
		this.domain = calValDomain.toString();
		this.description = calValDomain.getDescription();
	}
	
	/**
	 * Construct a DomainDescription from a TemporaryAndTestDomain.
	 * @param tempTestDomain the TemporaryAndTestDomain
	 */
	public DomainDescription(TemporaryAndTestDomain tempTestDomain) {
		this.domain = tempTestDomain.toString();
		this.description = tempTestDomain.getDescription();
	}
	
	/**
	 * Return the string representation of the domain
	 * @return the String domain name
	 */
	public String getDomain() {
		return domain;
	}
	
	/**
	 * Return the description of the domain as a String
	 * @return the String description
	 */
	public String getDescription() {
		return description;
	}
	
	
	/**
	 * Return the domain name as a String
	 * @return the String domain name
	 */
	@Override
	public String toString() {
		return domain;
	}
}
