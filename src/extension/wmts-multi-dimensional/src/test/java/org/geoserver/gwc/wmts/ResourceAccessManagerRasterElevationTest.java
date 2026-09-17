/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geoserver.gwc.wmts.dimensions.RasterElevationDimension;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Checks the granule level restrictions declared by a {@link ResourceAccessManager} are applied to every multi
 * dimensional operation reading a raster domain: DescribeDomains, GetDomainValues, GetHistogram and GetFeature.
 */
public class ResourceAccessManagerRasterElevationTest extends TestsSupport {

    private static final String WORLD = "MULTIPOLYGON(((-180 -90, 180 -90, 180 90, -180 90, -180 -90)))";

    private static final String ELSEWHERE = "MULTIPOLYGON(((-179 -89, -178 -89, -178 -88, -179 -88, -179 -89)))";

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/ResourceAccessManagerContext.xml");
    }

    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    /** describeDomains only reports dimensions declared on the layer, so the elevation one has to be registered. */
    @Override
    protected void afterSetup(SystemTestData testData) {
        CoverageInfo raster = getCatalog().getCoverageByName(RASTER_ELEVATION.getLocalPart());
        registerLayerDimension(raster, ResourceInfo.ELEVATION, null, DimensionPresentation.LIST, minimumValue());
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addUser("cite", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_high", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_low", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_area", "cite", null, Collections.singletonList("ROLE_DUMMY"));
        addUser("cite_nowhere", "cite", null, Collections.singletonList("ROLE_DUMMY"));

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        CoverageInfo raster = getCoverageInfo();

        // the watertemp mosaic holds two granules at elevation 0 and two at elevation 100
        tam.putLimits("cite_high", raster, limits(ff.equals(ff.property("elevation"), ff.literal(100))));
        tam.putLimits("cite_low", raster, limits(ff.equals(ff.property("elevation"), ff.literal(0))));
        tam.putLimits("cite_area", raster, limits(area(WORLD)));
        tam.putLimits("cite_nowhere", raster, limits(area(ELSEWHERE)));
    }

    /** An attribute restriction, with no area: only the granule values are limited. */
    private CoverageAccessLimits limits(Filter readFilter) {
        return new CoverageAccessLimits(CatalogMode.HIDE, readFilter, null, null);
    }

    /** An area restriction, the shape GeoFence and ACL give a coverage: the area goes in the raster filter. */
    private CoverageAccessLimits limits(MultiPolygon area) {
        return new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, area, null);
    }

    private static MultiPolygon area(String wkt) throws Exception {
        return (MultiPolygon) new WKTReader().read(wkt);
    }

    @Test
    public void testDescribeDomainsRestricted() throws Exception {
        Document result = wmts("cite_high", "DescribeDomains", "");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='100']", "1");
        // the two granules at elevation 0 must not be counted
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation'][md:Size='1']", "1");
    }

    @Test
    public void testDescribeDomainsUnrestricted() throws Exception {
        Document result = wmts("cite", "DescribeDomains", "");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='0,100']", "1");
    }

    /** No granule is readable, so the dimension domain collapses rather than falling back to the full one. */
    @Test
    public void testDescribeDomainsOutsideAllowedArea() throws Exception {
        Document result = wmts("cite_nowhere", "DescribeDomains", "");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation'][md:Size='0']", "1");
    }

    @Test
    public void testGetDomainValuesRestricted() throws Exception {
        Document result = wmts("cite_high", "GetDomainValues", "&domain=elevation");
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", result);
        assertXpathEvaluatesTo("100", "/md:DomainValues/md:Domain", result);
    }

    @Test
    public void testGetDomainValuesUnrestricted() throws Exception {
        Document result = wmts("cite", "GetDomainValues", "&domain=elevation");
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", result);
        assertXpathEvaluatesTo("0,100", "/md:DomainValues/md:Domain", result);
    }

    @Test
    public void testGetDomainValuesRestrictedToLowElevation() throws Exception {
        Document result = wmts("cite_low", "GetDomainValues", "&domain=elevation");
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", result);
        assertXpathEvaluatesTo("0", "/md:DomainValues/md:Domain", result);
    }

    /** An area covering the whole mosaic must leave the domain alone. */
    @Test
    public void testGetDomainValuesWithinAllowedArea() throws Exception {
        Document result = wmts("cite_area", "GetDomainValues", "&domain=elevation");
        assertXpathEvaluatesTo("0,100", "/md:DomainValues/md:Domain", result);
    }

    @Test
    public void testGetDomainValuesOutsideAllowedArea() throws Exception {
        Document result = wmts("cite_nowhere", "GetDomainValues", "&domain=elevation");
        assertXpathEvaluatesTo("0", "/md:DomainValues/md:Size", result);
    }

    /** The histogram counts granules, so the hidden ones have to drop out instead of moving to another bucket. */
    @Test
    public void testGetHistogramRestricted() throws Exception {
        Document result = wmts("cite_high", "GetHistogram", "&histogram=elevation&resolution=25");
        checkXpathCount(result, "/md:Histogram[md:Domain='100.0/125.0/25.0']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2']", "1");
    }

    @Test
    public void testGetHistogramUnrestricted() throws Exception {
        Document result = wmts("cite", "GetHistogram", "&histogram=elevation&resolution=25");
        checkXpathCount(result, "/md:Histogram[md:Domain='0.0/125.0/25.0']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2,0,0,0,2']", "1");
    }

    /** GetFeature hands out the granules themselves, so the restricted ones must not show up at all. */
    @Test
    public void testGetFeatureRestricted() throws Exception {
        Document result = getFeature("cite_high");
        checkXpathCount(result, "/wmts:feature", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='100']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='0']", "0");
    }

    @Test
    public void testGetFeatureUnrestricted() throws Exception {
        Document result = getFeature("cite");
        checkXpathCount(result, "/wmts:feature", "4");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='0']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='100']", "2");
    }

    @Test
    public void testGetFeatureOutsideAllowedArea() throws Exception {
        checkXpathCount(getFeature("cite_nowhere"), "/wmts:feature", "0");
    }

    /** An area covering the whole mosaic must leave the granules alone. */
    @Test
    public void testGetFeatureWithinAllowedArea() throws Exception {
        checkXpathCount(getFeature("cite_area"), "/wmts:feature", "4");
    }

    /**
     * The request has to carry its own credentials: the filter chain authenticates it on its own, so a thread bound
     * login would be replaced by the anonymous user.
     */
    private Document wmts(String user, String request, String extraParams) throws Exception {
        return getResultAsDocument(execute(user, request, extraParams));
    }

    /** GetFeature answers in GML, so it needs the matching content type rather than the plain text/xml one. */
    private Document getFeature(String user) throws Exception {
        return getResultAsDocument(execute(user, "GetFeature", ""), "text/xml; subtype=gml/3.1.1");
    }

    private MockHttpServletResponse execute(String user, String request, String extraParams) throws Exception {
        setRequestAuth(user, "cite");
        String queryRequest = "request=%s&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326%s"
                .formatted(request, getLayerId(RASTER_ELEVATION), extraParams);
        return getAsServletResponse("gwc/service/wmts?" + queryRequest);
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        return new RasterElevationDimension(wms, getLayerInfo(), dimensionInfo);
    }

    // getCatalog(), not the catalog field: the field is only set by the @Before, which runs after onSetUp
    private LayerInfo getLayerInfo() {
        return getCatalog().getLayerByName(RASTER_ELEVATION.getLocalPart());
    }

    private CoverageInfo getCoverageInfo() {
        return (CoverageInfo) getLayerInfo().getResource();
    }
}
