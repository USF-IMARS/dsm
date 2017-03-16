/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.io.File;
import java.util.LinkedList;
import java.util.Date;

/**
 * A resource is a container for a product resource or thumbnail.
 * @version 3.0.0 Added the resource creation date field and site awareness.
 * @version 3.12 Added resource key
 * @version 3.19 Added published flag.
 */
public class Resource implements java.io.Serializable
{
    //Resources can be constructed in two ways and so can have two representations.
    //First, a client may create a new resource (usually within Product) to store
    //into the database. In this mode the resource has only one location, the local one.
    //Second, a resource can be constructed from a database record, and in this case
    //the resource will have a list of site locations, one of which should be the local
    //site. Depending on the mode, some fields may be undefined.

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//A description of this resource.
    private String description;

    //The file name minus all site and directory information. Currently, this is the
    //same on all sites.
    private String name;

    //The resource ID, which the database assigns. It is undefined (null) for new
    //resources not yet in the database.
    private String rid = null;

    //A string keyword associated with this resource. It may be null.
    private String key;

    //Has this resource been published on the IS? This field is only meaningful if
    //the DSM created this Resource from a database record.
    private boolean published = false;

    //A list of Locations, each of which has a site and directory.
    private LinkedList<Location> locations = new LinkedList<Location>();

    //The local location. It will be null if a local copy of the resource does not exist.
    private Location local = null;

    /**
     * Construct a Resource object. A client uses this constructor form (often via Product)
     * to put a new resource into the database. The site is assumed to be local, and
     * note that the site is unnamed at this time.
     * @param path A complete path to the resource file including directory (relative
     *      or absolute depending on the site) and name.
     * @param creation When this resource was created on this site.
     * @param description A text description of the resource. It may be null.
     */
    public Resource(File path, Date creation, String description)
    {
        this.description = description;
        name = path.getName();
        local = new Location(null,path,creation);
        locations.add(local);
    }

    /**
     * Construct a Resource object. The DSM uses this constructor form (via ProductFactory)
     * to create a resource from a database entry. The resource is incomplete because it
     * does not have site and directory information. See addLocal and addRemote.
     */
    Resource(String rid, String name, String description, boolean published)
    {
        this.rid = rid;
        this.name = name;
        this.description = description;
        this.published = published;
    }

    /**
     * This method adds remote site and directory information.
     */
    void addRemote(String site, String directory, Date creation)
    {
        locations.add(new Location(site,directory,name,creation));
    }

    /**
     * This method adds local site and directory information.
     */
    void setLocal(String site, String directory, Date creation)
    {
        local = new Location(site,directory,name,creation);
        locations.add(local);
    }

    /**
     * Set the resource key.
     */
    public void setKey(String key)
    {
        this.key = key;
    }

    /**
     * Get the database resource ID. It is null if not yet defined.
     */
    public final String getId()
    {
        return rid;
    }

    /**
     * Get a description of this resource. It may be null.
     */
    public final String getDescription()
    {
        return description;
    }

    /**
     * Get the resource key.
     */
    public final String getKey()
    {
        return key;
    }

    /**
     * Is this resource published on the IS computer?
     */
    public boolean isPublished()
    {
        return published;
    }

    /**
     * Get a complete path to this resource file on the local site.
     * It will be null if the resource is not local.
     */
    public File getFile()
    {
        return (local == null)? null : local.file;
    }

    /**
     * Get a complete path to this resource on a site.
     * @param site A DSM site
     * @return a file or null if it does not exist on the site
     */
    public File getFile(String site)
    {
        File f = null;
        if (site == null)
        {
            f = getFile();
        }
        else
        {
            for (Location x : locations)
            {
                if (site.equals(x.site))
                {
                    f = x.file;
                    break;
                }
            }
        }
        return f;
    }

    /**
     * Get this resource's name. It will be the file name without the directory
     * information.
     */
    public final String getName()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public boolean equals(Object object)
    {
        boolean match = false;
        if (object != null && object instanceof Resource)
        {
            Resource r = (Resource)object;
            match = name.equals(r.name);
        }
        return match;
    }

    class Location
    {
        //A site may be null, which indicates (temporarily) that it is local.
        //Local sites are NOT all null.
        String site;
        File file;  //complete file name, directory + name
        Date creation;

        Location(String site, String directory, String name, Date creation)
        {
            this.site = site;
            file = new File(directory,name);
            this.creation = creation;
        }

        Location(String site, File path, Date creation)
        {
            this.site = site;
            this.file = path;
            this.creation = creation;
        }
    }
}
