This module contains the JWT Headers code that is required specifically by GeoServer.

package `filter` contains the core functionality of the authentication filter. 

Package `filter\details` is the GeoServer Web Auth Details implementation that tracks which configuration was used for an authentication.  This is to facility auto-logout.
 
Package `filter\web` is the wicket infrastructure needed for the configuration GUI.

Please note that (gs-jwt-headers) `GeoserverJwtHeaderFilterConfig` wraps a (jwt-headers-util) `JwtConfiguration` for supplying configuration details.