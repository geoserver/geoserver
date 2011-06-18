package org.geoserver.wfs;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AliasTest extends WFSTestSupport {

    private Catalog catalog;
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new AliasTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        catalog = (Catalog) applicationContext.getBean("catalog");
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        addAliasedType(dataDirectory, MockData.FIFTEEN, "ft15");
    }

    private void addAliasedType(MockData dataDirectory, QName name, String alias)
            throws IOException {
        URL properties = MockData.class.getResource(name.getLocalPart() + ".properties");
        URL style = MockData.class.getResource(name.getLocalPart() + ".sld");
        String styleName = null;
        HashMap<String, Object> extra = new HashMap<String, Object>();
        if(style != null) {
            styleName = name.getLocalPart();
            dataDirectory.addStyle(styleName, style);
            extra.put(MockData.KEY_STYLE, styleName);
        }
        extra.put(MockData.KEY_ALIAS, alias);
        dataDirectory.addPropertiesType(name, properties, extra);
    }

    public void testAliasFifteen() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:ft15&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertTrue(doc.getElementsByTagName("gml:featureMember").getLength() > 0);
        assertTrue(doc.getElementsByTagName("cdf:ft15").getLength() > 0);
    }
    
    public void testGetByFeatureId() throws Exception {
        Document doc = getAsDOM("wfs?request=GetFeature&typename=cdf:ft15&version=1.0.0&featureId=ft15.1");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(1, doc.getElementsByTagName("gml:featureMember").getLength());
        final NodeList features = doc.getElementsByTagName("cdf:ft15");
        assertEquals(1, features.getLength());
        Node feature = features.item(0);
        final Node fidNode = feature.getAttributes().getNamedItem("fid");
        assertEquals("ft15.1", fidNode.getTextContent());
    }
    
    public void testDescribeFeatureType() throws Exception {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=cdf:ft15&version=1.0.0");
        print(doc);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("ft15", "/xs:schema/xs:element/@name", doc);
    }
    
    
}
