/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.catalog.testreader.CustomFormat.CUSTOM_DIMENSION_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.junit.Test;
import org.springframework.http.HttpStatus;
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
        // register dimension for raster custom
        CoverageInfo rasterCustom = getCatalog().getCoverageByName(RASTER_CUSTOM.getLocalPart());
        registerLayerDimension(
                rasterCustom,
                ResourceInfo.CUSTOM_DIMENSION_PREFIX + CUSTOM_DIMENSION_NAME,
                null,
                DimensionPresentation.LIST,
                null);
    }

    @Test
    public void testGetCapabilitiesOperation() throws Exception {
        // perform the get capabilities request
        MockHttpServletResponse response =
                getAsServletResponse("gwc/service/wmts?request=GetCapabilities");
        Document result = getResultAsDocument(response, "text/xml");
        // four total dimensions that we are going to check one by one
        checkXpathCount(result, "/wmts:Contents/wmts:Layer/wmts:Dimension", "5");
        // note, the capabilities output follows the same config as WMS, it's not dynamic like
        // DescribeDomains
        // check raster elevation dimension
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Default='0.0']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Value='0']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Value='100']",
                "1");
        // check raster time dimension
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Default='0.0']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Value='2008-10-31T00:00:00.000Z--2008-11-01T00:00:00.000Z']",
                "1");
        // check vector elevation dimension
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='ElevationWithStartEnd']/wmts:Dimension[wmts:Default='1.0']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='ElevationWithStartEnd']/wmts:Dimension[wmts:Value='1.0--5.0']",
                "1");
        // check vector time dimension
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='ElevationWithStartEnd']/wmts:Dimension[wmts:Default='2012-02-11T00:00:00Z']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='ElevationWithStartEnd']/wmts:Dimension[wmts:Value='2012-02-11T00:00:00.000Z']",
                "1");
        checkXpathCount(
                result,
                "/wmts:Contents/wmts:Layer[ows:Title='ElevationWithStartEnd']/wmts:Dimension[wmts:Value='2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testRasterDescribeDomainsOperation() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
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
                "/md:Domains/md:DimensionDomain[md:Domain='2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z']",
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
    public void testRasterDescribeDomainsOperationNoSpace() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME) + "&domains=elevation,time");
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
                "/md:Domains/md:DimensionDomain[md:Domain='2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z']",
                "1");
        // check the space domain is gone
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox", "0");
    }

    @Test
    public void testRasterDescribeDomainsOperationOnlySpace() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME) + "&domains=bbox");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "0");
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
    public void testRasterDescribeDomainsOperationInvalidDimension() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME) + "&domains=abcd");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result =
                getResultAsDocument(
                        response,
                        "text/xml",
                        HttpStatus.BAD_REQUEST); // check that we have two domains
        assertEquals(
                "InvalidParameterValue", xpath.evaluate("//ows:Exception/@exceptionCode", result));
        assertEquals("Domains", xpath.evaluate("//ows:Exception/@locator", result));
        assertThat(xpath.evaluate("//ows:ExceptionText", result), containsString("'abcd'"));
    }

    @Test
    public void testVectorDescribeDomainsOperation() throws Exception {
        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");

        // check the elevation domain
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='elevation' and md:Size='4']",
                "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='1.0,2.0,3.0,5.0']", "1");
        // check the time domain
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Size='2']",
                "1");
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
                        getLayerId(RASTER_ELEVATION_TIME) + "&elevation=100");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
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
                "/md:Domains/md:DimensionDomain[md:Domain='2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z']",
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
                        getLayerId(VECTOR_ELEVATION_TIME) + "&time=1980-10-31T00:00:00.000Z");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // the domain should not contain any values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='0']", "2");
        // no space domain either
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox", "0");
    }

    @Test
    public void testRasterDescribeDomainsOperationWithBoundingBoxNoResultsFilter()
            throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME) + "&bbox=5,5,6,6");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the space domain is not included
        checkXpathCount(result, "/md:Domains/md:SpaceDomain", "0");
        // the domain should not contain any values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='0']", "2");
    }

    @Test
    public void testRasterDescribeDomainsReprojectedFilterMosaic() throws Exception {
        // perform the get describe domains operation with a spatial restriction in 3857, crossing
        // the data
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME)
                                + "&bbox=700000,5000000,800000,6000000,EPSG:3857");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the space domain is not included
        checkXpathCount(result, "/md:Domains/md:SpaceDomain", "1");
        // the domain should not contain 2 values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "2");
    }

    @Test
    public void testRasterDescribeDomainsAcrossDateline() throws Exception {
        // perform the get describe domains operation with a spatial restriction across the
        // dateline,
        // with the part covering the data fully outside of the dateline
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME) + "&bbox=170,40,374,45,EPSG:4326");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the space domain is not included
        checkXpathCount(result, "/md:Domains/md:SpaceDomain", "1");
        // the domain should not contain 2 values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "2");
    }

    @Test
    public void testRasterDescribeDomainsReprojectedOutsideValid() throws Exception {
        // perform the get describe domains operation with a spatial restriction in 3857 and with
        // values wrapped to a "second copy of the world" past the dateline
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME)
                                + "&bbox=40000000,5000000,41000000,6000000,EPSG:3857");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the space domain is not included
        checkXpathCount(result, "/md:Domains/md:SpaceDomain", "1");
        // the domain should not contain 2 values
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Size='2']", "2");
    }

    @Test
    public void testRasterDescribeDomainsOperationWithBoundingAndWrongTileMatrixSet()
            throws Exception {
        // perform the get describe domains operation with a spatial restriction and in invalid tile
        // matrix set
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:XXXX",
                        getLayerId(RASTER_ELEVATION_TIME) + "&bbox=5,5,6,6");
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
                        getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=-180,-90,180,90");
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
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Size='4']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Domain='1.0,2.0,3.0,5.0']",
                "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'time' and md:Size='2']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Domain='2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testVectorDescribeDomainsReprojectedFilter() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME)
                                + "&bbox=-20000000,-20000000,20000000,20000000,EPSG:3857");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the space domain
        checkVectorElevationFullDomain(result);
    }

    @Test
    public void testVectorDescribeDomainsAcrossDateline() throws Exception {
        // spatial restriction across the dateline, the polygons are the 4 quadrants covering the
        // world
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=170,-90,190,90,EPSG:4326");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // should hav gotten back everything
        checkVectorElevationFullDomain(result);
    }

    @Test
    public void testVectorDescribeDomainsOutsideWorld() throws Exception {
        // spatial restriction is whole world but completely outside range, code should re-roll it
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=180,-90,540,90,EPSG:4326");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // should hav gotten back everything
        checkVectorElevationFullDomain(result);
    }

    @Test
    public void testVectorDescribeDomainsAcrossDatelineWebMercator() throws Exception {
        // spatial restriction across the dateline in 3857, the polygons are the 4 quadrants
        // covering the world
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME)
                                + "&bbox=19000000,-20000000,21000000,20000000,EPSG:3857");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // should hav gotten back everything
        checkVectorElevationFullDomain(result);
    }

    @Test
    public void testVectorDescribeDomainsOutswideWorldWebMercator() throws Exception {
        // spatial restriction outside of the valid 3857 domain, the polygons are the 4 quadrants
        // covering the world
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME)
                                + "&bbox=21000000,-20000000,59000000,20000000,EPSG:3857");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // should hav gotten back everything
        checkVectorElevationFullDomain(result);
    }

    public void checkVectorElevationFullDomain(Document result) throws Exception {
        // check the space domain, should be regular whole world
        assertXpathEvaluatesTo("1.1", "/md:Domains/@version", result);
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='-180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='-90.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='90.0']", "1");
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Size='4']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Domain='1.0,2"
                        + ".0,3.0,5.0']",
                "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'time' and md:Size='2']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and "
                        + "md:Domain='2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z']",
                "1");
    }

    /**
     * Same as {@link #testVectorDescribeDomainsOperationWithBoundingBoxFilter()} but with a limit
     * of zero, so all domain descriptions should contract to a min max value
     */
    @Test
    public void testVectorDescribeDomainsOperationWithLimitZero() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=-180,-90,180,90&expandLimit=0");
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
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Size='2']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Domain='1.0--5.0']",
                "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'time' and md:Size='2']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Domain='2012-02-11T00:00:00.000Z--2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testRasterGetHistogramOperationForElevation() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=25",
                        getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram
        checkXpathCount(result, "/md:Histogram[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Histogram[md:Domain='0.0/125.0/25.0']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2,0,0,0,2']", "1");
    }

    @Test
    public void testRasterEmptyElevationHistogram() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s"
                                + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=25&elevation=-100",
                        getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // print(result);
        // check the returned histogram
        assertEmptyHistogram(result, "elevation");
    }

    @Test
    public void testRasterEmptyTimeHistogram() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s"
                                + "&TileMatrixSet=EPSG:4326&histogram=time&elevation=-100",
                        getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // print(result);
        // check the returned histogram
        assertEmptyHistogram(result, "time");
    }

    @Test
    public void testRasterEmptyCustomHistogram() throws Exception {
        // perform the get histogram operation request (empty domain via dimension filter)
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s"
                                + "&TileMatrixSet=EPSG:4326&histogram=%s&%s=FOOBAR",
                        getLayerId(RASTER_CUSTOM), CUSTOM_DIMENSION_NAME, CUSTOM_DIMENSION_NAME);
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        assertEmptyHistogram(result, CUSTOM_DIMENSION_NAME);
    }

    @Test
    public void testGetTimeHistogramOnCoverageView() throws Exception {
        CoverageInfo coverageInfo = setupWaterTempTwoBandsView();

        // enable dimensions
        registerLayerDimension(
                coverageInfo,
                ResourceInfo.TIME,
                null,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());

        // test histogram
        String layerName = RASTER_ELEVATION_TIME.getPrefix() + ":waterView";
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                                + "&histogram=time&resolution=P1D",
                        layerName);
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram, it's two days, not just one
        checkXpathCount(result, "/md:Histogram[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Histogram[md:Domain='2008-10-31T00:00:00.000Z/2008-11-02T00"
                        + ":00:00.000Z/P1D']",
                "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2,2']", "1");
    }

    @Test
    public void testGetElevationHistogramOnCoverageView() throws Exception {
        CoverageInfo coverageInfo = setupWaterTempTwoBandsView();

        // enable dimensions
        registerLayerDimension(
                coverageInfo,
                ResourceInfo.ELEVATION,
                null,
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());

        // test histogram
        String layerName = RASTER_ELEVATION_TIME.getPrefix() + ":waterView";
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                                + "&histogram=elevation&resolution=100",
                        layerName);
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram, it's two days, not just one
        checkXpathCount(result, "/md:Histogram[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Histogram[md:Domain='0.0/200.0/100.0']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2,2']", "1");
    }

    public CoverageInfo setupWaterTempTwoBandsView() throws Exception {
        // setting up a 2 bands coverage view on watertemp
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("watertemp");

        // clear up in case already existing
        CoverageInfo previous = cat.getCoverageByName("waterView");
        if (previous != null) {
            cat.remove(cat.getLayerByName("waterView"));
            cat.remove(previous);
        }

        final InputCoverageBand band = new InputCoverageBand("watertemp", "0");
        final CoverageBand outputBand1 =
                new CoverageBand(
                        Collections.singletonList(band),
                        "watertemp@0",
                        0,
                        CompositionType.BAND_SELECT);
        final CoverageBand outputBand2 =
                new CoverageBand(
                        Collections.singletonList(band),
                        "watertemp@0",
                        1,
                        CompositionType.BAND_SELECT);
        final CoverageView coverageView =
                new CoverageView("waterView", Arrays.asList(outputBand1, outputBand2));
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        CoverageInfo coverageInfo =
                coverageView.createCoverageInfo("waterView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
        coverageInfo = cat.getCoverage(coverageInfo.getId());
        LayerInfo layer = builder.buildLayer(coverageInfo);
        cat.add(layer);
        return coverageInfo;
    }

    @Test
    public void testVectorGetHistogramOperationForTime() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1M",
                        getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram
        checkXpathCount(result, "/md:Histogram[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Histogram[md:Domain="
                        + "'2012-02-11T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1M']",
                "1");
        checkXpathCount(result, "/md:Histogram[md:Values='4']", "1");
    }

    @Test
    public void testVectorEmptyTimeHistogram() throws Exception {
        // perform the get histogram operation request, using a non existing elevation value
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s"
                                + "&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1M&elevation=-10",
                        getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // print(result);
        assertEmptyHistogram(result, "time");
    }

    public void assertEmptyHistogram(Document result, String dimension) throws Exception {
        checkXpathCount(result, "/md:Histogram[ows:Identifier='" + dimension + "']", "1");
        assertEquals("1", xpath.evaluate("count(/md:Histogram/md:Domain)", result));
        assertEquals("", xpath.evaluate("/md:Histogram/md:Domain", result));
        assertEquals("1", xpath.evaluate("count(/md:Histogram/md:Values)", result));
        assertEquals("", xpath.evaluate("/md:Histogram/md:Values", result));
    }

    @Test
    public void testVectorEmptyElevationHistogram() throws Exception {
        // perform the get histogram operation request, using a non existing elevation value
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s"
                                + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=P1M&elevation=-10",
                        getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // print(result);
        assertEmptyHistogram(result, "elevation");
    }

    @Test
    public void testRasterGetFeatureOperation() throws Exception {
        // perform the get feature operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(RASTER_ELEVATION_TIME));
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
                        getLayerId(RASTER_ELEVATION_TIME) + "&bbox=-1,-1,0,0");
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
                        getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the returned features
        checkXpathCount(result, "/wmts:feature", "4");
        checkXpathCount(result, "/wmts:feature/wmts:footprint/gml:Polygon", "4");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='1.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='3.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='5.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2012-02-11T00:00:00.000Z']", "3");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2012-02-12T00:00:00.000Z']", "1");
    }

    @Test
    public void testVectorGetFeatureOperationWithTimeFilter() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getLayerId(VECTOR_ELEVATION_TIME)
                                + "&time=2012-02-10T00:00:00.000Z/2012-02-11T00:00:00.000Z");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the filtered returned features
        checkXpathCount(result, "/wmts:feature", "3");
        checkXpathCount(result, "/wmts:feature/wmts:footprint/gml:Polygon", "3");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='1.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2012-02-11T00:00:00.000Z']", "3");
    }

    @Test
    public void testInvalidRequestWithNoOperation() throws Exception {
        // perform an invalid WMTS request that doesn't provide a valid request
        MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts?request~GetCapabilities!service~!'WMTS'version~'1.0.0");
        // this request should fail whit an exception report
        assertThat(response.getContentAsString(), containsString("Missing Request parameter"));
        assertThat(response.getStatus(), is(400));
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
        return getResultAsDocument(response, contentType, HttpStatus.OK);
    }

    private Document getResultAsDocument(
            MockHttpServletResponse response, String contentType, HttpStatus expectedStatus)
            throws Exception {
        String result = response.getContentAsString();
        assertThat(response.getStatus(), is(expectedStatus.value()));
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

    @Test
    public void testVectorGetDomainValuesOnTime() throws Exception {
        // full domain (only 2 entries)
        String baseRequest =
                "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                        + getLayerId(VECTOR_ELEVATION_TIME)
                        + "&TileMatrixSet=EPSG:4326&domain=time";
        Document dom = getAsDOM(baseRequest);
        print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo(
                "2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z",
                "/md:DomainValues/md:Domain",
                dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=1");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("2012-02-11T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

        // second page ascending
        dom = getAsDOM(baseRequest + "&fromValue=2012-02-11T00:00:00.000Z&limit=1");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2012-02-12T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

        // first page descending
        dom = getAsDOM(baseRequest + "&limit=1&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2012-02-12T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

        // second page descending
        dom = getAsDOM(baseRequest + "&fromValue=2012-02-12T00:00:00.000Z&limit=1&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2012-02-11T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);
    }

    @Test
    public void testRasterGetDomainValuesOnTime() throws Exception {
        // full domain (only 2 entries)
        String baseRequest =
                "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                        + getLayerId(RASTER_ELEVATION_TIME)
                        + "&TileMatrixSet=EPSG:4326&domain=time";
        Document dom = getAsDOM(baseRequest);
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo(
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z",
                "/md:DomainValues/md:Domain",
                dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=1");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

        // second page ascending
        dom = getAsDOM(baseRequest + "&fromValue=2008-10-31T00:00:00.000ZZ&limit=1");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

        // first page descending
        dom = getAsDOM(baseRequest + "&limit=1&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

        // second page descending
        dom = getAsDOM(baseRequest + "&fromValue=2008-11-01T00:00:00.000Z&limit=1&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);
    }

    @Test
    public void testVectorGetDomainValuesOnElevations() throws Exception {
        // full domain (only 2 entries)
        String baseRequest =
                "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                        + getLayerId(VECTOR_ELEVATION_TIME)
                        + "&TileMatrixSet=EPSG:4326&domain=elevation";
        Document dom = getAsDOM(baseRequest);
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("4", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("1.0,2.0,3.0,5.0", "/md:DomainValues/md:Domain", dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=3");
        print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("1.0,2.0,3.0", "/md:DomainValues/md:Domain", dom);

        // second page ascending (partial)
        dom = getAsDOM(baseRequest + "&fromValue=3.0&limit=3");
        print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("5.0", "/md:DomainValues/md:Domain", dom);

        // trying a page outside of the domain
        dom = getAsDOM(baseRequest + "&fromValue=5.0&limit=3");
        print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("0", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("", "/md:DomainValues/md:Domain", dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=3&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("desc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("5.0,3.0,2.0", "/md:DomainValues/md:Domain", dom);

        // second page ascending
        dom = getAsDOM(baseRequest + "&fromValue=2&limit=3&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("desc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("1.0", "/md:DomainValues/md:Domain", dom);
    }

    @Test
    public void testRasterGetDomainValuesOnElevation() throws Exception {
        // full domain (only 2 entries)
        String baseRequest =
                "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                        + getLayerId(RASTER_ELEVATION_TIME)
                        + "&TileMatrixSet=EPSG:4326&domain=elevation";
        Document dom = getAsDOM(baseRequest);
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("0,100", "/md:DomainValues/md:Domain", dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=1");
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("1", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("0", "/md:DomainValues/md:Domain", dom);

        // second page ascending
        dom = getAsDOM(baseRequest + "&fromValue=1&limit=1");
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("100", "/md:DomainValues/md:Domain", dom);

        // first page descending
        dom = getAsDOM(baseRequest + "&limit=1&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("100", "/md:DomainValues/md:Domain", dom);

        // second page descending
        dom = getAsDOM(baseRequest + "&fromValue=100&limit=1&sort=desc");
        print(dom);
        assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("0", "/md:DomainValues/md:Domain", dom);
    }

    @Test
    public void testRasterCustomGetDomainValues() throws Exception {
        // full domain (only 2 entries)
        String baseRequest =
                "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                        + getLayerId(RASTER_CUSTOM)
                        + "&TileMatrixSet=EPSG:4326&domain="
                        + CUSTOM_DIMENSION_NAME;
        Document dom = getAsDOM(baseRequest);
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo(
                "CustomDimValueA,CustomDimValueB,CustomDimValueC",
                "/md:DomainValues/md:Domain",
                dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=2");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo(
                "CustomDimValueA,CustomDimValueB", "/md:DomainValues/md:Domain", dom);

        // second page ascending
        dom = getAsDOM(baseRequest + "&fromValue=CustomDimValueB&limit=2");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("CustomDimValueC", "/md:DomainValues/md:Domain", dom);

        // first page descending
        dom = getAsDOM(baseRequest + "&limit=2&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo(
                "CustomDimValueC,CustomDimValueB", "/md:DomainValues/md:Domain", dom);

        // second page descending
        dom = getAsDOM(baseRequest + "&fromValue=CustomDimValueB&limit=2&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("CustomDimValueA", "/md:DomainValues/md:Domain", dom);
    }
}
