/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AliasTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        setAliasedType(CiteTestData.FIFTEEN, "ft15", getCatalog());
    }

    private void setAliasedType(QName qName, String alias, Catalog catalog) throws IOException {
        String name = qName.getLocalPart();
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(name);
        featureType.setName(alias);
        getCatalog().save(featureType);
    }

    @Test
    public void testAliasFifteen() throws Exception {
        Document doc =
                getAsDOM("wfs?request=GetFeature&typename=cdf:ft15&version=1.0.0&service=wfs");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertTrue(doc.getElementsByTagName("gml:featureMember").getLength() > 0);
        assertTrue(doc.getElementsByTagName("cdf:ft15").getLength() > 0);
    }

    @Test
    public void testGetByFeatureId() throws Exception {
        Document doc =
                getAsDOM("wfs?request=GetFeature&typename=cdf:ft15&version=1.0.0&featureId=ft15.1");
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        assertEquals(1, doc.getElementsByTagName("gml:featureMember").getLength());
        final NodeList features = doc.getElementsByTagName("cdf:ft15");
        assertEquals(1, features.getLength());
        Node feature = features.item(0);
        final Node fidNode = feature.getAttributes().getNamedItem("fid");
        assertEquals("ft15.1", fidNode.getTextContent());
    }

    @Test
    public void testDescribeFeatureType() throws Exception {
        Document doc = getAsDOM("wfs?request=DescribeFeatureType&typename=cdf:ft15&version=1.0.0");
        print(doc);
        assertEquals("xsd:schema", doc.getDocumentElement().getNodeName());

        XMLAssert.assertXpathEvaluatesTo("ft15", "/xs:schema/xs:element/@name", doc);
    }
}
