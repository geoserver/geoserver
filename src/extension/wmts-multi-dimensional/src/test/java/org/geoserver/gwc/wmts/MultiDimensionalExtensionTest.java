/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.catalog.testreader.CustomFormat.CUSTOM_DIMENSION_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.geowebcache.io.XMLBuilder;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/**
 * Tests that perform requests again the multidimensional extension and check the results for the main four operations:
 * GetCapabilities, DescribeDomains, GetHistogram and GetFewature
 */
public class MultiDimensionalExtensionTest extends TestsSupport {

    @Override
    protected void afterSetup(SystemTestData testData) {
        // registering elevation and time dimensions for a raster
        CoverageInfo rasterInfo = getCatalog().getCoverageByName(RASTER_ELEVATION_TIME.getLocalPart());
        registerLayerDimension(rasterInfo, ResourceInfo.ELEVATION, null, DimensionPresentation.LIST, minimumValue());
        registerLayerDimension(
                rasterInfo, ResourceInfo.TIME, null, DimensionPresentation.CONTINUOUS_INTERVAL, minimumValue());
        // registering elevation and time dimensions for a vector
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        registerLayerDimension(
                vectorInfo,
                ResourceInfo.ELEVATION,
                "startElevation",
                DimensionPresentation.CONTINUOUS_INTERVAL,
                minimumValue());
        registerLayerDimension(vectorInfo, ResourceInfo.TIME, "startTime", DimensionPresentation.LIST, minimumValue());
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
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?request=GetCapabilities");
        Document result = getResultAsDocument(response, "text/xml");
        // four total dimensions that we are going to check one by one
        checkXpathCount(result, "/wmts:Contents/wmts:Layer/wmts:Dimension", "5");
        // note, the capabilities output follows the same config as WMS, it's not dynamic like
        // DescribeDomains
        // check raster elevation dimension
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Default='0.0']", "1");
        checkXpathCount(result, "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Value='0']", "1");
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Value='100']", "1");
        // check raster time dimension
        checkXpathCount(
                result, "/wmts:Contents/wmts:Layer[ows:Title='watertemp']/wmts:Dimension[wmts:Default='0.0']", "1");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME));
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
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.23722068851276978']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='40.562080748421806']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='14.592757149389236']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='44.55808294568743']", "1");
    }

    @Test
    public void testRasterDescribeDomainsOperationNoSpace() throws Exception {
        // perform the get describe domains operation request
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&domains=elevation,time");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&domains=bbox");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "0");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.23722068851276978']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='40.562080748421806']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='14.592757149389236']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='44.55808294568743']", "1");
    }

    @Test
    public void testRasterDescribeDomainsOperationInvalidDimension() throws Exception {
        // perform the get describe domains operation request
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&domains=abcd");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result =
                getResultAsDocument(response, "text/xml", HttpStatus.BAD_REQUEST); // check that we have two domains
        assertEquals("InvalidParameterValue", xpath.evaluate("//ows:Exception/@exceptionCode", result));
        assertEquals("Domains", xpath.evaluate("//ows:Exception/@locator", result));
        assertThat(xpath.evaluate("//ows:ExceptionText", result), containsString("'abcd'"));
    }

    @Test
    public void testVectorDescribeDomainsOperation() throws Exception {
        // perform the get describe domains operation request
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");

        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation' and md:Size='4']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[md:Domain='1.0,2.0,3.0,5.0']", "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Size='2']", "1");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&elevation=100");
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
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='0.23722068851276978']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='40.562080748421806']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='14.592757149389236']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='44.55808294568743']", "1");
    }

    @Test
    public void testVectorDescribeDomainsOperationWithTimeFilterNoResults() throws Exception {
        // perform the get describe domains operation request filter elevations that are equal to
        // 100.0
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&time=1980-10-31T00:00:00.000Z");
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
    public void testRasterDescribeDomainsOperationWithBoundingBoxNoResultsFilter() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&bbox=5,5,6,6");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&bbox=700000,5000000,800000,6000000,EPSG:3857");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&bbox=170,40,374,45,EPSG:4326");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&bbox=40000000,5000000,41000000,6000000,EPSG:3857");
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
    public void testRasterDescribeDomainsOperationWithBoundingAndWrongTileMatrixSet() throws Exception {
        // perform the get describe domains operation with a spatial restriction and in invalid tile
        // matrix set
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:XXXX"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&bbox=5,5,6,6");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        // this request should fail because of the invalid tile matrix set
        assertThat(response.getContentAsString(), containsString("Unknown grid set"));
        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testVectorDescribeDomainsOperationWithBoundingBoxFilter() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=-180,-90,180,90");
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
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Size='4']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Domain='1.0,2.0,3.0,5.0']",
                "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'time' and md:Size='2']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Domain='2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testVectorDescribeDomainsReprojectedFilter() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=-20000000,-20000000,20000000,20000000,EPSG:3857");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=170,-90,190,90,EPSG:4326");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // should hav gotten back everything
        checkVectorElevationFullDomain(result);
    }

    @Test
    public void testVectorDescribeDomainsOutsideWorld() throws Exception {
        // spatial restriction is whole world but completely outside range, code should re-roll it
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=180,-90,540,90,EPSG:4326");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=19000000,-20000000,21000000,20000000,EPSG:3857");
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
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=21000000,-20000000,59000000,20000000,EPSG:3857");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // should hav gotten back everything
        checkVectorElevationFullDomain(result);
    }

    public void checkVectorElevationFullDomain(Document result) throws Exception {
        // check the space domain, should be regular whole world
        assertXpathEvaluatesTo("1.2", "/md:Domains/@version", result);
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='-180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='-90.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='180.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='90.0']", "1");
        // check that we have two domains
        checkXpathCount(result, "/md:Domains/md:DimensionDomain", "2");
        // check the elevation domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='elevation']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Size='4']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Domain='1.0,2" + ".0,3.0,5.0']",
                "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'time' and md:Size='2']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and "
                        + "md:Domain='2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z']",
                "1");
    }

    /**
     * Same as {@link #testVectorDescribeDomainsOperationWithBoundingBoxFilter()} but with a limit of zero, so all
     * domain descriptions should contract to a min max value
     */
    @Test
    public void testVectorDescribeDomainsOperationWithLimitZero() throws Exception {
        // perform the get describe domains operation with a spatial restriction
        String queryRequest = "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME) + "&bbox=-180,-90,180,90&expandLimit=0");
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
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Size='2']", "1");
        checkXpathCount(
                result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'elevation' and md:Domain='1.0--5.0']", "1");
        // check the time domain
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier='time']", "1");
        checkXpathCount(result, "/md:Domains/md:DimensionDomain[ows:Identifier = 'time' and md:Size='2']", "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Domain='2012-02-11T00:00:00.000Z--2012-02-12T00:00:00.000Z']",
                "1");
    }

    @Test
    public void testRasterGetHistogramOperationForElevation() throws Exception {
        // perform the get histogram operation request
        String queryRequest =
                "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=25"
                        .formatted(getLayerId(RASTER_ELEVATION_TIME));
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
        String queryRequest = String.format(
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
        String queryRequest = String.format(
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
        String queryRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s" + "&TileMatrixSet=EPSG:4326&histogram=%s&%s=FOOBAR",
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
                coverageInfo, ResourceInfo.TIME, null, DimensionPresentation.CONTINUOUS_INTERVAL, minimumValue());

        // test histogram
        String layerName = RASTER_ELEVATION_TIME.getPrefix() + ":waterView";
        String queryRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                        + "&histogram=time&resolution=P1D",
                layerName);
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram, it's two days, not just one
        checkXpathCount(result, "/md:Histogram[ows:Identifier='time']", "1");
        checkXpathCount(
                result, "/md:Histogram[md:Domain='2008-10-31T00:00:00.000Z/2008-11-02T00" + ":00:00.000Z/P1D']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='2,2']", "1");
    }

    @Test
    public void testGetElevationHistogramOnCoverageView() throws Exception {
        CoverageInfo coverageInfo = setupWaterTempTwoBandsView();

        // enable dimensions
        registerLayerDimension(
                coverageInfo, ResourceInfo.ELEVATION, null, DimensionPresentation.CONTINUOUS_INTERVAL, minimumValue());

        // test histogram
        String layerName = RASTER_ELEVATION_TIME.getPrefix() + ":waterView";
        String queryRequest = String.format(
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
                new CoverageBand(Collections.singletonList(band), "watertemp@0", 0, CompositionType.BAND_SELECT);
        final CoverageBand outputBand2 =
                new CoverageBand(Collections.singletonList(band), "watertemp@0", 1, CompositionType.BAND_SELECT);
        final CoverageView coverageView = new CoverageView("waterView", Arrays.asList(outputBand1, outputBand2));
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        CoverageInfo coverageInfo = coverageView.createCoverageInfo("waterView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_IMAGEN_IMAGEREAD", "false");
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
                "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1M"
                        .formatted(getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram
        checkXpathCount(result, "/md:Histogram[ows:Identifier='time']", "1");
        checkXpathCount(
                result, "/md:Histogram[md:Domain=" + "'2012-02-11T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1M']", "1");
        checkXpathCount(result, "/md:Histogram[md:Values='4']", "1");
    }

    @Test
    public void testVectorEmptyTimeHistogram() throws Exception {
        // perform the get histogram operation request, using a non existing elevation value
        String queryRequest = String.format(
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
        String queryRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=P1M&elevation=-10",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        // print(result);
        assertEmptyHistogram(result, "elevation");
    }

    /**
     * Tests that the raster elevation histogram respects a same-dimension filter. When requesting histogram=elevation
     * with elevation=0, the histogram domain should be restricted to elevation 0 only, NOT the full domain 0-100.
     */
    @Test
    public void testRasterGetHistogramWithSameElevationFilter() throws Exception {
        // First verify the full (unfiltered) histogram for comparison
        String fullRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=25",
                getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse fullResponse = getAsServletResponse("gwc/service/wmts?" + fullRequest);
        Document fullResult = getResultAsDocument(fullResponse);
        // full domain spans 0 to 125 with 5 buckets
        checkXpathCount(fullResult, "/md:Histogram[md:Domain='0.0/125.0/25.0']", "1");
        checkXpathCount(fullResult, "/md:Histogram[md:Values='2,0,0,0,2']", "1");

        // Now request histogram filtered by elevation=0 (same dimension)
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=25&elevation=0",
                getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);
        // The histogram domain should NOT be the full range 0.0/125.0/25.0
        // It should be restricted to around elevation=0 only
        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        // domain should NOT contain the full range
        assertThat(
                "Histogram domain should be restricted when same-dimension filter is applied, "
                        + "but got full domain instead: " + domain,
                domain,
                is(not("0.0/125.0/25.0")));
        // values should reflect only the filtered data (2 granules at elevation=0)
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        assertThat("Total histogram count should be 2 for elevation=0 filter", total, is(2));
    }

    /**
     * Tests that the raster time histogram respects a same-dimension time filter. When requesting histogram=time with
     * time=2008-10-31T00:00:00.000Z, the histogram should only cover that single time instant.
     */
    @Test
    public void testRasterGetHistogramWithSameTimeFilter() throws Exception {
        // Request histogram filtered by a single time value (same dimension being histogrammed)
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1D"
                        + "&time=2008-10-31T00:00:00.000Z",
                getLayerId(RASTER_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);
        checkXpathCount(filteredResult, "/md:Histogram[ows:Identifier='time']", "1");
        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        // domain should NOT span two days (the full unfiltered domain)
        assertThat(
                "Time histogram domain should be restricted to 2008-10-31 only, " + "but got full domain instead: "
                        + domain,
                domain,
                is(not("2008-10-31T00:00:00.000Z/2008-11-02T00:00:00.000Z/P1D")));
        // values should total 2 (the 2 granules at 2008-10-31)
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        assertThat("Total histogram count should be 2 for time=2008-10-31 filter", total, is(2));
    }

    /**
     * Tests that the vector time histogram respects a same-dimension time filter. When requesting histogram=time with
     * time=2012-02-11T00:00:00.000Z, the histogram should only include features at that time.
     */
    @Test
    public void testVectorGetHistogramWithSameTimeFilter() throws Exception {
        // The full (unfiltered) time histogram has 4 features across 2012-02-11 and 2012-02-12
        // Filter by time=2012-02-11 should restrict to only the 3 features on that day
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1D"
                        + "&time=2012-02-11T00:00:00.000Z",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);
        checkXpathCount(filteredResult, "/md:Histogram[ows:Identifier='time']", "1");
        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        // Full unfiltered domain is "2012-02-11.../2012-02-12.../P1M" with values="4" (P1M resolution)
        // or "2012-02-11.../2012-02-13.../P1D" with values="3,1" (P1D resolution)
        // With same-dimension filter on 2012-02-11, the domain should be restricted
        // and values should total 3 (not 4)
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        assertThat(
                "Total histogram count should be 3 for time=2012-02-11 filter, " + "but got values: " + values,
                total,
                is(3));
    }

    /**
     * Tests that the vector elevation histogram respects a cross-dimension time filter and correctly reports filtered
     * values (not just empty/non-empty, but the actual histogram counts).
     */
    @Test
    public void testVectorGetElevationHistogramFilteredByTime() throws Exception {
        // Filter by time=2012-02-12 should leave only 1 feature (feature 1, startElevation=2)
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                        + "&time=2012-02-12T00:00:00.000Z",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);
        checkXpathCount(filteredResult, "/md:Histogram[ows:Identifier='elevation']", "1");
        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        // The full (unfiltered) elevation histogram domain is "1.0/6.0/1.0" with values "1,1,1,0,1"
        // With time=2012-02-12 filter, only 1 feature at elevation=2 should remain
        assertThat(
                "Elevation histogram domain should be restricted to around elevation=2 "
                        + "when filtered by time=2012-02-12, but got: " + domain,
                domain,
                is(not("1.0/6.0/1.0")));
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        assertThat("Total histogram count should be 1 for time=2012-02-12 filter", total, is(1));
    }

    @Test
    public void testRasterGetFeatureOperation() throws Exception {
        // perform the get feature operation request
        String queryRequest = "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME));
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
        String queryRequest = "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(RASTER_ELEVATION_TIME) + "&bbox=-1,-1,0,0");
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the no features were returned
        checkXpathCount(result, "/wmts:feature", "0");
    }

    @Test
    public void testVectorGetFeatureOperation() throws Exception {
        // perform the get histogram operation request
        String queryRequest = "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(getLayerId(VECTOR_ELEVATION_TIME));
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
        String queryRequest = "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326"
                .formatted(
                        getLayerId(VECTOR_ELEVATION_TIME) + "&time=2012-02-10T00:00:00.000Z/2012-02-11T00:00:00.000Z");
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
                getAsServletResponse("gwc/service/wmts?request~GetCapabilities!service~!'WMTS'version~'1.0.0");
        // this request should fail whit an exception report
        assertThat(response.getContentAsString(), containsString("Missing Request parameter"));
        assertThat(response.getStatus(), is(400));
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        return null;
    }

    @Test
    public void testVectorGetDomainValuesOnTime() throws Exception {
        // full domain (only 2 entries)
        String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                + getLayerId(VECTOR_ELEVATION_TIME)
                + "&TileMatrixSet=EPSG:4326&domain=time";
        Document dom = getAsDOM(baseRequest);
        print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("2012-02-11T00:00:00.000Z,2012-02-12T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

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
        String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                + getLayerId(RASTER_ELEVATION_TIME)
                + "&TileMatrixSet=EPSG:4326&domain=time";
        Document dom = getAsDOM(baseRequest);
        // print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z", "/md:DomainValues/md:Domain", dom);

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
        String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
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
        String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
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
        String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                + getLayerId(RASTER_CUSTOM)
                + "&TileMatrixSet=EPSG:4326&domain="
                + CUSTOM_DIMENSION_NAME;
        Document dom = getAsDOM(baseRequest);
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("3", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("CustomDimValueA,CustomDimValueB,CustomDimValueC", "/md:DomainValues/md:Domain", dom);

        // first page ascending
        dom = getAsDOM(baseRequest + "&limit=2");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo("CustomDimValueA,CustomDimValueB", "/md:DomainValues/md:Domain", dom);

        // second page ascending
        dom = getAsDOM(baseRequest + "&fromValue=CustomDimValueB&limit=2");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("CustomDimValueC", "/md:DomainValues/md:Domain", dom);

        // first page descending
        dom = getAsDOM(baseRequest + "&limit=2&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("CustomDimValueC,CustomDimValueB", "/md:DomainValues/md:Domain", dom);

        // second page descending
        dom = getAsDOM(baseRequest + "&fromValue=CustomDimValueB&limit=2&sort=desc");
        // print(dom);
        assertXpathEvaluatesTo(CUSTOM_DIMENSION_NAME, "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("CustomDimValueA", "/md:DomainValues/md:Domain", dom);
    }

    /**
     * Tests what happens when the "start/end/period" time parameter expands to more than 100 dates, which exceeds the
     * default maxTimes limit in TimeParser. When the TimeParser throws a ServiceException due to exceeding the limit,
     * KvpUtils.parse() catches it silently, leaving the time parameter as a raw unparsed String. This String then gets
     * passed to DimensionFilterBuilder which creates a nonsensical equality filter (startTime = "the raw string"),
     * effectively matching nothing or everything depending on the DB behavior. This reproduces the MapStore scenario
     * where zooming out on a year of data causes the histogram to return the full domain.
     */
    @Test
    public void testVectorGetHistogramWithLargeStartEndPeriodExceedsMaxTimes() throws Exception {
        // Send a time range spanning 200 days with P1D period = 200+ dates
        // This exceeds the default maxTimes limit of 100 in TimeParser
        // The TimeParser will throw ServiceException, KvpUtils.parse() will swallow it,
        // and the time parameter remains as a raw String
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                        + "&time=2011-06-01T00:00:00.000Z/2012-06-01T00:00:00.000Z/P1D",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        // When the start/end/period expands to >100 dates, TimeParser throws
        // ServiceException("More than 100 times specified"). Check what the server returns.
        String contentType = filteredResponse.getContentType();
        String body = filteredResponse.getContentAsString();
        // The server should return a valid histogram (text/xml), not an error
        assertThat(
                "Server returned error instead of histogram when start/end/period "
                        + "exceeds maxTimes limit (>100 dates). Content-Type: " + contentType + " Body: " + body,
                contentType,
                is("text/xml"));
    }

    /**
     * Tests what happens when the "start/end/period" time parameter expands to more than 100 dates but the range should
     * EXCLUDE some features. If the maxTimes limit causes the filter to be silently dropped, the histogram would
     * incorrectly include all features.
     */
    @Test
    public void testVectorGetHistogramWithLargeStartEndPeriodShouldExcludeFeatures() throws Exception {
        // Send a time range spanning 365 days BEFORE the data (all data is in Feb 2012)
        // time=2010-01-01/2010-12-31/P1D = 365 dates, all before any features
        // If filter works: 0 features matched -> empty histogram
        // If filter silently dropped: 4 features matched -> full domain
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                        + "&time=2010-01-01T00:00:00.000Z/2010-12-31T00:00:00.000Z/P1D",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        String contentType = filteredResponse.getContentType();
        String body = filteredResponse.getContentAsString();
        // The server should return a valid histogram (text/xml), not an error
        assertThat(
                "Server returned error instead of histogram when start/end/period "
                        + "exceeds maxTimes limit (365 dates). Content-Type: " + contentType + " Body: " + body,
                contentType,
                is("text/xml"));
    }

    /**
     * Reproduces the MapStore scenario: GetHistogram with time parameter in "start/end/period" format (e.g.
     * "2012-02-11T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1D") on a point-based vector time dimension. The histogram
     * should only return data within that time range, NOT the full domain.
     */
    @Test
    public void testVectorGetHistogramWithStartEndPeriodTimeFilter() throws Exception {
        // Test with start/end/period format - this is what MapStore sends
        // Only features on 2012-02-11 should be included (features 0, 2, 3)
        // Feature 1 (startTime=2012-02-12) should be excluded
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                        + "&time=2012-02-11T00:00:00.000Z/2012-02-11T00:00:00.000Z/P1D",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);

        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        // With time=2012-02-11/2012-02-11/P1D, only features 0, 2, 3 match (startTime=02-11)
        // Full unfiltered histogram has total=4. Filtered should have total=3.
        assertThat(
                "Histogram with start/end/period time filter should exclude Feature 1 "
                        + "(startTime=02-12), but got values: " + values + " domain: " + domain,
                total,
                is(3));
    }

    /**
     * Reproduces the MapStore scenario with range dimensions: GetHistogram with time parameter in "start/end/period"
     * format on a range-based vector time dimension (startTime/endTime). The histogram should only return data that
     * intersects the specified time range.
     *
     * <p>With time=2012-02-12T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1D and range time dimension: Feature 0 [02-11,
     * 02-11T11:00] -> excluded (endTime before 02-12), Feature 1 [02-12, 02-12T10:00] -> matches, Feature 2 [02-11,
     * 02-13] -> matches, Feature 3 [02-11, 02-13] -> matches. So elevation domain should NOT start at 1.0 (Feature 0's
     * startElev) since it is excluded.
     */
    @Test
    public void testVectorGetHistogramWithStartEndPeriodOnRangeDimension() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", "endTime", DimensionPresentation.LIST, minimumValue());

            // Use start/end/period format covering only 2012-02-12
            // Feature 0 [02-11, 02-11T11:00] -> endTime < 02-12 -> excluded
            String filteredRequest = String.format(
                    "request=GetHistogram&Version=1.0.0&Layer=%s"
                            + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                            + "&time=2012-02-12T00:00:00.000Z/2012-02-12T00:00:00.000Z/P1D",
                    getLayerId(VECTOR_ELEVATION_TIME));
            MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
            Document filteredResult = getResultAsDocument(filteredResponse);
            print(filteredResult);

            String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
            String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
            int total = 0;
            for (String v : values.split(",")) {
                if (!v.isEmpty()) total += Integer.parseInt(v);
            }
            // Feature 0 (startElev=1) is excluded, so domain should NOT start at 1.0
            // Full unfiltered domain is "1.0/6.0/1.0" (4 features, startElev only)
            assertThat(
                    "Histogram with start/end/period time filter on range dimension "
                            + "should exclude Feature 0, domain should start at 2.0, "
                            + "but got values: " + values + " domain: " + domain,
                    domain,
                    is(not("1.0/6.0/1.0")));
            // Should have 3 matching features
            assertThat("Total count should be 3", total, is(3));
        } finally {
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", null, DimensionPresentation.LIST, minimumValue());
        }
    }

    /**
     * Reproduces the MapStore scenario with a TWO-PART time range (no period) that is WIDER than all data. This is
     * exactly what happens when MapStore sends time=2023-07-28/2027-09-26 but the layer data only spans 2025. Since the
     * filter encompasses all data, the histogram domain equals the full data extent. This test verifies that the
     * behavior is CORRECT (not a bug) — the domain represents actual data extent, not the requested filter bounds.
     */
    @Test
    public void testVectorGetHistogramWithWideRangeTimeFilter() throws Exception {
        // Two-part range that encompasses ALL data (data is in Feb 2012)
        // time=2010-01-01/2015-01-01 covers everything
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                        + "&time=2010-01-01T00:00:00.000Z/2015-01-01T00:00:00.000Z",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);

        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        // Full unfiltered domain is "1.0/6.0/1.0" with values "1,1,1,0,1" (total=4)
        // With a WIDE range encompassing all data, the result should be IDENTICAL
        // This is the expected behavior — not a bug
        assertThat(
                "With wide two-part time range encompassing all data, " + "domain should equal full data extent",
                domain,
                is("1.0/6.0/1.0"));
        assertThat(
                "With wide two-part time range encompassing all data, " + "all 4 features should be included",
                total,
                is(4));
    }

    /**
     * Proves that the TWO-PART time filter (no period) DOES correctly filter data when the range is narrower than the
     * full data extent. With time=2012-02-12/2012-02-13, only Feature 1 (startTime=2012-02-12) should match for
     * point-based time dimension. This test definitively shows the two-part format filtering works in GeoServer — the
     * user's real-world MapStore scenario returned the full domain only because the filter range encompassed all data.
     */
    @Test
    public void testVectorGetHistogramWithNarrowTwoPartTimeFilter() throws Exception {
        // Two-part range covering only 2012-02-12 to 2012-02-13
        // For point dimension (startTime only):
        // Feature 0: startTime=02-11 -> NOT in [02-12, 02-13] -> excluded
        // Feature 1: startTime=02-12 -> IN [02-12, 02-13] -> included
        // Feature 2: startTime=02-11 -> NOT in [02-12, 02-13] -> excluded
        // Feature 3: startTime=02-11 -> NOT in [02-12, 02-13] -> excluded
        String filteredRequest = String.format(
                "request=GetHistogram&Version=1.0.0&Layer=%s"
                        + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                        + "&time=2012-02-12T00:00:00.000Z/2012-02-13T00:00:00.000Z",
                getLayerId(VECTOR_ELEVATION_TIME));
        MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
        Document filteredResult = getResultAsDocument(filteredResponse);
        print(filteredResult);

        String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
        String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
        int total = 0;
        for (String v : values.split(",")) {
            if (!v.isEmpty()) total += Integer.parseInt(v);
        }
        // Only Feature 1 (elevation=2) should match
        // Domain should NOT be "1.0/6.0/1.0" (full extent) — it should be narrower
        assertThat(
                "With narrow two-part time range 2012-02-12/2012-02-13, "
                        + "only Feature 1 (elev=2) should match. Domain should not be full extent. "
                        + "Got values: " + values + " domain: " + domain,
                total,
                is(1));
        assertThat(
                "Domain should be restricted to elevation=2 area, not the full 1.0/6.0",
                domain,
                is(not("1.0/6.0/1.0")));
    }

    /**
     * Tests GetHistogram on a vector dataset with BOTH start and end time attributes (range dimension). When filtering
     * the time histogram by elevation=1 (range: startElev<=1 AND endElev>=1), only Feature 0 matches
     * (startTime=2012-02-11, endTime=2012-02-11T11:00), so the time histogram should be restricted to that single
     * feature's time range.
     */
    @Test
    public void testVectorGetHistogramWithRangeDimensionsFilteredByElevation() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            // Register both time and elevation as range dimensions (with endAttribute)
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", "endTime", DimensionPresentation.LIST, minimumValue());
            registerLayerDimension(
                    vectorInfo,
                    ResourceInfo.ELEVATION,
                    "startElevation",
                    "endElevation",
                    DimensionPresentation.LIST,
                    minimumValue());

            // Request time histogram filtered by elevation=1
            // Only Feature 0 has elevation range [1,2] which includes 1
            String filteredRequest = String.format(
                    "request=GetHistogram&Version=1.0.0&Layer=%s"
                            + "&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1D&elevation=1",
                    getLayerId(VECTOR_ELEVATION_TIME));
            MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
            Document filteredResult = getResultAsDocument(filteredResponse);
            print(filteredResult);

            String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
            String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
            int total = 0;
            for (String v : values.split(",")) {
                if (!v.isEmpty()) total += Integer.parseInt(v);
            }
            // Only Feature 0 matches elevation=1 (range [1,2]), so total count should be 1
            assertThat(
                    "With range dimensions, time histogram filtered by elevation=1 should "
                            + "contain only 1 feature, but got values: " + values + " domain: " + domain,
                    total,
                    is(1));
        } finally {
            // Restore original point-based dimensions
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", null, DimensionPresentation.LIST, minimumValue());
            registerLayerDimension(
                    vectorInfo,
                    ResourceInfo.ELEVATION,
                    "startElevation",
                    null,
                    DimensionPresentation.CONTINUOUS_INTERVAL,
                    minimumValue());
        }
    }

    /**
     * Tests GetHistogram on a vector dataset with BOTH start and end time/elevation attributes (range dimensions). When
     * filtering by time=2012-02-12T00:00:00.000Z, features whose time range intersects that instant should be included:
     * Feature 1 ([02-12, 02-12T10:00]), Feature 2 ([02-11, 02-13]), Feature 3 ([02-11, 02-13]) = 3 features.
     *
     * <p>With range elevation dimensions, the histogram domain should start at 2.0 (Feature 1's startElev), not 1.0
     * (Feature 0's startElev is excluded). Each feature is counted in every elevation bucket its range intersects, so
     * the total sum of values exceeds the feature count.
     *
     * <p>Bucket breakdown (domain "2.0/8.0/1.0", 6 buckets):
     *
     * <ul>
     *   <li>[2,3): Feature 1 [2,3] → 1
     *   <li>[3,4): Feature 1 [2,3], Feature 2 [3,4] → 2
     *   <li>[4,5): Feature 2 [3,4] → 1
     *   <li>[5,6): Feature 3 [5,7] → 1
     *   <li>[6,7): Feature 3 [5,7] → 1
     *   <li>[7,8): Feature 3 [5,7] → 1
     * </ul>
     *
     * Values: "1,2,1,1,1,1" (total=7)
     */
    @Test
    public void testVectorGetElevationHistogramWithRangeTimeFilteredByTime() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            // Register both time and elevation as range dimensions (with endAttribute)
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", "endTime", DimensionPresentation.LIST, minimumValue());
            registerLayerDimension(
                    vectorInfo,
                    ResourceInfo.ELEVATION,
                    "startElevation",
                    "endElevation",
                    DimensionPresentation.LIST,
                    minimumValue());

            // Request elevation histogram filtered by time=2012-02-12
            String filteredRequest = String.format(
                    "request=GetHistogram&Version=1.0.0&Layer=%s"
                            + "&TileMatrixSet=EPSG:4326&histogram=elevation&resolution=1"
                            + "&time=2012-02-12T00:00:00.000Z",
                    getLayerId(VECTOR_ELEVATION_TIME));
            MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
            Document filteredResult = getResultAsDocument(filteredResponse);
            print(filteredResult);

            String domain = xpath.evaluate("/md:Histogram/md:Domain", filteredResult);
            String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
            // The full (unfiltered) elevation domain with all 4 features is "1.0/8.0/1.0"
            // With time=2012-02-12 filter, Feature 0 (elevation [1,2]) is excluded,
            // so domain should start at 2.0, not 1.0
            assertThat(
                    "Elevation histogram domain should be restricted (start at 2.0) when "
                            + "Feature 0 is excluded by time filter, but got: " + domain,
                    domain,
                    is("2.0/8.0/1.0"));
            // Range histograms count each feature in every bucket its range intersects
            assertThat("Elevation histogram values for 3 matching features with range dims", values, is("1,2,1,1,1,1"));
        } finally {
            // Restore original point-based dimensions
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", null, DimensionPresentation.LIST, minimumValue());
            registerLayerDimension(
                    vectorInfo,
                    ResourceInfo.ELEVATION,
                    "startElevation",
                    null,
                    DimensionPresentation.CONTINUOUS_INTERVAL,
                    minimumValue());
        }
    }

    /**
     * Tests GetHistogram for same-dimension filtering with range dimensions. When requesting histogram=time with
     * time=2012-02-11T00:00:00.000Z and both startTime/endTime configured, the time range filter should intersect:
     * Feature 0 ([02-11, 02-11T11:00]), Feature 2 ([02-11, 02-13]), Feature 3 ([02-11, 02-13]) = 3 features. Feature 1
     * ([02-12, 02-12T10:00]) does NOT include 02-11 and is excluded.
     *
     * <p>With range time dimensions, the histogram domain spans the full time extent of matching features. The domain
     * "2012-02-11.../2012-02-14.../P1D" has 3 buckets. Compared to the unfiltered histogram values "3,3,2" (all 4
     * features), the filtered values should be "3,2,2" because Feature 1 is excluded from bucket [02-12, 02-13).
     */
    @Test
    public void testVectorGetTimeHistogramWithRangeTimeSameDimensionFilter() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            // Register time as range dimension (with endAttribute)
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", "endTime", DimensionPresentation.LIST, minimumValue());

            // Request time histogram filtered by time=2012-02-11 (same dimension)
            String filteredRequest = String.format(
                    "request=GetHistogram&Version=1.0.0&Layer=%s"
                            + "&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1D"
                            + "&time=2012-02-11T00:00:00.000Z",
                    getLayerId(VECTOR_ELEVATION_TIME));
            MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
            Document filteredResult = getResultAsDocument(filteredResponse);
            print(filteredResult);

            String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
            // Unfiltered range histogram would be "3,3,2" (all 4 features across 3 daily buckets)
            // With time=2012-02-11 filter, Feature 1 is excluded. Feature 1 only appeared in
            // bucket [02-12, 02-13), reducing that bucket from 3 to 2.
            // Result should be "3,2,2"
            assertThat(
                    "With range time dimension, time histogram filtered by time=2012-02-11 "
                            + "should exclude Feature 1 from second bucket",
                    values,
                    is("3,2,2"));
        } finally {
            // Restore original point-based dimension
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", null, DimensionPresentation.LIST, minimumValue());
        }
    }

    /**
     * Tests GetHistogram with a time RANGE filter (start/end) on range dimensions. When filtering by
     * time=2012-02-11T12:00:00.000Z/2012-02-12T00:00:00.000Z, Feature 0 ends at 02-11T11:00 (before the filter start
     * 02-11T12:00) so it is excluded. Features 1, 2, 3 all have time ranges that intersect the filter interval.
     *
     * <p>Compared to the unfiltered histogram "3,3,2", the first bucket [02-11, 02-12) drops from 3 to 2 because
     * Feature 0 is excluded. Result: "2,3,2".
     */
    @Test
    public void testVectorGetTimeHistogramWithRangeFilterInterval() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            // Register time as range dimension (with endAttribute)
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", "endTime", DimensionPresentation.LIST, minimumValue());

            // Request time histogram with a time RANGE filter
            String filteredRequest = String.format(
                    "request=GetHistogram&Version=1.0.0&Layer=%s"
                            + "&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1D"
                            + "&time=2012-02-11T12:00:00.000Z/2012-02-12T00:00:00.000Z",
                    getLayerId(VECTOR_ELEVATION_TIME));
            MockHttpServletResponse filteredResponse = getAsServletResponse("gwc/service/wmts?" + filteredRequest);
            Document filteredResult = getResultAsDocument(filteredResponse);
            print(filteredResult);

            String values = xpath.evaluate("/md:Histogram/md:Values", filteredResult);
            // Feature 0 ends at 02-11T11:00 (before filter start 02-11T12:00), excluded
            // First bucket [02-11, 02-12) drops from 3 to 2 (Feature 0 removed)
            // Result: "2,3,2" instead of unfiltered "3,3,2"
            assertThat(
                    "With range time dimension and time range filter "
                            + "2012-02-11T12:00/2012-02-12T00:00, Feature 0 should be excluded "
                            + "from first bucket",
                    values,
                    is("2,3,2"));
        } finally {
            // Restore original point-based dimension
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", null, DimensionPresentation.LIST, minimumValue());
        }
    }

    @Test
    public void testTileLayerNotInstanceOfGeoServerTileLayer() throws IOException {
        TileLayer tileLayer = mock(TileLayer.class);
        TileLayerDispatcher tld = mock(TileLayerDispatcher.class);
        XMLBuilder xml = new XMLBuilder(new StringBuilder());
        MultiDimensionalExtension extension =
                new MultiDimensionalExtension(getGeoServer(), getWMS(), getCatalog(), tld);
        // No exception is thrown when the layer isn't a GeoServerTileLayer whilst
        // the call below  was used to throw an exception when dealing with different
        // TileLayer implementations
        extension.encodeLayer(xml, tileLayer);
    }

    @Test
    public void testVectorGetDomainValuesTimeByEndAttribute() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", "endTime", DimensionPresentation.LIST, minimumValue());

            String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                    + getLayerId(VECTOR_ELEVATION_TIME)
                    + "&TileMatrixSet=EPSG:4326&domain=time";

            Document dom = getAsDOM(baseRequest + "&fromValue=2012-02-12T09:00:00.000Z&fromEnd=true");

            assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
            // sorted by end date too
            assertXpathEvaluatesTo(
                    "2012-02-12T00:00:00.000Z/2012-02-12T10:00:00.000Z,2012-02-11T00:00:00.000Z/2012-02-13T00:00:00.000Z",
                    "/md:DomainValues/md:Domain",
                    dom);
        } finally {
            registerLayerDimension(
                    vectorInfo, ResourceInfo.TIME, "startTime", null, DimensionPresentation.LIST, minimumValue());
        }
    }

    @Test
    public void testVectorGetDomainValuesOnElevationsByEndAttribute() throws Exception {
        FeatureTypeInfo vectorInfo = getCatalog().getFeatureTypeByName(VECTOR_ELEVATION_TIME.getLocalPart());
        try {
            registerLayerDimension(
                    vectorInfo,
                    ResourceInfo.ELEVATION,
                    "startElevation",
                    "endElevation",
                    DimensionPresentation.LIST,
                    minimumValue());
            // full domain (only 2 entries)
            String baseRequest = "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                    + getLayerId(VECTOR_ELEVATION_TIME)
                    + "&TileMatrixSet=EPSG:4326&domain=elevation";
            Document dom = getAsDOM(baseRequest + "&fromValue=3.5&fromEnd=true&limit=3");
            assertXpathEvaluatesTo("elevation", "/md:DomainValues/ows:Identifier", dom);
            assertXpathEvaluatesTo("3", "/md:DomainValues/md:Limit", dom);
            assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
            assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
            assertXpathEvaluatesTo("3.0/4.0,5.0/7.0", "/md:DomainValues/md:Domain", dom);
        } finally {
            registerLayerDimension(
                    vectorInfo,
                    ResourceInfo.ELEVATION,
                    "startElevation",
                    null,
                    DimensionPresentation.LIST,
                    minimumValue());
        }
    }
}
