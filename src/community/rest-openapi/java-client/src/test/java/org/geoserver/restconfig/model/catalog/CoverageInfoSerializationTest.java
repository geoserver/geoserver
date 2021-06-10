package org.geoserver.restconfig.model.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.geoserver.openapi.model.catalog.CoverageInfo;
import org.geoserver.openapi.model.catalog.KeywordInfo;
import org.geoserver.openapi.model.catalog.NumberRange;
import org.geoserver.restconfig.model.SerializationTest;
import org.hamcrest.core.StringStartsWith;
import org.junit.Test;

public class CoverageInfoSerializationTest extends SerializationTest {

    public @Test void testCoverageResponse() throws IOException {
        CoverageInfo cr = decode("CoverageInfo.json", CoverageInfo.class, CoverageInfo.class);
        assertNotNull(cr);
        assertEquals("sfdem", cr.getName());
        assertEquals("sfdem", cr.getNativeName());
        assertNotNull(cr.getNamespace());
        assertEquals("sf", cr.getNamespace().getPrefix());
        assertEquals(
                "http://localhost:8080/geoserver/rest/namespaces/sf.json",
                cr.getNamespace().getUri());
        assertEquals("Spearfish elevation", cr.getTitle());
        assertEquals("Generated from sfdem", cr.getDescription());
        assertEquals(
                "Digital elevation model for the Spearfish region.\r\n\r\nsfdem is a Tagged Image File Format with Geographic information",
                cr.getAbstract());
        assertEquals(
                Arrays.asList(
                        new KeywordInfo().value("WCS"),
                        new KeywordInfo().value("sfdem"),
                        new KeywordInfo().value("sfdem")),
                cr.getKeywords());
        assertThat(cr.getNativeCRS(), StringStartsWith.startsWith("PROJCS[\"NAD27 "));
        assertEquals("EPSG:26713", cr.getSrs());

        assertResponseBounds(
                cr.getNativeBoundingBox(), 589980, 609000, 4913700, 4928010, "EPSG:26713");
        assertResponseBounds(
                cr.getLatLonBoundingBox(),
                -103.87108701853181,
                -103.62940739432703,
                44.370187074132616,
                44.5016011535299,
                "EPSG:4326");

        assertEquals(Boolean.TRUE, cr.getEnabled());
        //		assertEquals("", cr.getMetadata());
        assertEquals("sfdem", cr.getStore().getName());
        assertEquals("sf", cr.getStore().getWorkspace().getName());
        assertEquals(Boolean.FALSE, cr.getServiceConfiguration());
        assertEquals("GeoTIFF", cr.getNativeFormat());

        // TODO:
        //		assertEquals("2", cr.getGrid().getAtDimension());
        //		assertEquals("EPSG:26713", cr.getGrid().getCrs());
        //
        //		assertNotNull(cr.getGrid().getRange());
        //		assertEquals("0 0", cr.getGrid().getRange().getLow());
        //		assertEquals("634 477", cr.getGrid().getRange().getHigh());
        //
        //		assertNotNull(cr.getGrid().getTransform());
        //		assertEquals(30, cr.getGrid().getTransform().getScaleX().doubleValue(), 1e-6);
        //		assertEquals(-30, cr.getGrid().getTransform().getScaleY().doubleValue(), 1e-6);
        //		assertEquals(0, cr.getGrid().getTransform().getShearX().doubleValue(), 1e-6);
        //		assertEquals(0, cr.getGrid().getTransform().getShearY().doubleValue(), 1e-6);
        //		assertEquals(589995, cr.getGrid().getTransform().getTranslateX().doubleValue(), 1e-6);
        //		assertEquals(4927995, cr.getGrid().getTransform().getTranslateY().doubleValue(), 1e-6);

        assertNotNull(cr.getSupportedFormats());
        assertEquals(
                Arrays.asList("ARCGRID", "IMAGEMOSAIC", "GEOTIFF", "GIF", "PNG", "JPEG", "TIFF"),
                cr.getSupportedFormats());

        assertNotNull(cr.getInterpolationMethods());
        assertEquals(
                Arrays.asList("nearest neighbor", "bilinear", "bicubic"),
                cr.getInterpolationMethods());
        assertEquals("nearest neighbor", cr.getDefaultInterpolationMethod());

        assertNotNull(cr.getDimensions());
        assertEquals(1, cr.getDimensions().size());

        assertEquals(
                "GridSampleDimension[-9.999999933815813E36,-9.999999933815813E36]",
                cr.getDimensions().get(0).getDescription());
        assertEquals(
                new NumberRange().min(-9.999999933815813E36).max(-9.999999933815813E36),
                cr.getDimensions().get(0).getRange());

        assertEquals(Collections.singletonList("EPSG:26713"), cr.getRequestSRS());
        assertEquals(Collections.singletonList("EPSG:26713"), cr.getResponseSRS());
    }
}
