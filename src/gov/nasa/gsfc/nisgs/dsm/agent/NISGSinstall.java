/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.DSMAdministrator;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.ProductType;
import gov.nasa.gsfc.nisgs.dsm.FileMover;
import gov.nasa.gsfc.nisgs.properties.Utility;
import gov.nasa.gsfc.nisgs.interp.DOMUtil;
import org.w3c.dom.Node;
import java.util.TreeSet;
import java.sql.Connection;
import java.io.File;
import java.io.FilenameFilter;

/**
 * NISGSinstall takes one required argument - path to an XML file containing
 * a NISGS-SPA-INSTALL element.  Installs and validates the output
 * types described in the file.  Other arguments after the first can be
 * flags (begin with "-", currently only -noancillary is recognized)
 * or Product IDs which are used to clean up the Markers table if necessary.
 * @version 1.0 and we know what that means...
 */
public class NISGSinstall
{
    private String args[];
    private String xmlfile;
    private DsmProperties properties;

    /**
     * Convenience method for searching the args.  Returns the index of
     * a string in the args (or -1 if it isn't there).  I am flat out
     * amazed that I have to write this loop myself...
     */
    private int hasArg(String arg)
    {
	for(int i = 0; i < args.length; i++)
	    if(arg.equals(args[i]))
		return i;
	return -1;
    }

    public static void main(String[] args) throws Exception
    {
	if(args.length < 1) {
	    System.err.println("ARGS: NISGSinstall SPA.xml");
	    System.exit(-1);
	}

	try
            {
                (new NISGSinstall(args)).run();
            }
	catch (Exception re)
            {
		System.err.println("NISGSinstall error:");
		re.printStackTrace();
            }
    }

    NISGSinstall(String args[]) throws Exception
    {
	this.xmlfile = args[0];
	this.args = args;
	properties = new DsmProperties();
    }

    void run() throws Exception
    {
        DSMAdministrator dsm = new DSMAdministrator("NISGSinstall","NISGSinstall");
	FileMover fm = FileMover.newMover(dsm.getSite(), properties.getIS_Site(), properties);
        try {
	    Node spaXML = DOMUtil.readXMLFile(xmlfile);

	    if(!spaXML.getNodeName().equals("NISGS-SPA-INSTALL")) {
		throw new Exception("Not an NISGS-SPA-INSTALL document: " + xmlfile);
	    }
	    // Walk through the OutputTypes list, creating and storing the
	    // ProductTypes, and stashing the directories for later validation
	    TreeSet<File> dirs = new TreeSet<File>();
	    Node outputTypes = DOMUtil.find_node(spaXML, "OutputTypes");
	    if(outputTypes == null) {
		throw new Exception("No OutputTypes element in " + xmlfile);
	    }
	    System.out.println("Registering output product types with DSM...");
	    for (Node ot : DOMUtil.Children(outputTypes, "OutputType")) {
		String name = DOMUtil.getNodeAttribute(ot, "name");
		System.out.println("\t" + name);
		String directory = DOMUtil.getNodeAttribute(ot, "is-directory");
		File dirFile = null;
		if(directory != null) {
		    dirFile = new File(directory);
		    dirs.add(dirFile);
		}
		ProductType pt = new ProductType(name,
						 DOMUtil.getNodeAttribute(ot, "spacecraft"),
						 DOMUtil.getNodeAttribute(ot, "sensor"),
						 DOMUtil.getNodeAttribute(ot, "description"),
						 DOMUtil.getNodeAttribute(ot, "level"),
						 dirFile);
		dsm.updateProductType(pt);
	    }
	    
	    // Walk the directories and check/add them on IS

	    for (File df : dirs) {
		System.out.println("Checking output product directory " + df);
		fm.mkdirs(df);
	    }

	    // Check for an ancillaries directory; if it exists, make sure
	    // all its files are in the IS isconfig directory
	    File xmlFile = new File(xmlfile);
	    File ancillaryDir = new File(xmlFile.getParentFile(), "ancillary");
	    if(ancillaryDir.isDirectory()) {
		FilenameFilter rsFilter =
		    new FilenameFilter() {
			public boolean accept(File dir, String name)
			{
			    return name.endsWith(".xml");
			}
		    };
		File[] rsFile = ancillaryDir.listFiles(rsFilter);
		
		for(File rsf : rsFile) {
		    if(rsf.length() == 0) {
			throw new Exception("Ancillary fetch file "
					    + rsf.getName()
					    + " is empty! This SPA is not assembled!");
		    }
		    File remoteFile = new File("isconfig", rsf.getName());
		    System.out.println("Installing ancillary retriever file "
				       + rsf.getName());
		    fm.moveFile(rsf, remoteFile);
		}
	    }

	    // Any args other than the first one (xmlfile) or args that
	    // look like flags are potential old PIDs that may need their
	    // markers handled
	    for (int i=1; i < args.length; i++) {
		String arg = args[i];
		if(!arg.startsWith("-"))
		    dsm.removeOldMarkers(arg);
	    }
	}
        finally {
	    dsm.dispose();
	    fm.quit();
	}
    }
}
