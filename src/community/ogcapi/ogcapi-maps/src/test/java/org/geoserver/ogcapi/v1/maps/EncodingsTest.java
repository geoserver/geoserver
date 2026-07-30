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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
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
        MockHttpServletResponse response = getAccepting("image/png,image/jpeg");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", getBaseMimeType(response.getContentType()));
        assertEquals(100, decode(response).getWidth());

        // a client asking only for JPEG gets JPEG, so the header really drives the choice
        response = getAccepting("image/jpeg");
        assertEquals(200, response.getStatus());
        assertEquals("image/jpeg", getBaseMimeType(response.getContentType()));

        // a wildcard states no preference, and lands on the default encoding
        response = getAccepting("*/*");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", getBaseMimeType(response.getContentType()));

        // no Accept header at all behaves the same way
        MockHttpServletResponse plain = getAsServletResponse(MAP);
        assertEquals(200, plain.getStatus());
        assertEquals("image/png", getBaseMimeType(plain.getContentType()));
    }

    /** /conf/png/content: one map per response, in colour, with an alpha channel carrying the transparency. */
    @Test
    public void testPngContent() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/png");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", getBaseMimeType(response.getContentType()));
        BufferedImage image = decode(response);
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
        MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/jpeg");
        assertEquals(200, response.getStatus());
        assertEquals("image/jpeg", getBaseMimeType(response.getContentType()));
        BufferedImage image = decode(response);
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

    private static BufferedImage decode(MockHttpServletResponse response) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertNotNull("the payload must decode to a single image", image);
        return image;
    }
}
