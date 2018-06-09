/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CatalogProxiesTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpVectorLayer(SystemTestData.BUILDINGS);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        LayerInfo li = getCatalog().getLayerByName(getLayerId(SystemTestData.BUILDINGS));
        Resource resource = getDataDirectory().config(li);
        Document dom;
        try (InputStream is = resource.in()) {
            dom = dom(resource.in());
        }
        Element defaultStyle = (Element) dom.getElementsByTagName("defaultStyle").item(0);
        Element defaultStyleId = (Element) defaultStyle.getElementsByTagName("id").item(0);
        defaultStyleId.setTextContent("danglingReference");
        try (OutputStream os = resource.out()) {
            print(dom, os);
        }
        getGeoServer().reload();
    }

    @Test
    public void testDanglingReferenceOnModificationProxy() {
        LayerInfo li = getCatalog().getLayerByName(getLayerId(SystemTestData.BUILDINGS));
        assertNull(li.getDefaultStyle());
    }

    @Test
    public void testDanglingReferenceEqualsHashcode() {
        LayerInfo li = getCatalog().getLayerByName(getLayerId(SystemTestData.BUILDINGS));
        // this would have failed with an exception, also check for stable hash code
        assertEquals(li.hashCode(), li.hashCode());
        // despite the dangling reference, the layer is equal to itself
        assertTrue(li.equals(li));
    }
}
