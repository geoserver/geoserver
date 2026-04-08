/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** KMZ-focused tests checking the WMS KMZ output format, including the zipped content. */
public class KMZWmsTest extends KMLBaseTest {

    @Test
    public void testContentDisposition() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format="
                + KMZMapOutputFormat.MIME_TYPE
                + "&layers="
                + MockData.BASIC_POLYGONS.getPrefix()
                + ":"
                + MockData.BASIC_POLYGONS.getLocalPart()
                + "&styles="
                + MockData.BASIC_POLYGONS.getLocalPart()
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals("attachment; filename=cite-BasicPolygons.kmz", resp.getHeader("Content-Disposition"));
    }

    @Test
    public void testKMZMixed() throws Exception {
        // force vector layers to be vector dumps (kmscore 100)
        MockHttpServletResponse response = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format="
                + KMZMapOutputFormat.MIME_TYPE
                + "&layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + ","
                + getLayerId(MockData.WORLD)
                + "&styles="
                + MockData.BASIC_POLYGONS.getLocalPart()
                + ","
                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&format_options=kmscore:100");

        // check the contents of the zip file
        ByteArrayInputStream bis = getBinaryInputStream(response);
        ZipInputStream zis = new ZipInputStream(bis);

        // first entry, the kml document itself
        ZipEntry entry = zis.getNextEntry();
        assertNotNull("Expected the KMZ to contain the kml document entry", entry);
        assertEquals("wms.kml", entry.getName());
        byte[] data = IOUtils.toByteArray(zis);
        Document dom = dom(new ByteArrayInputStream(data));

        // we have the placemarks in the first folder (vector), and no ground overlays
        XMLUnit.setIgnoreWhitespace(true);
        assertEquals("3", XMLUnit.newXpathEngine().evaluate("count(//kml:Folder[1]/kml:Placemark)", dom));
        assertEquals("0", XMLUnit.newXpathEngine().evaluate("count(//kml:Folder[1]/kml:GroundOverlay)", dom));
        // we have only the ground overlay in the second folder
        assertEquals("0", XMLUnit.newXpathEngine().evaluate("count(//kml:Folder[2]/kml:Placemark)", dom));
        assertEquals("1", XMLUnit.newXpathEngine().evaluate("count(//kml:Folder[2]/kml:GroundOverlay)", dom));
        assertEquals(
                "images/layers_1.png",
                XMLUnit.newXpathEngine().evaluate("//kml:Folder[2]/kml:GroundOverlay/kml:Icon/kml:href", dom));
        zis.closeEntry();

        // the images folder
        entry = zis.getNextEntry();
        assertNotNull("Expected images/ entry in the KMZ", entry);
        assertEquals("images/", entry.getName());
        zis.closeEntry();

        // the ground overlay for the raster layer
        entry = zis.getNextEntry();
        assertNotNull("Expected image entry for the ground overlay", entry);
        assertEquals("images/layers_1.png", entry.getName());
        zis.closeEntry();
        assertNull(zis.getNextEntry());
    }

    @Test
    public void testGraphicPackage() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wms?request=getmap&service=wms&version=1.1.1"
                + "&format="
                + KMZMapOutputFormat.MIME_TYPE
                + "&layers="
                + getLayerId(MockData.BRIDGES)
                + "&styles=bridge"
                + "&height=1024&width=1024&format_options=kmscore:100&bbox=-180,-90,180,90&srs=EPSG:4326");
        // save the response to a file and check the zip entries for bridge image
        ByteArrayInputStream bis = getBinaryInputStream(response);
        // read all bytes so we can both save and inspect
        byte[] kmzBytes = IOUtils.toByteArray(bis);

        // Inspect the KMZ (zip) contents and check for images/ and images/bridge.png
        boolean foundImagesFolder = false;
        boolean foundBridgePng = false;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(kmzBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("icons/".equals(name) || name.equals("icons")) {
                    foundImagesFolder = true;
                }
                if ("icons/bridge.png".equals(name) || "icons/bridge.PNG".equals(name)) {
                    foundBridgePng = true;
                }
                zis.closeEntry();
            }
        }

        assertTrue("KMZ should contain an images/ folder", foundImagesFolder);
        assertTrue("KMZ should contain images/bridge.png", foundBridgePng);
    }
}
