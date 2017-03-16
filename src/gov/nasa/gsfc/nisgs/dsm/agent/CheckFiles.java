/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.GregorianCalendar;
import java.sql.ResultSet;
import java.io.File;

/**
 * CheckFiles looks at all files the database thinks are on the local node
 * and sees if they are really here
 * @version 1.0 and we know what that means...
 */
public class CheckFiles
{
    private DsmProperties config;

    public static void main(String[] args) throws Exception
    {
        CheckFiles checkfiles = new CheckFiles();

	try
            {
                checkfiles.run();
            }
	catch (Exception re)
            {
		System.err.println("CheckFiles error:");
		re.printStackTrace();
            }
    }

    CheckFiles() throws Exception
    {
	config = new DsmProperties();

	// Crap out now if we don't have a sane data directory
	Utility.isWritableDirectory(config.getLocalDataDirectory());

        System.out.println("CheckFiles ready");
    }

    void run() throws Exception
    {
        DSMAdministrator dsm = new DSMAdministrator("CheckFiles","CheckFiles");
        try
	    {
		checkProductResources(dsm);
	    }
        catch (Exception e)
	    {
		throw e;
	    }
        finally
	    {
		dsm.dispose();
	    }
    }

    /**
     * Checks product resources on this site.
     */
    private void checkProductResources(DSMAdministrator dsm) throws Exception
    {
        String mysite = dsm.getSite();
        String qmysite = Utility.quote(mysite);
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT Resources.id,Resources.path,Directories.path");
        sb.append(" FROM Resources,ResourceSites,Directories,Products");
	sb.append(" WHERE Resources.id=ResourceSites.resource");
        sb.append(" AND Products.id=Resources.product");
        sb.append(" AND ResourceSites.directory=Directories.id");
        sb.append(" AND ResourceSites.site=");
        sb.append(qmysite);
        String sql = sb.toString();
        ResultSet r = dsm.query(sql);
	int numrows = 0;
	int numerrors = 0;
        while (r.next())
	    {
		String rid = r.getString(1);
		String filename = r.getString(2);
		String directory = r.getString(3);
		numerrors += checkFile(rid, directory,filename);
		numrows++;
	    }
	System.out.println(numrows + " file entries checked, " + numerrors + " errors detected.");
    }

    private int checkFile(String rid, String directory, String filename)
    {
        File file = new File(directory,filename);
        if (!file.isAbsolute())
	    {
		String dataRoot = config.getLocalDataDirectory();
		file = new File(dataRoot,file.getPath());
	    }
        if (file.exists())
	    {
		System.out.print('.');
		System.out.flush();
		return 0;
	    }
        else
	    {
		System.err.println("File " + file.getPath() + " does not exist (resource ID " + rid + ")");
		return 1;
	    }
    }
}
