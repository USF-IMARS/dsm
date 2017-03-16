/*
Copyright (c) 1999-2012, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;

import java.io.File;
import java.util.Date;

import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.dsm.Product;
import gov.nasa.gsfc.nisgs.dsm.ProductType;

public interface MoverRawProduct {

	public String getFault();

	public Pass getPass();

	public Date getStartTime();

	public Date getStopTime();
	
	public ProductType getProductType();

	public Product createProduct(File remoteDirectory) throws MoverException;

	public File getDataFile();

	public File getRecordFile();

        public String getFileNames();

	public void delete() throws MoverException;

	public void move(File problemFilesDirectory);

}
