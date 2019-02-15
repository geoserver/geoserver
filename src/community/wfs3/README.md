# WFS 3 prototype

This module contains a prototype based on the current draft specification of WFS 3.0, developed as part of the 
WFS 3.0 hackaton first, and then further developed to match Draft 1 spec and conformance tests, see:

* https://github.com/opengeospatial/WFS_FES
* https://github.com/opengeospatial/wfs3hackathon/blob/master/Implementations.md

Implementation wise:
* The module basically acts as an internal proxy around WFS 2.0, using a  servlet filter to adapt protocols. The long term approach would likely be to have a new MVCDispatcher that allows usage of Spring annotations instead (TBD).
 
This implementation contains the following prototype WFS3 extensions:
* Tiles extension, returning MapBOX/JSON/TopoJSON tiles. It does not cache tiles and will likely be removed when WFS3 is pushed to supported land, but served as a base to study a possible WMTS 2.0 API and WFS 3 tile extension in Testbed 14. Some bits can likely be re-used once a final version comes out.
* Styles extension, with the ability to get/put/delete styles (must be secured using service security). The API is not really compatible with GeoServer style management and should also be removed, but was used to provide feedback to OGC in the vector tiles pilot, which will likely be used for WMTS 2.0 (and some bits can likely be re-used once a final version comes out).