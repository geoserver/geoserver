/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs2_0.util.NCNameResourceCodec;
import org.junit.Test;

public class LayerCodecTest extends GeoServerSystemTestSupport {

    /** Only setup coverages */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
    }

    @Test
    public void testBasicKVP() throws Exception {
        {
            List<LayerInfo> list0 = NCNameResourceCodec.getLayers(getCatalog(), "pippo_topo");
            assertNotNull(list0);
            assertEquals(0, list0.size());
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

        {
            // Setting the LocalWorkspace to WCS
            WorkspaceInfo ws = getCatalog().getWorkspaceByName("wcs");
            assertNotNull(ws);
            WorkspaceInfo oldWs = LocalWorkspace.get();
            LocalWorkspace.set(ws);
            List<LayerInfo> list = NCNameResourceCodec.getLayers(getCatalog(), "BlueMarble");
            assertNotNull(list);
            assertEquals(1, list.size());
            LocalWorkspace.set(oldWs);
        }

        {
            List<LayerInfo> list = NCNameResourceCodec.getLayers(getCatalog(), "BlueMarble");
            assertNotNull(list);
            assertEquals(1, list.size());
        }
    }
}
