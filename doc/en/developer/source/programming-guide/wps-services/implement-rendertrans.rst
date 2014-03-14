.. _wps_services_implement_rendertrans:

Implementing a Rendering Transformation
=======================================

**Rendering Transformations** are a special kind of WPS process
which run within the GeoServer WMS rendering pipeline
to transform data in order to provide more effective visualization.
This section describes how to implement a rendering transformation process in Java.

Rendering transformations are very general, and can transform both the content
and the format of input data.
Content transformation typically involves complex geospatial processing
which requires access to the entire dataset
(in contrast to *geometry transformations*, which operate on a single spatial feature at a time).
Format transformation converts from vector to raster
or vice-versa in order to produce an output 
format appropriate to the desired visualization
(for instance, a raster for displaying continuous surfaces, or vector data for displaying discrete objects).

For more information about the function and use of rendering transformations within GeoServer, refer to the 
*Rendering Transformations* section of the *GeoServer User Guide*.


Lifecycle of a Rendering Transformation 
---------------------------------------

To implement a rendering transformation it is useful to understand
their lifecycle and operation within GeoServer.  
A rendering transformation is invoked in an SLD by providing a ``<Transformation>`` element inside a  ``<FeatureTypeStyle>``.
This element specifies the name of the transformation process and the names and values of 
the process parameters. As an example, the following is a portion of an SLD which uses
the ``gs:Heatmap`` transformation:

.. code-block:: xml
   :linenos:

            <FeatureTypeStyle>
              <Transformation>
                <ogc:Function name="gs:Heatmap">
                  <ogc:Function name="parameter">
                    <ogc:Literal>data</ogc:Literal>
                  </ogc:Function>
                  <ogc:Function name="parameter">
                    <ogc:Literal>weightAttr</ogc:Literal>
                    <ogc:Literal>pop2000</ogc:Literal>
                  </ogc:Function>
                  <ogc:Function name="parameter">
                    <ogc:Literal>radiusPixels</ogc:Literal>
                    <ogc:Function name="env">
                      <ogc:Literal>radius</ogc:Literal>
                      <ogc:Literal>100</ogc:Literal>
                    </ogc:Function>
                  </ogc:Function>
                  <ogc:Function name="parameter">
                    <ogc:Literal>pixelsPerCell</ogc:Literal>
                    <ogc:Literal>10</ogc:Literal>
                  </ogc:Function>
                  <ogc:Function name="parameter">
                    <ogc:Literal>outputBBOX</ogc:Literal>
                    <ogc:Function name="env">
                      <ogc:Literal>wms_bbox</ogc:Literal>
                    </ogc:Function>
                  </ogc:Function>
                  <ogc:Function name="parameter">
                    <ogc:Literal>outputWidth</ogc:Literal>
                    <ogc:Function name="env">
                      <ogc:Literal>wms_width</ogc:Literal>
                    </ogc:Function>
                  </ogc:Function>
                  <ogc:Function name="parameter">
                    <ogc:Literal>outputHeight</ogc:Literal>
                    <ogc:Function name="env">
                      <ogc:Literal>wms_height</ogc:Literal>
                    </ogc:Function>
                  </ogc:Function>
                </ogc:Function>
              </Transformation>
              ...


During WMS requests which uses an SLD specifying a transformation, 
the arguments passed to the transformation process 
are assembled from the parameters and values specified in the SLD.
Some argument values may be determined dynamically from SLD variables
(as shown above in **lines 22-40**).

Before the transformation process is executed, it is given an opportunity to rewrite 
the query GeoServer makes to the source datastore, via
the optional ``invertQuery`` or ``invertGridGeometry`` methods.  
This allows a transformation to enlarge the query extent,
since some kinds of transformations may need to include data which
lies outside the original query window.

The query is then performed against the source datastore,
and the transformation process is executed against the resulting dataset.
The transformation returns a computed output dataset
of the same or different format.
If the output has a different coordinate system (CRS) than 
the requested map it is reprojected automatically.
Finally, the output dataset is passed on through the rendering pipeline
to be styled by the symbolizers defined in the SLD ``<FeatureTypeStyle>``.


Transformation process class
----------------------------

Like other WPS processes, rendering transformations are implememented as Java classes.
A process class implements the ``GSProcess`` marker interface,
and is registered with GeoServer via an ``applicationContext.xml`` file.
For further information about the basic steps for creating, building and deploying a GeoServer WPS process in Java
refer to the :ref:`wps_services_implementing` section.

WPS processes must provide metadata about themselves and their parameters.  
The easiest way to do this is to use the GeoTools annotation-based Process API,
which uses Java annotations to specify metadata.
For example, the code below shows the process metadata specified for the ``gs:Heatmap`` rendering transformation:

.. code-block:: java

   @DescribeProcess(title = "Heatmap", 
                description = "Computes a heatmap surface over a set of irregular data points as a GridCoverage.")
   public class HeatmapProcess implements GeoServerProcess {


GeoServer instantiates a **single instance** of each rendering transformation class.
This means that rendering transformation classes must be **stateless**,
since they may be called concurrently to service different requests.
This is ensured by avoiding declaring any instance variables within the class.
For complex transformations it may be desirable to implement an auxiliary class
to allow the use of instance variables.

execute method
--------------

Like all process classes, a rendering transformation class must declare an ``execute`` method, 
which is called by GeoServer to perform the transformation.
The signature of the ``execute`` method specifies the types of the input parameters 
and the process result.

The declaration of the ``execute`` method for the Heatmap transformation is:

.. code-block:: java

    @DescribeResult(name = "result", description = "The heat map surface as a raster")
    public GridCoverage2D execute(

      // tranformation input data
      @DescribeParameter(name = "data", description = "Features containing the data points") 
        SimpleFeatureCollection obsFeatures,

      // process parameters
      @DescribeParameter(name = "radiusPixels", 
                         description = "Radius to use for the kernel, in pixels") 
        Integer argRadiusPixels,
      @DescribeParameter(name = "weightAttr", 
                  description = "Featuretype attribute containing the point weight value", 
                         min = 0, max = 1) 
         String valueAttr,
      @DescribeParameter(name = "pixelsPerCell", 
                         description = "Number of pixels per grid cell (default = 1)", 
                         min = 0, max = 1) 
        Integer argPixelsPerCell,

      // output map parameters
      @DescribeParameter(name = "outputBBOX", 
                         description = "Georeferenced bounding box of the output") 
        ReferencedEnvelope argOutputEnv,
      @DescribeParameter(name = "outputWidth", description = "Width of the output raster") 
        Integer argOutputWidth,
      @DescribeParameter(name = "outputHeight", description = "Height of the output raster") 
        Integer argOutputHeight,

      ) throws ProcessException {
      ...



Input parameters
^^^^^^^^^^^^^^^^

The supported process input parameters are defined as parameters to the ``execute`` method.
The metadata for them is supplied via ``@DescribeParameter`` annotations.

To accept the input data to be transformed, the process must define one input parameter of type ``SimpleFeatureCollection`` or ``GridCoverage2D``.
In the invoking SLD only the name of this parameter is specified,
since GeoServer provides the dataset to be transformed as the parameter value.

Any number of other parameters can be defined.
Parameters can be mandatory or optional (optional parameters have a value of ``null`` if not present).
Lists of values can be accepted by defining an array-valued parameter.

Some transformations require information about the request map extent and coordinate system, and request image width and height.
Situations where these are required include:

* the transformation operation depends on the request resolution
* the transformation computes a raster result in the request coordinate system
  to ensure optimal visual quality 
  
These values can be obtained from SLD **predefined variables** and passed in via parameters of types 
``ReferencedEnvelope`` and ``Integer``.
(See the *Variable Substitution in SLD* section in the *User Guide* for details of all predefined variables available.)

In the case of the Heatmap transformation, the request resolution is used to determine the ground size of the
``radiusPixels`` parameter, and the output raster is computed in the request coordinate system
to avoid undesired reprojection.
To support this the transformation defines the required ``outputBBOX``, ``outputWidth`` and ``outputHeight`` parameters.
These are supplied by predefined SLD variables as shown in **lines 22-40** of the SLD snippet above.


Transformation output
^^^^^^^^^^^^^^^^^^^^^

The output of a transformation is a new dataset of type ``SimpleFeatureCollection`` or ``GridCoverage2D``.
This is specified as the return type of the ``execute`` method.
Name and description metadata is provided by the ``@DescribeResult`` annotation on the ``execute`` method.

If the output dataset is not in the coordinate system requested for map output, GeoServer 
reprojects it automatically.  
As noted in the previous section, there may be situations where it is desirable to avoid this. 
In this case the transformation must ensure that the output has the appropriate CRS.


Query rewriting
---------------

If required, the rendering transformation has the ability to alter the query made against the source dataset.
This allows expanding the extent of the data to be read, which is necessary for some kinds of 
transformations (in particular, ones whose result is determined by computing
over a spatial window around the input).  This also allows controlling query optimizations
(for instance, ensuring that geometry decimation does not
prevent point features from being read).

Query rewriting is performed by providing one of the methods ``invertQuery`` or ``invertGridGeometry``.

These methods have the general signature of::

  X invertX( [inputParam,]* Query targetQuery, GridGeometry targetGridGeometry)
  
The ``targetQuery`` parameter is the query constructed from the original request.

The ``targetGridGeometry`` parameter is the georeferenced extent of the requested output map.  
It is not used in the dataset query, but may be needed for use in conjunction with 
the transformation parameters to determine how to rewrite the query.
For instance, if a parameter is specified in output units, 
the output extent information is required to transform the value into units
applicable in the input CRS.

In addition, these methods can accept any number of the input parameters 
defined for the ``execute`` method.  
If defined these parameters must be annotated with
``@DescribeParameter`` in the same way as in the ``execute`` method.

invertQuery method
^^^^^^^^^^^^^^^^^^

This method is called when the rendering tranformation applies to vector data 
(the data input is of type ``SimpleFeatureCollection``).

The method returns a new ``Query`` value, which contains any required alterations of extent or query optimizations.
This is used to query the source dataset.

The Heatmap process implements the ``invertQuery`` method in order to enlarge the query extent
by the ground size corresponding to the ``radiusPixels`` parameter.
To allow converting the pixel size into a ground distance the input parameters providing the output map extents are also required.
The signature of the implemented method is:

.. code-block:: java

    public Query invertQuery(
            @DescribeParameter(name = "radiusPixels", 
                           description = "Radius to use for the kernel", min = 0, max = 1) 
              Integer argRadiusPixels,
            // output image parameters
            @DescribeParameter(name = "outputBBOX", 
                           description = "Georeferenced bounding box of the output") 
              ReferencedEnvelope argOutputEnv,
            @DescribeParameter(name = "outputWidth", 
                           description = "Width of the output raster") 
              Integer argOutputWidth,
            @DescribeParameter(name = "outputHeight", 
                           description = "Height of the output raster") 
              Integer argOutputHeight,

            Query targetQuery, GridGeometry targetGridGeometry
       ) throws ProcessException {
            ...




invertGridGeometry method
^^^^^^^^^^^^^^^^^^^^^^^^^

This method is called when the rendering tranformation applies to raster data 
(the data input is of type ``GridCoverage2D``).

The method returns a new ``GridGeometry`` value, 
which is used as the query extent against the source raster dataset.


Summary
-------

In summary, the key features of a rendering transformation process class are:

* There must be an input parameter which is a ``FeatureCollection`` or a ``GridCoverage2D``
* It may be useful to have input parameters which provide the request map extent and image dimensions
* There must be a single result of type ``FeatureCollection`` or ``GridCoverage2D``
* The optional ``invertQuery`` or ``invertGridGeometry`` methods may be supplied to rewrite the initial data query
* The transformation process class must be **stateless**





            

