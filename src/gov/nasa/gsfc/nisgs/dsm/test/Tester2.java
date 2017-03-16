/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import gov.nasa.gsfc.nisgs.dsm.*;
import java.util.*;

public class Tester2
{
    public static void main(String[] args)
    {
        try
        {
            DSM dsm = new DSM("seabiscuit","greenrat","myname","localhost");

            Calendar calendar = Calendar.getInstance();
            calendar.set(2004,11,25,0,0,0);
            Date start = calendar.getTime();

            Pass pass = dsm.getPass("AQUA",start);
            System.out.println(pass);

            Product p1 = dsm.getProduct("2");
            System.out.println(p1);

            Product p2 = dsm.getProduct("aqua.gbad.pds",pass);
            System.out.println(p2);

            Product p3 = dsm.getProduct("aqua.modis.pds",pass,start);
            System.out.println(p3);

            Product p4 = dsm.reserveProduct("aqua.modis.pds", "");
            System.out.println("Reserve product "+p4);
            if (p4 != null)
            {
                dsm.releaseProduct(p4);
                System.out.println("Release product "+p4);
            }

            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
