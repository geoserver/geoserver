/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

@Ignore // needs a DGGS store with time support
public class DAPATest extends DGGSTestSupport {

    @Test
    public void testGetVariablesJSON() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/dggs/v1/collections/H3/dapa/variables", 200);

        // check one field
        assertEquals(
                "Field of type String",
                readSingle(doc, "variables[?(@.id == 'shape')].description"));
        // self link
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/collections/H3/dapa/variables?f=application%2Fjson",
                readSingle(doc, "links[?(@.rel=='self')].href"));
        // collection link
        assertEquals(
                "http://localhost:8080/geoserver/ogc/dggs/v1/collections/H3?f=application%2Fjson",
                readSingle(
                        doc, "links[?(@.rel=='collection' && @.type == 'application/json')].href"));
    }

    @Test
    public void testH3GetMaxSpaceTime() throws Exception {
        // uses a fake datetime, h3 does not really have it, right now there is no test store
        // that has time information
        DocumentContext doc =
                getAsJSONPath(
                        "ogc/dggs/v1/collections/H3/dapa/area:aggregate-space-time?bbox=-10,-10,10,"
                                + "10&datetime=2000/2020&functions=min,max&variables=shape",
                        200);
        // geometry is the aggregation one
        JSONArray coordinates = doc.read("features[0].geometry.coordinates", JSONArray.class);
        assertEquals(
                "[[[-10,-10],[10,-10],[10,10],[-10,10],[-10,-10]]]", coordinates.toJSONString());
        // check properties
        JSONObject properties = doc.read("features[0].properties", JSONObject.class);
        // aggregation time
        assertEquals("2000/2020", properties.getAsString("phenomenonTime"));
        // the actual results
        assertEquals("hexagon", properties.getAsString("shape_min"));
        assertEquals("pentagon", properties.getAsString("shape_max"));
    }
}
