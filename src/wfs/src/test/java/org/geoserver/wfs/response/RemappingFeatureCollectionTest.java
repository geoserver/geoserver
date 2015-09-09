/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Point;

public class RemappingFeatureCollectionTest {

    @Test
    public void testPreserveRestrictions() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        tb.add("geom", Point.class);
        ab.setName("name");
        ab.setBinding(String.class);
        ab.setLength(20);
        tb.add(ab.buildDescriptor("name"));
        tb.setName("testType");
        SimpleFeatureType original = tb.buildFeatureType();
        int length = FeatureTypes.getFieldLength(original.getDescriptor("name"));
        assertEquals(20, length);

        ListFeatureCollection fc = new ListFeatureCollection(original);
        RemappingFeatureCollection remapped = new RemappingFeatureCollection(fc,
                Collections.singletonMap("name", "xyz"));
        int remappedLength = FeatureTypes.getFieldLength(remapped.getSchema().getDescriptor("xyz"));
        assertEquals(20, remappedLength);
    }

}
