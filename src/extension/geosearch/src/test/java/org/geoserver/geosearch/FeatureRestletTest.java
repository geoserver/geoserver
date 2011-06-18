package org.geoserver.geosearch;

import javax.xml.namespace.QName;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class FeatureRestletTest extends GeoServerTestSupport {
    public void testSingleFeature() throws Exception{
        QName typename = MockData.BASIC_POLYGONS;
        final String path = 
            "/rest/layers/" 
            + typename.getPrefix() 
            + ":" 
            + typename.getLocalPart() 
            + "/"
            + typename.getLocalPart()
            + ".1107531493630.kml";

        FeatureTypeInfo fti = getFeatureTypeInfo(typename);
        fti.getMetadata().put("indexingEnabled", true);
        getCatalog().save(fti);
        
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(200, response.getStatusCode());
    }
}
