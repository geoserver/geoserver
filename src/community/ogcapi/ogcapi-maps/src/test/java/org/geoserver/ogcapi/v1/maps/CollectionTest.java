/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.geoserver.catalog.ResourceInfo.TIME;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class CollectionTest extends MapsTestSupport {

    private static final String NATURE_GROUP = "NATURE";
    private static final String NATURE_TITLE = "I love nature";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        // compute tight bounds for Lakes
        FeatureTypeInfo lakesType = catalog.getFeatureTypeByName(getLayerId(MockData.LAKES));
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setupBounds(lakesType);
        catalog.save(lakesType);

        // create layer group
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        if (lakes != null && forests != null) {
            group.setName(NATURE_GROUP);
            group.setTitle(NATURE_TITLE);
            group.getLayers().add(lakes);
            group.getLayers().add(forests);
            group.getStyles().add(null);
            group.getStyles().add(null);
            cb.calculateLayerGroupBounds(group);
            catalog.add(group);
        }
    }

    @Test
    public void testLayerJson() throws Exception {
        DocumentContext json =
                getAsJSONPath("ogc/maps/v1/collections/" + getLayerId(MockData.LAKES), 200);
        testLakesJson(json);
    }

    private void testLakesJson(DocumentContext json) {
        assertEquals("cite:Lakes", json.read("id"));
        assertEquals("Lakes", json.read("title"));

        DocumentContext spatial = readSingleContext(json, "$.extent.spatial.bbox");
        assertEquals(6e-4, spatial.read("$[0]"), 0d);
        assertEquals(-0.0018, spatial.read("$[1]"), 0d);
        assertEquals(0.0031, spatial.read("$[2]"), 0d);
        assertEquals(-1.0E-4, spatial.read("$[3]"), 0d);
    }

    @Test
    public void testLayerGroupJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/" + NATURE_GROUP, 200);
        testNatureJson(json);
    }

    private void testNatureJson(DocumentContext json) {
        assertEquals("NATURE", json.read("id"));
        assertEquals(NATURE_TITLE, json.read("title"));

        DocumentContext spatial = readSingleContext(json, "$.extent.spatial.bbox");
        assertEquals(-180, spatial.read("$[0]"), 0d);
        assertEquals(-90, spatial.read("$[1]"), 0d);
        assertEquals(180, spatial.read("$[2]"), 0d);
        assertEquals(90, spatial.read("$[3]"), 0d);
    }

    @Test
    public void testTemporalCollectionHTML() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, DimensionPresentation.LIST, null, null, null);
        org.jsoup.nodes.Document document =
                getAsJSoup("ogc/maps/v1/collections/sf:timeseries?f=html");

        String id = getLayerId(TIMESERIES).replace(":", "__");

        // check temporal extent
        assertEquals(
                "Temporal extent: 2014-01-01T00:00:00Z/2019-01-01T00:00:00Z",
                document.select("#" + id + "_temporal").text());
    }

    @Test
    public void testCollectionJson() throws Exception {
        setupRasterDimension(TIMESERIES, TIME, DimensionPresentation.LIST, null, null, null);
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:timeseries", 200);

        assertEquals("2014-01-01T00:00:00Z", json.read("$.extent.temporal.interval[0][0]"));
        assertEquals("2019-01-01T00:00:00Z", json.read("$.extent.temporal.interval[0][1]"));
    }
}
