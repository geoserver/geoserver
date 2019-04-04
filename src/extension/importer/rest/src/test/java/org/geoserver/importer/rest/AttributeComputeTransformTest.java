/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.util.Date;
import org.geoserver.importer.transform.AttributeComputeTransform;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

public class AttributeComputeTransformTest extends TransformTestSupport {

    public void testTransformLiteral() throws Exception {
        AttributeComputeTransform tx =
                new AttributeComputeTransform("theDate", Date.class, "2012-05-03T12:00:00Z");

        // reference
        // riverType = DataUtilities.createType(namespace+".river",
        // "id:0,geom:MultiLineString,river:String,flow:0.0");

        // transforming type
        SimpleFeatureType transformedType = tx.apply(null, null, riverType);
        AttributeDescriptor ad = transformedType.getDescriptor("theDate");
        assertNotNull(ad);
        assertEquals(Date.class, ad.getType().getBinding());

        // transforming feature
        SimpleFeature riverFeature = riverFeatures[0];
        SimpleFeature targetFeature =
                SimpleFeatureBuilder.build(transformedType, riverFeature.getAttributes(), "theId");
        SimpleFeature transformed = tx.apply(null, null, riverFeature, targetFeature);
        assertEquals(
                Converters.convert("2012-05-03T12:00:00Z", Date.class),
                transformed.getAttribute("theDate"));
    }

    public void testTransformExpression() throws Exception {
        AttributeComputeTransform tx =
                new AttributeComputeTransform("flowSquared", Double.class, "flow * flow");

        // reference
        // riverType = DataUtilities.createType(namespace+".river",
        // "id:0,geom:MultiLineString,river:String,flow:0.0");

        // transforming type
        SimpleFeatureType transformedType = tx.apply(null, null, riverType);
        AttributeDescriptor ad = transformedType.getDescriptor("flowSquared");
        assertNotNull(ad);
        assertEquals(Double.class, ad.getType().getBinding());

        // transforming feature
        SimpleFeature riverFeature = riverFeatures[0];
        SimpleFeature targetFeature =
                SimpleFeatureBuilder.build(transformedType, riverFeature.getAttributes(), "theId");
        SimpleFeature transformed = tx.apply(null, null, riverFeature, targetFeature);
        Double flow = (Double) riverFeature.getAttribute("flow");
        assertEquals(flow * flow, (Double) transformed.getAttribute("flowSquared"), 0d);
    }

    public void testAddExisting() throws Exception {
        AttributeComputeTransform tx = new AttributeComputeTransform("flow", Double.class, "123");

        // reference
        // riverType = DataUtilities.createType(namespace+".river",
        // "id:0,geom:MultiLineString,river:String,flow:0.0");

        // transforming type
        try {
            SimpleFeatureType transformedType = tx.apply(null, null, riverType);
            fail("Should have thrown an exception, flow is already there");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("flow"));
        }
    }

    public void testJSON() throws Exception {
        doJSONTest(new AttributeComputeTransform("flowSquared", Double.class, "flow * flow"));
        doJSONTest(new AttributeComputeTransform("theDate", Date.class, "2012-05-03T12:00:00Z"));
    }
}
