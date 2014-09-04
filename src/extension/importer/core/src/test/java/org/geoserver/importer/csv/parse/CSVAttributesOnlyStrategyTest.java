/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.csv.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.geoserver.importer.csv.CSVFileState;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

public class CSVAttributesOnlyStrategyTest {

    @Test
    public void testBuildFeatureType() {
        String input = CSVTestStrategySupport.buildInputString("quux,morx\n");
        CSVFileState fileState = new CSVFileState(input, "foo");
        CSVStrategy strategy = new CSVAttributesOnlyStrategy(fileState);
        SimpleFeatureType featureType = strategy.getFeatureType();

        assertEquals("Invalid attribute count", 2, featureType.getAttributeCount());
        assertEquals("Invalid featuretype name", "foo", featureType.getName().getLocalPart());
        assertEquals("Invalid name", "foo", featureType.getTypeName());

        List<AttributeDescriptor> attrs = featureType.getAttributeDescriptors();
        assertEquals("Invalid number of attributes", 2, attrs.size());
        List<String> attrNames = new ArrayList<String>(2);
        for (AttributeDescriptor attr : attrs) {
            if (!(attr instanceof GeometryDescriptor)) {
                attrNames.add(attr.getName().getLocalPart());
            }
        }
        Collections.sort(attrNames);
        assertEquals("Invalid property descriptor", "morx", attrNames.get(0));
        assertEquals("Invalid property descriptor", "quux", attrNames.get(1));
    }

    @Test
    public void testCreateFeature() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("fleem,zoo,morx", "3,4,car",
                "8,9.9,cdr");
        CSVFileState fileState = new CSVFileState(input, "bar");
        CSVStrategy strategy = new CSVAttributesOnlyStrategy(fileState);

        SimpleFeatureType featureType = strategy.getFeatureType();
        assertEquals("Invalid attribute count", 3, featureType.getAttributeCount());
        List<AttributeDescriptor> attrs = featureType.getAttributeDescriptors();
        CSVTestStrategySupport.verifyType(attrs.get(0), Integer.class);
        CSVTestStrategySupport.verifyType(attrs.get(1), Double.class);
        CSVTestStrategySupport.verifyType(attrs.get(2), String.class);

        CSVIterator iterator = strategy.iterator();

        assertTrue("next value not read", iterator.hasNext());
        SimpleFeature feature = iterator.next();
        assertEquals("Invalid feature property", 3, feature.getAttribute("fleem"));
        assertEquals("Invalid feature property", 4.0,
                Double.parseDouble(feature.getAttribute("zoo").toString()), 0.1);
        assertEquals("Invalid feature property", "car", feature.getAttribute("morx"));

        assertTrue("next value not read", iterator.hasNext());
        feature = iterator.next();
        assertEquals("Invalid feature property", 8, feature.getAttribute("fleem"));
        assertEquals("Invalid feature property", 9.9,
                Double.parseDouble(feature.getAttribute("zoo").toString()), 0.1);
        assertEquals("Invalid feature property", "cdr", feature.getAttribute("morx"));
        assertFalse("extra next value", iterator.hasNext());

        try {
            iterator.next();
            fail("NoSuchElementException should have been thrown");
        } catch (NoSuchElementException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testCreateNoGeometry() throws IOException {
        String input = CSVTestStrategySupport.buildInputString("blub", "fubar");
        CSVFileState fileState = new CSVFileState(input, "zul");
        CSVStrategy strategy = new CSVAttributesOnlyStrategy(fileState);
        CSVIterator iterator = strategy.iterator();
        assertTrue("next value not read", iterator.hasNext());
        SimpleFeature feature = iterator.next();
        Object defaultGeometry = feature.getDefaultGeometry();
        assertNull("Unexpected geometry", defaultGeometry);
    }
}
