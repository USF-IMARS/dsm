/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import gov.nasa.gsfc.nisgs.dsm.*;

public class Racer2
{
    @SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception
    {
            DSM dsm = new DSM("NISDS1","gopherA","ga","localhost");

             Product p = dsm.reserveProduct("aqua.productx", "");

              System.out.println(p);

              dsm.dispose();
    }
}
