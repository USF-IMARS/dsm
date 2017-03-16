/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

/**
 * This class represents a compound TLE file, which contains TLEs for more than one
 * spacecraft.
 */
class TLE
{
    private HashMap<String,MyTLE> map = new HashMap<String,MyTLE>();

    TLE(File tleFile) throws Exception
    {
        BufferedReader in = new BufferedReader(new FileReader(tleFile));
        while (true)
        {
            String spacecraft = in.readLine();
            if (spacecraft == null) break;
            spacecraft= spacecraft.trim();
            MyTLE tle = new MyTLE();
            tle.line1 = in.readLine();
            tle.line2 = in.readLine();
            map.put(spacecraft,tle);
        }
        in.close();
    }

    String[] getElements(String spacecraft)
    {
        String[] element = null;
        MyTLE tle = map.get(spacecraft);
        if (tle != null)
        {
            element = new String[3];
            element[0] = spacecraft;
            element[1] = tle.line1;
            element[2] = tle.line2;
        }
        return element;
    }

    private class MyTLE
    {
        String line1;
        String line2;
    }
}
