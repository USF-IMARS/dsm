/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;



/**
 * Copies file between locations managed by the DSM.
 */

public class CopyMover extends FileMover
{

    String fromSite;
    String toSite;

    CopyMover(String fromSite, String toSite, DsmProperties dsmp)
    {
	super(dsmp);
	this.fromSite = fromSite;
	this.toSite = toSite;
    }

    public void quit() throws Exception
    {
    }

    public void moveFile(File from, File to) throws Exception
    {
	// Smack all of the paths to be absolute
	from = makeAbsolute(from, fromSite);
	to = makeAbsolute(to, toSite);

	// We need to spit here if either of these files are not
	// absolute paths (we tried to make them absolute, we really tried...)
	if(!from.isAbsolute())
	    throw new Exception("Relative from path in CopyMover: " + from);
	if(!to.isAbsolute())
	    throw new Exception("Relative to path in CopyMover: " + to);

	// We've defended ourselves as well as we can, drop into the
	// real file mover
	moveFiles(from, to);
    }

    private void moveFiles(File from, File to) throws Exception
    {
	if(from.isDirectory()) {
	    to.mkdir();
	    File subfiles[] = from.listFiles();
	    int i;
	    for(i=0; i< subfiles.length; i++) {
		moveFiles(subfiles[i], new File(to, subfiles[i].getName()));
	    }
	}
	else {
	    // Copying code found on-line
	    FileChannel sourceChannel = new FileInputStream(from).getChannel();
	    FileChannel destinationChannel = new FileOutputStream(to).getChannel();

	    // It would be really cool if the line below worked reliably:
	    // sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
	    // However, it's known to fail for files 2GB or greater,
	    // on all major JVMs, requiring the following silly workaround
	    // (taken from
	    // http://forum.java.sun.com/thread.jspa?threadID=439695&messageID=2917510):

	    int maxCount = (64 * 1024 * 1024) - (32 * 1024);
	    long size = sourceChannel.size();
	    long position = 0;
	    while (position < size) {
		position += sourceChannel.transferTo(position, maxCount, destinationChannel);
	    }

	    sourceChannel.close();
	    destinationChannel.close();
	}
    }

    public void mkdirs(File todirpath)
	throws Exception
    {
	todirpath = makeAbsolute(todirpath, toSite);
	// If it doesn't already exist, try to create it
	if(!(todirpath.exists() && todirpath.isDirectory()))
	    if(!todirpath.mkdirs()) {
		throw new Exception("Failed to create directory "
				    + todirpath.getPath());
	    }
    }

    public boolean exists(File to)
	throws Exception
    {
	to = makeAbsolute(to, toSite);
	return to.exists();
    }

    public boolean delete(File to)
	throws Exception
    {
	to = makeAbsolute(to, toSite);
	return to.delete();
    }
}
