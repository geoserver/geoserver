/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests that perform requests again the multidimensional extension and check the results for the
 * main four operations: GetCapabilities, DescribeDomains, GetHistogram and GetFewature
 */
public class MultiDimensionalExtensionTest extends TestsSupport {

    // xpath engine that will be used to check XML content
    private static XpathEngine xpath;

    {
        // registering namespaces for the xpath engine
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("wmts", "http://www.opengis.net/wmts/1.0");
        namespaces.put(
                "md",
                "http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd");
        namespaces.put("gml", "http://www.opengis.net/gml");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void afterSetup(SystemTestData testData) {
        // registering elevation and time dimensions for a raster
        CoverageInfo rasterInfo =
                getCatalog().getCoverageByName(RASTER_ELEVATION_TIME.getLocalPart());
        registerLayerDimension(
                rasterInfo,
                ResourceInfo.ELEVATION,
                null,
                DimensionPresentation.LIST,
                minimumValue());
        registerLayerDimension(
                rasterInfo,
                ResourceInfo.TIME,
                null,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());
        // registering elevation and time dimensions for a vector
        FeatureTypeInfo vectorInfo =
                getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        registerLayerDimension(
                vectorInfo,
                ResourceInfo.ELEVATION,
                "startElevation",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());
        registerLayerDimension(
                vectorInfo,
                ResourceInfo.TIME,
                "startTime",
                DimensionPresentation.LIST,
                minimumValue());
    }

    @Test
    public void testGetCapabilitiesOperation() throws Exception {
        // perform the get capabilities request
        MockHttpServletResponse response =
                getAsServletResponse("gwc/service/wmts?request=GetCapabilities");
        Document result = getResultAsDocument(response, "application/vnd.ogc.wms_xml");
        // check raster layer dimensions
        checkXpathCount(result, "/wmts:Contents/wmts:Layer/wmts:Dimension", "4");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer/wmts:Dimension[ows:Identifier='elevation']",
                "2");
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer/wmts:Dimension[ows:Identifier='time']", "2");
        // check raster elevation dimension
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Default='0.0']", "1");
        checkXpathCount(result, "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Value='0']", "1");
        checkXpathCount(result, "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Value='100']", "1");
        // check raster time dimension
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Default='0.0']", "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Value='2008-10-31T00:00:00.000Z--2008-11-01T00:00:00.000Z']",
                "1");
        // check vector elevation dimension
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Default='1.0']", "1");
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Value='1.0--2.0']", "1");
        // check vector time dimension
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Default='2012-02-11T00:00:00Z']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Value='2012-02-11T00:00:00.000Z']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer/wmts:Dimension[wmts:Value='2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testRasterDescribeDomainsOperation() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // both domains contain two elements
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "2");
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='0,100']", "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[md:Domain='2008-10-31T00:00:00.000Z--2008-11-01T00:00:00.000Z']",
                "1");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.23722068851276978']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='40.562080748421806']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='14.592757149389236']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='44.55808294568743']",
                "1");
    }

    @Test
    public void testVectorDescribeDomainsOperation() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        VECTOR_ELEVATION_TIME.getPrefix()
                                + ":"
                                + VECTOR_ELEVATION_TIME.getLocalPart());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // both domains contain two elements
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "2");
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='1.0--2.0']", "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[md:Domain='2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z']",
                "1");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='-180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='-90.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='90.0']", "1");
    }

    @Test
    public void testRasterDescribeDomainsOperationWithElevationFilter() throws Exception {
        // perform the get describe domains operation request filter elevations that are equal to
        // 100.0
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart()
                                + "&elevation=100");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='100']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='1']", "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[md:Domain='2008-10-31T00:00:00.000Z--2008-11-01T00:00:00.000Z']",
                "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "1");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.23722068851276978']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='40.562080748421806']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='14.592757149389236']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='44.55808294568743']",
                "1");
    }

    @Test
    public void testVectorDescribeDomainsOperationWithTimeFilterNoResults() throws Exception {
        // perform the get describe domains operation request filter elevations that are equal to
        // 100.0
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        VECTOR_ELEVATION_TIME.getPrefix()
                                + ":"
                                + VECTOR_ELEVATION_TIME.getLocalPart()
                                + "&time=1980-10-31T00:00:00.000Z");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // the domain should not contain any values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='0']", "2");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='0.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='-1.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='-1.0']", "1");
    }

    @Test
    public void testRasterDescribeDomainsOperationWithBoundingBoxNoResultsFilter()
            throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart()
                                + "&bbox=5,5,6,6");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='0.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='-1.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='-1.0']", "1");
        // the domain should not contain any values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='0']", "2");
    }

    @Test
    public void testRasterDescribeDomainsOperationWithBoundingAndWrongTileMatrixSet()
            throws Exception {
        // perform the get describe domains operation with a spatial restriction and in invalid tile
        // matrix set
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:XXXX",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart()
                                + "&bbox=5,5,6,6");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        // this request should fail because of the invalid tile matrix set
        assertThat(response.getContentAsString(), containsString("Unknown grid set"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testVectorDescribeDomainsOperationWithBoundingBoxFilter() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        VECTOR_ELEVATION_TIME.getPrefix()
                                + ":"
                                + VECTOR_ELEVATION_TIME.getLocalPart()
                                + "&bbox=-180,-90,180,90");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='-180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='-90.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='90.0']", "1");
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // both domains contain two elements
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "2");
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='1.0--2.0']", "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[md:Domain='2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testRasterGetHistogramOperationForElevation() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=25",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check the returned histogram
        checkXpathCount(result, "/md:Histogram[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Histogram[md:Domain='0.0/100.0/25.0']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2,0,0,2']", "1");
    }

    @Test
    public void testVectorGetHistogramOperationForTime() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1M",
                        VECTOR_ELEVATION_TIME.getPrefix()
                                + ":"
                                + VECTOR_ELEVATION_TIME.getLocalPart());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check the returned histogram
        checkXpathCount(result, "/md:Histogram[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Histogram[md:Domain='2012-02-11T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1M']",
                "1");
        checkXpathCount(result, "/md:Histogram[md:Values='3']", "1");
    }

    @Test
    public void testRasterGetFeatureOperation() throws Exception {
        // perform the get feature operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the returned features
        checkXpathCount(result, "/wmts:feature", "4");
        checkXpathCount(result, "/wmts:feature/wmts:footprint/gml:MultiPolygon", "4");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='0']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='100']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2008-10-31T00:00:00.000Z']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2008-11-01T00:00:00.000Z']", "2");
    }

    @Test
    public void testRasterGetFeatureOperationWithBoundingBoxFilterNoResults() throws Exception {
        // perform the get feature operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + RASTER_ELEVATION_TIME.getLocalPart()
                                + "&bbox=-1,-1,0,0");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the no features were returned
        checkXpathCount(result, "/wmts:feature", "0");
    }

    @Test
    public void testVectorGetFeatureOperation() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + VECTOR_ELEVATION_TIME.getLocalPart());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the returned features
        checkXpathCount(result, "/wmts:feature", "3");
        checkXpathCount(result, "/wmts:feature/wmts:footprint/gml:Polygon", "3");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='1.0']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2012-02-11T00:00:00.000Z']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2012-02-12T00:00:00.000Z']", "1");
    }

    @Test
    public void testVectorGetFeatureOperationWithTimeFilter() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        RASTER_ELEVATION_TIME.getPrefix()
                                + ":"
                                + VECTOR_ELEVATION_TIME.getLocalPart()
                                + "&time=2012-02-10T00:00:00.000Z/2012-02-11T00:00:00.000Z");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the filtered returned features
        checkXpathCount(result, "/wmts:feature", "2");
        checkXpathCount(result, "/wmts:feature/wmts:footprint/gml:Polygon", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='1.0']", "2");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2012-02-11T00:00:00.000Z']", "2");
    }

    /** Helper method that will create a default value strategy, minimum value in this case. */
    private DimensionDefaultValueSetting minimumValue() {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(DimensionDefaultValueSetting.Strategy.MINIMUM);
        return defaultValueSetting;
    }

    /** Helper method that will register a dimension for some layer. */
    private void registerLayerDimension(
            ResourceInfo info,
            String dimensionName,
            String attributeName,
            DimensionPresentation presentation,
            DimensionDefaultValueSetting defaultValue) {
        DimensionInfo dimension = new DimensionInfoImpl();
        dimension.setEnabled(true);
        dimension.setPresentation(presentation);
        dimension.setDefaultValue(defaultValue);
        dimension.setAttribute(attributeName);
        info.getMetadata().put(dimensionName, dimension);
        getCatalog().save(info);
    }

    /**
     * Helper method that simply extracts the result of a request to a string and builds a document.
     * Also checks that the content type is 'text/xml'.
     */
    private Document getResultAsDocument(MockHttpServletResponse response) throws Exception {
        return getResultAsDocument(response, "text/xml");
    }

    /**
     * Helper method that simply extracts the result of a request to a string and build a document.
     * Also checks the content type of the response.
     */
    private Document getResultAsDocument(MockHttpServletResponse response, String contentType)
            throws Exception {
        String result = response.getContentAsString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getContentType(), is(contentType));
        return XMLUnit.buildTestDocument(result);
    }

    /** Helper method that perform a XPATH count and check the result. */
    private void checkXpathCount(Document result, String path, String count) throws Exception {
        String finalPath = String.format("count(/%s)", path);
        assertThat(xpath.evaluate(finalPath, result), is(count));
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        return null;
    }
}
