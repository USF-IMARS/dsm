/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.util.Date;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * This class is one spacecraft pass.
 * @version 1.1.1 Added contains() method.
 * @version 3.19 Added creation date field.
 */
public final class Pass implements java.io.Serializable
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String station;
    private String spacecraft;
    private Date aos;
    private Date los;
    //The database automatically creates this ID.
    private String id;
    private float centerLatitude = Float.NaN;
    private float centerLongitude = Float.NaN;
    private float northLatitude = Float.NaN;
    private float southLatitude = Float.NaN;
    private float eastLongitude = Float.NaN;
    private float westLongitude = Float.NaN;
    private boolean deleteProtected = false;
    private Date creation = null;

    /**
     * Create a pass object.
     * @param station The location where the pass was observed. This name should
     *      match an entry in the database's Stations table, but it is not required.
     * @param spacecraft The spacecraft name. The name will be converted to all
     *      uppercase.
     * @param aos Acquisition of signal
     * @param los Loss of signal
     */
    public Pass(String station, String spacecraft, Date aos, Date los)
    {
        this.station = station.trim();
        this.spacecraft = spacecraft.trim().toUpperCase();
        this.aos = aos;
        this.los = los;
        creation = new java.util.Date();
    }

    /**
     * Create a pass object.
     * @param station The location where the pass was observed. This name should
     *      match an entry in the database's Stations table, but it is not required.
     * @param spacecraft The spacecraft name. The name will be converted to all
     *      uppercase.
     * @param aos Acquisition of signal
     * @param los Loss of signal
     * @param centerLatitude center latitude
     * @param centerLongitude center longitude
     * @param northLatitude north latitude
     * @param southLatitude south latitude
     * @param eastLongitude east longitude
     * @param westLongitude west longitude
     */
    public Pass(String station, String spacecraft, Date aos, Date los,
                float centerLatitude, float centerLongitude,
                float northLatitude, float southLatitude,
                float eastLongitude, float westLongitude)
    {
        this(station,spacecraft,aos,los);
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.northLatitude = northLatitude;
        this.southLatitude = southLatitude;
        this.eastLongitude = eastLongitude;
        this.westLongitude = westLongitude;
    }

    /**
     * The DSM uses this package-level constructor to create a pass object
     * from a database result set, which is the result of a pass query.
     * @param r A result set containing information for one pass
     */
    public Pass(java.sql.ResultSet r) throws Exception
    {
        id = r.getString("id");
        station = r.getString("station");
        spacecraft = r.getString("spacecraft");
        aos = Utility.parse(r.getString("aos"));
        los = Utility.parse(r.getString("los"));
        creation = Utility.parse(r.getString("creation"));
        deleteProtected = r.getBoolean("deleteProtected");
        String sclat = r.getString("centerLatitude");
        String sclong = r.getString("centerLongitude");
        if (sclat != null && sclong != null)
        {
            float clat = Float.parseFloat(sclat);
            float clong = Float.parseFloat(sclong);
            setCenter(clat,clong);
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
            setCorners(nlat,slat,elong,wlong);
        }
    }

    /**
     * Get the spacecraft for this pass.
     */
    public String getSpacecraft()
    {
       return spacecraft;
    }

    /**
     * Get the database ID for this pass. Each pass has a unique ID.
     */
    public String getId()
    {
       return id;
    }

    /**
     * Get the AOS (acquisition of signal) time for this pass.
     */
    public java.util.Date getAos()
    {
       return aos;
    }

    /**
     * Get the LOS (loss of signal) time for this pass.
     */
    public Date getLos()
    {
       return los;
    }

    /**
     * Get the delete protection field.
     */
    public boolean isDeleteProtected()
    {
        return deleteProtected;
    }

    /**
     * Get the date and time when this pass was created and added to the database.
     */
    public Date getCreation()
    {
        return creation;
    }

    /**
     * Determine if the time is within the AOS and LOS of this pass
     * and the designated spacecraft is the spacecraft of this pass.
     */
    public boolean contains(String spacecraft, java.util.Date time)
    {
        return spacecraft.equals(this.spacecraft) && (!time.after(los) ||
                !time.before(aos));
    }

    /**
     * Get the station name for this pass.
     */
    public String getStationName()
    {
       return station;
    }

    /**
     * Get the center latitude for this pass.
     * @return center latitude or Float.NaN if undefined.
     */
    public float getCenterLatitude()
    {
        return centerLatitude;
    }

    /**
     * Get the center longitude for this pass.
     * @return center longitude or Float.NaN if undefined.
     */
    public float getCenterLongitude()
    {
        return centerLongitude;
    }

    /**
     * Get the north latitude for this pass.
     * @return north latitude or Float.NaN if undefined.
     */
    public float getNorthLatitude()
    {
        return northLatitude;
    }

    /**
     * Get the south latitude for this pass.
     * @return south latitude or Float.NaN if undefined.
     */
    public float getSouthLatitude()
    {
        return southLatitude;
    }

    /**
     * Get the east longitude for this pass.
     * @return east longitude or Float.NaN if undefined.
     */
    public float getEastLongitude()
    {
        return eastLongitude;
    }

    /**
     * Get the west longitude for this pass.
     * @return west longitude or Float.NaN if undefined.
     */
    public float getWestLongitude()
    {
        return westLongitude;
    }

    /**
     * Set the spacecraft for this pass.
     */
    public void setSpacecraft(String spacecraft)
    {
       this.spacecraft = spacecraft.toUpperCase();
    }

    /**
     * Set the station name for this pass.
     */
    public void setStationName(String station)
    {
       this.station = station;
    }

    /**
     * Set the AOS (acquisition of signal) time for this pass.
     */
    public void setAos(Date aos)
    {
       this.aos = aos;
    }

    /**
     * Set the LOS (loss of signal) time for this pass.
     */
    public void setLos(Date los)
    {
       this.los = los;
    }

    /**
     * Set the database ID for this pass. Each pass has a unique ID.
     */
    void setId(String id)
    {
       this.id = id;
    }

    /**
     * Set the delete protection field. This field is only effective is it is set
     * prior to storing the pass in the database. If true, the DSM agents will not
     * routinely delete this pass from the database.
     */
    public void setDeleteProtection(boolean deleteProtected)
    {
        this.deleteProtected = deleteProtected;
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
     * Two passes are equal if they have the same database ID. If two passes have same
     * spacecraft, AOS, and LOS but different IDs, this method will return false.
     */
    public boolean equals(Object o)
    {
        boolean match = false;
        if (o != null && o instanceof Pass)
        {
            Pass x = (Pass)o;
            match = x.id.equals(id);
        }
        return match;
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer(500);
        sb.append("Pass=");
        sb.append(id);
        sb.append(" station=");
        sb.append(station);
        sb.append(" spacecraft=");
        sb.append(spacecraft);
        sb.append(" aos=");
        sb.append(Utility.format(aos));
        sb.append(" los=");
        sb.append(Utility.format(los));
        return sb.toString();
    }

    String getSqlInsertionStatement()
    {
        StringBuffer sb = new StringBuffer(600);
        sb.append("INSERT INTO Passes SET station=");
        sb.append(Utility.quote(station));
        sb.append(",spacecraft=");
        sb.append(Utility.quote(spacecraft));
        sb.append(",aos=");
        sb.append(Utility.quote(Utility.format(aos)));
        sb.append(",los=");
        sb.append(Utility.quote(Utility.format(los)));
        sb.append(",deleteProtected=");
        sb.append(deleteProtected? "1" : "0");
        sb.append(",creation=");
        sb.append(Utility.quote(Utility.format(creation)));
        if (!Float.isNaN(centerLatitude))
        {
            sb.append(",centerLatitude=");
            sb.append(centerLatitude);
        }
        if (!Float.isNaN(centerLongitude))
        {
            sb.append(",centerLongitude=");
            sb.append(centerLongitude);
        }
        if (!Float.isNaN(northLatitude))
        {
            sb.append(",northLatitude=");
            sb.append(northLatitude);
        }
        if (!Float.isNaN(southLatitude))
        {
            sb.append(",southLatitude=");
            sb.append(southLatitude);
        }
        if (!Float.isNaN(eastLongitude))
        {
            sb.append(",eastLongitude=");
            sb.append(eastLongitude);
        }
        if (!Float.isNaN(westLongitude))
        {
            sb.append(",westLongitude=");
            sb.append(westLongitude);
        }
        return sb.toString();
    }

    //Collections.sort(list, new Comparator<Pass>() {
//	int compare (Pass

}
