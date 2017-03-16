/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.Product;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Collection;
import java.sql.ResultSet;
import java.io.File;

/**
 * TestProduct looks at all files the database thinks are on the local node
 * and sees if they are really here
 * @version 1.0 and we know what that means...
 */
public class TestProduct
{
    private DsmProperties config;

    public static void main(String[] args) throws Exception
    {
        TestProduct testP = new TestProduct();

	try
            {
                testP.run(args[0]);
            }
	catch (Exception re)
            {
		System.err.println("TestProduct error:");
		re.printStackTrace();
            }
    }

    TestProduct() throws Exception
    {
	config = new DsmProperties();

	// Crap out now if we don't have a sane data directory
	Utility.isWritableDirectory(config.getLocalDataDirectory());

        System.out.println("TestProduct ready");
    }

    void run(String productID) throws Exception
    {
        DSMAdministrator dsm = new DSMAdministrator("TestProduct","TestProduct");
	System.err.println("Trying " + productID);
        try {
	    String foo = "SELECT Products.* from Products where id=" + productID;
	    List<Product> products = dsm.queryProducts(foo);

	    for (Product p : products) {
                System.out.println("Product " + p.getId());
                Collection<String> ancestors = p.getContributingProductsIds();
                System.out.println(" has " + ancestors.size() + " ancestors:");
                for(String s : ancestors) {
		    System.out.println(s);
                }
	    }

	}
        finally {
	    dsm.dispose();
	}
    }
}
