/* I used this script to create the initial dsm database.
 * If you run it against an existing database, you will destroy the station
 * and product type tables, which are human-built. That would not be advisable.
 */
USE DSM;

DROP TABLE IF EXISTS Products;
CREATE TABLE Products (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    productType VARCHAR(128) NOT NULL,          #reference to ProductTypes
    pass MEDIUMINT UNSIGNED NOT NULL,          #reference to Passes(id)
    startTime DATETIME NOT NULL,
    stopTime DATETIME NOT NULL,
    creation DATETIME NOT NULL,
    agent VARCHAR(32) NOT NULL DEFAULT '****', #who inserted this product?
    centerLatitude FLOAT NULL,
    centerLongitude FLOAT NULL,
    northLatitude FLOAT NULL,
    southLatitude FLOAT NULL,
    eastLongitude FLOAT NULL,
    westLongitude FLOAT NULL,
    subproduct VARCHAR(32) NULL,               #reference to table name
    algorithm VARCHAR(32),
    algorithmVersion VARCHAR(32) NOT NULL DEFAULT '0',
    hasThumbnails TINYINT(1) NOT NULL DEFAULT '0',
    deleteProtected TINYINT(1) NOT NULL DEFAULT '0',
    deleteMark TINYINT(1) NOT NULL DEFAULT '0',    #marked for deletion
    markerId MEDIUMINT UNSIGNED NOT NULL DEFAULT 0, #product Id of marker
    INDEX (productType),
    INDEX (pass),
    INDEX (startTime)
) ENGINE=InnoDB COMMENT="The primary table; the products list";

DROP TABLE IF EXISTS ProductionDataSets;
CREATE TABLE ProductionDataSets (
    product MEDIUMINT UNSIGNED NOT NULL PRIMARY KEY,       #reference to Products(id)
    packetCount MEDIUMINT UNSIGNED NOT NULL DEFAULT '0',
    gapCount MEDIUMINT UNSIGNED NOT NULL DEFAULT '0',
    missingPacketCount MEDIUMINT UNSIGNED NOT NULL DEFAULT '0'
) ENGINE=InnoDB COMMENT="PDS Level 0 Products subtable";

DROP TABLE IF EXISTS Markers;
CREATE TABLE Markers (
    product MEDIUMINT UNSIGNED NOT NULL,
    gopherColony VARCHAR(32) NOT NULL,
    /* 0=running, 1=completed, >=2 means fault */
    status TINYINT(1) NOT NULL DEFAULT '0',
    /* Unique string for every NCS station invocation */
    /* (typically "SITEID-processid") */
    location VARCHAR(64) NOT NULL,
    UNIQUE KEY (product,gopherColony)
) ENGINE=InnoDB COMMENT="Product usage markers";

DROP TABLE IF EXISTS Mutex;
CREATE TABLE Mutex(
    i INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB COMMENT="Table used for joins to allow atomic non-duplicate insert into Markers" ;
INSERT INTO Mutex(i) VALUES (0), (1);

DROP TABLE IF EXISTS Passes;
CREATE TABLE Passes (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    station VARCHAR(32) NOT NULL,              #reference to Stations(name)
    spacecraft VARCHAR(32) NOT NULL,
    aos DATETIME NOT NULL,
    los DATETIME NOT NULL,
    centerLatitude FLOAT NULL,
    centerLongitude FLOAT NULL,
    northLatitude FLOAT NULL,
    southLatitude FLOAT NULL,
    eastLongitude FLOAT NULL,
    westLongitude FLOAT NULL,
    deleteProtected TINYINT(1) NOT NULL DEFAULT '0',
    creation DATETIME NOT NULL
) ENGINE=InnoDB COMMENT="A spacecraft pass"; 

DROP TABLE IF EXISTS Stations;
CREATE TABLE Stations (
    name VARCHAR(32) NOT NULL PRIMARY KEY,
    path VARCHAR(255) NOT NULL DEFAULT '',
    description VARCHAR(255) NOT NULL DEFAULT '',
    latitude FLOAT NULL,
    longitude FLOAT NULL,
    height FLOAT NULL
) ENGINE=InnoDB COMMENT="An extended description of a station";

DROP TABLE IF EXISTS ProductTypes;
CREATE TABLE ProductTypes (
    name VARCHAR(128) NOT NULL PRIMARY KEY,
    spacecraft VARCHAR(32) NOT NULL,
    sensor VARCHAR(32) NOT NULL,
    description VARCHAR(255) NOT NULL,
    level VARCHAR(32) NULL,
    is_directory VARCHAR(128) NULL
) ENGINE=InnoDB COMMENT="A product may reference one product type. Optional but encouraged.";

DROP TABLE IF EXISTS Directories;
CREATE TABLE Directories (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(128) NOT NULL,
    UNIQUE KEY (path)
) ENGINE=InnoDB COMMENT="A list of directories";

DROP TABLE IF EXISTS Resources;
CREATE TABLE Resources (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product MEDIUMINT UNSIGNED NOT NULL,       #reference to Products(id)
    rkey VARCHAR(32) NOT NULL DEFAULT 'DATA',
    path VARCHAR(128) NOT NULL,                #no directory information
    description VARCHAR(128),
    published TINYINT(1) NOT NULL DEFAULT '0',
    INDEX (product),
    INDEX (path)
) ENGINE=InnoDB COMMENT="Each product has one or more resources (files,urls,etc)";

DROP TABLE IF EXISTS ResourceSites;
CREATE TABLE ResourceSites (
    resource MEDIUMINT UNSIGNED NOT NULL,      #reference to Resource(id)
    site VARCHAR(32) NOT NULL,                 #reference to Sites(id)
    directory MEDIUMINT UNSIGNED NOT NULL,     #reference to Directories(id)
    creation DATETIME NOT NULL,
    PRIMARY KEY (resource,site),
    INDEX (resource)
) ENGINE=InnoDB COMMENT="A linking table for Resources, Directories, and Sites";

DROP TABLE IF EXISTS Thumbnails;
CREATE TABLE Thumbnails (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(255) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB COMMENT="Each product may have one or more thumbnails (usually jpegs)";

DROP TABLE IF EXISTS ProductThumbnails;
CREATE TABLE ProductThumbnails (
    product MEDIUMINT UNSIGNED NOT NULL,       #reference to Products(id)
    thumbnail MEDIUMINT UNSIGNED NOT NULL,     #reference to Thumbnails(id)
    PRIMARY KEY (product,thumbnail),
    INDEX (product)
) ENGINE=InnoDB COMMENT="A linking table for Products and Thumbnails";

DROP TABLE IF EXISTS Ancestors;
CREATE TABLE Ancestors (
    product MEDIUMINT UNSIGNED NOT NULL,       #reference to Products(id)
    ancestor MEDIUMINT UNSIGNED NOT NULL,      #reference to Products(id)
    PRIMARY KEY (product,ancestor),
    INDEX (product),
    INDEX (ancestor)
) ENGINE=InnoDB COMMENT="A linking table for Products and ancestor Products";
 
DROP TABLE IF EXISTS Contributors;
CREATE TABLE Contributors (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    path VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    INDEX (path)
) ENGINE=InnoDB COMMENT="A list of files and URLs that were used to create a product.";
 
DROP TABLE IF EXISTS ProductContributors;
CREATE TABLE ProductContributors (
    product MEDIUMINT UNSIGNED NOT NULL,       #reference to Products(id)
    contributor MEDIUMINT UNSIGNED NOT NULL,   #reference to Contributors(id)
    PRIMARY KEY (product,contributor),
    INDEX (product)
) ENGINE=InnoDB COMMENT="A linking table for Products and contributing resources";

/* static ancillary */
DROP TABLE IF EXISTS StaticAncillaries;
CREATE TABLE StaticAncillaries (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    akey VARCHAR(32) NOT NULL,
    path VARCHAR(255) NOT NULL,                #no directory information
    description VARCHAR(255),
    deleteProtected TINYINT(1) NOT NULL DEFAULT '0',
    deleteMark TINYINT(1) NOT NULL DEFAULT '0',    #marked for deletion
    published TINYINT(1) NOT NULL DEFAULT '0',
    UNIQUE KEY (akey)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS StaticAncillarySites;
CREATE TABLE StaticAncillarySites (
    aid MEDIUMINT UNSIGNED NOT NULL,           #reference to StaticAncillaries(id)
    site VARCHAR(32) NOT NULL,                 #reference to Sites(id)
    directory MEDIUMINT UNSIGNED NOT NULL,     #reference to Directories(id)
    fault TINYINT(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (aid,site)
) ENGINE=InnoDB COMMENT="A linking table for StaticAncillaries, Directories and Sites";

/* UTCPOLE, LEAPSEC, and other time-sorted ancillary files: f(keyword,time) */
DROP TABLE IF EXISTS TimeAncillaries;
CREATE TABLE TimeAncillaries (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    time DATETIME NOT NULL,
    path VARCHAR(255) NOT NULL,                #no directory information
    akey VARCHAR(32) NOT NULL,
    deleteProtected TINYINT(1) NOT NULL DEFAULT '0',
    deleteMark TINYINT(1) NOT NULL DEFAULT '0',    #marked for deletion
    published TINYINT(1) NOT NULL DEFAULT '0',
    INDEX (akey(8),time)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS TimeAncillarySites;
CREATE TABLE TimeAncillarySites (
    aid MEDIUMINT UNSIGNED NOT NULL,           #reference to TimeAncillaries(id)
    site VARCHAR(32) NOT NULL,                 #reference to Sites(id)
    directory MEDIUMINT UNSIGNED NOT NULL,     #reference to Directories(id)
    fault TINYINT(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (aid,site)
) ENGINE=InnoDB COMMENT="A linking table for TimeAncillaries, Directories and Sites";

/* Ancillaries that are functions of keyword, spacecraft, and time. TLEs are here. */
DROP TABLE IF EXISTS SatTimeAncillaries;
CREATE TABLE SatTimeAncillaries (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    spacecraft VARCHAR(32) NOT NULL,
    time DATETIME NOT NULL,
    path VARCHAR(128) NOT NULL,                #no directory information
    akey VARCHAR(32) NOT NULL,
    deleteProtected TINYINT(1) NOT NULL DEFAULT '0',
    deleteMark TINYINT(1) NOT NULL DEFAULT '0',    #marked for deletion
    published TINYINT(1) NOT NULL DEFAULT '0',
    INDEX (akey(8),spacecraft,time)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS SatTimeAncillarySites;
CREATE TABLE SatTimeAncillarySites (
    aid MEDIUMINT UNSIGNED NOT NULL,           #reference to SatTimeAncillaries(id)
    site VARCHAR(32) NOT NULL,                 #reference to Sites(id)
    directory MEDIUMINT UNSIGNED NOT NULL,     #reference to Directories(id)
    fault TINYINT(1) NOT NULL DEFAULT '0',
    PRIMARY KEY (aid,site)
) ENGINE=InnoDB COMMENT="A linking table for SatTimeAncillaries, Sites, and Directories";

/* Clients add requests to this queue to have files or products transferred to
their site. A transfer command asks: "Please copy item <tableId> in table <table>
to my site <site>." When the DSM completes the request, it sets 'complete' to a
non-zero. 1=success, 2+ = unrecoverable failure. */
DROP TABLE IF EXISTS TransferCommands;
CREATE TABLE TransferCommands (
    id MEDIUMINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tableName VARCHAR(32) NOT NULL,
    tableId MEDIUMINT UNSIGNED NOT NULL,
    site VARCHAR(32) NOT NULL,                 #reference to Sites(id)
    creation DATETIME NOT NULL,
    complete TINYINT(1) NOT NULL DEFAULT '0'
) ENGINE=InnoDB COMMENT="A queue for site-to-site file transfer requests";

/* This table is intended to be the database equivalent of a Java
	Properties container.  The sizes are conservatively small in the
	vague hope of portability.  NOTE that MySQL will not accept a
	tkey size > 255 (!).  Relational database random implementation
	limitations suck.
*/

DROP TABLE IF EXISTS NisgsProperties;
CREATE TABLE NisgsProperties (
    tkey VARCHAR(255) NOT NULL PRIMARY KEY,
    tvalue VARCHAR(1024)
) ENGINE=InnoDB COMMENT "A property list for all NISGS agents";

/* Table used to map algorithm names to algorithm group tags
    (gopherColony in Markers table).
    NOTE this table is declared with case-sensitive collation,
    which should be the default for all string (VARCHAR) comparisons
    in the IPOPP database.  We should probably sprinkle
    COLLATE utf8_bin
    declarations over this entire file.
*/
DROP TABLE IF EXISTS Algorithms;
CREATE TABLE Algorithms (
    name VARCHAR(32) NOT NULL COLLATE utf8_bin,
    gopherColony VARCHAR(32) NOT NULL COLLATE utf8_bin,
    PRIMARY KEY (name),
    UNIQUE (name, gopherColony)
) ENGINE=InnoDB COMMENT="Maps algorithm names to algorithm group tags";
