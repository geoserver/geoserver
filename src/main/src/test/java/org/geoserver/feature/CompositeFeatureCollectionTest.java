/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.DataTestCase;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;

public class CompositeFeatureCollectionTest extends DataTestCase {

    @Test
    public void testCompositeEmpty() {
        CompositeFeatureCollection<SimpleFeatureType, SimpleFeature> fc =
                new CompositeFeatureCollection<>(new ArrayList<>());
        assertEquals(0, fc.size());
        assertNull(fc.getBounds());
        assertNull(DataUtilities.first(fc));
    }

    @Test
    public void testComposeOne() {
        CompositeFeatureCollection<SimpleFeatureType, SimpleFeature> fc =
                new CompositeFeatureCollection<>(Arrays.asList(DataUtilities.collection(riverFeatures[0])));
        assertEquals(1, fc.size());
        assertEquals(new ReferencedEnvelope(5, 13, 3, 7, riverType.getCoordinateReferenceSystem()), fc.getBounds());
        assertEquals(riverFeatures[0], DataUtilities.first(fc));
    }

    @Test
    public void testComposeMany() {
        SimpleFeatureCollection roads = DataUtilities.collection(roadFeatures);
        SimpleFeatureCollection rivers = DataUtilities.collection(riverFeatures);
        SimpleFeatureCollection lakes = DataUtilities.collection(lakeFeatures);
        CompositeFeatureCollection<SimpleFeatureType, SimpleFeature> fc =
                new CompositeFeatureCollection<>(Arrays.asList(roads, rivers, lakes));
        // 3 roads, 2 rivers, 1 lake
        assertEquals(6, fc.size());
        assertEquals(new ReferencedEnvelope(1, 16, 0, 10, riverType.getCoordinateReferenceSystem()), fc.getBounds());
        assertEquals(roadFeatures[0], DataUtilities.first(fc));
    }
}
