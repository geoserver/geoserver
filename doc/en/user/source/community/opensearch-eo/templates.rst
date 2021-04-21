.. _oseotemplates:

OpenSearch/STAC JSON templates
==============================

General rules for writing the (Geo)JSON templates:

* Single ``//`` and multiline ``/* ... */`` comments are allowed in the input, for the editor's convenience.
  and will be discarded in the output.
* The properties follow the same names as the OpenSearch queryables:

  * If the column in the database has no prefix, use none in the template too (e.g. ``startTime``).
  * If the column has a prefix, it gets transformed, so for example, ``eoInstrument`` in the database becomes ``eo:instrument`` in the template.
  * The "eo" prefix has a special rule, when it's used in the ``products`` table, it's called ``eop`` in the template. For example the column ``eoSensorMode`` in the ``products`` tables becomes ``eop:sensorMode``.
* Two special CQL functions assist in the creation of links:

  * ``serviceLink`` takes a URL template using the `Java Formatter syntax <https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html>`_ and all the subsequent paramters are meant to be replaced in the template, one by one. This is used for links that are built on the fly based on product/collection attributes only.
  * ``templateLink`` takes a URL template using a single ``${BASE_URL}`` placeholder, and replaces it with the base URL of the request. This is already used by RSS output, and assumes the links are contained database fields, like the OGC links.

For for information about building feature templates, refer to the :ref:`features templating <community_wfstemplating>` documentation.