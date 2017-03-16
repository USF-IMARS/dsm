/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;

/**
 * Little class that packages a marker reference (markerId, gopherColony)
 * for convenient storage in containers.
 */

public class MarkerRef
{
    public int markerId;
    public String gopherColony;

    public MarkerRef (int markerId, String gopherColony) {
	this.markerId = markerId;
	this.gopherColony = gopherColony;
    }

    /**
     * These overrides on the hashCode/equals methods
     * are literally the only reason for this class to exist.
     */
    public int hashCode() {
	return markerId + gopherColony.hashCode();
    }

    /**
     * These overrides on the hashCode/equals methods
     * are literally the only reason for this class to exist.
     */
    public boolean equals (Object obj) {
	if (obj instanceof MarkerRef) {
	    MarkerRef mr = (MarkerRef) obj;
	    return this.markerId == mr.markerId
		&& this.gopherColony.equals(mr.gopherColony);
	}
	else
	    return false;
    }

    public String toString() {
	return "MarkerRef{"+markerId+", "+gopherColony+"}";
    }
}
