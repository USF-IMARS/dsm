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
 * Writes files to the IS using FTP.
 */

public class FtpISWriter extends FtpMover
{
    FtpISWriter(DsmProperties dsmprops) throws Exception
    {
	super(dsmprops);
	ftp.login(dsmp.getProperty("IS_ftpWriterUser"),
		  dsmp.getProperty("IS_ftpWriterPassword"));
	ftp.setType(FTPTransferType.BINARY);
	ftp.setConnectMode(FTPConnectMode.PASV);
    }

    public void moveFile(File from, File to) throws Exception
    {
	// For some unknown reason, the chdir method requires an
        // absolute path (?!) which is silly for FTP purposes...
        File absrempath = to;
        if(!to.isAbsolute())
            absrempath = new File("/", to.getPath());
	File absfrompath = makeAbsolute(from, dsmp.getSite());

	// We've defended ourself as well aa we can, call the
	// (potentially recursive) file tree mover
	moveFiles(absfrompath, absrempath);
    }

    private void moveFiles(File from, File to) throws Exception
    {
	if(from.isDirectory()) {
	    ftp.mkdir(to.getPath());
	    File subfiles[] = from.listFiles();
	    int i;
	    for(i=0; i< subfiles.length; i++) {
		moveFiles(subfiles[i], new File(to, subfiles[i].getName()));
	    }
	}
	else {
	    String topath = to.getParent();
	    String frompath = from.getPath();
	    try {
		ftp.chdir(topath);
		ftp.put(frompath, to.getName());
	    }
	    // In a bold attempt to get a useful message out of this mess
	    // if it fails...
	    catch (Exception e) {
		throw new Exception("While FTP writing from "
				    + frompath
				    + " to "
				    + topath
				    + ": "
				    + e,
				    e);
	    }
	}
    }

    public void mkdirs(File todirpath)
	throws Exception
    {
	// Canonical shortcut - try to chdir to the path, and only go
	// through the hassle of creating the path if that fails

	try {
	    ftp.chdir(todirpath.getPath());
	}
	catch (Exception e) {
	    // Oh foo - try walking down the path, creating the
	    // directories one at a time

	    try {
		mkdirsHelper(todirpath);
	    }
	    catch (Exception e2) {
		throw new Exception("Trouble creating directory "
				    + todirpath.getPath(),
				    e2);
	    }
	}
	finally {
	    // Let's at least try to get back to the FTP root...
	    try { ftp.chdir("/");} catch (Exception e){};
	}
    }

    /**
     * Semantics of ftp.delete are different than Java File.delete,
     * so we return false if the file did not exist.  We just try to delete
     * it, and rely on exceptions for other errors.
     */
    public boolean delete(File to)
	throws Exception
    {
	File absrempath = to;
	// For some unknown reason, the chdir method requires an
        // absolute path (?!) which is silly for FTP purposes...
	if(!to.isAbsolute())
            absrempath = new File("/", to.getPath());
	ftp.chdir(absrempath.getPath());

	String filename = to.getName();
	if (ftp.exists(filename)) {
	    ftp.delete(filename);
	    return true;
	}
	return false;
    }

    /**
     * You know what would have been useful?  A File API that would let
     * you get at the individual names in the path in some reasonable
     * sequential way, thus avoiding the somewhat convoluted recursion
     * below, which will likely completely obscure any errors that happen
     */
    void mkdirsHelper(File f)
	throws Exception
    {
	if(f != null) {
	    mkdirsHelper(f.getParentFile());
	    String fn = f.getName();
	    // We try to chdir first - if we win, it was there already
	    try {
		ftp.chdir(fn);
	    }
	    // If we failed, try to mkdir and then chdir - if this one
	    // fails, we want to crap out.
	    catch (Exception e) {
		ftp.mkdir(fn);
		ftp.chdir(fn);
	    }
	}
    }
}
