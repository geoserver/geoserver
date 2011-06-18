var geotools = Packages.org.geotools;

metadata = {
    title: "Centroid Example",
    description: "Example script that calculates the centroids of the input geometries",
    inputs: {
        features: geotools.feature.FeatureCollection
    },
    outputs: {
        centroid: geotools.feature.FeatureCollection
    }
}

function process(input) {
    var schema = rewriteSchema(input.features.getSchema());
    var collection =
         new geotools.feature.DefaultFeatureCollection(null, schema);

    var it = input.features.iterator();
    var builder = new geotools.feature.simple.SimpleFeatureBuilder(schema);

    while (it.hasNext()) {
        var feature = it.next();
        builder.init(feature);
        feature = builder.buildFeature(null);

        feature.setDefaultGeometry(feature.getDefaultGeometry().getCentroid());
        collection.add(feature);
    }

    input.features.close(it);


    return {
        centroid: collection
    };
}

function rewriteSchema(schema) {
    var builder = new geotools.feature.simple.SimpleFeatureTypeBuilder();
    var add = 
        builder["add(java.lang.String,java.lang.Class,org.opengis.referencing.crs.CoordinateReferenceSystem)"]; 
    // specify a method signature so that Rhino won't get confused if the CRS is null

    var attributes = schema.getAttributeDescriptors();
    var defaultGeometry = schema.getGeometryDescriptor();

    for (var i = 0, len = attributes.size(); i < len; i++) {
        var attr = attributes.get(i);
        if (attr === defaultGeometry) {
            add.call(
                builder,
                attr.getName().getLocalPart(),
                Packages.com.vividsolutions.jts.geom.Point,
                defaultGeometry.getCoordinateReferenceSystem()
            );
        } else {
            builder.add(attr);
        }
    }

    builder.setName(schema.getName());

    return builder.buildFeatureType();
}
