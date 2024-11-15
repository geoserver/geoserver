/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.wmts.dimensions.Dimension;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class SidecarTypeTest extends TestsSupport {

    protected String getTestLayerId() {
        return getLayerId(VECTOR_ELEVATION_TIME);
    }

    @Override
    protected void afterSetup(SystemTestData testData) throws IOException {
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

    @After
    public void cleanupVectorSidecar() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo vector = catalog.getFeatureTypeByName(getTestLayerId());
        vector.getMetadata().remove(MultiDimensionalExtension.SIDECAR_TYPE);
        catalog.save(vector);
    }

    protected void setupVectorSidecar() throws Exception {
        Catalog catalog = getCatalog();
        FeatureTypeInfo vector = catalog.getFeatureTypeByName(getTestLayerId());
        vector.getMetadata()
                .put(MultiDimensionalExtension.SIDECAR_TYPE, SIDECAR_VECTOR_ET.getLocalPart());
        catalog.save(vector);
    }

    @Override
    protected Dimension buildDimension(DimensionInfo dimensionInfo) {
        return null;
    }

    @Test
    public void testVectorSidecarDescribeDomainsOperation() throws Exception {
        // setup sidecar, has different values than the main table, checking that indeed it's being
        // used
        setupVectorSidecar();

        // perform the get describe domains operation request
        String queryRequest =
                String.format(
                        "request=DescribeDomains&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getTestLayerId());
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
        checkXpathCount(
                result, "/md:Domains/md:DimensionDomain[md:Domain='11.0,12.0,13.0,15.0']", "1");
        // check the time domain
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[ows:Identifier='time' and md:Size='2']",
                "1");
        checkXpathCount(
                result,
                "/md:Domains/md:DimensionDomain[md:Domain='2011-02-11T00:00:00.000Z,2011-02-12T00:00:00.000Z']",
                "1");
        // check the space domain
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@CRS='EPSG:4326']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@minx='-170.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@miny='-80.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxx='170.0']", "1");
        checkXpathCount(result, "/md:Domains/md:SpaceDomain/md:BoundingBox[@maxy='80.0']", "1");
    }

    @Test
    public void testVectorSidecarGetHistogramOperationForTime() throws Exception {
        // setup sidecar, it has different values on purpose
        setupVectorSidecar();

        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetHistogram&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326&histogram=time&resolution=P1M",
                        getTestLayerId());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response);
        print(result);
        // check the returned histogram
        checkXpathCount(result, "/md:Histogram[ows:Identifier='time']", "1");
        checkXpathCount(
                result,
                "/md:Histogram[md:Domain="
                        + "'2011-02-11T00:00:00.000Z/2011-02-12T00:00:00.000Z/P1M']",
                "1");
        checkXpathCount(result, "/md:Histogram[md:Values='4']", "1");
    }

    @Test
    public void testVectorSidecarGetFeatureOperation() throws Exception {
        // setup sidecar, it has different values on purpose
        setupVectorSidecar();

        // perform the get histogram operation request
        String queryRequest =
                String.format(
                        "request=GetFeature&Version=1.0.0&Layer=%s&TileMatrixSet=EPSG:4326",
                        getTestLayerId());
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wmts?" + queryRequest);
        Document result = getResultAsDocument(response, "text/xml; subtype=gml/3.1.1");
        // check the returned features
        checkXpathCount(result, "/wmts:feature", "4");
        checkXpathCount(result, "/wmts:feature/wmts:footprint/gml:Polygon", "4");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='11.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='12.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='13.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='15.0']", "1");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2011-02-11T00:00:00.000Z']", "3");
        checkXpathCount(result, "/wmts:feature[wmts:dimension='2011-02-12T00:00:00.000Z']", "1");
    }

    @Test
    public void testVectorSidecarGetDomainValuesOnTime() throws Exception {
        // setup sidecar, has different domain values on purpose
        setupVectorSidecar();

        // full domain (only 2 entries)
        String baseRequest =
                "gwc/service/wmts?request=GetDomainValues&Version=1.0.0&Layer="
                        + getTestLayerId()
                        + "&TileMatrixSet=EPSG:4326&domain=time";
        Document dom = getAsDOM(baseRequest);
        print(dom);
        assertXpathEvaluatesTo("time", "/md:DomainValues/ows:Identifier", dom);
        assertXpathEvaluatesTo("1000", "/md:DomainValues/md:Limit", dom);
        assertXpathEvaluatesTo("asc", "/md:DomainValues/md:Sort", dom);
        assertXpathEvaluatesTo("2", "/md:DomainValues/md:Size", dom);
        assertXpathEvaluatesTo(
                "2011-02-11T00:00:00.000Z,2011-02-12T00:00:00.000Z",
                "/md:DomainValues/md:Domain",
                dom);
    }
}
