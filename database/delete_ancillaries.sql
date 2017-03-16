/* This script clears the database tables that record ancillary files.
   It doesn't clear the StaticAncillary tables, which are a minor pain
   to recreate.
   It doesn't touch the underlying file system.
*/
delete from TimeAncillaries;
delete from TimeAncillarySites;
delete from SatTimeAncillaries;
delete from SatTimeAncillarySites;
