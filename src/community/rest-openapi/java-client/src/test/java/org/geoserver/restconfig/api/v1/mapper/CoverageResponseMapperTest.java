package org.geoserver.restconfig.api.v1.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.geoserver.openapi.model.catalog.CoverageDimensionInfo;
import org.geoserver.openapi.model.catalog.CoverageInfo;
import org.geoserver.openapi.model.catalog.NumberRange;
import org.geoserver.openapi.v1.model.CoverageResponse;
import org.geoserver.openapi.v1.model.CoverageResponseWrapper;
import org.geoserver.restconfig.model.SerializationTest;
import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

public class CoverageResponseMapperTest extends SerializationTest {

    private CoverageResponseMapper mapper;

    public @Before void before() {
        mapper = Mappers.getMapper(CoverageResponseMapper.class);
        assertNotNull(mapper);
    }

    public @Test void testMapCoverageResponseMapperToCoverageInfo() throws IOException {

        final CoverageInfo expected =
                super.decode("CoverageInfo.json", CoverageInfo.class, CoverageInfo.class);

        final CoverageResponseWrapper responseWrapper =
                super.decode(
                        "CoverageResponse.json", CoverageInfo.class, CoverageResponseWrapper.class);
        final CoverageResponse response = responseWrapper.getCoverage();
        CoverageInfo mapped = mapper.map(response);

        assertEquals(expected.getAbstract(), mapped.getAbstract());
        assertEquals(expected.getAdvertised(), mapped.getAdvertised());
        assertEquals(expected.getAlias(), mapped.getAlias());
        assertEquals(
                expected.getDefaultInterpolationMethod(), mapped.getDefaultInterpolationMethod());

        // TODO
        // assertEquals(expected.getDataLinks(), mapped.getDataLinks());
        // assertEquals(expected.getGrid(), mapped.getGrid());

        assertEquals(expected.getDescription(), mapped.getDescription());
        assertEquals(expected.getDimensions(), mapped.getDimensions());
        assertEquals(expected.getDisabledServices(), mapped.getDisabledServices());
        assertEquals(expected.getEnabled(), mapped.getEnabled());
        assertEquals(expected.getInterpolationMethods(), mapped.getInterpolationMethods());
        assertEquals(expected.getKeywords(), mapped.getKeywords());
        assertEquals(expected.getLatLonBoundingBox(), mapped.getLatLonBoundingBox());

        assertEquals(expected.getMetadataLinks(), mapped.getMetadataLinks());
        assertEquals(expected.getName(), mapped.getName());

        // ignore the isolated property, doesn't come in the response.
        assertEquals(expected.getNamespace().getPrefix(), mapped.getNamespace().getPrefix());
        assertEquals(expected.getNamespace().getUri(), mapped.getNamespace().getUri());

        assertEquals(expected.getNativeBoundingBox(), mapped.getNativeBoundingBox());
        assertEquals(expected.getNativeFormat(), mapped.getNativeFormat());
        assertEquals(expected.getNativeCRS(), mapped.getNativeCRS());
        assertEquals(expected.getNativeName(), mapped.getNativeName());

        assertEquals(expected.getProjectionPolicy(), mapped.getProjectionPolicy());
        assertEquals(expected.getRequestSRS(), mapped.getRequestSRS());
        assertEquals(expected.getResponseSRS(), mapped.getResponseSRS());
        assertEquals(expected.getServiceConfiguration(), mapped.getServiceConfiguration());

        assertEquals(expected.getSupportedFormats(), mapped.getSupportedFormats());
        assertEquals(expected.getSrs(), mapped.getSrs());
        assertEquals(expected.getStore(), mapped.getStore());
        assertEquals(expected.getTitle(), mapped.getTitle());
    }

    public @Test void testMapCoverageResponseMapperToCoverageInfo_Infinity() throws IOException {

        final CoverageResponseWrapper responseWrapper =
                super.decode(
                        "CoverageResponse_GridSampleDimension_Infinity.json",
                        CoverageInfo.class,
                        CoverageResponseWrapper.class);
        final CoverageResponse response = responseWrapper.getCoverage();

        CoverageInfo mapped = mapper.map(response);

        List<CoverageDimensionInfo> dimensions = mapped.getDimensions();
        assertEquals(1, dimensions.size());
        CoverageDimensionInfo dim = dimensions.get(0);
        assertEquals("GRAY_INDEX", dim.getName());
        assertEquals("GridSampleDimension[-Infinity,Infinity]", dim.getDescription());
        assertEquals(Collections.singletonList(-9.999999933815813E36d), dim.getNullValues());
        NumberRange range = dim.getRange();
        assertNotNull(range);
        assertTrue(Double.NEGATIVE_INFINITY == range.getMin().doubleValue());
        assertTrue(Double.POSITIVE_INFINITY == range.getMax().doubleValue());
    }

    /**
     * Difference between geoserver 2.15.x and 2.16.x, nativeCoverageName can be on the "parameters"
     * map instead of a proper property
     *
     * @throws IOException
     */
    public @Test void testMapCoverageResponseMapperToCoverageInfo_NativeCoverageNameAsParameter()
            throws IOException {
        CoverageResponseWrapper crw =
                super.decode(
                        "CoverageResponse_NativeCoverageNameAsParameter.json",
                        CoverageInfo.class,
                        CoverageResponseWrapper.class);
        assertNotNull(crw);
        CoverageResponse response = crw.getCoverage();
        assertNotNull(response);

        CoverageInfo mapped = mapper.map(response);

        assertEquals("PublishedName", mapped.getName());
        assertEquals("PublishedName", mapped.getNativeName());
        assertEquals("sfdem", mapped.getNativeCoverageName());
    }
}
