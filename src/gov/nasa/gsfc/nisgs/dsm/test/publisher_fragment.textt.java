/*
Copyright (c) 1999-2007, United States Government, as represented by
the Administrator for The National Aeronautics and Space Administration.
All rights reserved.
*/
///*
// * 
// *
 //* SAVE this 
// * 
//String sql = "SELECT Resources.id,Resources.path,Directories.path,Products.productType " +
 //               "FROM Resources,Directories,Products,(SELECT * FROM ResourceSites WHERE site=" +
 //               Utility.quote(dsm.getSite()) +
 //               ") AS LocalSite LEFT JOIN (SELECT * FROM ResourceSites WHERE site=" +
  //              Utility.quote(is_site) +
  //              ") AS ISSite ON LocalSite.resource=ISSite.resource " +
  //              "WHERE ISSite.resource is null AND Directories.id=LocalSite.directory " +
  //              "AND LocalSite.resource=Resources.id AND Resources.product=Products.id";
//*/
package gov.nasa.gsfc.nisgs.dsm.test;
