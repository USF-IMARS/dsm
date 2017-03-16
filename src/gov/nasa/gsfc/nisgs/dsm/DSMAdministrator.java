/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.sql.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * DSMAdministrator is an interface class to the DSM subsystem and database.
 * Only DSM agents, servers, and administrative tools should use it.
 * It needs mysql-jdbc.jar.
 * @version 3.0 Added support for transfer commands and rollbacks.
 */
public final class DSMAdministrator extends DSM
{
    /**
     * Construct the DSMAdministrator object.
     * It establishes a network connection to the database server.
     * It uses the site and host information passed as properties. See
     * the corresponding DSM constructor for more information.
     * @param myGroup Each program using the DSM should have a unique group
     *      name. The DSM uses the group name to mark products so as to keep
     *      track of which ones this program has already processed. See
     *      reserveProduct(). The DSM does not enforce the uniqueness of a
     *      group name.
     * @param myName Each instance of this program should have a unique name
     *      to distinguish it from other instances in the same group. The DSM
     *      does not enforce uniqueness.
     */
    public DSMAdministrator(String myGroup, String myName) throws Exception
    {
        super(myGroup,myName);
    }

    /**
     * Construct the DSMAdministrator object.
     * It establishes a network connection to the database server.
     * @param mySite A name for the computer on which this program is running.
     *      All DSM-users on the same computer must use the same site name.
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
    public DSMAdministrator(String mySite, String myGroup, String myName,
            String dbhost) throws Exception
    {
        super(mySite,myGroup,myName,dbhost,"dsm","b28c935");
    }

    public Connection getConnection()
    {
	return dsmProperties.getConnection();
    }

    /**
     * A generic database select method used by DSM agents only.
     */
    public ResultSet query(String sqlString) throws SQLException
    {
        Statement s = dsmProperties.getConnection().createStatement();
        return Utility.executeQuery(s, sqlString);
    }

    /**
     * A generic database update method used by DSM agents only.
     * It does not commit updates.
     */
    public int update(String sqlString) throws SQLException
    {
	int result = 0;
        Statement s = dsmProperties.getConnection().createStatement();
	try {
	    result = Utility.executeUpdate(s, sqlString);
	}
	finally {
	    s.close();
	}
	return result;
    }

    /**
     * A generic database method to commit updates used by DSM agents only.
     */
    public void commit() throws SQLException
    {
        Utility.commitConnection(dsmProperties.getConnection());
    }

    /**
     * A generic database method to rollbacks updates used by DSM agents only.
     */
    public void rollback() throws SQLException
    {
        dsmProperties.getConnection().rollback();
    }

    /**
     * Store a product in the database with a specified site name.
     * This allows agents to register products for other computers.
     * The resources must reside on the target site, and the directory
     * names must be valid on the site.
     * @param xsite a valid site name
     * @param product a reference to a Product object.
     * @return the product ID for this product, which is its primary key.
     */
    public String storeProduct(String xsite, Product product) throws Exception
    {
        return ProductStore.store(dsmProperties.getConnection(),product,xsite,getUser(), true);
    }

    /**
     * Get a list of products that were put into the database by the named agent.
     */
    public java.util.List<Product> getProductsByAgent(String agent)
            throws Exception
    {
        String sql = "SELECT * FROM Products WHERE agent=" + Utility.quote(agent);
        return queryProducts(sql);
    }

    /**
     * Create a new product type and add it to the ProductTypes table.
     * @param productType a reference to a ProductType object.
     */
    public void createProductType(ProductType productType) throws Exception
    {
        ProductTypes.createProductType(dsmProperties.getConnection(),productType);
    }

    /**
     * Update a type in the ProductTypes table.
     * If the type is not in the table, it creates a new entry.
     * @param productType a reference to a ProductType object.
     */
    public void updateProductType(ProductType productType) throws Exception
    {
        ProductTypes.updateProductType(dsmProperties.getConnection(),productType);
    }

    /**
     * Delete a product type from the ProductType table.
     * @param name a reference to a ProductType object.
     */
    public void deleteProductType(String name) throws Exception
    {
        ProductTypes.deleteProductType(dsmProperties.getConnection(),name);
    }

    /**
     * Get an array of product types. This includes product types that are
     * defined in the Products table but do not have an entry in the
     * ProductTypes table. Each product type contains its keyword list.
     * Undefined product types will be inside ProductType objects but with
     * all fields except name set to null.
     */
    public ProductType[] getProductTypes() throws Exception
    {
        return ProductTypes.getProductTypes(dsmProperties.getConnection());
    }

    /**
     * Get a list of all passes.
     */
    public java.util.List<Pass> getPasses() throws Exception
    {
        Statement s = dsmProperties.getConnection().createStatement();
        java.util.List<Pass> list = new java.util.LinkedList<Pass>();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Passes");
        while (r.next())
        {
            list.add(new Pass(r));
        }
        s.close();
        return list;
    }

    /**
     * Create a new station and add it to the Stations table. A station entry
     * contains more descriptive information about a station. A product does
     * not require it.
     * @param station a reference to a Station object.
     */
    public void createStation(Station station) throws Exception
    {
        Stations.createStation(dsmProperties.getConnection(),station);
    }

    /**
     * Delete a station record from the Stations table. This has no effect on
     * products that refer to stations. Those references will simply have no
     * information in the Stations table.
     * @param name station name
     */
    public void deleteStation(String name) throws Exception
    {
        Stations.deleteStation(dsmProperties.getConnection(),name);
    }

    /**
     * Get a list of stations.
     */
    public Station[] getStations() throws Exception
    {
        return Stations.getStations(dsmProperties.getConnection());
    }

    /**
     * Update an ancillary site table with a new ancillary file location.
     * This method is used when an ancillary file is copied to another computer.
     */
    public void insertAncillarySiteUpdate(String siteTable, String site,
            String directoryPath, String ancillaryId, boolean doCommit) throws Exception
    {
        String dirId = Utility.getDirectoryId(dsmProperties.getConnection(),directoryPath,false);
        String sql = "INSERT INTO " + siteTable + " (aid,site,directory) VALUES (" +
                ancillaryId + "," + Utility.quoteComma(site) + dirId + ")";
        Statement statement = dsmProperties.getConnection().createStatement();
        Utility.executeUpdate(statement, sql);
        if (doCommit) commit();
        statement.close();
    }

    /**
     * Update a resource site table with a new resource file location.
     * This method is used when a resource is copied to another computer.
     */
    public void insertResourceSiteUpdate(String site, String directoryPath,
            String resourceId, boolean doCommit) throws Exception
    {
        String dirId = Utility.getDirectoryId(dsmProperties.getConnection(),directoryPath,false);
        String creation = Utility.format(new java.util.Date());
        StringBuffer sb = new StringBuffer(512);
        sb.append("INSERT INTO ResourceSites (resource,site,directory,creation) VALUES (");
        sb.append(resourceId);
        sb.append(Utility.COMMA);
        sb.append(Utility.quoteComma(site));
        sb.append(dirId);
        sb.append(Utility.COMMA);
        sb.append(Utility.quote(creation));
        sb.append(")");
        Statement statement = dsmProperties.getConnection().createStatement();
        Utility.executeUpdate(statement, sb.toString());
        if (doCommit) commit();
        statement.close();
    }

    /**
     * Issue a SQL query (select) statement directly to the DSM database
     * to get a list of products.
     */
    public java.util.List<Product> queryProducts(String sqlQueryString)
            throws Exception
    {
        Statement s = dsmProperties.getConnection().createStatement();
        ResultSet r = Utility.executeQuery(s, sqlQueryString);
        java.util.List<Product> list = new java.util.LinkedList<Product>();
        while (r.next())
        {
            list.add(ProductFactory.makeProduct(dsmProperties.getConnection(),getSite(),r));
        }
        s.close();
        return list;
    }
}
