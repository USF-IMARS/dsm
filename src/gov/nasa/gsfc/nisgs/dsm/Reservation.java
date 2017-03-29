/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * Reservation handles reserving and releasing products for the DSM.
 * It also contains logic to copy products from other sites to the local site.
 * It is not public. It exists to reduce the size of the DSM class.
 * @version 8/16/05 I set Markers.status=2 if I have an error fetching a product.
 *      made all reserveProducts blockers.
 * @version 3.19a Do a rollback in reserve() if the update failed.
 */
final public class Reservation
{
    private static final long TRANSFER_COMMAND_WAIT = 6000L;
    private Connection connection;
    private String mysite;
    private String issite;
    private String mypid;

    public void setPid(String pid)
    {
	mypid = pid;
    }

    /**
     * Stupid little utility function that takes a space-delimited
     * list of product types and returns an array of SQL-quoted
     * strings representing the types
     */
    private String[] splitquote(String otherTypes)
    {
	/* Would you believe, "".split(" +") gives you an array
	   with one "" entry, rather than a zero-size array?
	   I can see arguments both ways, but really prefer the
	   zero case for this. */
	
	String[] result;
	if(otherTypes.equals(""))
	    result = new String[] {};
	else {
	    result = otherTypes.split(" +");
	    for(int i=0; i < result.length; i++)
		result[i] = Utility.quote(result[i]);
	}
	return result;
    }

    Reservation(Connection c, String mysite, String issite)
    {
        connection = c;
        this.mysite = mysite;
	this.issite = issite;
    }

    /**
     * Release a reserved product. The DSM marks it as completed by this group.
     * @param qgroup A group name, quoted for a sql statement
     * @param product A product to be released
     * @param completionCode 1 is normal. 2 is an error.
     */
    
    void releaseProduct(String qgroup, Product product, int completionCode)
	throws Exception
    {
	releaseProduct(qgroup, product.getId(), completionCode);
    }

    private void releaseProduct(String qgroup, String productID, int completionCode)
            throws Exception
    {
        StringBuffer sb = new StringBuffer(256);
        sb.append("UPDATE LOW_PRIORITY Markers SET status=");
        sb.append(completionCode);
        sb.append(" WHERE product=");
        sb.append(productID);
        sb.append(" AND gopherColony=");
        sb.append(qgroup);
        Statement statement = connection.createStatement();
        Utility.executeUpdate(statement, sb.toString());
        Utility.commitConnection(connection);
        statement.close();
    }

    /**
     * Reserve a product of type productType that is unmarked by this group.
     * This method marks the product with this group name. It blocks.
     * @param myqroup a group name, unquoted
     * @param productType a product type
     * @param timeSleep The time in seconds between polls. It must be > 0.
     * @return a product.
     */
    Product reserveProduct(String mygroup, String productType, String otherTypes, int timeSleep)
            throws Exception
    {
	final String qgroup = Utility.quote(mygroup);
	final String whereCompare = "=";
	final String qproductType = Utility.quote(productType);
	final String qotherType[] = splitquote(otherTypes);

	// Before we start polling, do a sanity check on the types
	checkTypes(connection, qproductType, qotherType, whereCompare);

	// Go off and poll
	return poll(
		    new Pollable() {
			public Product pollForProduct() throws Exception
			{
			    return reserve(qgroup,
					   qproductType,
					   qotherType,
					   whereCompare);
			}
		    },
		    (long)timeSleep);
    }

    /**
     * Reserve a product that is unmarked by this group in which the product's
     * type matches the product type mask. This method marks the product with
     * this group name. It blocks.
     * @param mygroup A group name
     * @param productTypeMask A product type search pattern in SQL format
     *      Use SQL pattern matching syntax. SQL pattern matching allows you
     *      to use `_' to match any single character and `%' to match an arbitrary
     *      number of characters (including zero characters). SQL patterns are
     *      case-insensitive by default.
     * @param otherTypes A space-delimited string of patterns for the other
     *      types being reserved.  May contain {var} substrings that must
     *      be mapped to '%' for SQL.
     * @param timeSleep The time in seconds between polls. It must be > 0.
     * @return A product
     */
	Product reserveProductLikeProductType(
		String mygroup,
		String productTypeMask,
		String otherTypes,
		int timeSleep
	) throws Exception {
//		System.out.println("reserveProductLikeProductType(" + mygroup+","+productTypeMask+","+otherTypes+")");
		final String qgroup = Utility.quote(mygroup);
		final String whereCompare = "LIKE";
		final String qproductTypeMask = Utility.quote(productTypeMask);
		final String qotherType[] =  splitquote(otherTypes.replaceAll("\\{[^}]*}","%"));

		// Before we start polling, do a sanity check on the types
		checkTypes(connection, qproductTypeMask, qotherType, whereCompare);

		// Go off and poll
		return poll(
			new Pollable() {
				public Product pollForProduct() throws Exception
				{
					return reserve(
						qgroup,
						qproductTypeMask,
						qotherType,
						whereCompare
					);
				}
			},
			(long)timeSleep
		);
	}


    /**
     * Reserve a product of type productType that is unmarked by this group,
     * and has existing products temporally before and after it.
     * This method marks the product with this group name. It blocks.
     * @param myqroup a group name, unquoted
     * @param productType a product type
     * @param otherTypes  A space-separated list of other product types
     * @param granuleDuration  The duration of a granule in seconds; the interval of time before and after the current product that is considered when looking for pre and post data.
     * @param prePostCount  The number of intervals before and after the current product that are checked.  Often defaulted to 1 by callers in the Product class.
     * @param timeSleep The time in seconds between polls. It must be > 0.
     * @return a product.
     */
    Product reserveProduct(String mygroup, String productType, String otherTypes, 
			   final double granuleDuration,
			   final int prePostCount,
			   int timeSleep)
	throws Exception
    {
	final String qgroup = Utility.quote(mygroup);
	final String whereCompare = "=";
	final String qproductType = Utility.quote(productType);
	final String qotherType[] = splitquote(otherTypes);

	// Before we start polling, do a sanity check on the types
	checkTypes(connection, qproductType, qotherType, whereCompare);

	// Go off and poll
	return poll(
		    new Pollable() {
			public Product pollForProduct() throws Exception
			{
			    return reserve(qgroup,
					   qproductType,
					   qotherType,
					   granuleDuration,
					   prePostCount,
					   whereCompare);
			}
		    },
		    (long)timeSleep);
    }

    /**
     * Reserve a product that is unmarked by this group in which the product's
     * type matches the product type mask. This method marks the product with
     * this group name. It blocks.
     * @param mygroup A group name
     * @param productTypeMask A product type search pattern in SQL format
     *      Use SQL pattern matching syntax. SQL pattern matching allows you
     *      to use `_' to match any single character and `%' to match an arbitrary
     *      number of characters (including zero characters). SQL patterns are
     *      case-insensitive by default.
     * @param otherTypes A space-delimited string of patterns for the other
     *      types being reserved.  May contain {var} substrings that must
     *      be mapped to '%' for SQL.
     * @param timeSleep The time in seconds between polls. It must be > 0.
     * @return A product
     */
	Product reserveProductLikeProductType(
		String mygroup,
		String productTypeMask,
		String otherTypes,
		final double granuleDuration,
		final int prePostCount,
		int timeSleep
	) throws Exception {
//		System.out.println("reserveProductLikeProductType("
//				+ mygroup+","
//				+ productTypeMask+","
//				+ otherTypes+","
//				+ Double.toString(granuleDuration)+","
//				+ Integer.toString(prePostCount)+")"
//		);
		final String qgroup = Utility.quote(mygroup);
		final String whereCompare = "LIKE";
		final String qproductTypeMask = Utility.quote(productTypeMask);
		final String qotherType[] =  splitquote(otherTypes.replaceAll("\\{[^}]*}","%"));

		// Before we start polling, do a sanity check on the types
		checkTypes(connection, qproductTypeMask, qotherType, whereCompare);

		// Go off and poll
		return poll(
			new Pollable() {
				public Product pollForProduct() throws Exception
				{
					return reserve(
						qgroup,
						qproductTypeMask,
						qotherType,
						granuleDuration,
						prePostCount,
						whereCompare
					);
				}
			},
			(long)timeSleep
		);
	}

    /**
     * Get a product by its product ID.
     */
    Product getProduct(String productID) throws Exception
    {
        Product product = null;
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Products WHERE id=" + productID);
        if (r.next())
        {
            product = ProductFactory.makeProduct(connection,mysite,r);
        }
        s.close();
        return product;
    }

    /**
     * Copy a product from another site (computer) to this site (computer).
     * It waits until the operation is complete. To save time, you should not call
     * this method unless you know the resources are not local. You can test this
     * by making a Product and testing resourcesAreLocal().
     * @return a Product object with local resources or null on failure.
     */
    Product copyProduct(String productID) throws Exception
    {
        Statement s = connection.createStatement();
        String today = Utility.format(new java.util.Date());
        String sql = "INSERT LOW_PRIORITY INTO TransferCommands VALUES (DEFAULT,\'Products\'," +
                productID + Utility.COMMA + Utility.quoteComma(mysite) +
                Utility.quoteComma(today) + "0)";
        Utility.executeUpdate(s, sql);
        Utility.commitConnection(connection);
        String id = Utility.getLastAutoIncrementedValue(s);
        int complete = 0;
        do
        {
            try { Thread.sleep(TRANSFER_COMMAND_WAIT); }
            catch ( InterruptedException e) {}
            ResultSet r = Utility.executeQuery(s, "SELECT complete FROM TransferCommands WHERE id=" + id);
            if (r.next())
            {
                complete = r.getInt(1);
            }
        }
        while (complete == 0);
        s.close();
        return (complete == 1)? getProduct(productID) : null;
    }

    /**
     * Sanity check before we poll - want to ensure that the product types
     * called for exist.  It gets a little complex when there are wild cards
     * in the types - sorry about that...
     */

    static public void checkTypes(Connection connection,
			   String productType, String[] otherType,
			   String whereCompare)
	throws Exception
    {
	// We want to do an SQL query of the form
	// SELECT pt.name, pt0.name...
	// FROM pt as ProductTypes, pt0 as ProductTypes...
	// WHERE pt.name = $productType
	// AND pt0.name = $otherType[0] ...
	Statement s = null;
	String sql = null;
	int i;
	try {
	    s = connection.createStatement();

	    if (whereCompare.equals("=")) {
		sql = "SELECT pt.name";
		for(i=0; i < otherType.length; i++)
		    sql += ", pt" + i + ".name";

		sql += " FROM ProductTypes AS pt";
		for(i=0; i < otherType.length; i++)
		    sql += ", ProductTypes AS pt" + i;

		sql += " WHERE pt.name = " + productType;
		for(i=0; i < otherType.length; i++)
		    sql += " AND pt" + i + ".name = " + otherType[i];

		ResultSet rs = Utility.executeQuery(s, sql);
		if(!rs.next())
		    throw new Exception("One of these Product Types does not exist: " + productType + Arrays.deepToString(otherType));
	    }
	    else if (whereCompare.equals("LIKE")) {
		// This one's nasty, as we want to do a LIKE query on the
		// primary productType, then pick it apart and check the
		// other types.
		sql = "SELECT name from ProductTypes WHERE name LIKE "
		    + productType;
		ResultSet rs = Utility.executeQuery(s, sql);
		// Drain the query into a List of Strings (to keep
		// the SQL interface happy so we don't hold the
		// connection open while we do other things).
		ArrayList<String> ptype = new ArrayList<String>();
		while(rs.next()) {
		    ptype.add(rs.getString(1));
		}
		// We need a regular expression to snag the subtype.  This is
		// OK because we only handle exactly one wildcard...
		String typeSnag = productType.replace("%", "(\\S+)");
		// How do we specify that the match must be exact?
		Pattern tsp = Pattern.compile(typeSnag);
		for(String ctype : ptype) {
		    // The ctype strings are plain, whereas the original
		    // type string is quoted, so...
		    Matcher m = tsp.matcher(Utility.quote(ctype));
		    // This ought to be "rare" so we "log" it...
		    if(!m.matches())
			System.err.println(typeSnag + " didn't match " + Utility.quote(ctype));
		    String wildType = m.group(1);
		    m.reset();
		    // Use wildType to munge the rest of the strings,
		    // exit with success if we do a query and it works...
		    sql = "SELECT pt.name";
		    for(i=0; i < otherType.length; i++)
			sql += ", pt" + i + ".name";

		    sql += " FROM ProductTypes AS pt";
		    for(i=0; i < otherType.length; i++)
			sql += ", ProductTypes AS pt" + i;

		    sql += " WHERE pt.name = "
			+ productType.replace("%", wildType);
		    for(i=0; i < otherType.length; i++)
			sql += " AND pt" + i + ".name = "
			    + otherType[i].replace("%", wildType);
		    rs = Utility.executeQuery(s, sql);
		    if(rs.next())
			return;
		}
		// If we get here, we looped down all the types
		// and didn't find a set in ProductTypes we liked
		throw new Exception("One of these Product Types does not exist: " + productType + Arrays.deepToString(otherType));
	    }
	    else
		throw new Exception("Invalid whereCompare: " + whereCompare);
	}
	finally {
	    s.close();
	}
    }

    /**
     * Inner otherProduct testing function used by reserve() methods below.
     * Takes a Pass ID and a set of Product types; runs a query and returns
     * a ResultSet of Product IDs.  The result set will be empty if some of
     * the Products don't currently exist.  We return NULL for the result set
     * if the database query times out.
     */

	private ResultSet getOtherProductSet(Statement s, String thePass, String qproductType, String[] otherType, String whereCompare)
	throws Exception
	{
	/* The inner query is of the form:
		SELECT p.id, p0.id, ... ,pN.id
		FROM Products AS p, Products AS p0, ... Products as pN
		WHERE p.pass = "thePass" AND p.productType = theProd
		AND p0.pass = "thePass" AND p0.productType = "productType" ...
		AND pN.pass = "thePass" AND pN.productType = "productType"

		which should emit zero or several rows of products to check
	*/
		int i;
		String qthePass = Utility.quote(thePass);
		String rsql = "SELECT p.id ";
		for(i=0; i < otherType.length; i++)
			rsql += ", p" + i + ".id ";
		rsql += "FROM Products as p ";
		for(i=0; i < otherType.length; i++)
			rsql += ", Products as p" + i + " ";
		rsql += "WHERE p.pass = " + qthePass
			+ " AND p.productType " + whereCompare + " " + qproductType;
		for(i=0; i < otherType.length; i++)
			rsql += " AND p" + i + ".pass = " + qthePass
				+ " AND p" + i + ".productType " + whereCompare + " " + otherType[i];

		/* This query has been known to block for a LONG time, so
			we run it in a subthread with a timer on, using the usual
			verbose Java Way (tm) (See DSM.fetchProduct()) */
		class TimedQuery extends Thread
		{
			String qs;
			Statement s;
			TimedQuery(String qs, Statement s) {
				this.qs = qs; this.s = s;
			}
			public Exception exception = null;
			public ResultSet rs = null;
			public void run() {
				try {
					rs = Utility.executeQuery(s, qs);
				}
				catch (Exception e) {
					exception = e;
				}
			}
		};
		TimedQuery tq = new TimedQuery(rsql, s);
		tq.start();
		/* 300 seconds ought to be long enough */
		tq.join(300 * 1000);
		if(tq.isAlive()) {
			tq.interrupt();
			/* Log the error and return NULL */
			System.err.println("Reservation query timed out:" + rsql);
			return null;
		}
		if(tq.exception != null)
			throw tq.exception;

		return tq.rs;
	}

    /**
     * Interface used by polling method below.  Lambdas would make this
     * stuff go away, but that's for newer versions of Java...
     */
    interface Pollable {
	public Product pollForProduct() throws Exception;
    }

    private Product poll(Pollable pp, long timeSleep) throws Exception
    {

        Product product = null;
        do
        {
            product = pp.pollForProduct();
            if (product == null)
            {
                try { Thread.sleep(timeSleep); }
                catch (InterruptedException ie) {}
            }
        }
        while (product == null);
        return product;
    }
	
    private Product poll(String qgroup,
			 String qproductType, String[] otherType,
			 String whereCompare,long timeSleep) throws Exception
    {
	// Before we start polling, do a sanity check on the types
	//checkTypes(connection, qproductType, otherType, whereCompare);

        Product product = null;
        do
        {
            product = reserve(qgroup,qproductType,otherType,whereCompare);
            if (product == null)
            {
                try { Thread.sleep(timeSleep); }
                catch (InterruptedException ie) {}
            }
        }
        while (product == null);
        return product;
    }


	/** returns true if given product has resources
	 */
	private boolean hasResources(String productID, Statement statement) throws SQLException{
		String scanquery =
				"SELECT Resources.id FROM Resources "
						+ " LEFT JOIN ResourceSites"
						+ " ON Resources.id = ResourceSites.resource"
						+ " WHERE Resources.product="+ productID
						+ " AND Resources.rkey='DATA'"
						+ " AND (Resources.published <> 0"
						+ " OR ResourceSites.site=" + Utility.quote(mysite)
						+ ")";
		;

		ResultSet scanset = Utility.executeQuery(statement, scanquery);
		if(!scanset.next()) {
			return false;
//			System.out.println("Reservation.reserve: " + productID
//					+ " has no accessible DATA resource; skipping.");
		} else {
			return true;
		}
	}

	/** actually does the product reserving
	 */
	private boolean markProduct(String pid, String qgroup, Statement s) throws SQLException{
		// Everything looks OK, mark the product
		String isql =
				"INSERT INTO Markers VALUES ("
						+ pid + ", " + qgroup + ", 0,"
						+ Utility.quote(mysite + "-" + mypid) + ")";
		try {
			Utility.executeUpdate(s, isql);
			Utility.commitConnection(connection);
		}
		catch (SQLException se) {
			// This is not really a satisfactory response here,
			// but what can you do?  We tried to write the database...
			System.err.println("ERR: could not mark product in DB w/ query : \n" + isql + "\n" + se.toString());
			connection.rollback();
			return false;
		}
		return true;
	}

	/** Reserves the first product from the Products table that matches the given queryProductType and has not been
	 * marked as processed by the given group.
	 */
	private Product reserve(
			String queryGroup,
			String queryProductType,
			String[] otherTypes,
			String whereCompare
	) throws Exception{
		Statement statement = connection.createStatement();
		// select products which have not been marked processed by my group matching queryProductType or otherTypes
		ArrayList<String> typesToReserve = new ArrayList<>();
		typesToReserve.add(queryProductType);
		typesToReserve.addAll(Arrays.asList(otherTypes));

		try {
			for (String reserveType : typesToReserve) {
				// NOTE: is pass needed in the query below? Only if ids are not unique across multiple passes.
				String sqlQuery = "SELECT DISTINCT id, pass FROM Products WHERE Products.productType LIKE " + reserveType + " AND Products.id NOT IN ( " +
						" SELECT product from Markers WHERE Markers.gopherColony = " + queryGroup + ")";
				System.out.println(sqlQuery);
				ResultSet queryResult = Utility.executeQuery(statement, sqlQuery);
				// copy over result now so the query doesn't close before we're done.
				ArrayList<String> passIDs = new ArrayList<>();
				while (queryResult.next()) {  // TODO: should this be do-while or does the iterator need to be primed?
					passIDs.add(queryResult.getString(1));  // index starts @ 1
				}
				// handle the results:
				for (String passID : passIDs){
					System.out.print('\n'+passID);
					if (hasResources(passID, statement)) {
						System.out.print(" hasRes");
						if (markProduct(passID, queryGroup, statement)) {
							System.out.print(" marked");
							// Create the Product objects and drag
							// their resources to the local machine
							try {
								ResultSet r = Utility.executeQuery(statement, "SELECT Products.* from Products where Products.id=" + passID);
								r.next();
								Product result = ProductFactory.makeProduct(connection, mysite, r);
								System.out.print(" made");

								// copy resources to local if needed
								if (!result.resourcesAreLocal()) {
									result = copyProduct(passID);
									System.out.print(" copied");
								}

								if (result == null) {
									//probably files are gone.
									throw new AssertionError("null product encountered");
								} else {
									Utility.commitConnection(connection);
									System.out.print(" returned");
									return result;
								}

							} catch (SQLException | AssertionError se) {
								System.out.println("\n ERR: " + se.toString());
								releaseProduct(queryGroup, passID, 2);
							}
						}
					}
				}
			}
		} finally {
			statement.close();
		}
		return null;
	}

    /** Does the dirty work of reserving a product as follows:
     *
     * Does a join query against the Products and Markers tables
     * to create a candidate list of products that need processing
     *
     * Loops down the candidate list and:
     *
     * * Checks for existence of previous/next Products
     *    (failure means skip me
     *
     * * Checks for existence of products in otherType list
     *    (failure means skip me)
     *
     * * Inserts a grabbed record in the Markers table
     *    (failure means skip me)
     *
     * * Creates a Product object for the grabbed product
     *    (failure means mark as failed in Markers and skip
     *
     * * Forces Product resources to be local
     *    (failure means mark as failed in Markers and skip)
     *
     */

	private Product reserve(
			String qgroup,
			String qproductType, String[] otherType,
			double granuleDuration,
			int prePostCount,
			String whereCompare) throws Exception
	{
		Product result = null;
		Statement s = connection.createStatement();
		try {
			/* The first query is of the form:
				SELECT DISTINCT pass, id from Products
				LEFT JOIN Markers ON Products.id = Markers.product
				AND Markers.gopherColony = "qgroup"
				WHERE Markers.gopherColony IS NULL
				AND Products.productType = "productType"

				It generates a list of passes that have products that do not have
				the appropriate Markers table entry.  If this comes up empty,
				we'll do the next loop zero times and punt immediately (yay!) */
			String rsql =
				"SELECT DISTINCT pass, id from Products"
				+ " LEFT JOIN Markers ON Products.id = Markers.product"
				+ "  AND Markers.gopherColony = " + qgroup
				+ " WHERE Markers.gopherColony IS NULL"
				+ " AND Products.productType " + whereCompare + " " + qproductType;
			//System.err.println("QUERY1: " + rsql);
			ResultSet rset = Utility.executeQuery(s, rsql);
			// Drain the query into a List of Strings
			// (to keep the SQL interface happy so we don't hold the
			// connection open while we do other things).
			ArrayList<String> passList = new ArrayList<String>();
			ArrayList<String> prodList = new ArrayList<String>();
			while(rset.next()) {
				passList.add(rset.getString(1));
				prodList.add(rset.getString(2));
			}
			//System.err.println("Result count: " + passList.size());
			// Commit now to create a clean rollback point if we need it
			Utility.commitConnection(connection);

			// Loop down the passes (and products in parallel, which means
			// we get to use exposed, unadorned iterators... *sigh*...
			Iterator<String> passIT = passList.iterator();
			Iterator<String> prodIT = prodList.iterator();

			nextset:
			while(passIT.hasNext() && prodIT.hasNext()) {
				String thePass = passIT.next();
				String theProd = prodIT.next();

				// Try to grab the entry in the Markers table.
				// An atomic "grab if it isn't already there" is of the form:
				//
				// INSERT INTO Markers(product, gopherColony, status, location)
				// SELECT 5, "VIIRS-SDR grp1", 0, "thevoid" FROM Mutex
				// LEFT OUTER JOIN Markers
				// ON Markers.product=5
				//    AND Markers.gopherColony="VIIRS-SDR grp1"
				// WHERE Mutex.i=1 AND Markers.product IS NULL
				//                 AND Markers.gopherColony IS NULL;
				//
				// (This incantation courtesy of:
				// http://www.xaprb.com/blog/2005/09/25/insert-if-not-exists-queries-in-mysql/)
				// A successful grab returns 1, a miss returns 0,
				// which means it's likely another entity already has this
				// reservation taken, and we can just skip to the next one
				String gsql = "INSERT INTO Markers (product, gopherColony, status, location)"
					+ " SELECT " + theProd + ", " + qgroup + ", 0, " + Utility.quote(mysite + "-" + mypid) + " FROM Mutex"
					+ " LEFT OUTER JOIN Markers"
					+ " ON Markers.product=" + theProd
					+ " AND Markers.gopherColony=" + qgroup
					+ " WHERE Mutex.i=1 AND Markers.product IS NULL AND Markers.gopherColony IS NULL";


				try {
					//System.err.println("Grabbing marker with " + gsql);
					int gresult = Utility.executeUpdate(s, gsql);
					if(gresult == 0)
						// Someone else has it; let them do it
						continue nextset;
					if(gresult != 1) {
						// Holy @%*% - we may have destroyed the Markers
						// table - roll back and continue
						System.err.println("Attempt to grab reservation ("
							+ theProd + ", " + qgroup
							+ ") failed with an update result of "
							+ gresult);
						System.err.println("Rolling back and skipping!");
						connection.rollback();
						continue nextset;
					}
					//System.err.println("Got it");
					// The current entry is grabbed.  If we discover that it
					// isn't ready after all, we must delete it and continue
					// to the next one.  It appears to be sufficient to
					// rollback() and continue, as rollback() aborts the
					// current transaction and implicitly starts another one.

					// Storage for all product IDs we're going to check
					ArrayList<String> allpids = new ArrayList<String>();

					// First check for the other products in the primary
					rset = getOtherProductSet(s, thePass, qproductType, otherType, whereCompare);
					if(rset == null) {
						// This only happens with a pretty strange database error.
						// Try to rollback and give up
						connection.rollback();
						return null;
					}

					// If this result set is empty, skip to the next pass at once
					if(!rset.next()) {
						//System.err.println("Other products missing");
						connection.rollback();
						continue nextset;
					}

					// Otherwise, hose the product IDs into allpids
					for(int i= 0; i <= otherType.length; i++)
						allpids.add(rset.getString(i+1));

					// Then check for other products in the prev and next
					for(int neighbor = 0; neighbor < prePostCount; neighbor++) {
						// The time offset we need is
						// (neighbor + 0.5) * granuleDuration,
						// and we need to format it as
						// seconds.microseconds
						// because date math in MySQl is funky...
						String timeOffset = String.format("%.06f", granuleDuration * (neighbor + 0.5));
						// First get the ids for the previous and next products
						// (skip if they don't exist)
						// Query is of the form:
						// SELECT prevprod.id, pp.id, nextprod.id
						// FROM Products as prevprod, Products as nextprod, Products as pp
						// WHERE pp.id="theProd"
						// AND pp.productType = prevprod.productType
						// AND pp.productType = nextprod.productType
						// AND prevprod.startTime < DATE_SUB(pp.startTime, INTERVAL 5.000000 SECOND_MICROSECOND)
						// AND prevprod.stopTime > DATE_SUB(pp.startTime, INTERVAL 5.000000 SECOND_MICROSECOND)
						// AND nextprod.startTime < DATE_ADD(pp.stopTime, INTERVAL 5.000000 SECOND_MICROSECOND)
						// AND nextprod.stopTime > DATE_ADD(pp.stopTime, INTERVAL 5.000000 SECOND_MICROSECOND);
						//
						String psql =
							"SELECT prevprod.id, pp.id, nextprod.id, prevprod.pass, nextprod.pass"
							+ " FROM Products as prevprod, Products as nextprod, Products as pp"
							+ " WHERE pp.id = " + theProd
							+ " AND pp.productType = prevprod.productType"
							+ " AND pp.productType = nextprod.productType"
							+ " AND prevprod.startTime < DATE_SUB(pp.startTime, INTERVAL " + timeOffset + " SECOND_MICROSECOND)"
							+ " AND  prevprod.stopTime > DATE_SUB(pp.startTime, INTERVAL " + timeOffset + " SECOND_MICROSECOND)"
							+ " AND  nextprod.startTime < DATE_ADD(pp.stopTime, INTERVAL " + timeOffset + " SECOND_MICROSECOND)"
							+ " AND  nextprod.stopTime > DATE_ADD(pp.stopTime, INTERVAL " + timeOffset + " SECOND_MICROSECOND)";
						rset =  Utility.executeQuery(s, psql);
						if(!rset.next()) {
							//System.err.println("Prev/next products missing");
							connection.rollback();
							continue nextset;
						}

						String thePrevprod = rset.getString(1);
						String theNextprod = rset.getString(3);
						String thePrevpass = rset.getString(4);
						String theNextpass = rset.getString(5);

						// Then check for the previous/current/next product sets

						// First the next one
						rset = getOtherProductSet(s, theNextpass, qproductType, otherType, whereCompare);
						if(rset == null)
							return null;

						// If this result set is empty, skip to the next pass at once
						if(!rset.next()) {
							//System.err.println("Next products missing");
							connection.rollback();
							continue nextset;
						}
						// Hose the pids out into a List again
						for(int i= 0; i <= otherType.length; i++)
							allpids.add(rset.getString(i+1));

						// Then the previous one
						rset = getOtherProductSet(s, thePrevpass, qproductType, otherType, whereCompare);
						if(rset == null) {
							// This only happens with a pretty strange database error.
							// Try to rollback and give up
							connection.rollback();
							return null;
						}

						// If this result set is empty, skip to the next pass at once
						if(!rset.next()) {
							//System.err.println("Prev products missing");
							connection.rollback();
							continue nextset;
						}
						// Hose the pids out into a List again
						for(int i= 0; i <= otherType.length; i++)
							allpids.add(rset.getString(i+1));

					}
					// Scan all the products, and screech if any of them
					// has no accessible DATA resources.  An accessible resource
					// is either marked as published, or on our site.
					// Most of the time these "failures" are transient - a product
					// has been created, but not published yet, so we just
					// quietly skip them

					for(String productID : allpids ) {
						String scanquery =
							"SELECT Resources.id FROM Resources "
							+ " LEFT JOIN ResourceSites"
							+ " ON Resources.id = ResourceSites.resource"
							+ " WHERE Resources.product="+ productID
							+ " AND Resources.rkey='DATA'"
							+ " AND (Resources.published <> 0"
							+ " OR ResourceSites.site=" + Utility.quote(mysite)
							+ ")";
						;

						ResultSet scanset = Utility.executeQuery(s, scanquery);
						if(!scanset.next()) {
							System.err.println("Reservation.reserve: " + productID
								+ " has no accessible DATA resource?!");
							connection.rollback();
							continue nextset;
						}
					}


					// Create the Product objects and drag
					// their resources to the local machine

					try {
						ResultSet r = Utility.executeQuery(s, "SELECT Products.* from Products where Products.id=" + theProd);
						r.next();
						result = ProductFactory.makeProduct(connection,mysite,r);
						if (!result.resourcesAreLocal()) {
							Product xproduct = copyProduct(theProd);
							if (xproduct == null) {
								//probably files are gone.
								releaseProduct(qgroup, theProd, 2);
								continue;
							}
							result = xproduct;
						}
					}
					catch (SQLException se) {
						releaseProduct(qgroup, theProd, 2);
					}
					// If we get here, the current productID and all its friends
					// have DATA resources, it has been put into the Markers table,
					// and it has been made a local resource, so we should
					// exit the loop and return it now
					Utility.commitConnection(connection);
					return result;
				}
				catch (Exception e) {
					// We get here if some error happened while validating
					// the current entry.  We should mark this entry as failed
					// (and go on to the next one?)
					System.err.println("ERROR while trying to reserve with " + gsql);
					e.printStackTrace();
					releaseProduct(qgroup, theProd, 2);
					continue nextset;
				}
			}
		}
		finally {
			s.close();
		}
		return result;
	}
}
