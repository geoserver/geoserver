package org.geoserver.restconfig.model.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geoserver.openapi.model.catalog.CoverageInfo;
import org.geoserver.openapi.v1.model.CoverageDimensionResponse;
import org.geoserver.openapi.v1.model.CoverageResponse;
import org.geoserver.openapi.v1.model.CoverageResponseWrapper;
import org.geoserver.openapi.v1.model.NumberRangeResponse;
import org.geoserver.restconfig.model.SerializationTest;
import org.junit.Test;

public class CoverageResponseSerializationTest extends SerializationTest {

    public @Test void testCoverageResponse() throws IOException {
        CoverageResponseWrapper crw =
                decode("CoverageResponse.json", CoverageInfo.class, CoverageResponseWrapper.class);
        assertNotNull(crw);
        CoverageResponse cr = crw.getCoverage();
        assertNotNull(cr);
        assertEquals("sfdem", cr.getName());
        assertEquals("sfdem", cr.getNativeName());
        assertNotNull(cr.getNamespace());
        assertEquals("sf", cr.getNamespace().getName());
        assertEquals(
                "http://localhost:8080/geoserver/rest/namespaces/sf.json",
                cr.getNamespace().getHref());
        assertEquals("Spearfish elevation", cr.getTitle());
        assertEquals("Generated from sfdem", cr.getDescription());
        assertEquals(
                "Digital elevation model for the Spearfish region.\r\n\r\nsfdem is a Tagged Image File Format with Geographic information",
                cr.getAbstract());
        assertEquals(Arrays.asList("WCS", "sfdem", "sfdem"), cr.getKeywords().getString());
        assertResponseCRS(cr.getNativeCRS(), "projected", "PROJCS[\"NAD27 ");
        assertEquals("EPSG:26713", cr.getSrs());

        assertResponseBounds(cr.getNativeBoundingBox(), 589980, 609000, 4913700, 4928010);
        assertResponseCRS(cr.getNativeBoundingBox().getCrs(), "projected", "EPSG:26713");

        assertResponseBounds(
                cr.getLatLonBoundingBox(),
                -103.87108701853181,
                -103.62940739432703,
                44.370187074132616,
                44.5016011535299);
        assertEquals(cr.getLatLonBoundingBox().getCrs(), "EPSG:4326");

        assertEquals(Boolean.TRUE, cr.getEnabled());
        //		assertEquals("", cr.getMetadata());
        assertEquals("sf:sfdem", cr.getStore().getName());
        assertEquals("coverageStore", cr.getStore().getAtClass());
        assertEquals(Boolean.FALSE, cr.getServiceConfiguration());
        assertEquals("GeoTIFF", cr.getNativeFormat());

        assertNotNull(cr.getGrid());
        assertEquals("2", cr.getGrid().getAtDimension());
        assertEquals("EPSG:26713", cr.getGrid().getCrs());

        assertNotNull(cr.getGrid().getRange());
        assertEquals("0 0", cr.getGrid().getRange().getLow());
        assertEquals("634 477", cr.getGrid().getRange().getHigh());

        assertNotNull(cr.getGrid().getTransform());
        assertEquals(30, cr.getGrid().getTransform().getScaleX().doubleValue(), 1e-6);
        assertEquals(-30, cr.getGrid().getTransform().getScaleY().doubleValue(), 1e-6);
        assertEquals(0, cr.getGrid().getTransform().getShearX().doubleValue(), 1e-6);
        assertEquals(0, cr.getGrid().getTransform().getShearY().doubleValue(), 1e-6);
        assertEquals(589995, cr.getGrid().getTransform().getTranslateX().doubleValue(), 1e-6);
        assertEquals(4927995, cr.getGrid().getTransform().getTranslateY().doubleValue(), 1e-6);

        assertNotNull(cr.getSupportedFormats());
        assertEquals(
                Arrays.asList("ARCGRID", "IMAGEMOSAIC", "GEOTIFF", "GIF", "PNG", "JPEG", "TIFF"),
                cr.getSupportedFormats().getString());

        assertNotNull(cr.getInterpolationMethods());
        assertEquals(
                Arrays.asList("nearest neighbor", "bilinear", "bicubic"),
                cr.getInterpolationMethods().getString());
        assertEquals("nearest neighbor", cr.getDefaultInterpolationMethod());

        assertNotNull(cr.getDimensions());
        assertNotNull(cr.getDimensions().getCoverageDimension());
        assertEquals(1, cr.getDimensions().getCoverageDimension().size());

        CoverageDimensionResponse cd = cr.getDimensions().getCoverageDimension().get(0);
        assertEquals(
                "GridSampleDimension[-9.999999933815813E36,-9.999999933815813E36]",
                cd.getDescription());
        assertEquals(
                new NumberRangeResponse().min("-9.999999933815813E36").max("-9.999999933815813E36"),
                cd.getRange());

        assertEquals(Collections.singletonList("EPSG:26713"), cr.getRequestSRS().getString());
        assertEquals(Collections.singletonList("EPSG:26713"), cr.getResponseSRS().getString());
    }

    public @Test void testCoverageResponse_GridSampleDimension_Infinite() throws IOException {
        CoverageResponseWrapper crw =
                decode(
                        "CoverageResponse_GridSampleDimension_Infinity.json",
                        CoverageInfo.class,
                        CoverageResponseWrapper.class);
        assertNotNull(crw);
        CoverageResponse cr = crw.getCoverage();
        assertNotNull(cr);
        assertEquals("PublishedName", cr.getName());
        assertEquals("PublishedName", cr.getNativeName());
        assertEquals("sfdem", cr.getNativeCoverageName());

        assertNotNull(cr.getDimensions());
        assertNotNull(cr.getDimensions().getCoverageDimension());
        assertEquals(1, cr.getDimensions().getCoverageDimension().size());

        CoverageDimensionResponse cd = cr.getDimensions().getCoverageDimension().get(0);
        assertEquals("GridSampleDimension[-Infinity,Infinity]", cd.getDescription());
        assertEquals(new NumberRangeResponse().min("-inf").max("inf"), cd.getRange());
        List<Double> nullValues = cd.getNullValues().getDouble();
        assertEquals(Arrays.asList(-9.999999933815813E36d), nullValues);
    }

    /**
     * Difference between geoserver 2.15.x and 2.16.x, nativeCoverageName can be on the "parameters"
     * map instead of a proper property
     *
     * @throws IOException
     */
    public @Test void testCoverageResponse_NativeCoverageNameAsParameter() throws IOException {
        CoverageResponseWrapper crw =
                decode(
                        "CoverageResponse_NativeCoverageNameAsParameter.json",
                        CoverageInfo.class,
                        CoverageResponseWrapper.class);
        assertNotNull(crw);
        CoverageResponse cr = crw.getCoverage();
        assertNotNull(cr);
        assertEquals("PublishedName", cr.getName());
        assertEquals("PublishedName", cr.getNativeName());
        assertNull(cr.getNativeCoverageName());
        Map<String, Object> parameters = cr.getParameters();
        assertEquals("sfdem", parameters.get("nativeCoverageName"));
    }
}
