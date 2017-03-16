/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;

import com.enterprisedt.net.ftp.*;
import java.io.*;

public class FtpTest
{
    public static void main(String[] args) throws Exception
    {
        new FtpTest();
    }

    FtpTest() throws Exception
    {
        FTPClient isftp = new FTPClient();
        isftp.setRemoteHost("is.sci.gsfc.nasa.gov");
        isftp.connect();
        isftp.login("anonymous","stan@hilinski.net");
        isftp.setType(FTPTransferType.BINARY);

        String remoteFilePath = "ancillary/ephemeris/iirv";
        File localDirectory = new File("/debris/iirv");
        transferDirectoryContents(isftp,localDirectory,remoteFilePath);

        isftp.quit();
    }

    private void transferDirectoryContents(FTPClient isftp, File localDirectory,
        String remoteFilePath) throws Exception
    {
        FTPFile[] list = isftp.dirDetails(remoteFilePath);

        //For something like ancillary/spatial/dem, name will be "dem".
        if (localDirectory.exists())
        {
            if (!localDirectory.isDirectory())
            {
                throw new Exception(localDirectory.getPath() + " is not a directory.");
            }
        }
        else
        {
            localDirectory.mkdir();
        }

        for (int n = 0; n < list.length; n++)
        {
            String lname = list[n].getName();
            if (lname.equals(".") || lname.equals("..")) continue;
            String rpath = remoteFilePath + "/" + lname;
            File lfile = new File(localDirectory,lname);
            if (!list[n].isDir())
            {
                isftp.get(lfile.getPath(),rpath);
            }
            else
            {
                transferDirectoryContents(isftp,lfile,rpath);
            }
        }
    }
}
