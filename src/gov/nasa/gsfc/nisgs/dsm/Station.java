/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * A station in the DSM database.
 */
public class Station implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//Station name. Required. (32 chars)
    private String name;

    //A description of the station. Required. (255 chars)
    private String description = "";

    //A url to the station's website.  Required. (255 chars)
    private String path = "";

    //The center location of this stations.
    //NaN means it is has not been defined.
    private float latitude = Float.NaN;
    private float longitude = Float.NaN;

    //height above earth reference spheroid
    private float height = Float.NaN;

    /**
     * Construct a station object. The name and description should not
     * be null.
     */
    public Station(String name, String path, String description)
    {
        this.name = name;
        this.path = path;
        this.description = description;
    }

    /**
     * Construct a station object. The name and description should not
     * be null.
     */
    public Station(String name, String path, String description,
                    float latitude, float longitude, float height)
    {
        this.name = name;
        this.path = path;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.height = height;
    }

    /**
     * The DSM uses this package-level constructor to create a station object
     * from a database result set, which is the result of a stations query.
     */
    Station(java.sql.ResultSet r) throws java.sql.SQLException
    {
        name = r.getString("name");
        path = r.getString("path");
        description = r.getString("description");
        if (r.getString("latitude") != null)
        {
            latitude = r.getFloat("latitude");
        }
        if (r.getString("longitude") != null)
        {
            longitude = r.getFloat("longitude");
        }
        if (r.getString("height") != null)
        {
            height = r.getFloat("height");
        }
    }

    public void setLatitude(float latitude)
    {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude)
    {
        this.longitude = longitude;
    }

    public void setHeight(float height)
    {
        this.height = height;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public String getPath()
    {
        return path;
    }

    public String getLatitude()
    {
        return Float.toString(latitude);
    }

    public String getLongitude()
    {
        return Float.toString(longitude);
    }

    public String getHeight()
    {
        return Float.toString(height);
    }

    public String toString()
    {
        return name;
    }

    public boolean equals(Object o)
    {
        return o != null && name.equals(o.toString());
    }

    String getSqlInsertionStatement()
    {
        StringBuffer sb = new StringBuffer(600);
        sb.append("INSERT INTO Stations SET name=");
        sb.append(Utility.quoteComma(name));
        sb.append("description=");
        sb.append(Utility.quoteComma(description));
        sb.append("path=");
        sb.append(Utility.quote(path));

        if (!Float.isNaN(latitude))
        {
            sb.append(",latitude=");
            sb.append(latitude);
        }
        if (!Float.isNaN(longitude))
        {
            sb.append(",longitude=");
            sb.append(longitude);
        }
        if (!Float.isNaN(height))
        {
            sb.append(",height=");
            sb.append(height);
        }
        return sb.toString();
    }
}
