/*
 * (c) 2017 Open Source Geospatial Foundation - all rights reserved
 */
package org.geoserver.ows;

import static org.geoserver.data.test.CiteTestData.BASIC_POLYGONS;
import static org.geoserver.data.test.CiteTestData.POINTS;
import static org.junit.Assert.assertEquals;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class SimulateIntegrationTest extends GeoServerSystemTestSupport {

    @Test
    public void testGetFeature() throws Exception {
        JSONObject rsp =
                (JSONObject)
                        getAsJSON(
                                "wfs?"
                                        + String.join(
                                                "&",
                                                "service=wfs",
                                                "request=GetFeature",
                                                "version=1.0.0",
                                                "srsName=EPSG:4326",
                                                "bbox=-170,-80,170,80",
                                                "typename=" + getLayerId(POINTS),
                                                "simulate=true"));

        print(rsp);
        assertService(rsp, "wfs", "1.0.0");

        JSONObject req = rsp.getJSONObject("operation").getJSONObject("request");
        JSONArray queries = req.getJSONArray("query");

        assertEquals(1, queries.size());

        JSONObject query = queries.getJSONObject(0);
        JSONObject typeName = query.getJSONArray("type_name").getJSONObject(0);

        assertEquals(POINTS.getLocalPart(), typeName.getString("local_part"));
        assertEquals(POINTS.getPrefix(), typeName.getString("prefix"));
        assertEquals(POINTS.getNamespaceURI(), typeName.getString("namespace_uri"));
    }

    @Test
    public void testGetMap() throws Exception {
        JSONObject rsp =
                (JSONObject)
                        getAsJSON(
                                "wms?"
                                        + String.join(
                                                "&",
                                                "service=wms",
                                                "request=GetMap",
                                                "version=1.1.1",
                                                "layers=" + getLayerId(BASIC_POLYGONS),
                                                "styles=",
                                                "bbox=-170,-80,170,80",
                                                "srs=EPSG:4326",
                                                "width=256",
                                                "height=256",
                                                "format=image/png",
                                                "simulate=true"));
        print(rsp);

        assertService(rsp, "wms", "1.1.1");

        JSONObject req = rsp.getJSONObject("operation").getJSONObject("request");
        JSONArray layers = req.getJSONArray("layers");
        assertEquals(1, layers.size());

        JSONObject layer = layers.getJSONObject(0);
        assertEquals("cite:BasicPolygons", layer.getString("name"));
    }

    @Test
    public void testGetMapWithViewParams() throws Exception {
        JSONObject rsp =
                (JSONObject)
                        getAsJSON(
                                "wms?"
                                        + String.join(
                                                "&",
                                                "service=wms",
                                                "request=GetMap",
                                                "version=1.1.1",
                                                "layers=" + getLayerId(BASIC_POLYGONS),
                                                "styles=",
                                                "bbox=-170,-80,170,80",
                                                "srs=EPSG:4326",
                                                "width=256",
                                                "height=256",
                                                "format=image/png",
                                                "viewparams=foo:bar;baz:bam",
                                                "simulate=true"));
        print(rsp);

        JSONObject req = rsp.getJSONObject("operation").getJSONObject("request");
        JSONArray vp = req.getJSONArray("view_params");
        assertEquals(1, vp.size());

        JSONObject kvp = vp.getJSONObject(0);
        assertEquals(2, kvp.size());

        assertEquals("bam", kvp.get("BAZ"));
        assertEquals("bar", kvp.get("FOO"));
    }

    void assertService(JSONObject rsp, String id, String ver) {
        JSONObject srv = rsp.getJSONObject("service");
        assertEquals(id, srv.getString("name"));
        assertEquals(ver, srv.getString("version"));
    }
}
