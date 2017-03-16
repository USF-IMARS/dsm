/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.test;
import gov.nasa.gsfc.nisgs.dsm.DSM;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.dsm.Product;

import java.util.Calendar;
import java.util.Date;

public class ProductGeotiffSteps {

	public static boolean geotiff(String[] inputs, String[] outputs) {
		return true;
	}
  public static void main(String[] args)
    {
        try
        {
        	// this just connects to the database
            DSM dsm = new DSM("drl-lab","sst-geotiff-group1","sst-geotiff","drl-host");
  
	        // now search for a product LIKE the pattern.  
            // Why are we doing this?
            Product p_modisSST = dsm.reserveProductLikeProductType("%.modis.sst", "");
            
            // the product type in this context would be something like: (in this case)
            // aqua.modis.sst or terra.modis.sst
            //
            String ptn_modisSST = p_modisSST.getProductType();
            
            // chop off the tail of productType & get the satellite name
            int dotIndex = ptn_modisSST.indexOf('.');
            String satName = ptn_modisSST.substring(0, dotIndex);

            // NOTE: I think p_modisSST.getSpacecraft()
            // would do the same as above
            //
            
            // Now we use the time of FIRST
            // product that was reserved to get
            // the PASS around the same time...
            // 
            // NOTE: the spaceCraft name seems redundant, 
            // don't we already have it? why did we get
            // it the first time and peel it out of the
            // productType name?
            Date time = p_modisSST.getStartTime();
            String spaceCraft = p_modisSST.getSpacecraft();
            
            Pass pass = dsm.getPass(spaceCraft,time);
            	              
            // get associated resource called DATA from the product
            // the resource if basically a filename.  The filename -- why
            // do we this?
            // i think it just to print the FILENAME of the resource associated
            // with this product.  As I interpret things, this is the OUTPUT
            // from that algorithm...???
            String livesAt = p_modisSST.getResource("DATA");
            System.out.println("The product data file lives at: " + livesAt);
           
            // this call will look in the database for a product,
            // In this case we take the satellite name of the original
            // product we looked up and use that to form a ProductType.
            // That's specified in the first parameter and should
            // already exist in the database.
            // The DSM will COPY the product's FILES using the
            // DSMR/transaction manager if they are NOT local to our
            // system.  
            // After this it will return the Product of that ProductType
            //  WHich DOESNT SEEM TO BE USED?
            Product p_mxsd03 = dsm.fetchProduct(satName + ".modis.mxd03", pass, time);
            
            // these aren't QUITE right because I don't really understand
            // the syntax in the XML...
            
            // input filename strings
            String[] inputs = new String[6];
            inputs[0] = satName + ".modis.sst.filename";
            inputs[1] = satName + ".modis.sst.ext";
            inputs[2] = satName + ".modis.sst.base";
            inputs[3] = satName + ".modis.mxd03.filename" ;
            inputs[4] = satName + ".modis.mxd03.ext" ;
            inputs[5] = satName + ".modis.mxd03.base" ;
            
           
            // output filename strings
            String[] outputs = new String[3];
            outputs[0] = satName + ".modis.sst" ;
            outputs[1] = satName + ".modis.mxd03" ;
            outputs[2] = satName + ".modis.sst.geotiff" ;
            

            // run algorithm
            geotiff(inputs, outputs);
            
            // upon success, gather up the outputs and form a
            // new product.
            // the product is based on the old but with a new
            // productType
            
            String outputFilename = null;
			String geotiffProductType = outputFilename;
            Product p_geotiff = new Product(p_modisSST, geotiffProductType);
            
            p_geotiff.setAlgorithm("sst-geotiff", "1.0");
            
            // this just seems to say that there's going to be
            // A NEW RESOURCE in the database for someone else
            // to use, and in this case it seems to be the principle
            // OUTPUT of the algorithm
            // ???
            outputFilename = satName + ".modis.sst.geotiff";
            p_geotiff.addResource("DATA", outputFilename);
            
            // this updates the database itself, and publisher
            // MAY move the resources to IS
            dsm.storeProduct(p_geotiff);
            
 
            // release the very first product that was looked up
            // the reason is that once it was reserved, some other
            // producer couldn't get there hands on it... 
            // ???
            dsm.releaseProduct(p_modisSST);
 

            dsm.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
