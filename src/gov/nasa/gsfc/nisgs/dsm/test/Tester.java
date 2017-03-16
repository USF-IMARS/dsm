/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import java.util.*;
import gov.nasa.gsfc.nisgs.dsm.*;

public class Tester
{
    public static void main(String[] args)
    {
        String host = "localhost";
        if (args.length > 0)
        {
            host = args[0];
        }

        try
        {
            DSM dsm = new DSM("seabiscuit","gopher0","g0",host);

            Calendar calendar = Calendar.getInstance();
            calendar.set(2004,11,25,0,0,0);
            Date start = calendar.getTime();
            calendar.add(Calendar.MINUTE,10);
            Date stop = calendar.getTime();

            dsm.putStaticAncillary("crappola","/home/nisgs/data/crappola.dat");
            dsm.putStaticAncillary("wingnut","/home/nisgs/bear/wingnut.dat","a description");
            dsm.putUTCPOLE(start,"/home/nisgs/bear/utcpole.dat");
            dsm.putLEAPSEC(start,"/home/nisgs/data/leapsec.dat");
            dsm.putTLE(start,"Aqua","/home/nisgs/data/tle-aqua1.dat");
            dsm.putTLE(start,"Terra","/home/nisgs/tle/tle-terra1.dat");
            dsm.putTLE(stop,"Terra","/home/nisgs/tle/tle-terra2.dat");

            String x = dsm.getStaticAncillary("crappola");
            System.out.println("crappola="+x);

            x = dsm.getStaticAncillary("wingnut");
            System.out.println("wingnut="+x);

            x = dsm.getUTCPOLE(stop);
            System.out.println("utcpole="+x);

            x = dsm.getLEAPSEC(stop);
            System.out.println("leapsec="+x);

            x = dsm.getTLE(stop,"terra");
            System.out.println("terra2 tle="+x);

            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
