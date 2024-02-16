# GeoIP {: #monitor_geoip }

The monitor extension has the capability to integrate with the [MaxMind GeoIP](http://www.maxmind.com/en/geolocation_landing) database in order to provide geolocation information about the origin of a request. This functionality is not enabled by default.

!!! note

    At this time only the freely available GeoLite City database is supported.

## Enabling GeoIP Lookup

In order to enable the GeoIP lookup capabilities

1.  Download the [GeoLite City](http://dev.maxmind.com/geoip/geolite) database.
2.  Uncompress the file and copy **`GeoLiteCity.dat`** to the **`monitoring`** directory.
3.  Restart GeoServer.
