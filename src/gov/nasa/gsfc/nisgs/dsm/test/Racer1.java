/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import gov.nasa.gsfc.nisgs.dsm.*;
import java.util.*;

public class Racer1
{
    public static void main(String[] args)
    {
        try
        {
            DSM dsm = new DSM("NISDS1","seabiscuit","seabiscuit","localhost");

        Calendar calendar = Calendar.getInstance();
        calendar.set(2004,11,25,0,0,0);
        Date start = calendar.getTime();
        calendar.add(Calendar.MINUTE,10);
        Date stop = calendar.getTime();

        Pass pass = new Pass("gsfc-drolab","AQUA",start,stop);
        dsm.createPass(pass);

        Product product = new Product(start,stop,"gopher0.g0","aqua.productx",pass);
        product.addResource("DATA","/home/nisgs/P015400641m1.PDS","data");
        dsm.storeProduct(product);

        product = new Product(start,stop,"gopher0.g0","aqua.productx",pass);
        product.addResource("DATA","/home/nisgs/P015400641m2.PDS","data");
        dsm.storeProduct(product);

        product = new Product(start,stop,"gopher0.g0","aqua.productx",pass);
        product.addResource("DATA","/home/nisgs/P015400641m3.PDS","data");
        dsm.storeProduct(product);

        product = new Product(start,stop,"gopher0.g0","aqua.productx",pass);
        product.addResource("DATA","/home/nisgs/P015400641a1.PDS","data");
        dsm.storeProduct(product);

            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
