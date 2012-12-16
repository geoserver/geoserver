package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import net.opengis.wcs20.ExtensionItemType;
import net.opengis.wcs20.GetCoverageType;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class GetCoverageKvpTest extends GeoServerSystemTestSupport {
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // don't need any data
    }

    private GetCoverageType parse(String url) throws Exception {
        Map<String, Object> rawKvp = KvpUtils.parseQueryString(url);
        Map<String, Object> kvp = parseKvp(rawKvp);
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType gc = (GetCoverageType) reader.createRequest();
        return (GetCoverageType) reader.read(gc, kvp, rawKvp);
    }
    
    @Test
    public void testParseBasic() throws Exception {
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1&coverageId=theCoverage");
        
        assertEquals("theCoverage", gc.getCoverageId());
    }
    
    @Test
    public void testGeotiffExtensionJPEG() throws Exception {
        
        GetCoverageType gc = parse("wcs?request=GetCoverage&service=WCS&version=2.0.1" +
        		"&coverageId=theCoverage&compression=JPEG&jpeg_quality=75&predictor=None" +
        		"&interleave=pixel&tiling=true&tileheight=256&tilewidth=256");
        
        Map<String, Object> extensions = getExtensionsMap(gc);
        
        assertEquals("JPEG", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:compression"));
        assertEquals("75", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:jpeg_quality"));
        assertEquals("None", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:predictor"));
        assertEquals("pixel", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:interleave"));
        assertEquals("true", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tiling"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tileheight"));
        assertEquals("256", extensions.get("http://www.opengis.net/wcs/geotiff/1.0:tilewidth"));
    }
    
    private Map<String, Object> getExtensionsMap(GetCoverageType gc) {
        // collect extensions
        Map<String, Object> extensions = new HashMap<String, Object>();
        for (ExtensionItemType item : gc.getExtension().getContents()) {
            Object value = item.getSimpleContent() != null ? item.getSimpleContent() : item.getObjectContent();
            extensions.put(item.getNamespace() + ":" + item.getName(), value);
        }
        return extensions;
    }

    
    
}
