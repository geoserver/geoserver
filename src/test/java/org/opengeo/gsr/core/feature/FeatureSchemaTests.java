package org.opengeo.gsr.core.feature;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.opengeo.gsr.core.geometry.Geometry;
import org.opengeo.gsr.core.geometry.Point;
import org.opengeo.gsr.core.geometry.SpatialReferenceWKID;
import org.opengeo.gsr.validation.JSONSchemaUtils;

public class FeatureSchemaTests extends JSONSchemaUtils {

    ObjectMapper mapper;
    
    public FeatureSchemaTests() {
        mapper = new ObjectMapper();
    }
    
    @Test
    public void testFeatureJsonSchema() throws Exception {
        Geometry geometry = new Point(-118.5, 33.80, new SpatialReferenceWKID(4326));
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("OWNER", "Joe Smith");
        attributes.put("VALUE", 94820.37);
        attributes.put("APPROVED", true);
        attributes.put("LASTUPDATE", 1227663551096L); // Date encoded as milliseconds since epoch
        Feature feature = new Feature(geometry, attributes);
        String json = mapper.writeValueAsString(feature);
        assertTrue(validateJSON(json, "gsr/1.0/feature.json"));
    }

    @Test
    public void testFeatureIdSetJsonSchema() throws Exception {
        String objectIdFieldName = "objectid";
        int[] ids = {1,2,3,4,5,6,7};
        FeatureIdSet fIdSet = new FeatureIdSet(objectIdFieldName, ids);
        String json = mapper.writeValueAsString(fIdSet);
        assertTrue(validateJSON(json, "gsr/1.0/featureIdSet.json"));
    }
    
    @Test
    public void testFieldJsonSchema() throws Exception {
        Field field = new Field("magnitude", FieldTypeEnum.STRING, "Magnitude");
        String json = mapper.writeValueAsString(field);
        assertTrue(validateJSON(json, "gsr/1.0/field.json"));
    }
    
    @Test
    public void testStringFieldJsonSchema() throws Exception {
        Field field = new StringField("name", FieldTypeEnum.STRING, "Name", 30);
        String json = mapper.writeValueAsString(field);
        assertTrue(validateJSON(json, "gsr/1.0/field.json"));
    }
}
