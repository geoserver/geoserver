.. _services_webadmin_wfs:

WFS settings
============

This page details the configuration options for WFS in the web administration interface.

Workspace
---------

Select :guilabel:`workspace` empty to configure global WFS settings.

See the section on :ref:`workspace_services` to override settings used by WFS :ref:`virtual_services`.

Service Metadata
----------------

For a description of WFS service options, see the section on :ref:`service_metadata`.

Features
--------

.. figure:: img/services_WFS_features.png
   
   WFS configuration options - Features section

The `Open Geospatial Consortium <http://www.opengeospatial.org/>`_ (OGC) Web Feature Service (WFS) is a protocol for serving geographic features across the Web. Feature information that is encoded and transported using WFS includes both feature geometry and feature attribute values. Basic Web Feature Service (WFS) supports feature query and retrieval. Feature limits and bounding can be configured on the WFS page. 

**Maximum number of features** — Maximum number of features that a WFS GetFeature operation should generate, regardless of the actual number of query hits. A WFS request can potentially contain a large dataset that is impractical to download to a client, and/or too large for a client's renderer. Maximum feature limits are also available for feature types. The default number is 1000000.

**Maximum number of features for preview (Values <= 0 use the maximum number of features)** - Maximum number of features to use for layer previews. The default is 50 features.

**Return bounding box with every feature** — When creating the GetFeature GML output, adds an auto-calculated bounds element on each feature type. Not typically enabled, as including bounding box takes up extra bandwidth. 

**Ignore maximum number of features when calculating hits** - When calculating the total number of hits, ignore the Maximum number of features setting. This can be used to get the count of matching features, even if they would not be made available for download because they exceed the maximum count specified. On very large data sets, this can slow down the response.

**Activate complex to simple features conversion** - Enables the conversion of complex features to simple features, using only SF-0 (simple) attributes for compatible output formats like CSV, KML, SHAPE-ZIP.

Service Levels
--------------

.. figure:: img/services_WFS_servicelevel.png
   
   WFS configuration options - Service Level section

GeoServer is compliant with the full "Transactional Web Feature Server" (WFS-T) level of service as defined by the OGC. Specifying the WFS service level limits the capabilities of GeoServer while still remaining compliant. The WFS Service Level setting defines what WFS operations are "turned on". 

**Basic** — Basic service levels provides facilities for searching and retrieving feature data with the GetCapabilities, DescribeFeatureType and GetFeature operations. It is compliant with the OGC basic Web Feature Service. This is considered a READ-ONLY web feature service. 

**Transactional** — In addition to all basic WFS operations, transactional service level supports transaction requests. A transaction request facilitates the creation, deletion, and updating of geographic features in conformance with the OGC Transactional Web Feature Service (WFS-T). 

**Complete** — Includes LockFeature support to the suite of transactional level operations. LockFeature operations help resolve links between related resources by processing lock requests on one or more instances of a feature type. 



Extra SRS codes for WFS capabilities generation
-----------------------------------------------

WFS 1.1.0 onwards adds the ability to reproject GetFeature output to a user specified target SRS. The list of applicable target SRS is defined on a feature type basis in the capabilities documents, and GeoServer allows reprojection to any supported SRS in its internal database. To declare that GeoServer would have to add 5000+ ``otherSRS/otherCRS`` elements per feature type, which is clearly impractical. As a result, no declaration is made by default.

A list of values to be declared in all feature types can be provided in the WFS administration panel, as a comma separated list of EPSG codes:

.. figure:: img/services_WFS_othersrs.png
   
   WFS otherSRS/otherCRS configuration

The list will be used only for the capabilities document generation. It does not limit the actual target SRS usage in GetFeature requests.


CSV
---
.. figure:: img/services_WFS_csv.png

   WFS configuration options - CSV section 

CSV is still widely used as a format to exchange tabular data. For GeoServer CSV output format, the date field format can specified using the Date Format text box as shown above.
Here are some common formatting patterns supported

.. list-table::
   :widths: 25 25
   :header-rows: 1

   * - Date and Time Pattern 
     - Result
   * - ``yyyy.MM.dd G 'at' HH:mm:ss z``
     - 2001.07.04 AD at 12:08:56 PDT  
   * - ``EEE, MMM d, ''yy``
     - Wed, Jul 4, '01
   * - ``yyyy-MM-dd'T'HH:mm:ss.SSSZ``
     - 2001-07-04T12:08:56.235-0700 
   * - ``yyyy-MM-dd'T'HH:mm:ss.SSSXXX``
     - 2001-07-04T12:08:56.235-07:00


Here, **yyyy-MM-dd'T'HH:mm:ss.SSS'Z'** pattern represents the year, month, day, hour, minute, second and milliseconds. The components are separated by a hyphen character. A literal 'T' character is used to separate the date and time parts. A literal 'Z' character represents the UTC time zone. For instance, yyyy represents the year with four digits, MM represents the month with leading zeros, dd represents the day with leading zeros, and so on.

Similarly, patterns can be formed by using the characters provided below

::

  y- Year (four digits) 
  yy- Year (two digits) 
  yyyy- Year (four digits) 
  M- Month (1 or 2 digits) 
  MM- Month (2 digits, with leading zero) 
  MMM- Month abbreviation (e.g., 'Jan', 'Feb') 
  MMMM- Full month name (e.g., 'January', 'February') 
  d- Day of the month (1 or 2 digits) 
  dd- Day of the month (2 digits, with leading zero) 
  E- Day of the week abbreviation (e.g., 'Mon', 'Tue') 
  EEEE- Full day of the week (e.g., 'Monday', 'Tuesday') 
  H- Hour in 24-hour format (0 to 23) 
  HH- Hour in 24-hour format (2 digits, with leading zero) 
  h- Hour in 12-hour format (1 to 12) 
  hh- Hour in 12-hour format (2 digits, with leading zero) 
  m- Minute (1 or 2 digits) 
  mm- Minute (2 digits, with leading zero) 
  s- Second (1 or 2 digits) 
  ss- Second (2 digits, with leading zero) 
  SSS- Represents the milliseconds in a three-digit format (e.g., 750)
  a- AM/PM marker 
  n- Nanosecond 
  Z- Time zone offset (e.g., '+0800') 
  zzzz- Time zone full name (e.g., 'Pacific Standard Time')

Reference SimpleDateFormat Link : https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/text/SimpleDateFormat.html


Configuration of Output Format types allowed in GetFeature
----------------------------------------------------------

Checking the ``Enable Output Format type checking`` checkbox will enable restrictions on the available output formats for GetFeature requests.  It will also limit what Output Format types will be displayed as available in GetCapabilities responses. GetFeature requests with Output types not included in the ``Allowed Output types`` panel will result in an ``Invalid Parameter ServiceException`` response. 

Note that if the Allowed Output Types panel is left as empty and the ``Enable Output Format Type checking`` is checked, all Output types will be restricted.

.. figure:: img/services_WFS_outputformat.png

   Output Format types configuration

GML
---

.. figure:: img/services_WFS_srsstyle.png
   
   WFS configuration options - GML sections

Geography Markup Language (GML) is the XML-based specification defined by the Open Geospatial Consortium (OGC) to express geographical features. GML serves as a modeling language for geographic systems as well as an open interchange format for geographic transactions on the Internet. 

The older GML standard, `GML 2 <http://portal.opengeospatial.org/files/?artifact_id=11339>`_ encodes geographic information, including both spatial and non-spatial properties. GML3 extends GML2 support to 3D shapes (surfaces and solids) as well as other advanced facilities. GML 3 is a modular superset of GML 2 that simplifies and minimizes the implementation size by allowing users to select out necessary parts. Additions in GML 3 include support for complex geometries, spatial and temporal reference systems, topology, units of measure, metadata, gridded data, and default styles for feature and coverage visualization. GML 3 is almost entirely backwards compatible with GML 2.

WFS 2.0.0 request return GML 3.2 as the default format, WFS 1.1.0 requests return GML 3 as the default format, and WFS 1.0.0 requests return GML 2 as the default format. For each of the GML formats supported by GeoServer, a different SRS format can be selected.

**EPSG Code** — Returns the typical EPSG number in the form ``EPSG:XXXX`` (e.g. ``EPSG:4326``). This formats the geographic coordinates in longitude/latitude (x/y) order. 

**OGC HTTP URL** — Returns a URL that identifies each EPSG code: ``http://www.opengis.net/gml/srs/epsg.xml#XXXX`` (e.g. ``http://www.opengis.net/gml/srs/epsg.xml#4326``). This formats the geographic coordinates in longitude/latitude (x/y) order.  This format is the default GML 2 SRS convention.

**OGC Experimental URN** - Returns a URN that identifies each EPSG code: ``urn:x-ogc:def:crs:EPSG:XXXX`` (e.g. ``urn:x-ogc:def:crs:EPSG:4326``). This format was the original GML 3 SRS convention.

**OGC URN** — (WFS 1.1.1 only) Returns the colon delimited SRS formatting: ``urn:ogc:def:crs:EPSG::XXXX`` (e.g ``urn:ogc:def:crs:EPSG::4326``). This is the revised GML 3 SRS convention, and is the default for GML 3.2. This formats data in the traditional axis order for geographic and cartographic systems—latitude/longitude (y/x).

**OGC HTTP URI** - Returns a URI that identifies each EPSG code: ``http://www.opengis.net/def/crs/EPSG/0/XXXX`` (e.g. ``http://www.opengis.net/def/crs/EPSG/0/4326``). 

For each GML type, there is also an "Override GML Attributes" checkbox. Selecting this (checking the checkbox) will cause attributes to be redefined in the application schema.

Override GML 3.2 MIME type
^^^^^^^^^^^^^^^^^^^^^^^^^^

The default MIME used for GML 3.2 encoded responses is ``application/gml+xml; version=3.2`` which is the MIME type mandated by OGC WFS 2.0 specification. This MIME type is not identified as XML by most common clients such as browsers. 

Option :guilabel:`Override MIME Type` allows the selection of the MIME type that should be used for the responses encoded in GML 3.2.  

.. figure:: img/services_WFS_mimetype.png

The available MIME types are: ``application/gml+xml; version=3.2``, ``text/xml; subtype=gml/3.2`` and ``text/xml``. 

Configure XML Entity Expansion limit on WFS XML readers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default, the WFS XML reader sets Entity Expansion limit to ``100``, but it can be configured via the ``org.geoserver.wfs.xml.entityExpansionLimit`` system property, or using the :file:`web.xml` init parameter, or by Environment variable.

For example, using the command line the limit can be adjusted using a parameter:

    -Dorg.geoserver.wfs.xml.entityExpansionLimit=50

Or in Tomcat properties file (``{TOMCAT_HOME}/conf/catalina.properties``) adding the line:

    org.geoserver.wfs.xml.entityExpansionLimit=50

Conformance
-----------

.. figure:: img/services_WFS_conformance.png
   
   WFS configuration options - Conformance section

Selecting the :guilabel:`Encode canonical WFS schema location` checkbox modifies the WFS responses to include the canonical schema locations in the ``xsi:schemaLocation`` attribute, instead of
using the default schema locations on the local GeoServer. Note that turning this option on may result in the client not being able to validate the WFS response, depending on network
configuration.

Encode response with
--------------------

.. figure:: img/services_WFS_featureMembers.png
   
   WFS configuration options - Encode response with
   
The :guilabel:`Encode response with` radio button group has two selection - :guilabel:`One "featureMembers" element` (the default) or :guilabel:`Multiple "featureMember" elements`. This switches the WFS 1.1.0 encoding accordingly. Use of multiple ``featureMember`` elements may be required for Application Schema referencing.

SHAPE-ZIP output format 
-----------------------

.. figure:: img/services_WFS_shapezipprojection.png
   
   WFS configuration options - Encode response with
   
Selecting the :guilabel:`Use ESRI WKT format for SHAPE-ZIP generated .prj files` checkbox modifies how projections are encoded in the Shapefile zip output format. If this checkbox
is not selected, OGC WKT format will be used. If this checkbox is selected, ESRI WKT format will be used.

Note: this requires an ``esri.properties`` file to be provided in the ``user_projections`` subdirectory of the GeoServer data directory. This may be obtained from the GeoTools EPSG extension.

Selecting the :guilabel:`Include WFS request dump file` checkbox specifies if the file 'wfsrequest.txt' will be included in the Shapefile zip output. 'wfsrequest.txt' contains a dump of the full request URL used to get the Shapefile zip output.  If this checkbox is not selected, 'wfsrequest.txt' will not be included in the output. If this checkbox is selected, 'wfsrequest.txt' will be included in the output.

Stored Queries
--------------

.. figure:: img/global-queries.png

Selecting the :guilabel:`Allow Global Stored Queries` checkbox determines if global stored queries will included for usage in workspace virtual services, or not. When disabled, only stored queries created inside the workspace will be visible.

.. figure:: img/disable-stored-queries-management.png

Selecting the :guilabel:`Disable stored queries management` checkbox will disable stored queries write operations from being present in GetCapabilities response. Attempting to triggering one of operations related to stored queries will result in exception.

i18n Settings
-------------

Select default language for WFS Service.

.. figure:: img/i18n_default_language.png
   
   Default language

See :ref:`internationalization` section for a how this setting is used.
