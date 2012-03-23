package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import javax.xml.namespace.QName;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.w3c.dom.Document;

public class LayerWorkspaceTest extends WMSTestSupport{

    private Catalog catalog;

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        catalog = getCatalog();
    }

    LayerInfo layer(Catalog cat, QName name) {
        return cat.getLayerByName(getLayerId(name));
    }

    protected void registerNamespaces(java.util.Map<String,String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
    };

    public void testGlobalCapabilities() throws Exception {
        LayerInfo layer = layer(catalog, MockData.PRIMITIVEGEOFEATURE);
        Document doc = getAsDOM("/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathExists("//wms:Layer[wms:Name='" + layer.prefixedName() + "']", doc);
    }

    public void testWorkspaceCapabilities() throws Exception {
        Document doc = getAsDOM("/sf/wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathExists("//wms:Layer[wms:Name='" + MockData.PRIMITIVEGEOFEATURE.getLocalPart()+ "']", doc);
    }
}
