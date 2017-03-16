/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.*;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.FileMover;
import gov.nasa.gsfc.nisgs.dsm.CopyMover;

import java.io.File;
import java.io.FilenameFilter;

/**
 * SDRMover moves level 0 SDR files from the local computer to the IS computer.
 * It has two -D parameters, -DsleepSeconds=30 and -DerrorLimit=2
 * It watches over its incoming files directory (nisgs.properties values
 * DSM_INCOMING_DIRECTORY or DSM_DATA_DIRECTORY).
 */
public  class SDRMover extends Mover {
	public static  FilenameFilter SDRFilter = new FilenameFilter() {
        public boolean accept(File dir, String name)
        {
	    return SDRType.isSDRfile(name);
        }
    };
    
	SDRMover() throws Exception {
		super();		
	}

	@Override
	public String getMyName() {
		return "SDRMover";
	}

	@Override
	public PassType getPassType() {
		return PassType.SDR;
	}

	@Override
	public String getVersion() {
		return "v1.0";
	}

	@Override
	public MoverRawProduct makeMoverRawProduct(File file, DSMAdministrator dsm) {
		return new SDR(getMyName(), file, dsm);
	}

	@Override
	public MoverPassCreate makePassCreate(File[] passFile) throws MoverException {
		return new SDRPassCreate(getMyName(), passFile);
	}

	@Override
	public FilenameFilter getFilenameFilter() {
		return SDRFilter;
	}

	@Override
	public int getSliverTimeInSeconds() {
		return 0; // slivers passes less than this are ignored
	}

	@Override
	public void dataFileMover(FileMover fileMover, File from, File to) throws MoverException {
	    // Internal performance enhancement hack:
	    // If our fileMover is a CopyMover, we will short-circuit the copy
	    // and try to just do File.renameTo().  If it works, we're good
	    // since the rest of SDRmover will do the "delete" which will succeed
	    // since the file won't be there...
	    if(fileMover instanceof CopyMover) {
		try {
		    from = fileMover.makeAbsoluteLocal(from);
		    to = fileMover.makeAbsoluteLocal(to);
		    if (!from.renameTo(to))
			throw new MoverException("SDRMover: renaming " + from + " to " + to + " failed");
		}
		catch (Exception e) {
		    throw new MoverException(e);
		}
	    }
	    else {
		try {
			fileMover.moveFile(from, to);
		} catch (Exception e) {
			throw new MoverException(e);
		}
	    }
	}

	@Override
	public void contructionRecordMover(FileMover fileMover, File from, File to)  throws MoverException {
		// does nothing since the data file and 'construction record' for an SDR are the same
	}
	
	
	
    public static void main(String[] args) throws Exception
    {
        int n = Integer.getInteger("sleepSeconds",30).intValue();
        
        if (n < 1) {
        	n = 1;
        }
        long sleepyTime = (long)n * ONE_SECOND;
       
        errorLimit = Integer.getInteger("errorLimit",2).intValue();
        
       
        SDRMover rdrMover = new SDRMover();
	
        rdrMover.getLogger().report("SDRMover starting in " + rdrMover.config.getIngest_Mode() + " mode");
        	
        
        while (true)
        {
            try
            {
		// Pass the batchRegister flag for SDRs
            	rdrMover.run(true);
            }
            catch (Exception re)
            {
            	rdrMover.getLogger().report("SDRMover processing exception: " + re.getMessage());
            	re.printStackTrace();
            }
            try { 
            	Thread.sleep(sleepyTime); 
            } catch (InterruptedException e) {
            	// nothing...
            }
        }
    }

}
