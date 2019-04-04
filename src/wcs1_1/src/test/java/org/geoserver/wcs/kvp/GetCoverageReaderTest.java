/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wcs11.RangeSubsetType;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class GetCoverageReaderTest extends WCSTestSupport {

    static GetCoverageRequestReader reader;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = (Catalog) applicationContext.getBean("catalog");
        reader = new GetCoverageRequestReader(catalog);
    }

    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.1.1");
        raw.put("request", "GetCoverage");

        return raw;
    }

    @Test
    public void testMissingParams() throws Exception {
        Map<String, Object> raw = baseMap();

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, format is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, format is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        raw.put("format", "image/tiff");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, boundingBox is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        raw.put("BoundingBox", "-45,146,-42,147");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
        } catch (WcsException e) {
            fail("This time all mandatory params where provided?");
            assertEquals("MissingParameterValue", e.getCode());
        }
    }

    @Test
    public void testUnknownCoverageParams() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = "fairyTales:rumpelstilskin";
        raw.put("identifier", layerId);
        raw.put("format", "SuperCoolFormat");
        raw.put("BoundingBox", "-45,146,-42,147");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("That coverage is not registered???");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("identifier", e.getLocator());
        }
    }

    @Test
    public void testBasic() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");
        raw.put("store", "false");
        raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:4326");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getIdentifier().getValue());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat());
        assertFalse(getCoverage.getOutput().isStore());
        assertEquals(
                "urn:ogc:def:crs:EPSG:6.6:4326",
                getCoverage.getOutput().getGridCRS().getGridBaseCRS());
    }

    @Test
    public void testUnsupportedCRS() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:-1000");

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals("GridBaseCRS", e.getLocator());
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

    @Test
    public void testGridTypes() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");

        raw.put("gridType", GridType.GT2dGridIn2dCrs.getXmlConstant());
        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridType.GT2dGridIn2dCrs.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridType());

        raw.put("gridType", GridType.GT2dSimpleGrid.getXmlConstant());
        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridType.GT2dSimpleGrid.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridType());

        // try with different case
        raw.put("gridType", GridType.GT2dSimpleGrid.getXmlConstant().toUpperCase());
        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridType.GT2dSimpleGrid.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridType());

        raw.put("gridType", GridType.GT2dGridIn3dCrs.getXmlConstant());
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridType", e.getLocator());
        }

        raw.put("gridType", "Hoolabaloola");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridType", e.getLocator());
        }
    }

    @Test
    public void testGridCS() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");

        raw.put("GridCS", GridCS.GCSGrid2dSquare.getXmlConstant());
        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridCS.GCSGrid2dSquare.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridCS());

        raw.put("GridCS", GridCS.GCSGrid2dSquare.getXmlConstant().toUpperCase());
        getCoverage = (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        assertEquals(
                GridCS.GCSGrid2dSquare.getXmlConstant(),
                getCoverage.getOutput().getGridCRS().getGridCS());

        raw.put("GridCS", "Hoolabaloola");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridCS", e.getLocator());
        }
    }

    @Test
    public void testGridOrigin() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");
        raw.put("GridOrigin", "10.5,-30.2");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        Double[] origin = (Double[]) getCoverage.getOutput().getGridCRS().getGridOrigin();
        assertEquals(2, origin.length);
        assertEquals(0, Double.compare(10.5, (double) origin[0]));
        assertEquals(0, Double.compare(-30.2, (double) origin[1]));

        raw.put("GridOrigin", "12");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOrigin", e.getLocator());
        }

        raw.put("GridOrigin", "12,a");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOrigin", e.getLocator());
        }
    }

    @Test
    public void testGridOffsets() throws Exception {
        Map<String, Object> raw = baseMap();

        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");

        raw.put("GridOffsets", "10.5,-30.2");
        raw.put("GridType", GridType.GT2dSimpleGrid.getXmlConstant());
        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        Double[] offsets = (Double[]) getCoverage.getOutput().getGridCRS().getGridOffsets();
        assertEquals(2, offsets.length);
        assertEquals(0, Double.compare(10.5, (double) offsets[0]));
        assertEquals(0, Double.compare(-30.2, (double) offsets[1]));

        raw.put("GridOffsets", "12");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOffsets", e.getLocator());
        }

        raw.put("GridOffsets", "12,a");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOffsets", e.getLocator());
        }
    }

    /** Tests Bicubic (also called cubic) interpolation with a RangeSubset. */
    @Test
    public void testInterpolationBicubic() throws Exception {
        this.testRangeSubset("cubic");
    }

    /** Tests Bilinear (also called linear) interpolation with a RangeSubset. */
    @Test
    public void testInterpolationBilinear() throws Exception {
        this.testRangeSubset("linear");
    }

    /** Tests Nearest neighbor (also called nearest) interpolation with a RangeSubset. */
    @Test
    public void testInterpolationNearest() throws Exception {
        this.testRangeSubset("nearest");
    }

    protected Map parseKvp(Map /* <String,String> */ raw) throws Exception {
        // parse like the dispatcher but make sure we don't change the original map
        HashMap input = new HashMap(raw);
        List<Throwable> errors = KvpUtils.parse(input);
        if (errors != null && errors.size() > 0) throw (Exception) errors.get(0);

        return caseInsensitiveKvp(input);
    }

    protected Map caseInsensitiveKvp(HashMap input) {
        // make it case insensitive like the servlet+dispatcher maps
        Map result = new HashMap();
        for (Iterator it = input.keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            result.put(key.toUpperCase(), input.get(key));
        }
        return new CaseInsensitiveMap(result);
    }

    /**
     * Tests valid range subset expressions, but with a mix of valid and invalid identifiers.
     *
     * @param interpolation The used interpolation method.
     */
    private void testRangeSubset(String interpolation) throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);

        raw.put("identifier", layerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-45,146,-42,147");
        raw.put("rangeSubset", "BlueMarble:" + interpolation + "[Bands[Red_band]]");

        GetCoverageType getCoverage =
                (GetCoverageType) reader.read(reader.createRequest(), parseKvp(raw), raw);
        RangeSubsetType rs = getCoverage.getRangeSubset();
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        List keys = axis.getKey();

        assertNotNull(rs);
        assertEquals(1, rs.getFieldSubset().size());

        assertEquals("BlueMarble", field.getIdentifier().getValue());
        assertEquals(1, field.getAxisSubset().size());

        assertEquals("Bands", axis.getIdentifier());

        assertEquals(1, keys.size());
        assertEquals("Red_band", keys.get(0));

        assertEquals(field.getInterpolationType(), interpolation);
    }
}
