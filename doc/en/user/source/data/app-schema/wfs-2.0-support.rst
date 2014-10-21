.. _app-schema.wfs20-support:

WFS 2.0 Support
===============

..  _app-schema.resolve:

Resolving
---------

Local resolve is supported in app-schema. This can be done by setting the 'resolve' parameter to either 'local' or 'all'. (Remote Resolving is not supported.)
The parameter 'resolveDepth' specifies how many levels of references will be resolved. The parameter 'resolveTimeOut' may be used to specify, in seconds,
an upper limit to how long app-schema should search for the feature needed for resolving. If the time out limit is reached, the feature is not resolved.

When resolving without Feature Chaining (see below), a GML ID is extracted from the x-link reference and a brute force is done on all feature types to find a feature with this GML ID.
The extraction of this GML ID from the Xlink Reference is done using the following rules:

  * In case of a URN: The GML ID comes after last colon in the URN. Make sure that the  *full* GML ID is included after the last colon (including a possible feature type prefix).
  * In case of a URL: The GML ID comes after the # symbol.

Failing to respect one of these rules will result in failure of resolve.

Resolving and Feature Chaining By Reference
```````````````````````````````````````````
The 'resolve' and 'resolveDepth' parameters may also be used in the case of :ref:`app-schema.feature-chaining-by-reference`.
In this case, no brute force will take place, but resolving will instruct App-Schema to do full feature chaining rather than inserting a reference. The URI will not be used to find the feature, 
but the feature chaining parameters specified in the mapping, as with normal feature chaining. Because of this, the parameter 'resolveTimeOut' will be ignored in this case.

However, be aware that every feature can only appear once in a response. If resolving would break this rule, for example with circular references, the encoder will change the resolved feature back
to an (internal) x-link reference.


GetPropertyValue
----------------

The GetPropertyValue request is now fully supported. Resolving is also possible in this request, following the same rules as described above.

Paging
------

Paging is now supported in App-Schema. There are a few exceptions:

   * Paging is only supported for data stores with JDBC back ends and will not work for data stores with property files. It has been tested with Oracle and PostGIS databases.
   * Paging with filters involving attributes that are mapped to functions will not be supported, as this cannot be translated into SQL.

For more efficient SQL queries generation, please set ``isDenormalised`` to false where applicable (when a one to one database table is used). See :ref:`app-schema.mapping-file`.
