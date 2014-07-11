package org.geoserver.rest.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.junit.Before;
import org.junit.Test;

public class MapJSONFormatTest {

    private MapJSONFormat mapJSONFormat;

    @Before
    public void setUp() {
        mapJSONFormat = new MapJSONFormat();
    }

    @Test
    public void testBooleanResultTrue() {
        Object jsonObject = mapJSONFormat.toJSONObject(true);
        assertTrue(jsonObject instanceof Boolean);
        Boolean b = (Boolean) jsonObject;
        assertTrue(b);
    }

    @Test
    public void testBooleanResultFalse() {
        Object jsonObject = mapJSONFormat.toJSONObject(false);
        assertTrue(jsonObject instanceof Boolean);
        Boolean b = (Boolean) jsonObject;
        assertFalse(b);
    }

    @Test
    public void testNullResult() {
        Object jsonObject = mapJSONFormat.toJSONObject(null);
        assertTrue(jsonObject instanceof JSONNull);
    }

    @Test
    public void testArray() {
        Object jsonObject = mapJSONFormat.toJSONObject(Arrays.asList(1, 2, 3));
        assertTrue(jsonObject instanceof JSONArray);
        JSONArray jsonArray = (JSONArray) jsonObject;
        assertEquals(1, jsonArray.getInt(0));
        assertEquals(2, jsonArray.getInt(1));
        assertEquals(3, jsonArray.getInt(2));
    }

    @Test
    public void testMap() {
        Object jsonObject = mapJSONFormat.toJSONObject(Collections.singletonMap("foo", "bar"));
        assertTrue(jsonObject instanceof JSONObject);
        JSONObject json = (JSONObject) jsonObject;
        assertEquals("bar", json.get("foo"));
    }

    @Test
    public void testMapWithNullValue() {
        Object jsonObject = mapJSONFormat.toJSONObject(Collections.singletonMap("foo", null));
        assertTrue(jsonObject instanceof JSONObject);
        JSONObject json = (JSONObject) jsonObject;
        Object jsonNullObj = json.get("foo");
        assertTrue(jsonNullObj instanceof JSONObject);
        JSONObject jsonNull = (JSONObject) jsonNullObj;
        assertTrue(jsonNull.isNullObject());
    }

    @Test
    public void testNested() {
        Map<String, Object> input = new HashMap<String, Object>();

        List<Object> list = Arrays.<Object> asList("quux", true, 7, null);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("fleem", "morx");
        map.put("list", list);

        input.put("map", map);

        Object jsonObject = mapJSONFormat.toJSONObject(input);
        assertTrue(jsonObject instanceof JSONObject);
        JSONObject json = (JSONObject) jsonObject;

        Object nestedMapObj = json.get("map");
        assertTrue(nestedMapObj instanceof JSONObject);
        JSONObject nestedJson = (JSONObject) nestedMapObj;

        assertEquals("morx", nestedJson.getString("fleem"));
        Object listObj = nestedJson.get("list");
        assertTrue(listObj instanceof JSONArray);
        JSONArray arrayList = (JSONArray) listObj;
        assertEquals("quux", arrayList.getString(0));
        assertTrue(arrayList.getBoolean(1));
        assertEquals(7, arrayList.getInt(2));
        Object nullObj = arrayList.get(3);
        JSONObject jsonNull = (JSONObject) nullObj;
        assertTrue(jsonNull.isNullObject());
    }
}
