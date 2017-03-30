NASA DRL Data Storage Manager (DSM) as modified and maintained by the USF IMaRS group.

# Dev Setup

Developed originally using Eclipse, currently developed with IntelliJ IDEA. The `.idea` files included should include necessary project configuration for IntelliJ IDEA.

Most necessary libraries should be included with the following exceptions: `geo`, `nsls`, `properties`, and `interp` are to be installed in `dsm/..` 

IPOPP installs these and `dsm` in `~/drl` by default so there should be no configuration required to use this as a drop-in replacement in an existing IPOPP install.

just build w/ `build.sh` and you should be all set.


# TODO

## old, unverified TODOs:

1. The DSM library expects the IS computer to be named "IS", and there is no way to change the name. ProductStore and AncillaryDepot each has a static method to change the name. DSM.jar does not have access to a changed name, so it cannot use the method. Knowing the IS name is important to determine if something is published.
Update: The importance of this issue may be mitigated now that I have a "published" flag in the database.

2. One cannot store ancillaries in the top level IS directory. "null" is not acceptable as a directory in the database.

3. I need to check all agents to distinguish between "/" and "\" when doing ftp. Otherwise, NISGS is OS-dependent.

4. The properties files need relative directories.

5. You need to look at rollback whenever you do a DB update on exception.

6. Remove deprecated methods.


# ChangeLog
```
    1.0     Monday, January 10, 2005
            The first release.
    1.0.1   Thursday, January 20, 2005
            I added putTimedAncillary. I established a convention that the
            spacecraft name is all uppercase. I changed all methods to ensure this.
    1.0.2   Tuesday, January 25, 2005
            I changed Product to remove its own time converter (SimpleDateFormat object)
            and to use the one in Utility instead. Product's version did not account 
            for GMT-0.
    1.1.0   Thursday, January 27, 2005
            Added "gov" to the package string so it is now gov.nasa.gsfc.nisgs.dsm.
            DSM.java: Changed reserveProduct signatures so that sleepSeconds is of
            type Integer instead of int per user request.
            Added Product[] getProducts(int passID), Pass[] getPasses()
            Product.java: Added setCenter and setCorners method with Number arguments.
    1.1.1   Tuesday, February 08, 2005
            Product.java: Added setAgent(String) and changed the default agent string 
            to "****". The Product(Product) constructor would throw an exception before
            these changes were made.
            DSM.java: Added a getPass() method that does not require a station name.
            Pass.java: Added contains() method.
    1.1.2   Wednesday, February 09, 2005
            Product.java: Added productType argument to Product(Product) constructor.
            We split DSMAdministrator.java from DSM.java. DSM.java now only
            contains methods used by the community, and DSMAdministrator has DSM
            agent and administrative methods. DSMAdministrator derives from DSM.
            Utility.java: Made quote() public.
    1.2     Wednesday, February 23, 2005
            This version uses a modified set of database tables, and it will not 
            work with the earlier database version. I have added site and directory
            information to the DB. This version keeps track of site and directory
            location for products and ancillary data.
    2.0     Monday, March 14, 2005        
            This version uses the 1.2 database with many new methods including
            reserving products by wildcard product types and recognising NISGS
            system properties.
    3.0     Tuesday, June 21, 2005
            A major upgrade. Introduces DSMR, which copies files on demand, and
            deletes Ancillaryman. PdsMover no longer sends PDS files to NISDS1.
            Adds TransferCommands table to the database.
    3.5     Wednesday, July 13, 2005
            Fixed subtable support and added getAttribute to Product class.
    3.10    Made it more robust to errors.
    3.14    Added methods to delete resources and ancillaries. Added "addResource()."
    3.15    Tuesday, November 29, 2005
            Changed DB tables TLEs to SatTimeAncillaries and TLE_Sites to
            SatTimeAncillarySites, which generalized the table. Added support
            for other spacecraft-time ancillaries such as LUTs.
            Enhanced Caption so that it inserts geolocation into products.
            Use the latest version of the ftp library.
    3.16    Thursday, December 01, 2005
            Added delete protection to products, passes, and ancillaries.
            Added directory transfer in DSMR for dem-like ancillaries.
    3.18    Wednesday, January 18, 2006
            Added "published" flag to database. Made sleep time in agents a -D parameter.
    3.19    Wednesday, January 25, 2006
            Fixed capsule problem. It threw a null pointer exception if it tried to
            create an ancestor Product that was unpublished. I added "published" to
            Resource to distinguish between unpublished and deleted.
            Added a creation date to Pass. Hoover deletes passes by creation date.
    3.20    Tuesday, February 07, 2006
            I added a "delete" flag to products and ancillaries in the database.
    3.22    2/24/2006
            I fixed a problem in DSM's fetchProduct method in which they would block
            forever if they could not get a product.
    3.23    2/24/2006-3/7/06
            KR -- added a check in PdsPassCreate to throw it pass "slivers" below 60 seconds
            KR -- copied from an older DMS directory the subdirs Test and Support that may be
                  useful in the future
    4.0.0  2016-03-16 
            USF IMaRS production checkin. Uncertain what changes made since 3.23
    4.0.1  2016-03-17
            Fixed memory leak(s) w/ PdsMover
    4.1.0  2016-03-30
            Fixed handling of ReserveProductLikeProductType to allow wildcards to select
            multiple products within the same pass. Required a full rewrite of 
            Reservation.reserve(). Tested only manually; working in IMaRS oc & sst pipelines.
```
