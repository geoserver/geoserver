/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class LayerWorkspaceTest extends WMSTestSupport {

    private Catalog catalog;

    @Before
    public void setCatalog() throws Exception {
        catalog = getCatalog();
    }

    LayerInfo layer(Catalog cat, QName name) {
        return cat.getLayerByName(getLayerId(name));
    }

    @Test
    public void testGlobalCapabilities() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists("//Layer[Name='" + layer.prefixedName() + "']", doc);
    }

    @Test
    public void testGlobalDescribeLayer() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc =
                getAsDOM(
                        "/wms?service=WMS&request=describeLayer&version=1.1.1&LAYERS="
                                + layer.getName(),
                        true);
        assertXpathExists("//LayerDescription[@name='" + layer.prefixedName() + "']", doc);
    }

    @Test
    public void testWorkspaceCapabilities() throws Exception {
        Document doc = getAsDOM("/sf/wms?service=WMS&request=getCapabilities&version=1.1.1", true);
        assertXpathExists(
                "//Layer[Name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart() + "']", doc);
    }

    @Test
    public void testWorkspaceDescribeLayer() throws Exception {
        Document doc =
                getAsDOM(
                        "/sf/wms?service=WMS&request=describeLayer&version=1.1.1&LAYERS="
                                + MockData.PRIMITIVEGEOFEATURE.getLocalPart(),
                        true);
        assertXpathExists(
                "//LayerDescription[@name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart() + "']",
                doc);
    }
}
