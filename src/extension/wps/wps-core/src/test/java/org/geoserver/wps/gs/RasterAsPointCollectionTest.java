package org.geoserver.wps.gs;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.wfs.FeatureCollectionType;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.feature.FeatureCollection;
import org.geotools.wfs.v1_0.WFSConfiguration;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class RasterAsPointCollectionTest extends WPSTestSupport {

    static final double EPS = 1e-6;

    public static QName RESTRICTED = new QName(MockData.SF_URI, "restricted", MockData.SF_PREFIX);

    public static QName DEM = new QName(MockData.SF_URI, "sfdem", MockData.SF_PREFIX);

    // put everything in the SF namespace because the GML encoder always applies the "feature" prefix to
    // the output GML, so we need all tests to generate GML in the same namespace
    public static QName TASMANIA_BM_ZONES = new QName(MockData.SF_URI, "BmZones",
            MockData.SF_PREFIX);

    @Override
    protected void setUpInternal() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wps", "http://www.opengis.net/wps/1.0.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("feature", "http://cite.opengeospatial.org/gmlsf");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
        dataDirectory.addPropertiesType(RESTRICTED,
                getClass().getResource("restricted.properties"), Collections.singletonMap(
                        MockData.KEY_SRS_NUMBER, "EPSG:26713"));
        dataDirectory.addPropertiesType(TASMANIA_BM_ZONES, getClass().getResource(
                "tazdem_zones.properties"), Collections.singletonMap(MockData.KEY_SRS_NUMBER,
                "EPSG:26713"));
    }
    
    public void testStatisticsTazDem() throws Exception {
    	
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:RasterAsPointCollection</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>data</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                + "            <ows:Identifier>"
                + getLayerId(MockData.TASMANIA_DEM)
                + "</ows:Identifier>\n"
                + "            <wcs:DomainSubset>\n"
                + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                + "                <ows:LowerCorner>145 -41.05</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>145.05 -41</ows:UpperCorner>\n"
                + "              </gml:BoundingBox>\n"
                + "            </wcs:DomainSubset>\n"
                + "            <wcs:Output format=\"image/tiff\"/>\n"
                + "          </wcs:GetCoverage>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n" + "  </wps:ResponseForm>\n" + "</wps:Execute>";

        MockHttpServletResponse response = postAsServletResponse(root(), xml);

        Parser p = new Parser(new WFSConfiguration());
        FeatureCollectionType fct = (FeatureCollectionType) p.parse(new ByteArrayInputStream(
                response.getOutputStreamContent().getBytes()));
        FeatureCollection fc = (FeatureCollection) fct.getFeature().get(0);

        assertEquals(36, fc.size());
        
        // get first feature
        SimpleFeature sf = (SimpleFeature) fc.features().next();
        Geometry simplified = ((Geometry) sf.getDefaultGeometry());
        assertTrue(simplified instanceof Point);
        assertEquals(sf.getID(),"0");	
        assertEquals(sf.getAttributeCount(), 5);
        assertEquals("75",sf.getProperty("GRAY_INDEX").getValue());
        
        // the latter would work only with shapefile or if we had a target schema
//        assertEquals(Short.class,sf.getFeatureType().getDescriptor("GRAY_INDEX").getType().getBinding());
//        assertEquals((short)75,sf.getProperty("GRAY_INDEX"));
        
        
    }

    

}
