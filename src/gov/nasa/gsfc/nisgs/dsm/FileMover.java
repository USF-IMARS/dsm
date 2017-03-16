/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Abstract class that handles moving files from Site to Site (Site
 * has its NISGS meaning here of machine/data-directory).
 */

public abstract class FileMover
{
    // It looks like everyone needs dsmp...

    protected DsmProperties dsmp;

    public FileMover(DsmProperties dsmp)
    {
	this.dsmp = dsmp;
    }

    /**
     * Look up the IS site.
     */
    public String getIS_Site() {
	return dsmp.getIS_Site();
    }

    // These are static so they can be used by the static newMover
    // method below

    /**
     * Encodes current restrictions on host lookup.  Maybe one day
     * when we require Site/Host/DataDirectory registration with DSM...
     */
    private static String findHost(String site, DsmProperties dsmp)
	throws Exception
    {
	if(site.equals(dsmp.getSite()))
	    return dsmp.getHost();
	else if (site.equals(dsmp.getIS_Site()))
	    return dsmp.getIS_Host();
	else
	    throw new Exception("Can't find host for " + site);
    }

    /**
     * Encodes current restrictions on data directory.  Maybe one day
     * when we require Site/Host/DataDirectory registration with DSM...
     */
    private static String findSite(String site, DsmProperties dsmp)
	throws Exception
    {
	if(site.equals(dsmp.getSite()))
	    return dsmp.getSite();
	else if (site.equals(dsmp.getIS_Site()))
	    return dsmp.getIS_Site();
	else
	    throw new Exception("Can't find site for " + site);
    }

    /**
     * returns a File made absolute by assuming it is relative to the
     * site's data directory - we curently have no use for absolute
     * paths on other sites/hosts.
     */

    protected File makeAbsolute(File f, String site)
	throws Exception
    {
	return makeAbsoluteLocal(f);
    }

    /**
     * returns a File made absolute relative to the local site's data directory
     * This is public because it is occasionally useful to call it instead of
     * actually moving a file.  If FileMover could somehow know when just
     * renameTo() was the right thing...
     */

    public File makeAbsoluteLocal(File f)
	throws Exception
    {
	if(!f.isAbsolute())
	    f = new File(dsmp.getLocalDataDirectory(), f.getPath());
	return f;
    }

    public void testLocalAccess(File from)
	throws Exception
    {
	from = makeAbsoluteLocal(from);
	if(!from.exists())
	    throw new FileNotFoundException(from + " was not found");
	if(!from.canRead())
	    throw new Exception(from + " cannot be read");
    }


    /** Yes, I know the Right Pattern Thing to do here is to create a Factory
     *	class.  Go away, pattern junkies...
     */
    public static FileMover newMover(String fromSite, String toSite,
				     DsmProperties dsmp)
	throws Exception
    {
	// If we are moving data on the same host, we need CopyMover
	String fromHost = findHost(fromSite, dsmp);
	String toHost = findHost(toSite, dsmp);

	if(fromHost.equals(toHost))
	    return new CopyMover(fromSite, toSite, dsmp);
	else {
	    String mySite = dsmp.getSite();
	    String isSite = dsmp.getIS_Site();

	    if(toSite.equals(mySite)) {
		if(!fromSite.equals(isSite))
		    throw new Exception("Can't get file from "
					+ fromSite
					+ " because it's not IS");
		// Fetching a file - use FtpISReader
		return new FtpISReader(dsmp);
	    }
	    else if(fromSite.equals(mySite)) {
		if(!toSite.equals(isSite))
		    throw new Exception("Can't send file to "
					+ fromSite
					+ " because it's not IS");
		// Storing a file - use FtpISWriter
		return new FtpISWriter(dsmp);
	    }
	    else
		throw new Exception("FileMover can't move from "
				    + fromSite
				    + " to "
				    + toSite
				    + " while executing on "
				    + mySite);
	}
    }

    /**
     * Moves File from to File to.  NOTE that both arguments must be
     * complete paths - moveFile("a", "b") will create a renamed copy
     * of "a" at "b".  If "a" is a directory, "b" will be created as
     * a directory and will contain a copy of the contents of "a".
     * Can be used multiple times on the same FileMover object.
     */
    public abstract void moveFile(File from, File to) throws Exception;

    /**
     * Creates directories at the (presumably) writable destination.
     */
    public abstract void mkdirs(File todirpath) throws Exception;

    /**
     * Probes the existence of a file at the destination.  If the file
     * we want to move is already there, we can skip the copy.
     */
    public abstract boolean exists(File to) throws Exception;

    /**
     * Deletes a file at the destination.
     */
    public abstract boolean delete(File to) throws Exception;

    /**
     * Closes a FileMover session.  Calling moveFile() after quit()
     * is an error
     */
    public abstract void quit() throws Exception;

    /**
     * Closes a FileMover session, carefully.  This is normally called
     * in a finally{} context, where there might be an Exception
     * in flight (which is passed in).  If there <b>is</b> an Exception
     * in mid-flight, we raise it as the main problem in the chain.
     */
    public void quit(Exception primaryE) throws Exception
    {
	try {
	    quit();
	}
	catch (Exception ftpe) {
	    if(primaryE == null)
		throw ftpe;
	    else
		throw new Exception(ftpe.toString()
				    + "while handling other Exception",
				    primaryE);
	}
    }
}
