/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.File;
import java.util.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * This class contains all information related to one product in the DSM
 * database. Fields are optional except for the following:<br>
 * start time, agent, product type<br>
 * You must specify at least one resource for this product.
 * See "addResource".<br>
 * If you specify a subproduct table, you must provide any required
 * arguments via "addAttribute."
 * @version 1.0.1 Removed SimpleTimeFormat object and now use Utility to
 *      convert times.
 * @version 1.1.0 Added versions of setCenter and setCorners with Number
 *      arguments.
 * @version 1.1.1 Added setAgent(String) and changed the default agent string to
 *      "****". The Product(Product) constructor would throw an exception before
 *      these changes were made.
 * @version 1.1.2 Added productType argument to Product(Product) constructor.
 * @version 3.0.0 Added support for site-awareness. Added the creation date.
 * @version 3.4 Added String getAttribute(String name)
 * @version 3.12 Added Comparable interface.
 */
public final class Product implements java.io.Serializable, Comparable<Product>
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//This field is null when software is using this class to store a product
    //into the database. It is not null when software is using it to fetch a
    //product from the database. The database assigns the product ID.
    private String productId = null;

    //A brief product description, and a reference to an entry in the
    //ProductTypes table. However, it is not required that there is a
    //matching entry in ProductTypes.
    private String productType;

    //Required.
    private Date startTime;
    private Date stopTime;

    //When this product was created.
    private Date creation = new Date();

    //The pass during which this product was created. Required.
    private Pass pass;

    //The location information is not required but encouraged. The values
    //may be negative. "Float.NaN denotes an undefined value.
    private float centerLatitude = Float.NaN;
    private float centerLongitude = Float.NaN;
    private float northLatitude = Float.NaN;
    private float southLatitude = Float.NaN;
    private float eastLongitude = Float.NaN;
    private float westLongitude = Float.NaN;

    //If true, the DSM agent (Hoover) will not delete this product while
    //conducting normal cleanup.
    private boolean deleteProtected = false;

    //A code for who inserted this product into the database. Optional.
    //The default agent name is "****".
    private String agent = "****";

    //algorithm. Optional.
    private String algorithm;

    //algorithm version. Optional.
    private String algorithmVersion = "0";

    //Contains Resource objects. It must not be empty.
    private Map<String,Resource> resources = new TreeMap<String,Resource>();

    //Contains thumbnails as Resource objects It may be null.
    private Collection<Resource> thumbnails = null;

    //Contains contributing resources as Resource objects.
    //The collection may be null.
    private Collection<Resource> contributingResources = null;

    //Contains products that were directly used to create this
    //product. The collection may be null. It is a collection
    //of product IDs (strings) and not the actual product objects.
    private Collection<String> contributingProductIDs = null;

    //This is a subtable name where extended information can be found.
    //Optional.
    private String subproduct = null;

    //This list contains attribute pairs (name + value) for database fields
    //that belong to a subproduct. The legal list of names depends on the
    //fields that are defined in the specific subproduct table.
    //It may be null.
    private ArrayList<Attribute> attributesList = null;

    //Product ID of the (productID, gopherColony) entry in the
    //Markers table that was created when this product was created
    private String markerId = "0";

    /**
     * Create a Product. This constructor has all fields that the database
     * requires except resources. You must still use addResource to add
     * at least one resource to the product.
     * @param start The start date and time of the product.
     * @param stop The stop (end) date and time of the product.
     * @param agent This string identifies the program (or agent) who put
     *      this product in the database. Each agent should have a
     *      unique name. Agent names are used in debugging, and some programs
     *      use the agent name to retrieve all products they put into the
     *      database. By default, the agent name is "****".
     * @param productType A product type for this product. It should match
     *      a name in the ProductTypes table.
     * @param pass The pass associated with this product.
     */
    public Product(Date start, Date stop, String agent, String productType,
            Pass pass)
    {
        this.startTime = start;
        this.stopTime = stop;
        this.agent = agent;
        this.productType = productType;
        this.pass = pass;
    }

    /**
     * Create a product based upon an existing one. The new product will
     * have the same pass and start and stop times. The old product will
     * automatically be registered as a contributing product to this one.
     * The agent name will be "****" unless you change it.
     * @param product The product used to construct this product.
     * @param productType The productType for this new product.
     *          It should not be null.
     */
    public Product(Product product, String productType) throws Exception
    {
        this.startTime = product.startTime;
        this.stopTime = product.stopTime;
        this.pass = product.pass;
        this.productType = productType;
        addContributingProduct(product);
    }

    /**
     * Add a resource to this product. You must specify at least one resource
     * when you create a product, and no other product may share this resource.
     * @param keyword A unique keyword within this product that is associated
     *          with this resource. Others use the keyword to fetch it.
     * @param path The absolute path of the resource file.
     */
    public void addResource(String keyword, String path) throws Exception
    {
        addResource(keyword,path,null);
    }

    /**
     * Add a resource to this product. You must specify at least one resource
     * when you create a product, and no other product may share this resource.
     * @param keyword A unique keyword within this product that is associated
     *          with this resource. Others use the keyword to fetch it.
     * @param path The absolute path of the resource file.
     * @param description A brief description of the resource. It may be null.
     */
    public void addResource(String keyword, String path, String description)
            throws Exception
    {
        if (path == null)
        {
            throw new Exception("Resource path may not be null");
        }
        File file = new File(path);
        addResource(keyword,new Resource(file,creation,description));
    }

    /**
     * Add a resource to this product. You must specify at least one resource
     * when you create a product, and no other product may share this resource.
     * @param keyword A unique keyword within this product that is associated
     *          with this resource. Others use the keyword to fetch it.
     * @param resource A resource object.
     */
    void addResource(String keyword, Resource resource) throws Exception
    {
        if (resources.containsKey(keyword))
        {
            throw new Exception("Duplicate resource keyword " + keyword);
        }
        resource.setKey(keyword);
        resources.put(keyword,resource);
    }

    /**
     * A product may have one or more thumbnails. A thumbnail may be a small
     * image, or it may be other types provided a browser can understand
     * the mime type and can conveniently display the thumbnail.
     * Thumbnails are not required.
     * @param path The absolute path of the thumbnail file.
     * @param description A brief description of the thumbnail. May be null.
     */
    public void addThumbnail(String path, String description)
            throws Exception
    {
        if (path == null)
        {
            throw new Exception("path may not be null");
        }
        Resource t = new Resource(new File(path),creation,description);
        if (thumbnails == null)
        {
            thumbnails = new ArrayList<Resource>();
        }
        else if (thumbnails.contains(t))
        {
            throw new Exception("duplicate thumbnail " + path);
        }
        thumbnails.add(t);
    }

    /**
     * A product may have one or more thumbnails. A thumbnail may be a small
     * image, or it may be other types provided that a browser can understand
     * the mime type and is able to conveniently display the thumbnail.
     * Thumbnails are not required.
     * @param path The absolute path of the thumbnail file.
     */
    public void addThumbnail(String path) throws Exception
    {
        addThumbnail(path,null);
    }

    /**
     * Protect this product from being delete during routine cleanup.
     * This method must be called prior to storing a product.
     */
    public void setDeleteProtected(boolean deleteProtected)
    {
        this.deleteProtected = deleteProtected;
    }

    /**
     * Set the name of a subproduct table, where more extensive information
     * about this product exists. The subproduct name must match the name
     * of a Products subtable in the database.
     */
    public void setSubproduct(String subproduct)
    {
        this.subproduct = subproduct;
    }

    /**
     * Add an attribute to this product. Attributes are meaningful only if
     * you specify a subproduct, and then each attribute name must match a
     * field in the subproduct table.
     */
    public void addAttribute(String name, String value) throws Exception
    {
        if (name == null)
        {
            throw new Exception("Attribute name may not be null");
        }
        Attribute a = new Attribute(name,value);
        if (attributesList == null)
        {
            attributesList = new ArrayList<Attribute>();
        }
        else if (attributesList.contains(a))
        {
            throw new Exception("duplicate attribute " + name);
        }
        attributesList.add(a);
    }

    /**
     * Set the center of the observation swath for this product.
     */
    public void setCenter(float centerLatitude, float centerLongitude)
    {
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
    }

    /**
     * Set the corners of the observation swath for this product.
     */
    public void setCorners(float northLatitude, float southLatitude,
            float eastLongitude, float westLongitude)
    {
        this.northLatitude = northLatitude;
        this.southLatitude = southLatitude;
        this.eastLongitude = eastLongitude;
        this.westLongitude = westLongitude;
    }

    /**
     * Set the center of the observation swath for this product.
     */
    public void setCenter(Number centerLatitude, Number centerLongitude)
    {
        this.centerLatitude = centerLatitude.floatValue();
        this.centerLongitude = centerLongitude.floatValue();
    }

    /**
     * Set the corners of the observation swath for this product.
     */
    public void setCorners(Number northLatitude, Number southLatitude,
            Number eastLongitude, Number westLongitude)
    {
        this.northLatitude = northLatitude.floatValue();
        this.southLatitude = southLatitude.floatValue();
        this.eastLongitude = eastLongitude.floatValue();
        this.westLongitude = westLongitude.floatValue();
    }

    /**
     * Set an algorithm name and version number. There is no defined format
     * for these strings, but you should be consistent within products so
     * that it can be used as a search field. Do not set either field to null.
     */
    public void setAlgorithm(String algorithm, String algorithmVersion)
    {
        this.algorithm = algorithm;
        if (algorithmVersion != null)
        {
            this.algorithmVersion = algorithmVersion;
        }
    }

    /**
     * Add a contributing resource to this product. A contributing resource
     * is one that was used in the creation of this product, but it is not
     * a product itself in this database. A contributing resource is
     * optional. The path may reference a resource that does not exist.
     * It is not an error to add a resource multiple times - extras are
     * ignored.
     */
    public void addContributingResource(String path, String description)
            throws Exception
    {
        if (path == null)
        {
            throw new Exception("path may not be null");
        }
        Resource cr = new Resource(new File(path),creation,description);
        if (contributingResources == null)
        {
            contributingResources = new ArrayList<Resource>();
        }
        if (!contributingResources.contains(cr))
        {
	    contributingResources.add(cr);
	}
    }

    /**
     * Add a contributing product to this product. A contributing
     * product is one that was directly used in the creation of this
     * product, and it is also a product itself in this database. You are
     * not required to identify contributing products. Note that a
     * contributing product must have been previously inserted into the
     * database. (It must have a product ID.)
     * @param product A product directly used to create this product.
     */
    public void addContributingProduct(Product product) throws Exception
    {
        if (product == null)
        {
            throw new Exception("product may not be null");
        }
        if (product.getId() == null)
        {
            throw new Exception("This contributing product of type " +
                    product.getProductType() +
                    "has not been registered in the database, so it has no ID.");
        }
        addContributingProduct(product.getId());
    }

    /**
     * Add a contributing product to this product. A contributing
     * product is one that was directly used in the creation of this
     * product, and it is also a product itself in this database. It is
     * optional.
     * It is not an error to add a product multiple times - extras are
     * ignored.
     * @param productId A product (by ID) directly used to create this product.
     */
    public void addContributingProduct(String productId) throws Exception
    {
        if (productId == null)
        {
            throw new Exception("productId may not be null");
        }
        if (contributingProductIDs == null)
        {
            contributingProductIDs = new ArrayList<String>();
        }
        if (!contributingProductIDs.contains(productId))
        {
	    contributingProductIDs.add(productId);
	}
    }

    /**
     * Set the product's start time.
     */
    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Set the product's stop time.
     */
    public void setStopTime(Date stopTime)
    {
        this.stopTime = stopTime;
    }

    /**
     * Set the date and time when this product was created.
     */
    public void setCreationTime(Date c)
    {
        creation = c;
    }

    /**
     * Set the product type.
     */
    public void setProductType(String productType)
    {
        this.productType = productType;
    }

    /**
     * Set the pass from which this product originated.
     */
    public void setPass(Pass pass)
    {
        this.pass = pass;
    }

    /**
     * Set the name of the agent who created this product. Agent names are
     * used in debugging, and some programs use the agent name to retrieve
     * all products they put into the database. By default, the agent name
     * is "****".
     */
    public void setAgent(String agent)
    {
        this.agent = agent;
    }

    /**
     * Set the markerId.
     */
    public void setMarkerId(String markerId)
    {
	this.markerId = markerId;
    }

    /**
     * Simple accessor for markerId.  The heavy lifting of populating this field
     * is done in ProductFactory
     */
    public String getMarkerId()
    {
	return markerId;
    }

    /**
     * Get the product ID for this product, which is a unique ID the database
     * generates. It will only be non-null if you have retrieved a product
     * from the database.
     */
    public String getId()
    {
        return productId;
    }

    /**
     * Get the name of this product's subproducts table. It may be null.
     */
    public String getSubproduct()
    {
        return subproduct;
    }

    /**
     * Get this product's product type.
     */
    public String getProductType()
    {
        return productType;
    }

    /**
     * Get the spacecraft pass from which this product originated.
     */
    public Pass getPass()
    {
        return pass;
    }

    /**
     * Get the product start time.
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * Get the product start time as a string in this form:
     * "yyyy-MMM-dd HH:mm:ss"
     */
    public String getStartTimeString()
    {
        return Utility.format(startTime);
    }

    /**
     * Get the product stop time.
     */
    public Date getStopTime()
    {
        return stopTime;
    }

    /**
     * Get the product stop time as a string in this form:
     * "yyyy-MMM-dd HH:mm:ss"
     */
    public String getStopTimeString()
    {
        return Utility.format(stopTime);
    }

    /**
     * Get the date and time when this product was created.
     */
    public Date getCreationTime()
    {
        return creation;
    }

    /**
     * Determine if this product is protected from routine cleanup.
     */
    public boolean isDeleteProtected()
    {
        return deleteProtected;
    }

    /**
     * Get the identity of whoever created this product.
     * This will be some string tag that the creator provides.
     */
    public String getAgent()
    {
        return agent;
    }

    /**
     * Get the center latitude of this product.
     * @return the center latitude or Float.NaN if undefined.
     */
    public float getCenterLatitude()
    {
        return centerLatitude;
    }

    /**
     * Get the center longitude of this product.
     * @return the center longitude or Float.NaN if undefined.
     */
    public float getCenterLongitude()
    {
        return centerLongitude;
    }

    /**
     * Get the north latitude of this product.
     * @return the north latitude or Float.NaN if undefined.
     */
    public float getNorthLatitude()
    {
        return northLatitude;
    }

    /**
     * Get the south latitude of this product.
     * @return the south latitude or Float.NaN if undefined.
     */
    public float getSouthLatitude()
    {
        return southLatitude;
    }

    /**
     * Get the east longitude of this product.
     * @return the east longitude or Float.NaN if undefined.
     */
    public float getEastLongitude()
    {
        return eastLongitude;
    }

    /**
     * Get the west longitude of this product.
     * @return the west longitude or Float.NaN if undefined.
     */
    public float getWestLongitude()
    {
        return westLongitude;
    }

    /**
     * Get the algorithm used to maked this product. It may be null.
     */
    public String getAlgorithm()
    {
        return algorithm;
    }

    /**
     * Get the algorithm version used to maked this product.
     */
    public String getAlgorithmVersion()
    {
        return algorithmVersion;
    }

    /**
     * Get the name of the ground station that collected and created this product.
     * It may be null.
     */
    public String getStation()
    {
        return (pass == null)? null : pass.getStationName();
    }

    /**
     * Get the spacecraft associated with this product. If null, it indicates the
     * product was never associated with a pass.
     */
    public String getSpacecraft()
    {
        return (pass == null)? null : pass.getSpacecraft();
    }

    /**
     * Get the resource associated with the passed keyword.
     * @return a complete file specification for the resource on the local site,
     * or null if the keyword has no matching resource or a copy of the resource
     * is not on the local computer.
     */
    public String getResource(String keyword)
    {
        String p = null;
        Resource r = (Resource)resources.get(keyword);
        if (r != null)
        {
            File f = r.getFile();
            if (f != null) p = f.getPath();
        }
        return p;
    }

    /**
     * Get all resources as a collection of Resource objects.
     */
    public Collection<Resource> getResources()
    {
        return resources.values();
    }

    /**
     * Get a collection of Attribute objects for this product. Attributes only exist
     * when a product has a non-null subproduct. It may return null.
     */
    public Collection<Attribute> getAttributes()
    {
        return attributesList;
    }

    /**
     * Get an attribute by name.
     * @param name A unique attribute name. Case is significant.
     * @return an attribute value or null if the attribute is undefined or
     *      the product does not have a subproduct table.
     */
    public String getAttribute(String name)
    {
        String value = null;
        if (attributesList != null)
        {
            for (Attribute a : attributesList)
            {
                if (a.getName().equals(name))
                {
                    value = a.getValue();
                    break;
                }
            }
        }
        return value;
    }

    /**
     * Get a collection of thumbnails, which are Resource objects,
     * for this product. It may return null.
     */
    public Collection<Resource> getThumbnails()
    {
        return thumbnails;
    }

    /**
     * Get a collection of contributing resources, which are Resource objects,
     * for this product. It may return null.
     */
    public Collection<Resource> getContributingResources()
    {
        return contributingResources;
    }

    /**
     * Get a collection of contributing product IDs, which are Strings,
     * for this product. It may return null.
     */
    public Collection<String> getContributingProductsIds()
    {
        return contributingProductIDs;
    }

    public String toString()
    {
        String pid = (productId == null)? "" : productId;
        return "Product " + pid + " " + productType + " " +
                getStartTimeString() + " " + getStopTimeString();
    }

    /**
     * Set the product ID, which is the primary key in the Products table.
     * This field is meaningless and ignored when you are creating a product
     * to store into the database because the database itself generates an ID.
     * Software will set this field when it fetches an existing product from
     * the database. It is not useful as a public method.
     */
    void setId(String id)
    {
        productId = id;
    }

    /**
     * Determine if all resources are local to this site.  Enforces the
     * requirement that products must have at least one resource.
     * @return true if all resources are local and false otherwise
     */
    public boolean resourcesAreLocal()
	throws Exception
    {
        boolean isLocal = true;
	if (resources.isEmpty()) {
	    // It is an error to call this on a Product with no resources
	    throw new Exception("resourcesAreLocal: product " + productId
				+ " has no resources!");
	}
        Collection<Resource> list = resources.values();
        for (Resource r : list)
        {
            isLocal = r.getFile() != null;
            if (!isLocal) break;
        }
        return isLocal;
    }

    public int compareTo(Product p)
    {
        int value = 0;
        if (productId == null || p.productId == null)
        {
            value = startTime.compareTo(p.startTime);
            if (value == 0)
            {
                value = stopTime.compareTo(p.stopTime);
            }
        }
        else
        {
            value = productId.compareTo(p.productId);
        }
        return value;
    }

    public boolean equals(Object o)
    {
        boolean match = false;
        if (o != null && o instanceof Product)
        {
            Product p = (Product)o;
            int n = compareTo(p);
            match = (n == 0);
        }
        return match;
    }

    /**
     * Get the map that stores resources with their keys.
     */
    java.util.Map<String,Resource> getResourcesMap()
    {
        return resources;
    }
}
