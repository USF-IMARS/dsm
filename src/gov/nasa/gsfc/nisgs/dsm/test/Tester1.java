/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import java.util.*;
import gov.nasa.gsfc.nisgs.dsm.*;

public class Tester1
{
    public static void main(String[] args) throws Exception
    {
        String host = "localhost";
        if (args.length > 0)
        {
            host = args[0];
        }

        DSM dsm = new DSM("seabiscuit","gopher1","user1",host);

        Calendar calendar = Calendar.getInstance();
        calendar.set(2004,11,25,0,0,0);
        Date start = calendar.getTime();
        calendar.add(Calendar.MINUTE,10);
        Date stop = calendar.getTime();

        Pass pass = new Pass("drolab","AQUA",start,stop);
        dsm.createPass(pass);

        Product product = new Product(start,stop,"gopher0.g0","aqua.modis.pds",pass);
        product.addResource("DATA","/home/nisgs/P015400641m1.PDS","data");
        product.addResource("CREC","/home/nisgs/P015400650m1.PDS","construction record");
        dsm.storeProduct(product);

        product = new Product(start,stop,"gopher0.g0","aqua.modis.pds",pass);
        product.addResource("DATA","/home/nisgs/P015400641m2.PDS","data");
        product.addResource("CREC","/home/nisgs/P015400650m2.PDS","construction record");
        dsm.storeProduct(product);

        product = new Product(start,stop,"gopher0.g0","aqua.modis.pds",pass);
        product.addResource("DATA","/home/nisgs/P015400641m3.PDS","data");
        product.addResource("CREC","/home/nisgs/P015400650m3.PDS","construction record");
        dsm.storeProduct(product);

        product = new Product(start,stop,"gopher0.g0","aqua.airs.pds",pass);
        product.addResource("DATA","/home/nisgs/P015400641a1.PDS","data");
        product.addResource("CREC","/home/nisgs/P015400650a1.PDS","construction record");
        dsm.storeProduct(product);

        dsm.dispose();
    }
}
