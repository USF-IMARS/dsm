/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import java.io.File;
import com.enterprisedt.net.ftp.*;

/**
 * Abstract class that moves files between locations managed by the DSM
 * using FTP.
 * Implementing classes actually only move data to and from the IS.
 */

public abstract class FtpMover extends FileMover
{
    protected FTPClient ftp;

    FtpMover(DsmProperties dsmprops) throws Exception
    {
	super(dsmprops);
	ftp = new FTPClient();
	ftp.setTimeout(100 * 1000);  //100 seconds for now
	ftp.setRemoteHost(dsmp.getIS_Host());
	ftp.connect();
    }

    public void quit() throws Exception
    {
	ftp.quit();
    }

    public boolean exists(File to)
	throws Exception
    {
	File absrempath = to;
	// For some unknown reason, the chdir method requires an
        // absolute path (?!) which is silly for FTP purposes...
	if(!to.isAbsolute())
            absrempath = new File("/", to.getPath());
	ftp.chdir(absrempath.getPath());
	return ftp.exists((String)(to.getName()));
    }
}
