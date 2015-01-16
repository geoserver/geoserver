.. _rest_api_sldpackage:

SLD Package
===========

An ``SLD Package`` describes  and SLD style, packaged together with its externals resources (images). See resource:ref:`SLD <styling>`.

``/sld/<ws>/<s>``
----------------------

Uploads and SLD package to create / update a style.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - 
     - 405
     - 
     - 
     -
   * - POST
     - Create a new style and copy the related resources in the styles folder
     - 201
     - ZIP
     - ZIP
     - 
   * - PUT
     - Updates an existing style SLD and resources
     - 200
     - ZIP
     - ZIP
     -
   * - DELETE
     - 
     - 405
     -
     -
     - 


When executing a POST or PUT request with an SLD package (zip file), the ``Content-type`` header should be set to application/zip.

The zip file should contain the sld and all the needed resources (currently image files). The resources
will be copied together with the SLD file into the styles folder.

Parameters
~~~~~~~~~~

.. _rest_api_styles_name:

``name``
^^^^^^^^

The ``name`` parameter specifies the name to be given to the style. This option is most useful when executing a POST request with a style in SLD format, and an appropriate name can be not be inferred from the SLD itself.
