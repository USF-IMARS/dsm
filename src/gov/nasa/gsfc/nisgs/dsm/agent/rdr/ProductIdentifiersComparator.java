/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent.rdr;

import java.util.Comparator;
class ProductIdentifiersComparator implements Comparator<ProductIdentifiers> {

	@Override
	public int compare(ProductIdentifiers p0, ProductIdentifiers p1) {
		return p0.toString().compareTo(p1.toString());
	}
	
}