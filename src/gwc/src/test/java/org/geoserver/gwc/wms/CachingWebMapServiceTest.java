package org.geoserver.gwc.wms;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.awt.image.BufferedImage;
import org.geoserver.gwc.GWC;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

public class CachingWebMapServiceTest extends GeoServerSystemTestSupport{

    @Before
    public void setUp() throws Exception {
        ApplicationContext context = applicationContext;
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testNonTileRequests() throws Exception {
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
        BufferedImage img = getAsImage("http://localhost:9595/geoserver/topp/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&tiled=true&STYLES&LAYERS=topp:states&exceptions=application/vnd.ogc.se_inimage&tilesOrigin=-124.73142200000001,24.955967&WIDTH=256&HEIGHT=256&SRS=EPSG:4326&BBOX=-135,45,-123.75,56.25","image/png");
        assertNotNull(img);
        
    }

}
