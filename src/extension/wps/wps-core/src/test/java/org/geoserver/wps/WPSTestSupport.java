/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geoserver.wps.xml.WPSConfiguration;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.process.Processors;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.junit.After;
import org.junit.Assert;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public abstract class WPSTestSupport extends GeoServerSystemTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    // WCS 1.1
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_DEM = new QName(WCS_URI, "DEM", WCS_PREFIX);
    public static QName TASMANIA_DEM_NODATA = new QName(WCS_URI, "DEMNODATA", WCS_PREFIX);
    public static QName HOLE = new QName(WCS_URI, "hole", WCS_PREFIX);
    public static QName ELSHAPED = new QName(WCS_URI, "ElShaped", WCS_PREFIX);
    public static QName RAIN = new QName(WCS_URI, "rain", WCS_PREFIX);
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName ROTATED_CAD = new QName(WCS_URI, "RotatedCad", WCS_PREFIX);
    public static QName WORLD = new QName(WCS_URI, "World", WCS_PREFIX);
    public static String TIFF = "tiff";

    List<GridCoverage> coverages = new ArrayList<>();

    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    static {
        Processors.addProcessFactory(MonkeyProcess.getFactory());
        Processors.addProcessFactory(MultiRawProcess.getFactory());
        Processors.addProcessFactory(MultiOutputEchoProcess.getFactory());
    }

    public static class AutoCloseableResource implements AutoCloseable {
        WPSResourceManager resourceManager;

        RawData rawData;

        Resource resource;

        public File getFile() {
            return file;
        }

        File file;

        public AutoCloseableResource(WPSResourceManager resourceManager, RawData rawData) throws IOException {

            // Final checks on the result
            Assert.assertNotNull(rawData);

            this.resourceManager = resourceManager;
            this.rawData = rawData;
            this.resource = resourceManager.getTemporaryResource(rawData.getFileExtension());
            this.file = resource.file();
            try (InputStream in = rawData.getInputStream()) {
                IOUtils.copy(in, file);
            }
        }

        @Override
        public void close() throws IOException {
            // clean up process
            IOUtils.delete(file, true);
            resourceManager.finished(resourceManager.getExecutionId(true));
        }
    }

    public static class AutoDisposableGridCoverage2D extends GridCoverage2D implements AutoCloseable {

        public AutoDisposableGridCoverage2D(CharSequence name, GridCoverage2D coverage) {
            super(name, coverage);
        }

        @Override
        public void close() {
            CoverageCleanerCallback.disposeCoverage(this);
        }
    }

    protected void setUpInternal(SystemTestData testData) throws Exception {}

    protected void setUpNamespaces(Map<String, String> namespaces) {}

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setupIAULayers(true, true);
    }

    protected void scheduleForDisposal(GridCoverage coverage) {
        this.coverages.add(coverage);
    }

    @After
    public void disposeCoverages() {
        for (GridCoverage coverage : coverages) {
            CoverageCleanerCallback.disposeCoverage(coverage);
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // addUser("admin", "geoxserver", null, Arrays.asList("ROLE_ADMINISTRATOR"));
        // addLayerAccessRule("*", "*", AccessMode.READ, "*");
        // addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("feature", getFeatureNamespace());

        testData.registerNamespaces(namespaces);
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }

    /** Namespace used by the "feature" prefix in GML outputs */
    protected String getFeatureNamespace() {
        return "http://geoserver.sf.net";
    }

    /** Subclasses can override to register custom namespace mappings for xml unit */
    protected void registerNamespaces(Map<String, String> namespaces) {
        // TODO Auto-generated method stub

    }

    protected final void setUpUsers(Properties props) {}

    protected final void setUpLayerRoles(Properties properties) {}

    //    @Before
    //    public void login() throws Exception {
    //        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    //    }

    protected String root() {
        return "wps?";
    }

    /** Validates a document based on the WPS schema */
    protected void checkValidationErrors(Document dom) throws Exception {
        checkValidationErrors(dom, new WPSConfiguration());
    }

    /** Validates a document against the */
    protected void checkValidationErrors(Document dom, Configuration configuration) throws Exception {
        Parser p = new Parser(configuration);
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Exception exception : p.getValidationErrors()) {
                SAXParseException ex = (SAXParseException) exception;
                LOGGER.warning(ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString());
            }
            fail("Document did not validate.");
        }
    }

    protected String readFileIntoString(String filename) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(filename);
                BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /** Adds the wcs 1.1 coverages. */
    public void addWcs11Coverages(SystemTestData testData) throws Exception {
        String styleName = "raster";
        testData.addStyle(styleName, "raster.sld", MockData.class, getCatalog());

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.STYLE, styleName);

        // wcs 1.1
        testData.addRasterLayer(TASMANIA_DEM, "tazdem.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(TASMANIA_DEM_NODATA, "tazdemNoData2.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(HOLE, "hole.zip", null, props, MockData.class, getCatalog());
        testData.addRasterLayer(ELSHAPED, "elshaped.zip", null, props, MockData.class, getCatalog());
        testData.addRasterLayer(RAIN, "rain.zip", "asc", props, MockData.class, getCatalog());
        testData.addRasterLayer(TASMANIA_BM, "tazbm.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(ROTATED_CAD, "rotated.tiff", TIFF, props, MockData.class, getCatalog());
        testData.addRasterLayer(WORLD, "world.tiff", TIFF, props, MockData.class, getCatalog());
    }

    /** Submits an asynch execute request and waits for the final result, which is then returned */
    protected Document submitAsynchronous(String xml, long maxWaitSeconds) throws Exception {
        Document dom = postAsDOM("wps", xml);
        assertXpathExists("//wps:ProcessAccepted", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String fullStatusLocation = xpath.evaluate("//wps:ExecuteResponse/@statusLocation", dom);
        String statusLocation = fullStatusLocation.substring(fullStatusLocation.indexOf('?') - 3);
        return waitForProcessEnd(statusLocation, maxWaitSeconds);
    }

    protected Document waitForProcess(
            String statusLocation, long maxWaitSeconds, ThrowingFunction<Document, Boolean> exitCondition)
            throws Exception {
        await().atMost(maxWaitSeconds, SECONDS).until(() -> {
            MockHttpServletResponse response = getAsServletResponse(statusLocation);
            String contents = response.getContentAsString();
            // super weird... and I believe related to the testing harness... just
            // ignoring it for the moment.
            if ("".equals(contents)) {
                return false;
            }
            Document dom = dom(new ByteArrayInputStream(contents.getBytes()));
            // print(dom);
            return exitCondition.apply(dom);
        });

        return getAsDOM(statusLocation);
    }

    protected Document waitForProcessEnd(String statusLocation, long maxWaitSeconds) throws Exception {
        return waitForProcess(statusLocation, maxWaitSeconds, this::executionComplete);
    }

    private boolean executionComplete(Document dom) throws XpathException {
        return countMatches(dom, "//wps:Status/wps:ProcessAccepted") == 0
                && countMatches(dom, "//wps:Status/wps:ProcessStarted") == 0
                && countMatches(dom, "//wps:Status/wps:ProcessQueued") == 0;
    }

    protected int countMatches(Document d, String xpath) throws XpathException {
        return xp.getMatchingNodes(xpath, d).getLength();
    }

    protected Document waitForProcessStart(String statusLocation, long maxWaitSeconds) throws Exception {
        return waitForProcess(statusLocation, maxWaitSeconds, this::executionStarted);
    }

    private boolean executionStarted(Document d) throws XpathException {
        return countMatches(d, "//wps:Status/wps:ProcessAccepted") == 0
                && countMatches(d, "//wps:Status/wps:ProcessQueued") == 0;
    }
}
