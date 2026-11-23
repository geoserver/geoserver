/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.DownloadLinkHandler.CloseableLinksIterator;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.data.CloseableIterator;
import org.geotools.api.data.FileGroupProvider.FileGroup;
import org.geotools.api.data.FileResourceInfo;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.ows.OWS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DirectDownloadTest extends GeoServerSystemTestSupport {

    private static Map<String, String> TEST_NAMESPACES;

    private XpathEngine xpathEngine;

    @BeforeClass
    public static void configureXMLUnit() throws Exception {

        System.clearProperty("DefaultCatalogStore");

        TEST_NAMESPACES = new HashMap<>();
        TEST_NAMESPACES.put("csw", CSW.NAMESPACE);
        TEST_NAMESPACES.put("dc", DC.NAMESPACE);
        TEST_NAMESPACES.put("dct", DCT.NAMESPACE);
        TEST_NAMESPACES.put("csw", CSW.NAMESPACE);
        TEST_NAMESPACES.put("ows", OWS.NAMESPACE);
        TEST_NAMESPACES.put("ogc", OGC.NAMESPACE);
        TEST_NAMESPACES.put("gml", "http://www.opengis.net/gml");
        TEST_NAMESPACES.put("xlink", XLINK.NAMESPACE);
        TEST_NAMESPACES.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        TEST_NAMESPACES.put("xsd", "http://www.w3.org/2001/XMLSchema");
        TEST_NAMESPACES.put("xs", "http://www.w3.org/2001/XMLSchema");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(TEST_NAMESPACES));
    }

    public static String CSW_PREFIX = "csw";

    public static String CSW_URI = "http://www.opengis.net/csw/2.0.2";

    public static QName WATTEMP = new QName(CSW_URI, "watertemp", CSW_PREFIX);

    private static final String GET_RECORD_REQUEST = "csw?service=csw&version=2.0.2&request=GetRecords"
            + "&elementsetname=full&typeNames=csw:Record&resultType=results"
            + "&constraint=title=%27watertemp%27";

    private static final String DOWNLOAD_REQUEST = "ows?service=CSW&version=2.0.2&request=DirectDownload&resourceId=";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        enableDirectDownload();
    }

    private void enableDirectDownload() {
        final Catalog cat = getCatalog();
        String name = "watertemp";
        final CoverageInfo coverageInfo = cat.getCoverageByName(name);
        coverageInfo.setName(name);
        coverageInfo.setNativeName(name);
        // This line of code has been commented on purpose
        // in order to test the fix for GEOS-7290
        // coverageInfo.setNativeCoverageName(name);
        coverageInfo.setTitle(name);
        final MetadataMap metadata = coverageInfo.getMetadata();
        DirectDownloadSettings settings = new DirectDownloadSettings();
        settings.setDirectDownloadEnabled(true);

        // Setting 10K limit
        settings.setMaxDownloadSize(10);
        metadata.getMap().put(DirectDownloadSettings.DIRECTDOWNLOAD_KEY, settings);
        cat.save(coverageInfo);
    }

    @Test
    public void testGetRecordWithDirectDownloadLink() throws Exception {
        Document dom = getAsDOM(GET_RECORD_REQUEST);
        print(dom);

        // check we have the expected results
        assertXpathEvaluatesTo("1", "count(//csw:Record/dc:identifier)", dom);
        assertXpathEvaluatesTo("6", "count(//csw:Record/dct:references)", dom);
        assertXpathEvaluatesTo("csw:watertemp", "//csw:Record/dc:identifier", dom);

        NodeList nodes = getMatchingNodes("//csw:Record/dct:references", dom);
        int size = nodes.getLength();

        // Get direct download links
        Set<String> links = new HashSet<>();
        for (int i = 0; i < size; i++) {
            Node node = nodes.item(i);
            String link = node.getTextContent();
            if (link.toUpperCase().contains("DIRECTDOWNLOAD") && link.contains("TIME")) {
                links.add(link);
            }
        }

        // Get links from the reader
        final Catalog cat = getCatalog();
        String name = "watertemp";
        final CoverageInfo coverageInfo = cat.getCoverageByName(name);
        Set<String> generatedLinks = new HashSet<>();
        GridCoverage2DReader reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        FileResourceInfo resourceInfo = (FileResourceInfo) reader.getInfo(name);
        try (CloseableIterator<FileGroup> files = resourceInfo.getFiles(null)) {
            String baseLink = DownloadLinkHandler.LINK;
            MockHttpServletRequest request = createRequest(baseLink);
            baseLink = request.getRequestURL() + "?" + request.getQueryString();
            baseLink = baseLink.replace(
                            "${nameSpace}", coverageInfo.getNamespace().getName())
                    .replace("${layerName}", coverageInfo.getName())
                    .replace("${version}", "2.0.2");

            try (CloseableLinksIterator<String> iterator = new CloseableLinksIterator<>(baseLink, files)) {
                while (iterator.hasNext()) {
                    generatedLinks.add(iterator.next());
                }
            }
        }

        // Look for links matching
        Iterator<String> it = links.iterator();
        int matches = 0;
        while (it.hasNext()) {
            String cswLink = it.next();
            if (generatedLinks.contains(cswLink)) {
                matches++;
            }
        }
        assertEquals(4, matches);
    }

    @Test
    public void testDirectDownloadExceedingLimit() throws Exception {
        Document dom = getAsDOM(GET_RECORD_REQUEST);
        NodeList nodes = getMatchingNodes("//csw:Record/dct:references", dom);
        int size = nodes.getLength();
        String downloadLink = null;

        // Getting the fullDataset Download link
        for (int i = 0; i < size; i++) {
            Node node = nodes.item(i);
            String link = node.getTextContent();
            if (link.toUpperCase().contains("DIRECTDOWNLOAD") && !link.contains("TIME")) {
                downloadLink = link;
                break;
            }
        }
        downloadLink = downloadLink.substring(downloadLink.indexOf("ows?"));

        // The output will not be a zip.
        // The download will exceed the limit so it returns an exception
        MockHttpServletResponse response = getAsServletResponse(downloadLink);
        assertEquals("application/xml", response.getContentType());

        Document domResponse =
                dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        Element root = domResponse.getDocumentElement();
        assertEquals("ows:ExceptionReport", root.getNodeName());
        String exceptionText = evaluate("//ows:ExceptionReport/ows:Exception/ows:ExceptionText", domResponse);
        assertTrue(exceptionText.contains(DirectDownload.LIMIT_MESSAGE));
    }

    @Test
    public void testDirectDownloadFile() throws Exception {
        Document dom = getAsDOM(GET_RECORD_REQUEST);
        NodeList nodes = getMatchingNodes("//csw:Record/dct:references", dom);
        int size = nodes.getLength();

        // Getting a file downloadLink
        String link = null;
        for (int i = 0; i < size; i++) {
            Node node = nodes.item(i);
            link = node.getTextContent();
            if (link.toUpperCase().contains("DIRECTDOWNLOAD") && link.contains("TIME")) {
                break;
            }
        }
        link = link.substring(link.indexOf("ows?"));

        // The output will be a zip holding only the requested granule
        MockHttpServletResponse response = getAsServletResponse(link);
        assertEquals("application/zip", response.getContentType());
        String fileId = link.substring(link.indexOf("file=") + "file=".length()).split("&")[0];
        // the file id is the 40 chars SHA-1 hex digest, a "-" and the file name
        assertEquals(Set.of(fileId.substring(41)), getZipEntries(response));
    }

    private static Set<String> getZipEntries(MockHttpServletResponse response) throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    @Test
    public void testDirectDownloadMissingNamespace() throws Exception {
        assertInvalidParameter(
                DOWNLOAD_REQUEST + "watertemp", "resourceId", DirectDownload.INVALID_RESOURCE + "watertemp");
    }

    @Test
    public void testDirectDownloadUnknownLayer() throws Exception {
        assertInvalidParameter(
                DOWNLOAD_REQUEST + "csw:missing",
                "resourceId",
                DirectDownload.INVALID_COVERAGE_DOWNLOAD + "csw:missing");
    }

    @Test
    public void testDirectDownloadShortFileId() throws Exception {
        assertInvalidParameter(
                DOWNLOAD_REQUEST + "csw:watertemp&file=abc-watertemp.tiff",
                "file",
                DirectDownload.INVALID_FILE + "abc-watertemp.tiff");
    }

    @Test
    public void testDirectDownloadUnknownFileId() throws Exception {
        String fileId = "0".repeat(40) + "-watertemp.tiff";
        assertInvalidParameter(
                DOWNLOAD_REQUEST + "csw:watertemp&file=" + fileId, "file", DirectDownload.INVALID_FILE + fileId);
    }

    @Test
    public void testDirectDownloadLayerOverride() throws Exception {
        String request = DOWNLOAD_REQUEST + "csw:watertemp";
        String unavailable = DirectDownload.INVALID_COVERAGE_DOWNLOAD + "csw:watertemp";
        // no size limit, the full mosaic exceeds the one set by enableDirectDownload
        DirectDownloadSettings enabled = new DirectDownloadSettings();
        enabled.setDirectDownloadEnabled(true);
        try {
            // enabled on the layer
            setLayerSettings(enabled);
            assertEquals("application/zip", getAsServletResponse(request).getContentType());

            // no layer settings, the service ones apply
            setLayerSettings(null);
            setServiceSettings(enabled);
            assertEquals("application/zip", getAsServletResponse(request).getContentType());

            // the layer settings win over the service ones
            setLayerSettings(new DirectDownloadSettings());
            assertInvalidParameter(request, "resourceId", unavailable);

            // neither layer nor service settings
            setLayerSettings(null);
            setServiceSettings(null);
            assertInvalidParameter(request, "resourceId", unavailable);
        } finally {
            setServiceSettings(null);
            enableDirectDownload();
        }
    }

    private void assertInvalidParameter(String request, String locator, String message) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals("application/xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertXpathEvaluatesTo("InvalidParameterValue", "//ows:Exception/@exceptionCode", dom);
        assertXpathEvaluatesTo(locator, "//ows:Exception/@locator", dom);
        assertXpathEvaluatesTo(message, "//ows:Exception/ows:ExceptionText", dom);
    }

    private void setLayerSettings(DirectDownloadSettings settings) {
        CoverageInfo coverageInfo = getCatalog().getCoverageByName("watertemp");
        putOrRemove(coverageInfo.getMetadata(), settings);
        getCatalog().save(coverageInfo);
    }

    private void setServiceSettings(DirectDownloadSettings settings) {
        GeoServer gs = getGeoServer();
        CSWInfo csw = gs.getService(CSWInfo.class);
        putOrRemove(csw.getMetadata(), settings);
        gs.save(csw);
    }

    private static void putOrRemove(MetadataMap metadata, DirectDownloadSettings settings) {
        if (settings == null) {
            metadata.remove(DirectDownloadSettings.DIRECTDOWNLOAD_KEY);
        } else {
            metadata.put(DirectDownloadSettings.DIRECTDOWNLOAD_KEY, settings);
        }
    }

    private XpathEngine getXpathEngine() {
        if (xpathEngine == null) {
            xpathEngine = XMLUnit.newXpathEngine();
            Map<String, String> namespaces = new HashMap<>();
            namespaces.putAll(TEST_NAMESPACES);
            xpathEngine.setNamespaceContext(new SimpleNamespaceContext(namespaces));
        }
        return xpathEngine;
    }

    /**
     * Return the flattened value corresponding to an XPath expression from a document.
     *
     * @param xpath XPath expression
     * @param document the document under test
     * @return flattened string value
     */
    protected String evaluate(String xpath, Document document) {
        try {
            return getXpathEngine().evaluate(xpath, document);
        } catch (XpathException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the list of nodes in a document that match an XPath expression.
     *
     * @param xpath XPath expression
     * @param document the document under test
     * @return list of matching nodes
     */
    protected NodeList getMatchingNodes(String xpath, Document document) {
        try {
            return getXpathEngine().getMatchingNodes(xpath, document);
        } catch (XpathException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void resetStore() {
        System.setProperty("DefaultCatalogStore", "org.geoserver.csw.store.simple.GeoServerSimpleCatalogStore");
    }
}
