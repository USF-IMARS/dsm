/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm.agent;
import gov.nasa.gsfc.nisgs.dsm.*;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import gov.nasa.gsfc.drl.geo.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * This program creates an XML capsule resource for every product.
 * @version 3.19 Skip products that are unpublished or who has an unpublished
 *              ancestor product.
 */
public class Capsule
{
    private static final long ONE_SECOND = 1000L;
    private DsmLog logger = null;
    private DsmProperties config;
    private DocumentBuilder builder;
    // The sql string I use to find all products without a capsule resource.
    // It selects all Products with no capsule and at least one DATA
    // resource, which keeps Capsule from doing silly stuff like running up
    // behind the deleter and recreating just-deleted capsules.
    private static String sqlhead =
	"SELECT Products.* FROM Products"
	+ " LEFT JOIN Resources as rc"
	+ " ON Products.id=rc.product AND rc.rkey=\'capsule\'"
	+ " LEFT JOIN Resources as rd"
	+ " ON Products.id=rd.product AND rd.rkey=\'DATA\'"
	+ " WHERE rc.rkey IS NULL AND rd.rkey IS NOT NULL";
    /*
	"SELECT Products.* FROM Products LEFT JOIN Resources ON " +
            "Products.id=Resources.product AND Resources.rkey=\'capsule\' " +
            "WHERE Resources.rkey IS NULL";
    */
    // List of Product IDs that have failed previously
    Set<String> losers;

    public static void main(String[] args) throws Exception
    {
        int n = Integer.getInteger("sleepSeconds",37).intValue();
        if (n < 1) n = 1;
        long sleepyTime = (long)n * ONE_SECOND;

	Capsule capsule = new Capsule();

        while (true)
        {
            try
            {
                capsule.run();
            }
            catch (Exception re)
            {
                capsule.logger.report("Capsule error",re);
            }
            try { Thread.sleep(sleepyTime); } catch (InterruptedException e) {}
        }
    }

    /**
     * Constructor that only fails when it sees a condition that is
     * unlikely to heal itself - database connection failures get
     * retried, bogus config files and directory structures crap out.
     */

    Capsule() throws Exception
    {	
	long sleepMillis = 1000;
	while(logger == null) {
	    try {
		config = new DsmProperties();
		logger = new DsmLog("Capsule", config);
	    }
	    catch (Exception e) {
		System.err.println("Capsule error in initialization!");
		e.printStackTrace();
		try {
		    Thread.sleep(sleepMillis);
		}
		catch (Exception ee) {};
		if(sleepMillis < 1000000)
		    sleepMillis = 2 * sleepMillis;
	    }
	}
	losers = new HashSet<String>();
        String is_site = config.getIS_Site();
        String mysite = config.getSite();
        if (!mysite.equals(is_site)){
	    System.out.println("This site is " + mysite);
	    System.out.println("Capsule must run on site " + is_site);
	    Exception e = new Exception("Fatal. Capsule must run on the IS computer.");
	    logger.report("Capsule error!",e);
	    throw e;
	}

	// Complain NOW if the local data directory is hosed somehow...
	try {
	    Utility.isWritableDirectory(config.getLocalDataDirectory());
	}
	catch (Exception e) {
	    logger.report("LocalDataDirectory", e);
	    System.exit(-1);
	}

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        logger.report("Capsule ready.");
    }

     /**
      * Welcome to Java where almost everything is possible, but nothing
      * is easy or terse.  I believe the following code is nearly the
      * shortest way in Java to bounce these two containers off each other.
      * We walk the losers list, checking for each product ID in the products
      * list.  If a loser is found in the products list, it is removed from
      * the products list.  If a loser is NOT found in the products list,
      * it is removed from losers.  This keeps the losers list pruned down
      * to those products that are still showing up in the "products without
      * capsules" query.
      */
    private void flushLosers(List<Product> products, Set<String> losers)
    {
 	Iterator<String> li = losers.iterator();
 	while(li.hasNext()) {
 	    String loserID = li.next();
 	    Iterator<Product> pi = products.iterator();
 	    boolean foundit = false;
 	    while(pi.hasNext()) {
 		if(loserID.equals(pi.next().getId())) {
 		    foundit = true;
 		    pi.remove();
 		    break;
 		}
 	    }
 	    if(!foundit) {
 		li.remove();
	    }
 	}
    }

    private void run() throws Exception
    {
        DSMAdministrator dsm = new DSMAdministrator("Capsule","Capsule");
        try
        {
            // I get unexpired products without capsules
	    // and build a capsule for each one.
	    String sql = sqlhead
		+ " AND Products.creation >= "
		+ Utility.quote(Hoover.computeCutoff(config.getProperty("DSM_ResourcesDaysRetention","365")));
            java.util.List<Product> products = dsm.queryProducts(sql);

	    // But first, clear any previous losers from the list
	    flushLosers(products, losers);

            for (Product product : products)
            {
                try
                {
                    try
                    {
                        doGeolocation(dsm,product);
                    }
                    catch (Exception geoe)
                    {
                        logger.report("Skipping. Capsule cannot do geolocation for " +
                                product.getProductType(),geoe);
                    }

                    //I get the product type object for this product.
                    ProductType productType = dsm.getProductType(product.getProductType());

                    //I create an element for this product.
                    Document document = builder.newDocument();  //the xml document
                    Element root = makeProductElement(document,product,productType);
                    if (root == null)
                    {
                        //The product is not yet published.
                        continue;
                    }

                    //I append a list of contributing products.
                    if (product.getContributingProductsIds() != null)
                    {
                        Element aelement = makeAncestorsElement(document,product,dsm);
                        if (aelement != null)
                        {
                            root.appendChild(aelement);
                        }
                        else
                        {
                            //Some ancestor product is not yet published.
                            continue;
                        }
                    }

                    document.appendChild(root);

                    //I create the xml file.
                    File xmlFile = makeXmlFile(document,productType,product,config);

                    //I add the file as a product resource.
                    dsm.addResource(xmlFile,product,"capsule",null,"capsule report");
                    logger.report("Capsule created xml capsule for product " + product.toString());
                }
                catch (Exception pe)
                { 		    
		    // Report this (if we haven't already done so)
 		    if(!losers.contains(product.getId())) {
			losers.add(product.getId());
 			logger.report("Error creating capsule for "
 				      + product.toString(),
 				      pe);
 		    }
                }
            }
        }
        catch (Exception e)
        {
            throw e;
        }
        finally
        {
            dsm.dispose();
        }
    }

    private void doGeolocation(DSMAdministrator dsm, Product product)
            throws Exception
    {
        if (Float.isNaN(product.getNorthLatitude()))  //any will do
        {
            String tleFile = //dsm.getTLE(product);
		dsm.getTimedAncillary("drl.tle", product.getStartTime());

            if (tleFile == null)
            {
                throw new Exception("TLE for " + product.getSpacecraft() +
                        " does not exist.");
            }
            TLE tle = new TLE(new File(tleFile));
            String[] elements = tle.getElements(product.getSpacecraft());
            if (elements == null)
            {
                throw new Exception("TLE " + tleFile +
                        " does not contain " + product.getSpacecraft());
            }
            Date t0 = product.getStartTime();
            Date t1 = product.getStopTime();
            if (t0.equals(t1))
            {
                //if t0==t1, I must add some time to t1 so Geo works.
                long t = t1.getTime() + 150000L;
                t1 = new Date(t);
            }
            Geo geo = new Geo(elements);
            Geo.Extents e = geo.getEstExtents(t0.getTime(),t1.getTime());
            float nlat = (float)e.getMinLat();
            float slat = (float)e.getMaxLat();
            float clat = (nlat + slat) / 2f;
            float wlon = 0f;
            float x = (float)e.getBeginLeft().getLon();
            float y = (float)e.getEndLeft().getLon();
            if ((x < 0) == (y < 0))
            {
                wlon = Math.min(x,y);
            }
            else if (x < 90f || y < 90f)  //straddles 0
            {
                wlon = Math.min(x,y);
            }
            else  //straddles +180,-180
            {
                wlon = Math.max(x,y);
            }
            float elon = 0f;
            x = (float)e.getBeginRight().getLon();
            y = (float)e.getEndRight().getLon();
            if ((x < 0) == (y < 0))
            {
                elon = Math.max(x,y);
            }
            else if (x < 90f || y < 90f)  //straddles 0
            {
                elon = Math.max(x,y);
            }
            else  //straddles +180,-180
            {
                elon = Math.min(x,y);
            }
            float clon = (elon + wlon) / 2f;
            if ((wlon > 90f) && (elon < 0)) //straddles +180,-180
            {
                clon += 180f;
                if (clon > 180f) clon -= 180f;
            }
            product.setCorners(nlat,slat,elon,wlon);
            product.setCenter(clat,clon);
            StringBuffer sb = new StringBuffer(512);
            sb.append("UPDATE Products SET centerLatitude=");
            sb.append(clat);
            sb.append(",centerLongitude=");
            sb.append(clon);
            sb.append(",northLatitude=");
            sb.append(nlat);
            sb.append(",southLatitude=");
            sb.append(slat);
            sb.append(",eastLongitude=");
            sb.append(elon);
            sb.append(",westLongitude=");
            sb.append(wlon);
            sb.append(" WHERE id=");
            sb.append(product.getId());
            dsm.update(sb.toString());
            dsm.commit();
        }
    }

    //returns null if product not published.
    private Element makeAncestorsElement(Document document, Product product,
            DSMAdministrator dsm) throws Exception
    {
        Element ancestorsElement = null;
        if (product.getContributingProductsIds() != null)
        {
            TreeSet<Product> ancestors = new TreeSet<Product>();
            addAncestors(product,ancestors,dsm);
            ancestorsElement = document.createElement("ancestors");
            for (Product a : ancestors)
            {
                ProductType productType = dsm.getProductType(a.getProductType());
                Element element = makeProductElement(document,a,productType);
                if (element == null)
                {
                    //ancestor product is not completely published.
                    ancestorsElement = null;
                    break;
                }
                ancestorsElement.appendChild(element);
            }
        }
        return ancestorsElement;
    }

    private void addAncestors(Product product, TreeSet<Product> ancestors,
            DSMAdministrator dsm) throws Exception
    {
        Collection<String> pids = product.getContributingProductsIds();
        if (pids != null)
        {
            for (String pid : pids)
            {
                Product p = dsm.getProduct(pid);
                if (p == null)
                {
                    throw new Exception("Product " + pid + " is not in the database.");
                }
                ancestors.add(p);
                addAncestors(p,ancestors,dsm);
            }
        }
    }

    //return null if product not yet published on IS.
    private Element makeProductElement(Document document, Product product,
            ProductType productType) throws Exception
    {
        Element root = document.createElement("product");
        root.setAttribute("id",product.getId());

        Element productTypeElement = document.createElement("productType");
        root.appendChild(productTypeElement);
        //productType may be null, so I get the name from product.
        productTypeElement.setAttribute("name",product.getProductType());
        if (productType != null)
        {
            productTypeElement.setAttribute("spacecraft",productType.getSpacecraft());
            productTypeElement.setAttribute("sensor",productType.getSensor());
            productTypeElement.setAttribute("description",productType.getDescription());
            String level = productType.getLevel();
            if (level != null)
            {
                productTypeElement.setAttribute("level",level);
            }
            File is_directory = productType.getISdirectory();
            if (is_directory != null)
            {
                productTypeElement.setAttribute("is_directory",is_directory.getPath());
            }
        }

        Pass pass = product.getPass();
        Element passElement = document.createElement("pass");
        root.appendChild(passElement);
        passElement.setAttribute("id",pass.getId());
        passElement.appendChild(makeTextNode(document,"station",pass.getStationName()));
        passElement.appendChild(makeTextNode(document,"spacecraft",pass.getSpacecraft()));
        String aos = Utility.format(pass.getAos());
        String los = Utility.format(pass.getLos());
        passElement.appendChild(makeTextNode(document,"aos",aos));
        passElement.appendChild(makeTextNode(document,"los",los));
        if (!Float.isNaN(pass.getCenterLatitude()))
        {
            String clat = Float.toString(pass.getCenterLatitude());
            String clong = Float.toString(pass.getCenterLongitude());
            passElement.appendChild(makeTextNode(document,"centerLatitude",clat));
            passElement.appendChild(makeTextNode(document,"centerLongitude",clong));
        }
        if (!Float.isNaN(pass.getNorthLatitude()))
        {
            String nlat = Float.toString(pass.getNorthLatitude());
            String slat = Float.toString(pass.getSouthLatitude());
            String elong = Float.toString(pass.getEastLongitude());
            String wlong = Float.toString(pass.getWestLongitude());
            passElement.appendChild(makeTextNode(document,"northLatitude",nlat));
            passElement.appendChild(makeTextNode(document,"southLatitude",slat));
            passElement.appendChild(makeTextNode(document,"eastLongitude",elong));
            passElement.appendChild(makeTextNode(document,"westLongitude",wlong));
        }

        root.appendChild(makeTextNode(document,"startTime",product.getStartTimeString()));
        root.appendChild(makeTextNode(document,"stopTime",product.getStopTimeString()));
        root.appendChild(makeTextNode(document,"creation",Utility.format(product.getCreationTime())));
        root.appendChild(makeTextNode(document,"agent",product.getAgent()));
        if (product.getAlgorithm() != null)
        {
            root.appendChild(makeTextNode(document,"algorithm",product.getAlgorithm()));
        }
        if (product.getAlgorithmVersion() != null)
        {
            root.appendChild(makeTextNode(document,"algorithmVersion",product.getAlgorithmVersion()));
        }
        if (!Float.isNaN(product.getCenterLatitude()))
        {
            String clat = Float.toString(product.getCenterLatitude());
            String clong = Float.toString(product.getCenterLongitude());
            root.appendChild(makeTextNode(document,"centerLatitude",clat));
            root.appendChild(makeTextNode(document,"centerLongitude",clong));
        }
        if (!Float.isNaN(product.getNorthLatitude()))
        {
            String nlat = Float.toString(product.getNorthLatitude());
            String slat = Float.toString(product.getSouthLatitude());
            String elong = Float.toString(product.getEastLongitude());
            String wlong = Float.toString(product.getWestLongitude());
            root.appendChild(makeTextNode(document,"northLatitude",nlat));
            root.appendChild(makeTextNode(document,"southLatitude",slat));
            root.appendChild(makeTextNode(document,"eastLongitude",elong));
            root.appendChild(makeTextNode(document,"westLongitude",wlong));
        }
        String subproduct = product.getSubproduct();
        if (subproduct != null)
        {
            Element subproductElement = document.createElement("subproduct");
            root.appendChild(subproductElement);
            subproductElement.setAttribute("name",subproduct);
            java.util.Collection<Attribute> attributes = product.getAttributes();
            if (attributes != null)
            {
                for (Attribute attribute : attributes)
                {
                    String aname = attribute.getName();
                    String avalue = attribute.getValue();
                    subproductElement.appendChild(makeTextNode(document,aname,avalue));
                }
            }
        }

        Element resourcesElement = document.createElement("resources");
        root.appendChild(resourcesElement);
        Collection<Resource> resources = product.getResources();
        for (Resource resource : resources)
        {
            if (!resource.isPublished())
            {
                return null;
            }
            Element resourceElement = document.createElement("resource");
            resourcesElement.appendChild(resourceElement);
            resourceElement.setAttribute("id",resource.getId());
            resourceElement.setAttribute("rkey",resource.getKey());
            File file = resource.getFile();
            String xpath = null;
            if (file == null)
            {
                xpath = "*DELETED*";
            }
            else
            {
                xpath = file.getPath();
            }
            resourceElement.setAttribute("path",xpath);
            if (resource.getDescription() != null)
            {
                resourceElement.setAttribute("description",resource.getDescription());
            }
        }

        Collection<Resource> thumbnails = product.getThumbnails();
        if (thumbnails != null)
        {
            Element thumbnailsElement = document.createElement("thumbnails");
            root.appendChild(thumbnailsElement);
            for (Resource thumbnail : thumbnails)
            {
                Element thumbnailElement = document.createElement("thumbnail");
                thumbnailsElement.appendChild(thumbnailElement);
                thumbnailElement.setAttribute("id",thumbnail.getId());
                thumbnailElement.setAttribute("path",thumbnail.getFile().getPath());
                if (thumbnail.getDescription() != null)
                {
                    thumbnailElement.setAttribute("description",thumbnail.getDescription());
                }
            }
        }

        Collection<Resource> contributors = product.getContributingResources();
        if (contributors != null)
        {
            Element contributorsElement = document.createElement("contributors");
            root.appendChild(contributorsElement);
            for (Resource contributor : contributors)
            {
                Element contributorElement = document.createElement("contributor");
                contributorsElement.appendChild(contributorElement);
                contributorElement.setAttribute("id",contributor.getId());
                contributorElement.setAttribute("path",contributor.getFile().getPath());
                if (contributor.getDescription() != null)
                {
                    contributorElement.setAttribute("description",contributor.getDescription());
                }
            }
        }

        return root;
    }

    private Element makeTextNode(Document document, String label, String value)
    {
        Element e = document.createElement(label);
        e.appendChild(document.createTextNode(value));
        return e;
    }

    private File makeXmlFile(Document document, ProductType productType,
            Product product, DsmProperties config) throws Exception
    {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        File directory = null;
        if (productType == null || productType.getISdirectory() == null)
        {
            directory = new File(config.getProperty("IS_dropbox","/"));
        }
        else
        {
            directory = productType.getISdirectory();
        }
        File rootDirectory = new File(config.getLocalDataDirectory());
        File absoluteDirectory = new File(rootDirectory,directory.getPath());
        String filename = product.getProductType() + "_" + product.getId() + ".xml";
        File xmlFile = new File(absoluteDirectory,filename);
        FileWriter fw = new FileWriter(xmlFile);
        StreamResult result = new StreamResult(fw);
        transformer.transform(source, result);

        //the xmlFile object is absolute, which is not what I want to put into the database.
        //I want the root part peeled off.
        return new File(directory,filename);
    }
}

