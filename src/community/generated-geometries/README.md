Implementation details
----------------------

Module introduces a few extension points for the GeoServer's backend and frontend. First one:
 
	org.geoserver.catalog.RetypeFeatureTypeCallback
	
is applied in two contexts: 
- at the end of Feature Type creation / configuration process,
- for Feature Source overriding, if needed.
Both usages serves overriding feature types' and feature sources' definitions on the fly, providing particular callback is applicable for invocation parameters.
Typical usage scenario for it should be returning new objects (feature type or feature source) based on source values given on arguments or returning source objects themselves if callback should not modify the current flow. Callback should be transparent, i.e. not throwing any exception, just not to break flows where it can't be applicable.


Second extension is designed for configuring geometry attributes and generating geometry data:

	org.geoserver.generatedgeometries.core.GeometryGenerationStrategy<FT extends FeatureType, F extends Feature>
	
It's purpose is to modify feature type to add required geometry if it does not contain any, but other attributes can be used for geometry calculation. Type redefinition accurs in ``org.geoserver.generatedgeometries.core.GeometryGenerationStrategy.defineGeometryAttributeFor`` method. Once defined, geometry configuration will be stored in feature info metadata. 
Stored configuration will be read during actual geometry creating in the ``org.geoserver.generatedgeometries.core.GeometryGenerationStrategy.generateGeometry`` method.  
Packages ``org.geoserver.generatedgeometries.core.longitudelatitude`` and ``org.geoserver.generatedgeometries.web.longitudelatitude`` contains an example how long/lat points can be built on the fly.  


Last extension point is required on the frontend:

	org.geoserver.generatedgeometries.web.GeometryGenerationStrategyUIGenerator

It represents the UI configuration panel for particular geometry generation strategy.   


Activation
----------

Module is activated by 'generated-geometries' maven profile.
