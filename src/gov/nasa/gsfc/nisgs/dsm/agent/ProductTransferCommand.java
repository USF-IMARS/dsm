/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import java.util.Collection;
import java.io.File;
import gov.nasa.gsfc.nisgs.dsm.*;
import gov.nasa.gsfc.nisgs.dsm.FileMover;


/**
 * ProductTransferCommand copies one product from the IS to the local computer.
 */
class ProductTransferCommand extends TransferCommand
{
    private String is_site;
    private Product product;
    private FileMover fm = null;

    ProductTransferCommand(DsmProperties setup, DSMAdministrator dsm,
            String tableId, String commandId)
    {
        super(setup,dsm,"Products",tableId,commandId);
        is_site = setup.getIS_Site();
    }

    /**
     * Get a string representation of the product to be copied.
     */
    String getItem()
    {
        return (product == null)? null : product.toString();
    }

    /**
     * Determine if the product is present on the IS or local.
     */
    boolean test() throws Exception
    {
        boolean ok = true;
        product = dsm.getProduct(tableId);
        if (product == null)
        {
            throw new Exception("Product " + tableId + "does not exist in the database.");
        }
        Collection<Resource> resources = product.getResources();
        for (Resource r : resources)
        {
            //local? on IS?
            ok = (r.getFile() != null) || (r.getFile(is_site) != null);
            if (!ok) break;
        }
        return ok;
    }

    /**
     * Execute the transfer. Do not execute before you test.
     */
    void execute() throws Exception
    {
        state = SUCCESS;
        Collection<Resource> resources = product.getResources();
        fm = FileMover.newMover(is_site, dsm.getSite(), setup);
	Exception primaryE = null;
        try
        {
            for (Resource r : resources)
            {
                fetchResource(r);
            }
        }
        catch (Exception e)
        {
            state = FAILURE;
	    primaryE = e;
            throw e;
        }
        finally
        {
            if (fm != null)
		    fm.quit(primaryE);
        }
    }

    private void fetchResource(Resource r) throws Exception
    {
        boolean fault = false;
        File localFile = r.getFile();
        boolean isLocal = localFile != null;
        if (isLocal && !localFile.exists())
        {
            isLocal = false;
            fault = true;
            //fault means it is not local but the database thinks it is.
        }

        if (!isLocal)
        {
            File remoteFile = r.getFile(is_site);
            if (remoteFile == null)
            {
                throw new Exception("This file is not on the IS.");
            }

            if (localFile == null)
            {
                localFile = new File(localDataDirectory,r.getName());
            }
            fm.moveFile(remoteFile, localFile.getAbsoluteFile());
            if (!fault)
            {
                //if fault is true, the db already thinks it's local.
                final boolean doCommit = true;
                dsm.insertResourceSiteUpdate(dsm.getSite(),localFile.getParent(),
                        r.getId(),doCommit);
            }
        }
    }
}
