.. _rest_api_resources:

Resources
=========

``/resource</path/to/resource>``
---------------------------------

.. list-table::
   :widths: 10 30 10 20
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Parameters
   * - GET
     - Download a resource, list contents of directory, or show formatted resource metadata.
     - 200
     - operation (default | metadata); format (html | xml | json)
   * - HEAD
     - Show resource metadata in HTTP headers.
     - 200
     -
   * - PUT
     - Upload/move/copy a resource, create directories on the fly (overwrite if exists). For move/copy operations, place source path in body.
       Copying is not supported for directories.
     - 200 (exists) 201 (new)
     - operation (default | copy | move)
   * - DELETE
     - Delete a resource (recursively if directory)
     - 200
     - 

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET or DELETE for a resource that does not exist
     - 404
   * - PUT to directory
     - 405
   * - PUT method=copy with source directory
     - 405
   * - PUT with source path that doesn't exist
     - 404
   * - POST 
     - 405

Headers
~~~~~~~

.. list-table::
   :header-rows: 1

   * - Header
     - Description
   * - Last-Modified
     - When resource was last modified.
   * - Content-Type
     - Will guess mime-type from extension or content.
   * - Resource-Type (custom)
     - directory | resource
   * - Resource-Parent (custom)
     - Path to parent

Format
~~~~~~

Examples are given in XML. The JSON and HTML formats are analogue.

Metadata
^^^^^^^^

.. code-block:: xml

   <ResourceMetaData>
      <name> nameOfFile </name>
      <parent> <path> path/to/parent </path>
           <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate"
      href="http://localhost:8080/geoserver/rest/resource/path/to/parent?operation=metadata&format=xml" 
            type="application/xml"/>
      </parent>
      <lastModified> date </lastModified>
      <type> undefined | resource | directory </type>
   </ResourceMetaData>


Directories
^^^^^^^^^^^

.. code-block:: xml

   <ResourceDirectory>`
      <name> nameOfDirectory </name>
      <parent> <path> path/to/parent </path>
           <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate"
      href="http://localhost:8080/geoserver/rest/resource/path/to/parent?operation=metadata&format=xml" 
            type="application/xml"/>
      </parent>
      <lastModified> date </lastModified>
      <children>
          <child>
                  <name> ... </name>
                  <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate"
                   href="http://localhost:8080/geoserver/rest/resource/path/to/child"/>
          </child>
          <child>
                  ...
          </child>
          ...
      </children>` 
   </ResourceDirectory>


