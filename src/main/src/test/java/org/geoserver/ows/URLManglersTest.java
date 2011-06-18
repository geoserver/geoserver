package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.util.Collections;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.test.GeoServerTestSupport;

public class URLManglersTest extends GeoServerTestSupport {
    
    private static final String BASEURL = "http://localhost:8080/geoserver";
    
    public void testBasic() {
        String url =  buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test", url);
    }
    
    public void testKVP() {
        String url =  buildURL(BASEURL, "test", Collections.singletonMap("param", "value()"), URLType.SERVICE);
        assertEquals("http://localhost:8080/geoserver/test?param=value%28%29", url);
    }
    
    public void testProxyBase() {
        GeoServerInfo gi = getGeoServer().getGlobal();
        gi.setProxyBaseUrl("http://geoserver.org/");
        getGeoServer().save(gi);
        
        String url =  buildURL(BASEURL, "test", null, URLType.SERVICE);
        assertEquals("http://geoserver.org/test", url);
    }
    
    
    
    
}
