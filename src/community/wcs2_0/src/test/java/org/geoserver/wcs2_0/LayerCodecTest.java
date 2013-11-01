package org.geoserver.wcs2_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.junit.Test;

public class LayerCodecTest extends GeoServerSystemTestSupport {

    /**
     * Only setup coverages
     */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
    }
    
    @Test
    public void testBasicKVP() throws Exception {
        {
            List<LayerInfo> list0 = NCNameResourceCodec.getLayers(getCatalog(), "pippo_topo");
            assertNull(list0);
        }

        {
            List<LayerInfo> list1 = NCNameResourceCodec.getLayers(getCatalog(), "pippo__topo");
            assertNotNull(list1);
            assertEquals(0, list1.size());
        }

        {
            List<LayerInfo> list = NCNameResourceCodec.getLayers(getCatalog(), "wcs__BlueMarble");
            assertNotNull(list);
            assertEquals(1, list.size());
        }

    }

}
