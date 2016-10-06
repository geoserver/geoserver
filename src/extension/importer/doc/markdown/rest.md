# Importer REST API Reference

This document is a reference for the Importer rest api.

## Overview

The importer REST api is modelled closely after the model used internally by 
the importer. In order to understand the api it is useful to understand this
model and define some key terms.

An *import* refers to the top level object and is a "session" like entity the 
state of the entire import. It maintains information relevant to the import as 
a whole such as user infromation, timestamps, etc...

An import is made of any number of *task* objects. A task represents a unit of 
work to the importer and contains the following information:

* The data being imported
* The target store that is the destination of the import
 
The data of a task, referred to as its *source*, is the data to be processed as
part of the task. This data comes in a variety of forms including:

* A spatial file (Shapefile, GeoTiff, KML, etc...)
* A directory of spatial files
* A table in a spatial database

A task is classified as either "direct" or "indirect". A direct task is one 
in which the data being imported requires no transformation to be imported. It 
is imported *directly*. An example of such a task is one that involves simply 
importing an existing Shapefile as is. An indirect task is one that does 
require a transformation to the original import data. An example of an indirect
task is one that involves importing a Shapefile into an existing PostGIS 
database. Alternatively an indirect task can be defined as one that requires 
an "ingest" step.

A task is further broken down into *item* objects. An item represents a single 
dataset or layer that results from the import. For example, consider importing
a directory of Shapefiles. Each shapefile in the directory represents a single
item. An item maintains all the information needed to publish a spatial 
resource including:

* The spatial reference system (srs) of the resource
* The spatial bounds of the resource
* The name the resource is to be published under

## API Reference

* [Imports](#imports)
* [Tasks](#tasks)
* [Layers](#layers)
* [Directories](#directories)
* [Mosaics](#mosaics)
* [Databases](#databases)
* [Transforms](#transforms)

### Preamble

- TODO: Partial representations
- TODO: Error codes (400/500/404)

### <a id="imports"></a> Imports

#### List all imports

    GET /imports

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
       "imports": [{
         "id": 0,
         "state": "COMPLETE",
         "href": "http://localhost:8080/geoserver/rest/imports/0"

       }, {
         "id": 1,
         "state": "READY",
         "href": "http://localhost:8080/geoserver/rest/imports/1"          
       }]
    }

#### Get a single import

    GET /imports/:import

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "import": {
        "id": 1,
        "state": "READY", 
        "href": "http://localhost:8080/geoserver/rest/imports/1", 
        "data": {
          "type": "file", 
          "format": "Shapefile"
          "location": "/Users/jdeolive/Data/van", 
          "href": "http://localhost:8080/geoserver/rest/imports/1/data", 
        }, 
        "targetWorkspace": {
          "workspace": {
            "name": "scratch"
          }
        }, 
        "tasks": [
          {
            "id": 0,
            "state": "READY", 
            "href": "http://localhost:8080/geoserver/rest/imports/1/tasks/0"
          }
        ] 
      }
    }


#### Create a new import

    POST /imports

##### Input

###### targetWorkspace
&nbsp;&nbsp;
Optional *object* - The target GeoServer [workspace](#) to import into

###### targetStore
&nbsp;&nbsp;
Optional *object* - The target GeoServer [store](#) to import into 

###### data
&nbsp;&nbsp;
Optional *object* - The [data](#) to import

<!-- end list -->

    Content-Type: application/json

    {
       "import": {
          "targetWorkspace": {
             "workspace": {
                "name": "scratch"
             }
          },
          "targetStore": {
             "dataStore": {
                "name": "shapes"
             }
          },
          "data": {
            "type": "file",
            "file": "/data/spearfish/archsites.shp"
          }
       }
    }

##### Response

    Status: 201 Created
    Location: http://localhost:8080/geoserver/rest/imports/2
    Content-Type: application/json

    {  
      "import": {
        "id": 2, 
        "href": "http://localhost:8080/geoserver/rest/imports/2", 
        "state": "READY", 
        "targetWorkspace": {
          "workspace": {
            "name": "scratch"
          }
        }, 
        "targetStore": {
          "dataStore": {
            "name": "shapes", 
            "type": "PostGIS"
          }
        }, 
        "data": {
          "type": "file", 
          "format": "Shapefile", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/data", 
          "file": "archsites.shp"
        }, 
        "tasks": [
          {
            "id": 0, 
            "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0", 
            "state": "READY"
          }
        ]
      }
    }


#### Create a new import with a specific id

TODO: ask Ian about this, do we really need this ability?

#### Delete an import

    DELETE /imports/:import

##### Response

    Status: 204 No Content

### <a id="tasks"></a> Tasks

#### List all tasks of an import

    GET /imports/:import/tasks

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "tasks": [
        {
          "id": 0, 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0", 
          "state": "READY"
        }
      ]
    }

#### Get a single task

    GET /imports/:import/tasks/:task

##### Response

    Status: 200 OK
    Content-type: application/json

    {
      "task": {
        "id": 0, 
        "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0", 
        "state": "READY", 
        "data": {
          "type": "file", 
          "format": "Shapefile", 
          "file": "archsites.shp"
        }, 
        "target": {
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/target", 
          "dataStore": {
            "name": "shapes", 
            "type": "PostGIS"
          }
        }, 
        "progress": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/progress", 
        "layer": {
          "name": "archsites", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/layer"
        }, 
        "transformChain": {
          "type": "vector", 
          "transforms": []
        }
      }
    }

#### Create a new task from multi part form data

    POST /imports/:import/tasks

##### Input

Multipart encoded data as defined by RFC 2388.

    Content-type: multipart/form-data


##### Response

    Status: 201 Created
    Location: http://localhost:8080/geoserver/rest/imports/1/tasks/1
    Content-type: application/json

    {
      "task": {
        "id": 1, 
        "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1", 
        "state": "READY", 
        "data": {
          "type": "file", 
          "format": "Shapefile", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/data", 
          "file": "bugsites.shp"
        }, 
        "target": {
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/target", 
          "dataStore": {
            "name": "shapes", 
            "type": "PostGIS"
          }
        },
        "progress": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/progress", 
        "layer": {
          "name": "bugsites", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/layer"
        }, 
        "transformChain": {
          "type": "vector", 
          "transforms": []
        }
      }
    }

#### Create a new task from form upload

    POST /imports/:import/tasks

##### Input

Form url encoded data containing `url` parameter.

    Content-type: application/x-www-form-urlencoded

    url=file:///data/spearfish/

##### Response

    Status: 201 Created
    Location: http://localhost:8080/geoserver/rest/imports/1/tasks/1
    Content-type: application/json

    {
      "task": {
        "id": 2, 
        "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/2", 
        "state": "READY", 
        "data": {
          "type": "file", 
          "format": "Shapefile", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/2/data", 
          "file": "roads.shp"
        }, 
        "target": {
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/2/target", 
          "dataStore": {
            "name": "shapes", 
            "type": "PostGIS"
          }
        },
        "progress": "http://localhost:8080/geoserver/rest/imports/2/tasks/2/progress", 
        "layer": {
          "name": "roads", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/2/layer"
        }, 
        "transformChain": {
          "type": "vector", 
          "transforms": []
        }
      }
    }

#### Modify a task

    PUT /imports/:import/tasks/:task

##### Input

###### updateMode
&nbsp;&nbsp;
Optional *string* - The [Update mode](#), one of ``replace``, ``append``, or ``update``


<!-- end list -->

    Content-Type: application/json

    {
      "task": {
         "updateMode": "append"
      }
    }

##### Response

    Status: 202 Accepted


#### Delete a task

    DELETE /imports/:import/tasks/:task

##### Response

    Status: 204 No Content

#### Get data for a task

    GET /imports/:import/tasks/:task/data

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "type": "directory", 
      "format": "Shapefile", 
      "location": "/data/spearfish", 
      "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data", 
      "files": [
        {
          "file": "archsites.shp", 
          "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/archsites.shp"
        }, 
        {
          "file": "bugsites.shp", 
          "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/bugsites.shp"
        }, 
        {
          "file": "roads.shp", 
          "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/roads.shp"
        }
      ]
    }


### <a id="layers"></a> Layers

#### Get a task layer

    GET /imports/:import/tasks/:task/layer

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "layer": {
        "name": "archsites", 
        "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/layer", 
        "originalName": "archsites", 
        "nativeName": "archsites", 
        "srs": "EPSG:26713", 
        "bbox": {
          "minx": 589860, 
          "miny": 4914479, 
          "maxx": 608355, 
          "maxy": 4926490, 
          "crs": "PROJCS[\"NAD27 / UTM zone 13N\", ... AUTHORITY[\"EPSG\",\"26713\"]]"
        }, 
        "style": {
          "name": "scratch_archsites", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/layer/style"
        }
      }
    }

#### Modify a layer

    PUT /imports/:import/tasks/:task/layer

##### Input

###### name
&nbsp;&nbsp;
Optional *string* - Name of the layer.

###### srs
&nbsp;&nbsp;
Optional *string* - Spatial reference system identifier of the layer.

###### bbox
&nbsp;&nbsp;
Optional *object* - Bounding box of the layer.

###### style
&nbsp;&nbsp;
Optional *object* - Style of the layer.

<!-- end list -->

    Content-Type: application/json

    {
      "layer": {
        "name": "archaeological_sites", 
        "srs": "EPSG:26713", 
        "bbox": {
          "minx": 589860, 
          "miny": 4914479, 
          "maxx": 608355, 
          "maxy": 4926490, 
          "crs": "EPSG:26713"
        }, 
        "style": {
          "name": "point"
        }
      }
    }

##### Response

    Status: 202 Accepted
    Content-Type: application/json

    {
      "layer": {
        "name": "archaeological_sites", 
        "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/layer", 
        "originalName": "archsites", 
        "nativeName": "archsites", 
        "srs": "EPSG:26713", 
        "bbox": {
          "minx": 589860, 
          "miny": 4914479, 
          "maxx": 608355, 
          "maxy": 4926490, 
          "crs": "PROJCS[\"NAD27 / UTM zone 13N\", ... AUTHORITY[\"EPSG\",\"26713\"]]"
        }, 
        "style": {
          "name": "point", 
          "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0/layer/style"
        }
      }
    }


###  <a id="directories"></a> Directories

The following operations are specific to data objects of type `directory`.

#### List files of directory

    GET /imports/:import/tasks/:task/data/files

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "files": [
        {
          "file": "archsites.shp", 
          "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/archsites.shp"
        }, 
        {
          "file": "bugsites.shp", 
          "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/bugsites.shp"
        }, 
        {
          "file": "roads.shp", 
          "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/roads.shp"
        }
      ]
    }


#### Get a single file

    GET /imports/:import/tasks/:task/data/files/:file

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "type": "file", 
      "format": "Shapefile", 
      "href": "http://localhost:8080/geoserver/rest/imports/3/tasks/1/data/files/archsites.shp", 
      "location": "/data/spearfish", 
      "file": "archsites.shp", 
      "prj": "archsites.prj", 
      "other": [
        "archsites.dbf", 
        "archsites.shx"
      ]
    }

#### Delete a file

    DELETE /imports/:import/tasks/:task/data/files/:file

##### Response

    Status: 204 No Content


### <a id="mosaics"></a> Mosaics    

In addition to the following operation data objects of type `mosaic` inheiret all those of type `directory`.

#### Update timestamp of a mosaic

    PUT /imports/:import/tasks/:task/data/files/:file

##### Input

###### timestamp
&nbsp;&nbsp;
Optional *string* - Timestamp to associate with the mosaic granule 

    Content-Type: application/json

    {
       "timestamp": "2004-01-01T00:00:00.000+0000"
    }

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "type": "file", 
      "format": "GeoTIFF", 
      "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/bm_200401.tif", 
      "location": "/data/bluemarble/mosaic", 
      "file": "bm_200401.tiff", 
      "prj": null, 
      "other": [], 
      "timestamp": "2004-01-01T00:00:00.000+0000"
    }

###  <a id="databases"></a> Databases

The following operations are specific to data objects of type `database`.

#### List tables of a database

    GET /imports/:import/tasks/:task/data/tables

#### Get a single table

    GET /imports/:import/tasks/:task/data/tables/:table

#### Delete a table

    DELETE /imports/:import/tasks/:task/data/tables/:table

###  <a id="transforms"></a> Transforms

#### List all transforms of a task

    GET /imports/:import/tasks/:task/transforms

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "transforms": [
        {
          "type": "ReprojectTransform", 
          "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transforms/0", 
          "source": null, 
          "target": "EPSG:4326"
        }, 
        {
          "type": "DateFormatTransform", 
          "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transforms/1", 
          "field": "date", 
          "format": "yyyyMMdd"
        }
      ]
    }

#### Get a single transform

    GET /imports/:import/tasks/:task/transforms/:transform

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "type": "ReprojectTransform", 
      "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transforms/0", 
      "source": null, 
      "target": "EPSG:4326"
    }

#### Create a new transform

    POST /imports/:import/tasks/:task/transforms

##### Input

###### type
&nbsp;&nbsp;
Required *string* - Identifies the class of transform

Other properties are dependent on the specific transform. See .... for more details.

    Content-Type: application/json

    {
       "type": "AttributeRemapTransform",
       "field": "cat",
       "target": "java.lang.Integer"
    }

##### Response

    Status: 201 OK
    Location: http://localhost:8080/geoserver/rest/imports/0/tasks/1/transform/2

#### Modify a transform

    PUT /imports/:import/tasks/:task/transforms/:transform

##### Input

###### type
&nbsp;&nbsp;
Required *string* - Identifies the class of transform

Other properties are dependent on the specific transform. See .... for more details.

    Content-Type: application/json
    {
       "type": "ReprojectTransform",
       "target": "EPSG:3005"
    }

##### Response

    Status: 200 OK
    Content-Type: application/json

    {
      "type": "ReprojectTransform", 
      "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transform/0", 
      "source": null, 
      "target": "EPSG:3005"
    }

#### Delete a transform

    DELETE /imports/:import/tasks/:task/transforms/:transform

##### Response

    Status: 200 OK


