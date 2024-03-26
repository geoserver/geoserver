/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Test;

public class PreviewLayerTest extends GeoServerWicketTestSupport {

    /**
     * Check MapML srs is preserved for layer groups
     *
     * @throws Exception
     */
    @Test
    public void testMapmlFormatLink() throws Exception {
        Catalog cat = getCatalog();

        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("foo");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName(getLayerId(MockData.PRIMITIVEGEOFEATURE)));
        lg.setBounds(new ReferencedEnvelope(0, 1, 0, 1, CRS.decode("MapML:OSMTILE")));
        PreviewLayer layer = new PreviewLayer(lg);
        layer.getWmsLink(
                (r, p) -> {
                    assertEquals("MapML:OSMTILE", p.get("srs"));
                    assertEquals("0.0,0.0,1.0,1.0", p.get("bbox"));
                });
    }
}
