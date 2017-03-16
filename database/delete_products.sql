/* This script clears the database tables that record the current product
   set.  It doesn't clear the passes records, nor the ancillary records.
   It doesn't touch the underlying file system either.
*/
delete from ResourceSites;
delete from Resources;
delete from Products;
delete from Markers;
delete from TransferCommands;
delete from Contributors;
delete from Ancestors;
delete from ProductContributors;
delete from ProductThumbnails;
delete from Thumbnails;
delete from ProductionDataSets;
delete from Algorithms;
