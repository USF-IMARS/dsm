/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
 */
package gov.nasa.gsfc.nisgs.dsm.agent;

import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.FileMover;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.dsm.ProductFactory;
import gov.nasa.gsfc.nisgs.dsm.agent.rdr.NPOESSFilename;
import gov.nasa.gsfc.nisgs.dsm.Product;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.sql.Connection;

import ncsa.hdf.hdf5lib.*;
import ncsa.hdf.hdf5lib.H5;

/**
 * Analyze level 0 SDR files, extract and organize start/stop pass times. Create
 * pass entry for each pass extracted at the DSM. Then copy the files to the SDR
 * move directory.
 */
public class SDRPassCreate implements MoverPassCreate {
	public final static String MYNAME="SDR";
	
	private Map<String, List<RDRrec>> recordsBySpacecraft;
	private Map<String, List<PassTime>> passTimesBySpacecraft;

	private List<Pass> theFinalPassList = new LinkedList<Pass>();

	private DsmLog log;
    private DSMAdministrator dsm;

	private Comparator<RDRrec> recTimeSort = new RecTimeSorter();

	private final static String stationName = "gsfc-drolab";

	private String moverName; // name of instance of Mover for this datafile/construction record such as SDRMover, PDSMover, etc...

	public SDRPassCreate(String moverName, File[] passFile) throws MoverException {

		this.moverName = moverName;
		
		// The log goes to the NSLS logging system.
		try {
			log = new DsmLog(moverName + "/SDRPassCreate");
		} catch (Exception e1) {
			throw new MoverException(e1);
		}
		log.report(moverName + " SDRPassCreate: processing started.");


		// Get all construction records in the SDR directory.

		File[] SDRRDRrecFiles = passFile;

		if (SDRRDRrecFiles.length <= 0) {
			log.report(moverName + " SDRPassCreate: no SDR files found to process...");
			return;
		}
		
		// put the rdr records into a hash list by spacecraft name
		recordsBySpacecraft = new HashMap<String,List<RDRrec>>();
		for (int i = 0; i < SDRRDRrecFiles.length; i++) {

			RDRrec rdrRec = null;
			try {
				rdrRec = readRecord(SDRRDRrecFiles[i]);
			}
			catch (Exception e)
			{
				continue;
			}
			
			if (recordsBySpacecraft.containsKey(rdrRec.getSpacecraft()) == false) {
				recordsBySpacecraft.put(rdrRec.getSpacecraft(), new LinkedList<RDRrec>());
			}
			recordsBySpacecraft.get(rdrRec.getSpacecraft()).add(rdrRec);

		}

		// Get a DSM connection for the life of this object
		try {
			dsm = new DSMAdministrator("SDRPassCreate","SDRPassCreate");
		}
		catch (Exception e)
		{
			throw new MoverException(e);
		}

		// sort the records by start time by spacecraft
		Set<String> spacecrafts = recordsBySpacecraft.keySet();
		for (String spacecraft : spacecrafts) {
			Collections.sort(recordsBySpacecraft.get(spacecraft), recTimeSort);
		}
		
		// In theory for each spacecraft the starting time of the first rec and stopping time
		// for the last record would be the pass time overall per spacecraft.
		// However this does not take into account pass data that may be placed into the incoming
		// directory from the same spacecraft during a pass for a different
		// time period.   This would result in all the records found by spacecraft being placed together, even though
		// they are different passes in wall clock time.   For example suppose a 6am pass is processed
		// and then a 3pm pass on the same day is arriving.  For some reason the 6am pass files are placed
		// into the incoming data directory as the 3pm arrives.   Well simply sorting the resulting records
		// by time will result in a computed pass time of 6am to 3pm -- which is not correct.
		// There isn't a good way to differentiate the records files.  The orbit field in the SDR filename
		// could be used but at this point RT-STPS does nothing with the field.
		
		

		// group by time, within X minutes should be part of the same sat. pass
		// this handles the case of someone dumping an old pass into the directory
		// while a new pass is being processed and having them being computed as
		// being from the same pass
		passTimesBySpacecraft = new HashMap<String, List<PassTime>>();
	
		for (String spacecraft : spacecrafts) {
			recordsBySpacecraft.get(spacecraft);
			
			// morph the grouped recs into PassTimes
			passTimesBySpacecraft.put(spacecraft, calculatePassTimes(recordsBySpacecraft.get(spacecraft)));

		}
	

	}

	/**
	 * Groups records by time, about 15 min apart are considered part of the same pass
	 * @param list
	 * @return
	 */
	private List<PassTime> calculatePassTimes(List<RDRrec> rdrRecs) {
	    // Doing MUCH simpler stuff for NPP - one SDR == one pass
	    // (blithely ignoring possibility of overlapping times
	    List<PassTime> pts = new LinkedList<PassTime>();
	    Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();

	    for (RDRrec rr : rdrRecs) {
		Date startDateTime = rr.getStartTime();
		Date stopTime = rr.getStopTime();
		
		// take the stopTime which just has time and fix up the year missing items
		Calendar startDateTimeCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		startDateTimeCal.setTime(startDateTime);
		Calendar stopDateTimeCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		stopDateTimeCal.setTime(stopTime);
		
		stopDateTimeCal.set(Calendar.YEAR, startDateTimeCal.get(Calendar.YEAR));
		stopDateTimeCal.set(Calendar.DAY_OF_YEAR, startDateTimeCal.get(Calendar.DAY_OF_YEAR));
		
		stopTime = stopDateTimeCal.getTime();

		PassTime pt = new PassTime(rr, startDateTime, stopTime);
		pts.add(pt);
	    }
	    return pts;
	}

	
	public void createPassesForDSM(int sliverSeconds) throws MoverException {
		// create passes for the spacecraft from the DSM if any
		// If not passes from the DSM match the passTimes
		// Then use the passTimes to create new passes in the DSM
		createPassesFromDSM();

		// delete any passes that are really short slivers
		deleteSliverPasses(sliverSeconds);
		
	}



    /**
     * For each of our input PassTime objects,
     * see if there is one Pass in the database that will hold it.
     * If not, check theFinalPassList.
     * If not there either,
     * create an appropriate Pass and add it to theFinalPassList
     * (to be written to the database later).
     * NOTE: A log message in this method emits a string that the processing
     * monitor recognizes to detect the start of a pass:
     * "SDRPassCreate: pass already exists".
     */
    private void createPassesFromDSM() throws MoverException {
	
	// Compare the pass times calculated by spacecraft to the dsm passes by spacecraft
	// if the DSM does not hold the pass, then create a new pass and put that in the final 
	// pass list...
	Set<String> spacecrafts = recordsBySpacecraft.keySet();
	for (String spacecraft : spacecrafts) {
	    
	    //List<Pass> passes = null;
	    List<Pass> passes = new LinkedList<Pass>();
	    //log.report("spacecraft is " + spacecraft);
	    //log.report("length of passes is: " + passes.size());
	    List<PassTime> passTimes = passTimesBySpacecraft.get(spacecraft);
	    for (PassTime passTime : passTimes) {
		// Is there a decent matching pass in the database now?
		Pass oldPass;
		try {
		    oldPass = dsm.findPass(spacecraft, passTime.getStartTime(), passTime.getStopTime());
		}
		catch (Exception e) {
		    throw new MoverException(e);
		}
		if(oldPass == null) {
		    // Not in the database; in theFinalPassList?
		    oldPass = passTimeTouchesPasses(passTime, theFinalPassList);
				
		    if (oldPass == null) {
			
			Pass newPass = new Pass(stationName, spacecraft, passTime.getStartTime(), passTime.getStopTime());
			
			theFinalPassList.add(newPass);
			
			log.report(moverName + " SDRPassCreate: creating new pass: <<"
				   + spacecraft
				   + ">>"
				   + " AOS: "
				   + passTime.getStartTime().toString()
				   + "  LOS: "
				   + passTime.getStopTime().toString());
		    }
		}
	    }
	}
    }

	private void deleteSliverPasses(int sliverSeconds) {
		// run through the collected final pass list
		// if any passes are smaller than sliverSeconds
		// remove them from the final pass list
		for (int i = 0; i < theFinalPassList.size(); i++) {
			
			Pass p = theFinalPassList.get(i);
		
			if (isSliver(p, sliverSeconds)) {
				
				theFinalPassList.remove(i);
				
				log.report(moverName + " SDRPassCreate: sliver processing removing pass: <<"
						+ p.getSpacecraft()
						+ ">>"
						+ " AOS: "
						+ p.getAos()
						+ "  LOS: "
						+ p.getLos());
			}
		}
		
		
	}
	




	// this sends them...
	public void sendPassesToDSM() throws MoverException {

		// Any DSM setup exceptions are fatal and rethrown. Exceptions in the
		// try/catch
		// block will just abort the current session, and we will try again in
		// the next
		// cycle.
		try {

			for (Pass p : theFinalPassList) {
				dsm.createPass(p);
			}

		} catch (Exception ex) {

			log.report(moverName + " SDRPassCreate: sendPassesToDSM failed");
			throw new MoverException(ex);

		} finally {

			try {
				dsm.dispose();
			} catch (Exception e) {

				throw new MoverException(e);
			}
		}
	}



    /**
     * NOTE: A log message in this method emits a string that the processing
     * monitor recognizes to detect the start of a pass:
     * "SDRPassCreate: New pass --".
     */
	public void logPasses() {

		for (Pass p : theFinalPassList) {

			String s = moverName + " SDRPassCreate: New pass -- id: "
			+ p.getId()
			+ " <<" + p.getSpacecraft()
			+ ">>" + " AOS: " + p.getAos().toString() + "  LOS: "
			+ p.getLos().toString();
			log.report(s);
		}
		
	}

	// compare pass time to a list of passes
	// if the pass time overlaps any of the times
	// in the list, return a Pass that overlaps
        // If a Pass matches exactly, return that one, otherwise
        // return the first one that touches
	private Pass passTimeTouchesPasses(PassTime pt, List<Pass> pList) {

		if (pList == null) {
			// no passes at all, this must be fresh and empty database
			return null;
		}
		Pass result = null;
		for (Pass p : pList) {

			// this just says- if (start is between AOS & LOS
			// or
			// stop is between AOS & LOS)
			// then it does overlap
			//

			Date pStart = p.getAos();
			Date pStop = p.getLos();

			Date ptStart = pt.getStartTime();
			Date ptStop = pt.getStopTime();


			boolean startInside = (pt.getStartTime().compareTo(pStart) >= 0) && (pt.getStartTime().compareTo(pStop) <= 0);

			boolean stopInside = (pt.getStopTime().compareTo(pStart) >= 0) && (pt.getStopTime().compareTo(pStop) <= 0);


			if(startInside && stopInside) 
			{
				return p;
			}
		}
		if (result == null)
		{
		}
		return result;
	}
	
	// this is taken directly from SDR mover code...
	// except for the bit about spacecraft, which i stole
	// from some other part of SDR code... KR
	private RDRrec readRecord(File rdrFilename) throws MoverException {
		return new RDRrec( new NPOESSFilename(rdrFilename.getName(),rdrFilename.getAbsolutePath()) );
	}


	private boolean isSliver(PassTime passTime, int sliverSeconds) {
		long startTime = passTime.getStartTime().getTime();
		long stopTime = passTime.getStopTime().getTime();
		long delta = stopTime - startTime;
		long sliverL = sliverSeconds * 1000l;
		return (delta < sliverL);
	}
	private boolean isSliver(Pass pass, int sliverSeconds) {
		long startTime = pass.getAos().getTime();
		long stopTime = pass.getLos().getTime();
		long delta = stopTime - startTime;
		long sliverL = sliverSeconds * 1000l;
		return (delta < sliverL);
	}

	
	private class RecTimeSorter implements Comparator<RDRrec>{
		@Override
		public int compare(RDRrec o1, RDRrec o2) {
			return o1.getStartTime().compareTo(o2.getStartTime());
		}
	}
}
