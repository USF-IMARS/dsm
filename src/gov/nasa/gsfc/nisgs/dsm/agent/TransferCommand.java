/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;

/**
 * TransferCommand is an abstract class that copies a product or a file from the
 * IS to the local computer. DSMR uses it.
 */
abstract class TransferCommand
{
    static final int PENDING = 0;
    static final int SUCCESS = 1;
    static final int FAILURE = 2;
    protected int state = PENDING;
    protected DsmProperties setup;
    protected DSMAdministrator dsm;
    protected final String id;
    protected final String table;
    protected final String tableId;
    protected String localDataDirectory;
    protected String is_host;
    protected String is_ftpuser;
    protected String is_ftppassword;
    protected String is_name = null;

    TransferCommand(DsmProperties setup, DSMAdministrator dsm, String table,
            String tableId, String id)
    {
        localDataDirectory = setup.getLocalDataDirectory();
        is_host = setup.getIS_Host();
        is_ftpuser = setup.getProperty("IS_ftpReaderUser");
        is_ftppassword = setup.getProperty("IS_ftpReaderPassword");
        is_name = setup.getIS_Site();
	this.setup = setup;
        this.dsm = dsm;
        this.table = table;
        this.tableId = tableId;
        this.id = id;
    }

    /**
     * Has this request been serviced or is it still open? Pending means open.
     */
    final boolean isPending()
    {
        return state == PENDING;
    }

    final void setState(int state)
    {
        this.state = state;
    }

    final int getState()
    {
        return state;
    }

    final String getId()
    {
        return id;
    }

    public boolean equals(Object o)
    {
        boolean match = false;
        if (o instanceof TransferCommand)
        {
            TransferCommand tc = (TransferCommand)o;
            match = table.equals(tc.table) && tableId.equals(tc.tableId);
        }
        return match;
    }

    /**
     * Determine if the thing is present on the IS.
     */
    abstract boolean test() throws Exception;

    /**
     * Execute the transfer. Do not execute before you test.
     */
    abstract void execute() throws Exception;

    /**
     * Get a string representation of the thing to be copied.
     */
    abstract String getItem();

    public String toString()
    {
        return "TransferCommand id="+id+" table="+table+" tableId="+tableId+ " state="+state;
    }
}
