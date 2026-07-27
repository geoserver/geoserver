# IAU Authority Planetary CRS Extension

Packages the GeoTools `gt-iau-wkt` library so GeoServer can serve data in
International Astronomical Union coordinate reference systems — over 2000 CRSs
covering planetary bodies (Mars, the Moon, Jupiter moons, etc.).

## User Manual

* [Planetary CRS support](https://docs.geoserver.org/latest/en/user/extensions/iau/index.html) (GeoServer User Manual)

## Developer Notes

This extension has no GeoServer-specific Java code. It exists purely to produce
a distribution zip bundling `gt-iau-wkt` alongside the standard GeoServer
license/README files: dropping the zip into `webapps/geoserver/WEB-INF/lib`
and restarting GeoServer makes the IAU codes visible in the SRS list and usable
in GetMap/GetFeature/GetCoverage responses.

The end-user install instructions shipped inside the zip live in
`src/release/extensions/iau/iau-readme.md`.
