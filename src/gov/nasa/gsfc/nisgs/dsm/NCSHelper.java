/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;

import gov.nasa.gsfc.nisgs.dsm.DSM;
import gov.nasa.gsfc.nisgs.dsm.Pass;
import gov.nasa.gsfc.nisgs.dsm.Product;
import java.util.List;
import java.util.ArrayList;

/**
 * This class serves as a scratchpad for NCS scripts.  It lets you store
 * a set of input Products, a set of ancillary files, and a set of output
 * Products.  Its primary purpose is
 * to shorten the NCS scripting necessary to create output products with
 * appropriate metadata, and to allow that creation to happen as one
 * atomic database transaction.
 *
 * For now we support the current normal case - all products descend from
 * a common pass, all outputs depend on all inputs.
 */

public class NCSHelper
{
    // Infrastructure slots
    DSM dsm;
    String algoName;
    String algoVersion;

    // Lists of stuff
    Pass pass;
    List<Product> inputs;
    List<Product> outputs;
    List<String> ancillaries;

    /**
     * Constructor setting up the DSM opbject, the name and version
     * of the algorithm.
     */
    public NCSHelper(DSM dsm, String algoName, String algoVersion)
    {
	this.dsm = dsm;
	this.algoName = algoName;
	this.algoVersion = algoVersion;
	inputs = new ArrayList<Product>();
	outputs = new ArrayList<Product>();
	ancillaries = new ArrayList<String>();
    }

    /**
     * Verbose toString, for debugging
     */

    public String toString()
    {
	String res = "{NCSHelper - Pass " + pass;

	res += "\nInputs:";
	for (Product ip : inputs)
	    res += "\n\t" + ip;

	res += "\nAncillaries:";
	for (String a : ancillaries)
	    res += "\n\t" + a;

	res += "\nOutputs:";
	for (Product op : outputs)
	    res += "\n\t" + op;

	res += "}";
	return res;
    }
	
    /**
     * Adds the Pass for future registration purposes
     */
    public void setPass(Pass p)
    {
	pass = p;
    }

    /**
     * Adds an input Product for future registration purposes.
     * Quietly refuses to add a Product more than once.
     */
    public void addInputProduct(Product p)
    {
	if(!inputs.contains(p))
	    inputs.add(p);
    }

    /**
     * Adds an ancillary file name for future registration purposes.
     * Quietly refuses to add a file name more than once.
     */
    public void addAncillary(String filename)
    {
	if(!ancillaries.contains(filename))
	    ancillaries.add(filename);
    }

    /**
     * Creates and adds a not-yet registered Product for future registration.
     * Quietly refuses to add a product type and filename pair more than once.
     */

    public Product addOutputProduct(String productType, String filename)
	throws Exception
    {
	// Search the outputs list to see if this product is already there
	// We have a match if the productType matches
	// and the filename matches the DATA resource
	for (Product op : outputs) {
	    if(productType.equals(op.getProductType())) {
		String opfn = op.getResource("DATA");
		if(opfn != null && filename.endsWith(opfn))
		    // It's already there
		    return op;
	    }
	}

	Product p = new Product(pass.getAos(), pass.getLos(), "NCSHelper", productType, pass);
	p.setAlgorithm(algoName, algoVersion);

	// Add filename as DATA resource
	dsm.addDataResource(p, filename);
	outputs.add(p);
	return p;
    }

    /**
     * Creates Product database entries for all the output products,
     * registering them with all the input products and all the ancillary
     * files as ancestors.  This is a major improvement over building
     * the outputs one at a time because it does the product creation
     * inside a single database transaction.
     * By convention, the first input is the one 
     */
    public void addOutputsToDatabase()
	throws Exception
    {
	// First, update all the products
	String markerId = inputs.get(0).getId().toString();

	for (Product p : outputs) {
	    p.setMarkerId(markerId);
	    for (String aFileName : ancillaries) {
		p.addContributingResource(aFileName, " ");
	    }
	    for (Product ip : inputs) {
		p.addContributingProduct(ip);
	    }
	}
	// Then store them all in one transaction gulp
	try {
	    dsm.commitConnection();
	    for (Product p : outputs) {
		dsm.storeProduct(p, false);
	    }
	    dsm.commitConnection();
	}
	catch (Exception e) {
	    dsm.rollbackConnection();
	    throw e;
	}
	// Hard to believe, but that's it!
    }
}
