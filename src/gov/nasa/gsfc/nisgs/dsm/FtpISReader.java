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
 * Fetches files from the IS via FTP.
 */

public class FtpISReader extends FtpMover
{
    FtpISReader(DsmProperties dsmprops) throws Exception
    {
	super(dsmprops);
	ftp.login(dsmp.getProperty("IS_ftpReaderUser"),
		  dsmp.getProperty("IS_ftpReaderPassword"));
	ftp.setType(FTPTransferType.BINARY);
	// ftp.setConnectMode(FTPConnectMode.PASV);
    }

    public void moveFile(File from, File to) throws Exception
    {
	// Not much to do here - just force the paths to be absolute
	// The from path has to be syntactically absolute to make the FTP
	// package happy, for some reason...
	if(!from.isAbsolute())
	    from = new File("/", from.getPath());
	moveFiles(from, makeAbsolute(to, dsmp.getSite()));
    }

    private void moveFiles(File from, File to) throws Exception
    {
	boolean fromDirectory = false;
	String fromPath = from.getPath();

	try {
	    ftp.chdir(fromPath);
	    fromDirectory = true;
	    ftp.chdir("/");
	}
	catch (Exception e) {};

	if(fromDirectory) {
	    to.mkdir();
	    String subfiles[] = ftp.dir(fromPath);
	    int i;
	    // NOTE that ftp.dir(path) apparently produces a list of
	    // strings containing the
	    // relative/full/path/followed/by/filename, so we want to
	    // peel the file name off at the top of the loop
	    for(i=0; i<subfiles.length; i++) {
		File sf = new File (subfiles[i]);
		String filename = sf.getName();
		moveFiles(new File(from, filename),
			  new File(to, filename));
	    }
	}
	else {
	    // In a bold attempt to get a useful message out of this mess
	    String topath = to.getPath();
	    try {
		ftp.get(topath, fromPath);
	    }
	    catch (FTPException ftpe) {
		throw new Exception("While FTP reading from "
				    + ftp.getRemoteHost() + ":"
				    + fromPath
				    + " to "
				    + topath
				    + ": "
				    + ftpe
				    + " Code:" + ftpe.getReplyCode(),
				    ftpe);
	    }
	}
    }

    public void mkdirs(File todirpath)
	throws Exception
    {
	throw new Exception("Read-only access: can't create directory "
				+ todirpath.getPath());
    }

    public boolean delete(File to)
	throws Exception
    {
	throw new Exception("Read-only access: can't delete "
				+ to.getPath());
    }
}
