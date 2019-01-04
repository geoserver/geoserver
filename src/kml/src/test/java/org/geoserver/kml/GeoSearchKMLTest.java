/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GeoSearchKMLTest extends RegionatingTestSupport {

    @Before
    public void resetMetadata() throws IOException {
        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().remove("kml.regionateFeatureLimit");
        getCatalog().save(fti);
    }

    @After
    public void cleanupRegionationDatabases() throws IOException {
        File dir = getDataDirectory().findOrCreateDir("geosearch");
        FileUtils.deleteDirectory(dir);
    }

    @Test
    public void testSelfLinks() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.BASIC_POLYGONS.getPrefix()
                        + ":"
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "&styles="
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "&height=1024&width=1024&bbox=-180,-90,0,90&srs=EPSG:4326"
                        + "&featureid=BasicPolygons.1107531493643&format_options=selfLinks:true";

        Document document = getAsDOM(path);
        // print(document);
        assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:Placemark)", document);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/cite/BasicPolygons/1107531493643.kml",
                "//kml:Placemark/atom:link/@href",
                document);
        assertXpathEvaluatesTo("self", "//kml:Placemark/atom:link/@rel", document);
    }

    /** Test that requests regionated by data actually return stuff. */
    @Test
    public void testDataRegionator() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:external-sorting;regionateAttr:NUM_LANES";

        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        int westCount = document.getDocumentElement().getElementsByTagName("Placemark").getLength();

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");

        assertEquals(1, westCount);
    }

    /** Test that requests regionated by geometry actually return stuff. */
    @Test
    public void testGeometryRegionator() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:geometry;regionateAttr:the_geom";
        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(
                1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");
    }

    /** Test that requests regionated by random criteria actually return stuff. */
    @Test
    public void testRandomRegionator() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:random";
        Document document = getAsDOM(path + "&bbox=-180,-90,0,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(
                1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());

        assertStatusCodeForGet(204, path + "&bbox=0,-90,180,90");
    }

    /**
     * Test that when a bogus regionating strategy is requested things still work. TODO: Evaluate
     * whether an error message should be returned instead.
     */
    @Test
    public void testBogusRegionator() throws Exception {
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + MockData.DIVIDED_ROUTES.getPrefix()
                        + ":"
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&styles="
                        + MockData.DIVIDED_ROUTES.getLocalPart()
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:bogus";
        Document document = getAsDOM(path + "&bbox=0,-90,180,90", true);
        assertEquals("ServiceExceptionReport", document.getDocumentElement().getTagName());
    }

    /** Test whether geometries that cross tiles get put into both of them. */
    @Test
    public void testBigGeometries() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + CENTERED_POLY.getPrefix()
                        + ":"
                        + CENTERED_POLY.getLocalPart()
                        + "&styles="
                        + "&height=1024&width=1024&srs=EPSG:4326"
                        + "&format_options=regionateBy:external-sorting;regionateattr:foo";

        assertStatusCodeForGet(204, path + "&bbox=-180,-90,0,90");

        Document document = getAsDOM(path + "&bbox=0,-90,180,90");
        assertEquals("kml", document.getDocumentElement().getTagName());
        assertEquals(
                1, document.getDocumentElement().getElementsByTagName("Placemark").getLength());
    }

    /** Test whether specifying different regionating strategies changes the results. */
    @Test
    public void testStrategyChangesStuff() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + TILE_TESTS.getPrefix()
                        + ":"
                        + TILE_TESTS.getLocalPart()
                        + "&bbox=-180,-90,0,90&styles="
                        + "&height=1024&width=1024&srs=EPSG:4326";

        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().put("kml.regionateFeatureLimit", 2);
        getCatalog().save(fti);

        Document geo =
                getAsDOM(path + "&format_options=regionateBy:geometry;regionateattr:location");
        assertEquals("kml", geo.getDocumentElement().getTagName());

        NodeList geoPlacemarks = geo.getDocumentElement().getElementsByTagName("Placemark");
        assertEquals(2, geoPlacemarks.getLength());

        Document data =
                getAsDOM(path + "&format_options=regionateBy:external-sorting;regionateAttr:z");
        assertEquals("kml", data.getDocumentElement().getTagName());

        NodeList dataPlacemarks = data.getDocumentElement().getElementsByTagName("Placemark");
        assertEquals(2, dataPlacemarks.getLength());

        for (int i = 0; i < geoPlacemarks.getLength(); i++) {
            String geoName = ((Element) geoPlacemarks.item(i)).getAttribute("id");
            String dataName = ((Element) dataPlacemarks.item(i)).getAttribute("id");

            assertTrue(
                    geoName + " and " + dataName + " should not be the same!",
                    !geoName.equals(dataName));
        }
    }

    /** Test whether specifying different regionating strategies changes the results. */
    @Test
    public void testDuplicateAttribute() throws Exception {
        final String path =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + TILE_TESTS.getPrefix()
                        + ":"
                        + TILE_TESTS.getLocalPart()
                        + "&bbox=-180,-90,0,90&styles="
                        + "&height=1024&width=1024&srs=EPSG:4326";

        FeatureTypeInfo fti = getFeatureTypeInfo(TILE_TESTS);
        fti.getMetadata().put("kml.regionateFeatureLimit", 2);

        Document geo =
                getAsDOM(path + "&format_options=regionateBy:best_guess;regionateattr:the_geom");
        assertEquals("kml", geo.getDocumentElement().getTagName());
    }
}
