/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.DSM;
import java.text.SimpleDateFormat;

/**
 * This tools stores a timed ancillary file into the dsm database.
 * A timed ancillary is a function of a keyword and time only (no spacecraft).
 * The two best known files of this time are LEAPSEC and UTCPOLE files.
 * The file will not be immediately available. It takes a few minutes for the DSM
 * to publish it to the IS computer.
 */
public class PutTimedAncillary
{
    public static void main(String[] args)
    {
        if (args.length != 3)
        {
            explain();
            System.exit(-1);
        }
        String key = args[0];
        String path = args[2];

        java.util.Date time = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try
        {
            time = sdf.parse(args[1]);
        }
        catch (java.text.ParseException pe)
        {
            System.out.println("Error parsing time.");
            System.out.println("The second argument must be yyyyMMddHHmmss");
            System.exit(-2);
        }

        try
        {
            DSM dsm = new DSM("Ancillary","PutTimedAncillary");
            dsm.putTimedAncillary(key,time,path);
            dsm.dispose();
            System.out.println("Ok. Stored.");
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }

    private static void explain()
    {
        System.out.println("This program stores a timed ancillary file in the DSM database.");
        System.out.println("A timed ancillary file is a function only of time and keyword and is independent of spacecraft.");
        System.out.println("It needs three arguments: keyword  time  filepath");
        System.out.println("keyword - LEAPSEC and UTCPOLE are known. A keyword must be the same for related files to be useful.");
        System.out.println("time - the time of the file in the format yyyyMMddHHmmss");
        System.out.println("path - the directory and name of the file. The IS has special naming requirements.");
        System.out.println("See the accompanying README file for details.");
        System.out.println("It may take several minutes for this file to be disseminated.");
    }
}
