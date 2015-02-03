.. _data_external_stored_query:

Cascaded Web Feature Service Stored Queries
===========================================

Stored query is a feature of Web Feature Services. It allows servers to serve pre-configured filter
queries or even queries that cannot be expressed as GetFeature filter queries. The relationship
between stored query parameters and properties returned by the query are not well defined. To allow
cascading to stored queries, the relationship between the query received by GeoServer and the
parameters passed to the stored query must be defined.

In practice this means that when setting up a layer based on a stored query, the administrator must
not only select the stored query that they want to cascade requests to, but also select what values
are passed for each parameters. GeoServer supports multiple strategies to pass these values:
static values, view parameters passed by the user and expressions that take advantage of the
context of this particular request.

.. list-table::
   :widths: 20 80

   * - **Parameter type**
     - **Explanation**
   * - Default
     - The specified value is used unless overwritten by a view parameter
   * - Static
     - The specified value is always used despite view parameters
   * - Blocked
     - This parameter will never be passed to the stored query
   * - CQL Expression
     - An expression that will be evaluated on every request

CQL Expressions
---------------
