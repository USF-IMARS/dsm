/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
 */
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.*;
import java.io.File;
import java.io.FilenameFilter;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * PdsMover moves level 0 PDS files from the local computer to the IS computer.
 * It has two -D parameters, -DsleepSeconds=30 and -DerrorLimit=2
 * It watches over its incoming files directory (nisgs.properties values
 * DSM_INCOMING_DIRECTORY or DSM_DATA_DIRECTORY).
 */
public class PdsMover
{
	private static final String VERSION = "5.0.1";
	private static final String MYNAME = "pds-mover";
	private static final long ONE_SECOND = 1000L;
	private static int errorLimit = 2;
	private DsmLog logger = null;
	private DsmProperties config;
	private File is_dropbox;
	private int consecutiveFailures = 0;
	private File incomingDataDirectory;
	private File problemFilesDirectory;

	public static void main(String[] args) throws Exception
	{
		int n = Integer.getInteger("sleepSeconds",30).intValue();
		if (n < 1) n = 1;
		long sleepyTime = ONE_SECOND;//(long)n * ONE_SECOND;
		errorLimit = Integer.getInteger("errorLimit",2).intValue();

		//My key for new PDS products is the existence of construction record files.
		FilenameFilter pdsFilter = new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				return name.endsWith("0.PDS");
			}
		};

		PdsMover mover = new PdsMover();

		while (true)
		{
			try
			{
				mover.run(pdsFilter);
			}
			catch (Exception re)
			{
				mover.logger.report(re);
			}
			try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
		}
	}

	/**
	 * Constructor that only fails when it sees a condition that is
	 * unlikely to heal itself - database connection failures get
	 * retried, bogus config files and directory structures crap out.
	 */

	PdsMover() throws Exception
	{
		//The DSM properties class holds the contents of the dsm.properties file.
		long sleepMillis = 1000;
		while(logger == null) {
			try {
				config = new DsmProperties();
				logger = new DsmLog("PdsMover", config);
			}
			catch (Exception e) {
				System.err.println("PdsMover error in initialization!");
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
		if(config.getProperty("DSM_INCOMING_DIRECTORY") != null)
			incomingDataDirectory =
				new File(config.getProperty("DSM_INCOMING_DIRECTORY"));
		else
			incomingDataDirectory =
				new File(config.getProperty("DSM_DATA_DIRECTORY"));

		// Complain NOW if the local data directory is hosed somehow...
		try {
			Utility.isWritableDirectory(incomingDataDirectory);
		}
		catch (Exception e) {
			logger.report("Incoming Directory", e);
			System.exit(-1);
		}

		//I copy problem files to the failure directory.
		problemFilesDirectory = new File(incomingDataDirectory,"FAILED/");
		if (!problemFilesDirectory.exists())
		{
			problemFilesDirectory.mkdir();
		}

		logger.report("PdsMover ready - v" + VERSION);
	}

	void run(FilenameFilter pdsFilter) throws Exception
	{
		//Get all construction records in the pds directory.
		File[] pdsFiles = incomingDataDirectory.listFiles(pdsFilter);

		if (pdsFiles.length > 0)
		{
			logger.report("PdsMover Processing Started");

			logger.report("PdsMover " + pdsFiles.length + " PDS construction records found");

			// in the new mover, we try to create passes 
			// before doing anything... this is done in parts
			// mainly for debugging purposes...
			PdsPassCreate createPasses = new PdsPassCreate(incomingDataDirectory);
			// create them but don't actually update them yet...
			createPasses.createPassesForDSM(60);
			// delete slivers of 60 seconds or less

			logger.report("PdsMover Calling Sliver processing");
			createPasses.deleteSliverPasses(60);

			logger.report("PdsMover Sliver processing End");
			// now actually send them..
			createPasses.sendPassesToDSM();
			// and now tell the log...
			createPasses.logPasses();

			logger.report("PdsMover Pass Creation Phase Over");

			// just in case we deleted things, get them again

			logger.report("PdsMover re-listing PDS construction records");
			pdsFiles = incomingDataDirectory.listFiles(pdsFilter);

			logger.report("PdsMover " + pdsFiles.length + " PDS construction records re-found");

			// traditional mover processing ...
			final boolean passive = true;
			FileMover fm = FileMover.newMover(config.getSite(), config.getIS_Site(), config);
			Exception primaryE = null;
			try
			{
				logger.report("PdsMover attempting to transfer files to IS");

				transfer(fm, config.getIS_Site(), pdsFiles);
				logger.report("PdsMover transfer completed");
			}
			catch (Exception te)
			{
				primaryE = te;
				throw te;
			}
			finally
			{
				fm.quit(primaryE);
				logger.report("PdsMover processing ending");
			}
		}
	}

	private void transfer(FileMover fm, String toSite, File[] pdsFiles) throws Exception
	{
		//Any DSM setup exceptions are fatal and rethrown. Exceptions in the try/catch
		//block will just abort the current session, and we will try again in the next
		//cycle.
		try(DSMAdministrator dsm = new DSMAdministrator(MYNAME,MYNAME)){

			//I get all level 0 products that I put into the dsm. I do this so I can
			//skip PDS files that I have already processed.
			java.util.List<Product> dsmProducts = null;
			try
			{
				dsmProducts = dsm.getProductsByAgent(MYNAME);
			}
			catch (Exception dsmpe)
			{
				logger.report("PdsMover processing aborted...");
				throw dsmpe;
			}

			boolean failed = false;

			//I process each PDS in my list of files.
			for (int n = 0; n < pdsFiles.length; n++)
			{
				PDS pds = new PDS(MYNAME,pdsFiles[n],dsm);
				try
				{
					if (pds.getFault() != null)
					{
						throw new Exception(pds.getFault());
					}
					if (dsmProducts.contains(pds))
					{
						throw new Exception("DSM already contains " + pds);
					}
					else
					{
						//From the product type, I get the IS directory from the dsm
						//to where I will put the pds. If the dsm does not have a directory,
						//I put the pds in the IS "dropbox."
						File remoteDirectory = is_dropbox;
						ProductType productType = pds.getProductType();
						if (productType != null && productType.getISdirectory() != null)
						{
							remoteDirectory = productType.getISdirectory();
						}

						Product product = pds.createProduct(remoteDirectory);
						logger.report("Send to IS "+product);
						// ftp.sendPDS(pds,remoteDirectory);
						fm.moveFile(pds.getDataFile(),
								new File(remoteDirectory, pds.getDataFile().getName()));
						fm.moveFile(pds.getRecordFile(),
								new File(remoteDirectory, pds.getRecordFile().getName()));

						dsm.storeProduct(toSite, product);
					}
				}
				catch (Exception pe)
				{
					failed = true;
					logger.report("PdsMover Error", pe);

					if (consecutiveFailures > errorLimit && pds != null)
					{
						logger.report("PdsMover moving files to FAILED");
						pds.move(problemFilesDirectory);
					}
				}
				finally
				{
					pds.delete();
				}
			}

			if (failed)
			{
				++consecutiveFailures;
			}
			else
			{
				consecutiveFailures = 0;
			}
		}
	}
}
