/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.File;

/**
 * A product type in the DSM database. A product type should identify a
 * unique spacecraft and sensor and probably this should be reflected in the
 * product type name. That is, each spacecraft and sensor should have its
 * own set of product types. You should avoid creating a product type
 * that applies to multiple spacecraft and/and or sensors because this
 * may cause you configuration problems later on. It is not a DSM demand
 * however, just a strong suggestion.
 * @version 2.0
 */
public final class ProductType implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//The product type name must be unique among all product types in the database.
    private String name;

    //The spacecraft sensor. It may not be null.
    private String sensor = null;

    //The spacecraft. It may not be null.
    private String spacecraft = null;

    //A description of the product type. It may not be null.
    private String description = null;

    //A product level, which usually indicates to what extent the product has
    //been processed. Level 0 products are usually just raw packets, but
    //higher numbers up to 5 indicate more processing. The default is null,
    //which means the level is undefined.
    private String level = null;

    //The directory on the IS computer where this product type is stored
    //(published). If null, the DSM will put products in the site's base
    //directory.
    private File is_directory = null;


    /**
     * Construct a product type. Only level may be null if you plan to store
     * this product type in the database. The name must be unique because it
     * @param name A unique name for this product type, which usually includes
     *        the spacecraft and sensor names. Case is significant.
     * @param spacecraft The spacecraft name. It will be stored all uppercase.
     * @param sensor The sensor name. Case is significant. If the product
     *        type does not belong to an instrument, you should invent a
     *        name that would categorize it.
     * @param description A short descrption of the product type.
     * @param level A level number, usually one of 0, 1, 1a, 1b, 2, 3.
     *        It indicates a processing level where 0 is raw data and each
     *        level higher defining a greater degree of refinement.
     *        You may set this field to null.
     */
    public ProductType(String name, String spacecraft, String sensor,
            String description, String level)
    {
        this.name = name;
        this.spacecraft = spacecraft.toUpperCase();
        this.sensor = sensor;
        this.description = description;
        this.level = level;
    }

    /**
     * Construct a product type. Only level and is_directory may be null if
     * you plan to store this product type in the database. The name must be
     * unique because it is the primary key. Note that most installations
     * will have product type naming conventions including the spacecraft
     * and instrument. A typical convention: "<spacecraft>.<sensor>.<name>"
     * @param name A unique name for this product type, which usually includes
     *        the spacecraft and sensor names. Case is significant.
     * @param spacecraft The spacecraft name. It will be stored all uppercase.
     * @param sensor The sensor name. Case is significant. If the product
     *        type does not belong to an instrument, you should invent a
     *        name that would categorize it.
     * @param description A short descrption of the product type.
     * @param level A level number, usually one of 0, 1, 1a, 1b, 2, 3.
     *        It indicates a processing level where 0 is raw data and each
     *        level higher defining a greater degree of refinement.
     *        You may set this field to null.
     * @param is_directory The directory on the Information Services (IS)
     *        computer where the DSM publishes (puts) new products. The name
     *        is usually specified as relative to some unnamed base directory.
     *        If is_directory is null, the DSM will copy any products of this
     *        type to a generic "dropbox" directory.
     */
    public ProductType(String name, String spacecraft, String sensor,
            String description, String level, File is_directory)
    {
        this.name = name;
        this.spacecraft = spacecraft.toUpperCase();
        this.sensor = sensor;
        this.description = description;
        this.level = level;
        this.is_directory = is_directory;
    }

    /**
     * The DSM uses this package-level constructor to create a product type
     * object from a database result set, which is the result of a query.
     */
    ProductType(java.sql.ResultSet r) throws java.sql.SQLException
    {
        name = r.getString("name");
        spacecraft = r.getString("spacecraft");
        sensor = r.getString("sensor");
        description = r.getString("description");
        level = r.getString("level");
        String is_dir = r.getString("IS_Directory");
    if (is_dir != null) is_directory = new File(is_dir);
    }

    /**
     * Another constructor only the DSM uses.
     */
    ProductType(String name)
    {
        this.name = name;
    }

    /**
     * Get the product type name. It may not be null.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the name of the sensor associated with this product type.
     * It may not be null.
     */
    public String getSensor()
    {
        return sensor;
    }

    /**
     * Get the name of the spacecraft associated with this product type.
     * It may not be null.
     */
    public String getSpacecraft()
    {
        return spacecraft;
    }

    /**
     * Get a description of the product type. It may not be null.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Get the product type level number. If null, it is undefined.
     */
    public String getLevel()
    {
        return level;
    }

    /*
     * Get the Information Services (IS) directory where these products are stored.
     * The path may be relative to an IS root directory. It may be null, which means
     * the directory is undefined.
     */
    public final File getISdirectory()
    {
        return is_directory;
    }

    /**
     * Does this product type have all database-required fields set to
     * non-null values?
     */
    public boolean isComplete()
    {
        return (name != null) && (spacecraft != null) &&
                (sensor != null) && (description != null);
    }

    /**
     * Test if this product type equals another.
     * @param o A product type object or a string. The comparison is make by
     *      the product type name using toString().
     */
    public boolean equals(Object o)
    {
        return o != null && name.equals(o.toString());
    }

    public String toString()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setSensor(String sensor)
    {
        this.sensor = sensor;
    }

    public void setSpacecraft(String spacecraft)
    {
        this.spacecraft = spacecraft;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    /* Set the directory on the Information Services (IS) computer where the
     * DSM publishes (puts) new products. The name is usually specified as
     * relative to some unnamed base directory. If directory is null, the
     * DSM will copy any products of this type to a generic "dropbox"
     * directory.
     */
    public void setISdirectory(File directory)
    {
        is_directory = directory;
    }
}
