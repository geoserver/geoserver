/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

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
     * /conf/svg/content: a single SVG document sized as requested. The second assertion of the requirement, map
     * coordinates running from 0,0 to the requested width and height, is not met by the streaming SVG renderer
     * GeoServer uses by default: it writes the world coordinates in the {@code viewBox} instead. That is the shared WMS
     * SVG encoder, so the deviation is recorded here rather than changed under the Maps API.
     */
    @Test
    public void testSvgContent() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(MAP + "&f=image/svg%2Bxml");
        assertEquals(200, response.getStatus());
        assertEquals("image/svg+xml", getBaseMimeType(response.getContentType()));
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertEquals("svg", dom.getDocumentElement().getLocalName());
        assertEquals("100", dom.getDocumentElement().getAttribute("width"));
        assertEquals("100", dom.getDocumentElement().getAttribute("height"));
        // one map per document: the lake polygon is there, and it is the only feature drawn
        assertEquals(1, dom.getElementsByTagName("path").getLength());
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
