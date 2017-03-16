/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.*;
import java.sql.*;

import java.util.ArrayList;
import java.util.HashSet;
import gov.nasa.gsfc.nisgs.dsm.DsmProperties;
import gov.nasa.gsfc.nisgs.dsm.FileMover;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * DSM is an interface class to the DSM subsystem and database.
 * It needs mysql-jdbc.jar.
 * <p>
 * The fetchProduct and reserveProduct methods all block when fetching or reserving
 * products. The timeQuota and timeSleep parameters determine the times the DSM uses.
 * If you are implementing multiple threads with different time blocking requirements,
 * you should instantiate more than one DSM object.
 * @version 3.0 Added support for transfer commands
 * @version 3.22 Fixed an infinite blocking problem in all fetchProduct methods.
 */
public class DSM implements AutoCloseable
{
    private static final int ONE_SECOND = 1000;
    private int timeQuota = 300 * ONE_SECOND;
    private int timeSleep = 5 * ONE_SECOND;
    private String thisSite;
    private String thisGroup;
    private String thisUser;
    private String thisPid;
    protected DsmProperties dsmProperties;
    protected AncillaryDepot ancillaryDepot;
    protected Reservation reservation;
    protected File rootDataDirectory;

    /**
     * Construct the DSM object. This is the preferred constructor.
     * It will establish a network connection to
     * the database server. This constructor expects certain information to
     * be defined in system properties. This is the preferred constructor.<br>
     * NISGS_HOME - The NISGS root directory. The constructor will look for
     * the file nisgs.properties in this directory, and it will throw an
     * exception if it is not found. You should define NISGS_HOME with a
     * -D parameter when you run your java program. If you omit NISGS_HOME,
     * then you must define the following system properties explicitly.
     * <br>
     * DSM_DATABASE_HOME - An IP address or host name for the DSM database.
     * If you do not define it as a system property, the constructor will
     * look for it in nisgs.properties. An explicit definition takes precedence
     * over a nisgs.properties definition. If undefined in either case, the
     * constructor throws an exception.
     * <br>
     * NISGS_SITE_NAME - The nickname used for this computer. It should be
     * predefined in the DSM database. If you do not define it as a system
     * property, the constructor will look for it in nisgs.properties.
     * An explicit definition takes precedence over a nisgs.properties
     * definition. If undefined in either case, the constructor throws an
     * exception.
     * <br>
     * @param myGroup Each program using the DSM should have a unique group
     *      name. The DSM uses the group name to mark products so as to keep
     *      track of which ones this program has already processed. See
     *      reserveProduct(). The DSM does not enforce the uniqueness of a
     *      group name.
     * @param myName Each instance of this program should have a unique name
     *      to distinguish it from other instances in the same group. The DSM
     *      does not enforce uniqueness.
     */
    public DSM(String myGroup, String myName) throws Exception
    {
        thisGroup = myGroup;
        thisUser = myName;
        dsmProperties = new DsmProperties();
	thisSite = dsmProperties.getSite();
        String dir = dsmProperties.getLocalDataDirectory();
        rootDataDirectory = new File(dir);
        finishInit(dsmProperties.getIS_Site());
    }

    /**
     * Construct the DSM object. It will establish a network connection to
     * the database server.
     * @param mySite A name for the computer on which this program is running.
     *          Site names are (must be) predefined in the DSM, and all programs
     *          running on the same computer must use the same site name for
     *          that computer. Case is significant.
     * @param myGroup Each program using the DSM should have a unique group
     *      name. The DSM uses the group name to mark products so as to keep
     *      track of which ones this program has already processed. See
     *      reserveProduct(). The DSM does not enforce the uniqueness of a
     *      group name.
     * @param myName Each instance of this program should have a unique name
     *      to distinguish it from other instances in the same group. The DSM
     *      does not enforce uniqueness.
     * @param dbhost The name of the computer or IP address where the database
     *      server resides.
     * @param dbuser The database user name to be used to log into the database
     *      server.
     * @param dbpassword  The database password to be used to log into the
     *      database server.
     */
    public DSM(String mySite, String myGroup, String myName, String dbhost,
            String dbuser, String dbpassword)
            throws Exception
    {
        thisGroup = myGroup;
        thisUser = myName;
        thisSite = mySite;
	dsmProperties = new DsmProperties(mySite, dbhost, dbuser, dbpassword);

        finishInit("IS");
    }

    /**
     * Construct the DSM object. It will establish a network connection to
     * the database server.
     * @param mySite A name for the computer on which this program is running.
     *          Site names are (must be) predefined in the DSM, and all programs
     *          running on the same computer must use the same site name for
     *          that computer. Case is significant.
     * @param myGroup Each program using the DSM should have a unique group
     *      name. The DSM uses the group name to mark products so as to keep
     *      track of which ones this program has already processed. See
     *      reserveProduct(). The DSM does not enforce the uniqueness of a
     *      group name.
     * @param myName Each instance of this program should have a unique name
     *      to distinguish it from other instances in the same group. The DSM
     *      does not enforce uniqueness.
     * @param dbhost The name of the computer or IP address where the database
     *      server resides.
     */
    public DSM(String mySite, String myGroup, String myName, String dbhost)
            throws Exception
    {
        this(mySite,myGroup,myName,dbhost,"dsm","b28c935");
    }

    private void finishInit(String informationServicesSite)
	throws Exception
    {
	dsmProperties.initializeConnection();
        ancillaryDepot = new AncillaryDepot(thisSite,
					    dsmProperties.getConnection(),
					    rootDataDirectory,
					    informationServicesSite);
        reservation = new Reservation(dsmProperties.getConnection(),thisSite, informationServicesSite);
    }



    /**
     * Shut down the DSM object. This will close the network connection.
     */
    public void dispose() throws Exception
    {
	dsmProperties.dispose();
    }

    /**
     * close=dispose; added for AutoCloseable
     */
    public void close() throws Exception {dispose();}

    /**
     * This method will close the connection as part of cleanup.
     * Do not call it directly; use dispose() instead.
     */
    public void finalize()
    {
        try { dispose(); }
        catch (Exception e) {}
    }

    /**
     * Get the site name for this DSM instance.
     */
    public String getSite()
    {
        return thisSite;
    }

    /**
     * Get the group name for this DSM instance.
     */
    public String getGroup()
    {
        return thisGroup;
    }

    /**
     * Get the user name for this DSM instance.
     */
    public String getUser()
    {
        return thisUser;
    }

    /**
     * Get our PID (some string guaranteed unique for this agent)
     */
    public String getPid()
    {
	return thisPid;
    }

    /**
     * Set our PID (some string guaranteed unique for this agent)
     */
    public void setPid(String pid)
    {
	thisPid = pid;
	reservation.setPid(pid);
    }

    /**
     * Set the total amount of time in seconds that reserveProduct or fetchProduct waits
     * before it gives up and returns null. Use Integer.MAX_VALUE to have them wait
     * forever. The default is 300 seconds.
     */
    public void setTimeQuota(int seconds)
    {
        if (seconds < 0) seconds = 0;
        timeQuota = seconds * ONE_SECOND;
    }

    /**
     * Set the number of seconds between attempts that reserveProduct or fetchProduct
     * waits before trying to reserve or fetch a product. The default is 5 seconds.
     */
    public void setTimeSleep(int seconds)
    {
        if (seconds <= 0) seconds = 1;
        timeSleep = seconds * ONE_SECOND;
    }

    /**
     * This method allows convenient control of database commit behavior
     */
    public void commitConnection()
	throws Exception
    {
	Utility.commitConnection(dsmProperties.getConnection());
    }

    /**
     * This method allows convenient control of database rollback behavior
     */
    public void rollbackConnection()
	throws Exception
    {
	dsmProperties.getConnection().rollback();
    }

    /**
     * Store a new product in the DSM database.
     * @param product a reference to a Product object.
     * @param commitp boolean controlling whether we should commit this store now
     * @return the product ID for this product, which is its primary key.
     */
    public String storeProduct(Product product, boolean commitp)
	throws Exception
    {
        return ProductStore.store(dsmProperties.getConnection(),
				  product,thisSite,thisUser, commitp);
    }

    /**
     * Store a new product in the DSM database, and commit by default.
     * @param product a reference to a Product object.
     * @return the product ID for this product, which is its primary key.
     */
    public String storeProduct(Product product)
	throws Exception
    {
        return storeProduct(product, true);
    }

    /**
     * Get a product object from the database by its unique product ID.
     * No files are copied.
     * @param productID A unique product ID for the product, which the database
     *          created when the product was stored.
     * @return The product object with this ID or null if it does not exist.
     */
    public Product getProduct(String productID) throws Exception
    {
        return queryProduct("SELECT * FROM Products WHERE id=" + productID);
    }

    /**
     * Get the product object from the database for this product type and pass.
     * No files are copied.
     * @param productType A product type
     * @param pass A pass
     * @return A product of this product type and linked to the specified pass.
     *      If more than one product satisfies the conditions, it returns the
     *      first one that the database returns. It returns null if no product
     *      satisfies the conditions.
     */
    public Product getProduct(String productType, Pass pass) throws Exception
    {
        String sql = "SELECT * FROM Products WHERE productType=" +
                Utility.quote(productType) + " AND pass=" + pass.getId();
        return queryProduct(sql);
    }

    /**
     * Get the product object from the database for this product type and
     * pass and with a start and end time that brackets this time.
     * No files are copied. If more than one product satisfies the conditions,
     * it returns the first one that the database returns.
     * @param productType A product type
     * @param pass A pass
     * @param time A date and time
     * @return A product of this product type, linked to the specified pass,
     *      and one that brackets the specified time.
     *      If more than one product satisfies the conditions, it returns the
     *      first one that the database returns. It returns null if no product
     *      satisfies the conditions.
     */
    public Product getProduct(String productType, Pass pass, java.util.Date time)
            throws Exception
    {
        String timeString = Utility.quote(Utility.format(time));
        StringBuffer sb = new StringBuffer(512);
        sb.append("SELECT * FROM Products WHERE productType=");
        sb.append(Utility.quote(productType));
        sb.append(" AND pass=");
        sb.append(pass.getId());
        sb.append(" AND startTime<=");
        sb.append(timeString);
        sb.append(" AND stopTime>=");
        sb.append(timeString);
        return queryProduct(sb.toString());
    }

    /**
     * Get a product by its unique product ID.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor. It waits for the product to appear on the IS.
     * @param productID A product ID string that uniquely identifies a product.
     *      The database defines and assigns product IDs when products are stored.
     * @return a Product object or null if the product is unavailable within the
     *      allotted time.
     */
    public Product fetchProduct(String productID) throws Exception
    {
        Product product = getProduct(productID);

        if (product == null) {
	    int attempts = timeQuota / timeSleep;
	    for (int n = 0; n < attempts; n++) {
		try { Thread.sleep((long)timeSleep); }
		catch (InterruptedException ie) {}
		product = getProduct(productID);
		if (product != null) break;
	    }
	}
        if ((product != null) && !product.resourcesAreLocal()) {
	    // Reservation.copyProduct() can wait forever, so we have to
	    // subthread it in the usual Java verbose manner...
	    class CopyProduct extends Thread
	    {
		String productID;
		CopyProduct(String productID) {
		    this.productID = productID;
		}
		public Exception exception = null;
		public Product product = null;
		public void run() {
		    try {
			product =  reservation.copyProduct(productID);
		    }
		    catch (Exception e) {
			exception = e;
		    }
		}
	    };
	    CopyProduct cp = new CopyProduct(productID);
	    cp.start();
	    cp.join(timeQuota);

	    if(cp.isAlive()) {
		cp.interrupt();
		throw new Exception("Reservation.CopyProduct timed out");
	    }
	    if(cp.exception != null)
		throw cp.exception;

	    product = cp.product;
        }
        return product;
    }

    /**
     * Fetch the product for this product type and this pass.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor.
     * It will poll the database and wait for the product to appear.
     * @param productType A product type
     * @param pass A pass
     * @return A product of this product type and linked to the specified pass.
     *      If more than one product satisfies the conditions, it returns the
     *      first one that the database returns. It returns null if no product
     *      satisfies the conditions within the allotted time.
     */
    public Product fetchProduct(String productType, Pass pass) throws Exception
    {
        Product product = getProduct(productType,pass);
        if (product == null)
        {
            int attempts = timeQuota / timeSleep;
            for (int n = 0; n < attempts; n++)
            {
                try { Thread.sleep((long)timeSleep); }
                catch (InterruptedException ie) {}
                product = getProduct(productType,pass);
                if (product != null) break;
            }
        }
        if ((product != null) && !product.resourcesAreLocal())
        {
            product = reservation.copyProduct(product.getId());
        }
        return product;
    }

    /**
     * Get the product for this product type and pass and with a start and
     * end time that brackets this time.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor.
     * It will poll the database and wait for the product to appear.
     * @param productType A product type
     * @param pass A pass
     * @param time A date and time
     * @return A product of this product type, linked to the specified pass,
     *      and one that brackets the specified time.
     *      If more than one product satisfies the conditions, it returns the
     *      first one that the database returns. It returns null if no product
     *      satisfies the conditions within the allotted time.
     */
    public Product fetchProduct(String productType, Pass pass, java.util.Date time)
            throws Exception
    {
        Product product = getProduct(productType,pass,time);
        if (product == null)
        {
            int attempts = timeQuota / timeSleep;
            for (int n = 0; n < attempts; n++)
            {
                try { Thread.sleep((long)timeSleep); }
                catch (InterruptedException ie) {}
                product = getProduct(productType,pass,time);
                if (product != null) break;
            }
        }
        if ((product != null) && !product.resourcesAreLocal())
        {
            product = reservation.copyProduct(product.getId());
        }
        return product;
    }

    /**
     * Get a product of type productType that is unmarked by this group.
     * It marks the product with this group name.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor. This method blocks waiting for a product.
     * @param productType A product type
     * @param otherTypes space-separated list of other product types
     * @return A product or null if no unmarked product of this type is available.
     */
    public Product reserveProduct(String productType, String otherTypes) throws Exception
    {
        return reservation.reserveProduct(thisGroup,productType,otherTypes, timeSleep);
    }


    /**
     * Get a product of type productType that is unmarked by this group.
     * In this version of reserveProduct(), you may use a different group
     * name, which may be useful in programs that run multiple threads. This
     * method marks the product with this group name. It blocks.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor.
     * @param agroup A group name
     * @param productType A product type
     * @param otherTypes space-separated list of other product types
     * @return A product or null if no unmarked product of this type is available.
     */
    public Product reserveProduct(String agroup, String productType, String otherTypes) throws Exception
    {
        return reservation.reserveProduct(agroup,productType,otherTypes, timeSleep);
    }


    /**
     * Reserve a product that is unmarked by this group in which the product's
     * type matches the product type mask. This method marks the product with
     * this group name. It blocks.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor.
     * @param productTypeMask A product type search pattern in SQL format.
     *      Use SQL pattern matching syntax. SQL pattern matching allows you
     *      to use `_' to match any single character and `%' to match an arbitrary
     *      number of characters (including zero characters). SQL patterns are
     *      case-insensitive by default.
     * @param otherTypes space-separated list of other product types
     * @return A product or null if no unmarked product of this type is available.
     */
    public Product reserveProductLikeProductType(String productTypeMask, String otherTypes)
            throws Exception
    {
        return reservation.reserveProductLikeProductType(thisGroup,productTypeMask,otherTypes, timeSleep);
    }

    /**
     * Get a product of type productType that is unmarked by this group, and
     * products temporally before and after it.
     * It marks the product with this group name.
     * The DSM copies the product to the local site, which is the same site as
     * identified in the constructor. This method blocks waiting for a product.
     * @param productType A product type
     * @param otherTypes A space-separated list of other product types
     * @param granuleDuration Time in seconds.  The previous Product must contain the mainProduct.startTime - granuleDuration/2, and the next Product must contain the mainProduct.stopTime + granuleDuration/2.
     * @param prePostCount The number of granuleDuration-sized intervals to be checked before and after the main product's temporal extent.
     * @return A product or null if no unmarked product of this type is available.
     */
    public Product reserveProduct(String productType, String otherTypes, double granuleDuration, int prePostCount) throws Exception
    {
        return reservation.reserveProduct(thisGroup,productType,otherTypes, granuleDuration, prePostCount, timeSleep);
    }

    /**
     * This version of reserveProduct takes boxed numeric types as arguments,
     * to make it callable by Dsm_command.
     */
    public Product reserveProduct(String productType, String otherTypes, Double granuleDuration, Integer prePostCount) throws Exception
    {
        return reserveProduct(productType,otherTypes, granuleDuration.doubleValue(), prePostCount.intValue());
    }

    /**
     * This version of reserveProduct takes boxed numeric types as arguments,
     * to make it callable by Dsm_command, and it defaults prePostCount to 1
     * (the usual case of one granule before and after the main one).
     */
    public Product reserveProduct(String productType, String otherTypes, Double granuleDuration) throws Exception
    {
        return reserveProduct(productType,otherTypes, granuleDuration.doubleValue(), 1);
    }


    /**
     * Release a reserved product. The DSM marks it as completed by this group.
     * (The group name was created when this DSM instance was created.)
     * @param product A product to be released
     */
    public void releaseProduct(Product product) throws Exception
    {
        reservation.releaseProduct(Utility.quote(thisGroup),product,1);
    }

    /**
     * Release a reserved product. The DSM marks it as completed by agroup.
     * @param agroup A group name
     * @param product A product to be released
     */
    public void releaseProduct(String agroup, Product product) throws Exception
    {
        reservation.releaseProduct(Utility.quote(agroup),product,1);
    }

    /**
     * Fail a reserved product. The DSM marks it as completed by this group.
     * (The group name was created when this DSM instance was created.)
     * @param product A product to be failed
     */
    public void failProduct(Product product) throws Exception
    {
        reservation.releaseProduct(Utility.quote(thisGroup),product,2);
    }

    /**
     * Fail a reserved product. The DSM marks it as completed by agroup.
     * @param agroup A group name
     * @param product A product to be failed
     */
    public void failProduct(String agroup, Product product) throws Exception
    {
        reservation.releaseProduct(Utility.quote(agroup),product,2);
    }

    /**
     * Get one product type object by its name.
     * @return a product type or null if the type is not in the ProductTypes
     *      table
     */
    public ProductType getProductType(String name) throws Exception
    {
        return ProductTypes.getProductType(dsmProperties.getConnection(),name);
    }

    /**
     * Returns an absolute path to a local file for the Product.
     * Yes, this ought to be a Product method, but product objects
     * can't easily tell you what site we're currently on...
     */
    public String getProductPath (Product prod)
	throws Exception
    {
	String localpath = prod.getResource("DATA");
	if(localpath == null)
	    throw new Exception("No DATA resource for Product " + prod.getId() + "?!");

	File lf = new File(localpath);
	if(!lf.isAbsolute()) {
	    // Relative paths had better be relative to the data root
	    lf = new File(dsmProperties.getLocalDataDirectory(), lf.getPath());
	    // Throw a fit if we have constructed a path to a non-existent file...
	    if(!lf.exists())
		throw new Exception("Product " + prod.getId()
				    + " does not exist at " + lf.getPath());
	}
	return lf.getPath();
    }

    /**
     * Adds a "DATA" resource path to prod.
     * If we are on the IS host, and the incoming path begins with the
     * IS directory, remove it - IS DATA resources go into the database
     * as paths relative to the IS (presumably FTP) directory.
     */
    public void addDataResource(Product prod, String path)
	throws Exception
    {
	String isdir = dsmProperties.getLocalDataDirectory();

	if(thisSite.equals(dsmProperties.getIS_Site())
	   && path.startsWith(isdir))
	    prod.addResource("DATA", path.substring(isdir.length() + 1));
	else
	    prod.addResource("DATA", path);
    }

    /**
     * Returns the appropriate directory for this product type and host.
     * If we are on the IS host, return the is_directory for this product
     * type (or the dropbox if this type is unknown).  Otherwise return
     * an empty string
     */
    public String getProductDirectory(String productType)
	throws Exception
    {
	if(thisSite.equals(dsmProperties.getIS_Site())) {
	    ProductType pt = getProductType(productType);
	    if(pt != null && pt.getISdirectory() != null)
		return java.io.File.separator + pt.getISdirectory().getPath();
	    else {
		// Due to weirdness in Java File->path and other stuff, here
		// is the way to make sure this property gets handled the same
		// as the product path above
		File dropbox = new File(dsmProperties.getProperty("IS_dropbox"));
		return java.io.File.separator + dropbox.getPath();
	    }
	}
	else
	    return "";
    }

    /**
     * Delete a product resource file reference that is on this site. It deletes the
     * entry from the database, but it does not delete the file. It does not affect
     *any copy of this file existing on other computers.
     * @param filename The name of the file without directory information.
     *          For example, "file.dat', not "dir/dir1/dir2/file.dat".
     * @return true if it worked and false if no file by that name exists in the
     *          database.
     */
    public boolean deleteProductResource(String filename) throws Exception
    {
        boolean ok = false;
        String qfilename = Utility.quote(filename);
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT id from Resources WHERE path=" + qfilename);
        if (r.next())
        {
            ok = true;
            String rid = r.getString(1);
            String qmysite = Utility.quote(getSite());
            Utility.executeUpdate(s, "DELETE FROM ResourceSites WHERE resource=" +
                    rid + " AND site=" + qmysite);
            Utility.commitConnection(dsmProperties.getConnection());
        }
        return ok;
    }

    /**
     * Add a new resource file to an existing product in the database. Do NOT
     * use it to add a resource to a product that is not yet in the database.
     * @param file The file name to be added. It must be fully qualified with
     *          directory information.
     * @param product The product to which the resource will be added.
     * @param key A keyword to identify the product. Each resource has a unique
     *          keyword within a product. The primary resource is usually called
     *          "DATA." The capsule resource is "capsule." Use a short, case-
     *          sensitive name such as "image", "CREC", etc. The name should be
     *          unique within a product type.
     * @param site The site where the resource is located. (NISDS1, NISDS2, for
     *          example.) You may set this argument to null in which case the method
     *          uses the local site name. Most applications will use null.
     * @param comment A short phrase that describes the resource. It is optional,
     *          and you may pass null for this argument.
     */
    public void addResource(File file, Product product, String key, String site,
            String comment) throws Exception
    {
        if (product.getId() == null)
        {
            throw new Exception("addResource: This product is not yet in the database.");
        }
        if (site == null) site = thisSite;
        Resource resource = new Resource(file,product.getCreationTime(),comment);
        ProductStore.addResource(site,dsmProperties.getConnection(),
				 product,key,resource);
    }

    /**
     * Put a pass into the DSM database. It will insert the pass id into the
     * Pass object.
     * @param pass A pass object to be inserted into the database
     * @return the database pass ID
     */
    public String createPass(Pass pass) throws Exception
    {
        Statement statement = dsmProperties.getConnection().createStatement();
        String sql = pass.getSqlInsertionStatement();
        Utility.executeUpdate(statement, sql);
        Utility.commitConnection(dsmProperties.getConnection());
        String id = Utility.getLastAutoIncrementedValue(statement);
        pass.setId(id);
        statement.close();
        return id;
    }

    /**
     * Get a pass for a spacecraft such that the time is within (inclusive)
     * the pass's AOS and LOS. If more than one station has a pass that
     * satisfies the conditions, it is uncertain which pass the DSM will return.
     */
    public Pass getPass(String spacecraft, java.util.Date time) throws Exception
    {
        Pass pass = null;
        String timeString = Utility.quote(Utility.format(time));
        String spacecraftName = spacecraft.toUpperCase();
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Passes WHERE spacecraft=" +
                Utility.quote(spacecraftName) +
                " AND aos<=" + timeString + " AND los>=" + timeString);
        if (r.next())
        {
            pass = new Pass(r);
        }
        s.close();
        return pass;
    }

    /**
     * Get a pass for a spacecraft such that its start and stop times exactly
     * match the specified ones.
     * @param spacecraft a spacecraft name
     * @param startTime a date and time
     * @param stopTime a date and time
     * @return a Pass or null if no pass in the database qualifies
     */
    public Pass getPass(String spacecraft, java.util.Date startTime, java.util.Date stopTime) throws Exception
    {
        Pass pass = null;
        String startTimeString = Utility.quote(Utility.format(startTime));
        String stopTimeString = Utility.quote(Utility.format(stopTime));
        String spacecraftName = spacecraft.toUpperCase();
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Passes WHERE spacecraft=" +
                Utility.quote(spacecraftName) +
                " AND aos=" + startTimeString + " AND los=" + stopTimeString);
        if (r.next())
        {
            pass = new Pass(r);
        }
        s.close();
        return pass;
    }

    /**
     * Get a pass for a spacecraft and station such that the time is within
     * (inclusive) the pass's AOS and LOS.
     * @param spacecraft a spacecraft name
     * @param station a station name
     * @param time a date and time
     * @return a Pass or null if no pass in the database qualifies
     */
    public Pass getPass(String spacecraft, String station, java.util.Date time)
            throws Exception
    {
        Pass pass = null;
        String timeString = Utility.quote(Utility.format(time));
        String spacecraftName = spacecraft.toUpperCase();
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Passes WHERE station=" +
                Utility.quote(station) +
                " AND spacecraft=" + Utility.quote(spacecraftName) +
                " AND aos<=" + timeString + " AND los>=" + timeString);
        if (r.next())
        {
            pass = new Pass(r);
        }
        s.close();
        return pass;
    }


    /**
     * Find a pass for a spacecraft such that its start and stop times
     * surround the specified ones - aos &lt.= startTime, los &gt.= stopTime.
     * Returns the best fit (defined as smallest time interval that fits).
     * @param spacecraft a spacecraft name
     * @param startTime a date and time
     * @param stopTime a date and time
     * @return a Pass or null if no pass in the database qualifies
     */
    public Pass findPass(String spacecraft, java.util.Date startTime, java.util.Date stopTime) throws Exception
    {
        Pass pass = null;
        String startTimeString = Utility.quote(Utility.format(startTime));
        String stopTimeString = Utility.quote(Utility.format(stopTime));
        String spacecraftName = spacecraft.toUpperCase();
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery
	    (s, "SELECT * FROM Passes WHERE spacecraft=" + Utility.quote(spacecraftName)
	     + " AND aos <= " + startTimeString
	     + " AND los >= " + stopTimeString
	     + " ORDER BY los - aos ASC");
        if (r.next())
        {
            pass = new Pass(r);
        }
        s.close();
        return pass;
    }

    /**
     * Get a station by name from the Stations table.
     * @param name a unique name for the station, which is in the database
     * @return a Station object or null if the station is not in the table
     */
    public Station getStation(String name) throws Exception
    {
        return Stations.getStation(dsmProperties.getConnection(),name);
    }

    /**
     * Get a static ancillary data file associated with a unique label.
     * The DSM copies the file to the local site (computer).
     * @param akey a unique key string associated with the file
     * @return The path name of the file or null if no file exists.
     */
    public String getStaticAncillary(String akey) throws Exception
    {
        File file = ancillaryDepot.getStaticAncillary(thisSite,akey);
        return (file != null)? file.getAbsolutePath() : null;
    }

    /**
     * Put a static ancillary file into the DSM database. The DSM assumes the
     * file is on the local computer. It does not verify the file's existence.
     * @param label A unique keyword that identifies this file
     * @param path The path name of the file. It must include directory information.
     *      It may be just a directory (such as DEM) that contains ancillary files.
     *      When storing files from the Information Services (IS) computer, use a
     *      relative directory name that is relative to the root of the anonymous
     *      ftp directory tree.
     */
    public void putStaticAncillary(String label, String path)
            throws Exception
    {
        ancillaryDepot.storeStaticAncillary(label,path,null);
    }

    /**
     * Put a static ancillary file into the DSM database. The DSM assumes the
     * file is on the local computer. It does not verify the file's existence.
     * @param label A unique keyword that identifies this file
     * @param path The path name of the file. It must include directory information.
     *      It may be just a directory (such as DEM) that contains ancillary files.
     *      When storing files from the Information Services (IS) computer, use a
     *      relative directory name that is relative to the root of the anonymous
     *      ftp directory tree.
     * @param description a file description
     */
    public void putStaticAncillary(String label, String path, String description)
            throws Exception
    {
        ancillaryDepot.storeStaticAncillary(label,path,description);
    }

    /**
     * Put a timed ancillary file into the DSM database. The DSM assumes the
     * file is on the local computer. It does not verify the file's existence.
     * @param key A unique keyword that identifies this type of file. All files
     *          of the same type must use the same keyword. "UTCPOLE" and "LEAPSEC"
     *          are the keywords for those file types.
     * @param time A date and time for the file
     * @param path The path name of the file. It must include directory information.
     *      It may be just a directory (such as DEM) that contains ancillary files.
     *      When storing files from the Information Services (IS) computer, use a
     *      relative directory name that is relative to the root of the anonymous
     *      ftp directory tree.
     */
    public void putTimedAncillary(String key, java.util.Date time, String path)
            throws Exception
    {
        ancillaryDepot.storeTimedAncillary(key,time,path);
    }

    /**
     * Get a timed ancillary file for time nearest to the passed time.
     * Defaults the TimeWindow to "any time"
     * The DSM copies the file to the local site (computer).
     * @param key A unique keyword that identifies this type of file
     * @param time A date and time for the file
     * @return The path name of the file or null if no file exists.
     */
    public String getTimedAncillary(String key, java.util.Date time)
            throws Exception
    {
        return getTimedAncillary(key,time,TimeWindow.all());
    }

    /**
     * Get a timed ancillary file for time nearest to the passed time
     * that also matches the TimeWindow
     * The DSM copies the file to the local site (computer).
     * @param key A unique keyword that identifies this type of file
     * @param time A date and time for the file
     * @param tw A TimeWindow object
     * @return The path name of the file or null if no file exists.
     */
    public String getTimedAncillary(String key, java.util.Date time, TimeWindow tw)
            throws Exception
    {
        File file = ancillaryDepot.getTimedAncillary(thisSite,key,time, tw);
        return (file != null)? file.getAbsolutePath() : null;
    }


    /**
     * Put a spacecraft-time ancillary file into the DSM database. This type of
     * ancillary file is mapped by spacecraft, time, and a keyword. The DSM
     * assumes the file is on the local computer. It does not verify the file's
     * existence.
     * @param key A unique keyword that identifies this type of file. All files
     *          of the same type must use the same keyword. "drl.tle" is the
     *          keyword for TLE files.
     * @param time A date and time for the file
     * @param spacecraft A spacecraft name
     * @param path The path name of the file. It must include directory information.
     *      It may be just a directory (such as DEM) that contains ancillary files.
     *      When storing files from the Information Services (IS) computer, use a
     *      relative directory name that is relative to the root of the anonymous
     *      ftp directory tree.
     */
    public void putSatTimedAncillary(String key, java.util.Date time,
            String spacecraft, String path) throws Exception
    {
        ancillaryDepot.storeSatTimeAncillary(key,spacecraft,time,path);
    }

    /**
     * Get a spacecraft-time ancillary file for time nearest to the passed time.
     * The DSM copies the file to the local site (computer).
     * @param key A unique keyword that identifies this type of file
     * @param time A date and time for the file
     * @param spacecraft A spacecraft name
     * @return The path name of the file or null if no file exists.
     */
    public String getSatTimedAncillary(String key, java.util.Date time,
            String spacecraft) throws Exception
    {
        return getSatTimedAncillary(key,time,spacecraft, TimeWindow.all());
    }

    /**
     * Get a spacecraft-time ancillary file for time nearest to the passed time.
     * The DSM copies the file to the local site (computer).
     * @param key A unique keyword that identifies this type of file
     * @param time A date and time for the file
     * @param spacecraft A spacecraft name
     * @param tw A TimeWindow object
     * @return The path name of the file or null if no file exists.
     */
    public String getSatTimedAncillary(String key, java.util.Date time,
				       String spacecraft, TimeWindow tw)
	throws Exception
    {
        File file = ancillaryDepot.getSatTimeAncillary(key,time,spacecraft, tw);
        return (file != null)? file.getAbsolutePath() : null;
    }

    /**
     * Put a UTCPOLE file into the DSM database. The DSM assumes the file is
     * on the local computer. It does not verify the file's existence.
     * @param time The date and time of the UTCPOLE file.
     * @param path The path name of the file. It must include directory information.
     *      It may be just a directory (such as DEM) that contains ancillary files.
     *      When storing files from the Information Services (IS) computer, use a
     *      relative directory name that is relative to the root of the anonymous
     *      ftp directory tree.
     */
    public void putUTCPOLE(java.util.Date time, String path)
            throws Exception
    {
        ancillaryDepot.storeTimedAncillary("UTCPOLE",time,path);
    }

    /**
     * Get a UTCPOLE file for time nearest to UTCPOLE time.
     * The DSM copies the file to the local site (computer).
     * @param time The date and time of the UTCPOLE file.
     * @return The path name of the file or null if no file exists.
     */
    public String getUTCPOLE(java.util.Date time) throws Exception
    {
        return getTimedAncillary("UTCPOLE",time);
    }

    /**
     * Get the UTCPOLE file for time nearest to UTCPOLE time.
     * It uses the product's start time.
     * The DSM copies the file to the local site (computer).
     * @param product A product
     * @return The path name of the file or null if no file exists.
     */
    public String getUTCPOLE(Product product) throws Exception
    {
        return getUTCPOLE(product.getStartTime());
    }

    /**
     * Put a LEAPSEC file into the DSM database. The DSM assumes the file is
     * on the local computer. It does not verify the file's existence.
     * @param time The date and time of the leap second file.
     * @param path The path name of the file. It must include directory information.
     *      It may be just a directory (such as DEM) that contains ancillary files.
     *      When storing files from the Information Services (IS) computer, use a
     *      relative directory name that is relative to the root of the anonymous
     *      ftp directory tree.
     */
    public void putLEAPSEC(java.util.Date time, String path) throws Exception
    {
        ancillaryDepot.storeTimedAncillary("LEAPSEC",time,path);
    }

    /**
     * Get a LEAPSEC file for time nearest to LEAPSEC time.
     * The DSM copies the file to the local site (computer).
     * @param time The date and time of the UTCPOLE file.
     * @return The path name of the file or null if no file exists.
     */
    public String getLEAPSEC(java.util.Date time) throws Exception
    {
        return getTimedAncillary("LEAPSEC",time);
    }

    /**
     * Get the LEAPSEC file for time nearest to LEAPSEC time.
     * It uses the product's start time.
     * The DSM copies the file to the local site (computer).
     * @param product A product
     * @return The path name of the file or null if no file exists.
     */
    public String getLEAPSEC(Product product) throws Exception
    {
        return getLEAPSEC(product.getStartTime());
    }

    /**
     * Delete a time ancillary file reference that is on this site. (LEAPSEC and
     * UTCPOLE are time ancillary types.) It deletes the entry from the database,
     * but it does not delete the file. It does not affect any copy of this file
     * existing on other computers.
     * @param filename The name of the file without directory information.
     *          For example, "file.dat', not "dir/dir1/dir2/file.dat".
     * @return true if it worked and false if no file by that name exists in the
     *          database.
     */
    public boolean deleteTimedAncillary(String filename) throws Exception
    {
        return deleteAncillary(filename,"TimeAncillaries","TimeAncillarySites");
    }

    /**
     * Get a TLE file for the spacecraft and time nearest to the TLE time.
     * The DSM copies the file to the local site (computer).
     * @param time A date and time for the TLE
     * @param spacecraft The spacecraft name
     * @return The path name for the TLE file or null if no TLE
     *      for that spacecraft exists.
     */
    public String getTLE(java.util.Date time, String spacecraft) throws Exception
    {
        return getSatTimedAncillary("drl.tle", time, spacecraft);
    }

    /**
     * Get a TLE file for a product. It extracts the time and spacecraft from
     * the product.
     * The DSM copies the file to the local site (computer).
     * @param product A product
     * @return The path name for the TLE file or null if no TLE
     *      for that spacecraft exists.
     */
    public String getTLE(Product product) throws Exception
    {
        return getSatTimedAncillary("drl.tle",product.getStartTime(),product.getSpacecraft());
    }

    /**
     * Put a TLE file into the DSM database. The DSM assumes the TLE file is
     * on the local computer. It does not verify the file's existence.
     * @param time A date and time for the TLE
     * @param spacecraft The spacecraft name
     * @param path The path name of the TLE file
     */
    public void putTLE(java.util.Date time, String spacecraft, String path)
            throws Exception
    {
        ancillaryDepot.storeSatTimeAncillary("drl.tle",spacecraft,time,path);
    }

    /**
     * Delete a TLE file reference that is on this site. It deletes the entry
     * from the database, but it does not delete the file. It does not affect any copy
     * of this file existing on other computers.
     * @param filename The name of the file without directory information.
     *          For example, "file.dat', not "dir/dir1/dir2/file.dat".
     * @return true if it worked and false if no file by that name exists in the
     *          database.
     */
    public boolean deleteTLE(String filename) throws Exception
    {
        return deleteAncillary(filename,"SatTimeAncillaries","SatTimeAncillarySites");
    }

    /**
     * Delete an ancillary file reference that is on this site. It deletes the entry
     * from the database, but it does not delete the file. It does not affect any copy
     * of this file existing on other computers.
     * @param filename The name of the file without directory information.
     *          For example, "file.dat', not "dir/dir1/dir2/file.dat".
     * @return true if it worked and false if no file by that name exists in the
     *          database.
     */
    protected boolean deleteAncillary(String filename, String table, String siteTable)
            throws Exception
    {
        boolean ok = false;
        String qfilename = Utility.quote(filename);
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT id from " + table + " WHERE path=" + qfilename);
        if (r.next())
        {
            ok = true;
            String aid = r.getString(1);
            String qmysite = Utility.quote(getSite());
            Utility.executeUpdate(s, "DELETE FROM " + siteTable + " WHERE aid=" +
                    aid + " AND site=" + qmysite);
            Utility.commitConnection(dsmProperties.getConnection());
        }
        return ok;
    }

    /**
     * Get a single product from a SQL select statement that gets products.
     * It only returns the first one if more than one satisfies the statement.
     */
    protected Product queryProduct(String sqlQueryString) throws Exception
    {
        Product product = null;
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, sqlQueryString);
        if (r.next())
        {
            product = ProductFactory.makeProduct(dsmProperties.getConnection(),
						 thisSite,r);
        }
        s.close();
        return product;
    }

    /**
     * Deletes DSM records of a file 
     * Throws exceptions if it can't communicate to the database.
     * @param pathname String (must be an absolute pathname).
     * @param do_commit boolean (if true, really do the record deletion)
     * @return true if the file is safe to delete, false if it's not.
     */
    public boolean deleteFileRecord(String pathname, boolean do_commit)
	throws Exception
    {
	String ISsite = dsmProperties.getIS_Site();
	String ldd = dsmProperties.getLocalDataDirectory();
	File dFile;
	String dPath;
	String dName;

	// If we're on IS, and pathname begins with DSM_DATA_DIRECTORY,
	// clip it off
	if(thisSite.equals(ISsite)
	   && pathname.startsWith(ldd)) {
	    dFile = new File(pathname.substring(ldd.length() + 1));
	}
	else {
	    dFile = new File(pathname);
	}

	dPath = dFile.getParent();
	dName = dFile.getName();

	// The usual crap to ensure this statement gets closed...
	Statement stmt = dsmProperties.getConnection().createStatement();
	try {
	    // For us to be interested, the path has to be in Directories
	    String wdq =
		"SELECT id FROM Directories WHERE path = "
		+ Utility.quote(dPath);
	    // System.err.println("Dirq is " + wdq);
	    ResultSet rs = Utility.executeQuery(stmt, wdq);
	    int dirID;
	    if(rs.next())
		dirID = rs.getInt(1);
	    else
		return true;

	    // OK, we currently have three places to look:
	    // Products, TimeAncillaries and SatTimeAncillaries
	    // First Products
	    String finderq =
		"SELECT Resources.id , Products.deleteProtected, Products.id"
		+ " FROM Resources"
		+ " LEFT JOIN Products on Resources.product = Products.id"
		+ " JOIN ResourceSites ON ResourceSites.resource = Resources.id"
		+ " WHERE ResourceSites.site = " + Utility.quote(thisSite)
		+ " AND ResourceSites.directory = " + dirID
		+ " AND Resources.path = " + Utility.quote(dName);
	    // System.err.println("finderq is " + finderq);
	    rs = Utility.executeQuery(stmt, finderq);
	    HashSet<String> overID =
		deleteFileRecordGuts(do_commit, stmt, rs,
				     "ResourceSites", "resource",
				     "Resources");
	    if(overID != null) {
		// We deleted resources - check the Products
		// and maybe delete them
		for (String s : overID)
		    ProductFactory.checkProductResources(stmt, s);
		return true;
	    }

	    // Then TimeAncillaries (note the constant '' for overID)
	    finderq =
		"SELECT id, deleteProtected, '' FROM TimeAncillaries"
		+ " LEFT JOIN TimeAncillarySites"
		+ " ON TimeAncillaries.id = TimeAncillarySites.aid"
		+ " WHERE TimeAncillarySites.site = "
		+ Utility.quote(thisSite)
		+ " AND TimeAncillarySites.directory = " + dirID
		+ " AND TimeAncillaries.path="
		+ Utility.quote(dName);
	    // System.err.println("finderq is " + finderq);
	    rs = Utility.executeQuery(stmt, finderq);
	    overID = deleteFileRecordGuts(do_commit, stmt, rs,
					  "TimeAncillarySites", "aid",
					  "TimeAncillaries");
	    if(overID != null)
		return true;

	    // Then SatTimeAncillaries (note the constant '' for overID)
	    finderq =
		"SELECT id, deleteProtected, '' FROM SatTimeAncillaries"
		+ " LEFT JOIN SatTimeAncillarySites"
		+ " ON SatTimeAncillaries.id = SatTimeAncillarySites.aid"
		+ " WHERE SatTimeAncillarySites.site = "
		+ Utility.quote(thisSite)
		+ " AND SatTimeAncillarySites.directory = " + dirID
		+ " AND SatTimeAncillaries.path=" + Utility.quote(dName);
	    // System.err.println("finderq is " + finderq);
	    rs = Utility.executeQuery(stmt, finderq);
	    overID = deleteFileRecordGuts(do_commit, stmt, rs, 
					  "SatTimeAncillarySites", "aid",
					  "SatTimeAncillaries");
	    if(overID != null)
		return true;
	}
	finally {
	    stmt.close();
	}

	// If we get here, we can find no record of this file, anywhere
	// so we assume it's safe to delete
	return true;
    }

    /**
     * Default case is, yes, we want to commit.
     */
    public boolean deleteFileRecord(String pathname)
	throws Exception
    {
	return deleteFileRecord(pathname, true);
    }

    /**
     * Common boiler-plate inner loop of cleaning up resource/site tables
     * Shoots deleteable entries in site table, and cleans up main table
     * Returns NULL or a list of Strings representing the IDs of the overarching
     * database entities that care about these resources
     * (Product for Resources, nothing for ancillaries right now)
     */
    private HashSet<String> deleteFileRecordGuts(boolean do_commit,
						 Statement stmt, ResultSet rs,
						 String siteTable, String resourceCol,
						 String mainTable)
	throws Exception
    {
	HashSet<String> result = new HashSet<String>();
	// The usual crap where we walk through the result set storing
	// values, so we only have to traverse it once
	ArrayList<String> resID = new ArrayList<String>();
	// The result set has three entries per row:
	// (resourceID, deleteProtectInt, overIDString)
	// Collect the non-"" overIDs
	// If any of the entries are marked deleteProtected, punt now
	while(rs.next()) {
	    if(rs.getInt(2) != 0) {
		return null;
	    }
	    String overID = rs.getString(3);
	    if(!overID.equals(""))
		result.add(overID);
	    resID.add(rs.getString(1));
	}
	// Walk resID and delete
	for (String rid : resID) {
	    // Delete the ResourceSite entry
	    String deletestmt = "DELETE FROM " + siteTable
		+ " WHERE "+ resourceCol + " = " + rid
		+ " AND site= " + Utility.quote(thisSite);
	    Utility.executeUpdate(stmt, deletestmt);
	    // System.err.println("SQL is " + deletestmt);
	    // What the heck, may as well check for GC-able mainTable
	    // entries
	    deletestmt = "DELETE " + mainTable + " FROM " + mainTable
		+ " LEFT JOIN " + siteTable +" ON "
		+ siteTable + "." + resourceCol + " = " + mainTable + ".id"
		+ " WHERE " + mainTable + ".id = " + rid
		+ " AND " + siteTable + "." + resourceCol + " IS NULL";	    
	    Utility.executeUpdate(stmt, deletestmt);
	    // System.err.println("SQL is " + deletestmt);
	}
	// We just did something destructive - if we're supposed to commit,
	// do it now
	if(do_commit)
	    Utility.commitConnection(dsmProperties.getConnection());

	// If there was anything in resID, it's been deleted and...
	return (result.size() == 0 ? null : result);
    }

    /**
     * Removes entries from Markers table that may have been left over
     * from a previous run/crash of an NCS station.  Returns the number
     * of entries removed (0 or 1 in all but the weirdest cases).
     */
    public int removeOldMarkers(String pid)
	throws Exception
    {
	String oldLocation = getSite() + "-" + pid;
	String remstmt = "DELETE FROM Markers WHERE location = "
	    + Utility.quote(oldLocation)
	    + " AND status = 0";
	int result = 0;

	// The usual crap to ensure this statement gets closed...
	Statement stmt = dsmProperties.getConnection().createStatement();
	try {
	    result = Utility.executeUpdate(stmt, remstmt);
            Utility.commitConnection(dsmProperties.getConnection());
	}
	finally {
	    stmt.close();
	}
	return result;
    }

    /**
     * Returns a TimeWindow object, suitable for passing in to
     * getTimedAncillary or getSatTimeAncillary
     */
    public TimeWindow getTimeWindow(String pretime, String posttime,
				    String predate, String postdate)
	throws Exception
    {
	return new TimeWindow(pretime, posttime, predate, postdate);
    }

    /**
     * Add an entry in the Algorithms table.  Don't insert if it's
     * already there.
     */
    public void updateAlgorithms(String algoName, String gopherColony)
	throws Exception
    {
	/* There's got to be a simple way to do this without two queries.
	   All the ways I saw searching the net are complicated and not
	   quite what I need.
	   The Algorithms table is declared:
	
	   name VARCHAR(32) NOT NULL,
	   gopherColony VARCHAR(32) NOT NULL,
	   PRIMARY KEY (name),
	   UNIQUE (name, gopherColony)
	   
	   so it will at least throw an error if we try it.
	*/
	String qName =  Utility.quote(algoName);
	String qGopher =  Utility.quote(gopherColony);

	Statement stmt = dsmProperties.getConnection().createStatement();
	try {
	    ResultSet rs = Utility.executeQuery(stmt, "SELECT * from Algorithms where name=" + qName + " and gopherColony=" + qGopher);
	    if(!rs.next()) {
		Utility.executeUpdate(stmt, "insert into Algorithms values (" + qName + ", " + qGopher + ")");
		Utility.commitConnection(dsmProperties.getConnection());
	    }
	}
	finally {
	    stmt.close();
	}
    }

}
