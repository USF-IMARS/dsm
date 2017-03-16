/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package nasa.gsfc.nisgs.dsm;
import gov.nasa.gsfc.nisgs.dsm.DSM;

import java.util.*;

public class Tester3
{
    public static void main(String[] args)
    {
        try
        {
            DSM dsm = new DSM("computer","mygroup","myname","localhost");

            Calendar calendar = Calendar.getInstance();
            calendar.set(2004,1,25,0,0,0);
            Date date = calendar.getTime();

            for (int n = 0; n < 5; n++)
            {
                dsm.putUTCPOLE(date,"utcpolefile"+n);
                calendar.add(Calendar.MINUTE,30);
                date = calendar.getTime();
            }

            calendar.set(2004,1,25,0,23,0);
            String path = dsm.getUTCPOLE(calendar.getTime());
            System.out.println(path);

            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
