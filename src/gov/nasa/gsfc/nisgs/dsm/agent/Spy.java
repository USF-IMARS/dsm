/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.dsm.Product;
import gov.nasa.gsfc.nisgs.dsm.ProductFactory;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.GregorianCalendar;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.File;

/**
 * Spy shows product creation as it happens after a new
 * Pass show up...
 * @version 1.0 Initial version 
 * 
 */
public class Spy
{
    private DsmLog log;
    private DsmProperties config;
    DSMAdministrator dsm;
    private static final long ONE_SECOND = 1000L;
    private DsmLog logger;
    
    
    public static void main(String[] args) throws Exception
    {
    	long sleepyTime = (long)10 * ONE_SECOND;

        Spy spy = new Spy();

        while (true)
        {
        	java.util.Date now = new java.util.Date();
        	
            try
            {
                spy.run(now);
            }
            catch (Exception re)
            {
                spy.logger.report("Spy error",re);
            }
            try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
        }
    }

    Spy() throws Exception
    {
        config = new DsmProperties();
	logger = new DsmLog("Spy", config);
        logger.report("Spy Ready");
    }
    
    void run(java.util.Date now) throws Exception
    {
                
        DSMAdministrator dsm = new DSMAdministrator("Spy","Spy");
 
        logger.report("Spy starting...");

        
        Pass pass = getPass(now);
        
        if (pass != null) {
        	// begin processing...
        	logger.report("New Pass Found: " + pass.getSpacecraft() + "Creation: " + pass.getCreation().toString());
        	displayProductsFromPass(pass);
        }
  
        dsm.dispose();
        
        logger.report("Spy complete.");

    }
    
    private void displayProductsFromPass(Pass pass) {
    	
    }
    /**
     * Get the product object from the database for this product type and
     * pass and with a start and end time that brackets this time.
     * No files are copied. If more than one product satisfies the conditions,
     * it returns the first one that the database returns.
     * @param productType A product type
     * @param pass A pass
      * @return A product of this product type, linked to the specified pass,
     *      and one that brackets the specified time.
     *      If more than one product satisfies the conditions, it returns the
     *      first one that the database returns. It returns null if no product
     *      satisfies the conditions.
     */
    /*
    public Product getProduct(Pass pass)
            throws Exception
    {
        
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT * FROM Products WHERE pass=");
        sb.append(pass.getId());
        
        return queryProduct(sb.toString());
    }
    */
    /**
     * Get a single product from a SQL select statement that gets products.
     * It only returns the first one if more than one satisfies the statement.
     */
    /*
    protected Product queryProduct(String sqlQueryString) throws Exception
    {
        Product product = null;
        ResultSet r = dsm.query(sqlQueryString);
        if (r.next())
        {
            product = ProductFactory.makeProduct(connection,thisSite,r);
        }
        
        return product;
    }
    */
    
    /**
     * Get a pass any spacecraft if its current time is greater than our most recent
     * start time 
     */
    private Pass getPass(java.util.Date time) throws Exception
    {
        Pass pass = null;
        String timeString = Utility.quote(Utility.format(time));
        
        String sqlq = "SELECT * FROM Passes WHERE creation >= " + timeString;
       
        ResultSet r = dsm.query(sqlq);
        if (r.next())
        {
            pass = new Pass(r);
        }
        
        return pass;
    }
}
