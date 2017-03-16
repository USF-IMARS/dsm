/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

/**
 * Domains as specified by the JPSS specification with the sole exception of <code>drl</code> which is a local edition.
 * 
 *
 */
public enum FixedDomainDescription {
	ops("Operational Domain"),
	pop("Parallel Operational Domain"),
	iAndt("I & T Domain"),
	adr("Anomaly Duplication and Resolution"),
	dev("Development"),
	tia("Technology Insertion Activities"),
	ada("Algorithm Development Activities"),
	drl("Direct Readout Laboratory"),
	SCI("I put this in for LPEAT test of an old RDR");
	
	private String description;

	private FixedDomainDescription(String description) {
		this.description = description;
	}
	
	public String getDescription() { return description; }
	
	
	/**
	 * Return the string representation of the domains.  The "iAndt" item
	 * is converted to "int" which is the official designation.  However since <code>int</code>
	 * is a keyword in Java, the name had to be changed in the enumeration.
	 */
	@Override
	public String toString() {

		if (this == FixedDomainDescription.iAndt) {
			return "int"; // all this for this pesky special case
		} else {
			return this.name();
		}
	}
}
