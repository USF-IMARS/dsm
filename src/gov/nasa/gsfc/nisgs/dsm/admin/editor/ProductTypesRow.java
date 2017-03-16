/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin.editor;

import java.util.Vector;

// this is less than ideal, easy to confuse an uninitialized row with
// one, especially if you want just an empty row... FIX 
// KR

public class ProductTypesRow extends Vector<String> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ProductTypesRow() {
		super();
		
	}
	public ProductTypesRow(String name, String spacecraft, String sensor, 
			              String description, String level, String directory)
	{
		super();
	   	this.add(name);
	   	this.add(spacecraft);
    	this.add(sensor);
    	this.add(description);
    	this.add(level);
    	this.add(directory);
	}
}
