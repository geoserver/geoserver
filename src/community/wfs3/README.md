# WFS 3 experimental prototype

This module contains a prototype based on an early draft specification of WFS 3.0, developed as part of the 
WFS 3.0 hackaton, see:

* https://github.com/opengeospatial/WFS_FES
* https://github.com/opengeospatial/wfs3hackathon/blob/master/Implementations.md

The module supports a subset of the current specification as of March 7th 2018.
Parts missing:

* HTML outputs for all resources (freemarker based, with the exception of the API one which could use that 
  static page we already have for the documentation, loading dynamically the docs from JSON, which would
  be the only dynamic bit)
* Filtering by attribute (semantics to be clarified, see https://github.com/opengeospatial/WFS_FES/issues/67 )
* Single feature encoding for GeoJSON and GML 
* Expected MIME type for both GeoJSON and GML, and newer GeoJSON version encoding (EITF spec)
* OGR needs schemas to be listed in the API document for every feature type, however that has yet to enter the spec

Implementation wise:
* The module basically acts as an internal proxy around WFS 2.0, using a  servlet filter to adapt protocols
* The APIDocument class needs to be rewritten based on swagger-core, throwing away the current set of made-up-on-the-fly
  beans supporting Jackson encoding
* All tests are "fake" and need actual assertions
 

