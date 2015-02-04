.. _data_external_stored_query:

Cascaded Web Feature Service Stored Queries
===========================================

Stored query is a feature of Web Feature Services. It allows servers to serve pre-configured filter queries or even queries that cannot be expressed as GetFeature filter queries. This feature of GeoServer allows creating cascaded layers out of stored queries.

Stored queries are read-only, and thus layers derived from cascaded stored queries cannot be updated with WFS-T.

Cascaded Stored Query parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The relationship between stored query parameters and the schema returned by the query is not well defined. So to allow cascading to stored queries, the relationship between the query received by GeoServer and the parameters passed to the stored query must be defined.

In practice this means that when setting up a layer based on a stored query, the administrator must not only select the stored query that they want to cascade requests to, but also select what value, if any, is passed to each parameter. To allow for flexibility, cascaded stored queries can leverage view parameters passed to the query. This is similar to how arbitrary parameters are passed to :ref:`sql_views`. GeoServer supports multiple strategies to pass these values. See below for a full list.

.. list-table::
   :widths: 20 80

   * - **Parameter type**
     - **Explanation**
   * - :guilabel:`No mapping`
     - View parameter in query will be passed as such to the stored query
   * - :guilabel:`Blocked`
     - This parameter will never be passed to the stored query
   * - :guilabel:`Default`
     - The specified value is used unless overwritten by a view parameter 
   * - :guilabel:`Static`
     - The specified value is always used (view parameter value will be ignored)
   * - :guilabel:`CQL Expression`
     - An expression that will be evaluated on every request (see :ref:`data_external_stored_query_cql`)

See :ref:`using_a_parametric_sql_view` for more details how clients pass view parameters to GeoServer. 

Configuring a Cascaded Stored Query layer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

TODO


.. _data_external_stored_query_cql:

CQL Expressions
^^^^^^^^^^^^^^^

Parameter mappings configured as CQL expressions are evaluated for each request using a context derived from the request query and the view parameters. General information on CQL expressions is available here :ref:`ecql_expr`. In addition to standard CQL, the syntax here allows using the + operator to concatenate strings.

The context contains the following properties that may be used in the expressions:

.. list-table::
   :widths: 20 80

   * - **Property name**
     - **Explanation**
   * - ``bboxMinX`` ``bboxMinY`` ``bboxMaxX`` ``bboxMaxY``
     - Evaluates to a corner coordinate of the full extent of the query
   * - ``defaultSRS``
     - Evaluates to the default SRS of the feature type
   * - ``viewparam:name``
     - Evaluates to the value of the view parameter *name* in this query
