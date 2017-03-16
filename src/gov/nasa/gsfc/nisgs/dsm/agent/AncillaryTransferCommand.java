/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.FileMover;
import java.sql.ResultSet;
import java.io.File;


/**
 * AncillaryTransferCommand copies one ancillary file from the IS to the local
 * computer. DSMR uses it.
 */
class AncillaryTransferCommand extends TransferCommand
{
    private String siteTable;
    private String filename;

    AncillaryTransferCommand(DsmProperties setup, DSMAdministrator dsm,
			     String table, String siteTable, String tableId, 
			     String commandId)
    {
        super(setup,dsm,table,tableId,commandId);
        this.siteTable = siteTable;
    }

    /**
     * Determine if the thing is present on the IS or local.
     */
    boolean test() throws Exception
    {
        /* I return true unconditionally because an ancillary file
	   should always be either local or on the IS, so it's an
	   error otherwise, which execute() will report when it tries
	   to copy it. If I return false, the client may delay trying
	   to get the file because it assumes it has not arrived yet,
	   which is unreasonable for ancillaries. */
        return true;
    }

    /**
     * Get a string representation of the thing to be copied.
     */
    String getItem()
    {
        return filename;
    }

    /**
     * Execute the transfer. You must test() before you call this method.
     */
    void execute() throws Exception
    {
        state = FAILURE;

        //get the basic file name.
        ResultSet r = dsm.query("SELECT path FROM " + table + " WHERE id=" + tableId);
        if (!r.next())
	    {
		//I was asked to get a file that is not in the database.
		//This should never happen.
		throw new Exception("This file is not in the database.");
	    }

        filename = r.getString("path");

        //get a list of sites and directories.
        ResultSet rr = dsm.query("SELECT site,Directories.path FROM " + siteTable +
				 ",Directories WHERE aid=" + tableId + " AND directory=Directories.id");

        String mysite = dsm.getSite();
        String is_directory = null;
        String localDirectory = null;
        boolean isLocal = false;
        boolean fault = false;

        //I determine the local and IS locations, if any.
        while (rr.next())
	    {
		String site = rr.getString("site");
		String directory = rr.getString("path");
		if (site.equals(mysite))
		    {
			localDirectory = directory;
			isLocal = true;
		    }
		else if (site.equals(is_name))
		    {
			is_directory = directory;
		    }
	    }

        //I create the local file name.
        if (localDirectory == null)
	    {
		localDirectory = localDataDirectory;
	    }
        File localFile = new File(localDirectory,filename);


        //Verify that the local file really exists.
        if (isLocal)
	    {
		isLocal = localFile.exists();
		fault = !isLocal;
	    }

        if (!isLocal)
	    {
		if (is_directory == null)
		    {
			throw new Exception("This file is not on the IS.");
		    }
		
		// Strangely enough, this is it...
		FileMover fm = FileMover.newMover(setup.getIS_Site(), dsm.getSite(), setup);
		Exception primaryE = null;
		try {
		    fm.moveFile(new File(is_directory, filename), localFile);
		    //A fault means the db already has an entry for the local version.
		    if (!fault) {
			final boolean doCommit = true;
			dsm.insertAncillarySiteUpdate(siteTable,mysite,localDirectory,tableId,doCommit);
			}
		}
		catch (Exception e) {
		    state = FAILURE;
		    primaryE = e;
		    throw e;
		}
		finally {
		    fm.quit(primaryE);
		}
	    }

        state = SUCCESS;
    }
}
