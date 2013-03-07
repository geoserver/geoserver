.. _app-schema.joining:

Joining Support For Performance
===============================

App-schema joining is a optional configuration parameter that tells app-schema to use a different implementation for :ref:`app-schema.feature-chaining`, 
which in many cases can improve performance considerably, by reducing the amount of SQL queries sent to the DBMS.

Conditions
----------
In order to use App-schema Joining, the following configuration conditions must be met:

* All feature mappings used must be mapped to JDBC datastores.

* All feature mappings that are chained to each other must map to the same physical database.

* In your mappings, there are restrictions on the CQL expressions specified in the <SourceExpression> of both the referencing field in the parent feature as well as the referenced field in the nested feature (like FEATURE_LINK). Any operators or functions used in this expression must be supported by the filter capibilities, i.e. geotools must be able to translate them directly to SQL code. This can be different for each DBMS, though as a general rule it can assumed that comparison operators, logical operators and arithmetic operators are all supported but functions are not. Using simple field names for feature chaining is guaranteed to always work.

Failing to comply with any of these three restrictions when turning on Joining will result in exceptions thrown at run-time.

When using app-schema with Joining turned on, the following restrictions exist with respect to normal behaviour:

* XPaths specified inside Filters do not support handling referenced features (see  :ref:`app-schema.feature-chaining-by-reference`) as if they were actual nested features, i.e. XPaths can only be evaluated when they can be evaluated against the actual XML code produced by WFS according to the XPath standard.

Configuration
-------------
Joining is turned on by default. It is disabled by adding this simple line to your app-schema.properties file (see :ref:`app-schema.property-interpolation`) ::

     app-schema.joining = false

Or, alternatively, by setting the value of the Java System Property "app-schema.joining" to "false", for example ::

     java -DGEOSERVER_DATA_DIR=... -Dapp-schema.joining=false Start

Not specifying "app-schema.joining" parameter will enable joining by default. 

Database Design Guidelines
--------------------------

* Databases should be optimised for fast on-the-fly joining and ordering.

* Make sure to put indexes on all fields used as identifiers and for feature chaining, unique indexes where possible. Lack of indices may result in data being encoded in the wrong order or corrupted output when feature chaining is involved.

* Map your features preferably to normalised tables.

* It is recommended to apply feature chaining to regular one-to-many relationships, i.e. there should be a unique constraint defined on one of the fields used for the chaining, and if possible a foreign key constraint defined on the other field.

Effects on Performance
----------------------

Typical curves of response time for configurations with and without joining against the amount of features
produced will be shaped like this:

.. image:: joining.png

In the default implementation, response time increases rapidly with respect to the amount of produced features. This is because feature chaining
is implemented by sending multiple SQL requests to the DBMS per feature, so the amount of requests increases with the amount
of features produced. When Joining is turned on, response time will be almost constant with respect to the number of features. This is because in this implementation a small amount of larger queries is sent to the DBMS, independant of the amount of features produced.
In summary, difference in performance becomes greater as the amount of features requested gets bigger. General performance of joining will be dependant on database and mapping design (see above) and database size. 

Using joining is strongly recommended when a large number of features need to be produced, for example 
when producing maps with WMS (see :ref:`app-schema.wms-support`).

Optimising the performance of the database will maximise the benefit of using joining, including for small queries.
