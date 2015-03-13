.. _importer_rest_examples:

Importer REST API examples
==========================

Mass configuring a directory of shapefiles
------------------------------------------

In order to initiate an import of the ``c:\data\tasmania`` directory into the existing ``tasmania`` workspace the following JSON will be POSTed to GeoServer::

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

This curl command can be used for the purpose::
  
  curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports"
  
The importer will locate the files to be imported, and automatically prepare the tasks, returning the following response::

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

After checking every task is ready, the import can be initiated by executing a POST on the import resource::

  curl -u admin:geoserver -XPOST "http://localhost:8080/geoserver/rest/imports/9"
  
The resource can then be monitored for progress, and eventually final results::

  curl -u admin:geoserver -XGET "http://localhost:8080/geoserver/rest/imports/9"

Which in case of successful import will look like::

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

In this case, let's assume we have a single shapefile, tasmania_cities.shp, that does not have the .prj anciliary file 
(the example is equally good for any case where the prj file contents cannot be matched to an official EPSG code).

We are going to post the following import definition::

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
	
With the usual curl command::

 curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports"

The response in case the CRS is missing will be::

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

Drilling into the task layer we can see the srs information is missing::

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
	        "binding": "com.vividsolutions.jts.geom.MultiPoint"
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
	
The following PUT request will update the SRS::	

    curl -u admin:geoserver -XPUT -H "Content-type: application/json" -d @layerUpdate.json "http://localhost:8080/geoserver/rest/imports/13/tasks/0/layer/"
	
Where ``layerUpdate.json`` is::

	{
	   layer : {
	      srs: "EPSG:4326"
	   }
	}  
	
Getting the import definition again, we'll find it ready to execute::

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

A POST request will make it execute::

  curl -u admin:geoserver -XPOST "http://localhost:8080/geoserver/rest/imports/13"

And eventually succeed::

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
	
Uploading a CSV file to PostGIS while transforming it
-----------------------------------------------------

A remote sensing tool is generating CSV files with some locations and measurements, that we want to upload
into PostGIS as a new spatial table. The CSV file looks as follows::

	#AssetID, SampleTime, Lat, Lon, Value
	1, 2015-01-01T10:00:00, 10.00, 62.00, 15.2
	1, 2015-01-01T11:00:00, 10.10, 62.11, 30.25
	1, 2015-01-01T12:00:00, 10.20, 62.22, 41.2
	1, 2015-01-01T13:00:00, 10.31, 62.33, 27.6
	1, 2015-01-01T14:00:00, 10.41, 62.45, 12


First, we are going to create a empty import with an existing postgis store as the target::

	curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @import.json "http://localhost:8080/geoserver/rest/imports"

Where import.json is::

	{
	   "import": {
	      "targetWorkspace": {
	         "workspace": {
	            "name": "topp"
	         }
	      },
	      "targetStore": {
	         "dataStore": {
	            "name": "gttest"
	         }
	      }
	   }
	}

Then, we are going to POST the csv file to the tasks list, in order to create an import task for it::

	curl -u admin:geoserver -F name=test -F filedata=@values.csv "http://localhost:8080/geoserver/rest/imports/0/tasks"

And we are going to get back a new task definition, with a notification that the CRS is missing::	
	
	{
	  "task": {
	    "id": 0,
	    "href": "http://localhost:8080/geoserver/rest/imports/16/tasks/0",
	    "state": "NO_CRS",
	    "updateMode": "CREATE",
	    "data": {
	      "type": "file",
	      "format": "CSV",
	      "file": "values.csv"
	    },
	    "target": {
	      "href": "http://localhost:8080/geoserver/rest/imports/16/tasks/0/target",
	      "dataStore": {
	        "name": "values",
	        "type": "CSV"
	      }
	    },
	    "progress": "http://localhost:8080/geoserver/rest/imports/16/tasks/0/progress",
	    "layer": {
	      "name": "values",
	      "href": "http://localhost:8080/geoserver/rest/imports/16/tasks/0/layer"
	    },
	    "transformChain": {
	      "type": "vector",
	      "transforms": [
	        
	      ]
	    }
	  }
	}

As before, we are going to force the CRS by updating the layer::

    curl -u admin:geoserver -XPUT -H "Content-type: application/json" -d @layerUpdate.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer/"
	
Where ``layerUpdate.json`` is::

	{
	   layer : {
	      srs: "EPSG:4326"
	   }
	}  

Then, we are going to create a transformation mapping the Lat/Lon columns to a point::

	{
	  "type": "AttributesToPointGeometryTransform",
	  "latField": "Lat",
	  "lngField": "Lon"
	}

The above will be uploaded to GeoServer as follows::

    curl -u admin:geoserver -XPOST -H "Content-type: application/json" -d @toPoint.json "http://localhost:8080/geoserver/rest/imports/0/tasks/0/transforms"

Now the import is ready to run, and we'll execute it using::

    curl -u admin:geoserver -XPOST "http://localhost:8080/geoserver/rest/imports/0"

If all goes well the new layer is created in PostGIS and registered in GeoServer as a new layer.

In case the features in the CSV need to be appended to an existing layer a PUT request against the task might be performed, changing its
updateMode from "CREATE" to "APPEND". Changing it to "REPLACE" instead will preserve the layer, but remove the old conents and replace
them with the newly uploaded ones.