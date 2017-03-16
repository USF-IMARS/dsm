/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.File;
import java.sql.*;
import java.util.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * This class stores a Product object into the database. It is not public.
 * It should not be instantiated. ProductStore behaves as if a resource is
 * located on the local computer, but this is not required, and some agents,
 * such as PdsMover, register resources for other computers. ProductStore
 * does not confirm the existence of any resource.
 * ProductStore marks a resource as published if the site is the IS.
 * @version 3.0.0 Added the product creation date field and site awareness.
 * @version 3.0.1 Fixed product attributes.
 * @version 3.12 Added addResource().
 * @version 3.18 Added "published" database field to Resource.
 * @version 3.20 Added "delete" flag database field to Product.
 */
final class ProductStore
{
    static String IS = "IS";   //when site==IS, resource.published is true.
    static void setInformationServicesSite(String is)
    {
        IS = is;
    }

    /**
     * Store a new product into the DSM database.
     */
    static String store(Connection connection, Product product, String site,
			String maker, boolean commitp) throws Exception
    {
         //The product must have at least one resource.
        if (product.getResources().isEmpty())
        {
            throw new SQLException("No resources in this product");
        }

        String agent = product.getAgent();
        if (agent == null || agent.equals("****"))
        {
            if (maker == null) maker = site + ".unknown";
            product.setAgent(maker);
        }

        String productId = null;
        Statement statement = connection.createStatement();
        try
        {
            insertIntoProductsTable(statement,product);
            productId = Utility.getLastAutoIncrementedValue(statement);
            product.setId(productId);
            insertIntoResourcesTable(connection,statement,product,site);
            insertIntoThumbnailsTable(statement,product);
            insertIntoContributorsTable(connection,product);
            insertIntoAncestorsTable(statement,product);
            insertIntoSubproductTable(statement,product);
            if(commitp)
		Utility.commitConnection(connection);
        }
        catch (Exception e)
        {
            connection.rollback();
            throw e;
        }
        finally
        {
            statement.close();
        }

        return productId;
    }

    /**
     * Add a new resource to an existing product.
     */
    static void addResource(String site, Connection connection, Product product,
            String key, Resource resource) throws Exception
    {
        Statement statement = connection.createStatement();
        try
        {
            addResource(connection,statement,product,key,resource,site);
            Utility.commitConnection(connection);
        }
        catch (Exception e)
        {
            connection.rollback();
            throw e;
        }
        finally
        {
            statement.close();
        }
    }

    /**
     * Build and store a product row into the Products table.
     * It does not handle other tables.
     */
    private static void insertIntoProductsTable(Statement statement, Product product)
            throws SQLException
    {
        StringBuffer sb = new StringBuffer(512);
        sb.append("INSERT INTO Products VALUES (NULL,");
        sb.append(Utility.quoteComma(product.getProductType()));
        sb.append(product.getPass().getId());
        sb.append(Utility.COMMA);
        String startTime = Utility.format(product.getStartTime());
        sb.append(Utility.quoteComma(startTime));
        String stopTime = Utility.format(product.getStopTime());
        sb.append(Utility.quoteComma(stopTime));
        String creation = Utility.format(product.getCreationTime());
        sb.append(Utility.quoteComma(creation));
        sb.append(Utility.quoteComma(product.getAgent()));
        float xx = product.getCenterLatitude();
        sb.append(Float.isNaN(xx)? "NULL" : Float.toString(xx));
        sb.append(Utility.COMMA);
        xx = product.getCenterLongitude();
        sb.append(Float.isNaN(xx)? "NULL" : Float.toString(xx));
        sb.append(Utility.COMMA);
        xx = product.getNorthLatitude();
        sb.append(Float.isNaN(xx)? "NULL" : Float.toString(xx));
        sb.append(Utility.COMMA);
        xx = product.getSouthLatitude();
        sb.append(Float.isNaN(xx)? "NULL" : Float.toString(xx));
        sb.append(Utility.COMMA);
        xx = product.getEastLongitude();
        sb.append(Float.isNaN(xx)? "NULL" : Float.toString(xx));
        sb.append(Utility.COMMA);
        xx = product.getWestLongitude();
        sb.append(Float.isNaN(xx)? "NULL" : Float.toString(xx));
        sb.append(Utility.COMMA);
        sb.append(Utility.quoteComma(product.getSubproduct()));
        sb.append(Utility.quoteComma(product.getAlgorithm()));
        sb.append(Utility.quoteComma(product.getAlgorithmVersion()));
        sb.append((product.getThumbnails() != null)? "1," : "0,");
        sb.append(product.isDeleteProtected()? "1,0," : "0,0,");
	sb.append(product.getMarkerId());
	sb.append(")");
        String xproduct = sb.toString();
        Utility.executeUpdate(statement, xproduct);
    }

    /**
     * Build and store resource rows into the Resources table.
     */
    private static void insertIntoResourcesTable(Connection connection,
            Statement statement, Product product, String site)
            throws Exception
    {
        java.util.Map resourceMap = product.getResourcesMap();
        Iterator irkeys = resourceMap.keySet().iterator();
        while (irkeys.hasNext())
        {
            String key = (String)irkeys.next();
            Resource resource = (Resource)resourceMap.get(key);
            addResource(connection,statement,product,key,resource,site);
        }
    }

    /**
     * Add one resource to the database resource and site tables.
     */
    private static void addResource(Connection connection, Statement statement,
            Product product, String key, Resource resource, String site)
            throws Exception
    {
        resource.setKey(key);
        File file = resource.getFile();
        String published = site.equals(IS)? "1" : "0";

        String directoryId = Utility.getDirectoryId(connection,file.getParent(),false);
        StringBuffer sb = new StringBuffer(512);
        sb.append("INSERT INTO Resources (product,rkey,path,description,published) VALUES (");
        sb.append(product.getId());
        sb.append(Utility.COMMA);
        sb.append(Utility.quoteComma(key));
        sb.append(Utility.quoteComma(file.getName()));
        sb.append(Utility.quoteComma(resource.getDescription()));
        sb.append(published);
        sb.append(")");
        Utility.executeUpdate(statement, sb.toString());

        String resourceId = Utility.getLastAutoIncrementedValue(statement);
        sb.setLength(0);
        sb.append("INSERT INTO ResourceSites (resource,site,directory,creation) VALUES (");
        sb.append(resourceId);
        sb.append(Utility.COMMA);
        sb.append(Utility.quoteComma(site));
        sb.append(directoryId);
        sb.append(Utility.COMMA);
        String creation = Utility.format(new java.util.Date());
        sb.append(Utility.quote(creation));
        sb.append(")");
        Utility.executeUpdate(statement, sb.toString());
    }

    /**
     * This method builds and stores thumbnail rows into the Thumbnails
     * table. It handles two tables: Thumbnails and ProductThumbnails.
     */
    private static void insertIntoThumbnailsTable(Statement statement,
            Product product) throws SQLException
    {
        Collection thumbnailCollection = product.getThumbnails();
        if (thumbnailCollection != null)
        {
            StringBuffer sb = new StringBuffer(512);
            Iterator ti = thumbnailCollection.iterator();
            while (ti.hasNext())
            {
                sb.setLength(0);
                Resource thumbnail = (Resource)ti.next();
                String path = thumbnail.getFile().getPath();

                String x = "SELECT id FROM Thumbnails WHERE path=" +
                        Utility.quote(path);
                ResultSet resultSet = Utility.executeQuery(statement, x);
                String resourceId = null;
                if (resultSet.next())
                {
                    resourceId = resultSet.getString(1);
                }
                else
                {
                    sb.setLength(0);
                    sb.append(" INSERT INTO Thumbnails VALUES (NULL,");
                    sb.append(Utility.quoteComma(path));
                    sb.append(Utility.quote(thumbnail.getDescription()));
                    sb.append(")");
                    Utility.executeUpdate(statement, sb.toString());
                    resourceId = Utility.getLastAutoIncrementedValue(statement);
                }

                String sql = "INSERT INTO ProductThumbnails VALUES (" +
                        product.getId() + "," + resourceId + ")";
                Utility.executeUpdate(statement, sql);
            }
        }
    }

    /**
     * This method builds and stores contributor rows into the Contributors
     * table. It handles two tables: Contributors and ProductContributors.
     */
    private static void insertIntoContributorsTable(Connection connection,
            Product product) throws SQLException
    {
        Collection contributorCollection = product.getContributingResources();
        if (contributorCollection != null) {
	    Iterator ci = contributorCollection.iterator();
	    while (ci.hasNext()) {
		Resource contributor = (Resource)ci.next();
		String path = contributor.getFile().getPath();

		String resourceID = Utility.getID(connection, "Contributors",
						  "path=" + Utility.quote(path),
						  ", description=" + Utility.quote(contributor.getDescription()),
						  false);

		Statement statement = connection.createStatement();
		try {
		    String sql = "INSERT INTO ProductContributors VALUES (" +
			product.getId() + "," + resourceID + ")";
		    Utility.executeUpdate(statement, sql);
		}
		finally {
		    statement.close();
		}
		/*
		  sb.setLength(0);
		  Resource contributor = (Resource)ci.next();
		  String path = contributor.getFile().getPath();

		  String x = "SELECT id FROM Contributors WHERE path=" + Utility.quote(path);
		  ResultSet resultSet = Utility.executeQuery(statement, x);
		  String resourceId = null;
		  if (resultSet.next())
		  {
		  resourceId = resultSet.getString(1);
		  }
		  else
		  {
		  sb.setLength(0);
		  sb.append(" INSERT INTO Contributors VALUES (NULL,");
		  sb.append(Utility.quoteComma(path));
		  sb.append(Utility.quote(contributor.getDescription()));
		  sb.append(")");
		  Utility.executeUpdate(statement, sb.toString());
		  resourceId = Utility.getLastAutoIncrementedValue(statement);
		  }

		  String sql = "INSERT INTO ProductContributors VALUES (" +
		  product.getId() + "," + resourceId + ")";
		  Utility.executeUpdate(statement, sql);
		*/
	    }
	}
    }

    /**
     * This method builds and stores ancestor rows into the Ancestors table.
     */
    private static void insertIntoAncestorsTable(Statement statement,
            Product product) throws SQLException
    {
        Collection ancestorCollection = product.getContributingProductsIds();
        if (ancestorCollection != null)
        {
            Iterator ci = ancestorCollection.iterator();
            while (ci.hasNext())
            {
                String ancestorId = (String)ci.next();
                String sql = "INSERT INTO Ancestors VALUES (" +
                        product.getId() + "," + ancestorId + ")";
                Utility.executeUpdate(statement, sql);
            }
        }
    }

    /**
     * This method builds and stores information into the a subproduct table.
     */
    private static void insertIntoSubproductTable(Statement statement,
            Product product) throws SQLException
    {
        Collection<Attribute> attributesCollection = product.getAttributes();
        if (attributesCollection != null)
        {
            StringBuffer sb = new StringBuffer(512);
            String subproductTable = product.getSubproduct();
            if (subproductTable == null)
            {
                throw new SQLException("You specified attributes, but you have no subproduct name.");
            }
            sb.setLength(0);
            sb.append("INSERT INTO ");
            sb.append(subproductTable);
            sb.append(" SET product=");
            sb.append(product.getId());
            for (Attribute attribute : attributesCollection)
            {
                sb.append(Utility.COMMA);
                sb.append(attribute.toString());
            }
            String asql = sb.toString();
            Utility.executeUpdate(statement, asql);
        }
    }
}
