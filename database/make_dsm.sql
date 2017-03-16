/* This script creates the DSM database and sets the appropriate permissions
 * on it.  You need to source this script to mysql as root
 * If you run it against an existing database, you will destroy the station
 * and product type tables, which are human-built. That would not be advisable.
 */

/* This works by sourcing two component SQL scripts:

	make_dsm_database.sql which creates the DSM database
		and all the dsm user logins.
	make_dsm_tables, which creates all the empty tables

  The tables get data loaded into them later, as a result of SPA installation,
  site.properties table reading, etc.

 Enjoy.
*/
source make_dsm_database.sql;
source make_dsm_tables.sql;
