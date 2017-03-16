/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.sql.*;
import java.io.File;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * The job of this static class is handle anything to do with product types
 * and the ProductTypes table in the database. It should not be instantiated.
 * It exists to reduce the size and complexity of DSM and DSMAdministrator.
 */
public class ProductTypes
{

    /**
     * Check whether or not a product type is defined
     */
    static boolean isDefined(Connection connection, String name)
	throws Exception
    {
	boolean answer = false;
	Statement s = connection.createStatement();
	try {
	    ResultSet r = Utility.executeQuery(s, "SELECT * FROM ProductTypes WHERE name=" + Utility.quote(name));
	    if(r.next())
		answer = true;
	}
	finally {
	    s.close();
	}
	return answer;
    }

    /**
     * Get one product type object by its name.
     * @return a product type or null if the type is not in the ProductTypes
     *      table
     * @param connection a DSM database connection
     */
    static ProductType getProductType(Connection connection, String name)
            throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM ProductTypes WHERE name=" +
                Utility.quote(name));
        ProductType productType = null;
        if (r.next())
        {
            productType = new ProductType(r);
        }
        s.close();
        return productType;
    }

    /**
     * Create a new product type and add it to the ProductTypes table.
     * @param connection a DSM database connection
     * @param productType a reference to a ProductType object.
     */
    static void createProductType(Connection connection, ProductType productType)
            throws Exception
    {
        Statement statement = connection.createStatement();
        StringBuffer sb = new StringBuffer(1024);
        sb.append("INSERT INTO ProductTypes VALUES (");
        sb.append(Utility.quoteComma(productType.getName()));
        sb.append(Utility.quoteComma(productType.getSpacecraft()));
        sb.append(Utility.quoteComma(productType.getSensor()));
        sb.append(Utility.quoteComma(productType.getDescription()));
        String slevel = productType.getLevel();
        if (slevel.length() == 0) slevel = null;
        sb.append(Utility.quoteComma(slevel));
        File sdirfile = productType.getISdirectory();
	String sdir = (sdirfile == null ? "" : sdirfile.getPath());
        if (sdir.length() == 0) sdir = null;
        sb.append(Utility.quote(sdir));
        sb.append(")");
        Utility.executeUpdate(statement, sb.toString());
        Utility.commitConnection(connection);
        statement.close();
    }

    /**
     * Update a type in the ProductTypes table.
     * If the type is not in the table, it creates a new entry.
     * @param connection a DSM database connection
     * @param productType a reference to a ProductType object.
     */
    static void updateProductType(Connection connection, ProductType productType)
            throws Exception
    {
        Statement statement = connection.createStatement();
        Utility.executeUpdate(statement, "LOCK TABLES ProductTypes WRITE");
        try
        {
            ProductType pt = getProductType(connection, productType.getName());
            if (pt == null)
            {
                //not in table
                createProductType(connection,productType);
            }
            else
            {
                StringBuffer sb = new StringBuffer(1024);
                sb.append("UPDATE ProductTypes SET spacecraft=");
                sb.append(Utility.quoteComma(productType.getSpacecraft()));
                sb.append(" sensor=");
                sb.append(Utility.quoteComma(productType.getSensor()));
                sb.append(" description=");
                sb.append(Utility.quote(productType.getDescription()));
                String level = pt.getLevel();
                if (level != null && level.length() > 0)
                {
                    sb.append(", level=");
                    sb.append(Utility.quote(level));
                }
                File dirfile = pt.getISdirectory();
		String dir = (dirfile == null ? null : dirfile.getPath());
                if (dir != null && dir.length() > 0)
                {
                    sb.append(", is_directory=");
                    sb.append(Utility.quote(dir));
                }
                sb.append(" WHERE name=");
                sb.append(Utility.quote(productType.getName()));
                String sql = sb.toString();
                Utility.executeUpdate(statement, sql);
                Utility.commitConnection(connection);
            }
        }
        catch (SQLException e)
        {
            throw e;
        }
        finally
        {
            Utility.executeUpdate(statement, "UNLOCK TABLES");
            statement.close();
        }
    }

    /**
     * Delete a product type from the ProductType table.
     * @param connection a DSM database connection
     * @param name a reference to a ProductType object.
     */
    static void deleteProductType(Connection connection, String name)
            throws Exception
    {
        Statement s = connection.createStatement();
        Utility.executeUpdate(s, "DELETE ProductTypes FROM ProductTypes WHERE name=" +
                Utility.quote(name));
        Utility.commitConnection(connection);
        s.close();
    }

    /**
     * Get a list of product types. This includes product types that are
     * defined in the Products table but do not have an entry in the
     * ProductTypes table. Each product type contains its keyword list.
     * Undefined product types will be inside ProductType objects but with
     * all fields except name set to null.
     * @param connection a DSM database connection
     */
    static java.util.List<ProductType> getProductTypesList(Connection connection)
            throws Exception
    {
        Statement s = connection.createStatement();
        java.util.List<ProductType> list = new java.util.LinkedList<ProductType>();

        ResultSet r = Utility.executeQuery(s, "SELECT * FROM ProductTypes");
        while (r.next())
        {
            list.add(new ProductType(r));
        }
        r.close();

        r = Utility.executeQuery(s, "SELECT DISTINCT productType FROM Products");

        ProductType dummy = new ProductType("dummy");
        while (r.next())
        {
            String name = r.getString(1);
            dummy.setName(name);
            if (name != null && !list.contains(dummy))
            {
                list.add(dummy);
                dummy = new ProductType("dummy");
            }
        }
        s.close();
        return list;
    }

    /**
     * Get an array of product types. This includes product types that are
     * defined in the Products table but do not have an entry in the
     * ProductTypes table. Each product type contains its keyword list.
     * Undefined product types will be inside ProductType objects but with
     * all fields except name set to null.
     * @param connection a DSM database connection
     */
    static ProductType[] getProductTypes(Connection connection) throws Exception
    {
        java.util.List<ProductType> list = getProductTypesList(connection);
        int size = list.size();
        ProductType[] ptarray = new ProductType[size];
        list.toArray(ptarray);
        return ptarray;
    }
}
