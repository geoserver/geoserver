/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.gml2.GML;
import org.geotools.xsd.Schemas;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class FeatureTypeInfoSchemaBuilderTest extends WFSTestSupport {

    protected QName UUID_TEST = new QName(MockData.CITE_URI, "uuid", MockData.CITE_PREFIX);

    @Test
    public void testBuildGml2() throws Exception {
        FeatureTypeSchemaBuilder builder = new FeatureTypeSchemaBuilder.GML2(getGeoServer());

        FeatureTypeInfo lines = getFeatureTypeInfo(SystemTestData.LINES);
        XSDSchema schema = builder.build(new FeatureTypeInfo[] {lines}, null);

        assertNotNull(schema);
        XSDElementDeclaration element = Schemas.getElementDeclaration(schema, SystemTestData.LINES);
        assertNotNull(element);

        assertTrue(element.getType() instanceof XSDComplexTypeDefinition);

        XSDElementDeclaration id =
                Schemas.getChildElementDeclaration(
                        element, new QName(SystemTestData.CGF_URI, "id"));
        assertNotNull(id);

        XSDElementDeclaration lineStringProperty =
                Schemas.getChildElementDeclaration(
                        element, new QName(SystemTestData.CGF_URI, "lineStringProperty"));
        assertNotNull(lineStringProperty);

        XSDTypeDefinition lineStringPropertyType = lineStringProperty.getType();
        assertEquals(GML.NAMESPACE, lineStringPropertyType.getTargetNamespace());
        assertEquals(GML.LineStringPropertyType.getLocalPart(), lineStringPropertyType.getName());

        XSDTypeDefinition geometryAssociationType = lineStringPropertyType.getBaseType();
        assertNotNull(geometryAssociationType);
        assertEquals(GML.NAMESPACE, geometryAssociationType.getTargetNamespace());
        assertEquals(GML.GeometryAssociationType.getLocalPart(), geometryAssociationType.getName());
    }

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        super.setUpInternal(testData);
        testData.addVectorLayer(
                UUID_TEST, null, FeatureTypeInfoSchemaBuilderTest.class, getCatalog());
    }

    protected XpathEngine xpath;

    @Before
    public void setXPath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Test
    public void testUUID() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=wfs&version=1.1.0&request=DescribeFeatureType&typeName="
                                + getLayerId(UUID_TEST));
        assertEquals(
                "1",
                (xpath.evaluate("count(//xsd:element[@name='uuid' and @type='xsd:string'])", dom)));
    }
}
