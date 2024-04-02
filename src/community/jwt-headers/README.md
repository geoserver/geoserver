The JWT Headers module functionality is mirrored in GeoNetwork.  The module is split into two sub-modules:

1. `jwt-headers-core`

    This contains shared functionality between GeoServer and GeoNetwork. This does things like Access Token validation and JSON processing.       

2. `gs-jwt-headers`

    This is the GeoServer specific components to integrate the functionality inside GeoServer.

Please see the README.md files inside these two modules.