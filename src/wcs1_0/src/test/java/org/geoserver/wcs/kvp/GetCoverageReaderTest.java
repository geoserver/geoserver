package org.geoserver.wcs.kvp;

import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import net.opengis.wcs10.GetCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.wcs.test.WCSTestSupport;
import org.vfny.geoserver.wcs.WcsException;

public class GetCoverageReaderTest extends WCSTestSupport {

    static Wcs10GetCoverageRequestReader reader;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCoverageReaderTest());
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        Catalog catalog = (Catalog) applicationContext.getBean("catalog");
        reader = new Wcs10GetCoverageRequestReader(catalog);
    }

    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    public void testMissingParams() throws Exception {
        Map<String, Object> raw = baseMap();

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("Hey, format is missing, this should have failed");
        } catch (WcsException e) {
            assertEquals("MissingParameterValue", e.getCode());
        }

        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
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

        raw.put("version", "1.0.0");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("crs", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
        } catch (WcsException e) {
            fail("This time all mandatory params where provided?");
            assertEquals("MissingParameterValue", e.getCode());
        }

    }

    private Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.0.0");
        raw.put("request", "GetCoverage");
        return raw;
    }

    public void testUnknownCoverageParams() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = "fairyTales:rumpelstilskin";
        raw.put("sourcecoverage", layerId);
        raw.put("format", "SuperCoolFormat");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("crs", "EPSG:4326");
        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("That coverage is not registered???");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("sourcecoverage", e.getLocator());
        }
    }

    public void testBasic() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("BBOX", "-45,146,-42,147");
        raw.put("CRS", "EPSG:4326");
        raw.put("width", "150");
        raw.put("height", "150");

        GetCoverageType getCoverage = (GetCoverageType) reader.read(reader.createRequest(),
                parseKvp(raw), raw);
        assertEquals(layerId, getCoverage.getSourceCoverage());
        assertEquals("image/tiff", getCoverage.getOutput().getFormat().getValue());
        assertEquals("EPSG:4326", getCoverage.getOutput().getCrs().getValue());
    }

    public void testUnsupportedCRS() throws Exception {
        Map<String, Object> raw = baseMap();
        final String layerId = getLayerId(TASMANIA_BM);
        raw.put("SourceCoverage", layerId);
        raw.put("version", "1.0.0");
        raw.put("format", "image/tiff");
        raw.put("CRS", "urn:ogc:def:crs:EPSG:6.6:-1000");
        raw.put("width", "150");
        raw.put("height", "150");

        try {
            reader.read(reader.createRequest(), parseKvp(raw), raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals("crs", e.getLocator());
            assertEquals("InvalidParameterValue", e.getCode());
        }
    }

}
