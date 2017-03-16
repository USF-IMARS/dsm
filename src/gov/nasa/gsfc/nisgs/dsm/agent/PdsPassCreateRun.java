/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;



import java.io.File;
import java.io.FilenameFilter;
import java.util.*;



/**
 * run PdsCreatePass as its own standalone process.
 * Check file times to see if the contents of the directory
 * should be process again.  
 */
public class PdsPassCreateRun extends Thread {
	
	String passDirName;
	String moveDirName;
	static Date startDate; 
	boolean passCreation = false;
	boolean debug = false;
	
	PdsPassCreateRun() {
		
		super("PdsPassCreateRun");
		
		passDirName = new String();
		moveDirName = new String();
		
		startDate = new Date();
		
		startDate.setTime(0);
		
	}
	
	public void run() {
		
		boolean startProcessing; 


		
		try {
		    DsmLog log = new DsmLog("PdsPassCreateRun");
		    log.report("PdsPassCreate -- run thread activated.");

			while (true) {
				
				printDebug("PdsPassCreateRun -- waking up");
				
				startProcessing = false;
				
				if (moveDirName.equals(""))
					startProcessing = newFilesToProcess1();
				else
					startProcessing = newFilesToProcess2();
				
				if (startProcessing) {
					
					printDebug("PdsPassCreateRun -- processing files");
					
					if (passDirName.equals("")) throw (new Exception());
					
					PdsPassCreate passes = new PdsPassCreate(new File(passDirName));
					
					if (debug)
						passes.printPasses();
					
					
					// create them but don't
					// actually update them yet...
					passes.createPassesForDSM(60);
						
					if (debug)
							passes.printPrePassList();
						
					if (passCreation) {
						passes.sendPassesToDSM();
						
						if (debug)
							passes.printFinalPassList();
						else
							passes.logPasses();
						
						if (!moveDirName.equals(""))
							movePassesToDir(moveDirName);
					}
					
				}
				sleep(11 * 1000);
			}
		} catch (Exception ie) {
			
			ie.printStackTrace();
			
		}
		
	}
	
	private void movePassesToDir(String destDir) throws Exception {
	      
		
		// open the destination... make sure it exists
		@SuppressWarnings("unused") File destDirectory = new File(destDir);		
		
		//NOTE -- ALL PDS files only
        FilenameFilter pdsFilter = new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
            	return name.endsWith(".PDS");
            }
        };
        File pdsDirectory = new File(passDirName);
        
 
        File[] pdsFiles = pdsDirectory.listFiles(pdsFilter);
        
        // let's try renaming it... simple
        for (int i = 0; i < pdsFiles.length; i++) { 
        	String fileName = pdsFiles[i].getName();
        	File toGo = new File(destDir,fileName);
	    
        	if (pdsFiles[i].renameTo(toGo) == false) throw (new Exception());
    
        }
		
	}
	
	// this is the original decision maker on when
	// to process pass files.  Basically the idea here
	// it not repeat history.  However since the newer
	// version will MOVE the files once done, this is
	// somewhat defunct... 
	private boolean newFilesToProcess1() {
	    boolean timeToProcess = false;
	    
		//My key for new PDS products is the existence of construction record files.
        FilenameFilter pdsFilter = new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
            	// by time and file type
                //return ( (dir.lastModified() > startDate.getTime()) && name.endsWith("0.PDS"));
            	return name.endsWith("0.PDS");
            }
        };
        
        // Get all construction records in the pds directory.
        File pdsDirectory = new File(passDirName);
        
 
        File[] pdsFiles = pdsDirectory.listFiles(pdsFilter);
        
        // now update the start time to oldest file time
        for (int i = 0; i < pdsFiles.length; i++) { 
        	if (pdsFiles[i].lastModified() > startDate.getTime()) {
        		startDate.setTime(pdsFiles[i].lastModified());
        		timeToProcess = true;
        	}
        }
            
        return (timeToProcess);
	}
	
	// this is a slight varient of above
	// processing starts when any PDS files
	// are present.  This is meant to be used
	// in conjunction with MOVING the files once
	// done...
	private boolean newFilesToProcess2() {
	   
	    
		//My key for new PDS products is the existence of construction record files.
        FilenameFilter pdsFilter = new FilenameFilter() {
            public boolean accept(File dir, String name)
            {
            	// by time and file type
                //return ( (dir.lastModified() > startDate.getTime()) && name.endsWith("0.PDS"));
            	return name.endsWith("0.PDS");
            }
        };
        
        // Get all construction records in the pds directory.
        File pdsDirectory = new File(passDirName);
        
        // the returns the construction records OLDER
        // than the stored startTime
        //
        File[] pdsFiles = pdsDirectory.listFiles(pdsFilter);
        
        // now update the start time to oldest file time
       if (pdsFiles.length > 0) return true;
       
       return false;
	}
	public void printDebug(String sout) {
		if (debug)
			System.out.println(sout);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String passDirName = new String();
		String moveDirName = new String();
		boolean passCreation = false;
		boolean debug = false;
		
		try {
			// process the arguments
			int i = 0;
			while (i < args.length) {
				if (args[i].equals("-pdsDir")) {
					++i;
					if (i < args.length) {
						passDirName = args[i];
						++i;
					} else throw (new Exception());
				} else if (args[i].equals("-debug")) {
						debug = true;
						++i;
				} else if (args[i].equals("-createPassesOn")) {
						passCreation = true;
						++i;
				} else if (args[i].equals("-moveDir")) {
					++i;
					if (i < args.length) {
						moveDirName = args[i];
						++i;
					} else throw (new Exception());
				} else {
					throw (new Exception());
				}
			}
						
			// make one
			PdsPassCreateRun passRunner = new PdsPassCreateRun();
			
			// settings...
			if (!passDirName.equals(""))
				passRunner.passDirName = passDirName;
			else 
				throw (new Exception());
			
			if (!moveDirName.equals(""))
				passRunner.moveDirName = moveDirName;
			
			passRunner.debug = debug;
			passRunner.passCreation = passCreation;
			
			// make it go
			passRunner.start();
			
		} catch(Exception e) {
			System.out.println("PdsPassCreate: usage -- -pdsDir <dir> -moveDir <dir> -createPassesOn (really create passes) -debug");
		}
		
	}

}
