package org.geoserver.wcs;

import static org.geoserver.data.test.MockData.*;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wcs.WcsException;
import org.w3c.dom.Document;

public class GetCoverageTest extends AbstractGetCoverageTest {

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCoverageTest());
    }


    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        // add a data source that cannot restrict spatial data on its own
        dataDirectory.addCoverageFromZip(RAIN, 
                MockData.class.getResource("rain.zip"), "asc", "raster");
    }

    public void testKvpBasic() throws Exception {
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-90,-180,90,180,urn:ogc:def:crs:EPSG:6.6:4326");
        raw.put("store", "false");
        raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:4326");

        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        assertEquals(1, coverages.length);
        GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        assertEquals(CRS.decode("urn:ogc:def:crs:EPSG:6.6:4326"), coverage.getEnvelope()
                .getCoordinateReferenceSystem());
    }

    public void testAntimeridianWorld() throws Exception {
        // for the moment, just make sure we don't die and return something, see
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(WORLD);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/geotiff");
        raw.put("BoundingBox", "175,10,-175,20,urn:ogc:def:crs:OGC:1.3:CRS84");
        raw.put("store", "false");
        // raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:4326");

        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        assertEquals(1, coverages.length);
        GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        assertEquals(CRS.decode("urn:ogc:def:crs:EPSG:6.6:4326"), coverage.getEnvelope()
                .getCoordinateReferenceSystem());
    }

    public void testAntimeridianTaz() throws Exception {
        // for the moment, just make sure we don't die and return something, see
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/geotiff");
        raw.put("store", "false");

        // complete coverage from left side of request bbox
        raw.put("BoundingBox", "145,-80,-175,0,urn:ogc:def:crs:OGC:1.3:CRS84");
        executeGetCoverageKvp(raw);

        // partial coverage from left side of request bbox
        raw.put("BoundingBox", "147,-80,-175,0,urn:ogc:def:crs:OGC:1.3:CRS84");
        executeGetCoverageKvp(raw);

        // partial coverage from both left and right side
        raw.put("BoundingBox", "147.2,-80,147,0,urn:ogc:def:crs:OGC:1.3:CRS84");
        executeGetCoverageKvp(raw);

        // partial coverage from right side
        raw.put("BoundingBox", "175,-80,147,0,urn:ogc:def:crs:OGC:1.3:CRS84");
        executeGetCoverageKvp(raw);

        // full coverage from right side
        raw.put("BoundingBox", "175,-80,150,0,urn:ogc:def:crs:OGC:1.3:CRS84");
        executeGetCoverageKvp(raw);
    }

    public void testWrongFormatParams() throws Exception {
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", getLayerId);
        raw.put("format", "SuperCoolFormat");
        raw.put("BoundingBox", "-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326");
        try {
            executeGetCoverageKvp(raw);
            fail("When did we learn to encode SuperCoolFormat?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("format", e.getLocator());
        }
    }

    public void testDefaultGridOrigin() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>(baseMap());
        final String getLayerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/geotiff");
        // use a bbox larger than the source
        raw.put("BoundingBox", "-45,146,-42,149,urn:ogc:def:crs:EPSG:6.6:4326");

        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        AffineTransform2D tx = (AffineTransform2D) coverages[0].getGridGeometry().getGridToCRS();
        // take into account the "pixel is area" convention
        assertEquals(0.0, tx.getTranslateX() + tx.getScaleX() / 2, 1e-9);
        assertEquals(0.0, tx.getTranslateY() + tx.getScaleY() / 2, 1e-9);
    }
    
    public void testSpatialSubsetOnePixel() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>(baseMap());
        final String getLayerId = getLayerId(RAIN);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/geotiff");
        // this bbox is inside, and smaller than a single pixel
        raw.put("BoundingBox", "-45,146,-42,149,urn:ogc:def:crs:EPSG:6.6:4326");

        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        Envelope envelope = coverages[0].getEnvelope();
        assertEquals(-45d, envelope.getMinimum(0));
        assertEquals(-40d, envelope.getMaximum(0));
        assertEquals(146d, envelope.getMinimum(1));
        assertEquals(151d, envelope.getMaximum(1));
    }

    public void testWrongGridOrigin() throws Exception {
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(TASMANIA_BM);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/geotiff");
        raw.put("BoundingBox", "-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326");
        raw.put("GridOrigin", "12,13,14");
        try {
            executeGetCoverageKvp(raw);
            fail("We should have had a WcsException here?");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.name(), e.getCode());
            assertEquals("GridOrigin", e.getLocator());
        }
    }
    
    public void testReproject() throws Exception {
        // add the target code to the supported ones
        Catalog catalog = getCatalog();
        final String layerId = getLayerId(TASMANIA_BM);
        CoverageInfo ci = catalog.getCoverageByName(layerId);
        ci.getResponseSRS().add("EPSG:3857");
        catalog.save(ci);
        
        // do the request
        Map<String, Object> raw = baseMap();
        raw.put("identifier", layerId);
        raw.put("format", "image/geotiff");
        raw.put("BoundingBox", "-80,-180,80,180,urn:ogc:def:crs:EPSG:6.6:4326");
        raw.put("GridBaseCRS", "EPSG:3857");
        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        
        Envelope envelope = coverages[0].getEnvelope();
        System.out.println(envelope);
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");
        assertEquals(targetCRS, envelope.getCoordinateReferenceSystem());
        
        ReferencedEnvelope nativeBounds = ci.getNativeBoundingBox();
        ReferencedEnvelope expected = nativeBounds.transform(targetCRS, true);
        
        assertEquals(expected.getMinimum(0), envelope.getMinimum(0));
        assertEquals(expected.getMaximum(0), envelope.getMaximum(0));
        assertEquals(expected.getMinimum(1), envelope.getMinimum(1));
        assertEquals(expected.getMaximum(1), envelope.getMaximum(1));
    }

    public void testWorkspaceQualified() throws Exception {
        String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
        ServletResponse r = getAsServletResponse("wcs?identifier=" + TASMANIA_BM.getLocalPart()
                + queryString);
        assertTrue(r.getContentType().startsWith("multipart/related"));

        Document dom = getAsDOM("cdf/wcs?identifier=" + TASMANIA_BM.getLocalPart() + queryString);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    public void testLayerQualified() throws Exception {
        String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
        ServletResponse r = getAsServletResponse("wcs/BlueMarble/wcs?identifier=BlueMarble"
                + queryString);
        assertTrue(r.getContentType().startsWith("multipart/related"));

        Document dom = getAsDOM("wcs/DEM/wcs?identifier=BlueMarble" + queryString);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    public void testInputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setInputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                    + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?identifier=" + getLayerId(TASMANIA_BM)
                    + queryString);
            //print(dom);
            // check it's an error, check we're getting it because of the input limits
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate("/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()", dom);
            assertTrue(error.matches(".*read too much data.*"));
        } finally {
            setInputLimit(0);
        }
    }
    
    public void testOutputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setOutputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                    + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?identifier=" + getLayerId(TASMANIA_BM)
                    + queryString);
            //print(dom);
            // check it's an error, check we're getting it because of the output limits
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate("/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()", dom);
            System.out.println(error);
            assertTrue(error.matches(".*generate too much data.*"));
        } finally {
            setOutputLimit(0);
        }
    }



}
