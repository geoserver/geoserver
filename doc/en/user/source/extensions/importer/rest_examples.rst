.. _importer_rest_examples:

Importer REST API examples
==========================

Mass configuring a directory of shapefiles
------------------------------------------

In order to initiate an import of the ``c:\data\tasmania`` directory into the existing ``tasmania`` workspace:

1. The following JSON will be POSTed to GeoServer.
   
   .. code-block:: json
      :caption: import.json
      
        {
           "import": {
              "targetWorkspace": {
                 "workspace": {
                    "name": "tasmania"
                 }
              },
              "data": {
                "type": "directory",
                "location": "C:/data/tasmania"
              }
           }
        }

2. This curl command can be used for the purpose:

  .. code-block:: bash
  
     curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
       -d @import.json \
       "http://localhost:8080/geoserver/rest/imports"

  The importer will locate the files to be imported, and automatically prepare the tasks, returning the following response:

   .. code-block:: json

       {
         "import": {
           "id": 9,
           "href": "http://localhost:8080/geoserver/rest/imports/9",
           "state": "PENDING",
           "archive": false,
           "targetWorkspace": {
             "workspace": {
               "name": "tasmania"
             }
           },
           "data": {
             "type": "directory",
             "format": "Shapefile",
             "location": "C:\\data\\tasmania",
             "href": "http://localhost:8080/geoserver/rest/imports/9/data"
           },
           "tasks": [
             {
               "id": 0,
               "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/0",
               "state": "READY"
             },
             {
               "id": 1,
               "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/1",
               "state": "READY"
             },
             {
               "id": 2,
               "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/2",
               "state": "READY"
             },
             {
               "id": 3,
               "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/3",
               "state": "READY"
             }
           ]
         }
       }

3. After checking every task is ready, the import can be initiated by executing a POST on the import resource:
   
   .. code-block:: bash

      curl -u admin:geoserver -XPOST \
         "http://localhost:8080/geoserver/rest/imports/9"
  
4. The resource can then be monitored for progress, and eventually final results:

   .. code-block:: bash  
   
      curl -u admin:geoserver -XGET \
         "http://localhost:8080/geoserver/rest/imports/9"

   Which in case of successful import will look like:

   .. code-block:: json

      {
        "import": {
          "id": 9,
          "href": "http://localhost:8080/geoserver/rest/imports/9",
          "state": "COMPLETE",
          "archive": false,
          "targetWorkspace": {
            "workspace": {
              "name": "tasmania"
            }
          },
          "data": {
            "type": "directory",
            "format": "Shapefile",
            "location": "C:\\data\\tasmania",
            "href": "http://localhost:8080/geoserver/rest/imports/9/data"
          },
          "tasks": [
            {
              "id": 0,
              "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/0",
              "state": "COMPLETE"
            },
            {
              "id": 1,
              "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/1",
              "state": "COMPLETE"
            },
            {
              "id": 2,
              "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/2",
              "state": "COMPLETE"
            },
            {
              "id": 3,
              "href": "http://localhost:8080/geoserver/rest/imports/9/tasks/3",
              "state": "COMPLETE"
            }
          ]
        }
      } 
	
Configuring a shapefile with no projection information
------------------------------------------------------

In this case, let's assume we have a single shapefile, :file:`tasmania_cities.shp``, that does not have the :file:`.prj` sidecar file 
(the example is equally good for any case where the :file:`prj` file contents cannot be matched to an official EPSG code).

1. We are going to post the following import definition:
   
   .. code-block:: json
      :caption: import.json

      {
         "import": {
            "targetWorkspace": {
               "workspace": {
                  "name": "tasmania"
               }
            },
            "data": {
              "type": "file",
              "file": "C:/data/tasmania/tasmania_cities.shp"
            }
         }
      }

2. With the cURL POST command:
   
   .. code-block:: bash

      curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
         -d @import.json \
         "http://localhost:8080/geoserver/rest/imports"

   The response in case the CRS is missing will be:

   .. code-block:: json

      {
        "import": {
          "id": 13,
          "href": "http://localhost:8080/geoserver/rest/imports/13",
          "state": "PENDING",
          "archive": false,
          "targetWorkspace": {
            "workspace": {
              "name": "tasmania"
            }
          },
          "data": {
            "type": "file",
            "format": "Shapefile",
            "file": "tasmania_cities.shp"
          },
          "tasks": [
            {
              "id": 0,
              "href": "http://localhost:8080/geoserver/rest/imports/13/tasks/0",
              "state": "NO_CRS"
            }
          ]
        }
      }

3. Drilling into the task layer:

   .. code-block:: bash
      
      curl -u admin:geoserver -XGET -H "Content-type: application/json" \
           http://localhost:8080/geoserver/rest/imports/13/tasks/0/layer
   
   We can see the srs information is missing:
   
   .. code-block:: json

      {
        "layer": {
          "name": "tasmania_cities",
          "href": "http://localhost:8080/geoserver/rest/imports/13/tasks/0/layer",
          "title": "tasmania_cities",
          "originalName": "tasmania_cities",
          "nativeName": "tasmania_cities",
          "bbox": {
            "minx": 146.2910004483,
            "miny": -43.85100181689,
            "maxx": 148.2910004483,
            "maxy": -41.85100181689
          },
          "attributes": [
            {
              "name": "the_geom",
              "binding": "org.locationtech.jts.geom.MultiPoint"
            },
            {
              "name": "CITY_NAME",
              "binding": "java.lang.String"
            },
            {
              "name": "ADMIN_NAME",
              "binding": "java.lang.String"
            },
            {
              "name": "CNTRY_NAME",
              "binding": "java.lang.String"
            },
            {
              "name": "STATUS",
              "binding": "java.lang.String"
            },
            {
              "name": "POP_CLASS",
              "binding": "java.lang.String"
            }
          ],
          "style": {
            "name": "tasmania_tasmania_cities2",
            "href": "http://localhost:8080/geoserver/rest/imports/13/tasks/0/layer/style"
          }
        }
      }

4. Use the following json snippet to update the SRS:

   .. code-block:: bash
      :caption: layerUpdate.json
      
       {
          layer : {
             srs: "EPSG:4326"
          }
       }  
   
   Using cURL PUT command:
   
   .. code-block:: bash

      curl -u admin:geoserver -XPUT -H "Content-type: application/json" \
        -d @layerUpdate.json \
        "http://localhost:8080/geoserver/rest/imports/13/tasks/0/layer/"


5. Getting the import definition again:

   .. code-block:: bash
   
      curl -u admin:geoserver -XGET -H "Content-type: application/json" \
           http://localhost:8080/geoserver/rest/imports/13/tasks/0
   
   The import is now ready to execute:

   .. code-block:: json
   
      {
        "import": {
          "id": 13,
          "href": "http://localhost:8080/geoserver/rest/imports/13",
          "state": "PENDING",
          "archive": false,
          "targetWorkspace": {
            "workspace": {
              "name": "tasmania"
            }
          },
          "data": {
            "type": "file",
            "format": "Shapefile",
            "file": "tasmania_cities.shp"
          },
          "tasks": [
            {
              "id": 0,
              "href": "http://localhost:8080/geoserver/rest/imports/13/tasks/0",
              "state": "READY"
            }
          ]
        }
      }

6. A POST request will execute the import:

   .. code-block:: bash

      curl -u admin:geoserver -XPOST \
        "http://localhost:8080/geoserver/rest/imports/13"

   With a successful import marking the task as ``COMPLETE``:

   .. code-block:: json
   
      {
        "import": {
          "id": 13,
          "href": "http://localhost:8080/geoserver/rest/imports/13",
          "state": "COMPLETE",
          "archive": false,
          "targetWorkspace": {
            "workspace": {
              "name": "tasmania"
            }
          },
          "data": {
            "type": "file",
            "format": "Shapefile",
            "file": "tasmania_cities.shp"
          },
          "tasks": [
            {
              "id": 0,
              "href": "http://localhost:8080/geoserver/rest/imports/13/tasks/0",
              "state": "COMPLETE"
            }
          ]
        }
      }

Uploading a Shapefile to PostGIS
--------------------------------

This example shows the process for uploading a Shapefile (in a zip file) to an existing PostGIS datastore (cite:postgis).

1. Setup ``cite:postgis`` datastore:
   
   .. literalinclude:: files/postgis.json
      :language: json
      :caption:  postgis.json
   
   Using curl POST:
   
   .. code-block:: bash
   
      curl  -u admin:geoserver -XPOST -H "Content-type: application/json" \
        -d @postgis.json \
        "http://localhost:8080/geoserver/rest/workspaces/cite/datastores.json"

2. Create the import definition:

   .. literalinclude:: files/import.json
      :language: json
      :caption:  import.json

   POST this definition to /geoserver/rest/imports:
   
   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
        -d @import.json \
        "http://localhost:8080/geoserver/rest/imports"

   The response will contain the import ID.

3. We now have an empty import with no tasks. To add a task, POST the shapefile to the list of tasks:

   .. code-block:: bash   

      curl -u admin:geoserver \
        -F name=myshapefile.zip -F filedata=@myshapefile.zip \
        "http://localhost:8080/geoserver/rest/imports/14/tasks"

4. Since we sent a shapefile, importer assumes the target will be a shapefile store. To import to PostGIS, we will need to reset it.
   
   Create the following JSON file:
   
   .. code-block:: json
      :caption: target.json

      {
        "dataStore": {
          "name":"postgis"
        }
      }

   PUT this file to /geoserver/rest/imports/14/tasks/0/target:

   .. code-block:: bash  
      
      curl -u admin:geoserver -XPUT -H "Content-type: application/json" \
        -d @target.json \
        "http://localhost:8080/geoserver/rest/imports/14/tasks/0/target"

5. Finally, we execute the import by sending a POST to /geoserver/rest/imports/14:

   .. code-block:: bash  
   
      curl -u admin:geoserver -XPOST \
        "http://localhost:8080/geoserver/rest/imports/14"
	
Uploading a CSV file to PostGIS while transforming it
-----------------------------------------------------

A remote sensing tool is generating CSV files with some locations and measurements, that we want to upload
into PostGIS as a new spatial table.

1. First, we are going to create a empty import with an existing postgis store as the target:

   .. code-block:: bash

      curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
        -d @import.json \
        "http://localhost:8080/geoserver/rest/imports"
   
   Where :file:`import.json` is:

   .. literalinclude:: files/import.json
      :language: json
      :caption:  import.json

2. Then, we are going to POST the csv file to the tasks list.

   .. literalinclude:: files/values.csv
      :language: text
      :caption:  values.csv

   In order to create an import task for it:

   .. code-block:: bash
   
      curl -u admin:geoserver -F name=test -F filedata=@values.csv \
        "http://localhost:8080/geoserver/rest/imports/0/tasks"

   And we are going to get back a new task definition, with a notification that the CRS is missing:

   .. code-block:: json

      {
        "task": {
          "id": 0,
          "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0",
          "state": "NO_CRS",
          "updateMode": "CREATE",
          "data": {
            "type": "file",
            "format": "CSV",
            "file": "values.csv"
          },
          "target": {
            "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/target",
            "dataStore": {
              "name": "postgis",
              "type": "PostGIS"
            }
          },
          "progress": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/progress",
          "layer": {
            "name": "values",
            "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer"
          },
          "transformChain": {
            "type": "vector",
            "transforms": []
          }
        }
      }

3. Force the CRS by updating the layer:

   .. literalinclude:: files/layerUpdate.json
      :language: json
      :caption:  layerUpdate.json
   
   Using PUT to update task layer:
   
   .. code-block:: bash

      curl -u admin:geoserver -XPUT -H "Content-type: application/json" \
        -d @layerUpdate.json \
        "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer/"
   
   Updating the srs:
   
   .. code-block:: json
   
      {
        "layer": {
          "name": "values",
          "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer",
          "title": "values",
          "originalName": "values",
          "nativeName": "values",
          "srs": "EPSG:4326",
          "bbox": {
            "minx": 0,
            "miny": 0,
            "maxx": -1,
            "maxy": -1
          },
          "attributes": [
            {
              "name": "AssetID",
              "binding": "java.lang.Integer"
            },
            {
              "name": "SampleTime",
              "binding": "java.lang.String"
            },
            {
              "name": "Lat",
              "binding": "java.lang.Double"
            },
            {
              "name": "Lon",
              "binding": "java.lang.Double"
            },
            {
              "name": "Value",
              "binding": "java.lang.Double"
            }
          ],
          "style": {
            "name": "point",
            "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer/style"
          }
        }
      }

4. Then, we are going to create a transformation mapping the Lat/Lon columns to a point:

   .. literalinclude:: files/toPoint.json
      :language: json
      :caption:  toPoint.json
         
   The above will be uploaded task transforms:
   
   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
        -d @toPoint.json \
        "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"

5. Now the import is ready to run, and we'll execute it using:

   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST \
        "http://localhost:8080/geoserver/rest/imports/0"

6. The new layer is created in PostGIS and registered in GeoServer as a new layer.

   In case the features in the CSV need to be appended to an existing layer a PUT request against the task might be performed, changing its
   updateMode from "CREATE" to "APPEND". Changing it to "REPLACE" instead will preserve the layer, but remove the old contents and replace
   them with the newly uploaded ones.

Replacing PostGIS table using the contents of a CSV file
--------------------------------------------------------

To update the ``values`` layer with new content:
      
#. Create a new import into ``cite:postgis``:

   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
        -d @import.json "http://localhost:8080/geoserver/rest/imports"
        
   Using:
   
   .. literalinclude:: files/import.json
      :language: json
      :caption:  import.json
        
#. Use :file:`replace.csv` to create a new task:

   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST \
        -F filedata=@replace.csv \
        "http://localhost:8080/geoserver/rest/imports/1/tasks"
        
   The csv file has an additional column:
        
   .. literalinclude:: files/replace.csv
      :language: text
      :caption:  replace.csv

#. Update task with as a "REPLACE" and supply srs information:

   .. code-block:: bash
   
      curl -u admin:geoserver -XPUT -H "Content-type: application/json" \
        -d @taskUpdate.json \
        "http://localhost:8080/geoserver/rest/imports/1/tasks/0"
   
   Using:
   
   .. literalinclude:: files/taskUpdate.json
      :language: json
      :caption:  taskUpdate.json

#. Update transform to supply a geometry column:

   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST -H "Content-type: application/json" \
        -d @toPoint.json \
        "http://localhost:8080/geoserver/rest/imports/1/tasks/0/transforms"
        
   Using:
   
   .. literalinclude:: files/toPoint.json
      :language: json
      :caption:  toPoint.json

#. Double check import:

   .. code-block:: bash
   
      curl -u admin:geoserver -XGET \
        http://localhost:8080/geoserver/rest/imports/1.json
   
   .. code-block:: json
      :emphasize-lines: 15
   
      {
        "import": {
          "id": 2,
          "href": "http://localhost:8080/geoserver/rest/imports/1",
          "state": "PENDING",
          "archive": false,
          "targetWorkspace": {
            "workspace": {
              "name": "cite",
              "isolated": false
            }
          },
          "targetStore": {
            "dataStore": {
              "name": "postgis",
              "type": "PostGIS"
            }
          },
          "tasks": [
            {
              "id": 0,
              "href": "http://localhost:8080/geoserver/rest/imports/1/tasks/0",
              "state": "READY"
            }
          ]
        }
      }
   
   Task:

   .. code-block:: bash
   
      curl -u admin:geoserver -XGET \
        http://localhost:8080/geoserver/rest/imports/1/tasks/0.json
        
   .. code-block:: json
      :emphasize-lines: 5
   
      {
        "task": {
          "id": 0,
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0",
          "state": "READY",
          "updateMode": "REPLACE",
          "data": {
            "type": "file",
            "format": "CSV",
            "file": "replace.csv"
          },
          "target": {
            "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/target",
            "dataStore": {
              "name": "postgis",
              "type": "PostGIS"
            }
          },
          "progress": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/progress",
          "layer": {
            "name": "replace",
            "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/layer"
          },
          "transformChain": {
            "type": "vector",
            "transforms": [
              {
                "type": "AttributesToPointGeometryTransform",
                "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/transforms/0"
              }
            ]
          }
        }
      }
   
   Check layer to ensure ``name`` indicates layer to replace, and ``nativeName`` indicates
   the table contents to replace:
   
   .. code-block:: bash
   
      curl -u admin:geoserver -XGET \
        http://localhost:8080/geoserver/rest/imports/1/tasks/0/layer.json
        
   .. code-block:: json
      :emphasize-lines: 3,5,6,7
   
      {
        "layer": {
          "name": "values",
          "href": "http://localhost:8080/geoserver/rest/imports/1/tasks/0/layer",
          "title": "values",
          "originalName": "replace",
          "nativeName": "replace",
          "srs": "EPSG:4326",
          "bbox": {
            "minx": 0,
            "miny": 0,
            "maxx": -1,
            "maxy": -1
          },
          "attributes": [
            {
              "name": "AssetID",
              "binding": "java.lang.Integer"
            },
            {
              "name": "SampleTime",
              "binding": "java.lang.String"
            },
            {
              "name": "Lat",
              "binding": "java.lang.Double"
            },
            {
              "name": "Lon",
              "binding": "java.lang.Double"
            },
            {
              "name": "Value",
              "binding": "java.lang.Integer"
            }
          ],
          "style": {
            "name": "point",
            "href": "http://localhost:8080/geoserver/rest/imports/1/tasks/0/layer/style"
          }
        }
      }
      
   Transform:
   
   .. code-block:: bash
   
      curl -u admin:geoserver -XGET \
        http://localhost:8080/geoserver/rest/imports/1/tasks/0/transforms/0.json
   
#. To run the import:

   .. code-block:: bash
   
      curl -u admin:geoserver -XPOST \
        "http://localhost:8080/geoserver/rest/imports/1"

Uploading and optimizing a GeoTiff with ground control points 
-------------------------------------------------------------

A data supplier is periodically providing GeoTiffs that we need to configure in GeoServer.
The GeoTIFF is referenced via Ground Control Points, is organized by stripes, and has no overviews.
The objective is to rectify, optimize and publish it via the importer.

First, we are going to create a empty import with no store as the target::

    curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports"

Where import.json is::

    {
       "import": {
          "targetWorkspace": {
             "workspace": {
                "name": "sf"
             }
          }
       }
    }

Then, we are going to POST the GeoTiff file to the tasks list, in order to create an import task for it::

    curl -u admin:geoserver -F name=test -F filedata=@box_gcp_fixed.tif "http://localhost:8080/geoserver/rest/imports/0/tasks"
    
We are then going to append the transformations to rectify (gdalwarp), retile (gdal_translate) and add overviews (gdaladdo) to it::

   curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @warp.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"
   curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @gtx.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"
   curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @gad.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"
   

``warp.json`` is::

    {
      "type": "GdalWarpTransform",
      "options": [ "-t_srs", "EPSG:4326"]
    }


``gtx.json`` is::

    {
      "type": "GdalTranslateTransform",
      "options": [ "-co", "TILED=YES", "-co", "BLOCKXSIZE=512", "-co", "BLOCKYSIZE=512"]
    }

``gad.json`` is::

    {
      "type": "GdalAddoTransform",
      "options": [ "-r", "average"],
      "levels" : [2, 4, 8, 16]
    }

    
Now the import is ready to run, and we'll execute it using::

    curl -u admin:geoserver -XPOST "http://localhost:8080/geoserver/rest/imports/0"

A new layer ``box_gcp_fixed`` layer will appear in GeoServer, with an underlying GeoTiff file ready
for web serving.

Adding a new granule into an existing mosaic 
--------------------------------------------

A data supplier is periodically providing new time based imagery that we need to add into an existing mosaic
in GeoServer.
The imagery is in GeoTiff format, and lacks a good internal structure, which needs to be aligned with
the one into the other images.
  
First, we are going to create a import with an indication of where the granule is located, and
the target store:

    curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports"

Where import.json is::

    {
       "import": {
          "targetWorkspace": {
             "workspace": {
                "name": "topp"
             }
          },
          "data": {
            "type": "file",
            "file": "/home/aaime/devel/gisData/ndimensional/data/world/world.200407.3x5400x2700.tiff"
          },
          "targetStore": {
             "dataStore": {
                "name": "bluemarble"
             }
          }
       }
    }

We are then going to append the transformations to harmonize the file with the rest of the mosaic::

   curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @gtx.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"
   curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @gad.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"
   
``gtx.json`` is::

    {
      "type": "GdalTranslateTransform",
      "options": [ "-co", "TILED=YES"]
    }

``gad.json`` is::

    {
      "type": "GdalAddoTransform",
      "options": [ "-r", "average"],
      "levels" : [2, 4, 8, 16, 32, 64, 128]
    }

    
Now the import is ready to run, and we'll execute it using::

    curl -u admin:geoserver -XPOST "http://localhost:8080/geoserver/rest/imports/0"

The new granule will be ingested into the mosaic, and will thus be available for time based requests.

Asynchronously fetching and importing data from a remote server 
---------------------------------------------------------------

We assume a remote FTP server contains multiple shapefiles that we need to import in GeoServer
as new layers. The files are large, and the server has much better bandwidth than the client,
so it's best if GeoServer performs the data fetching on its own.

In this case a asynchronous request using ``remote`` data will be the best fit::

    curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports?async=true"

Where import.json is::

    {
       "import": {
          "targetWorkspace": {
             "workspace": {
                "name": "topp"
             }
          },
          "data": {
            "type": "remote",
            "location": "ftp://myserver/data/bc_shapefiles",
            "username": "dan",
            "password": "secret"
          }
       }
    }
    
The request will return immediately with an import context in "INIT" state, and it will remain in such
state until the data is fetched and the tasks created.
Once the state switches to "PENDING" the import will be ready for execution. Since there is
a lot of shapefiles to process, also the import run will be done in asynchronous mode::

    curl -u admin:geoserver -XPOST "http://localhost:8080/geoserver/rest/imports/0?async=true"
    
The response will return immediately in this case as well, and the progress can be followed as the
tasks in the import switch state.


Importing and optimizing a large image with a single request
------------------------------------------------------------

A large image appears every now and then on a mounted disk share, the image needs to be
optimized and imported into GeoServer as a new layer.
Since the source is large and we need to copy it on the local disk where the data dir resides,
a "remote" data is the right tool for the job, an asynchronous execution is also recommended
to avoid waiting on a possibly large command.
In this case the request will also contains the "exec=true" parameter to force the importer
an immediate execution of the command.

The request will then look as follows::

    curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports?async=true&exec=true"

Where import.json is::

    {
      "import": {
        "targetWorkspace": {
          "workspace": {
            "name": "topp"
          }
        },
        "data": {
          "type": "remote",
          "location": "\/mnt\/remoteDisk\/bluemarble.tiff"
        },
        "transforms": [
          {
            "type": "GdalTranslateTransform",
            "options": [
              "-co", "TILED=YES",
              "-co", "COMPRESS=JPEG",
              "-co", "JPEG_QUALITY=85",
              "-co", "PHOTOMETRIC=YCBCR"
            ]
          },
          {
            "type": "GdalAddoTransform",
            "options": [
              "-r",
              "average",
              "--config", "COMPRESS_OVERVIEW", "JPEG",
              "--config", "PHOTOMETRIC_OVERVIEW", "YCBCR"
            ],
            "levels": [ 2, 4, 8, 16, 32, 64 ]
          }
        ]
      }
    }
    
Given the request is asynchronous, the client will have to poll the server in order to check
if the initialization and execution have succeeded.
