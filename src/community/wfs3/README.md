# WFS 3 prototype

This module contains a prototype based on the current draft specification of WFS 3.0, developed as part of the 
WFS 3.0 hackaton first, and then further developed to match Draft 1 spec and conformance tests, see:

* https://github.com/opengeospatial/WFS_FES
* https://github.com/opengeospatial/wfs3hackathon/blob/master/Implementations.md

Implementation wise:
* The module basically acts as an internal proxy around WFS 2.0, using a  servlet filter to adapt protocols. The long term approach would likely be to have a new MVCDispatcher that allows usage of Spring annotations instead (TBD).
 

