GeoServer 2.1-RC1 (January 16, 2011)
------------------------------------

The first GeoServer 2.1 release candidate. Some notable changes from beta3 
include:

  * RESTConfig api improvements
  * Security framework enhancements
  * GWC disk quota feature

The entire changelog is available here:

  http://jira.codehaus.org/browse/GEOS/fixforversion/16982
 
This release is based on GeoTools 2.7-beta1.

GeoServer 2.1-beta3 (December 17, 2010)
---------------------------------------

The third beta release of GeoServer 2.1.

The most notable change since beta2 is the additioon of WMS 1.3 support.

The entire change log is avaialble here:

  http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&version=17024 
This release is based on GeoTools 2.7-M5.

GeoServer 2.1-beta2 (December 02, 2010)
----------------------------------------

The second beta release of GeoServer 2.1.

The entire change log is available here:

  http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&version=15130

This release is based on the GeoTools 2.7-M4 release.

GeoServer 2.1-beta1 (September 02, 2010)
----------------------------------------

The first release of GeoServer 2.1. A number of notable features have made it
into the 2.1 stream. These include:

 * wms cascading
 * virtual services
 * sql layers
 * wps 
 * uom support
 * dpi scaling

The entire change log is available here:

  http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&version=15082

This release is based on the GeoTools 2.7-M3 release.
 

GeoServer 2.0-RC1 (August 14, 2009)
-----------------------------------
The first release candidate version for 2.0. The focus for this release has been
mostly on the user interface brush up to ensure a really nice user experience. With
55 issues fixed, including 30 bug fixes and 12 improvements, this first Release Candidate
is called to become the 2.0 milestone.
Thanks to all the developers for the hard work and specially the user community for
the bug reports.

The entire changelog for this release can be found:

http://jira.codehaus.org/browse/GEOS/fixforversion/15379

This release is based on GeoTools 2.6.x (revision 33745)

GeoServer 2.0-beta2 (July 13, 2009)
-----------------------------------

The second beta release for 2.0. The focus for this release cycle has been qa
and continuing to hammer out the bugs with 75 issues resolved. This release also
marks the official addition of the "app-schema" extension which allows GeoServer
to serve up complex feature content and truly support GML application schemas.
Special thanks to AuScope for all their hard work. 

The entire changelog for this release can be found:

http://jira.codehaus.org/browse/GEOS/fixforversion/15284

This release is based on GeoTools 2.6.x (revision 33554)

GeoServer 2.0-beta1 (May 25, 2009)
----------------------------------

This is the first beta in the 2.x series. Development has focused
mainly around the new Wicket based user interface and catalog
persistence, closing a wopping 101 issues in our bug tracker,
among improvements and bug fixes. The new UI is getting very close
to its final appearance and the configuration subsystem has been
undergoing more testing.

This release is based on GeoTools 2.6.x (revision 33092)

GeoServer 2.0-alpha2 (April 15, 2009)
-------------------------------------

The next big milestone release for 2.0. This is the second major milestone 
release of 2.x. Development has focused mainly around the new Wicket based
user interface, and around the backend persistence and configuration sub
systems.

This release is based on GeoTools 2.6.x (revision 32802)


GeoServer 2.0.0-alpha1 (August 11, 2008)
----------------------------------------

This is the first experimental release of the 2.0.0 which introduces the new,
revamped UI.

This release is based on Geotools 2.5.x (revision 31153)


GeoServer 1.7.0-beta2 (July 30, 2008)
-------------------------------------

This is the second beta release of the 1.7.0, and is still unstable.  This release
includes 35 bug fixes and improvements, including:

* Optimized KML regionating output
* Improved per-layer security

For more information on bug fixes, please see:
http://jira.codehaus.org/browse/GEOS/fixforversion/14377

This release is based on Geotools 2.5.x (revision 31101).


GeoServer 1.7.0-beta1
---------------------

The first beta release of 1.7. This release is a unstable release. Major
functionality includes:

 * per layer security
 * WCS 1.1.1 support
 * WFS Xlink support
 * GDAL integration with support for MrSID and ECW formats
 * new configuration backend

This release is based on geotools 2.5.x:
  revision = 30605
  svn tag = 'geoserver-2.0.0-alpha1'


GeoServer 1.7.0-alpha1 
----------------------

The first release of the 1.7.x branch. This release is experimental and marked
as an alpha. The major developments of 1.7 are Xlink support for WFS 1.1 and a
full implementation of WCS 1.1.1.

This release is based on GeoTools 2.5.x:
  revision = 29730, 
  svn tag = 'geoserver-1.7.0-alpha1'


GeoServer 1.6.4b
---------------
This release is based off of Geotools 2.4.4 and fixes the following issue:
http://jira.codehaus.org/browse/GEOS-1942


GeoServer 1.6.4
---------------
This release contains 35 patches and bugfixes since 1.6.3.  While mainly a bugfix release, this version has:

* Better default formatting of KML output
* Improved and updated Windows installer
* Support for Freemarker templates for coverage layers

See the entire 1.6.4 changelog:
http://jira.codehaus.org/browse/GEOS/fixforversion/14170

This release is based off of Geotools 2.4.3.


GeoServer 1.6.3
---------------
This release contains over 30 patches and improvements since 1.6.2.  In particular, this new version has:

* Watermarking
* Ability to limit the SRS list in the WMS capabilities
* Better coverage reprojection
* Optimized KML generation

Check out the entire 1.6.3 changelog:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=14102

This release is based off of Geotools 2.4.2.


GeoServer 1.6.2a
----------------
Fixed path parsing issue in demo section.


GeoServer 1.6.2
---------------
This release contains one small but important patch since 1.6.1:

* Data_dir inside .war distribution is now hidden by default

See http://jira.codehaus.org/browse/GEOS-1785

This release is based off of Geotools 2.4.1


Changes:
http://jira.codehaus.org/browse/GEOS-1785


GeoServer 1.6.1
---------------
This release has over 40 patches and improvements since 1.6.0. They include

* Feature type aliases
* Max features support, defined per feature type
* GetFeatureInfo on coverages
* Generate PDFs from coverages
* UpdateSequence support in WMS and WCS

And many many more. Check out the entire 1.6.x changelog:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&&fixfor=14070&pid=10311&sorter/field=issuekey&sorter/order=DESC

This release is based off of Geotools 2.4.1. 

GeoServer 1.6.0
---------------

The official 1.6.0 release of GeoServer. The 1.6 branch contians new features not present in 1.5. The highlights include:

* WFS 1.1 
* WMS performance enhancements
* Security
* WFS Versioning Extension (experimental)
* WFS reprojection 
* KML Superoverlays, Templates, and general improvements 
* GeoJSON and GeoRSS output formats

And many many more. Check out the entire 1.6.x changelog:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&&fixfor=12871&fixfor=13993&fixfor=13874&fixfor=13631&fixfor=13619&fixfor=13675&fixfor=13547&fixfor=13250&fixfor=13200&pid=10311&sorter/field=issuekey&sorter/order=DESC

This release is based off of Geotools 2.4.0. 

GeoServer 1.6.0-RC3
-------------------

This release is the third release candidate for 1.6.0. It addresses some very 
important performance and scalability issues regarding memory leaks. With a 
few other odds and ends. The changlog is available here:

http://jira.codehaus.org/secure/ReleaseNote.jspa?version=13993&styleName=Text&projectId=10311

This release is based off of Geotools 2.4-SNAPSHOT, tag = geoserver-1.6.0-rc3,
revision = 28790

GeoServer 1.6.0-RC2
-------------------

This release is the second release candidate for 1.6.0. It includes some key
bug fixes since RC1. Notable fixes since RC1 include:

* KML / SLD rule processing
* better error reporting for invalid XML requests
* request parameter to enable XML validation
* datastore fixes including PostGIS reprojection and Oracle permissions
* SVG rendering improvements

Also in this release are some notable improvements to the experimental
Versioning WFS support, which includes the addition of
VersionedFeatureCollection.

The entire changelog can be found here:

http://jira.codehaus.org/secure/ReleaseNote.jspa?version=13874&styleName=Text&projectId=10311

This release is based on Geotools 2.4-SNAPSHOT, tag = geoserver-1.6.0-rc2,
revision = 28406

GeoServer 1.6-rc1
---------------------

The main focus of this release is bug fixing, bringing us very close to a final release.
Notable changes:
* more reprojection support in wfs (to handle requests with geometries expressed in a
  SRS other than the feature type native one)
* wfs 1.1 is now usable and conforms to the specification even with lon/lat oriented
  data sources (the default output is lat/lon thought, as the spec mandates)
* some external graphics related issues were fixed, as well as some layer labelling order issues
* component WMS is now supported, so it's possible to direct a wms call to a remote wfs
  source without configuring the wfs layer into GeoServer
* sld library  mode does not conform to the SLD 1.0 specification (this will break some
  client that used the old non conformant behaviour, we'll restore it as a fallback in rc2)
* problems with connection pool exhaustion during GeoServer configuration tshould be gone
* issues in migration from GeoServer 1.5.0/1.5.1/1.5.2 data directory should be fixed
* logging configuration issues should be gone, GeoServer fully uses log4j now
 

For a full change log see:
http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&styleName=Html&version=13631

This release is based on GeoTools 2.4-SNAPSHOT, tag geoserver-1.6-rc1, revision 27907

GeoServer 1.6-beta4
---------------------

The main focus of this release is bug fixing.
We tried to fix most of the outstanding bugs in order to have a final
beta. Notable changes and fixes are:
* logging issues under windows should be fixed
* KML output from layer preview is back at work
* added support for component WMS, that is, the ability to render on the fly data provided by
  another WFS server, where the server and the feature type are specified in the WMS request itself,
  such as: 
  http://localhost:8080/geoserver/wms?...&styles=population&...&layers=topp:states&...&remote_ows_type=WFS&remote_ows_url=http://sigma.openplans.org:8080/geoserver/wfs
* better mime type detection for static file published in data_dir/www
* upgraded GeoJSON output to the latest draft available at geojson.org
* support for right to left languages in labelling
* support for deploy into Oracle Application Server
* improved labelling (labels are now better centered into their halos)
* various minor KML improvements, including the ability to use external graphics for point symbolizers
  and to add ogc filters to KML reflector calls
  
Please download and test, test test. We'll be releasing a RC1 soon, so this is the right time
to point out any problem you may encounter with GeoServer 1.6.0.

For a full change log have a look at:
http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&styleName=Html&version=13619

This release is based on GeoTools 2.4-SNAPSHOT, tag geoserver-1.6-beta4, revision 27634

GeoServer 1.6-beta3
---------------------

The main focus of this release is WMS performance. 
We did introduce a new non antialiasing rendering mode that can be enabled by
appending &format_options=antialias:none to the GetMap request that can generate
images with an optimal palette automatically, greatly reducing the resulting image
size (up to 4 times smaller than the standard paletted output, and 10 times smaller
than the full color png output).
We also worked on rendering performance, making standard rendering up to 2 times faster, and 
up to 6 times faster in the specific case of shapefiles (using the non antialiased rendering
mode, with antialias and translucency in the picture the speed up is not as big, but still
very interesting).

Another major news is the the integration of the WFS datastore in the release, we went
through some testing and we are now capable of WFS cascading. This functionality is still
quite new, so help us test it out.

GeoJSON output is now integrated in the release as a standard output format.

We also had various fixes in WFS, such as the ability to query with bboxes and geometries 
in SRS other than the feature native one.

For a full change log have a look at:
http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&styleName=Html&version=13675

This release is based on GeoTools 2.4-SNAPSHOT, tag geoserver-1.6-beta3, revision 27025

GeoServer 1.6-beta2
---------------------

This is the second beta release in the 1.6 series of GeoServer.
Besides the usual raft of bug fixes, the most notable additions are
templated GetFeatureInfo, a much more configurable logging subsystem, 
and the new connection pooling subystem with much better control 
on the number of opened database connections.

The full change log for this release can be found here:
http://jira.codehaus.org/secure/ReleaseNote.jspa?projectId=10311&styleName=Html&version=13547

This release is based on Geotools 2.4-SNAPSHOT, tag = geoserver-1.6-beta2,
revision 26251.


GeoServer 1.6-beta1
---------------------

This is the first beta release in the 1.6 series of GeoServer 
which contains integrated WFS 1.1 support, along with the 
first version of the security subsystem, templated GeoRSS and KML,
revamped demos and sample requests, faster shapefile rendering,
as well as the option to test out the new versioning WFS module. 

The issue log for this release can be found here:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&fixfor=13250&pid=10311

This release is based on Geotools 2.4-SNAPSHOT, tag = geoserver-1.6-beta1,
revision = 26134


GeoServer 1.6-alpha2
---------------------

This is the first alpha release in the 1.6 series of GeoServer 
which contains integrated WFS 1.1 support. Along with WFS 1.1 
come some other interesting new features:

- Sorting in a GetFeature request
- Reprojection in a GetFeature request
- GML3 Simple Feature Profile 0 as an output format
- Filter 1.1 support
- A dynamic dispatch system for Open Web Services
- Improved xml parsing and encoding 

The issue log for this release can be found here:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=13200

This release is based on Geotools 2.4-SNAPSHOT, tag = geoserver-1.6-alpha2,
revision = 24440

GeoServer 1.5.0-beta1
---------------------

This is the first beta release for the 1.5 sereies of GeoServer.
Major features / improvements of this release include:

- A lot of Bug fixed against 1.4 series
- JAI/ImageIO for Map Producers
- WCS 0.4 (2D Coverages)
- WMS Raster support
- KML Raster support


The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12870

Based on GeoTools 2.3.1-SNAPSHOT

NOTES:
1.In order to get more performances, you should have a complete JAI/ImageIO install (with native interfaces too) into your JRE.
Moreover, we suggest to use the latest daily distributions of JAI 1.1.4 and ImageIO 1.2.
2.Very few CITE Tests are still failing. This distribution passes most of the WMS,WFS and WCS CITE Tests.

GeoServer 1.4.0-RC1
-------------------

This is the first release candidate for the 1.4 series of GeoServer.
Major features / improvements of this release include:

- WMS GetMap filter support
- More robust KML point styling
- Integration with OSCache, please see: http://geoserver.org/display/GEOSDOC/Caching
- Numerous bug fixes and improvements


The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12666

Based on GeoTools 2.2.1

GeoServer 1.4.0-M2-WCS
----------------------

Major features / improvements of this release include:

- WCS 0.4 (2D Coverages)
- WMS Raster support
- KML Raster support

The issue log for this release is here:

Base on Geotools 2.3.0-M0 (tag = 2.3.0-M0)
        JAI      1.1.4+   (https://jai.dev.java.net/)
        ImageIO  1.1+     (https://jai-imageio.dev.java.net/)

GeoServer 1.4.0-M2
------------------

Major features / improvements of this release include:

- Upgraded MapBuilder demo version
- WMS Base-map option
- GetFeatureInfo now supports reprojection
- KML Reprojection

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12950

Base on Geotools 2.2.0 (tag = 2.2.0)


GeoServer 1.4.0-M1
------------------

Major features / improvements of this release include:

- GetMap factoring out to spring extension point
- Developer Documentation 

Along with numerous ui bug fixes coming out of the move to Spring.
Special thanks to the following people for contributions / bug fixing:

- Saul Farber
- Alessio Fabiani

The issue log for this release is here:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&mode=hide&sorter/order=ASC&sorter/field=priority&pid=10311&fixfor=12710

Base on Geotools 2.2.0 (tag = 2.2.0)

GeoServer 1.4.0-M0
------------------

The issue log for this release is here:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=11555

The first release of the GeoServer 1.4.x series. This development 
stream represents a major refactoring of the core codebase. The 
purpose of which is to move to a more component-oriented framework. 

The major changehas been the move to the spring framework. Spring 
is a IOC container that GeoServer now loads to bootstrap itself and 
manages component dependencies in the system.

Based on geotools 2.2.x (tag = geoserver-1.4.0-M0)

Geoserver 1.3.2
---------------
SLD Wizard to create SLD files with a nice user interface.
Up to 60-70% speed increases in WMS rendering.
Built on GeoTools 2.2.x
Including KML/KMZ output formats for WMS.
PDF output format for WMS.
Caching headers for WMS (http://jira.codehaus.org/browse/GEOS-454).
Support for more math functions in filters.
Many big fixes and improvements.

The change log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12332


Geoserver 1.3.1
---------------
Fully tested with GeoTools 2.2.x. This release also contains KML and KMZ support for 
data viewing in Google Earth. For more information visit 
http://geoserver.org/display/GEOSDOC/Google+Earth 


Geoserver 1.3.1 beta
--------------------
The main adition to this release is the move from Geotools 2.1.x to Geotools 2.2.x
Bug fixes and new features listed in Jira will be added once this beta version has been tested
by the community for a couple of days.

The build.xml file has also been reformatted to reflect the changes in the data_dir system, and it 
has been cleaned up.

Geoserver 1.3.0
---------------

The issue log for this release is here:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12106

This is the official 1.3.0 release. 1.3.0 Represents a major milestone for the 
GeoServer project. Months of bug fixes and patches have allowed us to acheive 
our goal of a stable release. Much thanks to everyone who helped out with 
contributions and bug reports.

Based on the geotools 2.1.1 release (tag = 2.1.1)

Geoserver 1.3.0 PR1
-------------------

The issue log for this release is here:

http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12106

This is the first 1.3.0 feature complete release. Changes from RC7 include bug
fixes and documentation.

Based on the geotools 2.1.1.PR0 release (tag = 2.1.1.PR0)

Geoserver 1.3.0 RC7
-------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12105

The major changes since RC6 have been:

* Reorganization of the data directory structure
* SLD Validation
* Lenient CRS Transforms
* Logging
* Http Unit Testing

Based on the geotools 2.1.x branch (tag = geoserver-1.3.0.RC7)

Geoserver 1.3.0 RC6
-------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12080

The major changes since RC5 have been:

* support for Java 5
* option to output verbose GML (boundedBy element per feature)
* handling of form-based POST requests
* Oracle spatial support for MultiPoints
* Fast SVG Renderer - support for heterogeneos geometry types

Based on the geotools 2.1.x branch (tag = geoserver-1.3.0.RC6)

Geoserver 1.3.0 RC5
-------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=12031

The major changes since RC4 have been:
* strategy -- partial buffer. Now the default strategy. Please TEST this one. If you are 
    having weird errors, set WEB-INF/web.xml serviceStratagy back to SPEED.
* NullPointerException in XMLConfigWriter
* GIF - improvments
* Cannot turn off Antialias in config
* GIF -- no feature gives invalid image
* tomcat error re-writing
* better error reporting
* easier upgrade path
* Website upgrade
* Performed more testing with Java 1.5, still not fully complete
* Ant buildfile no work with 1.5.3
* Error while trying to "Apply" or "Load" data config changes


Geoserver 1.3.0 RC4
-------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=11906

The major changes since RC3 have been:
* SVG Support with full support for sld
* Map builder preview with custom configuration works
* Writing logs to disk
* GetLegendGraphic will now find the required style in a multiple styles remote SLD document
* Color Scheme
* Indexed Shapefile support
* Compression/Decompression of remote SLD documents
* Data Store Editor User Interface Improvments
* XML request character set detection
* PostGIS support - more intuitive error messages regarding permissions
* Shapefile support - url handling
* Linux support - start / stop scripts
* Developer Documentation



Geoserver 1.3.0 RC4.SC1
-------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=11906

The major changes since RC3 have been:
* Color Scheme
* Indexed Shapefile support
* Compression/Decompression of remote SLD documents
* Data Store Editor User Interface Improvments
* XML request character set detection
* PostGIS support - more intuitive error messages regarding permissions
* Shapefile support - url handling
* Linux support - start / stop scripts
* Developer Documentation

Geoserver 1.3.0 RC3
-------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=11901

The major changes since RC2 have been:
* You can now have the same FeatureType name for different datasets (each in different namespaces).
* Large improvements to GIF format support
* Improved performance for GetFeature
* New output format for WFS: SHAPE-ZIP  (shapefile-in-zip-file)
* FeatureType summary page with link to Mapbuilder demo for each layer
* Many improvements to the Renderer, labeling, etc...
* Currently compatible with Geotools 2.1.x branch and Geotools 2.2 (trunk)
* added "Strict CITE conformance" checkbox to WFS because of problems in the way CITE test verifies the GetCapabilitites document
* fixed VM crash for labeling (linux only)
* <PropertyIsLike> now optimized in PostGIS and Oracle (SQL92 implementation)
* Labeling option (SLD) for grouping, priority, overlapping
* Useability changes & testing
* Now based on StreamingRenderer/GTRenderer interfaces

Based on the geotools 2.1.x branch (tag = for_geoserver_1_3_rc3)

GeoServer 1.3.0-RC2 README file
--------------------------------

The issue log for this release is here:
http://jira.codehaus.org/secure/IssueNavigator.jspa?reset=true&pid=10311&fixfor=11771

This release is based on the geotools 2.1.0 release (2.1.x svn branch).

1. JAI should no longer be required - this time I mean it.
      If you get an error regarding a security issue (because the jars a 'sealed')
      please leave a message on the geoserver-devel mailing list.  This will only
      occur if you installed JAI.  You can do one of the following:
          a) remove from the lib/ directory "jai-core-1.1.3-alpha.jar",
              "jai_imageio-1.1-alpha.jar" and "jai_codec-1.1.3-alpha.jar".
          b) uninstall JAI
      I've had one report of this problem and I think I solved it.  I
      have been unable to reproduce the issue.
      
2. I believe I fixed the error-logging issue for tomcat users.  Please
   tell me if the problem still occurs.
   
3. You can now use <Function> elements in your WFS <Filter>

4. KML (google map) output supported (james macgill)

5. Several other bugs fixed in geoserver and geotools


If you look at the roadmap for the next few releases 
(http://jira.codehaus.org/browse/GEOS?report=com.atlassian.jira.plugin.system.project:roadmap-panel)
you should see we are very rapidly approaching whats required for the 1.3.0 release.

The geoserver-devel mailing list has been "bursting at the seams" recently with 
good ideas and discussions for whats going to be happening over the longer term.

GeoServer 1.3.0-RC1 README file
--------------------------------
0.  No longer relies on JAI!

1.  Moved to Geotools 2.1.x branch
2.  GetCapabilities (WFS and WMS) better reflect the added functionality + other fixes
3.  Better SLD support and error reporting
4.  Better handling of CRS xformations
5.  Support for Min/MaxScaleDenominator in SLD
6.  Better type system for <Filter>
7.  Documentation updates
8.  FeatureReader/FeatureCollection merge
9.  New PNG writer (Jai-independent)
10. GIF writer improvements
11. Transparency support improvements
12. non-JAI JPEG writer
13. Memory leak fixed
14. WMS speed improvements
15. Data/Time problem fixed
16. Other bug fixes, improvements, and testing


GeoServer 1.3.0-beta4 README file
--------------------------------

Geoserver WFS is in excellent shape, and the WMS has improved
significantly.

* (WMS) Better label rendering
* (WMS) Better renderer performance
* (WMS) Better reprojection support
* (WMS) SLD-POST
* (WMS) SLD-InlineFeatures
* (WMS) GetMap POST
* (WMS) SLD-POST/GetMap POST schema validation
* (WMS) Parser error checking improvements

* (FILTER) added autogeneration of custom <Function name="..">
* (FILTER) parser improvements
* (FILTER) smarter/faster processing

* (PostGIS) minor bug fixes
* (PostGIS) allow use of VIEWs as FeatureTypes

* (CONFIG) GEOSERVER_DATA_DIR improvements
* (WAR)    WAR generation improvements


Plus a large set of bug fixes and testing.

NOTE: This release is based on the "for_Geoserver-1.3.0beta4" tag.

GeoServer 1.3.0-beta3 README file
--------------------------------

Major Changes
-------------
1. Geoserver now passes all WFS and WMS CITE tests
2. Geoserver WMS now does reprojection
     You can use "&SRS=NONE" if you dont want to do any reprojection.
3. Geoserver now allows you to define your geoserver data directory
    (previously called "GEOSERVER_HOME") - see below
4. Added a bunch of "helpers" for SRS (spatial referencing system) since people will have to define their SRSs now
5. Full SLD schema validation support



Minor Changes
-------------
1. bug fixes
2. set of lite renderer improvements
3. added a WFS lock tutorial (http://geoserver.org/display/GEOS/Feature+Locking)
4. upgraded to latest geotools


You can define your geoserver data directory in three ways:

1."GEOSERVER_DATA_DIR" system property.
   this will most likely have come from "java -DGEOSERVER_DATA_DIR=..."
or from you web container's GUI
2. "GEOSERVER_DATA_DIR" in the web.xml document:
     <context-param>
            <param-name>GEOSERVER_DATA_DIR</param-name>
            <param-value>c:\myGeoserverData</param-value>
     </context-param>
3. Defaults to the old behavior - ie. the application root - usually "server/geoserver" in your .WAR.




To make a new one of these data directories, just:

1. create the data directory
2. copy "data/" from an already running geoserver
3. create a "WEB-INF/" directory
5. copy "catalog.xml" and "services.xml" into the WEB-INF/ directory


GeoServer 1.3.0-beta2 README file
--------------------------------

Welcome to GeoServer 1.3.0-Beta2!  This release contains a number of bug fixes 
and improvements to the 1.3.0-beta release.

Quick to do list for the 1.3.0-beta2 release:

1.  (done) Gabriel's code re-organization
2.  (done) Solve some install issues
3.  (done) Fixing some obvious bugs
4.  (done) Finish CITE WFS testing & fixing
5.  (done) PostGIS 1.0 WKB changes
6.  (done) PostGIS column escaping for PK
7.  (done) Bounds for tables w/o geometries.  
8.  (done) NameSpace problem udigers reported
9.  (done) change how the packaging/installation is done
10. (done) work with UDIGer to get it working with geoserver better
11. (done) minor changes to how the load/save configuration works (bug fixes)

release process/content changes
-------------------------------
1. (done) changed release compression and content.
       binary release is tar.gz  -- for unix people (and winzip/winrar handles it)
       .exe for windows
       .war for containers
       .zip for documentation-only
       
      {thanks to paul ramsey for pointing out .zip and permissions in unix}
      
    NOTE: there were a lot of changes here, please ensure everything is in your release!
    
    
2. (done) removed most .jars from lib/ in the .exe 
3. (done) added in the "binary installer"
4. (done) added documentation to .exe and binary releases
5. (done) documentation changes (index.htmls and such)
6. (done) removed a bunch of stuff from the userbasic config's demo (I deleted these earlier, but someone re-commited them)
7. (done) tried the mapbuilder stuff and added a link to it on the "welcome page" so others can try it out
8. (done) problems with compiling .jsps 


  
We hope it will make it much easier for new developers to 
get acquainted and involved with the GeoServer Project.  The full 
changelog can be viewed at:
http://jira.codehaus.org/secure/BrowseProject.jspa?id=10311&report=changelog


Supporting GeoServer
--------------------
If you like GeoServer and are using it, we ask you to kindly add yourself to 
our user map at http://www.moximedia.com:8080/imf-ows/imf.jsp?site=gs_users  
Doing so helps ensure our continued funding, as our primary funder wants to 
know that people are actually using the project.  The map also serves as a 
demonstration of GeoServer as it is built using GeoServer for the queries and 
MapServer to display.

Documentation
-------------
Documentation is available online at 
http://geoserver.sourceforge.net/documentation/index.html
The documentation is also available for download from sourceforge, and can
be built from the source downloading using the 'ant document' command.

Additional  wiki-based documentation is available at:
http://geoserver.org/display/GEOS/Welcome

Additional Geotools documentation is available at:
http://www.geotools.org/

Commercial Support
------------------
For users who need support faster than the email lists and forums can provide
Refractions Research offers commercial grade support.  They can assist with
installations, customizations, and day to day operations.  If GeoServer is 
lacking certain features compared to commercial WFS's we recommend contacting
Refractions as the money required to implement the needed features may 
be less than the license for proprietary software.  For more 
information see http://refractions.net/geoserver/support.html

Support is also available by axios (http://axios.es/index_en.html) the 
contact person is gabriel (groldan@axios.es). 

Bugs
----------
We've tested the release extensively, but we can not possibly test all possible
servlet container, operating system, and data store combinations.  So please 
make use of our JIRA task tracker at: 
http://jira.codehaus.org/secure/BrowseProject.jspa?id=10311 or email 
geoserver-devel@lists.sourceforge.net or geoserver-user@lists.sourceforge.net.  
It's the only way we can know to fix them.  And
if you can fix the bugs yourself even better, as the source is open and we're
always more than happy to take patches.  



