/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.util.ArrayList;
import java.sql.ResultSet;
import gov.nasa.gsfc.nisgs.dsm.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * This program processes the TransferCommands list from the database.
 * It copies files from the IS to the local computer as commanded.
 * If the item is not registered as being on the IS, it waits until the item arrives.
 * It fails the request if the item is not on the IS but is registered to be there.
 * @version 3.22a Fixed a bug where DSMR entered an infinite loop if the file did not exist
 *      on the IS.
 */
public class DSMR
{
    private static final long ONE_SECOND = 1000L;
    private DsmLog logger = null;
    private DsmProperties config;

    public static void main(String[] args) throws Exception
    {
        int n = Integer.getInteger("sleepSeconds",5).intValue();
        if (n < 1) n = 1;
        long sleepyTime = (long)n * ONE_SECOND;

        DSMR dsmr = new DSMR();

        while (true)
        {
            try
            {
                dsmr.run();
            }
            catch (Exception e)
            {
                dsmr.logger.report("DSMR error",e);
            }
            try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
        }
    }

    /**
     * Constructor that only fails when it sees a condition that is
     * unlikely to heal itself - database connection failures get
     * retried, bogus config files and directory structures crap out.
     */

    DSMR() throws Exception
    {	
	long sleepMillis = 1000;
	while(logger == null) {
	    try {
		config = new DsmProperties();
		logger = new DsmLog("DSMR", config);
	    }
	    catch (Exception e) {
		System.err.println("DSMR error in initialization!");
		e.printStackTrace();
		try {
		    Thread.sleep(sleepMillis);
		}
		catch (Exception ee) {};
		if(sleepMillis < 1000000)
		    sleepMillis = 2 * sleepMillis;
	    }
	}

	// Complain NOW if the local data directory is hosed somehow...
	try {
	    Utility.isWritableDirectory(config.getLocalDataDirectory());
	}
	catch (Exception e) {
	    logger.report("LocalDataDirectory", e);
	    System.exit(-1);
	}

        logger.report("DSMR ready.");
    }

    void run() throws Exception
    {
        //I make a database connection. I then ask for a list of open transfer
        //commands for my site.
        DSMAdministrator dsm = new DSMAdministrator("DSMR","DSMR");
        ArrayList<TransferCommand> transferCommands = getTransferCommands(dsm,config);
        for (TransferCommand tc : transferCommands)
        {
        	// Test can throw an exception and when it does, 
        	// the entire list is not traversed, and gets to 
        	// the same point over and over. Breaking it out
        	// as follows should fix the issue. KR
        	Boolean tcTestResult = false;
        	try {
        		tcTestResult = tc.test();
        	} catch (Exception te) {
        		logger.report("DSMR: Error in test method "+tc, te);
        	}
            //Is this request open? Is the item on the local or IS computer?
            //I skip for now items that are not local and have not yet been published.
            if (tc.isPending() && tcTestResult)
            {
                //I do the transfer.
                try
                {
                    tc.execute();
                    logger.report("DSMR copied from IS " + tc.getItem());
                }
                catch (Exception te)
                {
                    //Usually because someone deleted it on the IS but not from the DB.
		    // JRB - changed from the form below:
                    logger.report("DSMR: Error executing "+tc, te);
		    // because that form caused an exception during
		    // exception processing.  Bleah.
                    // logger.report("DSMR: Error executing "+ tc + te);
                }


                //I close all commands requesting the same files.
                int state = tc.getState();
                for (TransferCommand tcx : transferCommands)
                {
                    if (tc.equals(tcx))
                    {
                        tcx.setState(state);
                        try
                        {
                            dsm.update("UPDATE TransferCommands SET complete=" + state +
                                    " WHERE id=" + tcx.getId());
                            dsm.commit();
                        }
                         catch (Exception ue)
                        {
                            logger.report("DSMR: Error updating TransferCommands table "+tcx, ue);
                        }
                    }
                }
            }
        }
        dsm.dispose();
    }

    /**
     * Get a list of open tranfer commands for this site.
     */
    private ArrayList<TransferCommand> getTransferCommands(DSMAdministrator dsm,
            DsmProperties setup) throws Exception
    {
        ResultSet r = dsm.query("SELECT id,tableName,tableId FROM TransferCommands WHERE site=" +
                Utility.quote(dsm.getSite()) + " AND complete=0 ORDER BY id");
        ArrayList<TransferCommand> transferCommands = new ArrayList<TransferCommand>();
        while (r.next())
        {
            String table = r.getString("tableName");
            String tableId = r.getString("tableId");
            String id = r.getString("id");

            TransferCommand tc = null;
            if (table.equals("Products"))
            {
                tc = new ProductTransferCommand(setup,dsm,tableId,id);
            }
            else
            {
                String siteTable = null;
                if (table.equals("SatTimeAncillaries"))
                {
                    siteTable = "SatTimeAncillarySites";
                }
                else if (table.equals("TimeAncillaries"))
                {
                    siteTable = "TimeAncillarySites";
                }
                else if (table.equals("StaticAncillaries"))
                {
                    siteTable = "StaticAncillarySites";
                }
                else
                {
                    throw new Exception("DSMR: Unknown table name " + table);
                }

                tc = new AncillaryTransferCommand(setup,dsm,table,siteTable,tableId,id);
            }
            transferCommands.add(tc);
        }
        r.close();
        return transferCommands;
    }
}
