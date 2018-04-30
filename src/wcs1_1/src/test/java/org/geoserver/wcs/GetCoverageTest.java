/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.geoserver.data.test.MockData.WORLD;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

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

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.xml.PreventLocalEntityResolver;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletResponse;
import org.vfny.geoserver.wcs.WcsException;
import org.w3c.dom.Document;

public class GetCoverageTest extends AbstractGetCoverageTest {

    private static final QName MOSAIC = new QName(MockData.SF_URI, "rasterFilter",
            MockData.SF_PREFIX);

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);
    
    private static final QName SPATIO_TEMPORAL = new QName(MockData.SF_URI, "spatio-temporal", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(MOSAIC, "raster-filter-test.zip", null, getCatalog());
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addRasterLayer(SPATIO_TEMPORAL, "spatio-temporal.zip", null, null, SystemTestData.class, getCatalog());
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testSpatialSubsetOnePixel() throws Exception {
        Map<String, Object> raw = new HashMap<String, Object>(baseMap());
        final String getLayerId = getLayerId(RAIN);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/geotiff");
        // this bbox is inside, and smaller than a single pixel
        raw.put("BoundingBox", "-45,146,-42,149,urn:ogc:def:crs:EPSG:6.6:4326");

        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        Envelope envelope = coverages[0].getEnvelope();
        assertEquals(-45d, envelope.getMinimum(0), 1e-6);
        assertEquals(-42d, envelope.getMaximum(0), 1e-6);
        assertEquals(146d, envelope.getMinimum(1), 1e-6);
        assertEquals(149d, envelope.getMaximum(1), 1e-6);
    }

    @Test
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

    @Test
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

        // System.out.println(coverages[0]);

        // check the envelope
        Envelope envelope = coverages[0].getEnvelope();
        // System.out.println(envelope);
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");
        assertEquals(targetCRS, envelope.getCoordinateReferenceSystem());

        ReferencedEnvelope nativeBounds = ci.getNativeBoundingBox();
        ReferencedEnvelope expected = nativeBounds.transform(targetCRS, true);

        assertEquals(0, Double.compare(expected.getMinimum(0), envelope.getMinimum(0)));
        assertEquals(0, Double.compare(expected.getMaximum(0), envelope.getMaximum(0)));
        assertEquals(0, Double.compare(expected.getMinimum(1), envelope.getMinimum(1)));
        assertEquals(0, Double.compare(expected.getMaximum(1), envelope.getMaximum(1)));

        // check we did not get a massive raster out (GEOS-5346)
        GridEnvelope range = coverages[0].getGridGeometry().getGridRange();
        assertEquals(360, range.getSpan(0));
        assertEquals(499, range.getSpan(1));
    }
    
    @Test
    public void testRotated() throws Exception {
        final String layerId = getLayerId(MockData.ROTATED_CAD);

        // do the request
        Map<String, Object> raw = baseMap();
        raw.put("identifier", layerId);
        raw.put("format", "image/geotiff");
        raw.put("BoundingBox", "7.7634301664746515,45.14713380418506,7.764350661575157,45.14763319238466,EPSG:4326");
        GridCoverage[] coverages = executeGetCoverageKvp(raw);

        // System.out.println(coverages[0]);

        // check the envelope
        Envelope envelope = coverages[0].getEnvelope();
        // System.out.println(envelope);
        CoordinateReferenceSystem targetCRS = CRS.decode("urn:x-ogc:def:crs:EPSG:3003");
        assertEquals(targetCRS, envelope.getCoordinateReferenceSystem());

        // check we did not get a massive raster out
        GridEnvelope range = coverages[0].getGridGeometry().getGridRange();
        assertEquals(482, range.getSpan(0));
        assertEquals(447, range.getSpan(1));
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
        ServletResponse r = getAsServletResponse("wcs?identifier=" + TASMANIA_BM.getLocalPart()
                + queryString);
        assertTrue(r.getContentType().startsWith("multipart/related"));

        Document dom = getAsDOM("cdf/wcs?identifier=" + TASMANIA_BM.getLocalPart() + queryString);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testNotExistent() throws Exception {
        String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
        Document dom = getAsDOM("wcs?identifier=NotThere" + queryString);
        // print(dom);
        checkOws11Exception(dom, "InvalidParameterValue", "identifier");
    }

    
    @Test
    public void testLayerQualified() throws Exception {
        String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
        ServletResponse r = getAsServletResponse("wcs/BlueMarble/wcs?identifier=BlueMarble"
                + queryString);
        assertTrue(r.getContentType().startsWith("multipart/related"));

        Document dom = getAsDOM("wcs/DEM/wcs?identifier=BlueMarble" + queryString);
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testLargerThanData() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wcs/BlueMarble/wcs?identifier="
                + getLayerId(TASMANIA_BM)
                + "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-90,-180,90,180,urn:ogc:def:crs:EPSG:6.6:4326&GridBaseCRS=EPSG:4326");

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("image/tiff", coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);

        // save
        File tiffFile = File.createTempFile("wcs", "", new File("target"));
        IOUtils.copy(coveragePart.getInputStream(), new FileOutputStream(tiffFile));

        // make sure we can read the coverage back
        GeoTiffReader reader = new GeoTiffReader(tiffFile);
        GridCoverage2D result = reader.read(null);
        coverages.add(result);

        // see that we got the entire coverage, but nothing more
        CoverageInfo ci = getCatalog().getCoverageByName(TASMANIA_BM.getLocalPart());
        GridCoverage2D original = (GridCoverage2D) ci.getGridCoverage(null, null);
        coverages.add(original);

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

    @Test
    public void testInputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setInputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                    + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?identifier=" + getLayerId(TASMANIA_BM)
                    + queryString);
            // print(dom);
            // check it's an error, check we're getting it because of the input limits
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate(
                    "/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()", dom);
            assertTrue(error.matches(".*read too much data.*"));
        } finally {
            setInputLimit(0);
        }
    }

    @Test
    public void testTimeInputLimitsDefault() throws Exception {
        String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326&timeSequence=2010-01-01/2011-01-01/P1D";
        Document dom = getAsDOM("wcs/BlueMarble/wcs?identifier=" + getLayerId(TASMANIA_BM)
                + queryString);
        // print(dom);
        String text =
                checkOws11Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
        assertThat(text, containsString("More than 100 times"));
    }

    @Test
    public void testTimeInputLimitsCustom() throws Exception {
        GeoServer gs = getGeoServer();
        WCSInfo wcs = gs.getService(WCSInfo.class);
        wcs.setMaxRequestedDimensionValues(2);
        gs.save(wcs);
        try {
            String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                    + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326&timeSequence=2010-01-01/2011-01-01/P1D";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?identifier=" + getLayerId(TASMANIA_BM)
                    + queryString);
            // print(dom);
            String text =
                    checkOws11Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
            assertThat(text, containsString("More than 2 times"));
        } finally {
            wcs.setMaxRequestedDimensionValues(
                    DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES);
            gs.save(wcs);
        }    
    }

    @Test
    public void testOutputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setOutputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.1.1&&format=image/geotiff"
                    + "&BoundingBox=-45,146,-42,147,urn:ogc:def:crs:EPSG:6.6:4326";
            Document dom = getAsDOM("wcs/wcs?identifier=" + getLayerId(TASMANIA_BM) + queryString);
            // print(dom);
            // check it's an error, check we're getting it because of the output limits
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate(
                    "/ows:ExceptionReport/ows:Exception/ows:ExceptionText/text()", dom);
            assertTrue(error.matches(".*generate too much data.*"));
        } finally {
            setOutputLimit(0);
        }
    }

    @Test
    public void testRasterFilterGreen() throws Exception {
        String queryString = "wcs?identifier=" + getLayerId(MOSAIC) + "&request=getcoverage"
                + "&service=wcs&version=1.1.1&&format=image/tiff"
                + "&BoundingBox=0,0,1,1,urn:ogc:def:crs:EPSG:6.6:4326"
                + "&CQL_FILTER=location like 'green%25'";

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

    @Test
    public void testRasterFilterRed() throws Exception {
        String queryString = "wcs?identifier=" + getLayerId(MOSAIC) + "&request=getcoverage"
                + "&service=wcs&version=1.1.1&&format=image/tiff"
                + "&BoundingBox=0,0,1,1,urn:ogc:def:crs:EPSG:6.6:4326"
                + "&CQL_FILTER=location like 'red%25'";

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

    @Test
    public void testReadNoGridCRS() throws Exception {
        String request = //
        "  <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\" "
                + "                   xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" "
                + "                   xmlns:gml=\"http://www.opengis.net/gml\""
                + "                   xmlns:ows=\"http://www.opengis.net/ows/1.1\" >\n"
                + "   <ows:Identifier>"
                + getLayerId(MockData.TASMANIA_DEM)
                + "   </ows:Identifier>\n"
                + "            <wcs:DomainSubset>\n"
                + "              <ows:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                + "                <ows:LowerCorner>-180.0 -90.0</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>180.0 90.0</ows:UpperCorner>\n"
                + "              </ows:BoundingBox>\n" //
                + "            </wcs:DomainSubset>\n"
                + "            <wcs:Output format=\"image/tiff\"/>\n"
                + "          </wcs:GetCoverage>";

        MockHttpServletResponse response = postAsServletResponse("wcs", request);

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
    }
    
    @Test
    public void testEntityExpansion() throws Exception {
        String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE wcs:GetCoverage [<!ELEMENT wcs:GetCoverage (ows:Identifier) >\n"
                + "  <!ATTLIST wcs:GetCoverage\n"
                + "    service CDATA #FIXED \"WCS\"\n"
                + "            version CDATA #FIXED \"1.1.1\"\n"
                + "            xmlns:ows CDATA #FIXED \"http://www.opengis.net/ows/1.1\"\n"
                + "            xmlns:wcs CDATA #FIXED \"http://www.opengis.net/wcs/1.1.1\">\n"
                + "  <!ELEMENT ows:Identifier (#PCDATA) >\n"
                + "  <!ENTITY xxe SYSTEM \"FILE:///file/not/there?.XSD\" >]>\n"
                + "  <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\" "
                + "                   xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n"
                + "                   xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\">\n"
                + "   <ows:Identifier>&xxe;</ows:Identifier>\n" + "  </wcs:GetCoverage>";

        Document dom = postAsDOM("wcs", request);
        // print(dom);
        String error = xpath.evaluate("//ows:ExceptionText", dom);
        assertTrue(error.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
        
        request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE wcs:GetCoverage [<!ELEMENT wcs:GetCoverage (ows:Identifier) >\n"
                + "  <!ATTLIST wcs:GetCoverage\n"
                + "    service CDATA #FIXED \"WCS\"\n"
                + "            version CDATA #FIXED \"1.1.1\"\n"
                + "            xmlns:ows CDATA #FIXED \"http://www.opengis.net/ows/1.1\"\n"
                + "            xmlns:wcs CDATA #FIXED \"http://www.opengis.net/wcs/1.1.1\">\n"
                + "  <!ELEMENT ows:Identifier (#PCDATA) >\n"
                + "  <!ENTITY xxe SYSTEM \"jar:file:///file/not/there?.xsd\" >]>\n"
                + "  <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\" "
                + "                   xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n"
                + "                   xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\">\n"
                + "   <ows:Identifier>&xxe;</ows:Identifier>\n" + "  </wcs:GetCoverage>";

        dom = postAsDOM("wcs", request);
        // print(dom);
        error = xpath.evaluate("//ows:ExceptionText", dom);
        assertTrue(error.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }

    /**
     * This tests just ended up throwing an exception as the coverage being encoded
     * was too large due to a bug in the scales estimation
     * 
     */
    @Test
    public void testRotatedPost() throws Exception {
        String request = "<GetCoverage xmlns=\"http://www.opengis.net/wcs/1.1.1\" xmlns:gml=\"http://www.opengis.net/gml\"\n" + 
                "             xmlns:ows11=\"http://www.opengis.net/ows/1.1\"\n" + 
                "             xmlns:ows=\"http://www.opengis.net/ows/1.1\"\n" +
                "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + //
                "             xmlns:wcs=\"http://schemas.opengis.net/wcs/1.1.1\"\n" + 
                "             xmlns:xlink=\"http://www.w3.org/1999/xlink\"\n" + 
                "             service=\"WCS\"\n" + 
                "             version=\"1.1.1\"\n" + 
                "             xsi:schemaLocation=\"http://www.opengis.net/wcs/1.1.1 http://schemas.opengis.net/wcs/1.1.1/wcsAll.xsd\">\n" + 
                "   <ows11:Identifier>RotatedCad</ows11:Identifier>\n" + 
                "   <DomainSubset>\n" + 
                "      <ows11:BoundingBox crs=\"urn:ogc:def:crs:OGC:1.3:CRS84\">\n" + 
                "         <ows11:LowerCorner>7.7634301664746515 45.14713380418506</ows11:LowerCorner>\n" + 
                "         <ows11:UpperCorner>7.764350661575157 45.14763319238466</ows11:UpperCorner>\n" + 
                "      </ows11:BoundingBox>\n" + 
                "   </DomainSubset>\n" + 
                "   <Output format=\"image/tiff\"/>\n" + 
                "</GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", request);

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
        
        // check the image is suitably small (without requiring an exact size)
        assertTrue(image.getWidth() < 1000);
        assertTrue(image.getHeight() < 1000);
    }
    
    /**
     * This tests just ended up throwing an exception as the coverage being encoded
     * was too large due to a bug in the scales estimation
     * 
     */
    @Test
    public void testRotatedGet() throws Exception {
        String request = "wcs?&service=WCS&request=GetCoverage&version=1.1.1&identifier=RotatedCad&BoundingBox=7.7634071540971386,45.14712131948007,7.76437367395267,45.14764567708965,urn:ogc:def:crs:OGC:1.3:CRS84&Format=image/tiff";
        MockHttpServletResponse response = getAsServletResponse(request);

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
        
        // check the image is suitably small (without requiring an exact size)
        assertTrue(image.getWidth() < 1000);
        assertTrue(image.getHeight() < 1000);
    }

    @Test
    public void testBicubicInterpolation() throws Exception {
        this.testInterpolationMethods("cubic");
    }

    @Test
    public void testBilinearInterpolation() throws Exception {
        this.testInterpolationMethods("linear");
    }

    @Test
    public void testNearestNeighborInterpolation() throws Exception {
        this.testInterpolationMethods("nearest");
    }

    @Test
    public void testUnknownInterpolation() throws Exception {
        this.testInterpolationMethods("unknown");
    }

    @Test
    public void testEmptyInterpolation() throws Exception {
        this.testInterpolationMethods("");
    }
    
    @Test
    public void testDeferredLoading() throws Exception {
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(SPATIO_TEMPORAL);
        raw.put("identifier", getLayerId);
        raw.put("format", "image/tiff");
        raw.put("BoundingBox", "-90,-180,90,180,urn:ogc:def:crs:EPSG:6.6:4326");
        raw.put("store", "false");
        raw.put("GridBaseCRS", "urn:ogc:def:crs:EPSG:6.6:4326");

        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        assertEquals(1, coverages.length);
        assertDeferredLoading(coverages[0].getRenderedImage());
    }

    private void testInterpolationMethods(String method) throws Exception {
        String queryString = "wcs?identifier=" + getLayerId(MOSAIC) + "&request=getcoverage"
                + "&service=wcs&version=1.1.1&&format=image/tiff"
                + "&BoundingBox=0,0,1,1,urn:ogc:def:crs:EPSG:6.6:4326"
                + "&RangeSubset=contents:" + method;

        MockHttpServletResponse response = getAsServletResponse(queryString);
        try {
            this.getMultipart(response);
            assertEquals(response.getStatus(), 200);
        } catch (ClassCastException e) {
            assertEquals("application/xml", response.getContentType());
        }
    }

}
