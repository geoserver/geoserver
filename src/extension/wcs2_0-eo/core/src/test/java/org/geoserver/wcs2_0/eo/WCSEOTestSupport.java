/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.junit.After;
import org.junit.Before;
import org.opengis.coverage.grid.GridCoverage;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Base support class for wcs EO tests.
 *
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
public abstract class WCSEOTestSupport extends GeoServerSystemTestSupport {
    protected static QName TIMERANGES =
            new QName(MockData.SF_URI, "timeranges", MockData.SF_PREFIX);

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    protected static QName SPATIO_TEMPORAL =
            new QName(MockData.SF_URI, "spatio-temporal", MockData.SF_PREFIX);

    protected static QName MULTIDIM = new QName(MockData.SF_URI, "multidim", MockData.SF_PREFIX);

    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    List<GridCoverage> coverages = new ArrayList<GridCoverage>();

    /**
     * Small value for comparaison of sample values. Since most grid coverage implementations in
     * Geotools 2 store geophysics values as {@code float} numbers, this {@code EPS} value must be
     * of the order of {@code float} relative precision, not {@code double}.
     */
    static final float EPS = 1E-5f;

    static {
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch (Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    /** @return The global wcs instance from the application context. */
    protected WCSInfo getWCS() {
        return getGeoServer().getService(WCSInfo.class);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do not setup anything here, we'll setup mosaics later
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(
                TIMERANGES, "timeranges.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(
                SPATIO_TEMPORAL,
                "spatio-temporal.zip",
                null,
                null,
                SystemTestData.class,
                getCatalog());
        testData.addRasterLayer(
                MULTIDIM, "multidim.zip", null, null, SystemTestData.class, getCatalog());

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wcs", "http://www.opengis.net/wcs/2.0");
        namespaces.put("wcscrs", "http://www.opengis.net/wcs/service-extension/crs/1.0");
        namespaces.put("ows", "http://www.opengis.net/ows/2.0");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("int", "http://www.opengis.net/WCS_service-extension_interpolation/1.0");
        namespaces.put("gmlcov", "http://www.opengis.net/gmlcov/1.0");
        namespaces.put("swe", "http://www.opengis.net/swe/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
        namespaces.put("wcsgs", "http://www.geoserver.org/wcsgs/2.0");
        namespaces.put("wcseo", "http://www.opengis.net/wcseo/1.0");
        namespaces.put("eop", "http://www.opengis.net/eop/2.0");
        namespaces.put("om", "http://www.opengis.net/om/2.0");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    /** Marks the coverage to be cleaned when the test ends */
    protected void scheduleForCleaning(GridCoverage coverage) {
        if (coverage != null) {
            coverages.add(coverage);
        }
    }

    @After
    public void cleanCoverages() {
        for (GridCoverage coverage : coverages) {
            CoverageCleanerCallback.disposeCoverage(coverage);
        }
    }

    /** Parses a multipart message from the response */
    protected Multipart getMultipart(MockHttpServletResponse response)
            throws MessagingException, IOException {
        MimeMessage body = new MimeMessage((Session) null, getBinaryInputStream(response));
        Multipart multipart = (Multipart) body.getContent();
        return multipart;
    }

    /** Configures the specified dimension for a coverage */
    protected void setupRasterDimension(
            String coverageName,
            String metadataKey,
            DimensionPresentation presentation,
            Double resolution) {
        CoverageInfo info = getCatalog().getCoverageByName(coverageName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if (resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        info.getMetadata().put(metadataKey, di);
        getCatalog().save(info);
    }

    /** Clears dimension information from the specified coverage */
    protected void clearDimensions(String coverageName) {
        CoverageInfo info = getCatalog().getCoverageByName(coverageName);
        info.getMetadata().remove(ResourceInfo.TIME);
        info.getMetadata().remove(ResourceInfo.ELEVATION);
        getCatalog().save(info);
    }

    protected void enableEODataset(String coverageName) {
        CoverageInfo ci = getCatalog().getCoverageByName(coverageName);
        ci.getMetadata().put(WCSEOMetadata.DATASET.key, true);
        getCatalog().save(ci);
        setupRasterDimension(coverageName, ResourceInfo.TIME, DimensionPresentation.LIST, null);
    }

    @Before
    public void enableWCSEO() {
        WCSInfo wcs = getGeoServer().getService(WCSInfo.class);
        wcs.getMetadata().put(WCSEOMetadata.ENABLED.key, true);
        wcs.getMetadata().put(WCSEOMetadata.COUNT_DEFAULT.key, String.valueOf(20));
        wcs.getSRS().clear();
        wcs.getSRS().add("4326");
        wcs.getSRS().add("3857");
        getGeoServer().save(wcs);

        wcs = getGeoServer().getService(WCSInfo.class);
        assertTrue(wcs.getMetadata().get(WCSEOMetadata.ENABLED.key, Boolean.class));
    }

    @Before
    public void enableEODatasets() {
        enableEODataset(getLayerId(WATTEMP));
        enableEODataset(getLayerId(TIMERANGES));
        String spatioTemporal = getLayerId(SPATIO_TEMPORAL);
        enableEODataset(spatioTemporal);
        setupRasterDimension(
                spatioTemporal, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        String multidim = getLayerId(MULTIDIM);
        enableEODataset(multidim);
        setupRasterDimension(multidim, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        setupRasterDimension(
                multidim,
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + "WAVELENGTH",
                DimensionPresentation.LIST,
                null);
    }
}
