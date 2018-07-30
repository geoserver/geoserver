/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.model.geometry.Geometry;
import com.boundlessgeo.gsr.model.geometry.Point;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKID;

public class FeatureSchemaTest extends JsonSchemaTest {

    public FeatureSchemaTest() {
        super();
    }

    @Test
    public void testFeatureJsonSchema() throws Exception {
        Geometry geometry = new Point(-118.5, 33.80, new SpatialReferenceWKID(4326));
        List<Attribute> attr = new ArrayList<>();
        AttributeList attributes = new AttributeList(attr);
        //TODO: for some reason this throws an exception in the JSON validator. Investigate
        attributes.add(new Attribute("OWNER", "Joe Smith"));
        attributes.add(new Attribute("VALUE", 94820.37));
        attributes.add(new Attribute("APPROVED", true));
        attributes.add(new Attribute("LASTUPDATE", 1227663551096L)); // Date encoded as milliseconds since epoch
        Feature feature = new Feature(geometry, null);
        String json = getJson(feature);
        //System.out.println(json);
        assertTrue(validateJSON(json, "gsr/1.0/feature.json"));
    }

    @Test
    public void testFeatureIdSetJsonSchema() throws Exception {
        String objectIdFieldName = "objectid";
        long[] ids = { 1, 2, 3, 4, 5, 6, 7 };
        FeatureIdSet fIdSet = new FeatureIdSet(objectIdFieldName, ids);
        String json = getJson(fIdSet);
        assertTrue(validateJSON(json, "gsr/1.0/featureIdSet.json"));
    }

    @Test
    public void testFieldJsonSchema() throws Exception {
        Field field = new Field("magnitude", FieldTypeEnum.STRING, "Magnitude");
        String json = getJson(field);
        assertTrue(validateJSON(json, "gsr/1.0/field.json"));
    }

    @Test
    public void testStringFieldJsonSchema() throws Exception {
        Field field = new Field("name", FieldTypeEnum.STRING, "Name", 30);
        String json = getJson(field);
        assertTrue(validateJSON(json, "gsr/1.0/field.json"));
    }
}
