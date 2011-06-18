package org.geoserver.wps.gs;


import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public class CollectGeometriesTest extends WPSTestSupport {

    public void testCollect() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
            "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n" + 
            "  <ows:Identifier>gs:CollectGeometries</ows:Identifier>\n" + 
            "  <wps:DataInputs>\n" + 
            "    <wps:Input>\n" + 
            "      <ows:Identifier>features</ows:Identifier>\n" + 
            "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n" + 
            "        <wps:Body>\n" + 
            "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\" outputFormat=\"GML2\">\n" + 
            "            <wfs:Query typeName=\"" + getLayerId(MockData.BASIC_POLYGONS) +  "\"/>\n" + 
            "          </wfs:GetFeature>\n" + 
            "        </wps:Body>\n" + 
            "      </wps:Reference>\n" + 
            "    </wps:Input>\n" + 
            "  </wps:DataInputs>\n" + 
            "  <wps:ResponseForm>\n" + 
            "    <wps:RawDataOutput mimeType=\"application/wkt\">\n" + 
            "      <ows:Identifier>result</ows:Identifier>\n" + 
            "    </wps:RawDataOutput>\n" + 
            "  </wps:ResponseForm>\n" + 
            "</wps:Execute>";
        
        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        
        Geometry actual = new WKTReader().read(response.getOutputStreamContent());
        Geometry expected = new WKTReader().read("MULTIPOLYGON(((-1 0, 0 1, 1 0, 0 -1, -1 0)), " +
        		" ((-2 6, 1 6, 1 3, -2 3, -2 6)), ((-1 5, 2 5, 2 2, -1 2, -1 5)))");
        
        // equals does not work with geometry collections... go figure
        assertTrue(expected.equalsExact(actual));
    }
}
