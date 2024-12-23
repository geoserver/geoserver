/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.dimension.RasterTimeDimensionDefaultValueTest;
import org.geoserver.wms.dimension.VectorElevationDimensionDefaultValueTest;
import org.geotools.api.data.Query;
import org.junit.Before;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public abstract class TestsSupport extends WMSTestSupport {

    // xpath engine that will be used to check XML content
    protected static XpathEngine xpath;

    {
        // registering namespaces for the xpath engine
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("wmts", "http://www.opengis.net/wmts/1.0");
        namespaces.put("md", "http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd");
        namespaces.put("gml", "http://www.opengis.net/gml");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    protected static final QName RASTER_ELEVATION_TIME = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static final QName RASTER_ELEVATION = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    protected static final QName RASTER_TIME =
            new QName(MockData.SF_URI, "watertemp_future_generated", MockData.SF_PREFIX);
    protected static final QName RASTER_CUSTOM = new QName(MockData.SF_URI, "watertemp_custom", MockData.SF_PREFIX);

    protected static final QName VECTOR_ELEVATION_TIME =
            new QName(MockData.SF_URI, "ElevationWithStartEnd", MockData.SF_PREFIX);
    public static final QName VECTOR_ELEVATION =
            new QName(MockData.SF_URI, "ElevationWithStartEnd", MockData.SF_PREFIX);
    public static final QName SIDECAR_VECTOR_ET =
            new QName(MockData.SF_URI, "SidecarTimeElevationWithStartEnd", MockData.SF_PREFIX);
    protected static final QName VECTOR_TIME = new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);
    protected static final QName VECTOR_CUSTOM = new QName(MockData.SF_URI, "TimeElevationCustom", MockData.SF_PREFIX);

    protected WMS wms;
    protected Catalog catalog;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // do no setup common layers
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // raster with elevation dimension
        testData.addRasterLayer(
                RASTER_ELEVATION,
                "/org/geoserver/wms/dimension/watertemp.zip",
                null,
                Collections.emptyMap(),
                getClass(),
                getCatalog());
        // raster with time dimension
        RasterTimeDimensionDefaultValueTest.prepareFutureCoverageData(
                RASTER_TIME, this.getDataDirectory(), this.getCatalog());
        // raster with custom dimension
        testData.addRasterLayer(
                RASTER_CUSTOM,
                "/org/geoserver/wms/dimension/custwatertemp.zip",
                null,
                Collections.emptyMap(),
                getClass(),
                getCatalog());
        // vector with elevation dimension
        testData.addVectorLayer(
                VECTOR_ELEVATION,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        testData.addVectorLayer(
                SIDECAR_VECTOR_ET,
                Collections.emptyMap(),
                "/SidecarTimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        // vector with time dimension
        testData.addVectorLayer(
                VECTOR_TIME,
                Collections.emptyMap(),
                "/TimeElevationWithStartEnd.properties",
                this.getClass(),
                getCatalog());
        // vector with custom dimension
        testData.addVectorLayer(
                VECTOR_CUSTOM,
                Collections.emptyMap(),
                "TimeElevationCustom.properties",
                VectorElevationDimensionDefaultValueTest.class,
                getCatalog());
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
        // invoke after setup callback
        afterSetup(testData);
    }

    protected void afterSetup(SystemTestData testData) throws IOException {}

    @Before
    public void setup() throws Exception {
        wms = getWMS();
        catalog = getCatalog();
    }

    protected abstract Dimension buildDimension(DimensionInfo dimensionInfo);

    protected Dimension buildDimensionWithEndAttribute(DimensionInfo dimensionInfo) {
        return buildDimension(dimensionInfo);
    }

    protected void testDomainsValuesRepresentation(
            int expandLimit, boolean useEndAttribute, String... expectedDomainValues) {
        DimensionInfo dimensionInfo = createDimension(true, null);
        Dimension dimension =
                useEndAttribute ? buildDimensionWithEndAttribute(dimensionInfo) : buildDimension(dimensionInfo);
        List<String> valuesAsStrings = dimension.getDomainValuesAsStrings(Query.ALL, expandLimit).second;
        assertThat(valuesAsStrings.size(), is(expectedDomainValues.length));
        assertThat(valuesAsStrings, containsInAnyOrder(expectedDomainValues));
    }

    protected void testDomainsValuesRepresentation(int expandLimit, String... expectedDomainValues) {
        testDomainsValuesRepresentation(expandLimit, false, expectedDomainValues);
    }

    protected void testDefaultValueStrategy(
            DimensionDefaultValueSetting.Strategy strategy, String expectedDefaultValue) {
        DimensionDefaultValueSetting defaultValueStrategy = new DimensionDefaultValueSetting();
        defaultValueStrategy.setStrategyType(strategy);
        testDefaultValueStrategy(defaultValueStrategy, expectedDefaultValue);
    }

    protected void testDefaultValueStrategy(
            DimensionDefaultValueSetting defaultValueStrategy, String expectedDefaultValue) {
        DimensionInfo dimensionInfo = createDimension(true, defaultValueStrategy);
        Dimension dimension = buildDimension(dimensionInfo);
        String defaultValue = dimension.getDefaultValueAsString();
        assertThat(defaultValue, is(expectedDefaultValue));
    }

    protected static DimensionInfo createDimension(boolean enable, DimensionDefaultValueSetting defaultValueStrategy) {
        DimensionInfo dimension = new DimensionInfoImpl();
        dimension.setEnabled(enable);
        dimension.setPresentation(DimensionPresentation.LIST);
        dimension.setDefaultValue(defaultValueStrategy);
        return dimension;
    }

    /**
     * Helper method that simply extracts the result of a request to a string and builds a document. Also checks that
     * the content type is 'text/xml'.
     */
    protected Document getResultAsDocument(MockHttpServletResponse response) throws Exception {
        return getResultAsDocument(response, "text/xml");
    }

    /**
     * Helper method that simply extracts the result of a request to a string and build a document. Also checks the
     * content type of the response.
     */
    protected Document getResultAsDocument(MockHttpServletResponse response, String contentType) throws Exception {
        return getResultAsDocument(response, getBaseMimeType(contentType), HttpStatus.OK);
    }

    protected Document getResultAsDocument(
            MockHttpServletResponse response, String contentType, HttpStatus expectedStatus) throws Exception {
        String result = response.getContentAsString();
        assertThat(response.getStatus(), is(expectedStatus.value()));
        assertThat(getBaseMimeType(response.getContentType()), is(contentType));
        return XMLUnit.buildTestDocument(result);
    }

    /** Helper method that perform a XPATH count and check the result. */
    protected void checkXpathCount(Document result, String path, String count) throws Exception {
        String finalPath = String.format("count(/%s)", path);
        assertThat(xpath.evaluate(finalPath, result), is(count));
    }

    protected void registerLayerDimension(
            ResourceInfo info,
            String dimensionName,
            String attributeName,
            DimensionPresentation presentation,
            DimensionDefaultValueSetting defaultValue) {
        registerLayerDimension(info, dimensionName, attributeName, null, presentation, defaultValue);
    }

    /** Helper method that will register a dimension for some layer. */
    public static void registerLayerDimension(
            ResourceInfo info,
            String dimensionName,
            String attributeName,
            String endAttribute,
            DimensionPresentation presentation,
            DimensionDefaultValueSetting defaultValue) {
        DimensionInfo dimension = new DimensionInfoImpl();
        dimension.setEnabled(true);
        dimension.setPresentation(presentation);
        dimension.setDefaultValue(defaultValue);
        dimension.setAttribute(attributeName);
        if (endAttribute != null) dimension.setEndAttribute(endAttribute);
        info.getMetadata().put(dimensionName, dimension);
        info.getCatalog().save(info);
    }

    /** Helper method that will create a default value strategy, minimum value in this case. */
    public static DimensionDefaultValueSetting minimumValue() {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(DimensionDefaultValueSetting.Strategy.MINIMUM);
        return defaultValueSetting;
    }
}
