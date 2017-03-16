-- MySQL dump 10.9
--
-- Host: localhost    Database: DSM
-- ------------------------------------------------------
-- Server version	4.1.10-standard

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

--
-- Table structure for table `ProductTypes`
--

DROP TABLE IF EXISTS `ProductTypes`;
CREATE TABLE `ProductTypes` (
  `name` varchar(32) NOT NULL default '',
  `spacecraft` varchar(32) NOT NULL default '',
  `sensor` varchar(32) NOT NULL default '',
  `description` varchar(255) NOT NULL default '',
  `level` varchar(4) default NULL,
  `is_directory` varchar(128) default NULL,
  PRIMARY KEY  (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='A product may reference one product type. Optional but encou';

--
-- Dumping data for table `ProductTypes`
--


/*!40000 ALTER TABLE `ProductTypes` DISABLE KEYS */;
LOCK TABLES `ProductTypes` WRITE;
INSERT INTO `ProductTypes` VALUES ('aqua.0141.pds','AQUA','ceres','ceres 141 pds','0','gsfcdata/aqua/ceres/'),('aqua.0142.pds','AQUA','ceres','ceres 142 pds','0','gsfcdata/aqua/ceres/'),('aqua.0157.pds','AQUA','ceres','ceres 157 pds','0','gsfcdata/aqua/ceres/'),('aqua.0158.pds','AQUA','ceres','ceres 158 pds','0','gsfcdata/aqua/ceres/'),('aqua.0259.pds','AQUA','amsu','amsu 259 pds','0','gsfcdata/aqua/amsu/'),('aqua.0260.pds','AQUA','amsu','amsu 260 pds','0','gsfcdata/aqua/amsu/'),('aqua.0261.pds','AQUA','amsu','amsu 261 pds','0','gsfcdata/aqua/amsu/'),('aqua.0262.pds','AQUA','amsu','amsu 262 pds','0','gsfcdata/aqua/amsu/'),('aqua.0289.pds','AQUA','amsu','amsu 289 pds','0','gsfcdata/aqua/amsu/'),('aqua.0290.pds','AQUA','amsu','amsu 290 pds','0','gsfcdata/aqua/amsu/'),('aqua.0342.pds','AQUA','hsb','hsb 342 pds','0','gsfcdata/aqua/hsb/'),('aqua.0402.pds','AQUA','amsr','amsr 402 pds','0','gsfcdata/aqua/amsr/'),('aqua.0404.pds','AQUA','airs','airs 404 pds','0','gsfcdata/aqua/airs/'),('aqua.0405.pds','AQUA','airs','airs 405 pds','0','gsfcdata/aqua/airs/'),('aqua.0406.pds','AQUA','airs','airs 406 pds','0','gsfcdata/aqua/airs/'),('aqua.0407.pds','AQUA','airs','airs 407 pds','0','gsfcdata/aqua/airs/'),('aqua.0414.pds','AQUA','airs','airs 414 pds','0','gsfcdata/aqua/airs/'),('aqua.0415.pds','AQUA','airs','airs 415 pds','0','gsfcdata/aqua/airs/'),('aqua.0416.pds','AQUA','airs','airs 416 pds','0','gsfcdata/aqua/airs/'),('aqua.0417.pds','AQUA','airs','airs 417 pds','0','gsfcdata/aqua/airs/'),('aqua.gbad.att','AQUA','gbad','GBAD Attitude','1','gsfcdata/aqua/gbad/'),('aqua.gbad.eph','AQUA','gbad','GBAD Ephemeris','1','gsfcdata/aqua/gbad/'),('aqua.gbad.pds','AQUA','gbad','gbad pds','0','gsfcdata/aqua/gbad/'),('aqua.modis.chlor_a','AQUA','modis','chlorophyll_a','2','gsfcdata/aqua/modis/level2/'),('aqua.modis.chlor_a.geotiff','AQUA','modis','Chlorophyll_A GEOTIFF','2','gsfcdata/aqua/modis/level2'),('aqua.modis.crefl','AQUA','modis','crefl','2','gsfcdata/aqua/modis/level2/'),('aqua.modis.fire.geotiff','AQUA','modis','MYD014 Fire Detection','2','gsfcdata/aqua/modis/level2'),('aqua.modis.firedetection','AQUA','modis','MYD014 Fire Detection','2','gsfcdata/aqua/modis/level2/'),('aqua.modis.mxd01','AQUA','modis','MYD01 Granule','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.mxd021km','AQUA','modis','MYD02 1 km Granule','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.mxd021km.geotiff','AQUA','modis','MYD021KM Infrared GEOTIFF','1','gsfcdata/aqua/modis/level1'),('aqua.modis.mxd02hkm','AQUA','modis','MYD02 1/2 km Granule','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.mxd02hkm.geotiff','AQUA','modis','MYD02HKM True Color GEOTIFF','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.mxd02obc','AQUA','modis','MYD02 OBC','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.mxd02qkm','AQUA','modis','MYD02 1/4 km Granule','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.mxd02qkm.geotiff','AQUA','modis','MYD02QKM Gray Scale GEOTIFF','1','gsfcdata/aqua/modis/level1'),('aqua.modis.mxd03','AQUA','modis','Geolocated Granule','1','gsfcdata/aqua/modis/level1/'),('aqua.modis.ndvi','AQUA','modis','MYD013 Normalized Vegetation Index','2','gsfcdata/aqua/modis/level2/'),('aqua.modis.ndvi.geotiff','AQUA','modis','MYD013 NDVI GEOTIFF','2','gsfcdata/aqua/modis/level2'),('aqua.modis.pds','AQUA','modis','modis pds','0','gsfcdata/aqua/modis/level0/'),('aqua.modis.sst','AQUA','modis','Sea Surface Temperature','2','gsfcdata/aqua/modis/level2/'),('aqua.modis.sst.geotiff','AQUA','modis','Sea Surface Temperature GEOTIFF','2','gsfcdata/aqua/modis/level2'),('terra.modis.chlor_a','TERRA','modis','chlorophyll_a','2','gsfcdata/terra/modis/level2/'),('terra.modis.chlor_a.geotiff','TERRA','modis','Chlorophyll_A GEOTIFF','2','gsfcdata/terra/modis/level2/'),('terra.modis.crefl','TERRA','modis','crefl','2','gsfcdata/terra/modis/level2/'),('terra.modis.fire.geotiff','TERRA','modis','MOD014 Fire Detection','2','gsfcdata/terra/modis/level2/'),('terra.modis.firedetection','TERRA','modis','MOD014 Fire Detection','2','gsfcdata/terra/modis/level2/'),('terra.modis.firedetection.geotif','TERRA','modis','MOD014 Fire Detection','2','gsfcdata/terra/modis/level2/'),('terra.modis.mxd01','TERRA','modis','MOD01 Granule','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd021km','TERRA','modis','MOD02 1 km Granule','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd021km.geotiff','TERRA','modis','MOD021KM Infrared GEOTIFF','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd02hkm','TERRA','modis','MOD02 1/2 km Granule','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd02hkm.geotiff','TERRA','modis','MOD02HKM True Color GEOTIFF','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd02obc','TERRA','modis','MOD02 OBC','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd02qkm','TERRA','modis','MOD02 1/4 km Granule','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd02qkm.geotiff','TERRA','modis','MOD02QKM Gray Scale GEOTIFF','1','gsfcdata/terra/modis/level1/'),('terra.modis.mxd03','TERRA','modis','Geolocated Granule','1','gsfcdata/terra/modis/level1/'),('terra.modis.ndvi','TERRA','modis','MOD013 Normalized Vegetation Index','2','gsfcdata/terra/modis/level2/'),('terra.modis.ndvi.geotiff','TERRA','modis','MOD013 NDVI GEOTIFF','2','gsfcdata/terra/modis/level2/'),('terra.modis.pds','TERRA','modis','modis pds','0','gsfcdata/terra/modis/level0/'),('terra.modis.sst','TERRA','modis','Sea Surface Temperature','2','gsfcdata/terra/modis/level2/'),('terra.modis.sst.geotiff','TERRA','modis','Sea Surface Temperature GEOTIFF','2','gsfcdata/terra/modis/level2/');
UNLOCK TABLES;
/*!40000 ALTER TABLE `ProductTypes` ENABLE KEYS */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

