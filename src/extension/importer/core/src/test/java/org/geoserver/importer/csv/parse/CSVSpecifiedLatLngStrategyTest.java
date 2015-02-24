/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Point;

public class CSVSpecifiedLatLngStrategyTest {

    @Test
    public void testBuildFeatureType() {
        String input = CSVTestStrategySupport.buildInputString("quux,morx\n");
        CSVFileState fileState = new CSVFileState(input, "foo");
        CSVStrategy strategy = new CSVSpecifiedLatLngStrategy(fileState, "quux", "morx");
        SimpleFeatureType featureType = strategy.getFeatureType();

        assertEquals("Invalid attribute count", 1, featureType.getAttributeCount());
        assertEquals("Invalid featuretype name", "foo", featureType.getName().getLocalPart());
        assertEquals("Invalid name", "foo", featureType.getTypeName());

        GeometryDescriptor geometryDescriptor = featureType.getGeometryDescriptor();
        assertEquals("Invalid geometry attribute name", "location",
                geometryDescriptor.getLocalName());
    }

    @Test
    public void testCreateFeature() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("fleem,zoo,morx", "3,4,car",
                "8,9.9,cdr");
        CSVFileState fileState = new CSVFileState(input, "bar");
        CSVStrategy strategy = new CSVSpecifiedLatLngStrategy(fileState, "fleem", "zoo");

        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 2, featureType.getAttributeCount());
        List<AttributeDescriptor> attrs = featureType.getAttributeDescriptors();
        CSVTestStrategySupport.verifyType(attrs.get(0), String.class);
        CSVTestStrategySupport.verifyType(attrs.get(1), Point.class);

        CSVIterator iterator = strategy.iterator();

        assertTrue("next value not read", iterator.hasNext());
        SimpleFeature feature = iterator.next();
        assertNull("Unexpected property", feature.getAttribute("fleem"));
        assertNull("Unexpected property", feature.getAttribute("zoo"));
        assertEquals("Invalid feature property", "car", feature.getAttribute("morx"));
        assertNotNull("Missing geometry", feature.getDefaultGeometry());

        assertTrue("next value not read", iterator.hasNext());
        feature = iterator.next();
        assertNull("Unexpected property", feature.getAttribute("fleem"));
        assertNull("Unexpected property", feature.getAttribute("zoo"));
        assertEquals("Invalid feature property", "cdr", feature.getAttribute("morx"));
        assertNotNull("Missing geometry", feature.getDefaultGeometry());
        assertFalse("extra next value", iterator.hasNext());

        try {
            iterator.next();
            fail("NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testCreateSpecifiedLatLngColumnsDontExist() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("blub", "fubar");
        CSVFileState fileState = new CSVFileState(input, "zul");
        CSVStrategy strategy = new CSVSpecifiedLatLngStrategy(fileState, "non", "existing");
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 1, featureType.getAttributeCount());
        CSVIterator iterator = strategy.iterator();
        assertTrue("next value not read", iterator.hasNext());
        SimpleFeature feature = iterator.next();
        assertNull("Unexpected geometry", feature.getDefaultGeometry());
    }

    @Test
    public void testCreateSpecifiedLatLngColumnsNotNumeric() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("foo,bar", "3.8,quux");
        CSVFileState fileState = new CSVFileState(input, "zul");
        CSVStrategy strategy = new CSVSpecifiedLatLngStrategy(fileState, "foo", "bar");
        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 2, featureType.getAttributeCount());
        CSVIterator iterator = strategy.iterator();
        assertTrue("next value not read", iterator.hasNext());
        SimpleFeature feature = iterator.next();
        assertNull("Unexpected geometry", feature.getDefaultGeometry());
    }

}
