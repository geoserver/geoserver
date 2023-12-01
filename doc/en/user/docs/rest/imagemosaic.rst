.. _rest_imagemosaic:



Uploading a new image mosaic
----------------------------

**Upload a ZIP file containing a mosaic definition and granule(s)**

*Request*

.. admonition:: curl

   ::

       curl -u admin:geoserver -XPUT -H "Content-type:application/zip" --data-binary @polyphemus.zip
          http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus/file.imagemosaic

*Response*

::

   200 OK

Updating an image mosaic contents
---------------------------------

**Harvest (or reharvest) a single file into the mosaic and update the mosaic index**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/file/polyphemus_20130302.nc" 
          "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/poly-incremental/external.imagemosaic"

*Response*

::

   201 Created

**Harvest (or reharvest) a whole directory into the mosaic and update the mosaic index**

*Request*

.. admonition:: curl

   ::

        curl -v -u admin:geoserver -XPOST -H "Content-type: text/plain" -d "file:///path/to/the/mosaic/folder" 
           "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/poly-incremental/external.imagemosaic"

*Response*

::

   201 Created

Listing image mosaic details
----------------------------

**Retrieve the image mosaic index structure**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index.xml"

*Response*

.. code-block:: xml

       <Schema>
      <attributes>
        <Attribute>
          <name>the_geom</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>org.locationtech.jts.geom.Polygon</binding>
        </Attribute>
        <Attribute>
          <name>location</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.lang.String</binding>
        </Attribute>
        <Attribute>
          <name>imageindex</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.lang.Integer</binding>
        </Attribute>
        <Attribute>
          <name>time</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.sql.Timestamp</binding>
        </Attribute>
        <Attribute>
          <name>elevation</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.lang.Double</binding>
        </Attribute>
        <Attribute>
          <name>fileDate</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.sql.Timestamp</binding>
        </Attribute>
        <Attribute>
          <name>updated</name>
          <minOccurs>0</minOccurs>
          <maxOccurs>1</maxOccurs>
          <nillable>true</nillable>
          <binding>java.sql.Timestamp</binding>
        </Attribute>
      </attributes>
      <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml" type="application/xml"/>
    </Schema>

**Retrieve the existing granule information**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml?limit=2"

*Response*

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <wfs:FeatureCollection xmlns:gf="http://www.geoserver.org/rest/granules" xmlns:ogc="http://www.opengis.net/ogc" xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml">
      <gml:boundedBy>
        <gml:Box srsName="http://www.opengis.net/gml/srs/epsg.xml#4326">
          <gml:coord>
            <gml:X>5.0</gml:X>
            <gml:Y>45.0</gml:Y>
          </gml:coord>
          <gml:coord>
            <gml:X>14.875</gml:X>
            <gml:Y>50.9375</gml:Y>
          </gml:coord>
        </gml:Box>
      </gml:boundedBy>
      <gml:featureMember>
        <gf:NO2 fid="NO2.1">
          <gf:the_geom>
            <gml:Polygon>
              <gml:outerBoundaryIs>
                <gml:LinearRing>
                  <gml:coordinates>5.0,45.0 5.0,50.9375 14.875,50.9375 14.875,45.0 5.0,45.0</gml:coordinates>
                </gml:LinearRing>
              </gml:outerBoundaryIs>
            </gml:Polygon>
          </gf:the_geom>
          <gf:location>polyphemus_20130301.nc</gf:location>
          <gf:imageindex>336</gf:imageindex>
          <gf:time>2013-03-01T00:00:00Z</gf:time>
          <gf:elevation>10.0</gf:elevation>
          <gf:fileDate>2013-03-01T00:00:00Z</gf:fileDate>
          <gf:updated>2013-04-11T10:54:31Z</gf:updated>
        </gf:NO2>
      </gml:featureMember>
      <gml:featureMember>
        <gf:NO2 fid="NO2.2">
          <gf:the_geom>
            <gml:Polygon>
              <gml:outerBoundaryIs>
                <gml:LinearRing>
                  <gml:coordinates>5.0,45.0 5.0,50.9375 14.875,50.9375 14.875,45.0 5.0,45.0</gml:coordinates>
                </gml:LinearRing>
              </gml:outerBoundaryIs>
            </gml:Polygon>
          </gf:the_geom>
          <gf:location>polyphemus_20130301.nc</gf:location>
          <gf:imageindex>337</gf:imageindex>
          <gf:time>2013-03-01T00:00:00Z</gf:time>
          <gf:elevation>35.0</gf:elevation>
          <gf:fileDate>2013-03-01T00:00:00Z</gf:fileDate>
          <gf:updated>2013-04-11T10:54:31Z</gf:updated>
        </gf:NO2>
      </gml:featureMember>
    </wfs:FeatureCollection>


Removing image mosaic granules
------------------------------

**Remove all the granules originating from a particular file**

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XDELETE "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/polyphemus-v1/coverages/NO2/index/granules.xml?filter=location='polyphemus_20130301.nc'"
   
*Response*

::

   200 OK


Uploading an empty mosaic
-------------------------

**Upload an archive with the definition of an mosaic, but with no granules**

Given a :download:`empty.zip <artifacts/empty.zip>` file containing:

* ``datastore.properties`` (PostGIS connection parameters)
* ``indexer.xml`` (Mosaic indexer; note the ``CanBeEmpty=true`` parameter)
* ``polyphemus-test.xml`` (Auxiliary file used by the NetCDF reader to parse schemas and tables)

.. warning:: Make sure to update the ``datastore.properties`` file with your connection parameters and refresh the ZIP before uploading it. 

*Request*

.. admonition:: curl

   ::

       curl -u admin:geoserver -XPUT -H "Content-type:application/zip" --data-binary @empty.zip
          http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/file.imagemosaic?configure=none

.. note:: The ``configure=none`` parameter allows for future configuration after harvesting.

*Response*

::

  200 OK

**Configure a coverage on the mosaic**


Given a ``coverageconfig.xml``:

.. code-block:: xml

    <coverage>
      <nativeCoverageName>NO2</nativeCoverageName>
      <name>NO2</name>
    </coverage>

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d @"/path/to/coverageconfig.xml" "http://localhost:8080/geoserver/rest/workspaces/topp/coveragestores/empty/coverages"

.. note:: When specifying only the coverage name, the coverage will be automatically configured.

*Response*

::

  201 Created