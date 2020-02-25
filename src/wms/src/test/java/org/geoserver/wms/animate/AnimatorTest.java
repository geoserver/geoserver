/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.animate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.RenderedImageMap;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Some functional tests for animator
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S., alessio.fabiani@geo-solutions.it
 * @author Andrea Aime, GeoSolutions S.A.S., andrea.aime@geo-solutions.it
 */
public class AnimatorTest extends WMSTestSupport {

    /** default 'format' value */
    public static final String GIF_ANIMATED_FORMAT = "image/gif;subtype=animated";

    /** Testing FrameCatalog constructor from a generic WMS request. */
    @org.junit.Test
    public void testFrameCatalog() throws Exception {
        final WebMapService wms = (WebMapService) applicationContext.getBean("wmsService2");
        final String layerName =
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        GetMapRequest getMapRequest = createGetMapRequest(new QName(layerName));

        FrameCatalog catalog = null;
        try {
            catalog = new FrameCatalog(getMapRequest, wms, getWMS());
        } catch (RuntimeException e) {
            assertEquals(
                    "Missing \"animator\" mandatory params \"aparam\" and \"avalues\".",
                    e.getLocalizedMessage());
        }

        getMapRequest.getRawKvp().put("aparam", "fake_param");
        getMapRequest.getRawKvp().put("avalues", "val0,val\\,1,val2\\,\\,,val3");

        catalog = new FrameCatalog(getMapRequest, wms, getWMS());

        assertNotNull(catalog);
        assertEquals("fake_param", catalog.getParameter());
        assertEquals(4, catalog.getValues().length);
        assertEquals("val0", catalog.getValues()[0]);
        assertEquals("val\\,1", catalog.getValues()[1]);
        assertEquals("val2\\,\\,", catalog.getValues()[2]);
        assertEquals("val3", catalog.getValues()[3]);
    }

    /** Testing FrameVisitor animation frames setup and production. */
    @org.junit.Test
    public void testFrameVisitor() throws Exception {
        final WebMapService wms = (WebMapService) applicationContext.getBean("wmsService2");
        final String layerName =
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        GetMapRequest getMapRequest = createGetMapRequest(new QName(layerName));

        FrameCatalog catalog = null;

        getMapRequest.getRawKvp().put("aparam", "fake_param");
        getMapRequest.getRawKvp().put("avalues", "val0,val\\,1,val2\\,\\,,val3");
        getMapRequest.getRawKvp().put("format", GIF_ANIMATED_FORMAT);
        getMapRequest.getRawKvp().put("LAYERS", layerName);

        catalog = new FrameCatalog(getMapRequest, wms, getWMS());

        assertNotNull(catalog);

        FrameCatalogVisitor visitor = new FrameCatalogVisitor();
        catalog.getFrames(visitor);

        assertEquals(4, visitor.framesNumber);

        List<RenderedImageMap> frames = visitor.produce(getWMS());

        assertNotNull(frames);
        assertEquals(4, frames.size());
    }

    /** Produce animated gif through the WMS request. */
    @org.junit.Test
    public void testAnimator() throws Exception {
        final String layerName =
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        String requestURL =
                "wms/animate?layers="
                        + layerName
                        + "&aparam=fake_param&avalues=val0,val\\,1,val2\\,\\,,val3";

        checkAnimatedGif(requestURL, false, WMS.DISPOSAL_METHOD_DEFAULT);
        checkAnimatedGif(
                requestURL + "&format_options=gif_loop_continuously:true",
                true,
                WMS.DISPOSAL_METHOD_DEFAULT);
        checkAnimatedGif(
                requestURL + "&format_options=gif_loop_continuosly:true",
                true,
                WMS.DISPOSAL_METHOD_DEFAULT);
        // check all valid disposal methods
        for (String disposal : WMS.DISPOSAL_METHODS) {
            checkAnimatedGif(
                    requestURL + "&format_options=gif_disposal:" + disposal, false, disposal);
        }
    }

    private void checkAnimatedGif(String requestURL, boolean loopContinously, String disposal)
            throws Exception, IOException {
        MockHttpServletResponse resp = getAsServletResponse(requestURL);

        assertEquals("image/gif", resp.getContentType());
        try (ImageInputStream is = ImageIO.createImageInputStream(getBinaryInputStream(resp))) {
            ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
            reader.setInput(is);

            // check we have the right number of images
            assertEquals(4, reader.getNumImages(true));

            IIOMetadata imageMetadata = reader.getImageMetadata(0);
            LOGGER.info(Arrays.toString(imageMetadata.getMetadataFormatNames()));
            IIOMetadataNode node =
                    (IIOMetadataNode) imageMetadata.getAsTree("javax_imageio_gif_image_1.0");
            // print("", node);

            // check the applied disposal method
            IIOMetadataNode nodeGCE =
                    (IIOMetadataNode) node.getElementsByTagName("GraphicControlExtension").item(0);
            // assign the proper specified value for the disposal method option
            if (disposal == "backgroundColor") {
                disposal = "restoreToBackgroundColor";
            } else if (disposal == "previous") {
                disposal = "restoreToPrevious";
            }
            assertEquals(disposal, nodeGCE.getAttribute("disposalMethod").toString());

            NodeList nodes = node.getElementsByTagName("ApplicationExtensions");
            node = (IIOMetadataNode) nodes.item(0);
            nodes = node.getElementsByTagName("ApplicationExtension");
            boolean found = false;
            for (int i = 0; i < nodes.getLength(); i++) {
                node = (IIOMetadataNode) nodes.item(i);
                if ("NETSCAPE".equals(node.getAttribute("applicationID"))
                        && "2.0".equals(node.getAttribute("authenticationCode"))) {
                    found = true;
                    byte[] flags = (byte[]) node.getUserObject();
                    if (loopContinously) {
                        assertArrayEquals(new byte[] {0x1, 0x0, 0x0}, flags);
                    } else {
                        assertArrayEquals(new byte[] {0x1, 0x1, 0x0}, flags);
                    }
                }
            }
            if (!found) {
                fail("Could not find custom metadata node containing the loop control extension");
            }
        }
    }

    /** Utility method to print a metadata node */
    private void print(String prefix, IIOMetadataNode node) {
        Object user = node.getUserObject();
        System.out.println(
                prefix
                        + node.getNodeName()
                        + ": "
                        + node.getNodeValue()
                        + ", "
                        + (user instanceof byte[] ? Arrays.toString((byte[]) user) : user));
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            print(prefix + "Attribute ", (IIOMetadataNode) attributes.item(i));
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            print("  " + prefix, (IIOMetadataNode) children.item(i));
        }
    }

    /** Animate layers */
    @org.junit.Test
    public void testAnimatorLayers() throws Exception {
        final String layerName =
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        String requestURL = "cite/wms/animate?&aparam=layers&avalues=MapNeatline,Buildings,Lakes";

        // check we got a gif
        MockHttpServletResponse resp = getAsServletResponse(requestURL);
        assertEquals("image/gif", resp.getContentType());

        // check it has three frames
        ByteArrayInputStream bis = getBinaryInputStream(resp);
        ImageInputStream iis = ImageIO.createImageInputStream(bis);
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(iis);
        assertEquals(3, reader.getNumImages(true));
    }

    /** Animate layer groups */
    @org.junit.Test
    public void testAnimatorLayerGroups() throws Exception {
        Catalog catalog = getCatalog();
        LayerGroupInfo singleGroup =
                createLakesPlacesLayerGroup(
                        catalog, "singleGroup", LayerGroupInfo.Mode.SINGLE, null);
        try {
            LayerGroupInfo namedGroup =
                    createLakesPlacesLayerGroup(
                            catalog, "namedGroup", LayerGroupInfo.Mode.NAMED, null);
            try {
                LayerGroupInfo eoGroup =
                        createLakesPlacesLayerGroup(
                                catalog,
                                "eoGroup",
                                LayerGroupInfo.Mode.EO,
                                catalog.getLayerByName(getLayerId(MockData.LAKES)));
                try {

                    String requestURL =
                            "wms/animate?BBOX=0.0000,-0.0020,0.0035,0.0010&width=512&aparam=layers&avalues="
                                    + singleGroup.getName()
                                    + ","
                                    + namedGroup.getName()
                                    + ","
                                    + eoGroup.getName();

                    // check we got a gif
                    MockHttpServletResponse resp = getAsServletResponse(requestURL);
                    assertEquals("image/gif", resp.getContentType());

                    // check it has three frames
                    ByteArrayInputStream bis = getBinaryInputStream(resp);
                    ImageInputStream iis = ImageIO.createImageInputStream(bis);
                    ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
                    reader.setInput(iis);

                    // BufferedImage gif = getAsImage(requestURL, "image/gif");
                    // ImageIO.write(gif, "gif", new File("anim.gif"));

                    assertEquals(3, reader.getNumImages(true));

                    // single group:
                    BufferedImage image = reader.read(0);
                    assertPixel(image, 300, 270, Color.WHITE);
                    // places
                    assertPixel(image, 380, 30, COLOR_PLACES_GRAY);
                    // lakes
                    assertPixel(image, 180, 350, COLOR_LAKES_BLUE);

                    // named group:
                    image = reader.read(1);
                    assertPixel(image, 300, 270, Color.WHITE);
                    // places
                    assertPixel(image, 380, 30, COLOR_PLACES_GRAY);
                    // lakes
                    assertPixel(image, 180, 350, COLOR_LAKES_BLUE);

                    // EO group:
                    image = reader.read(2);
                    assertPixel(image, 300, 270, Color.WHITE);
                    // no places
                    assertPixel(image, 380, 30, Color.WHITE);
                    // lakes
                    assertPixel(image, 180, 350, COLOR_LAKES_BLUE);
                } finally {
                    catalog.remove(eoGroup);
                }
            } finally {
                catalog.remove(namedGroup);
            }
        } finally {
            catalog.remove(singleGroup);
        }
    }
}
