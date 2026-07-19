/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class LegendTest extends MapsTestSupport {

    private static final String NATURE_GROUP = "natureLegend";

    private static final String TWO_RULES = "tworules";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // a two layer group, whose legend must show one entry per member
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(NATURE_GROUP);
        group.getLayers().add(catalog.getLayerByName(getLayerId(MockData.LAKES)));
        group.getLayers().add(catalog.getLayerByName(getLayerId(MockData.FORESTS)));
        group.getStyles().add(catalog.getStyleByName("red"));
        group.getStyles().add(null);
        new CatalogBuilder(catalog).calculateLayerGroupBounds(group);
        catalog.add(group);

        // a two rule style, to check rule selection
        testData.addStyle(TWO_RULES, getClass(), catalog);
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakes.getStyles().add(catalog.getStyleByName(TWO_RULES));
        catalog.save(lakes);
    }

    @Test
    public void testRuleSelection() throws Exception {
        // only the named rule is drawn, out of the two the style holds
        DocumentContext json = getAsJSONPath(
                "ogc/maps/v1/collections/cite:Lakes/styles/" + TWO_RULES + "/legend?f=application/json&rule=blue", 200);
        List<?> rules = json.read("$.Legend[0].rules", List.class);
        assertThat(rules, hasSize(1));
        assertThat(
                json.read("$.Legend[0].rules[0].symbolizers[0].Polygon.fill", String.class),
                equalToIgnoringCase("#0000FF"));
    }

    @Test
    public void testLayerGroupLegend() throws Exception {
        // one legend per member, each in the style the group assigns it, so the red one must be there
        DocumentContext json =
                getAsJSONPath("ogc/maps/v1/collections/" + NATURE_GROUP + "/legend?f=application/json", 200);
        List<?> legends = json.read("$.Legend", List.class);
        assertThat(legends, hasSize(2));
        assertEquals("Lakes", json.read("$.Legend[0].layerName", String.class));
        assertThat(
                json.read("$.Legend[0].rules[0].symbolizers[0].Polygon.fill", String.class),
                equalToIgnoringCase("#FF0000"));
        assertEquals("Forests", json.read("$.Legend[1].layerName", String.class));
    }

    @Test
    public void testUnsupportedFormatIsNotAcceptable() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/maps/v1/collections/cite:Lakes/legend?f=application/x-nonsense");
        assertEquals(406, response.getStatus());
    }

    @Test
    public void testDefaultStyleLegend() throws Exception {
        BufferedImage image = getAsImage("ogc/maps/v1/collections/cite:Lakes/legend?f=image/png", "image/png");
        assertNotNull(image);
        assertTrue(image.getWidth() > 0);
        assertTrue(image.getHeight() > 0);
    }

    @Test
    public void testNamedStyleLegend() throws Exception {
        // the red style fills polygons with #FF0000, so its legend swatch must actually contain red pixels
        BufferedImage image =
                getAsImage("ogc/maps/v1/collections/cite:Lakes/styles/red/legend?f=image/png", "image/png");
        assertTrue("legend should contain the red swatch", containsColor(image, Color.RED));
    }

    @Test
    public void testJsonLegend() throws Exception {
        // the JSON legend must carry the actual rendering rules; the red style has a single polygon
        // symbolizer filled with #FF0000, and that must round-trip into the legend document
        DocumentContext json =
                getAsJSONPath("ogc/maps/v1/collections/cite:Lakes/styles/red/legend?f=application/json", 200);
        List<?> rules = json.read("$.Legend[0].rules", List.class);
        assertThat(rules, hasSize(1));
        assertThat(
                json.read("$.Legend[0].rules[0].symbolizers[0].Polygon.fill", String.class),
                equalToIgnoringCase("#FF0000"));
    }

    /** True if any pixel matches the given RGB (alpha ignored). */
    private static boolean containsColor(BufferedImage image, Color color) {
        int rgb = color.getRGB() & 0xFFFFFF;
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int p : pixels) if ((p & 0xFFFFFF) == rgb) return true;
        return false;
    }

    @Test
    public void testLegendOptionsApplied() throws Exception {
        // a larger label font must reach the legend renderer, giving an image at least as tall as the default
        BufferedImage plain = getAsImage("ogc/maps/v1/collections/cite:Lakes/legend?f=image/png", "image/png");
        BufferedImage larger = getAsImage(
                "ogc/maps/v1/collections/cite:Lakes/legend?f=image/png&legend-options=fontSize:20", "image/png");
        assertNotNull(plain);
        assertNotNull(larger);
        assertTrue(larger.getHeight() >= plain.getHeight());
    }

    @Test
    public void testConfiguredLegendSize() throws Exception {
        // no size requested, so the 40x40 graphic configured on the layer is used as is
        withLakesLegend(40, () -> {
            BufferedImage image = getAsImage("ogc/maps/v1/collections/cite:Lakes/legend?f=image/png", "image/png");
            assertEquals(255, alphaAt(image, 30, 30));
        });
    }

    @Test
    public void testRequestedSizeWinsOverConfiguredLegendSize() throws Exception {
        // the graphic is scaled down into the requested 20x20 box, leaving the rest of the canvas empty
        withLakesLegend(40, () -> {
            BufferedImage image =
                    getAsImage("ogc/maps/v1/collections/cite:Lakes/legend?f=image/png&width=20&height=20", "image/png");
            assertEquals(255, alphaAt(image, 5, 5));
            assertEquals(0, alphaAt(image, 30, 30));
        });
    }

    /** The alpha of the pixel at the given position, 255 for rendered data and 0 for an empty one. */
    private static int alphaAt(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    /**
     * Gives the Lakes layer a legend graphic of the given square size, a fully opaque red square stored in the styles
     * directory, runs the body and removes the legend afterwards.
     */
    private void withLakesLegend(int size, ThrowingRunnable body) throws Exception {
        File styles = getDataDirectory().get("styles").dir();
        File graphic = new File(styles, "lakes-legend.png");
        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = square.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, size, size);
        g.dispose();
        ImageIO.write(square, "PNG", graphic);

        LegendInfo legendInfo = new LegendInfoImpl();
        legendInfo.setOnlineResource(graphic.getName());
        legendInfo.setFormat("image/png");
        legendInfo.setWidth(size);
        legendInfo.setHeight(size);

        Catalog catalog = getCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        lakes.setLegend(legendInfo);
        catalog.save(lakes);
        try {
            body.run();
        } finally {
            LayerInfo current = catalog.getLayerByName(getLayerId(MockData.LAKES));
            current.setLegend(null);
            catalog.save(current);
            graphic.delete();
        }
    }

    @Test
    public void testLegendDisabledReturns404() throws Exception {
        withConformance(MapsConformance::setLegend, false, () -> {
            MockHttpServletResponse response =
                    getAsServletResponse("ogc/maps/v1/collections/cite:Lakes/legend?f=image/png");
            assertEquals(404, response.getStatus());
        });
    }

    @Test
    public void testStylesJsonHasLegendLinks() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/cite:Lakes/styles", 200);
        List<String> legendHrefs = json.read("$.styles[*].links[?(@.rel=='legend')].href");
        assertThat(legendHrefs, not(empty()));
        assertThat(legendHrefs.get(0), containsString("/legend"));
    }

    @Test
    public void testStylesHtmlHasLegendImage() throws Exception {
        Document document = getAsJSoup("ogc/maps/v1/collections/cite:Lakes/styles?f=text/html");
        assertThat(document.select("img[src*=/legend]"), not(hasSize(0)));
    }

    @Test
    public void testStylesHtmlNoLegendWhenDisabled() throws Exception {
        withConformance(MapsConformance::setLegend, false, () -> {
            Document document = getAsJSoup("ogc/maps/v1/collections/cite:Lakes/styles?f=text/html");
            assertThat(document.select("img[src*=/legend]"), hasSize(0));
        });
    }
}
