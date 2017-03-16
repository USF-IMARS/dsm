/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;

/**
 * An attribute is a name and value pair to be stored in a subproduct
 * table. The name must exist as a subproduct table data base field.
 */
public class Attribute implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
    private String value;

    Attribute(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    /**
     * Get the attribute name, i.e. the first part of "name=value."
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the attribute value, i.e. the second part of "name=value."
     */
    public String getValue()
    {
        return value;
    }

    public String toString()
    {
        return name + "='" + value + "'";
    }

    public boolean equals(Object o)
    {
        boolean match = false;
        if (o != null && o instanceof Attribute)
        {
            Attribute a = (Attribute)o;
            match = name.equals(a.name);
        }
        return match;
    }
}
