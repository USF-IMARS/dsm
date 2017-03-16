/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.sql.*;
import java.io.File;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * ProductFactory creates a Product object from a database Product record.
 * It is not public. It exists to reduce the size of the DSM class.
 * It should not be instantiated.
 * <p>
 * This could be a Product constructor, but I want to keep Product small.
 * Creating a product object requires subsequent database queries,
 * which is much more than I want Product to know about.
 * @version 3.0.0 Added the product creation date field and site awareness.
 * @version 3.19 Added "published" to Resource creation.
 */
public final class ProductFactory
{
    /**
     * Make a Product from a result set. The result set must be the
     * result of a query that selects products from the database.
     */
    static Product makeProduct(Connection connection, String site,
            ResultSet resultSet) throws Exception
    {
        //I can't use sql date/time types because I need to combine the
        //date and time into one date object, which they don't do. For
        //them, it's either date or time but not both.
        String startTime = resultSet.getString("startTime");
        String stopTime = resultSet.getString("stopTime");
        java.util.Date start = Utility.parse(startTime);
        java.util.Date stop = Utility.parse(stopTime);

        String agent = resultSet.getString("agent");
        String productType = resultSet.getString("productType");
        Pass pass = getPass(connection,resultSet.getString("pass"));
        Product product = new Product(start,stop,agent,productType,pass);

        String productId = resultSet.getString("id");
        product.setId(productId);

        String creationTime = resultSet.getString("creation");
        java.util.Date creation = Utility.parse(creationTime);
        product.setCreationTime(creation);

        String algorithm = resultSet.getString("algorithm");
        String algorithmVersion = resultSet.getString("algorithmVersion");
        product.setAlgorithm(algorithm,algorithmVersion);

        boolean deleteProtected = resultSet.getBoolean("deleteProtected");
        product.setDeleteProtected(deleteProtected);

	product.setMarkerId(resultSet.getString("markerId"));

        loadGeolocation(resultSet,product);
        loadResources(connection,site,productId,product);
        loadThumbnails(connection,productId,product);
        loadSubproductInformation(connection,resultSet,productId,product);
        loadAncestors(connection,productId,product);
        loadContributors(connection,productId,product);
	loadMarkerId(connection,product);
        return product;
    }

    /**
     * Get a pass based on a database pass id.
     */
    private static synchronized Pass getPass(Connection connection,
            String passId) throws Exception
    {
        Pass pass = null;
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Passes WHERE id=" +
                Utility.quote(passId));
        if (r.next())
        {
            pass = new Pass(r);
        }
        s.close();
        return pass;
    }

    private static void loadGeolocation(ResultSet r, Product product)
            throws Exception
    {
        String sclat = r.getString("centerLatitude");
        String sclong = r.getString("centerLongitude");
        if (sclat != null && sclong != null)
        {
            float clat = Float.parseFloat(sclat);
            float clong = Float.parseFloat(sclong);
            product.setCenter(clat,clong);
        }

        String snlat = r.getString("northLatitude");
        String sslat = r.getString("southLatitude");
        String selong = r.getString("eastLongitude");
        String swlong = r.getString("westLongitude");
        if (snlat != null && sslat != null && selong != null && swlong != null)
        {
            float nlat = Float.parseFloat(snlat);
            float slat = Float.parseFloat(sslat);
            float elong = Float.parseFloat(selong);
            float wlong = Float.parseFloat(swlong);
            product.setCorners(nlat,slat,elong,wlong);
        }
    }

    private static void loadResources(Connection connection, String site,
            String productId, Product p) throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Resources WHERE product=" +
                productId);
        while (r.next())
        {
            String rid = r.getString("id");
            String keyword = r.getString("rkey");
            String description = r.getString("description");
            String filename = r.getString("path");
            boolean published = r.getBoolean("published");

            Resource resource = new Resource(rid,filename,description,published);
            loadLocations(connection,site,resource);
            p.addResource(keyword,resource);
        }
        s.close();
    }

    private static void loadLocations(Connection connection, String localSite,
            Resource resource) throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, 
            "SELECT ResourceSites.site,ResourceSites.creation,Directories.path " +
            "FROM ResourceSites,Directories WHERE ResourceSites.resource=" +
            resource.getId() + " AND ResourceSites.directory=Directories.id");
        while (r.next())
        {
            String site = r.getString(1);
            String creationTime = r.getString(2);
            String directory = r.getString(3);
            java.util.Date creation = Utility.parse(creationTime);
            if (localSite.equals(site))
            {
                resource.setLocal(site,directory,creation);
            }
            else
            {
                resource.addRemote(site,directory,creation);
            }
        }
        s.close();
    }

    private static void loadThumbnails(Connection connection, String productId,
            Product p) throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT Thumbnails.path,Thumbnails.description FROM Thumbnails,ProductThumbnails WHERE ProductThumbnails.product=" +
                productId + " AND Thumbnails.id=ProductThumbnails.thumbnail");
        while (r.next())
        {
            String path = r.getString("path");
            String description = r.getString("description");
            p.addThumbnail(path,description);
        }
        s.close();
    }

    private static void loadSubproductInformation(Connection connection, ResultSet r,
            String productId, Product p) throws Exception
    {
        String subproduct = r.getString("subproduct");
        if (subproduct != null)
        {
            Statement s = connection.createStatement();
            p.setSubproduct(subproduct);
            ResultSet rr = Utility.executeQuery(s, "SELECT * FROM " + subproduct +
                    " WHERE product=" + productId);
            if (rr.next())
            {
                ResultSetMetaData rmd = rr.getMetaData();
                int count = rmd.getColumnCount();
                for (int i = 1; i <= count; i++)
                {
                    String name = rmd.getColumnName(i);
                    String value = rr.getString(i);
                    p.addAttribute(name,value);
                }
            }
            s.close();
        }
    }

    private static void loadAncestors(Connection connection, String productId,
            Product p) throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT ancestor FROM Ancestors WHERE product="
                + productId);
        while (r.next())
        {
            String pid = r.getString(1);
            p.addContributingProduct(pid);
        }
        s.close();
    }

    private static void loadContributors(Connection connection, String productId,
            Product p) throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT Contributors.path,Contributors.description FROM Contributors,ProductContributors WHERE ProductContributors.product="
                + productId + " AND Contributors.id=ProductContributors.contributor");
        while (r.next())
        {
            String path = r.getString(1);
            String description = r.getString(2);
            p.addContributingResource(path,description);
        }
        s.close();
    }

    /**
     * If markerId is not something valid, try to load it up now.
     */
    private static void loadMarkerId(Connection connection, Product p)
	throws Exception
    {
	if(p.getMarkerId().equals("0")) {
	    // If we have ancestors, one of them is probably our marker
	    // If we don't, we don't HAVE a markerId, and should punt
	    if(p.getContributingProductsIds() != null
		&& p.getContributingProductsIds().size() != 0) {
		// Go look up our gopherColony
		Statement s = connection.createStatement();
		try {
		    ResultSet r = Utility.executeQuery(s, "SELECT gopherColony FROM Algorithms WHERE name = " + Utility.quote(p.getAlgorithm()));
		    // There will be none or exactly one;
		    // gopherColony is a PRIMARY KEY in this table
		    if(r.next()) {
			String gopherColony = r.getString(1);
			// OK, check markers for appropriate entry
			r.close();
			r = Utility.executeQuery(s, "SELECT Markers.product from Markers LEFT JOIN Ancestors ON Markers.product = Ancestors.ancestor WHERE Ancestors.product = " + p.getId());
			if(r.next()) {
			    p.setMarkerId(r.getString(1));
			}
			// It is possible for this query to miss - say we have
			// deleted the ancestor product (and its marker entry)
			// and the deleter hasn't gotten around to this one yet.
			// This is OK, don't throw an error.
		    }
		    else {
			// Hmmm... we have no Algorithm table entry for this
			// Algorithm.  Weird... we'll just log it to stderr
			// and hope
			System.err.println("No Algorithm entry for " + p.getAlgorithm());
		    }
		}
		finally {
		    s.close();
		}
	    }
	}
    }

    /**
     * Allows FileMover argument to be optional
     */

    public static void removeProduct(Connection connection, String productID)
	throws Exception
    {
	removeProduct(connection, productID, null);
    }

    /**
     * Removes a Product (and all products that depend on it) from the database.
     * If you want associated files removed from IS, pass in a FileMover
     * object whose "to" side is IS.
     * Does NOT observe the deleteProtected flag.
     * It it safe to reprocess data recreating a Product after it has been
     * removeProduct()ed.
     */
    public static void removeProduct(Connection connection, String productID, FileMover fm)
	throws Exception
    {
	// We keep an ordered list of Product IDs so we can walk
	// the Product ancestry DAG
	List<String> allProducts = new LinkedList<String>();
	
	allProducts.add(productID);
	removeProducts(connection, allProducts, fm);
    }

    /**
     * Usual crap allowing FileMover to be optional.
     */
    public static void removeProducts(Connection connection, Collection<String> allProducts)
	throws Exception
    {
	removeProducts(connection, allProducts, null);
    }

    /**
     * Removes a list of Products (and all products that depend on them)
     * from the database.
     * Removes records of Resources - NOT THE RESOURCE FILES.
     * Does NOT observe the deleteProtected flag.
     * It it safe to reprocess data recreating a Product after it has been
     * removeProduct()ed.
     */
    public static void removeProducts(Connection connection, Collection<String> products, FileMover fm)
	throws Exception
    {
	// Make a local copy of the input Product list
	List<String> allProducts = new LinkedList<String>();
	for (String s : products)
	    allProducts.add(s);

	Statement stmt = connection.createStatement();
	try {
	    // For some brain-damaged reason, ListIterators throw the
	    // ConcurrentModificationException for LinkedLists, thus
	    // requiring the ugly index-based code below:
	    int i = 0;
	    while(i < allProducts.size()) {
		String pid = allProducts.get(i++);
		// Query for all Products that depend on this one
		String dep = "SELECT product FROM Ancestors WHERE ancestor="
		    + Utility.quote(pid);
		ResultSet rs = Utility.executeQuery(stmt, dep);
		while(rs.next()) {
		    // If we haven't seen this child before, tack it
		    // onto the end of the allProducts list
		    String childID = rs.getString(1);
		    if(!allProducts.contains(childID)) {
			allProducts.add(childID);
		    }
		}
		rs.close();
	    }
	    // All the products we need to kill are in allProducts, in
	    // pre-order, breadth-first traversal of the product DAG.
	    // Run through them and kill them all.
	    for (String pid : allProducts) {
		removeOneProduct(stmt, pid, fm);
	    }
	    // We must commit somewhere; here, or inside the loop?
	    Utility.commitConnection(connection);
	}
	finally {
	    stmt.close();
	}
    }

    /**
     * Usual crap allowing FileMover argument to be optional.
     */
    public static void removeOneProduct(Statement stmt, String productID)
	throws Exception
    {
	removeOneProduct(stmt, productID, null);
    }

    /**
     * Removes one Product from the database.  Removes all records of all
     * resources associated with this product.
     * Note that this does NOT commit().
     */
    public static void removeOneProduct(Statement stmt, String productID, FileMover fm)
	throws Exception
    {
	// This is going to be a popular string here...
	String qpid = Utility.quote(productID);
	// Grab the pass ID before we start killing stuff - we may have
	// to delete the pass for this product when we're done
	String passID = null;
	String sql = "SELECT pass FROM Products where id=" + qpid;
	ResultSet rs = Utility.executeQuery(stmt, sql);
	if(rs.next() && rs.isLast()) {
	    passID = rs.getString(1);
	    rs.close();
	}
	else throw new Exception("Missing Pass ID for Product " + productID);

	// Before we mung the database, think about deleting the actual files
	// If we have a FileMover, see if this product has any resources
	// at the IS site, and hold onto them for later deletion
	List<File> isFiles = new LinkedList<File>();
	if (fm != null) {
	    String isSite = fm.getIS_Site();
	    sql = "SELECT Directories.path, Resources.path from Resources "
		+ "LEFT JOIN ResourceSites on ResourceSites.resource = Resources.id "
		+ "LEFT JOIN Directories on Directories.id = ResourceSites.directory "
		+ "WHERE ResourceSites.site = " + Utility.quote(isSite)
		+ " AND Resources.product = " + qpid;
	    rs = Utility.executeQuery(stmt, sql);
	    while(rs.next()) {
		isFiles.add(new File(rs.getString(1), rs.getString(2)));
	    }
	}

	// The most critical thing to kill here is all the Resources
	// and ResourceSites entries - this keeps anybody from doing any
	// reprocessing based on this product.
	sql = "DELETE Resources, ResourceSites FROM Resources, ResourceSites WHERE Resources.product = " + qpid + " AND Resources.id = ResourceSites.resource";
	Utility.executeUpdate(stmt, sql);

	// We also need to smack the ProductThumbnail/Thumbnail tables
	sql = "DELETE ProductThumbnails, Thumbnails FROM ProductThumbnails, Thumbnails WHERE ProductThumbnails.product = " + qpid + " AND ProductThumbnails.thumbnail = Thumbnails.id";
	Utility.executeUpdate(stmt, sql);

	// And the ProductContributors/Contributors tables
	sql = "DELETE ProductContributors, Contributors FROM ProductContributors, Contributors WHERE ProductContributors.product = " + qpid + " AND ProductContributors.contributor = Contributors.id";
	Utility.executeUpdate(stmt, sql);

	// And the Ancestors table
	sql = "DELETE FROM Ancestors where product = " + qpid + " OR ancestor = " + qpid;
	Utility.executeUpdate(stmt, sql);

	// And the Markers table
	sql = "DELETE FROM Markers where product = " + qpid;
	Utility.executeUpdate(stmt, sql);

	// And subproduct tables, if any...
	sql = "SELECT subproduct from Products where id = " + qpid + " AND subproduct IS NOT NULL";
	rs = Utility.executeQuery(stmt, sql);
	if(rs.next()) {
	    sql = "DELETE FROM " + rs.getString(1) + " WHERE product = " + qpid;
	    Utility.executeUpdate(stmt, sql);
	}
	rs.close();

	// And TransferCommands...
	sql = "DELETE FROM TransferCommands WHERE tableName = 'Products' AND id = " + qpid;
	Utility.executeUpdate(stmt, sql);

	// And finally, the Product itself
	sql = "DELETE FROM Products where id = " + qpid;
	Utility.executeUpdate(stmt, sql);

	// Check the pass - if there are no more products associated
	// with this pass, we can shoot the pass too.
	sql = "DELETE Passes FROM Passes"
	    + " LEFT JOIN Products on Passes.id = Products.pass"
	    + " WHERE Passes.id = " + Utility.quote(passID)
	    + " AND Products.pass IS NULL";
	Utility.executeUpdate(stmt, sql);

	// If there were any IS resource files found, try and delete them
	// using the FileMover
	for (File f : isFiles)
	    fm.delete(f);
    }

    /**
     * Returns a MarkerRef pointing to the entry in the marker table for
     * the product in question.  Can return null, which means there is no
     * marker for this product.
     */
    public static MarkerRef findControllingMarker(Statement stmt, String productID)
	throws Exception
    {
	
	// Get the algorithm name and markerId from the Product
	String sql = "SELECT algorithm, markerId FROM Products WHERE id = " + productID;
	ResultSet rs = Utility.executeQuery(stmt, sql);
	if(rs.next()) {
	    String algorithm = rs.getString(1);
	    int markerId = rs.getInt(2);

	    sql = "SELECT gopherColony FROM Algorithms WHERE name = " + Utility.quote(algorithm);
	    rs = Utility.executeQuery(stmt, sql);
	    if(rs.next()) {
		String gopherColony = rs.getString(1);
		
		// If markerId is 0, check the Markers table.  Barring
		// cross-pass dependencies, there should be exactly one
		// Markers table entry with an ancestor of this product
		// with that gopherColony.
		if(markerId == 0) {
		    sql = "SELECT Markers.product from Markers"
			+ " LEFT JOIN Ancestors"
			+ " ON Markers.product = Ancestors.ancestor"
			+ " WHERE Ancestors.product = " + productID
			+ " AND Markers.gopherColony = " + Utility.quote(gopherColony);
		    rs = Utility.executeQuery(stmt, sql);

		    if(rs.next()) {
			markerId = rs.getInt(1);
			if(rs.next())
			    throw new Exception("findControllingMarker: more than one marker entry for ancestors of " + productID + " with gopherColony " + gopherColony);
		    }
		    else
			// There is no marker in our ancestors; it is safe
			// to punt
			return null;
		}
		// We have the markerId and the gopherColony; create the
		// MarkerRef result
		return new MarkerRef(markerId, gopherColony);
	    }
	    else {
		// There was no record in Algorithms mapping this algorithm name
		// to a gopherColony.  If this product has ancestors, this
		// is an error
		sql = "SELECT ancestor FROM Ancestors where product = " + productID;
		rs = Utility.executeQuery(stmt, sql);

		if(rs.next())
		    throw new Exception("Product " + productID + " has an algorithm name of " + algorithm + " which is not in the Algorithms table");
		else
		    return null;
	    }
	}
	// There was no product with this productID
	else
	    return null;
    }

    /**
     * Delete a particular MarkerRef from the Markers table.
     * Does NOT commit.
     */
    public static void deleteMarker(Statement stmt, MarkerRef mr)
	throws Exception
    {
	String sql = "DELETE FROM Markers WHERE product = " + mr.markerId
	    + " AND gopherColony = " + Utility.quote(mr.gopherColony);
	//System.err.println("deleteMarker: " + sql);
	Utility.executeUpdate(stmt, sql);
    }


    /**
     * Delete all markers on a particular Product.
     * Does NOT commit.
     */
    public static void deleteMarkersOnProduct(Statement stmt, String pid)
	throws Exception
    {
	String sql = "DELETE FROM Markers WHERE product = " + pid;
	Utility.executeUpdate(stmt, sql);
    }


    /**
     * Checks a Product - if it has no resources, deletes all knowledge of
     * it from the database.  NOTE that this takes a database statement,
     * and does NOT commit() anything it does.
     */
    public static void checkProductResources(Statement stmt, String productID)
	throws Exception
    {
	String resq =
	    "SELECT COUNT(id) FROM Resources where Resources.product = "
	    + Utility.quote(productID);
	ResultSet rs = Utility.executeQuery(stmt, resq);
	// There had better be exactly one result in this set...
	if(rs.next() && rs.isLast()) {
	    if(rs.getInt(1) == 0) {
		removeOneProduct(stmt, productID);
	    }
	}
	else
	    throw new Exception("SELECT COUNT(id) did not return ONE result");
    }

    /**
     * Given a Pass ID, returns a Set of the root Product IDs in this pass
     * (Products with no ancestors).  This is the set of Products that must
     * be retained to reprocess a Pass.
     */
    public static Set<String> rootProducts(Connection connection, String passID)
	throws Exception
    {
	Set<String> result = new HashSet<String>();
	Statement stmt = connection.createStatement();
	String sql = "SELECT Products.id FROM Products"
	    + " LEFT JOIN Ancestors"
	    + " ON Ancestors.product = Products.id"
	    + " WHERE Ancestors.ancestor IS NULL"
	    + " AND Products.pass = " + Utility.quote(passID);
	try {
	    ResultSet rs = Utility.executeQuery(stmt, sql);
	    while(rs.next()) {
		result.add(rs.getString(1));
	    }
	    rs.close();
	}
	finally {
	    stmt.close();
	}
	return result;
    }

    /**
     * Takes a Product ID and a Set of other Product IDs.  Adds immediate
     * children of the Product to the Set; returns a List of anything newly
     * added to the Set.
     */
    public static List<String> addChildren(Connection connection, String parent, Set<String> children)
	throws Exception
    {
	List<String> result = new LinkedList<String>();
	Statement stmt = connection.createStatement();
	String sql = "SELECT product FROM Ancestors WHERE ancestor = "
	    + Utility.quote(parent);
	try {
	    ResultSet rs = Utility.executeQuery(stmt, sql);
	    while(rs.next()) {
		String child = rs.getString(1);
		if(children.add(child))
		    result.add(child);
	    }
	}
	finally {
	    stmt.close();
	}
	return result;
    }

    /**
     * Takes a List of Product IDs and a Set of other Product IDs.  Adds
     * all children of all Products in the List to the Set (recursing down
     * to get all the distant relatives).
     */
    public static void addAllChildren(Connection connection, Collection<String> parents, Set<String> children)
	throws Exception
    {
	for (String parent : parents) {
	    List<String> newkids = addChildren(connection, parent, children);
	    addAllChildren(connection, newkids, children);
	}
    }


    /**
     * Given a List of Product IDs that is a set of root Products
     * (see rootProducts()), return a List of rootChildProducts
     * which can be fed to removeProduct() to prepare a pass for
     * reprocessing.
     */
    /*
    public static List<String> rootChildProducts(Connection connection, List<String> rootProducts)
	throws Exception
    {
	List<String> result = new LinkedList<String>();
	Statement stmt = connection.createStatement();
	String sql = "SELECT product FROM Ancestors WHERE ancestor = ";
	try {
	    for (String root : rootProducts) {
		ResultSet rs = Utility.executeQuery(stmt, sql + Utility.quote(root));
		while(rs.next()) {
		    String child = rs.getString(1);
		    if(!result.contains(child))
			result.add(child);
		}
		rs.close();
	    }
	}
	finally {
	    stmt.close();
	}
	return result;
	}*/
}
