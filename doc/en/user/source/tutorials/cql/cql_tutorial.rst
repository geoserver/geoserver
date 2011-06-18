.. _cql_tutorial:

CQL and ECQL
=============

CQL (OGC Common Query Language) is a query language created by OGC for the `Catalogue WebServices specification <http://www.opengeospatial.org/standards/cat>`_. Unlike the OGC Filter specification, CQL is plain text, human readable, and thus well suited for manual construction as opposed to machine generation.
However CQL has some serious limitations, for example it cannot encode id filters and requires an attribute to be on the left side of any comparison operator.
ECQL removes such limitations making for a more flexible language with stronger similarities with SQL. 

GeoServer supports the use of both CQL and ECQL in WMS and WFS requests, as well as in dynamic symbolizers. When the documentation refers to CQL
you can rest assured ECQL syntax can be used as well (and if not, please report that as a bug).

This tutorial introduces the language by example.
If you need a full reference instead have a look at the `ECQL BNF definition <http://docs.codehaus.org/display/GEOTOOLS/ECQL+Parser+Design>`_ on the GeoTools site.

Getting started
---------------
All the following examples are going to use the ``topp:states`` sample layer shipped with GeoServer, and will use the CQL_FILTER vendor parameter to show how the CQL filters alter the map appearance. The easiest way to follow the tutorial is to open your GeoServer map preview, click on the *options* button at the top of the map preview, in order to open the advanced options toolbar, and enter the filter in the CQL box.

.. figure:: gettingStarted.png
   :align: center
   
   *topp:states preview with advanced toolbar open.*
   
The attributes we'll be using in the filters are those included in the layer itself.
This is an example of attribute names and values for the state of Colorado:

.. list-table::
   
  * - **Attribute**
    - **states.6**
  * - STATE_NAME
    - Colorado
  * - STATE_FIPS
    - 08
  * - SUB_REGION
    - Mtn
  * - STATE_ABBR
    - CO
  * - LAND_KM
    - 268659.501
  * - WATER_KM
    - 960.364
  * - PERSONS
    - 3294394.0
  * - FAMILIES
    - 854214.0
  * - HOUSHOLD
    - 1282489.0
  * - MALE
    - 1631295.0
  * - FEMALE
    - 1663099.0
  * - WORKERS
    - 1233023.0
  * - DRVALONE
    - 1216639.0
  * - CARPOOL
    - 210274.0
  * - PUBTRANS
    - 46983.0
  * - EMPLOYED
    - 1633281.0
  * - UNEMPLOY
    - 99438.0
  * - SERVICE
    - 421079.0
  * - MANUAL
    - 181760.0
  * - P_MALE
    - 0.495
  * - P_FEMALE
    - 0.505
  * - SAMP_POP
    - 512677.0 
    

Simple comparisons
----------------------
   
Let's get started with the simplest example. In CQL basic arithmetic and comparisons 
do look exactly like plain text. The filter ``PERSONS > 15000000`` will extract only states that do
have more than 15 million inhabitants:

.. figure:: more15M.png
   :align: center
   
   *PERSONS > 15000000*
   
To check a range of values a between filter can be used instead: ``PERSONS BETWEEN 1000000 AND 3000000``:

.. figure:: between.png
   :align: center
  
   *PERSONS BETWEEN 1000000 AND 3000000*
   
Comparing with text is similar. In order to get only the state of California, the filter will be
``STATE_NAME = 'California'``. More complex text comparisons are available using ``LIKE`` comparisons. ``STATE_NAME LIKE 'N%'`` will extract all states starting with an ``N``.

.. figure:: startn.png
   :align: center
   
   *STATE_NAME LIKE 'N%'*
   
It is also possible to compare two attributes with each other. ``MALE > FEMALE`` selects the
states in which the male population surpasses the female one (a rare occurrence):

.. figure:: malefemale.png
   :align: center
   
   *MALE > FEMALE*
   
It is also possible to make simple math expressions using the ``+, -, *, /`` operators.
The following filter ``UNEMPLOY / (EMPLOYED + UNEMPLOY) > 0.07`` selects all states whose unemployment ratio is above 7% (remember the sample data is very old, don't draw any conclusion from the results)

.. figure:: employ.png
   :align: center
   
   *UNEMPLOY / (EMPLOYED + UNEMPLOY) > 0.07*
   
Id and list comparisons
-----------------------
   
If we want to extract only the states with a certain feature id we'll use the ``IN`` filter without specifying any attribute, as in ``IN ('states.1', 'states.12')``:

.. figure:: idfilter.png
   :align: center
   
   *IN ('states.1', 'states.12')*

If instead we want to extract the states whose name is in a given list we can use the ``IN`` filter specifying an attribute name, like in ``STATE_NAME IN ('New York', 'California', 'Montana', 'Texas')``:

.. figure:: statenames.png
   :align: center
   
   *STATE_NAME IN ('New York', 'California', 'Montana', 'Texas')*

Calling filter functions
------------------------

CQL/ECQL can call any of the :ref:`filter functions <filter_function_reference>` available in GeoServer.

For example, say we want to find all states whose name contains an "m", regardless of wheter it's a capital one, or not. We can call the ``strToLowerCase`` to turn all the state names to lowercase and then use a like comparison: ``strToLowerCase(STATE_NAME) like '%m%'``:

.. figure:: mstates.png
   :align: center
   
   *strToLowerCase(STATE_NAME) like '%m%'*

   
Geometric filters
------------------
CQL provides a full set of geometric filter capabilities. Say, for example, you want to display only the states that do cross the (-90,40,-60,45) bounding box.
The filter will be ``BBOX(the_geom, -90, 40, -60, 45)``

.. figure:: bbox.png
   :align: center
   
   *BBOX(the_geom, -90, 40, -60, 45)*
   
Conversely we can filter out all of the states that are overlapping that bounding box with the following filter ``DISJOINT(the_geom, POLYGON((-90 40, -90 45, -60 45, -60 40, -90 40)))``:

.. figure:: disjoint.png
   :align: center
   
   *DISJOINT(the_geom, POLYGON((-90 40, -90 45, -60 45, -60 40, -90 40)))*
