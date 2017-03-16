/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.admin;
import gov.nasa.gsfc.nisgs.dsm.DSM;
import java.text.SimpleDateFormat;

/**
 * This tools stores a satellite-timed ancillary file into the dsm database.
 * A satellite-timed ancillary is a function of a keyword, time, and spacecraft.
 * The two best known file of this time is a TLE file.
 * The file will not be immediately available. It takes a few minutes for the DSM
 * to publish it to the IS computer.
 */
public class PutSatTimedAncillary
{
    public static void main(String[] args)
    {
        if (args.length != 4)
        {
            explain();
            System.exit(-1);
        }
        String key = args[0];
        String spacecraft = args[1].toUpperCase();
        String path = args[3];

        java.util.Date time = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try
        {
            time = sdf.parse(args[2]);
        }
        catch (java.text.ParseException pe)
        {
            System.out.println("Error parsing time.");
            System.out.println("The third argument must be yyyyMMddHHmmss");
            System.exit(-2);
        }

        try
        {
            DSM dsm = new DSM("Ancillary","PutSatTimedAncillary");
            dsm.putSatTimedAncillary(key,time,spacecraft,path);
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
        System.out.println("This program stores a satellite-timed ancillary file in the DSM database.");
        System.out.println("A satellite-timed ancillary file is a function of time, spacecraft, and keyword.");
        System.out.println("It needs four arguments: keyword  spacecraft  time  filepath");
        System.out.println("keyword - TLE is known. A keyword must be the same for related files to be useful.");
        System.out.println("spacecraft - A spacecraft name like AQUA or TERRA. The program converts it to uppercase.");
        System.out.println("time - the time of the file in the format yyyyMMddHHmmss");
        System.out.println("path - the directory and name of the file. The IS has special naming requirements.");
        System.out.println("See the accompanying README file for details.");
        System.out.println("It may take several minutes for this file to be disseminated.");
    }
}
