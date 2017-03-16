/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.GregorianCalendar;
import java.sql.ResultSet;
import java.io.File;

/**
 * TestDFR looks at all files the database thinks are on the local node
 * and sees if they are really here
 * @version 1.0 and we know what that means...
 */
public class TestDFR
{
    private DsmProperties config;

    public static void main(String[] args) throws Exception
    {
        TestDFR testDFR = new TestDFR();

	try
            {
                testDFR.run(args[0]);
            }
	catch (Exception re)
            {
		System.err.println("TestDFR error:");
		re.printStackTrace();
            }
    }

    TestDFR() throws Exception
    {
	config = new DsmProperties();

	// Crap out now if we don't have a sane data directory
	Utility.isWritableDirectory(config.getLocalDataDirectory());

        System.out.println("TestDFR ready");
    }

    void run(String filename) throws Exception
    {
        DSMAdministrator dsm = new DSMAdministrator("TestDFR","TestDFR");
	System.err.println("Trying " + filename);
        try
	    {
		System.err.println("Result is " + dsm.deleteFileRecord(filename));
	    }
        finally
	    {
		dsm.dispose();
	    }
    }
}
