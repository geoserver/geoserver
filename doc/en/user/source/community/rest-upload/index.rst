.. _community_rest-upload:

Resumable REST Upload Plugin
============================
This plugin can be used for managing the upload of files in GeoServer via REST with the possibility to resume uploads which are not completed.

Installing the Plugin
----------------------------------------------

    #. Download the plugin from the `nightly GeoServer community module builds <https://build.geoserver.org/geoserver/master/community-latest/>`_.

    .. warning:: Make sure the version of the extension matches the version of the GeoServer instance!

    #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.


Plugin Description
----------------------------------------------

The upload service is published under the common prefix **/rest/resumableupload/${uploadId}**, where uploadId is the unique identifier of each upload request.


The completed uploaded file is moved to REST *ROOT folder*, if configured, otherwise in *$GEOSERVER_DATA_DIR/data* folder (see documentation of Path Mapper at the :ref:`Global Settings documentation page <config_globalsettings>`

Here is a description of general workflow that clients must follow to handle file upload, manage returned data and resume upload:


#.	The client starts a new upload task sending a *text/plain* **REST POST** at url **/rest/resumableupload/**, the body of request must contain the desired relative path for uploaded file.
#.	The server creates an **uploadId** that identifies this upload task. 

#.	The server replies with **201 (CREATED)**: the body of response contains the absolute URL to call in successive **PUT** requests to upload file. The same information is in the *Location* header attribute.

#.	The client starts the file upload with an *application/octet-stream* **REST PUT** at url returned from the POST (relative url is **/rest/resumableupload/${uploadId}** ). The body of the **PUT** contains the file bytes to upload. It is mandatory to set the header attribute **Content-Length** with the total number of byte of the current upload (same as the complete size of file in bytes). 

#.	The server saves a temporary file into the desired path relative to a configurable folder (default is *tmp/upload* subfolder of *GeoServer data directory*). 
	
#.	The server reply can be:

		* **200 (OK)** : The full file is correctly uploaded, the body of response contains the URL of uploaded file relative to the destination folder (*REST ROOT* or GEOSERVER DATA directory)
		* **308 (Resume Incomplete)** :The file is partially uploaded, the header of response contains the **Range** attribute in the format *0-<last uploaded byte index>*.

#.	The client resumes the file upload after a **308 PUT** response, sending another **REST PUT** at the same URL of previous. The body of request contains the bytes file to upload starting from the *<last uploaded byte index>+1* byte. The header of request must contains these attributes: Content-Length with the total number of byte of current upload and the "Range" in the format *Bytes:<last uploaded byte index+1> - <max byte index in current upload> / <complete file size in bytes>*. The server reply is as the step 2.

#.	The client can retrieve information about an upload task using **GET** request at the URL returned by the first **POST** request. 
	
#.	The server reply can be:

		* **200 (OK)** : The full file is correctly uploaded
		* **308 (Resume Incomplete)** :The file is partially uploaded, the header of response contains the "Range" attribute in the format "0-<last uploaded byte index>".


The uploads which are not completed and are not resumed from too much time (default value is 300000 milliseconds) will be removed from server.
The completed files will be reachable via GET request for a limited time (default value is 300000 milliseconds).

The timeout of files cleaner task and the temporary subfolder path can be configured by adding a **resumableUpload.properties** file to the GeoServer data directory with the definition of properties *resumable.tempPath* and *resumable.expirationDelay* (in milliseconds). Each modification of this file requires to restart GeoServer.

Example of usage
-------------------------


*POST*
REQUEST::

    curl -v -u admin:geoserver -H "Content-type text/plain" -XPOST -d "/test/test.txt" http://localhost:8080/geoserver/rest/resumableupload

REPLY::

    HTTP/1.1 201 Created-----TO USE IN PUT-----

    http://localhost:8080/geoserver/rest/resumableupload/1eb35cad-c715-40f5-adf5-bba91d9c947e-----------------------



*PUT (PARTIAL UPLOAD, SERVER FAILS, 3 bytes saved)*
REQUEST::

    curl -v -u admin:geoserver -H "Content-type application/octet-stream" -H "Content-Length:4" -XPUT --data-binary "test" http://localhost:8080/geoserver/rest/resumableupload/1eb35cad-c715-40f5-adf5-bba91d9c947e


*GET*
REQUEST::

    curl -v -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/resumableupload/1eb35cad-c715-40f5-adf5-bba91d9c947e

REPLY::

    HTTP/1.1 308 308
    Range: 0-2

*PUT (RESUME UPLOAD, transfer only "t" byte)*
REQUEST::

    curl -v -u admin:geoserver -H "Content-type application/octet-stream" -H "Content-Length:1" -H "Content-Range:Bytes=3-4/4" -XPUT --data-binary "t" http://localhost:8080/geoserver/rest/resumableupload/1eb35cad-c715-40f5-adf5-bba91d9c947e

REPLY::

    HTTP/1.1 200 OK
    test/test.txt
