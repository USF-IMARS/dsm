/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;

/**
 * SDRType is a class that packages the relationship between CDFCB file names
 * and DRL product types.  It holds a static array of objects and a few static
 * methods for looking stuff up.
 */
public class SDRType {

    String productType;
    String fileNameHead;
    
    SDRType(String productType, String fileNameHead) {
	this.productType = productType;
	this.fileNameHead = fileNameHead;
    }
    
    static SDRType sdrtype[] = {
	new SDRType("drl.npp.viirs.gimgo", "GIMGO"),
	new SDRType("drl.npp.viirs.gitco", "GITCO"),
	new SDRType("drl.npp.viirs.gmodo", "GMODO"),
	new SDRType("drl.npp.viirs.gmtco", "GMTCO"),
	new SDRType("drl.npp.viirs.gdnbo", "GDNBO"),
	new SDRType("drl.npp.viirs.gdtco", "GDTCO"),
	new SDRType("drl.npp.viirs.svdnb", "SVDNB"),
	new SDRType("drl.npp.viirs.svi01", "SVI01"),
	new SDRType("drl.npp.viirs.svi02", "SVI02"),
	new SDRType("drl.npp.viirs.svi03", "SVI03"),
	new SDRType("drl.npp.viirs.svi04", "SVI04"),
	new SDRType("drl.npp.viirs.svi05", "SVI05"),
	new SDRType("drl.npp.viirs.svm01", "SVM01"),
	new SDRType("drl.npp.viirs.svm02", "SVM02"),
	new SDRType("drl.npp.viirs.svm03", "SVM03"),
	new SDRType("drl.npp.viirs.svm04", "SVM04"),
	new SDRType("drl.npp.viirs.svm05", "SVM05"),
	new SDRType("drl.npp.viirs.svm06", "SVM06"),
	new SDRType("drl.npp.viirs.svm07", "SVM07"),
	new SDRType("drl.npp.viirs.svm08", "SVM08"),
	new SDRType("drl.npp.viirs.svm09", "SVM09"),
	new SDRType("drl.npp.viirs.svm10", "SVM10"),
	new SDRType("drl.npp.viirs.svm11", "SVM11"),
	new SDRType("drl.npp.viirs.svm12", "SVM12"),
	new SDRType("drl.npp.viirs.svm13", "SVM13"),
	new SDRType("drl.npp.viirs.svm14", "SVM14"),
	new SDRType("drl.npp.viirs.svm15", "SVM15"),
	new SDRType("drl.npp.viirs.svm16", "SVM16"),
	new SDRType("drl.npp.viirs.icdbg", "ICDBG"),
	new SDRType("drl.npp.viirs.ivcdb", "IVCDB"),
	new SDRType("drl.npp.viirs.ivobc", "IVOBC")
    };
    
    public static String findProductType(String fileName) {
	for (SDRType st : sdrtype) {
	    if(fileName.startsWith(st.fileNameHead))
		return st.productType;
	}
	return null;
    }

    public static String findFileNameHead(String type) {
	for (SDRType st : sdrtype) {
	    if(st.productType.equals(type))
		return st.fileNameHead;
	}
	return null;
    }

    public static boolean isSDRfile(String filename) {
	if(!filename.endsWith(".h5"))
	    return false;
	for (SDRType st : sdrtype) {
	    if(filename.startsWith(st.fileNameHead))
		return true;
	}
	return false;
    }
}
