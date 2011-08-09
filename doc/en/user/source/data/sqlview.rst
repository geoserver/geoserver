.. _sql_views:

SQL views
=========

The traditional way to use database backed data is to configure either a table or a database view as a new layer in GeoServer.
Starting with GeoServer 2.1.0 the user can also create a new layer by specifying a raw SQL query, without the need to actually creating a view in the database. The SQL can also be parameterized, and parameter values passed in along with a WMS or WFS request.

Creating a plain SQL view
-------------------------

In order to create an SQL view the administrator can go into the "create new layer" page. Upon selection of a database backed store a list of tables and views available for publication will appear, but at the bottom of if a new link, "create SQL view", will appear:

.. figure:: images/createsqlview.png
   :align: center
   
Selecting the link will open a new page where the SQL statement can be specified:

.. figure:: images/createsql.png
   :align: center
   
.. note::

   The query can be any SQL statement that can be validly executed as part of a subquery in the FROM clauses, that is ``select * from (<the sql view>) [as] vtable``. This is true for most SQL statements, but specific syntax might be needed to call onto a stored procedure depending on the database.
   Also, all the columns returned by the SQL statement must have a name, in some databases aliasing is required when calling function names
   
Once a valid SQL statement has been specified press the "refresh" link in the Attributes table to get a list of the feature type attributes:

.. figure:: images/sqlview-attributes.png
   :align: center

GeoServer will do its best to figure out automatically the geometry type and the native srid, but they should always be double checked and eventually corrected. In particular having the right SRID (spatial reference id) is key to have spatial queries actually work. In many spatial databases the SRID is equal to the EPSG code for the specific spatial reference system, but that is not always true (e.g., Oracle has a number of non EPSG SRID codes).

If stable feature ids are desired for the view's features one or more column providing a unique identification for the features should be checked in the "Indentifier" column. Always make sure those attributes generate a actually unique key, or filtering and WFS clients will mishbehave.

Once the query and the attribute details are set press save and the usual new layer configuration page will show up.
That page will have a link to a SQL view editor at the bottom of the "Data" tab:

.. figure:: images/sqlview-edit.png
   :align: center

Once create the SQL view based layer can be used as any other table backed layer.

Creating a parametric SQL view
------------------------------

.. warning:: As a rule of thumb use SQL parameter substitution only if the required functionality cannot be obtained with safer means, such as dynamic filtering (CQL filters) or SLD parameter substitution. Only use SQL parameters as a last resort, improperly validated parameters can open the door to `SQL injection attacks <http://en.wikipedia.org/wiki/SQL_injection>`_.

A parametric SQL view is based on a SQL query containing parameters whose values can be dinamically provided along WMS or WFS requests.
A parameter is bound by % signs, can have a default value, and should always have a validation regular expression.

Here is an example of a SQL query with two parameters, ``low`` and ``high``:

.. figure:: images/sqlview-parametricsql.png
   :align: center

The parameters can be manually specified, but GeoServer can figure out the parameter names by itself when the "Guess parameters from SQL" link is clicked. The result will be a parameter table filled with the parameter names and some default validation expressions:

.. figure:: images/sqlview-paramdefault.png
   :align: center

In this case the query cannot be executed without default values, as ``select gid, state_name, the_geom from pgstates where persons between and`` would be invalid SQL. Moreover, the two parameters are positive integer numbers, so the validation expression should be adjusted to allow only that kind of input:

.. figure:: images/sqlview-paramcustom.png
   :align: center
   
Once the default values have been set the "Attributes" refresh link can be used to double check the query, retrive the attributes and eventually fix the geometry and identifier details. At this point the workflow proceeds just like with a non parameterized query.

Going to the WMS preview for the ``popstates`` layer should result in all the states being displayed.
The SQL view parameters can now be specified by adding the ``viewparams`` parameter in the GetMap request. ``viewparams`` is structured as a set of key/value pairs separated by semicolumns: ``viewparams=p1:v1;p2:v2;...``.  If you want to use semicolons or commas in your values they will need to be escaped with a backslash (``\,`` and ``\;``).
For example, to select all states having more than 20 million inhabitatants the following params can be added to the normal GetMap request:
 ``low:20000000``

.. figure:: images/sqlview-20millions.png
   :align: center

In order to get all the states having between 2 and 5 millions inhabitatants the following can be specified instead:  ``&viewparams=low:2000000;high:5000000``

.. figure:: images/sqlview-2m-5m.png
   :align: center
   
   
The ``viewparams`` can be also specified on a layer per layer basis using the syntax ``viewparams=l1p1:v1;l1p2:v2,l2p1:v1;l2p2:v2,...``, that is, separating each layer map with a comma. In this case the number of parameter maps must match the number of layers (or feature types) included in the request.

Parameters and validation
-------------------------

A SQL view parameter can be anything, the only rule to follow is that the set of attributes returned by the view and their types must never change.
This in particular means it's possible to create views containing wide open parameters allowing to specify full SQL query portions.

For example, ``select * from pgstates %where%``, along with an empty validation regular experssion, would allow to specify the where clause of the query dynamically.
However, that opens a serious risk for `SQL injection attacks <http://en.wikipedia.org/wiki/SQL_injection>`_ unless access to the server is allowed only to trusted parties.

In general it is advised to use SQL parameters with great care and cast a validation regular expression that only allows for the intended parameter values. The expression should be created to prevent attacks, but not necessarily to double check the value is the expected type.

For example:

  * ``^[\d\.\+-eE]+$`` will check that the parameter value is composed with valid elements for a floating point number, eventually in scientific notation, but will not check that the provided value is actually a valid floating point number
  * ``[^;']+`` will check the parameter value does not contain quotes or semicolumn, preventing common sql injection attacks, without actually imposing much on the parameter value structure

Regular expressions references
------------------------------

Casting the proper validation regular expression is important in terms of security. 
Regular expressions are a wide topic that cannot be addressed in a short space. Here is a set of links on the internet to get more information about this topic:

  * The regular expression engine used by GeoServer is the Java built-in one. The `Pattern class javadocs <http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html>`_ contain the full specification of the allowed syntax.
  * This `<http://www.regular-expressions.info>`_ site is fully dedicated to regular expressions, with tutorials and examples.
  * This `applet <http://myregexp.com/>`_ can be used to interactively test a regular expression online.
  