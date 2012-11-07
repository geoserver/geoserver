package org.opengeo.gsr.core.feature;

import java.io.IOException;

import net.sf.json.util.JSONBuilder;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengeo.gsr.core.geometry.GeometryEncoder;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;

public class FeatureEncoder {
    private FeatureEncoder() {
        throw new RuntimeException("Feature encoder has only static methods, no need to instantiate it.");
    }
    
    public static
    <T extends FeatureType, F extends org.opengis.feature.Feature>
    void featuresToJson(FeatureCollection<T, F> collection, JSONBuilder json)
    throws IOException
    {
        FeatureIterator<F> iterator = collection.features();
        try {
            json.array();
            while (iterator.hasNext()) {
                F feature = iterator.next();
                featureToJson(feature, json);
            }
            json.endArray();
        } finally {
            iterator.close();
        }
    }
    
    public static void featureToJson(org.opengis.feature.Feature feature, JSONBuilder json) {
        GeometryAttribute geometry = feature.getDefaultGeometryProperty();
        json.object();
        json.key("geometry");
        GeometryEncoder.toJson((com.vividsolutions.jts.geom.Geometry)geometry.getValue(), json);
        json.key("attributes");
        json.object();
        
        for (Property prop : feature.getProperties()) {
            if (!prop.getName().equals(geometry.getName())) {
                json.key(prop.getName().getLocalPart()).value(prop.getValue());
            }
        }
        
        json.endObject();
        json.endObject();
    }
}
