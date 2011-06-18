package org.geoserver.wcs;

import static org.geoserver.data.test.MockData.TASMANIA_BM;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletResponse;

import junit.framework.Test;
import junit.textui.TestRunner;
import net.opengis.wcs10.GetCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.wcs.kvp.Wcs10GetCoverageRequestReader;
import org.geoserver.wcs.test.WCSTestSupport;
import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.metadata.iso.spatial.PixelTranslation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.wcs.WCSConfiguration;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

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

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCoverageTest());
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        kvpreader = (Wcs10GetCoverageRequestReader) applicationContext.getBean("wcs100GetCoverageRequestReader");
        service = (WebCoverageService100) applicationContext.getBean("wcs100ServiceTarget");
        configuration = new WCSConfiguration();
        catalog=(Catalog)applicationContext.getBean("catalog");
        xmlReader = new WcsXmlReader("GetCoverage", "1.0.0", configuration);
    }

    @Override
    protected String getLogConfiguration() {
        return "/DEFAULT_LOGGING.properties";
    }
    
    private Map<String, Object> baseMap() {
        Map<String, Object> raw = new HashMap<String, Object>();
        raw.put("service", "WCS");
        raw.put("version", "1.0.0");
        raw.put("request", "GetCoverage");
        return raw;
    }
    
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
        ((GridCoverage2D)coverages[0]).dispose(true);
    }
    
    /**
	 * Compare two grid to world transformations
	 * @param expectedTx
	 * @param tx
	 */
	private static void compareGrid2World(AffineTransform2D expectedTx,
			AffineTransform2D tx) {
		assertEquals("scalex",tx.getScaleX(), expectedTx.getScaleX(), 1E-6);
        assertEquals("scaley",tx.getScaleY(), expectedTx.getScaleY(), 1E-6);
        assertEquals("shearx",tx.getShearX(), expectedTx.getShearX(), 1E-6);
        assertEquals("sheary",tx.getShearY(), expectedTx.getShearY(), 1E-6);
        assertEquals("translatex",tx.getTranslateX(), expectedTx.getTranslateX(), 1E-6);
        assertEquals("translatey",tx.getTranslateY(), expectedTx.getTranslateY(), 1E-6);
	}

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

    public void testLayerQualified() throws Exception {
        String queryString ="&request=getcoverage&service=wcs&version=1.0.0&format=image/geotiff&bbox=146,-45,147,-42"+
            "&crs=EPSG:4326&width=150&height=150";

        ServletResponse response = getAsServletResponse( 
            "wcs/BlueMarble/wcs?sourcecoverage=BlueMarble"+queryString);
        assertTrue(response.getContentType().startsWith("image/tiff"));
        
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
        assertEquals("image/tiff;subtype=\"geotiff\"", response.getContentType());
        
        GeoTiffFormat format = new GeoTiffFormat();
        AbstractGridCoverage2DReader reader = format.getReader(getBinaryInputStream(response));
        
        assertEquals(CRS.decode("EPSG:3857"), reader.getOriginalEnvelope().getCoordinateReferenceSystem());
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

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

}
