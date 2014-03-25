/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;
import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class CSVSpecifiedWKTStrategyTest {

    @Test
    public void testBuildFeatureType() {
        String input = CSVTestStrategySupport.buildInputString("quux,morx\n");
        CSVFileState fileState = new CSVFileState(input, "foo");
        CSVStrategy strategy = new CSVSpecifiedWKTStrategy(fileState, "quux");
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 2, featureType.getAttributeCount());
        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertEquals("Invalid geometry attribute name", "quux", geometryDescriptor.getLocalName());
    }

    @Test
    public void testCreateFeature() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("fleem,zoo,morx",
                "foo,POINT(3.14 1.59),car");
        CSVFileState fileState = new CSVFileState(input, "bar");
        CSVStrategy strategy = new CSVSpecifiedWKTStrategy(fileState, "zoo");
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 3, featureType.getAttributeCount());
        CSVTestStrategySupport.verifyType(featureType.getDescriptor("fleem"), String.class);
        CSVTestStrategySupport.verifyType(featureType.getDescriptor("zoo"), Geometry.class);
        CSVTestStrategySupport.verifyType(featureType.getDescriptor("morx"), String.class);
        CSVIterator iterator = strategy.iterator();
        SimpleFeature feature = iterator.next();
        assertEquals("Invalid feature property", "foo", feature.getAttribute("fleem"));
        assertEquals("Invalid feature property", "car", feature.getAttribute("morx"));
        assertNotNull("Expected geometry", feature.getDefaultGeometry());
        Point point = (Point) feature.getAttribute("zoo");
        Coordinate coordinate = point.getCoordinate();
        assertEquals("Invalid x coordinate", coordinate.x, 3.14, 0.1);
        assertEquals("Invalid y coordinate", coordinate.y, 1.59, 0.1);
    }

    public void testCreateFeatureBadGeometry() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("fleem,morx", "foo,bar");
        CSVFileState fileState = new CSVFileState(input, "blub");
        CSVStrategy strategy = new CSVSpecifiedWKTStrategy(fileState, "fleem");
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 2, featureType.getAttributeCount());
        CSVTestStrategySupport.verifyType(featureType.getDescriptor("fleem"), Geometry.class);
        CSVTestStrategySupport.verifyType(featureType.getDescriptor("morx"), String.class);
        CSVIterator iterator = strategy.iterator();
        SimpleFeature feature = iterator.next();
        assertEquals("Invalid feature property", "bar", feature.getAttribute("morx"));
        assertNull("Unexpected geometry", feature.getAttribute("fleem"));
    }

}
