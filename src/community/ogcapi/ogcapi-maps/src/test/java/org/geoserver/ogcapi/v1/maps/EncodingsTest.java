/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The map encodings and the content negotiation that selects them: {@code /conf/core/map-op},
 * {@code /conf/png/content}, {@code /conf/jpeg/content}, {@code /conf/svg/content} and {@code /conf/html/content}.
 */
public class EncodingsTest extends MapsTestSupport {

    /** A map tight on Blue Lake, so a known pixel falls inside the polygon and carries its colour. */
    private static final String MAP =
            "ogc/maps/v1/collections/Lakes/map?bbox=-0.002,-0.003,0.005,0.002&width=100&height=100";

    private static final int LAKE_X = 50;
    private static final int LAKE_Y = 64;

    /**
     * Retrieves the map resource negotiating the encoding through the {@code Accept} header alone, with no {@code f}.
     */
    private MockHttpServletResponse getAccepting(String accept) throws Exception {
        MockHttpServletRequest request = createRequest(MAP);
        request.setMethod("GET");
        request.addHeader("Accept", accept);
        return dispatch(request, null);
    }

    /** /conf/core/map-op: the media type is negotiated through the Accept header, the f parameter is not required. */
    @Test
    public void testAcceptHeaderNegotiation() throws Exception {
        assertEquals(
                100,
                readImage(getAccepting("image/png,image/jpeg"), "image/png", "png")
                        .getWidth());

        // a client asking only for JPEG gets JPEG, so the header really drives the choice
        readImage(getAccepting("image/jpeg"), "image/jpeg", "jpeg");

        // a wildcard states no preference, and lands on the default encoding
        readImage(getAccepting("*/*"), "image/png", "png");

        // no Accept header at all behaves the same way
        readImage(getAsServletResponse(MAP), "image/png", "png");
    }

    /** /conf/png/content: one map per response, in colour, with an alpha channel carrying the transparency. */
    @Test
    public void testPngContent() throws Exception {
        BufferedImage image = getAsPNG(MAP + "&f=image/png");
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        // an alpha channel is present and used: the corner has no data, the lake interior is opaque
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        // the colours carry the feature: the default Lakes style fills them with a blue dominant tone
        assertEquals(0xFF4040C0, image.getRGB(LAKE_X, LAKE_Y));
    }

    /** /conf/jpeg/content: one opaque colour map per response, JPEG having no transparency to encode. */
    @Test
    public void testJpegContent() throws Exception {
        BufferedImage image = getAsJPEG(MAP + "&f=image/jpeg");
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertEquals(3, image.getColorModel().getNumComponents());
        // the lake renders blue; JPEG is lossy, so compare the dominant band rather than an exact value
        int lake = image.getRGB(LAKE_X, LAKE_Y);
        assertThat(lake & 0xFF, greaterThan((lake >> 16) & 0xFF));
    }

    /**
     * /conf/svg/content: with the Batik renderer configured, which is what the SVG conformance class follows, a single
     * SVG document sized as requested and drawn in a coordinate system running from 0,0 to that size.
     */
    @Test
    public void testSvgContent() throws Exception {
        withSvgRenderer(WMS.SVG_BATIK, () -> {
            MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/svg%2Bxml");
            assertEquals(200, response.getStatus());
            assertEquals("image/svg+xml", getBaseMimeType(response.getContentType()));
            Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
            Element svg = dom.getDocumentElement();
            assertEquals("svg", svg.getLocalName());
            assertEquals("100", svg.getAttribute("width"));
            assertEquals("100", svg.getAttribute("height"));
            // no viewBox means the default user space, which starts at 0,0 and ends at the width and height
            assertEquals("", svg.getAttribute("viewBox"));

            // the map is clipped to exactly that pixel box, and every drawn coordinate falls inside it
            NodeList paths = dom.getElementsByTagName("path");
            assertEquals("M0 0 L100 0 L100 100 L0 100 L0 0 Z", ((Element) paths.item(0)).getAttribute("d"));
            assertThat(paths.getLength(), greaterThan(1));
            for (int i = 1; i < paths.getLength(); i++) {
                for (String ordinate :
                        ((Element) paths.item(i)).getAttribute("d").split("[^0-9.]+")) {
                    if (ordinate.isEmpty()) continue;
                    assertThat(Double.parseDouble(ordinate), allOf(greaterThanOrEqualTo(0d), lessThanOrEqualTo(100d)));
                }
            }
        });
    }

    /**
     * The streaming SVG renderer, the GeoServer default, writes the world coordinates in the {@code viewBox} instead of
     * drawing in the requested pixel space, so it does not meet {@code /req/svg/content} B. The conformance class
     * follows the renderer choice, so with that one configured SVG is not an offered encoding at all.
     */
    @Test
    public void testStreamingSvgNotOffered() throws Exception {
        withSvgRenderer(WMS.SVG_SIMPLE, () -> {
            MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/svg%2Bxml");
            assertEquals(406, response.getStatus());
            assertThat(response.getContentAsString(), containsString("SVG"));
        });
        // an administrator accepting the deviation can still turn the class on, and then the map is encoded
        withSvgRenderer(
                WMS.SVG_SIMPLE,
                () -> withConformance(MapsConformance::setSvg, true, () -> {
                    MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/svg%2Bxml");
                    assertEquals(200, response.getStatus());
                    Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
                    // the non conformant part: the coordinate system is the world one, not the pixel one
                    assertThat(dom.getDocumentElement().getAttribute("viewBox"), containsString("-0.002"));
                }));
    }

    /** Runs the body with the given WMS SVG renderer configured, restoring the previous choice afterwards. */
    private void withSvgRenderer(String renderer, ThrowingRunnable body) throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        String previous = (String) wms.getMetadata().get("svgRenderer");
        wms.getMetadata().put("svgRenderer", renderer);
        gs.save(wms);
        try {
            body.run();
        } finally {
            wms.getMetadata().put("svgRenderer", previous);
            gs.save(wms);
        }
    }

    /** /conf/html/content: an HTML document presenting the geospatial data as a map. */
    @Test
    public void testHtmlContent() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + "&f=text/html");
        assertEquals(200, response.getStatus());
        assertEquals("text/html", getBaseMimeType(response.getContentType()));
        String html = response.getContentAsString();
        // the page carries a map viewer pointed at this collection, not just an arbitrary HTML document
        assertThat(html, containsString("Lakes"));
        assertThat(html, containsString("openlayers"));
    }

    @Test
    public void testTiffFormat() throws Exception {
        // TIFF is a configurable class, enabled by default, so the request must succeed and decode to a real raster
        BufferedImage tiff =
                getAsTIFF("ogc/maps/v1/collections/Lakes/map?f=image/tiff&bbox=-1,-1,1,1&width=50&height=50");
        assertEquals(50, tiff.getWidth());
        assertEquals(50, tiff.getHeight());
    }

    @Test
    public void testTiffDisabledNotAcceptable() throws Exception {
        // a disabled format class means the encoding is not offered: content negotiation fails with 406
        withConformance(MapsConformance::setTiff, false, () -> {
            MockHttpServletResponse response = getAsServletResponse(
                    "ogc/maps/v1/collections/Lakes/map?f=image/tiff&bbox=-1,-1,1,1&width=50&height=50");
            assertEquals(406, response.getStatus());
            assertThat(response.getContentAsString(), containsString("TIFF"));
        });
    }

    @Test
    public void testSvgDisabledNotAcceptable() throws Exception {
        withConformance(MapsConformance::setSvg, false, () -> {
            MockHttpServletResponse response = getAsServletResponse(
                    "ogc/maps/v1/collections/Lakes/map?f=image/svg%2Bxml&bbox=-1,-1,1,1&width=50&height=50");
            assertEquals(406, response.getStatus());
            assertThat(response.getContentAsString(), containsString("SVG"));
        });
    }

    /** An f value naming an encoding the server cannot produce is a failed negotiation, not a server error. */
    @Test
    public void testUnknownFormatNotAcceptable() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/nosuchthing");
        assertEquals(406, response.getStatus());
        assertEquals("application/json", getBaseMimeType(response.getContentType()));
        DocumentContext json = JsonPath.parse(response.getContentAsString());
        assertEquals("NotAcceptable", json.read("type"));
        assertThat(json.read("title"), containsString("image/nosuchthing"));
    }

    /** An f value that is not a media type at all is a bad parameter value, caught before the map is rendered. */
    @Test
    public void testMalformedFormatRejected() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + "&f=garbage");
        assertEquals(400, response.getStatus());
        DocumentContext json = JsonPath.parse(response.getContentAsString());
        assertEquals("InvalidParameterValue", json.read("type"));
        assertThat(json.read("title"), containsString("garbage"));
    }
}
