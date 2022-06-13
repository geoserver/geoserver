/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.junit.Test;

public class ComplexMetaDataMapTest {

    @Test
    public void testSimpleAttributes() {
        MetadataMap underlying = createMap();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        assertEquals(0, map.size("field-doesntexist"));

        // single-valued

        assertEquals(1, map.size("field-single"));

        ComplexMetadataAttribute<String> att = map.get(String.class, "field-single");

        assertEquals("single value string", att.getValue());
        att.setValue("alteredValue");
        assertEquals("alteredValue", underlying.get("field-single"));

        map.delete("field-single");

        assertEquals(0, map.size("field-single"));
        assertNull(underlying.get("field-single"));

        // multi-valued

        assertEquals(2, map.size("field-as-list"));

        att = map.get(String.class, "field-as-list", 1);
        assertEquals("field list value 2", att.getValue());
        att.setValue("alteredListValue");
        assertEquals("alteredListValue", ((ArrayList<?>) underlying.get("field-as-list")).get(1));

        map.delete("field-as-list", 0);

        assertEquals(1, map.size("field-as-list"));

        assertEquals("alteredListValue", att.getValue());
        assertEquals("alteredListValue", ((ArrayList<?>) underlying.get("field-as-list")).get(0));
        att.setValue("alteredListValue2");
        assertEquals("alteredListValue2", ((ArrayList<?>) underlying.get("field-as-list")).get(0));

        // create new multi value
        assertEquals(0, map.size("ohter-as-list"));

        att = map.get(String.class, "ohter-as-list", 1);
        att.setValue("insert-new-value");

        assertEquals(2, map.size("ohter-as-list"));

        att = map.get(String.class, "ohter-as-list", 1);
        assertEquals("insert-new-value", att.getValue());

        att = map.get(String.class, "ohter-as-list", 0);
        assertEquals(null, att.getValue());
    }

    @Test
    public void testSingleComplex() {
        MetadataMap underlying = createMap();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        ComplexMetadataMap subMap = map.subMap("object-field");

        assertEquals(1, subMap.size("field1"));
        assertEquals(1, subMap.size("field2"));
        assertEquals(2, subMap.size("field3"));

        ComplexMetadataAttribute<String> att = subMap.get(String.class, "field1");
        assertEquals("object field 01", att.getValue());
        att.setValue("alteredValue");
        assertEquals("alteredValue", underlying.get("object-field/field1"));

        att = subMap.get(String.class, "field3", 1);
        assertEquals("object field list value 2", att.getValue());
        att.setValue("alteredListValue");
        assertEquals(
                "alteredListValue", ((ArrayList<?>) underlying.get("object-field/field3")).get(1));

        subMap.delete("field3", 0);

        assertEquals(1, subMap.size("field3"));

        assertEquals("alteredListValue", att.getValue());
        assertEquals(
                "alteredListValue", ((ArrayList<?>) underlying.get("object-field/field3")).get(0));
        att.setValue("alteredListValue2");
        assertEquals(
                "alteredListValue2", ((ArrayList<?>) underlying.get("object-field/field3")).get(0));
    }

    @Test
    public void testMultiComplex() {
        MetadataMap underlying = createMap();
        ComplexMetadataMap map = new ComplexMetadataMapImpl(underlying);

        ComplexMetadataMap subMap = map.subMap("object-as-list", 1);

        ComplexMetadataAttribute<String> att1 = subMap.get(String.class, "field 01");
        assertEquals("object list value 2", att1.getValue());
        att1.setValue("alteredValue");
        assertEquals(
                "alteredValue", ((ArrayList<?>) underlying.get("object-as-list/field 01")).get(1));

        ComplexMetadataAttribute<String> att2 = subMap.get(String.class, "field 02", 1);
        assertEquals("object list value other 2.2", att2.getValue());
        att2.setValue("alteredOtherValue");
        assertEquals(
                "alteredOtherValue",
                ((ArrayList<?>) ((ArrayList<?>) underlying.get("object-as-list/field 02")).get(1))
                        .get(1));

        ComplexMetadataAttribute<String> att3 = subMap.get(String.class, "field 03");
        assertNull(att3.getValue());
        att3.setValue("alteredYetOtherValue");
        assertEquals(
                "alteredYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 03")).get(1));

        ComplexMetadataAttribute<String> att4 = subMap.get(String.class, "field 04");
        assertEquals("object single value", att4.getValue());
        att4.setValue("alteredYetYetOtherValue");
        assertEquals(
                "alteredYetYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 04")).get(1));
        assertEquals(
                "object single value",
                ((ArrayList<?>) underlying.get("object-as-list/field 04")).get(0));

        map.delete("object-as-list", 0);

        assertEquals("alteredValue", subMap.get(String.class, "field 01").getValue());
        assertEquals("alteredValue", att1.getValue());
        assertEquals(
                "alteredValue", ((ArrayList<?>) underlying.get("object-as-list/field 01")).get(0));
        assertEquals("alteredOtherValue", att2.getValue());
        assertEquals(
                "alteredOtherValue",
                ((ArrayList<?>) ((ArrayList<?>) underlying.get("object-as-list/field 02")).get(0))
                        .get(1));
        assertEquals("alteredYetOtherValue", att3.getValue());
        assertEquals(
                "alteredYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 03")).get(0));
        assertEquals("alteredYetYetOtherValue", att4.getValue());
        assertEquals(
                "alteredYetYetOtherValue",
                ((ArrayList<?>) underlying.get("object-as-list/field 04")).get(0));
    }

    private MetadataMap createMap() {
        MetadataMap map = new MetadataMap();
        // String
        map.put("field-single", "single value string");

        // list String
        ArrayList<Object> fieldAsList = new ArrayList<>();
        fieldAsList.add("field list value 1");
        fieldAsList.add("field list value 2");
        map.put("field-as-list", fieldAsList);

        // Object
        map.put("object-field/field1", "object field 01");
        map.put("object-field/field2", "object field 02");
        fieldAsList = new ArrayList<>();
        fieldAsList.add("object field list value 1");
        fieldAsList.add("object field list value 2");
        map.put("object-field/field3", fieldAsList);

        // String per object
        ArrayList<Object> fieldAsListObjectValue01 = new ArrayList<>();
        fieldAsListObjectValue01.add("object list value 1");
        fieldAsListObjectValue01.add("object list value 2");
        map.put("object-as-list/field 01", fieldAsListObjectValue01);

        // list per object
        ArrayList<Object> fieldAsListObjectValue02 = new ArrayList<>();
        ArrayList<Object> fieldAsListObjectValue0201 = new ArrayList<>();
        fieldAsListObjectValue0201.add("object list value other 1");
        ArrayList<Object> fieldAsListObjectValue0202 = new ArrayList<>();
        fieldAsListObjectValue0202.add("object list value other 2.1");
        fieldAsListObjectValue0202.add("object list value other 2.2");
        fieldAsListObjectValue02.add(fieldAsListObjectValue0201);
        fieldAsListObjectValue02.add(fieldAsListObjectValue0202);
        map.put("object-as-list/field 02", fieldAsListObjectValue02);

        ArrayList<Object> fieldAsListObjectValue03 = new ArrayList<>();
        fieldAsListObjectValue03.add("object incomplete list value");
        map.put("object-as-list/field 03", fieldAsListObjectValue03);

        map.put("object-as-list/field 04", "object single value");

        return map;
    }
}
