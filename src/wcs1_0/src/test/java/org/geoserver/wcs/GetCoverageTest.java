/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import net.opengis.wcs10.GetCoverageType;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geoserver.wcs.kvp.Wcs10GetCoverageRequestReader;
import org.geoserver.wcs.test.WCSTestSupport;
import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.operation.MultiplyConst;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.metadata.iso.spatial.PixelTranslation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.wcs.WCSConfiguration;
import org.geotools.xml.PreventLocalEntityResolver;
import org.junit.Before;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.servlet.ServletResponse;
import javax.xml.namespace.QName;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for GetCoverage operation on WCS.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GetCoverageTest extends WCSTestSupport {

    private Wcs10GetCoverageRequestReader kvpreader;
    private WebCoverageService100 service;

    private WCSConfiguration configuration;

    private WcsXmlReader xmlReader;

    private Catalog catalog;
    
    private static final QName MOSAIC = new QName(MockData.SF_URI, "rasterFilter", MockData.SF_PREFIX);
    
    private static final QName SPATIO_TEMPORAL = new QName(MockData.SF_URI, "spatio-temporal", MockData.SF_PREFIX);

    @Before
    public void setUp() {
        kvpreader = (Wcs10GetCoverageRequestReader) applicationContext.getBean("wcs100GetCoverageRequestReader");
        service = (WebCoverageService100) applicationContext.getBean("wcs100ServiceTarget");
        configuration = new WCSConfiguration();
        catalog=(Catalog)applicationContext.getBean("catalog");
        xmlReader = new WcsXmlReader("GetCoverage", "1.0.0", configuration,
                EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(MOSAIC, "raster-filter-test.zip", null, getCatalog());
        testData.addRasterLayer(SPATIO_TEMPORAL, "spatio-temporal.zip", null, null, SystemTestData.class, getCatalog());
        testData.addRasterLayer(new QName(MockData.WCS_URI, "category", MockData.WCS_PREFIX), "category.tiff", null, getCatalog());
        // enable dimensions on the water temperature layer
        setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
    }

    private Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.0.0");
        raw.put("request", "GetCoverage");
        return raw;
    }
    
    @Test
    public void testDomainSubsetRxRy() throws Exception {
    	// get base  coverage
        final GridCoverage baseCoverage = catalog.getCoverageByName(TASMANIA_BM.getLocalPart()).getGridCoverage(null, null);
        final AffineTransform2D expectedTx = (AffineTransform2D) baseCoverage.getGridGeometry().getGridToCRS();        
        final GeneralEnvelope originalEnvelope = (GeneralEnvelope) baseCoverage.getEnvelope();
        final GeneralEnvelope newEnvelope=new GeneralEnvelope(originalEnvelope);
        newEnvelope.setEnvelope(
        		originalEnvelope.getMinimum(0),
        		originalEnvelope.getMaximum(1)-originalEnvelope.getSpan(1)/2,
        		originalEnvelope.getMinimum(0)+originalEnvelope.getSpan(0)/2,
        		originalEnvelope.getMaximum(1)
        		);
        
        final MathTransform cornerWorldToGrid = PixelTranslation.translate(expectedTx,PixelInCell.CELL_CENTER,PixelInCell.CELL_CORNER);
        final GeneralGridEnvelope expectedGridEnvelope = new GeneralGridEnvelope(CRS.transform(cornerWorldToGrid.inverse(), newEnvelope),PixelInCell.CELL_CORNER,false);
        final StringBuilder envelopeBuilder= new StringBuilder();
        envelopeBuilder.append(newEnvelope.getMinimum(0)).append(",");
        envelopeBuilder.append(newEnvelope.getMinimum(1)).append(",");
        envelopeBuilder.append(newEnvelope.getMaximum(0)).append(",");
        envelopeBuilder.append(newEnvelope.getMaximum(1));
        
        Map<String, Object> raw = baseMap();
        final String layerID = getLayerId(TASMANIA_BM);
        raw.put("sourcecoverage", layerID);
        raw.put("version", "1.0.0");
        raw.put("format", "image/geotiff"); 
        raw.put("BBox", envelopeBuilder.toString());
        raw.put("crs", "EPSG:4326");
        raw.put("resx", Double.toString(expectedTx.getScaleX()));
        raw.put("resy", Double.toString(Math.abs(expectedTx.getScaleY())));

        final GridCoverage[] coverages = executeGetCoverageKvp(raw);
        final GridCoverage2D result=(GridCoverage2D) coverages[0];
        assertTrue(coverages.length==1);
        final AffineTransform2D tx = (AffineTransform2D) result.getGridGeometry().getGridToCRS();
        assertEquals("resx",expectedTx.getScaleX(),tx.getScaleX(),1E-6);
        assertEquals("resx",Math.abs(expectedTx.getScaleY()),Math.abs(tx.getScaleY()),1E-6);
        
        final GridEnvelope gridEnvelope = result.getGridGeometry().getGridRange();
        assertEquals("w",180,gridEnvelope.getSpan(0));
        assertEquals("h",180,gridEnvelope.getSpan(1));
        assertEquals("grid envelope",expectedGridEnvelope, gridEnvelope);
        
        // dispose
        CoverageCleanerCallback.disposeCoverage(baseCoverage);
        CoverageCleanerCallback.disposeCoverage(coverages[0]);
    }
    
    @Test
    public void testDeferredLoading() throws Exception {
        Map<String, Object> raw = baseMap();
        final String getLayerId = getLayerId(SPATIO_TEMPORAL);
        raw.put("sourcecoverage", getLayerId);
        raw.put("format", "image/tiff");
        raw.put("BBox", "-90,-180,90,180");
        raw.put("crs", "EPSG:4326");
        raw.put("resx", "0.001");
        raw.put("resy", "0.001");


        GridCoverage[] coverages = executeGetCoverageKvp(raw);
        assertEquals(1, coverages.length);
        assertDeferredLoading(coverages[0].getRenderedImage());
    }
    
    @Test
    public void testWorkspaceQualified() throws Exception {
        String queryString ="&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"+
            "&crs=EPSG:4326&width=150&height=150";

        ServletResponse response = getAsServletResponse( 
            "wcs?sourcecoverage="+TASMANIA_BM.getLocalPart()+queryString);
        assertTrue(response.getContentType().startsWith("image/tiff"));
        
        Document dom = getAsDOM( 
            "cdf/wcs?sourcecoverage="+TASMANIA_BM.getLocalPart()+queryString);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    @Test
    public void testGEOS6540_1() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&format=ArcGrid&crs=EPSG:4326" +
                "&bbox=0,0,1,1&width=50&height=60";

        MockHttpServletResponse response = getAsServletResponse(queryString);
        assertEquals("text/plain", response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.startsWith("NCOLS 50\nNROWS 60\n"));
        assertEquals("inline; filename=sf:rasterFilter.asc", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testGEOS6540_2() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&format=ARCGRID&crs=EPSG:4326" +
                "&bbox=0,0,1,1&width=50&height=60";

        MockHttpServletResponse response = getAsServletResponse(queryString);
        String content = response.getContentAsString();
        assertEquals("text/plain", response.getContentType());
        assertTrue(content.startsWith("NCOLS 50\nNROWS 60\n"));
        assertEquals("inline; filename=sf:rasterFilter.asc", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testGEOS6540_3() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&format=ARCGRID-GZIP&crs=EPSG:4326" +
                "&bbox=0,0,1,1&width=50&height=60";

        MockHttpServletResponse response = getAsServletResponse(queryString);
        byte[] content = response.getContentAsByteArray();
        assertEquals("application/x-gzip", response.getContentType());
        assertEquals((byte)0x1f, content[0]);
        assertEquals((byte)0x8b, content[1]);
        assertEquals((byte)0x08, content[2]);
        assertEquals((byte)0x00, content[3]);
        assertEquals("inline; filename=sf:rasterFilter.asc.gz", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testGEOS6540_4() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&format=application/x-gzip&crs=EPSG:4326" +
                "&bbox=0,0,1,1&width=50&height=60";

        MockHttpServletResponse response = getAsServletResponse(queryString);
        byte[] content = response.getContentAsByteArray();
        assertEquals("application/x-gzip", response.getContentType());
        assertEquals((byte)0x1f, content[0]);
        assertEquals((byte)0x8b, content[1]);
        assertEquals((byte)0x08, content[2]);
        assertEquals((byte)0x00, content[3]);
        assertEquals("inline; filename=sf:rasterFilter.asc.gz", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testNonExistentCoverage() throws Exception {
        String queryString ="&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"+
            "&crs=EPSG:4326&width=150&height=150";

        Document dom = getAsDOM( 
            "wcs?sourcecoverage=NotThere" + queryString);
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("InvalidParameterValue", "/ServiceExceptionReport/ServiceException/@code", dom);
        XMLAssert.assertXpathEvaluatesTo("sourcecoverage", "/ServiceExceptionReport/ServiceException/@locator", dom);
    }

    @Test
    public void testRequestDisabledResource() throws Exception {
        Catalog catalog = getCatalog();
        ResourceInfo tazbm = catalog.getResourceByName(getLayerId(MockData.TASMANIA_BM),
                ResourceInfo.class);
        try {

            tazbm.setEnabled(false);
            catalog.save(tazbm);

            String queryString = "&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"
                    + "&crs=EPSG:4326&width=150&height=150";

            Document dom = getAsDOM("wcs?sourcecoverage=" + TASMANIA_BM.getLocalPart()
                    + queryString);
            // print(dom);
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        } finally {
            tazbm.setEnabled(true);
            catalog.save(tazbm);
        }
    }


    @Test
    public void testLayerQualified() throws Exception {
        String queryString ="&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"+
            "&crs=EPSG:4326&width=150&height=150";

        MockHttpServletResponse response = getAsServletResponse( 
            "wcs/BlueMarble/wcs?sourcecoverage=BlueMarble"+queryString);
        assertTrue(response.getContentType().startsWith("image/tiff"));
        String disposition = response.getHeader("Content-Disposition");
        assertTrue(disposition.endsWith("BlueMarble.tif"));
        
        Document dom = getAsDOM( 
            "wcs/DEM/wcs?sourcecoverage=BlueMarble"+queryString);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
    }
    
    /**
     * Runs GetCoverage on the specified parameters and returns an array of coverages
     */
    GridCoverage[] executeGetCoverageKvp(Map<String, Object> raw) throws Exception {
        final GetCoverageType getCoverage = (GetCoverageType) kvpreader.read(kvpreader.createRequest(),parseKvp(raw), raw);
        return service.getCoverage(getCoverage);
    }

    /**
     * Runs GetCoverage on the specified parameters and returns an array of coverages
     */
    GridCoverage[] executeGetCoverageXml(String request) throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) xmlReader.read(null, new StringReader(
                request), null);
        return service.getCoverage(getCoverage);
    }
    
    @Test
    public void testInputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setInputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"
                    + "&crs=EPSG:4326&width=150&height=150";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?sourcecoverage=" + getLayerId(TASMANIA_BM)
                    + queryString);
            // print(dom);
            // check it's an error, check we're getting it because of the input limits
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate(
                    "/ServiceExceptionReport/ServiceException/text()", dom).trim();
            assertTrue(error.matches(".*read too much data.*"));
        } finally {
            setInputLimit(0);
        }
    }

    @Test
    public void testOutputLimits() throws Exception {
        try {
            // ridicolous limit, just one byte
            setOutputLimit(1);
            String queryString = "&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"
                    + "&crs=EPSG:4326&width=150&height=150";
            Document dom = getAsDOM("wcs/BlueMarble/wcs?sourcecoverage=" + getLayerId(TASMANIA_BM)
                    + queryString);
            // print(dom);
            // check it's an error, check we're getting it because of the output limits
            assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
            String error = xpath.evaluate(
                    "/ServiceExceptionReport/ServiceException/text()", dom).trim();
            assertTrue(error.matches(".*generate too much data.*"));
        } finally {
            setOutputLimit(0);
        }
    }
    
    @Test
    public void testReproject() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        		"<GetCoverage version=\"1.0.0\" service=\"WCS\" " +
        		"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
        		"xmlns=\"http://www.opengis.net/wcs\" " +
        		"xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
        		"xmlns:gml=\"http://www.opengis.net/gml\" " +
        		"xmlns:ogc=\"http://www.opengis.net/ogc\" " +
        		"xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
        		"  <sourceCoverage>" +  getLayerId(TASMANIA_BM) + "</sourceCoverage>\n" + 
        		"  <domainSubset>\n" + 
        		"    <spatialSubset>\n" + 
        		"      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
        		"        <gml:pos>146 -45</gml:pos>\n" + 
        		"        <gml:pos>147 42</gml:pos>\n" + 
        		"      </gml:Envelope>\n" + 
        		"      <gml:Grid dimension=\"2\">\n" + 
        		"        <gml:limits>\n" + 
        		"          <gml:GridEnvelope>\n" + 
        		"            <gml:low>0 0</gml:low>\n" + 
        		"            <gml:high>150 150</gml:high>\n" + 
        		"          </gml:GridEnvelope>\n" + 
        		"        </gml:limits>\n" + 
        		"        <gml:axisName>x</gml:axisName>\n" + 
        		"        <gml:axisName>y</gml:axisName>\n" + 
        		"      </gml:Grid>\n" + 
        		"    </spatialSubset>\n" + 
        		"  </domainSubset>\n" + 
        		"  <output>\n" + 
        		"    <crs>EPSG:3857</crs>\n" + 
        		"    <format>image/geotiff</format>\n" + 
        		"  </output>\n" + 
        		"</GetCoverage>";
        
        MockHttpServletResponse response = postAsServletResponse("wcs", xml);
        assertEquals("image/tiff", response.getContentType());
        
        GeoTiffFormat format = new GeoTiffFormat();
        GridCoverage2DReader reader = format.getReader(getBinaryInputStream(response));
        
        assertEquals(CRS.decode("EPSG:3857"), reader.getOriginalEnvelope().getCoordinateReferenceSystem());
    }
    
    @Test
    public void testEntityExpansion() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE GetCoverage [<!ELEMENT GetCoverage (sourceCoverage) >\n"
                + "  <!ATTLIST GetCoverage\n" 
                + "            service CDATA #FIXED \"WCS\"\n" 
                + "            version CDATA #FIXED \"1.0.0\"\n" 
                + "            xmlns CDATA #FIXED \"http://www.opengis.net/wcs\">\n"
                + "  <!ELEMENT sourceCoverage (#PCDATA) >\n"
                + "  <!ENTITY xxe SYSTEM \"FILE:///file/not/there?.XSD\" >]>\n"
                + "<GetCoverage version=\"1.0.0\" service=\"WCS\""
                + " xmlns=\"http://www.opengis.net/wcs\" >\n"
                + "  <sourceCoverage>&xxe;</sourceCoverage>\n" 
                + "</GetCoverage>";

        Document dom = postAsDOM("wcs", xml);
        String error = xpath.evaluate("//ServiceException", dom);
        assertTrue(error.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
        
        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE GetCoverage [<!ELEMENT GetCoverage (sourceCoverage) >\n"
                + "  <!ATTLIST GetCoverage\n" 
                + "            service CDATA #FIXED \"WCS\"\n" 
                + "            version CDATA #FIXED \"1.0.0\"\n" 
                + "            xmlns CDATA #FIXED \"http://www.opengis.net/wcs\">\n"
                + "  <!ELEMENT sourceCoverage (#PCDATA) >\n"
                + "  <!ENTITY xxe SYSTEM \"jar:file:///file/not/there?.XSD\" >]>\n"
                + "<GetCoverage version=\"1.0.0\" service=\"WCS\""
                + " xmlns=\"http://www.opengis.net/wcs\" >\n"
                + "  <sourceCoverage>&xxe;</sourceCoverage>\n" 
                + "</GetCoverage>";

        dom = postAsDOM("wcs", xml);
        error = xpath.evaluate("//ServiceException", dom);
        assertTrue(error.contains(PreventLocalEntityResolver.ERROR_MESSAGE_BASE));
    }

    @Test
    public void testRasterFilterGreen() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&&format=image/tiff&crs=EPSG:4326" + 
                "&bbox=0,0,1,1&CQL_FILTER=location like 'green%25'&width=150&height=150";
        
        MockHttpServletResponse response = getAsServletResponse(queryString);

        // make sure we can read the coverage back
        RenderedImage image = readTiff(response);
        
        // check the pixel
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(0, pixel[0]);
        assertEquals(255, pixel[1]);
        assertEquals(0, pixel[2]);
    }

    /**
     * Parses teh TIFF contained in the response as a {@link RenderedImage}
     * @param response
     *
     * @throws IOException
     */
    RenderedImage readTiff(MockHttpServletResponse response) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByFormatName("tiff").next();
        reader.setInput(ImageIO.createImageInputStream(getBinaryInputStream(response)));
        return reader.read(0);
    }
    
    @Test
    public void testTimeFirstPOST() throws Exception {
        String request = getWaterTempTimeRequest("2008-10-31T00:00:00.000Z");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        /*
        gdallocationinfo NCOM_wattemp_000_20081031T0000000_12.tiff 10 10
       Report:
         Location: (10P,10L)
         Band 1:
           Value: 18.2659999176394
       */
        
        checkPixelValue(response, 10, 10, 18.2659999176394);
    }
    
    @Test
    public void testTimeFirstKVP() throws Exception {
        String queryString ="request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff" +
        		"&bbox=0.237,40.562,14.593,44.558&crs=EPSG:4326&width=25&height=25&time=2008-10-31T00:00:00.000Z" +
        		"&coverage=" + getLayerId(WATTEMP);
        MockHttpServletResponse response = getAsServletResponse("wcs?" + queryString);
        
        checkPixelValue(response, 10, 10, 18.2659999176394);
    }

    @Test
    public void testTimeTooMany() throws Exception {
        GeoServer gs = getGeoServer();
        WCSInfo wcs = gs.getService(WCSInfo.class);
        wcs.setMaxRequestedDimensionValues(2);
        gs.save(wcs);
        try {
            String queryString =
                    "request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff"
                            + "&bbox=0.237,40.562,14.593,44.558&crs=EPSG:4326&width=25&height=25&time=2008-10-31/2008-11-31/PT1H"
                            + "&coverage="
                            + getLayerId(WATTEMP);
            MockHttpServletResponse response = getAsServletResponse("wcs?" + queryString);
            assertEquals("application/vnd.ogc.se_xml", response.getContentType());
            Document dom = dom(response, true);
            // print(dom);
            String text =
                    checkLegacyException(dom, ServiceException.INVALID_PARAMETER_VALUE, "time");
            assertThat(text, containsString("More than 2 times"));
        } finally {
            wcs.setMaxRequestedDimensionValues(
                    DimensionInfo.DEFAULT_MAX_REQUESTED_DIMENSION_VALUES);
            gs.save(wcs);
        }
    }

    @Test
    public void testTimeRangeKVP() throws Exception {
        setupRasterDimension(TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(TIMERANGES, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
        
        String baseUrl ="wcs?request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff" +
                "&bbox=0.237,40.562,14.593,44.558&crs=EPSG:4326&width=25&height=25" +
                "&coverage=" + getLayerId(TIMERANGES);

        // last range
        MockHttpServletResponse response = getAsServletResponse(baseUrl + "&TIME=2008-11-05T00:00:00.000Z/2008-11-06T12:00:00.000Z");
        assertEquals("image/tiff", response.getContentType());
        checkPixelValue(response, 10, 10, 13.337999683572);
        

        // middle hole, no data --> we should get back an exception
        Document dom = getAsDOM(baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z");
        // print(dom);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport)", dom);
        
        // first range
        response = getAsServletResponse(baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z");
        assertEquals("image/tiff", response.getContentType());
        checkPixelValue(response, 10, 10, 18.2659999176394);
    }

    private void checkPixelValue(MockHttpServletResponse response, int x, int y, double value) throws IOException,
            FileNotFoundException, DataSourceException {
        // save
        File tiffFile = File.createTempFile("wcs", "", new File("target"));
        try {
            IOUtils.copy(getBinaryInputStream(response), new FileOutputStream(tiffFile));
    
            // make sure we can read the coverage back
            GeoTiffReader reader = new GeoTiffReader(tiffFile);
            GridCoverage2D result = reader.read(null);
    
            
            
            // check a pixel
            double[] pixel = new double[1];
            result.getRenderedImage().getData().getPixel(x, y, pixel);
            assertEquals(value, pixel[0], 1e-6);
        } finally {
            tiffFile.delete();
        }
    }
    
    @Test
    public void testTimeSecond() throws Exception {
        String request = getWaterTempTimeRequest("2008-11-01T00:00:00.000Z");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        checkPixelValue(response, 10, 10, 18.2849999185419);
    }
    
    @Test
    public void testTimeKVPNow() throws Exception {
        String queryString ="request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff" +
                        "&bbox=0.237,40.562,14.593,44.558&crs=EPSG:4326&width=25&height=25&time=now" +
                        "&coverage=" + getLayerId(WATTEMP);
        MockHttpServletResponse response = getAsServletResponse("wcs?" + queryString);
        
        checkPixelValue(response, 10, 10, 18.2849999185419);
    }
    
    @Test
    public void testElevationFirst() throws Exception {
        String request = getWaterTempElevationRequest("0.0");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        assertEquals("image/tiff", response.getContentType());
        
        // same result as time first
        checkPixelValue(response, 10, 10, 18.2849999185419);
    }
    
    @Test
    public void testElevationSecond() throws Exception {
        String request = getWaterTempElevationRequest("100.0");
     
        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        assertEquals("image/tiff", response.getContentType());
        
        /*
        gdallocationinfo NCOM_wattemp_100_20081101T0000000_12.tiff  10 10
        Report:
         Location: (10P,10L)
         Band 1:
         Value: 13.337999683572
       */
        
        checkPixelValue(response, 10, 10, 13.337999683572);
    }

    private String getWaterTempElevationRequest(String elevation) {
        String request =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    		"<GetCoverage version=\"1.0.0\" service=\"WCS\"\n" + 
    		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wcs\"\n" + 
    		"  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\"\n" + 
    		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
    		"  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
    		"  <sourceCoverage>" + getLayerId(WATTEMP) + "</sourceCoverage>\n" + 
    		"  <domainSubset>\n" + 
    		"    <spatialSubset>\n" + 
    		"      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
    		"        <gml:pos>0.237 40.562</gml:pos>\n" + 
    		"        <gml:pos>14.593 44.558</gml:pos>\n" + 
    		"      </gml:Envelope>\n" + 
    		"      <gml:Grid dimension=\"2\">\n" + 
    		"        <gml:limits>\n" + 
    		"          <gml:GridEnvelope>\n" + 
    		"            <gml:low>0 0</gml:low>\n" + 
    		"            <gml:high>25 24</gml:high>\n" + 
    		"          </gml:GridEnvelope>\n" + 
    		"        </gml:limits>\n" + 
    		"        <gml:axisName>x</gml:axisName>\n" + 
    		"        <gml:axisName>y</gml:axisName>\n" + 
    		"      </gml:Grid>\n" + 
    		"    </spatialSubset>\n" + 
    		"  </domainSubset>\n" + 
    		"  <rangeSubset>\n" + 
    		"    <axisSubset name=\"ELEVATION\">\n" + 
    		"      <singleValue>" + elevation + "</singleValue>\n" + 
    		"    </axisSubset>\n" + 
    		"  </rangeSubset>\n" + 
    		"  <output>\n" + 
    		"    <crs>EPSG:4326</crs>\n" + 
    		"    <format>GEOTIFF</format>\n" + 
    		"  </output>\n" + 
    		"</GetCoverage>";
        return request;
    }

    private String getWaterTempTimeRequest(String date) {
        String request =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
		"<GetCoverage version=\"1.0.0\" service=\"WCS\"\n" + 
		"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wcs\"\n" + 
		"  xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\"\n" + 
		"  xmlns:ogc=\"http://www.opengis.net/ogc\"\n" + 
		"  xsi:schemaLocation=\"http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/getCoverage.xsd\">\n" + 
		"  <sourceCoverage>" + getLayerId(WATTEMP) + "</sourceCoverage>\n" + 
		"  <domainSubset>\n" + 
		"    <spatialSubset>\n" + 
		"      <gml:Envelope srsName=\"EPSG:4326\">\n" + 
		"        <gml:pos>0.237 40.562</gml:pos>\n" + 
		"        <gml:pos>14.593 44.558</gml:pos>\n" + 
		"      </gml:Envelope>\n" + 
		"      <gml:Grid dimension=\"2\">\n" + 
		"        <gml:limits>\n" + 
		"          <gml:GridEnvelope>\n" + 
		"            <gml:low>0 0</gml:low>\n" + 
		"            <gml:high>25 25</gml:high>\n" + 
		"          </gml:GridEnvelope>\n" + 
		"        </gml:limits>\n" + 
		"        <gml:axisName>x</gml:axisName>\n" + 
		"        <gml:axisName>y</gml:axisName>\n" + 
		"      </gml:Grid>\n" + 
		"    </spatialSubset>\n" + 
		"    <temporalSubset>\n" + 
		"      <gml:timePosition>" + date + "</gml:timePosition>\n" + 
		"    </temporalSubset>\n" + 
		"  </domainSubset>\n" + 
		"  <output>\n" + 
		"    <crs>EPSG:4326</crs>\n" + 
		"    <format>GEOTIFF</format>\n" + 
		"  </output>\n" + 
		"</GetCoverage>";
        return request;
    }
    
    @Test
    public void testRasterFilterRed() throws Exception {
        String queryString = "wcs?sourcecoverage=" + getLayerId(MOSAIC) + "&request=getcoverage" +
                "&service=wcs&version=1.0.0&format=image/tiff&crs=EPSG:4326" + 
                "&bbox=0,0,1,1&CQL_FILTER=location like 'red%25'&width=150&height=150";
        
        MockHttpServletResponse response = getAsServletResponse(queryString);

        RenderedImage image = readTiff(response);
        
        // check the pixel
        int[] pixel = new int[3];
        image.getData().getPixel(0, 0, pixel);
        assertEquals(255, pixel[0]);
        assertEquals(0, pixel[1]);
        assertEquals(0, pixel[2]);
    }

    @Test
    public void testCategoriesToArray() throws Exception {
        CoverageInfo myCoverage = getCatalog().getCoverageByName("category");
        GridCoverage gridCoverage = myCoverage.getGridCoverage(null, null);
        MultiplyConst op = new MultiplyConst();
        final ParameterValueGroup param = op.getParameters();

        param.parameter("Source").setValue(gridCoverage);
        param.parameter("constants").setValue(new double[] { 0.1 });
        boolean exception = false;
        try {
            op.doOperation(param, null);
        } catch (Exception e) {
            exception = true;
        }
        assertFalse(exception);
    }

    private void setInputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxInputMemory(kbytes);
        gs.save(info);
    } 
    

    private void setOutputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxOutputMemory(kbytes);
        gs.save(info);
    } 

}
