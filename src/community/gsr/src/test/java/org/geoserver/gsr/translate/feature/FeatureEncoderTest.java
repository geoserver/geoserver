/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.translate.feature;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.geoserver.gsr.api.GeoServicesJacksonJsonConverter;
import org.geoserver.gsr.api.ServiceException;
import org.geoserver.gsr.model.feature.Feature;
import org.geoserver.gsr.model.geometry.SpatialReferenceWKID;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class FeatureEncoderTest extends GeoServerSystemTestSupport {

    protected static final ObjectMapper mapper = new GeoServicesJacksonJsonConverter().getMapper();

    @Test
    public void testCreateFeature() throws IOException, ServiceException, SchemaException {
        final SimpleFeatureType TYPE =
                DataUtilities.createType(
                        "Location",
                        "the_geom:Point:srid=4326,"
                                + // <- the geometry attribute: Point type
                                "id:String,"
                                + // <- a String attribute
                                "intfield:Integer,"
                                + // <- a Integer attribute
                                "doublefield:Double");
        // <- a Double attribute
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);

        Point point = geometryFactory.createPoint(new Coordinate(-112.6653, 34.54645645));
        featureBuilder.add(point);
        featureBuilder.add("t0002");
        featureBuilder.add(2);
        featureBuilder.add(2.54);
        SimpleFeature feature = featureBuilder.buildFeature(null);

        Feature encodedFeature =
                FeatureEncoder.feature(feature, true, new SpatialReferenceWKID(32615), "id");
        String featureJson = mapper.writeValueAsString(encodedFeature);

        // Check the JSON to ensure the number attributes for the feature
        // aren't encoded as strings
        assertTrue(featureJson.contains("\"intfield\":2"));
        assertTrue(featureJson.contains("\"doublefield\":2.54"));
    }
}
