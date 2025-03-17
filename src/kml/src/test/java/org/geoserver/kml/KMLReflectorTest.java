/* (c) 2014 - 2017 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.awt.RenderingHints.Key;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.kml.regionate.CachedHierarchyRegionatingStrategy;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureListener;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.collection.AbstractFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Some functional tests for kml reflector
 *
 * @author David Winslow (OpenGeo)
 * @author Gabriel Roldan (OpenGeo)
 * @author Markus Innerebner (EURAC Research)
 * @version $Id$
 */
public class KMLReflectorTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("Bridge", "bridge.sld", getClass(), catalog);
        testData.addStyle("allsymbolizers", "allsymbolizers.sld", getClass(), catalog);
        testData.addStyle("labels", "labels.sld", getClass(), catalog);
        testData.addStyle("SingleFeature", "singlefeature.sld", getClass(), catalog);
        testData.addStyle("BridgeSubdir", "bridgesubdir.sld", getClass(), catalog);
        testData.addStyle("dynamicsymbolizer", "dynamicsymbolizer.sld", getClass(), catalog);
        testData.addStyle("relativeds", "relativeds.sld", getClass(), catalog);
        testData.addStyle("big-local-image", "big-local-image.sld", getClass(), catalog);
        testData.addStyle("big-mark", "big-mark.sld", getClass(), catalog);
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/bridge.png");
        testData.copyTo(getClass().getResourceAsStream("planet-42.png"), "styles/planet-42.png");
        File stylesDir = new File(testData.getDataDirectoryRoot(), "styles");
        new File(stylesDir, "graphics").mkdir();
        testData.copyTo(getClass().getResourceAsStream("bridge.png"), "styles/graphics/bridgesubdir.png");
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        File dir = getDataDirectory().findOrCreateDir("geosearch");
        CachedHierarchyRegionatingStrategy.clearAllHsqlDatabases(dir);
        super.onTearDown(testData);
    }

    /**
     * Verify that NetworkLink's generated by the reflector do not include a BBOX parameter, since that would override
     * the BBOX provided by Google Earth.
     *
     * @see <a href="https://osgeo-org.atlassian.net/browse/GEOS-2185">GEOS-2185</a>
     */
    @Test
    public void testNoBBOXInHREF() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getLocalPart();
        final XpathEngine xpath = XMLUnit.newXpathEngine();
        String requestURL = "wms/kml?mode=refresh&layers=" + layerName;
        Document dom = getAsDOM(requestURL);
        print(dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document)", dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document/kml:NetworkLink)", dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document/kml:LookAt)", dom);

        assertXpathEvaluatesTo(layerName, "kml:kml/kml:Document/kml:NetworkLink[1]/kml:name", dom);
        assertXpathEvaluatesTo("1", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:open", dom);
        assertXpathEvaluatesTo("1", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:visibility", dom);

        assertXpathEvaluatesTo("onStop", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:viewRefreshMode", dom);
        assertXpathEvaluatesTo("1.0", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:viewRefreshTime", dom);
        assertXpathEvaluatesTo("1.0", "kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:viewBoundScale", dom);
        Map<String, Object> expectedKVP = KvpUtils.parseQueryString(
                "http://localhost:80/geoserver/wms?format_options=MODE%3Arefresh%3Bautofit%3Atrue%3BKMPLACEMARK%3Afalse%3BKMATTR%3Atrue%3BKMSCORE%3A40%3BSUPEROVERLAY%3Afalse&service=wms&srs=EPSG%3A4326&width=2048&styles=BasicPolygons&height=2048&transparent=false&request=GetMap&layers=cite%3ABasicPolygons&format=application%2Fvnd.google-earth.kml+xml&version=1.1.1");
        Map<String, Object> resultedKVP = KvpUtils.parseQueryString(
                xpath.evaluate("kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:href", dom));

        assertMapsEqual(expectedKVP, resultedKVP);

        String href = xpath.evaluate("kml:kml/kml:Document/kml:NetworkLink/kml:Link/kml:href", dom);
        Pattern badPattern = Pattern.compile("&bbox=", Pattern.CASE_INSENSITIVE);
        assertFalse(badPattern.matcher(href).matches());
    }

    /**
     * Verify that NetworkLink's generated by the reflector do not include a BBOX parameter, since that would override
     * the BBOX provided by Google Earth.
     *
     * @see <a href="https://osgeo-org.atlassian.net/browse/GEOS-2185">GEOS-2185</a>
     */
    @Test
    public void testBBOXInHREF() throws Exception {
        final XpathEngine xpath = XMLUnit.newXpathEngine();
        String requestURL =
                "wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + "&bbox=-1,-1,-0.5,-0.5&mode=download";

        Document dom = getAsDOM(requestURL);
        // print(dom);

        assertEquals(1, xpath.getMatchingNodes("//kml:Placemark", dom).getLength());
    }

    @Test
    public void testDownloadMultiLayer() throws Exception {
        String requestURL = "wms/kml?&layers=" + getLayerId(MockData.LAKES) + "," + getLayerId(MockData.FORESTS);
        MockHttpServletResponse response = getAsServletResponse(requestURL);
        assertEquals(KMLMapOutputFormat.MIME_TYPE, response.getContentType());
        assertEquals("attachment; filename=cite-Lakes_cite-Forests.kml", response.getHeader("Content-Disposition"));
        Document dom = dom(getBinaryInputStream(response));
        print(dom);
        assertXpathEvaluatesTo("1", "count(kml:kml/kml:Document)", dom);
        assertXpathEvaluatesTo("2", "count(kml:kml/kml:Document/kml:NetworkLink)", dom);
        assertXpathEvaluatesTo("2", "count(kml:kml/kml:Document/kml:NetworkLink/kml:LookAt)", dom);
    }

    /** Do some spot checks on the KML generated when an overlay hierarchy is requested. */
    @Test
    public void testSuperOverlayReflection() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        final String requestUrl = "wms/kml?layers=" + layerName + "&styles=&mode=superoverlay";
        Document dom = getAsDOM(requestUrl);
        // print(dom);
        assertEquals("kml", dom.getDocumentElement().getLocalName());
        assertXpathExists("kml:kml/kml:Document/kml:Folder/kml:NetworkLink/kml:Link/kml:href", dom);
        assertXpathExists("kml:kml/kml:Document/kml:LookAt/kml:longitude", dom);
    }

    @Test
    public void testWmsRepeatedLayerWithNonStandardStyleAndCqlFiler() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();
        final String titleName = MockData.BASIC_POLYGONS.getLocalPart();
        final String abstractValue = "abstract about " + titleName;

        String requestUrl = "wms/kml?mode=refresh&layers="
                + layerName
                + ","
                + layerName
                + "&styles=Default,Default&cql_filter=att1<10;att1>1000";
        Document dom = getAsDOM(requestUrl);

        assertEquals("kml", dom.getDocumentElement().getLocalName());

        assertXpathEvaluatesTo("2", "count(kml:kml/kml:Document/kml:NetworkLink)", dom);
        assertXpathEvaluatesTo(titleName, "kml:kml/kml:Document/kml:NetworkLink[1]/kml:name", dom);
        assertXpathEvaluatesTo(abstractValue, "kml:kml/kml:Document/kml:NetworkLink[1]/kml:description", dom);
        assertXpathEvaluatesTo(titleName, "kml:kml/kml:Document/kml:NetworkLink[2]/kml:name", dom);
        assertXpathEvaluatesTo(abstractValue, "kml:kml/kml:Document/kml:NetworkLink[2]/kml:description", dom);

        XpathEngine xpath = XMLUnit.newXpathEngine();

        String url1 = xpath.evaluate("/kml:kml/kml:Document/kml:NetworkLink[1]/kml:Url/kml:href", dom);
        String url2 = xpath.evaluate("/kml:kml/kml:Document/kml:NetworkLink[2]/kml:Url/kml:href", dom);

        assertNotNull(url1);
        assertNotNull(url2);

        Map<String, Object> kvp1 = KvpUtils.parseQueryString(url1);
        Map<String, Object> kvp2 = KvpUtils.parseQueryString(url2);

        assertEquals(layerName, kvp1.get("layers"));
        assertEquals(layerName, kvp2.get("layers"));

        assertEquals("Default", kvp1.get("styles"));
        assertEquals("Default", kvp2.get("styles"));

        assertEquals("att1<10", kvp1.get("cql_filter"));
        assertEquals("att1>1000", kvp2.get("cql_filter"));
    }

    /** @see {@link KMLReflector#organizeFormatOptionsParams(Map, Map)} */
    @Test
    public void testKmlFormatOptionsAsKVP() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        final String baseUrl = "wms/kml?layers=" + layerName + "&styles=&mode=superoverlay";
        final String requestUrl = baseUrl + "&kmltitle=myCustomLayerTitle&kmscore=10&legend=true&kmattr=true";
        Document dom = getAsDOM(requestUrl);
        XpathEngine xpath = XMLUnit.newXpathEngine();

        // print(dom);
        // all the kvp parameters (which should be set as format_options now are correctly parsed)
        String result = xpath.evaluate("//kml:NetworkLink/kml:Link/kml:href", dom);
        Map<String, Object> kvp = KvpUtils.parseQueryString(result);
        List<String> formatOptions = Arrays.asList(((String) kvp.get("format_options")).split(";"));
        assertEquals(9, formatOptions.size());
        assertTrue(formatOptions.contains("LEGEND:true"));
        assertTrue(formatOptions.contains("SUPEROVERLAY:true"));
        assertTrue(formatOptions.contains("AUTOFIT:true"));
        assertTrue(formatOptions.contains("KMPLACEMARK:false"));
        assertTrue(formatOptions.contains("OVERLAYMODE:auto"));
        assertTrue(formatOptions.contains("KMSCORE:10"));
        assertTrue(formatOptions.contains("MODE:superoverlay"));
        assertTrue(formatOptions.contains("KMATTR:true"));
        assertTrue(formatOptions.contains("KMLTITLE:myCustomLayerTitle"));
    }

    @Test
    public void testKmlTitleFormatOption() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        final String requestUrl =
                "wms/kml?layers=" + layerName + "&styles=&mode=superoverlay&format_options=kmltitle:myCustomLayerTitle";
        // System.out.println(getAsServletResponse(requestUrl).getContentType());
        Document dom = getAsDOM(requestUrl);
        // print(dom);
        assertEquals("kml", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("myCustomLayerTitle", "/kml:kml/kml:Document/kml:name", dom);
    }

    @Test
    public void testKmlRefreshFormatOption() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        String requestUrl = "wms/kml?layers=" + layerName + "&format_options=kmlrefresh:expires";
        Document dom = getAsDOM(requestUrl);

        // print(dom);
        assertEquals("kml", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("onExpire", "/kml:kml/kml:Document/kml:NetworkLink/kml:Url/kml:refreshMode", dom);

        requestUrl = "wms/kml?layers=" + layerName + "&format_options=kmlrefresh:60";
        dom = getAsDOM(requestUrl);
        assertXpathEvaluatesTo("onInterval", "/kml:kml/kml:Document/kml:NetworkLink/kml:Url/kml:refreshMode", dom);
        assertXpathEvaluatesTo("60.0", "/kml:kml/kml:Document/kml:NetworkLink/kml:Url/kml:refreshInterval", dom);
    }

    @Test
    public void testKmlVisibleFormatOption() throws Exception {
        final String layerName = MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart();

        String requestUrl = "wms/kml?layers=" + layerName + "&format_options=kmlvisible:true";
        Document dom = getAsDOM(requestUrl);

        // print(dom);
        assertEquals("kml", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("1", "/kml:kml/kml:Document/kml:NetworkLink/kml:visibility", dom);

        requestUrl = "wms/kml?layers=" + layerName + "&format_options=kmlvisible:false";
        dom = getAsDOM(requestUrl);
        assertEquals("kml", dom.getDocumentElement().getLocalName());
        assertXpathEvaluatesTo("0", "/kml:kml/kml:Document/kml:NetworkLink/kml:visibility", dom);
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-1947 */
    @Test
    public void testExternalGraphicBackround() throws Exception {
        final String requestUrl = "wms/kml?layers=" + getLayerId(MockData.BRIDGES) + "&styles=Bridge&mode=download";
        Document dom = getAsDOM(requestUrl);
        // print(dom);

        // make sure we are generating icon styles, but that we're not sticking a color onto them
        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Style/kml:IconStyle/kml:Icon/kml:href)", dom);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//kml:Style/kml:IconStyle/kml:Icon/kml:color)", dom);
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-3994 */
    @Test
    public void testExternalGraphicSubdir() throws Exception {
        final String requestUrl =
                "wms/kml?layers=" + getLayerId(MockData.BRIDGES) + "&styles=BridgeSubdir&mode=download";
        Document dom = getAsDOM(requestUrl);
        // print(dom);
        // make sure we are generating icon styles with the subdir path
        XMLAssert.assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/styles/graphics/bridgesubdir.png",
                "//kml:Style[1]/kml:IconStyle/kml:Icon/kml:href",
                dom);
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-3965 */
    @Test
    public void testProxyBaseURL() throws Exception {
        GeoServer gs = getGeoServer();
        try {
            GeoServerInfo info = gs.getGlobal();
            info.getSettings().setProxyBaseUrl("http://myhost:9999/gs");
            gs.save(info);

            final String requestUrl = "wms/kml?layers=" + getLayerId(MockData.BRIDGES) + "&styles=Bridge&mode=download";
            Document dom = getAsDOM(requestUrl);

            // make sure we are using the proxy base URL
            XMLAssert.assertXpathEvaluatesTo(
                    "http://myhost:9999/gs/styles/bridge.png", "//kml:Style/kml:IconStyle/kml:Icon/kml:href", dom);
        } finally {
            GeoServerInfo info = gs.getGlobal();
            info.getSettings().setProxyBaseUrl(null);
            gs.save(info);
        }
    }

    @Test
    public void testFilteredData() throws Exception {
        // the style selects a single feature
        final String requestUrl =
                "wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + "&styles=SingleFeature&mode=download";
        Document dom = getAsDOM(requestUrl);
        // print(dom);

        // check we have indeed a single feature
        assertXpathEvaluatesTo("1", "count(//kml:Placemark)", dom);
    }

    @Test
    public void testForceRasterKml() throws Exception {
        final String requestUrl = "wms/reflect?layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&format_options=KMSCORE:0;mode:refresh&format= "
                + KMLMapOutputFormat.MIME_TYPE;
        Document dom = getAsDOM(requestUrl);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:GroundOverlay)", dom);
        String href = XMLUnit.newXpathEngine().evaluate("//kml:Folder/kml:GroundOverlay/kml:Icon/kml:href", dom);
        assertTrue(href.startsWith("http://localhost:8080/geoserver/wms"));
        assertTrue(href.contains("request=GetMap"));
        assertTrue(href.contains("format=image%2Fpng"));
    }

    @Test
    public void testForceRasterKmz() throws Exception {
        final String requestUrl = "wms/reflect?layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&format_options=KMSCORE:0;mode:refresh&format= "
                + KMZMapOutputFormat.MIME_TYPE;
        MockHttpServletResponse response = getAsServletResponse(requestUrl);
        assertEquals(KMZMapOutputFormat.MIME_TYPE, response.getContentType());
        assertEquals("attachment; filename=cite-BasicPolygons.kmz", response.getHeader("Content-Disposition"));

        try (ZipInputStream zis = new ZipInputStream(getBinaryInputStream(response))) {
            // first entry, the kml document itself
            ZipEntry entry = zis.getNextEntry();
            assertEquals("wms.kml", entry.getName());
            // we need to clone the input stream, as dom(is) closes the stream
            byte[] data = IOUtils.toByteArray(zis);
            Document dom = dom(new ByteArrayInputStream(data));
            assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:GroundOverlay)", dom);
            String href = XMLUnit.newXpathEngine().evaluate("//kml:Folder/kml:GroundOverlay/kml:Icon/kml:href", dom);
            assertEquals("images/layers_0.png", href);
            zis.closeEntry();

            // the images folder
            entry = zis.getNextEntry();
            assertEquals("images/", entry.getName());
            zis.closeEntry();

            // the ground overlay for the raster layer
            entry = zis.getNextEntry();
            assertEquals("images/layers_0.png", entry.getName());
            zis.closeEntry();
            assertNull(zis.getNextEntry());
        }
    }

    @Test
    public void testRasterTransformerSLD() throws Exception {
        URL url = getClass().getResource("allsymbolizers.sld");
        String urlExternal = URLDecoder.decode(url.toExternalForm(), "UTF-8");
        final String requestUrl = "wms/reflect?layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&format_options=KMSCORE:0;mode:refresh&format= "
                + KMLMapOutputFormat.MIME_TYPE
                + "&sld="
                + urlExternal;

        Document dom = getAsDOM(requestUrl);
        // print(dom);

        assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:GroundOverlay)", dom);
        String href = XMLUnit.newXpathEngine().evaluate("//kml:Folder/kml:GroundOverlay/kml:Icon/kml:href", dom);
        href = URLDecoder.decode(href, "UTF-8");
        assertTrue(href.startsWith("http://localhost:8080/geoserver/wms"));
        assertTrue(href.contains("request=GetMap"));
        assertTrue(href.contains("format=image/png"));
        assertTrue(href.contains("&sld=" + urlExternal));
    }

    @Test
    public void testRasterPlacemarkTrue() throws Exception {
        doTestRasterPlacemark(true);
    }

    @Test
    public void testRasterPlacemarkFalse() throws Exception {
        doTestRasterPlacemark(false);
    }

    protected void doTestRasterPlacemark(boolean doPlacemarks) throws Exception {
        // the style selects a single feature
        final String requestUrl = "wms/reflect?layers="
                + getLayerId(MockData.BASIC_POLYGONS)
                + "&styles=&format_options=mode:refresh;kmscore:0;kmplacemark:"
                + doPlacemarks
                + "&format="
                + KMZMapOutputFormat.MIME_TYPE;
        MockHttpServletResponse response = getAsServletResponse(requestUrl);
        assertEquals(KMZMapOutputFormat.MIME_TYPE, response.getContentType());

        // create the kmz
        File tempDir = org.geoserver.util.IOUtils.createRandomDirectory("./target", "kmplacemark", "test");
        tempDir.deleteOnExit();

        File zip = new File(tempDir, "kmz.zip");
        zip.deleteOnExit();

        try (FileOutputStream output = new FileOutputStream(zip)) {
            FileUtils.writeByteArrayToFile(zip, getBinary(response));
            output.flush();
        }

        assertTrue(zip.exists());

        // unzip and test it
        try (ZipFile zipFile = new ZipFile(zip)) {
            ZipEntry entry = zipFile.getEntry("wms.kml");
            assertNotNull(entry);
            assertNotNull(zipFile.getEntry("images/layers_0.png"));

            // unzip the wms.kml to file
            byte[] buffer = new byte[1024];
            int len;

            File temp = File.createTempFile("test_out", "kmz", tempDir);
            temp.deleteOnExit();
            try (InputStream inStream = zipFile.getInputStream(entry);
                    BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(temp))) {

                while ((len = inStream.read(buffer)) >= 0) outStream.write(buffer, 0, len);
            }

            // read in the wms.kml and check its contents
            Document document = dom(new BufferedInputStream(new FileInputStream(temp)));
            // print(document);

            assertEquals("kml", document.getDocumentElement().getNodeName());
            if (doPlacemarks) {
                assertEquals(
                        getFeatureSource(MockData.BASIC_POLYGONS).getFeatures().size(),
                        document.getElementsByTagName("Placemark").getLength());
                XMLAssert.assertXpathEvaluatesTo("3", "count(//kml:Placemark//kml:Point)", document);
            } else {
                assertEquals(0, document.getElementsByTagName("Placemark").getLength());
            }
        }
    }

    @Test
    public void testStyleConverter() throws Exception {
        // the style selects a single feature
        final String requestUrl =
                "wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + "&styles=allsymbolizers&mode=download";
        Document doc = getAsDOM(requestUrl);
        // print(doc);

        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Placemark[1]/kml:Style)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0", "count(//kml:Placemark[1]/kml:Style/kml:IconStyle/kml:Icon/kml:color)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/kml/icon/allsymbolizers?0.0.0=",
                "//kml:Placemark[1]/kml:Style/kml:IconStyle/kml:Icon/kml:href",
                doc);
        XMLAssert.assertXpathEvaluatesTo("b24d4dff", "//kml:Placemark[1]/kml:Style/kml:PolyStyle/kml:color", doc);
        XMLAssert.assertXpathEvaluatesTo("ffba3e00", "//kml:Placemark[1]/kml:Style/kml:LineStyle/kml:color", doc);
        XMLAssert.assertXpathEvaluatesTo("2.0", "//kml:Placemark[1]/kml:Style/kml:LineStyle/kml:width", doc);
        XMLAssert.assertXpathEvaluatesTo("1.4", "//kml:Placemark[1]/kml:Style/kml:LabelStyle/kml:scale", doc);
    }

    @Test
    public void testLabelFromTextSymbolizer() throws Exception {
        // the style selects a single feature
        final String requestUrl =
                "wms/kml?layers=" + getLayerId(MockData.NAMED_PLACES) + "&styles=labels&mode=download";
        Document doc = getAsDOM(requestUrl);
        // print(doc);

        XMLAssert.assertXpathEvaluatesTo("2", "count(//kml:Placemark)", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Placemark[kml:name='Ashton'])", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Placemark[kml:name='Goose Island'])", doc);
    }

    /** See https://osgeo-org.atlassian.net/browse/GEOS-2670 */
    @Test
    public void testDynamicSymbolizer() throws Exception {
        final String requestUrl =
                "wms/kml?layers=" + getLayerId(MockData.STREAMS) + "&styles=dynamicsymbolizer&mode=download";
        Document document = getAsDOM(requestUrl);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "http://127.0.0.1/Cam Stream", "//kml:Style[1]/kml:IconStyle/kml:Icon/kml:href", document);
    }

    @Test
    public void testRelativeDynamicSymbolizer() throws Exception {
        final String requestUrl = "wms/kml?layers=" + getLayerId(MockData.STREAMS) + "&styles=relativeds&mode=download";
        Document document = getAsDOM(requestUrl);

        assertEquals("kml", document.getDocumentElement().getNodeName());
        XMLAssert.assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/styles/icons/Cam%20Stream",
                "//kml:Style[1]/kml:IconStyle/kml:Icon/kml:href", document);
    }

    @Test
    public void testLegend() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        final String requestUrl = "wms/kml?layers="
                + layerId
                + "&styles=polygon&mode=download&format_options=legend:true" //
                + "&legend_options=fontStyle:bold;fontColor:ff0000;fontSize:18";
        Document doc = getAsDOM(requestUrl);
        // print(doc);

        assertEquals("kml", doc.getDocumentElement().getNodeName());

        // the icon itself
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String href = xpath.evaluate("//kml:ScreenOverlay/kml:Icon/kml:href", doc);
        assertTrue(href.contains("request=GetLegendGraphic"));
        assertTrue(href.contains("layer=cite%3ABasicPolygons"));
        assertTrue(href.contains("style=polygon"));
        assertTrue(href.contains("LEGEND_OPTIONS=fontStyle%3Abold%3BfontColor%3Aff0000%3BfontSize%3A18"));

        // overlay location
        XMLAssert.assertXpathEvaluatesTo("0.0", "//kml:ScreenOverlay/kml:overlayXY/@x", doc);
        XMLAssert.assertXpathEvaluatesTo("0.0", "//kml:ScreenOverlay/kml:overlayXY/@y", doc);
        XMLAssert.assertXpathEvaluatesTo("pixels", "//kml:ScreenOverlay/kml:overlayXY/@xunits", doc);
        XMLAssert.assertXpathEvaluatesTo("pixels", "//kml:ScreenOverlay/kml:overlayXY/@yunits", doc);
        XMLAssert.assertXpathEvaluatesTo("10.0", "//kml:ScreenOverlay/kml:screenXY/@x", doc);
        XMLAssert.assertXpathEvaluatesTo("20.0", "//kml:ScreenOverlay/kml:screenXY/@y", doc);
        XMLAssert.assertXpathEvaluatesTo("pixels", "//kml:ScreenOverlay/kml:screenXY/@xunits", doc);
        XMLAssert.assertXpathEvaluatesTo("pixels", "//kml:ScreenOverlay/kml:screenXY/@yunits", doc);
    }

    @Test
    public void testLookatOptions() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        final String requestUrl = "wms/kml?layers="
                + layerId
                + "&styles=polygon&mode=download"
                + "&format_options=lookatbbox:-20,-20,20,20;altitude:10;heading:0;tilt:30;range:100;altitudemode:absolute";
        Document doc = getAsDOM(requestUrl);
        // print(doc);

        // overlay location
        XMLAssert.assertXpathEvaluatesTo("0.0", "//kml:Document/kml:LookAt/kml:longitude", doc);
        XMLAssert.assertXpathEvaluatesTo("0.0", "//kml:Document/kml:LookAt/kml:latitude", doc);
        XMLAssert.assertXpathEvaluatesTo("10.0", "//kml:Document/kml:LookAt/kml:altitude", doc);
        XMLAssert.assertXpathEvaluatesTo("0.0", "//kml:Document/kml:LookAt/kml:heading", doc);
        XMLAssert.assertXpathEvaluatesTo("30.0", "//kml:Document/kml:LookAt/kml:tilt", doc);
        XMLAssert.assertXpathEvaluatesTo("100.0", "//kml:Document/kml:LookAt/kml:range", doc);
        XMLAssert.assertXpathEvaluatesTo("absolute", "//kml:Document/kml:LookAt/kml:altitudeMode", doc);
    }

    @Test
    public void testExtendedData() throws Exception {
        String layerId = getLayerId(MockData.AGGREGATEGEOFEATURE);
        final String requestUrl =
                "wms/kml?layers=" + layerId + "&mode=download&extendedData=true&kmattr=false&kmscore=100";
        Document doc = getAsDOM(requestUrl);

        // print(doc);

        // there is one schema
        XMLAssert.assertXpathEvaluatesTo("1", "count(//kml:Document/kml:Schema)", doc);
        // check we only have the non geom properties
        XMLAssert.assertXpathEvaluatesTo("6", "count(//kml:Document/kml:Schema/kml:SimpleField)", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0", "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiPointProperty'])", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0", "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiCurveProperty'])", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0", "count(//kml:Document/kml:Schema/kml:SimpleField[@name='multiSurfaceProperty'])", doc);
        // check the type mapping
        XMLAssert.assertXpathEvaluatesTo(
                "string", "//kml:Document/kml:Schema/kml:SimpleField[@name='description']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "double", "//kml:Document/kml:Schema/kml:SimpleField[@name='doubleProperty']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "int", "//kml:Document/kml:Schema/kml:SimpleField[@name='intRangeProperty']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "string", "//kml:Document/kml:Schema/kml:SimpleField[@name='strProperty']/@type", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "string", "//kml:Document/kml:Schema/kml:SimpleField[@name='featureCode']/@type", doc);

        // check the extended data of one feature
        String sd = "//kml:Placemark[@id='AggregateGeoFeature.f005']/kml:ExtendedData/kml:SchemaData/kml:SimpleData";
        XMLAssert.assertXpathEvaluatesTo("description-f005", sd + "[@name='description']", doc);
        XMLAssert.assertXpathEvaluatesTo("name-f005", sd + "[@name='name']", doc);
        XMLAssert.assertXpathEvaluatesTo("2012.78", sd + "[@name='doubleProperty']", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "Ma quande lingues coalesce, li grammatica del resultant "
                        + "lingue es plu simplic e regulari quam ti del coalescent lingues. Li nov lingua "
                        + "franca va esser plu simplic e regulari quam li existent Europan lingues.",
                sd + "[@name='strProperty']",
                doc);
        XMLAssert.assertXpathEvaluatesTo("BK030", sd + "[@name='featureCode']", doc);
    }

    @Test
    public void testHeightTemplate() throws Exception {
        File template = null;
        try {
            String layerId = getLayerId(MockData.LAKES);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, "height.ftl");
            FileUtils.write(template, "${FID.value}", "UTF-8");

            final String requestUrl = "wms/kml?layers=" + layerId + "&mode=download";
            Document doc = getAsDOM(requestUrl);
            // print(doc);

            String base = "//kml:Placemark[@id='Lakes.1107531835962']/kml:MultiGeometry";
            XMLAssert.assertXpathEvaluatesTo("1", "count(" + base + ")", doc);
            XMLAssert.assertXpathEvaluatesTo("1", base + "/kml:Point/kml:extrude", doc);
            XMLAssert.assertXpathEvaluatesTo("relativeToGround", base + "/kml:Point/kml:altitudeMode", doc);
            XMLAssert.assertXpathEvaluatesTo(
                    "0.0017851936218678816,-0.0010838268792710709,101.0", base + "/kml:Point/kml:coordinates", doc);
            XMLAssert.assertXpathEvaluatesTo("1", base + "/kml:Polygon/kml:extrude", doc);
            XMLAssert.assertXpathEvaluatesTo("relativeToGround", base + "/kml:Polygon/kml:altitudeMode", doc);

            assertXPathCoordinates(
                    "LinearRing",
                    "6.0E-4,-0.0018,101.0 0.0010,-6.0E-4,101.0 0.0024,-1.0E-4,101.0 0.0031,-0.0015,101.0 6.0E-4,-0.0018,101.0",
                    base + "/kml:Polygon/kml:outerBoundaryIs/kml:LinearRing/kml:coordinates",
                    doc);
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    @Test
    public void testHeightTemplatePoint() throws Exception {
        File template = null;
        try {
            String layerId = getLayerId(MockData.POINTS);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, "height.ftl");
            FileUtils.write(template, "${altitude.value}", "UTF-8");

            final String requestUrl = "wms/kml?layers=" + layerId + "&mode=download";
            Document doc = getAsDOM(requestUrl);

            String base = "//kml:Placemark[@id='Points.0']/kml:Point";
            XMLAssert.assertXpathEvaluatesTo("1", "count(" + base + ")", doc);
            XMLAssert.assertXpathEvaluatesTo("1", base + "/kml:extrude", doc);
            XMLAssert.assertXpathEvaluatesTo("relativeToGround", base + "/kml:altitudeMode", doc);
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    private void assertXPathCoordinates(String message, String expectedText, String xpath, Document doc)
            throws XpathException {
        XpathEngine engine = XMLUnit.newXpathEngine();
        String text = engine.evaluate(xpath, doc);
        if (equalsRegardingNull(expectedText, text)) {
            return;
        }
        if (expectedText != null && text != null) {
            String[] expectedCoordinates = expectedText.split("(\\s|,)");
            String[] actualCoordiantes = text.split("(\\s|,)");
            if (expectedCoordinates.length == actualCoordiantes.length) {
                final int LENGTH = actualCoordiantes.length;
                boolean checked = true;
                LIST:
                for (int i = 0; i < LENGTH; i++) {
                    String expected = expectedCoordinates[i];
                    String actual = actualCoordiantes[i];
                    if (expected.length() == actual.length()) {
                        if (!expected.equals(actual)) {
                            checked = false;
                            break LIST; // normal equals check will report issue
                        }
                    } else {
                        try {
                            double expectedOrdinate = Double.parseDouble(expected);
                            double actualOridnate = Double.parseDouble(actual);
                            if (Double.compare(expectedOrdinate, actualOridnate) != 0) {
                                // Could do a Math.abs(expectedOrdinate - actualOridnate) <= delta
                                // check
                                break LIST; // normal equals check will report issue
                            }
                        } catch (NumberFormatException formatException) {
                            checked = false;
                            break LIST; // normal equals check will report issue
                        }
                    }
                }
                if (checked) {
                    return; // double based comparison checked all elements
                }
            }
        }
        // call normal assertEquals for consistent failure message
        assertEquals(message, expectedText, text);
    }

    private boolean equalsRegardingNull(String expected, String actual) {
        if (expected == null) {
            return actual == null;
        }
        return expected.equals(actual); // fast string equals check
    }

    @Test
    public void testHeightTemplateNoExtrude() throws Exception {
        File template = null;
        try {
            String layerId = getLayerId(MockData.LAKES);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, "height.ftl");
            FileUtils.write(template, "${FID.value}", "UTF-8");

            final String requestUrl = "wms/kml?layers=" + layerId + "&mode=download&extrude=false";
            Document doc = getAsDOM(requestUrl);
            // print(doc);

            String base = "//kml:Placemark[@id='Lakes.1107531835962']/kml:MultiGeometry";
            XMLAssert.assertXpathEvaluatesTo("1", "count(" + base + ")", doc);
            XMLAssert.assertXpathEvaluatesTo("0", base + "/kml:Point/kml:extrude", doc);
            XMLAssert.assertXpathEvaluatesTo("relativeToGround", base + "/kml:Point/kml:altitudeMode", doc);
            XMLAssert.assertXpathEvaluatesTo(
                    "0.0017851936218678816,-0.0010838268792710709,101.0", base + "/kml:Point/kml:coordinates", doc);
            XMLAssert.assertXpathEvaluatesTo("0", base + "/kml:Polygon/kml:extrude", doc);
            XMLAssert.assertXpathEvaluatesTo("relativeToGround", base + "/kml:Polygon/kml:altitudeMode", doc);

            // Coordinate Formatting in JDK 1.7.0 does not include trailing 0 - see GEOS-5973
            // JDK 1.6: 0.0010
            // JDK 1.7: 0.001
            assertXPathCoordinates(
                    "kml:LinearRing",
                    "6.0E-4,-0.0018,101.0 0.001,-6.0E-4,101.0 0.0024,-1.0E-4,101.0 0.0031,-0.0015,101.0 6.0E-4,-0.0018,101.0",
                    base + "/kml:Polygon/kml:outerBoundaryIs/kml:LinearRing/kml:coordinates",
                    doc);
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    /** Verify that when GE asks for coordinates larger than 180 we still manage gracefully */
    @Test
    public void testCoordinateShift() throws Exception {
        Document document = getAsDOM(
                "wms/kml?layers=" + getLayerId(MockData.BASIC_POLYGONS) + "&mode=download&bbox=150,-90,380,90");
        // print(document);

        assertEquals(3, document.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testValidKML() throws Exception {
        GetMapRequest req = createGetMapRequest(MockData.STREAMS);
        req.setWidth(256);
        req.setHeight(256);

        WMSMapContent mapContent = new WMSMapContent(req);
        mapContent.addLayer(createMapLayer(MockData.STREAMS, "big-local-image"));

        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90, DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        KMLMapOutputFormat of = new KMLMapOutputFormat(getWMS());
        KMLMap map = of.produceMap(mapContent);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new KMLEncoder().encode(map.getKml(), bout, null);

        // Explicitly check for known bugs in the JavaAPIforKml library.
        // https://osgeo-org.atlassian.net/browse/GEOS-7963
        // https://github.com/micromata/javaapiforkml/issues/9
        Document document = dom(new ByteArrayInputStream(bout.toByteArray()));
        // print(document);
        XMLAssert.assertXpathNotExists("//kml:IconStyle/kml:Icon/kml:refreshInterval", document);
        XMLAssert.assertXpathNotExists("//kml:IconStyle/kml:Icon/kml:viewRefreshTime", document);
        XMLAssert.assertXpathNotExists("//kml:IconStyle/kml:Icon/kml:viewBoundScale", document);

        // Validate against the KML 2.2 schema.
        Unmarshaller unmarshaller = JAXBContext.newInstance(Kml.class).createUnmarshaller();
        unmarshaller.setSchema(
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new Source[] {
                    new StreamSource(
                            getClass().getResource("/org/geoserver/kml/xAL.xsd").toExternalForm()),
                    new StreamSource(getClass()
                            .getResource("/schema/ogckml/ogckml22.xsd")
                            .toExternalForm())
                }));
        unmarshaller.unmarshal(document);
    }

    // This unit test is provided for manual testing by a developer and is ignored by default since it can take several
    // minutes or longer to either run successfully or throw an OutOfMemoryError. This test may not throw an OOM with
    // the old XSLT-based transformation if the JVM memory limit is sufficiently high.
    @Ignore
    @Test
    public void testKmlTransformationMemoryUsage() throws Exception {
        // create a dummy feature source with a fixed number of features
        int size = 10000000;
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName("test");
        ftb.setSRS("EPSG:4326");
        ftb.add("geom", Point.class);
        SimpleFeatureType ft = ftb.buildFeatureType();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(ft);
        fb.add(new GeometryFactory().createPoint(new Coordinate(0, 0)));
        SimpleFeatureSource fs = new SimpleFeatureSource() {

            @Override
            public void removeFeatureListener(FeatureListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<Key> getSupportedHints() {
                throw new UnsupportedOperationException();
            }

            @Override
            public SimpleFeatureType getSchema() {
                return ft;
            }

            @Override
            public QueryCapabilities getQueryCapabilities() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Name getName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ResourceInfo getInfo() {
                throw new UnsupportedOperationException();
            }

            @Override
            public DataAccess<SimpleFeatureType, SimpleFeature> getDataStore() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getCount(Query query) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ReferencedEnvelope getBounds(Query query) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public ReferencedEnvelope getBounds() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void addFeatureListener(FeatureListener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SimpleFeatureCollection getFeatures(Query query) throws IOException {
                return new AbstractFeatureCollection(ft) {

                    @Override
                    public int size() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    protected Iterator<SimpleFeature> openIterator() {
                        return new Iterator<>() {
                            int i = 0;

                            @Override
                            public SimpleFeature next() {
                                return fb.buildFeature(String.valueOf(i++));
                            }

                            @Override
                            public boolean hasNext() {
                                return i < size;
                            }
                        };
                    }

                    @Override
                    public ReferencedEnvelope getBounds() {
                        return new ReferencedEnvelope(-180, 0, -90, 90, DefaultGeographicCRS.WGS84);
                    }
                };
            }

            @Override
            public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public SimpleFeatureCollection getFeatures() throws IOException {
                throw new UnsupportedOperationException();
            }
        };

        GetMapRequest req = new GetMapRequest();
        req.setBaseUrl("http://localhost:8080/geoserver");
        req.setLayers(List.of(new MapLayerInfo(fs)));
        req.setRawKvp(new HashMap<>());
        WMSMapContent mapContent = new WMSMapContent(req);
        mapContent.addLayer(
                new FeatureLayer(fs, getCatalog().getStyleByName("point").getStyle()));
        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90, DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        KMLMapOutputFormat of = new KMLMapOutputFormat(getWMS());
        KMLMap map = of.produceMap(mapContent);
        new KMLEncoder().encode(map.getKml(), OutputStream.nullOutputStream(), null);
        // old XSLT implementation should throw an OutOfMemorError but new implementation should complete successfully
    }

    @Test
    public void testExternalImageSize() throws Exception {
        GetMapRequest req = createGetMapRequest(MockData.STREAMS);
        req.setWidth(256);
        req.setHeight(256);

        WMSMapContent mapContent = new WMSMapContent(req);
        mapContent.addLayer(createMapLayer(MockData.STREAMS, "big-local-image"));

        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 0, -90, 90, DefaultGeographicCRS.WGS84));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        KMLMapOutputFormat of = new KMLMapOutputFormat(getWMS());
        KMLMap map = of.produceMap(mapContent);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new KMLEncoder().encode(map.getKml(), bout, null);

        Document document = dom(new ByteArrayInputStream(bout.toByteArray()));

        assertEquals("kml", document.getDocumentElement().getNodeName());
        assertEquals(1, document.getElementsByTagName("Style").getLength());

        XMLAssert.assertXpathExists("//kml:IconStyle/kml:scale", document);

        XPath xPath = XPathFactory.newInstance().newXPath();
        initXPath(xPath);

        Double scale = (Double)
                xPath.evaluate("//kml:IconStyle/kml:scale", document.getDocumentElement(), XPathConstants.NUMBER);
        assertEquals(42d / 16d, scale, 0.01);
    }

    @Test
    public void testKmzEmbededPointImageSize() throws Exception {

        WMSMapContent mapContent = createMapContext(MockData.POINTS, "big-mark");

        File temp = File.createTempFile("test", "kmz", new File("target"));
        temp.delete();
        temp.mkdir();
        temp.deleteOnExit();

        File zip = new File(temp, "kmz.zip");
        zip.deleteOnExit();

        // create hte map producer
        KMZMapOutputFormat mapProducer = new KMZMapOutputFormat(getWMS());
        KMLMap map = mapProducer.produceMap(mapContent);

        try (FileOutputStream output = new FileOutputStream(zip)) {
            new KMLMapResponse(new KMLEncoder(), getWMS()).write(map, output, null);
            output.flush();
        }

        assertTrue(zip.exists());

        // unzip and test it
        try (ZipFile zipFile = new ZipFile(zip)) {
            ZipEntry kmlEntry = zipFile.getEntry("wms.kml");
            try (InputStream kmlStream = zipFile.getInputStream(kmlEntry)) {
                Document kmlResult = XMLUnit.buildTestDocument(new InputSource(kmlStream));

                Double scale = Double.parseDouble(XMLUnit.newXpathEngine()
                        .getMatchingNodes("(//kml:Style)[1]/kml:IconStyle/kml:scale", kmlResult)
                        .item(0)
                        .getTextContent());
                assertEquals(49d / 16d, scale, 0.01);
            }
        }
    }

    WMSMapContent createMapContext(QName layer, String style) throws Exception {

        // create a map context
        WMSMapContent mapContent = new WMSMapContent();
        mapContent.addLayer(createMapLayer(layer, style));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        GetMapRequest getMapRequest = createGetMapRequest(new QName[] {layer});
        getMapRequest.setWidth(256);
        getMapRequest.setHeight(256);

        mapContent.setRequest(getMapRequest);
        mapContent.getViewport().setBounds(new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84));
        return mapContent;
    }

    void initXPath(XPath xpath) {
        SimpleNamespaceContext ctx = new SimpleNamespaceContext();
        ctx.bindNamespaceUri("kml", "http://www.opengis.net/kml/2.2");
        xpath.setNamespaceContext(ctx);
    }

    /**
     * Creates a key/value pair map from the cgi parameters in the provided url
     *
     * @param url an url where all the cgi parameter values are url encoded
     * @return a map with the key value pairs from the url with all the parameter names in upper case
     */
    static Map<String, String> toKvp(String url) {
        if (url.indexOf('?') > 0) {
            url = url.substring(url.indexOf('?') + 1);
        }
        Map<String, String> kvpMap = new HashMap<>();

        String[] tuples = url.split("&");
        for (String tuple : tuples) {
            String[] kvp = tuple.split("=");
            String key = kvp[0].toUpperCase();
            String value = kvp.length > 1 ? kvp[1] : null;
            if (value != null) {
                try {
                    value = URLDecoder.decode(value, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
            kvpMap.put(key, value);
        }

        return kvpMap;
    }

    static void assertMapsEqual(Map<String, Object> expected, Map<String, Object> actual) throws Exception {
        for (Map.Entry<String, Object> entry : expected.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("format_options")) {
                FormatOptionsKvpParser parser = new FormatOptionsKvpParser();
                Map expectedFormatOptions = (Map) parser.parse((String) entry.getValue());
                Map actualFormatOptions = (Map) parser.parse((String) actual.get(entry.getKey()));

                for (Object o : expectedFormatOptions.entrySet()) {
                    Map.Entry formatOption = (Map.Entry) o;
                    assertEquals(formatOption.getValue(), actualFormatOptions.get(formatOption.getKey()));
                }

                for (Object key : actualFormatOptions.keySet()) {
                    assertTrue(
                            "found unexpected key '" + key + "' in format options",
                            expectedFormatOptions.containsKey(key));
                }

                // special treatment for the format options
            } else {
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
            }
        }

        for (String key : actual.keySet()) {
            assertTrue(expected.containsKey(key));
        }
    }

    /**
     * Method testLookatOptionsWithRefreshMode tests if the two altitude values are obtained from the corresponding
     * bounding box. The first value (//kml:Document/kml:LookAt/kml:altitude) is calculated from the initial bounding
     * box. The second value (//kml:Document/kml:NetworkLink/kml:LookAt/kml:altitude) is calculated from the bounding
     * box passed to the WMS request. Test fails if those values are identical.
     *
     * @see <a href="https://osgeo-org.atlassian.net/browse/GEOS-6410">GEOS-6410</a>
     */
    @Test
    public void testLookatOptionsWithRefreshMode() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        final String requestUrl =
                "wms/kml?layers=" + layerId + "&styles=polygon&mode=refresh&bbox=10.56,46.99,11.50,47.26";
        Document doc = getAsDOM(requestUrl);
        // we expect that those values should not be the same, because first value is obtained from
        // initial bbox of the layer, while the second value from the bbox of the request
        XMLAssert.assertXpathValuesNotEqual(
                "//kml:Document/kml:LookAt/kml:altitude",
                "//kml:Document/kml:NetworkLink/kml:LookAt/kml:altitude",
                doc);
    }

    /**
     * Method testWMSTimeRequest tests if the time parameter of the request is also passed to the KML WMS request.
     *
     * @see <a href="https://osgeo-org.atlassian.net/browse/GEOS-6411">GEOS-6411</a>
     */
    @Test
    public void testWMSTimeRequest() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        String expectedTS = "time=2014-03-01";
        final String requestUrl =
                "wms/kml?layers=" + layerId + "&styles=polygon&mode=refresh&bbox=10.56,46.99,11.50,47.26&" + expectedTS;
        Document doc = getAsDOM(requestUrl);
        // we expect that those values should not be the same, because first value is obtained from
        // initial bbox of the layer, while the second value from the bbox of the request

        NodeList nodes = doc.getElementsByTagName("href");
        for (int i = 0; i < nodes.getLength(); ++i) {
            Element e = (Element) nodes.item(i);
            String actualTS = e.getTextContent();
            Assert.assertTrue("Time parameter missing", actualTS.contains(expectedTS));
        }
    }

    /**
     * Method testWMSElevationRequest tests if the elevation parameter of the request is also passed to the KML WMS
     * request.
     *
     * @see <a href="https://osgeo-org.atlassian.net/browse/GEOS-6411">GEOS-6411</a>
     */
    @Test
    public void testWMSElevationRequest() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        String expectedTS = "elevation=500";
        final String requestUrl =
                "wms/kml?layers=" + layerId + "&styles=polygon&mode=refresh&bbox=10.56,46.99,11.50,47.26&" + expectedTS;
        Document doc = getAsDOM(requestUrl);
        // we expect that those values should not be the same, because first value is obtained from
        // initial bbox of the layer, while the second value from the bbox of the request

        NodeList nodes = doc.getElementsByTagName("href");
        for (int i = 0; i < nodes.getLength(); ++i) {
            Element e = (Element) nodes.item(i);
            String actualTS = e.getTextContent();
            Assert.assertTrue("Elevation parameter missing", actualTS.contains(expectedTS));
        }
    }
}
