.. _rest_styles:

Styles
======

The REST API allows you to list, create, upload, update, and delete styles in GeoServer.

.. note:: Read the :api:`API reference for /styles <styles.yaml>`.

Listing all styles
------------------

**List all styles on the server, in JSON format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/styles.json

*Response*

.. code-block:: json

   {"styles":{"style":[{"name":"burg","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/burg.json"},{"name":"capitals","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/capitals.json"},{"name":"dem","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/dem.json"},{"name":"generic","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/generic.json"},{"name":"giant_polygon","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/giant_polygon.json"},{"name":"grass","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/grass.json"},{"name":"green","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/green.json"},{"name":"line","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/line.json"},{"name":"poi","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/poi.json"},{"name":"point","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/point.json"},{"name":"polygon","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/polygon.json"},{"name":"poly_landmarks","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/poly_landmarks.json"},{"name":"pophatch","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/pophatch.json"},{"name":"population","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/population.json"},{"name":"rain","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/rain.json"},{"name":"raster","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/raster.json"},{"name":"restricted","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/restricted.json"},{"name":"simple_roads","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/simple_roads.json"},{"name":"simple_streams","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/simple_streams.json"},{"name":"tiger_roads","href":"http:\/\/localhost:8080\/geoserver\/rest\/styles\/tiger_roads.json"}]}}


**List all styles in a workspace, in XML format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/cite/styles.xml

*Response*

.. code-block:: xml

   <styles>
     <style>
       <name>citerain</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/workspaces/cite/styles/citerain.xml" type="application/xml"/>
     </style>
   </styles>


Retrieve a style
----------------

**Download the actual style code for a style:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/styles/rain.sld

*Response*

.. code-block:: xml

        <StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
          xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
          <NamedLayer>
            <Name>rain</Name>
            <UserStyle>
              <Name>rain</Name>
              <Title>Rain distribution</Title>
              <FeatureTypeStyle>
                <Rule>
                  <RasterSymbolizer>
                    <Opacity>1.0</Opacity>
                    <ColorMap>
                      <ColorMapEntry color="#FF0000" quantity="0" />
                      <ColorMapEntry color="#FFFFFF" quantity="100"/>
                      <ColorMapEntry color="#00FF00" quantity="2000"/>
                      <ColorMapEntry color="#0000FF" quantity="5000"/>
                    </ColorMap>
                  </RasterSymbolizer>
                </Rule>
              </FeatureTypeStyle>
            </UserStyle>
          </NamedLayer>
        </StyledLayerDescriptor>


Creating a style
----------------

You can create a new style on the server in two ways. In the first way, the creation is done in two steps: the style entry is created in the catalog, and then the style content is uploaded. The second way can add the style to the server in a single step by uploading a ZIP containing the style content:

**Create a new style in two steps:**

*Request*

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XPOST -H "Content-type: text/xml" -d "<style><name>roads_style</name><filename>roads.sld</filename></style>" http://localhost:8080/geoserver/rest/styles

*Response*

::

   201 Created

*Request*

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XPUT -H "Content-type: application/vnd.ogc.sld+xml" -d @roads.sld http://localhost:8080/geoserver/rest/styles/roads_style

*Response*

::

   200 OK

**Create a new style in a single step:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPOST -H "Content-type: application/zip" --data-binary @roads_style.zip http://localhost:8080/geoserver/rest/styles

*Response*

::

   201 Created

   
This example will create a new style on the server and populate it the contents of a local SLD file and related images provided in a SLD package. 
A SLD package is a zip file containing the SLD style and related image files used in the SLD.

The following creates a new style named ``roads_style``.

Each code block below contains a single command that may be extended over multiple lines.

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPOST -H "Content-type: application/zip"
     --data-binary @roads_style.zip
     http://localhost:8080/geoserver/rest/styles

*Response*

::

   201 OK
   
The SLD itself can be downloaded through a a GET request: 

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XGET
     http://localhost:8080/geoserver/rest/styles/roads_style.sld  
   
Changing an existing style
--------------------------

**Edit/reupload the content of an existing style on the server:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPUT -H "Content-type: application/vnd.ogc.sld+xml" -d @roads.sld 
     http://localhost:8080/geoserver/rest/styles/roads_style

*Response*

::

   200 OK

**Edit/reupload the content of an existing style on the server when the style is in a workspace:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPUT -H "Content-type: application/vnd.ogc.sld+xml" -d @roads.sld 
     http://localhost:8080/geoserver/rest/workspaces/cite/styles/roads_style

*Response*

::

   200 OK

**Edit/reupload the content of an existing style on the server using a ZIP file containing a shapefile:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XPUT -H "Content-type: application/zip" --data-binary @roads_style.zip http://localhost:8080/geoserver/rest/styles/roads_style.zip

*Response*

::

   200 OK
   


Deleting a style
----------------

**Remove a style entry from the server, retaining the orphaned style content:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XDELETE http://localhost:8080/geoserver/rest/styles/zoning

*Response*

::

   200 OK

**Remove a style entry from the server, deleting the orphaned style content:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XDELETE http://localhost:8080/geoserver/rest/styles/zoning?purge=true

*Response*

::

   200 OK

