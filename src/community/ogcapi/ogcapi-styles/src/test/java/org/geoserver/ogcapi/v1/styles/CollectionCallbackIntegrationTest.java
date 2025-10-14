/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.junit.Test;

public class CollectionCallbackIntegrationTest extends StylesTestSupport {

    @Test
    public void testFeatureCollectionCallback() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + getLayerId(MockData.BASIC_POLYGONS), 200);

        // the collection shows links to the styles API
        // concentrate on one and check title and links
        assertEquals("A blue linestring style", readSingle(json, "styles[?(@.id == 'BasicPolygons')].title"));
        // there is at least a link to SLD 1.0
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/BasicPolygons?f=application%2Fvnd.ogc.sld%2Bxml",
                readSingle(
                        json,
                        "styles[?(@.id == 'BasicPolygons')].links[?(@.rel == 'stylesheet' && @.type == 'application/vnd.ogc.sld+xml')].href"));
        // link to the metadata
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/BasicPolygons/metadata?f=application%2Fjson",
                readSingle(
                        json,
                        "styles[?(@.id == 'BasicPolygons')].links[?(@.rel == 'describedBy' && @.type == 'application/json')].href"));
    }

    @Test
    public void testTilesCollectionCallback() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/collections/" + getLayerId(MockData.BASIC_POLYGONS), 200);

        // tile tiles API already adds the notion of style, but the links to the styles formats and
        // metadata need to be added
        // there is at least a link to SLD 1.0
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/BasicPolygons?f=application%2Fvnd.ogc.sld%2Bxml",
                readSingle(
                        json,
                        "styles[?(@.id == 'BasicPolygons')].links[?(@.rel == 'stylesheet' && @.type == 'application/vnd.ogc.sld+xml')].href"));
        // link to the metadata
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/BasicPolygons/metadata?f=application%2Fjson",
                readSingle(
                        json,
                        "styles[?(@.id == 'BasicPolygons')].links[?(@.rel == 'describedBy' && @.type == 'application/json')].href"));
    }

    @Test
    public void testTilesCollectionGroupCallback() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/collections/BasicStyleGroup", 200);

        // tile tiles API already adds the notion of style, but the links to the styles formats and
        // metadata need to be added
        // there is at least a link to SLD 1.0
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/BasicStyleGroupStyle?f=application%2Fvnd.ogc.sld%2Bxml",
                readSingle(
                        json,
                        "styles[?(@.id == 'BasicStyleGroupStyle')].links[?(@.rel == 'stylesheet' && @.type == 'application/vnd.ogc.sld+xml')].href"));
        // link to the metadata
        assertEquals(
                "http://localhost:8080/geoserver/ogc/styles/v1/styles/BasicStyleGroupStyle/metadata?f=application%2Fjson",
                readSingle(
                        json,
                        "styles[?(@.id == 'BasicStyleGroupStyle')].links[?(@.rel == 'describedBy' && @.type == 'application/json')].href"));
    }
}
