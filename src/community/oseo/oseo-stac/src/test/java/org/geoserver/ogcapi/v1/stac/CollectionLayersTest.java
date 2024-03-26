/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.geoserver.data.test.CiteTestData.TASMANIA_BM;
import static org.geoserver.data.test.CiteTestData.TASMANIA_DEM;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.hamcrest.CoreMatchers;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

public class CollectionLayersTest extends STACTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // collection specific templates
        copyTemplate("/collection-layers.ftl", "templates/ogc/stac/v1/", "collection.ftl");
        copyTemplate("/collection-layers.json", "templates/ogc/stac/v1/", "collections.json");
        copyTemplate("/item-collection-layers.json", "templates/ogc/stac/v1/", "items.json");
        copyTemplate("/item-collection-layers.ftl", "templates/ogc/stac/v1/", "item_include.ftl");

        // Create fake layers matching the collection ones
        Catalog catalog = getCatalog();
        NamespaceInfo ns = catalog.getNamespaceByPrefix("gs");
        System.out.println(catalog.getCoverages());

        CoverageInfo c1 = catalog.getCoverageByName(getLayerId(TASMANIA_DEM));
        c1.setNamespace(ns);
        c1.setName("landsat8-SINGLE");
        c1.setTitle("Single title");
        c1.setDescription("Single description");
        catalog.save(c1);

        CoverageInfo c2 = catalog.getCoverageByName(getLayerId(TASMANIA_BM));
        c2.setNamespace(ns);
        c2.setName("landsat8-SEPARATE");
        c2.setTitle("Separate title");
        c2.setDescription("Separate description");
        catalog.save(c2);

        LayerInfo l2 = catalog.getLayerByName(c2.prefixedName());
        System.out.println(catalog.getStyles());
        StyleInfo polygon = catalog.getStyleByName("polygon");
        l2.getStyles().add(polygon);
        catalog.save(l2);
    }

    @Test
    public void testCollectionHTML() throws Exception {
        Document document = getAsJSoup("ogc/stac/v1/collections/LANDSAT8?f=html");
        checkLandsat8CollectionLayers(document);
        assertEquals(
                "http://localhost:8080/geoserver/wms?request=GetCapabilities&service=WMS",
                document.select("a.wmsCapabilities").attr("href"));
    }

    private static void checkLandsat8CollectionLayers(Document document) {
        Elements titles = document.select("p.title");
        assertEquals(titles.get(0).text(), "Single title");
        assertEquals(titles.get(1).text(), "Separate title");

        Elements descriptions = document.select("p.description");
        assertEquals(descriptions.get(0).text(), "Single description");
        assertEquals(descriptions.get(1).text(), "Separate description");

        Elements styles = document.select("p.style");
        assertEquals(styles.get(0).text(), "raster: Raster");
        assertEquals(styles.get(1).text(), "raster: Raster");
        assertEquals(styles.get(2).text(), "polygon: Grey Polygon");
    }

    @Test
    public void testCollectionJSON() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8?f=json", 200);
        assertEquals("gs", doc.read("$.layers[0].workspace"));
        assertEquals("landsat8-SINGLE", doc.read("$.layers[0].layer"));
        assertEquals("raster", doc.read("$.layers[0].styles[0].name"));
        assertEquals("Raster", doc.read("$.layers[0].styles[0].title"));
        assertEquals(true, doc.read("$.layers[0].services.wms.enabled"));
        assertThat(
                doc.read("$.layers[0].services.wms.formats"),
                CoreMatchers.hasItems("image/png", "image/jpeg"));
        assertEquals(true, doc.read("$.layers[0].services.wcs.enabled"));
        assertThat(
                doc.read("$.layers[0].services.wcs.formats"),
                CoreMatchers.hasItems("image/tiff", "application/gml+xml"));

        assertEquals("gs", doc.read("$.layers[1].workspace"));
        assertEquals("landsat8-SEPARATE", doc.read("$.layers[1].layer"));
        assertEquals("raster", doc.read("$.layers[1].styles[0].name"));
        assertEquals("Raster", doc.read("$.layers[1].styles[0].title"));
        assertEquals("polygon", doc.read("$.layers[1].styles[1].name"));
        assertEquals("Grey Polygon", doc.read("$.layers[1].styles[1].title"));
    }

    @Test
    public void testItemsJSON() throws Exception {
        DocumentContext doc =
                getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/items/LS8_TEST.02", 200);
        DocumentContext wmsLink = readSingleContext(doc, "$.links[?(@.rel=='wms')]");
        assertEquals("http://localhost:8080/geoserver/wms", wmsLink.read("$.href"));
        assertEquals(List.of("landsat8-SINGLE", "landsat8-SEPARATE"), wmsLink.read("wms:layers"));
    }

    @Test
    public void testItemsHTML() throws Exception {
        Document doc = getAsJSoup("ogc/stac/v1/collections/LANDSAT8/items/LS8_TEST.02?f=html");
        checkLandsat8CollectionLayers(doc);
    }
}
