/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import java.util.*;
import gov.nasa.gsfc.nisgs.dsm.*;

public class Rogue
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
            DSM dsm = new DSM("santa","level0","rtstps0",host);

            Calendar calendar = Calendar.getInstance();
            calendar.set(2005,0,17,0,0,0);
            Date start = calendar.getTime();
            calendar.add(Calendar.SECOND,498);
            Date stop = calendar.getTime();

            Pass pass = new Pass("drolab","AQUA",start,stop);
            dsm.createPass(pass);

            Product product = new Product(start,stop,"rtstps0","aqua.modis.pds",pass);
            product.addResource("DATA","/home/nisgs/data/P1540064AAAAAAAAAAAAAA05017081230001.PDS","data");
            product.addResource("CREC","/home/nisgs/data/P1540064AAAAAAAAAAAAAA05017081230000.PDS","construction record");
            dsm.storeProduct(product);

            product = new Product(start,stop,"rtstps0","aqua.gbad.pds",pass);
            product.addResource("DATA","/home/nisgs/data/P1540957AAAAAAAAAAAAAA05017081230001.PDS","data");
            product.addResource("CREC","/home/nisgs/data/P1540957AAAAAAAAAAAAAA05017081230000.PDS","construction record");
            dsm.storeProduct(product);

            dsm.putLEAPSEC(start,"/home/nisgs/data/leapsec.dat");
            dsm.putUTCPOLE(start,"/home/nisgs/data/utlpole.dat");
            dsm.putTLE(start,"AQUA","/home/nisgs/data/AQUA_norad.tle");

            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
