/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.LinkedList;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * RDRMover moves level 0 RDR files from the local computer to the IS computer.
 * It has two -D parameters, -DsleepSeconds=30 and -DerrorLimit=2
 * It watches over its incoming files directory (nisgs.properties values
 * DSM_INCOMING_DIRECTORY or DSM_DATA_DIRECTORY).
 */
public abstract class Mover {
    
    protected static final long ONE_SECOND = 1000L;
    protected static int errorLimit = 2;
    protected DsmLog logger = null;
    protected DsmProperties config;
    private File is_dropbox;
    private File incomingDataDirectory;
    private File problemFilesDirectory;

    /**
     * Constructor that only fails when it sees a condition that is
     * unlikely to heal itself - database connection failures get
     * retried, bogus config files and directory structures crap out.
     */

    Mover() throws Exception
    {
    	//The DSM properties class holds the contents of the dsm.properties file.
    	long sleepMillis = 1000;
    	while(logger == null) {
    		try {
    			config = new DsmProperties();
    			logger = new DsmLog(getMyName(), config);
    		}
    		catch (Exception e) {
    			System.err.println(getMyName() + " error in initialization!");
    			e.printStackTrace();
    			try {
    				Thread.sleep(sleepMillis);
    			}
    			catch (Exception ee) {};
    			if(sleepMillis < 1000000)
    				sleepMillis = 2 * sleepMillis;
    		}
    	}
    	is_dropbox = new File(config.getProperty("IS_dropbox","/"));

    	// The directory we watch is controlled by properties
    	// DSM_INCOMING_DIRECTORY or DSM_DATA_DIRECTORY
    	// in that order
    	if(config.getProperty("DSM_INCOMING_DIRECTORY") != null) {
    		incomingDataDirectory = new File(config.getProperty("DSM_INCOMING_DIRECTORY"));
    	} else {
    		incomingDataDirectory = new File(config.getProperty("DSM_DATA_DIRECTORY"));
    	}

    	// Complain now if the local data directory is hosed somehow...
    	try {
    		Utility.isWritableDirectory(incomingDataDirectory);
    	}
    	catch (Exception e) {
    		logger.report("Incoming Directory fatal error -- ", e);
    		System.exit(-1);
    	}

    	// copy problem files to the failure directory.
    	problemFilesDirectory = new File(incomingDataDirectory,"FAILED/");
    	if (!problemFilesDirectory.exists())
    	{
    		problemFilesDirectory.mkdir();
    	}

    	logger.report(getMyName()+ " ready -- version " + getVersion());
    }

    void run(boolean batchRegister) throws Exception {
    	//Get all file that match the filter in the directory.
    	File[] filteredFiles = incomingDataDirectory.listFiles(getFilenameFilter());

    	if (filteredFiles.length > 0)
    	{
    		logger.report(getMyName() + " Processing Started");

    		logger.report(getMyName() + " " + filteredFiles.length + " " + getPassType() + " file found");

    		// in the new mover, we try to create passes 
    		// before doing anything... this is done in parts
    		// mainly for debugging purposes...
    		MoverPassCreate createPasses = makePassCreate(filteredFiles);
    		
    		// create them but don't actually update them yet...
    		// delete slivers of 60 seconds or less
    		createPasses.createPassesForDSM(getSliverTimeInSeconds());

		// OK, now send the passes to the DSM
		createPasses.sendPassesToDSM();

    		logger.report(getMyName() + " Pass Creation Phase Over");

    	       
    		// traditional mover processing ...

    		FileMover fm = FileMover.newMover(config.getSite(), config.getIS_Site(), config);
    		Exception primaryE = null;
    		try {
    			logger.report(getMyName() +  " attempting to transfer files to IS");

    			transfer(fm, config.getIS_Site(), filteredFiles, batchRegister);
    			logger.report(getMyName() +  " transfer completed");

    		} catch (Exception te) {
    			primaryE = te;
    			throw te;
    		} finally {
    			fm.quit(primaryE);
    			logger.report(getMyName() + " processing ending");
    		}

    	}
    }

    // Canonical Java optional argument simulation
    void run() throws Exception { run(false); }

    



    private void transfer(FileMover fm, String toSite, File[] files, boolean batchRegister) throws Exception {
    	//Any DSM setup exceptions are fatal and re-thrown. Exceptions in the try/catch
    	//block will just abort the current session, and we will try again in the next
    	//cycle.
    	DSMAdministrator dsm = new DSMAdministrator(getMyName(),getMyName());

    	//I get all level 0 products that I put into the dsm. I do this so I can
    	//skip RDR files that I have already processed.
    	List<Product> dsmProducts = null;
    	try {
	    dsmProducts = dsm.getProductsByAgent(getMyName());
    	} catch (Exception dsmpe) {
    		logger.report(getMyName() + " processing aborted...");
    		dsm.dispose();
    		throw dsmpe;
    	}

	// Anybody think there's too much ceremony in creating and
	// maintaining a list of pairs of objects in Java?  Anybody?
	class MovedProduct {
	    public Product product;
	    public MoverRawProduct rawProduct;

	    public MovedProduct(Product p, MoverRawProduct mwp) {
		product = p; rawProduct = mwp;
	    }
	}

	List<MovedProduct> batchProducts = new LinkedList<MovedProduct>();

	// Start with a commit; storeProduct will commit
	// after each successful file ingest
	dsm.commit();

    	//I process each RDR in my list of files.
    	for (int n = 0; n < files.length; n++)
    	{
		MoverRawProduct rawProduct = null;
		try
		{
    			rawProduct = makeMoverRawProduct(files[n],dsm);
		}
		catch (Exception e)
		{
			logger.report("Exception!");
		}
    		try
    		{
    			if (rawProduct.getFault() != null)
    			{
    				throw new Exception(rawProduct.getFault());
    			}
    			if (dsmProducts.contains(rawProduct))
    			{
    				throw new Exception("DSM already contains " + rawProduct);
    			}
    			else
    			{
    				//From the product type, I get the IS directory from the dsm
    				//to where I will put the RDR. If the dsm does not have a directory,
    				//I put the RDR in the IS "dropbox."
    				File remoteDirectory = is_dropbox;
    				ProductType productType = rawProduct.getProductType();
    				if (productType != null && productType.getISdirectory() != null)
    				{
    					remoteDirectory = productType.getISdirectory();
    				}

    				Product product = rawProduct.createProduct(remoteDirectory);
    				logger.report(getMyName() + " Send to IS "+product);
    				// ftp.sendRDR(RDR,remoteDirectory);
    				dataFileMover(fm, 
    								rawProduct.getDataFile(),
    								new File(remoteDirectory, rawProduct.getDataFile().getName()));
    				
    				contructionRecordMover(fm, 
    										rawProduct.getRecordFile(), 
    										new File(remoteDirectory, rawProduct.getRecordFile().getName()));
				if(batchRegister)
				    batchProducts.add(new MovedProduct(product, rawProduct));
				else {
				    dsm.storeProduct(toSite, product);
				    logger.report(getMyName() + " registered with DSM");
				    rawProduct.delete();
				}
    			}
    		}
    		catch (Exception pe)
    		{
    			logger.report(getMyName() + " Error", pe);
    			
    			pe.printStackTrace();

    			if (rawProduct != null)
    			{
			    logger.report(getMyName() + " moving files to FAILED: " +rawProduct.getFileNames());
    				rawProduct.move(problemFilesDirectory);
    			}
    		}
    	}
	if(batchRegister) {
	    // Rip down the new MovedProduct list, creating them
	    // and spitting if things fail
	    logger.report(getMyName() + " batch registering " + batchProducts.size() + " with DSM");
	    List<MovedProduct> safeToDelete = new LinkedList<MovedProduct>();
	    for(MovedProduct mp : batchProducts) {
		try {
		    dsm.storeProduct(toSite, mp.product);
		    safeToDelete.add(mp);
		}
		catch (Exception pe) {
		// Creating this product failed
		    logger.report(getMyName() + " Error", pe);
		    
		    pe.printStackTrace();
		    
		    if (mp.rawProduct != null)
    			{
			    logger.report(getMyName() + " moving files to FAILED: " +mp.rawProduct.getFileNames());
			    mp.rawProduct.move(problemFilesDirectory);
    			}
    		}
	    }
	    logger.report(getMyName() + " done batch registering");

	    // Do the deletion of the incoming products as a separate step
	    // (to make the registration as fast as possible)
	    for(MovedProduct mp : safeToDelete) {
		try {
		    mp.rawProduct.delete();
		}
		catch (Exception pe) {
		// Creating this product failed
		    logger.report(getMyName() + " Error", pe);
		    
		    pe.printStackTrace();
		    
		    if (mp.rawProduct != null)
    			{
			    logger.report(getMyName() + " moving files to FAILED: " +mp.rawProduct.getFileNames());
			    mp.rawProduct.move(problemFilesDirectory);
    			}
    		}
	    }
	}


    	try { dsm.dispose(); } catch (Exception edispose) {};
    }
    
	final public DsmLog getLogger() {
		return logger;
	}

	public abstract String getMyName();
    public abstract String getVersion();
    public abstract PassType getPassType();
    public abstract MoverPassCreate makePassCreate(File[] passFile) throws MoverException;
    public abstract MoverRawProduct makeMoverRawProduct(File file, DSMAdministrator dsm);
    public abstract FilenameFilter getFilenameFilter();
    public abstract int getSliverTimeInSeconds();
    public abstract void dataFileMover(FileMover fileMover, File from, File to) throws MoverException;
    public abstract void contructionRecordMover(FileMover fileMover, File from, File to) throws MoverException;
 
}
