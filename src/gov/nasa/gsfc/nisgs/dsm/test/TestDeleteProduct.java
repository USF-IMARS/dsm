/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.ProductFactory;
import gov.nasa.gsfc.nisgs.properties.Utility;
import java.io.File;

/**
 * TestDeleteProduct deletes Product "1"
 */
public class TestDeleteProduct
{
    private DsmProperties config;

    public static void main(String[] args) throws Exception
    {
        TestDeleteProduct testDeleteProduct = new TestDeleteProduct();

	try
            {
                testDeleteProduct.run(args[0]);
            }
	catch (Exception re)
            {
		System.err.println("TestDeleteProduct error:");
		re.printStackTrace();
            }
    }

    TestDeleteProduct() throws Exception
    {
	config = new DsmProperties();

        System.out.println("TestDeleteProduct ready");
    }

    void run(String processID) throws Exception
    {
 	System.err.println("Trying to remove Product " + processID);

	ProductFactory.removeProduct(config.getConnection(), processID);
	System.err.println("removeProduct() didn't die");

    }
}
