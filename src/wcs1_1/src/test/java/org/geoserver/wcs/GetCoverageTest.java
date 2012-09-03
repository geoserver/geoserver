package org.geoserver.wcs;

import static org.geoserver.data.test.MockData.*;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.*;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;

import junit.framework.Test;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.wcs.WcsException;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GetCoverageTest extends AbstractGetCoverageTest {

    private static final QName MOSAIC = new QName(MockData.SF_URI, "rasterFilter", MockData.SF_PREFIX);
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
        
        // this also adds the raster style
        dataDirectory.addCoverage(MOSAIC, 
                MockData.class.getResource("raster-filter-test.zip"), null, "raster");
        
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
    
    public void testLargerThanData() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wcs/BlueMarble/wcs?identifier=" + getLayerId(TASMANIA_BM)
                + "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff" 
                + "&BoundingBox=-90,-180,90,180,urn:ogc:def:crs:EPSG:6.6:4326&GridBaseCRS=EPSG:4326");
        
        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("image/tiff;subtype=\"geotiff\"", coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);
        
        // save
        File tiffFile = File.createTempFile("wcs", "", new File("target"));
        IOUtils.copy(coveragePart.getInputStream(), new FileOutputStream(tiffFile));

        // make sure we can read the coverage back
        GeoTiffReader reader = new GeoTiffReader(tiffFile);
        GridCoverage2D result = reader.read(null);
        
        // see that we got the entire coverage, but nothing more
        CoverageInfo ci = getCatalog().getCoverageByName(TASMANIA_BM.getLocalPart());
        GridCoverage2D original = (GridCoverage2D) ci.getGridCoverage(null, null);
        
        // the grid should not be swapped since the target output is expressed in EPSG:XYWZ form
        GridEnvelope originalRange = original.getGridGeometry().getGridRange();
        GridEnvelope actualRange = result.getGridGeometry().getGridRange();
        assertEquals(originalRange.getSpan(0), actualRange.getSpan(0));
        assertEquals(originalRange.getSpan(1), actualRange.getSpan(1));
        
        // check also the geographic bounds
        Envelope2D originalEnv = original.getEnvelope2D();
        Envelope2D actualEnv = result.getEnvelope2D();
        assertEquals(originalEnv.getMinX(), actualEnv.getMinX(), 1e-6);
        assertEquals(originalEnv.getMinY(), actualEnv.getMinY(), 1e-6);
        assertEquals(originalEnv.getMaxX(), actualEnv.getMaxX(), 1e-6);
        assertEquals(originalEnv.getMaxY(), actualEnv.getMaxY(), 1e-6);
        
        // cleanup
        tiffFile.delete();
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
            Document dom = getAsDOM("wcs/wcs?identifier=" + getLayerId(TASMANIA_BM)
                    + queryString);
            //print(dom);
            // check it's an error, check we're getting it because of the output limits
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate("/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()", dom);
            assertTrue(error.matches(".*generate too much data.*"));
        } finally {
            setOutputLimit(0);
        }
    }


    public void testRasterFilterGreen() throws Exception {
        String queryString = "wcs?identifier=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.1.1&&format=image/tiff" + 
                "&BoundingBox=0,0,1,1,urn:ogc:def:crs:EPSG:6.6:4326" + 
                "&CQL_FILTER=location like 'green%25'";
        
        MockHttpServletResponse response = getAsServletResponse(queryString);

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("image/tiff", coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);

        // make sure we can read the coverage back
        ImageReader reader = ImageIO.getImageReadersByFormatName("tiff").next();
        reader.setInput(ImageIO.createImageInputStream(coveragePart.getInputStream()));
        RenderedImage image = reader.read(0);
        
        // check the pixel
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
    }
    
    public void testRasterFilterRed() throws Exception {
        String queryString = "wcs?identifier=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.1.1&&format=image/tiff" + 
                "&BoundingBox=0,0,1,1,urn:ogc:def:crs:EPSG:6.6:4326" + 
                "&CQL_FILTER=location like 'red%25'";
        
        MockHttpServletResponse response = getAsServletResponse(queryString);

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("image/tiff", coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);

        // make sure we can read the coverage back
        ImageReader reader = ImageIO.getImageReadersByFormatName("tiff").next();
        reader.setInput(ImageIO.createImageInputStream(coveragePart.getInputStream()));
        RenderedImage image = reader.read(0);
        
        // check the pixel
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(255, pixel[0]);
        assertEquals(0, pixel[1]);
        assertEquals(0, pixel[2]);
    }

}
