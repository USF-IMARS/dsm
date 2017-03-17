/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
 */
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;

import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.ProductType;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.nsls.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.*;



/**
 * Analyze  level 0 PDS files, extract and organize
 * start/stop pass times.  Create pass entry for each
 * pass extracted at the DSM.  Then copy the files to 
 * the PDS move directory.
 */
public class PdsPassCreate implements MoverPassCreate {


	// Crec is a construction record
	private class Crec {
		Date startTime;
		Date stopTime;
		String fileName;
		String spacecraft;
		ProductType productType;
		int packetCount = 0;
		int gapCount = 0;
		int missingPacketCount = 0;
	}


	// PassTime is simply a pass start/stop time
	private class PassTime {
		Date startTime;
		Date stopTime;
	}

	// PassDate is a kind of Date, it adds
	// a 'type' field which is used in
	// finding passes start/stop times
	private class PassDate extends Date {
		char type;
		private static final long serialVersionUID = 0; // make warnings go away
		public PassDate(Date date, char t) {
			super(); 

			long foobar = date.getTime();

			this.setTime(foobar);
			type = t;
		}

		public PassDate(PassDate date) {
			super(); 

			long foobar = date.getTime();

			this.setTime(foobar);
			type = date.type;
		}

		public PassDate() {
			super(); 

			this.setTime(0);
			type = 'x';
		}
	}

	private LinkedList<Crec> crecList;
	private TreeSet<String> spacecraftCount;
	private HashMap<String, LinkedList<Crec>> spaceCraftHashList;
	private HashMap<String, LinkedList<PassDate>> passDateHashList;
	private HashMap<String, LinkedList<PassTime>> passHashList;
	private List<Pass> prePassList;
	private List<Pass> theFinalPassList; // really! 
	private boolean debug = false;
	private Log log;
	private File pdsDirectory;

	public PdsPassCreate(File directory, Log logger) throws MoverException {

		//The log class is my hookup to the NSLS logging system.
		try {
			log = logger;
			log.setDefaultSource(new gov.nasa.gsfc.nisgs.nsls.NSLS.DSM("PdsMover/PdsPassCreate"));
		} catch (Exception e1) {
			throw new MoverException(e1);
		}
		log.info("PdsPassCreate: processing started.");

		crecList = new LinkedList<Crec>();

		// Get all construction records in the pds directory.

		pdsDirectory = directory;
		File[] pdsCrecFiles = pdsDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				return name.endsWith("0.PDS");
			}
		});

		if (pdsCrecFiles.length <= 0) {
			debugPrint("Nothing to do...");
			return;
		}

		log.info("reading " + Integer.toString(pdsCrecFiles.length) + " pds files...");
		for (int i=0;  i < pdsCrecFiles.length; i++) {
			try {
				Crec crec = readConstructionRecord(pdsCrecFiles[i]);
				crecList.add(crec);
				log.info("Construction Record Read.");
			} catch (Exception e) {
				log.info("Cannot read construction record for pds.");
				throw new MoverException(e);
			}
		}

		//createFakeCrecEntry();

		// sort the crec list by spacecraft name
		Collections.sort(crecList, new Comparator<Crec> () {
			public int compare(Crec c1, Crec c2) {
				return(c1.spacecraft.compareTo(c2.spacecraft));
			}
		});  

		// i suppose this is a heavy-handed approach
		// but TreeSet throws out duplicates.  So...
		// insert all the spacecraft names into it, 
		// and resulting size() should be our space-
		// craft count. Then use the tree to sort
		// and find the passes of each spacecraft.
		spacecraftCount = new TreeSet<String>();
		for (Crec crec : crecList) {
			spacecraftCount.add(crec.spacecraft);
		}

		// use the spacecrafts as a KEY in hashMap
		// each key holds a linked list of the various
		// bookkeeping classes...
		spaceCraftHashList = new HashMap<String, LinkedList<Crec>>();
		for (String key : spacecraftCount ) {
			LinkedList<Crec> spaceCraftList = new LinkedList<Crec>();

			for (Crec crec : crecList) {
				if (crec.spacecraft.equals(key)) {
					spaceCraftList.add(crec);
				}
			}
			if (spaceCraftList.size() > 0) {
				spaceCraftHashList.put(key, spaceCraftList);
			}
		}
		System.out.print("Spacecraft hash list -- ");
		System.out.println(spaceCraftHashList.size());

		// step at a time - turn the crecs into PassDates
		passDateHashList = new HashMap<String, LinkedList<PassDate>>();
		LinkedList<Crec> tmpCrecList ;

		for (String key : spacecraftCount ) {

			tmpCrecList = spaceCraftHashList.get(key);

			if (tmpCrecList.size() > 0) {
				// -walk through the Crec tmp list
				// -grab the start, stop time and put
				// them in the passdate list
				// -finally put the whole passdate list
				// into its own hash table using key 
				LinkedList<PassDate> pdList = new LinkedList<PassDate>();
				createPassDateList(tmpCrecList, pdList);

				passDateHashList.put(key, pdList);
			}
		}


		// while we could have sorted the passDates
		// above, instead, make a new list that's 
		// sorted.  this is principally for debug 
		// purposes, all the steps are in the various
		// lists...
		passHashList = new HashMap<String, LinkedList<PassTime>>();
		LinkedList<PassDate> spdList;
		// And now we must descend into manual iterator construction
		// merely because we wish to call remove()
		for(Iterator i = spacecraftCount.iterator(); i.hasNext();) {
			String key = (String) i.next();
			spdList = passDateHashList.get(key);

			if (spdList.size() > 0) {
				LinkedList<PassTime> pList = new LinkedList<PassTime>();
				sortPassDates(spdList, pList);
				passHashList.put(key, pList);
			}
			else {
				// What has happened here is we have missing information
				// for this pass.  We can get this for aqua passes that
				// have missing gbad files.  The proper thing to do
				// here would be to throw away the files now.  At the
				// very least we should remove the key from the list.
				i.remove();
				log.warning("Missing information for pass of spacecraft "
						+ key
						+ " : pass ignored");
			}
		}
		//prePassList --- not initialized?
		theFinalPassList = new LinkedList<Pass>();

	}

	// add a construction record start/stop time
	// entry into our list.  If the start time is
	// not before the stopTime (or the same time),
	// the entry is not created and output error
	// is generated - a minor sanity check
	private void createPassDateList( LinkedList<Crec> cl, LinkedList<PassDate> pdl) {

		for (Crec crec : cl ) {	  

			int compareValue = crec.startTime.compareTo(crec.stopTime);

			if (compareValue <= 0) {
				PassDate ps = new PassDate(crec.startTime, 's');
				PassDate pe = new PassDate(crec.stopTime, 'e');
				pdl.add(ps);
				pdl.add(pe);
			} else {
				debugPrint("PdsPassCreate: add() -- attempting to add malformed date pair from crec.");
				debugPrint(crec.startTime.toString());
				debugPrint(crec.stopTime.toString());
			}
		}
	}



	// given a list of date pairs added to this
	// list, create the passes list.
	// The entire algorithm depends on their being
	// start/stop pairs -- overlap will result in
	// some new inner/out tag pair.
	// For example -- 's' = start, 'e' - end
	// Non-overlap sequence - sesesese
	// overlap sequence - ssesee, the two middle 'se's 
	// are overlapped by the outer se which forms the
	// pass time
	// if this isn't correct - this whole thing breaks!
	// KR
	private void sortPassDates(LinkedList<PassDate> pdl, LinkedList<PassTime> pl) {

		// sort by time...
		Collections.sort(pdl, new Comparator<PassDate> () {
			public int compare(PassDate d1, PassDate d2) {
				return(d1.compareTo(d2));
			}
		});

		// non-recursive way to find outer tag pairs
		int length = pdl.size();	  
		int passStart = 0;
		int pdIndexSave = -1; 
		PassDate pd;
		boolean passFound = false;

		for (int i = 0; i < length; i++) {

			pd = pdl.get(i);

			if (pd.type == 's') {
				++passStart;
				if (passStart == 1)
					pdIndexSave = i;
			}

			if (pd.type == 'e') { 
				--passStart;
				if (passStart == 0)
					passFound = true;

				// malformed?
				if (passStart < 0) { 
					passStart = 0; 
					continue; // UGLY!
				}
			}

			if (passFound) {

				PassTime p = new PassTime();
				PassDate spd = pdl.get(pdIndexSave);

				p.startTime = new Date();
				p.stopTime = new Date();

				p.startTime.setTime(spd.getTime());
				p.stopTime.setTime(pd.getTime());
				pl.add(p);


				passStart = 0;
				pdIndexSave = -1;
				passFound = false;
			}
		}
	}

	// compare pass time to a list of passes
	// if the pass time overlaps any of the times
	// in the list, return 1, otherwise return 0
	private int comparePassTimes(PassTime pt, LinkedList<Pass> pList) {


		for (Pass p : pList ) {

			// this just says- if (start is between AOS & LOS 
			//                             or 
			//                     stop is between AOS & LOS)
			//                 then it does overlap
			//

			boolean startInside = (pt.startTime.compareTo(p.getAos()) >= 0) && (pt.startTime.compareTo(p.getLos()) <= 0);
			boolean stopInside = (pt.stopTime.compareTo(p.getAos()) >= 0) && (pt.stopTime.compareTo(p.getLos()) <= 0);

			if ( startInside || stopInside )
				return 1;
		}
		return 0;
	}

	public void printCrecs() {
		// print them out


		for (Crec crec : crecList ) {			

			System.out.print("PDS Crec File: ");
			System.out.println(crec.fileName);

			System.out.print("Spacecraft: ");
			System.out.print(crec.spacecraft);

			System.out.print(" -- Time: Start - ");
			System.out.print(crec.startTime.toString());
			System.out.print("  Stop - ");
			System.out.println(crec.stopTime.toString());

		}		  
	}

	public void printPassDates() {

		LinkedList<PassDate> pdList;



		for (String key : spacecraftCount ) {

			System.out.print("Spacecraft: ");
			System.out.println(key);

			pdList = passDateHashList.get(key);

			for (PassDate pd : pdList ) {
				System.out.print("Time: Sorted - ");

				System.out.print(pd.toString());
				System.out.print(" ");

				System.out.println(pd.type);

			} 
		}
	}
	public void printPasses() {

		LinkedList<PassTime> pList;

		for (String key : spacecraftCount ) {

			System.out.print("Spacecraft: ");
			System.out.println(key);

			pList = passHashList.get(key);

			for (PassTime p : pList ) {  

				System.out.print("Pass found: <<<");
				System.out.print(p.startTime.toString());
				System.out.print("  ");

				System.out.print(p.stopTime.toString());
				System.out.println(">>>");
			}
		}		  
	}

	private void deleteSlivers(LinkedList<PassTime> slivers, LinkedList<Crec> crecs) 
	{
		// Sanity check - does the SLIVERS directory exist?
		File sliverDir = new File(pdsDirectory,"/SLIVERS/");
		if(!sliverDir.isDirectory()) {
			if(!sliverDir.mkdirs()) {
				log.info("PdsPassCreate: trouble creating SLIVERS directory");
			}
		}
		log.info("Sliver dir: " + sliverDir.getAbsolutePath());

		for (PassTime pt : slivers ) { 

			for (Crec c: crecs) {
				// the crec start time *must* be the same
				// time as the PT start time time, or after it...
				//   -- AND --
				// the crec stop time must be the same time
				// as the PT stop time, or before it
				if ((c.startTime.compareTo(pt.startTime) >= 0) &&
						(c.stopTime.compareTo(pt.stopTime) <= 0)) {
					// delete this one
					File sliverCR = new File(pdsDirectory, c.fileName);
					File sliverData = new File(pdsDirectory, c.fileName.substring(0,35) + "1.PDS");
					boolean status1, status2;


					File sliverCRdest = new File(sliverDir, sliverCR.getName());
					File sliverDatadest = new File(sliverDir, sliverData.getName());

					log.info("SliverCR Dest= " + sliverCRdest.getAbsolutePath());
					log.info("SliverDatadest Dest= " + sliverCRdest.getAbsolutePath());

					status1 = sliverCR.renameTo(sliverCRdest);
					status2 = sliverData.renameTo(sliverDatadest);

					if (status1 == false || status2 == false) {
						log.info("PdsPassCreate: failed to move file(s) to SLIVERS directory.");
					}

					log.info("PdsPassCreate: deleting sliver pass, from -- " +
							c.startTime.toString() + " to " +
							c.stopTime.toString());
					sliverCR.delete();
					sliverData.delete();
					log.info("PdsPassCreate: Sliver CR file deleted -- " + sliverCR.getName());
					log.info("PdsPassCreate: Sliver Data file deleted -- " + sliverData.getName());
				}
			}
		}
	}

	// find any short passes, partial passes and delete
	// them
	public void deleteSliverPasses(long seconds) throws Exception
	{

		LinkedList<PassTime> ptList;
		HashMap<String, LinkedList<PassTime>> sliverHash = new HashMap<String, LinkedList<PassTime>>();


		HashMap<String, LinkedList<PassTime>> passOkHash = new HashMap<String, LinkedList<PassTime>>();

		for (String key : spacecraftCount ) {

			ptList = passHashList.get(key);
			if(ptList == null)
				throw new Exception("passHashList('" + key +"') is null!");

			LinkedList<PassTime> sliverList = new LinkedList<PassTime>();
			LinkedList<PassTime> passOkList = new LinkedList<PassTime>();

			for (PassTime pt : ptList ) { 

				long startTimeIn_ms = pt.startTime.getTime();
				long stopTimeIn_ms = pt.stopTime.getTime();
				long timeDelta = 0;

				// seems unlikely but check for time wrap
				if (startTimeIn_ms <= stopTimeIn_ms) {
					timeDelta = stopTimeIn_ms - startTimeIn_ms;
				} else {
					timeDelta = startTimeIn_ms - stopTimeIn_ms; 
				}

				// make a slivers list of tiny passes
				// and make a good/ok list of passes
				// that are outside the time limit
				if (timeDelta <= (1000 * seconds)) {
					sliverList.add(pt);
				} else {
					passOkList.add(pt);
				}

			}

			if (sliverList.size() > 0)
				sliverHash.put(key, sliverList);
			if (passOkList.size() > 0) {
				passOkHash.put(key, passOkList);
			}
		}

		// print sliver list here...

		//
		// if any slivers exist... walk the crec list
		// find any crecs that are ***contained***
		// within a sliver.  DELETE IT
		//

		// now actually do the work
		// two things:
		// -- delete slivers if there are any
		// -- save the good passes only

		for (String key : spacecraftCount ) {
			LinkedList<PassTime> sliverList2;
			LinkedList<Crec> crecs;
			sliverList2 = sliverHash.get(key);
			crecs = spaceCraftHashList.get(key);

			if (sliverList2 != null) {
				deleteSlivers(sliverList2, crecs);
			}

			LinkedList<PassTime> passOkList2;
			LinkedList<PassTime> originalPassList;
			passOkList2 = passOkHash.get(key);
			if (passOkList2 != null) {
				originalPassList = passHashList.remove(key);
				passHashList.put(key, passOkList2);
				if (originalPassList == null) {
					throw new Exception();
				}
				log.info("PdsPassCreate: replaced original pass list: passes <" + originalPassList.size() + ">");
				log.info("PdsPassCreate: with new pass list: passes <" + passOkList2.size() + ">");

			}
		}	

		// now count up the carnage!!! 
		checkForShortCrecs(seconds);
	}

	private void checkForShortCrecs(long seconds) throws Exception {
		crecList = new LinkedList<Crec>();

		// Get all construction records in the pds directory.

		File[] pdsCrecFiles = pdsDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
				return name.endsWith("0.PDS");
			}
		});

		if (pdsCrecFiles.length <= 0) {

			return;
		}
		for (int i=0;  i < pdsCrecFiles.length; i++) {


			Crec crec = readConstructionRecord(pdsCrecFiles[i]);

			// pass time check
			long start_ms = crec.startTime.getTime();
			long stop_ms = crec.stopTime.getTime();
			long timeDelta = 0;
			if (start_ms <= stop_ms) {
				timeDelta = stop_ms - start_ms; // as it should be
			} else {
				timeDelta = start_ms - stop_ms;
			}
			if (timeDelta < (1000 * seconds)) {  

				log.info("PdsPassCreate: found short PDS, AFTER sliver processing -- " +
						pdsCrecFiles[i] + " : " +
						crec.startTime.toString() + " to " +
						crec.stopTime.toString());
			} 

		} 
	}

	/** this routine builds the real Pass info.
	 * NOTE: A log message in this method emits a string that the processing
	 * monitor recognizes to detect the start of a pass:
	 * "PdsPassCreate: pass already exists".
	 */
	public void createPassesForDSM(int sliverSeconds) throws MoverException {
		//Any DSM setup exceptions are fatal and rethrown. Exceptions in the try/catch
		//block will just abort the current session, and we will try again in the next
		//cycle.
		DSMAdministrator dsm;
		log.info("createPassesForDSM");
		try {
			dsm = new DSMAdministrator("PdsPassCreate","PdsPassCreate");
		} catch (Exception e) {
			throw new MoverException(e);
		}
		try {
			prePassList = dsm.getPasses();
		} catch (Exception e) {

			throw new MoverException(e);
		}


		// this should look familiar and suggests a more
		// general solution.  To future implementor, 
		// it seem like the pre-existing pass List could be
		// merged into the "new pass" list, and sorted it
		// and new passes generated from that.  Then if any
		// overlap, that is they were formed from the old pass
		// information and the new -- they could be deleted.
		// Here however is something simpler and far less
		// efficient because each new pass time must now be
		// checked against the pre-existing pass list.
		// Basically, the pre-existing
		// passes need to be checked before creating a new
		// pass.  So retrieve the pre-existing passes, and
		// then sort them by spaceCraft name -- this is similar
		// to the above alg in passCreate. 
		// Then before creating the new pass from our list,
		// check the pre-existing pass list to see if its
		// already at the DSM

		// use the spacecrafts as a KEY in hashMap
		// each key holds a linked list of the various
		// bookkeeping classes...
		HashMap<String, LinkedList<Pass>> preexistingPassHashList = new HashMap<String, LinkedList<Pass>>();

		for (String key : spacecraftCount ) {
			LinkedList<Pass> spaceCraftList = new LinkedList<Pass>();

			for (Pass ppass : prePassList){
				if (ppass.getSpacecraft().equals(key)) {
					spaceCraftList.add(ppass);
				}
			}

			if (spaceCraftList.size() > 0) {
				preexistingPassHashList.put(key, spaceCraftList);
			}
		}

		//OK, the result should be a list of pre-existing
		// passes sorted by spacecraft name
		try {
			LinkedList<PassTime> ptList;
			LinkedList<Pass> preList;

			for (String key : spacecraftCount ) {

				ptList = passHashList.get(key);
				preList = preexistingPassHashList.get(key);

				for (PassTime pt : ptList ) {  

					// rather ineffient!
					// basically if the PT-time does not overlap
					// any times in the pre-existing pass list,
					// create a new pass!
					// if preList is empty, everything goes in
					if (preList != null) {
						if (comparePassTimes(pt,preList) <= 0) {
							Pass pass = new Pass("gsfc-drolab", key, pt.startTime, pt.stopTime);

							theFinalPassList.add(pass);
						} else {
							log.info("PdsPassCreate: pass already exists in database: <<"
									+ 	key
									+ 	">>"
									+ 	" AOS: "
									+ 	pt.startTime.toString()
									+ 	"  LOS: "
									+ 	pt.stopTime.toString());
						}
					} else {
						Pass pass = new Pass("gsfc-drolab", key, pt.startTime, pt.stopTime);
						theFinalPassList.add(pass);
					}
				}

			}

		} catch (Exception ex) {


			debugPrint("PdsPassCreate: sendPassesToDSM failed." + ex);
			ex.printStackTrace();
			throw new MoverException(ex);

		} finally {

			try {
				dsm.dispose();
			} catch (Exception e) {

				throw new MoverException(e);
			}
		}
	}

	// this sends them...
	public void sendPassesToDSM() throws MoverException {

		//Any DSM setup exceptions are fatal and rethrown. Exceptions in the try/catch
		//block will just abort the current session, and we will try again in the next
		//cycle.
		DSMAdministrator dsm;
		try {
			dsm = new DSMAdministrator("PdsPassCreate","PdsPassCreate");
		} catch (Exception e) {
			throw new MoverException(e);
		}

		try {

			for (Pass p : theFinalPassList) {
				dsm.createPass(p);
			}

		} catch (Exception ex) {

			debugPrint("PdsPassCreate: sendPassesToDSM failed.");
			ex.printStackTrace();
			throw new MoverException(ex);

		} finally {

			try {
				dsm.dispose();
			} catch (Exception e) {
				throw new MoverException(e);
			}
		}
	}

	public void printPrePassList() {
		// debug


		for (Pass pr : prePassList) {

			System.out.print("Pre-existing pass -- ");
			System.out.print("<<");
			System.out.print(pr.getSpacecraft());
			System.out.print(">>");
			System.out.print(" aos: ");
			System.out.print(pr.getAos().toString());
			System.out.print(" los: ");
			System.out.println(pr.getLos().toString());

		}		  
	}

	public void printFinalPassList() {


		for (Pass p : theFinalPassList) {

			System.out.print("PdsPassCreate: -- new pass -- <<");
			System.out.print(p.getSpacecraft());
			System.out.print(">>");
			System.out.print(" AOS: ");
			System.out.print(p.getAos().toString());
			System.out.print("  LOS: ");
			System.out.println(p.getLos().toString());

		}
		log.info("PdsPassCreate: processing complete.");
	}
	/**
	 * NOTE: A log message in this method emits a string that the processing
	 * monitor recognizes to detect the start of a pass:
	 * "PdsPassCreate: New pass --".
	 */
	public void logPasses() {

		for (Pass p : theFinalPassList) {

			String s = 	"PdsPassCreate: New pass -- <<"
				+ 	p.getSpacecraft()
				+ 	">>"
				+ 	" AOS: "
				+ 	p.getAos().toString()
				+ 	"  LOS: "
				+ 	p.getLos().toString();
			log.info(s);
		}
	}

	// this is taken directly from PDS mover code...
	// except for the bit about spacecraft, which i stole
	// from some other part of pds code... KR
	private Crec readConstructionRecord(File fcrec) throws java.io.IOException
	{
		Crec crec = new Crec();
		crec.fileName = fcrec.getName();

		try (
				FileInputStream fis = new FileInputStream(fcrec);
				BufferedInputStream bis = new BufferedInputStream(fis);
				DataInputStream di = new DataInputStream(bis)
			){
			GregorianCalendar c0 = new GregorianCalendar(1958,Calendar.JANUARY,1);
			c0.setTimeZone(TimeZone.getTimeZone("GMT+0"));

			crec.spacecraft = (crec.fileName.startsWith("P154"))? "AQUA" : "TERRA";

			di.skipBytes(50);
			int scspairs = (int)di.readShort();
			byte[] scs = new byte[16];
			for (int n = 0; n < scspairs; n++)
			{
				di.read(scs);
			}
			di.skipBytes(12);

			short days0 = di.readShort();
			long msday0 = (long)di.readInt();
			@SuppressWarnings("unused") short micro0 = di.readShort();
			long x0 = days0 * 86400000L + msday0;
			long t0 = c0.getTimeInMillis() + x0;
			crec.startTime = new Date(t0);

			short days1 = di.readShort();
			long msday1 = (long)di.readInt();
			@SuppressWarnings("unused") short micro1 = di.readShort();
			long x1 = days1 * 86400000L + msday1;
			long t1 = c0.getTimeInMillis() + x1;
			crec.stopTime = new Date(t1);

			/**
	     //long esh0,esh1 16
	     //int rscorrpkts 4
	     //int packets    4
	     //long pktbytes  8
	     //int gaps       4
	     //long comple    8
	     //int skip       4
	     //int appids     4
	     //int apidspid   4
	     //long offset    8
	     //int vcids      4
	     //int vcidspid   4
	     //int pgaps      4

	     //for each pgap
	     //int gseq       4
	     //long goffset   8
	     //int missing    4
	     //long t0        8
	     //long t1        8
	     //long pre       8
	     //long post      8
			 */
			di.skipBytes(20);
			crec.packetCount = di.readInt();
			di.skipBytes(8);
			crec.gapCount = di.readInt();
			di.skipBytes(36);
			int pgaps = di.readInt();
			crec.missingPacketCount = 0;
			for (int n = 0; n < pgaps; n++)
			{
				di.skipBytes(12);
				int hole = di.readInt();
				crec.missingPacketCount += hole;
				di.skipBytes(32);
			}
		}

		return crec;
	}

	//testing only
	@SuppressWarnings("unused")
	private void createFakeCrecEntry() {
		// add a fake entry for testing...
		Date startTime = new Date();
		Date stopTime = new Date();

		Date orig;

		orig = crecList.get(0).startTime;
		startTime.setTime(orig.getTime() + 1000);

		orig = crecList.get(0).stopTime;
		stopTime.setTime(orig.getTime() + 1000);

		Crec c = new Crec();
		c.startTime = startTime;
		c.stopTime = stopTime;
		c.fileName = new String("P154");
		c.spacecraft = new String("AQUA");

		crecList.add(c);

		System.out.println(startTime.getTime());
		System.out.println(stopTime.getTime());	
	}

	private void debugPrint(String s) {
		if (debug)
			System.out.println(s);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			DsmProperties dsmp = new DsmProperties();
			String logHost = dsmp.getNslsHost();
			int logPort = dsmp.getNslsPort();
			String logDir = dsmp.getNslsDirectory();
			Log log = new Log(logHost,logPort,logDir);
			log.setDefaultSource(new gov.nasa.gsfc.nisgs.nsls.NSLS.DSM("PdsPassCreateMain"));
			dsmp.dispose();

			@SuppressWarnings("unused") PdsPassCreate passes = new PdsPassCreate(
					new File(args[0]),
					log
			);



		} catch( Exception e ) {

			e.printStackTrace();

		}
	}

}
