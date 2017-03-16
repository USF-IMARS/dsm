/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
package gov.nasa.gsfc.nisgs.dsm;
import java.sql.*;
import gov.nasa.gsfc.nisgs.properties.Utility;

/**
 * The job of this static class is handle anything to do with stations
 * and the Stations table in the database. It should not be instantiated.
 * It exists to reduce the size and complexity of DSM and DSMAdministrator.
 */
class Stations
{
    /**
     * Get a station by name from the Stations table.
     * @param connection a DSM database connection
     * @param a unique name for the station, which is in the database
     * @return a Station object or null if the station is not in the table
     */
    static Station getStation(Connection connection, String name)
            throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Stations WHERE name=" +
                Utility.quote(name));
        Station station = null;
        if (r.next())
        {
            station = new Station(r);
        }
        s.close();
        return station;
    }

    /**
     * Create a new station and add it to the Stations table. A station entry
     * contains more descriptive information about a station. A product does
     * not require it.
     * @param connection a DSM database connection
     * @param station a reference to a Station object.
     */
    static void createStation(Connection connection, Station station)
            throws Exception
    {
        Statement statement = connection.createStatement();
        String sql = station.getSqlInsertionStatement();
        Utility.executeUpdate(statement, sql);
        statement.close();
        Utility.commitConnection(connection);
    }

    /**
     * Delete a station record from the Stations table. This has no effect on
     * products that refer to stations. Those references will simply have no
     * information in the Stations table.
     * @param connection a DSM database connection
     * @param name station name
     */
    static void deleteStation(Connection connection, String name)
            throws Exception
    {
        Statement statement = connection.createStatement();
        Utility.executeUpdate(statement, "DELETE Stations FROM Stations WHERE name=" +
                Utility.quote(name));
        Utility.commitConnection(connection);
        statement.close();
    }

    /**
     * Get a list of stations.
     * @param connection a DSM database connection
     */
    static java.util.List<Station> getStationsList(Connection connection)
            throws Exception
    {
        Statement s = connection.createStatement();
        ResultSet r = Utility.executeQuery(s, "SELECT * FROM Stations ORDER BY name ASC");
        java.util.List<Station> list = new java.util.LinkedList<Station>();
        while (r.next())
        {
            Station station = new Station(r);
            list.add(station);
        }
        s.close();
        return list;
    }

    /**
     * Get a list of stations.
     * @param connection a DSM database connection
     */
    static Station[] getStations(Connection connection) throws Exception
    {
        java.util.List<Station> list = getStationsList(connection);
        int size = list.size();
        Station[] sarray = new Station[size];
        list.toArray(sarray);
        return sarray;
    }
}
