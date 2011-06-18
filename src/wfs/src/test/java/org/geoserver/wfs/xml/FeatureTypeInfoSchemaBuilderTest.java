package org.geoserver.wfs.xml;

import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.gml2.GML;
import org.geotools.xml.Schemas;

public class FeatureTypeInfoSchemaBuilderTest extends WFSTestSupport {

    public void testBuildGml2() throws Exception {
        FeatureTypeSchemaBuilder builder = new FeatureTypeSchemaBuilder.GML2(
                getGeoServer());

        FeatureTypeInfo lines = getFeatureTypeInfo(MockData.LINES);
        XSDSchema schema = builder.build(new FeatureTypeInfo[] { lines }, null);

        assertNotNull(schema);
        XSDElementDeclaration element = Schemas.getElementDeclaration(schema,
                MockData.LINES);
        assertNotNull(element);

        assertTrue(element.getType() instanceof XSDComplexTypeDefinition);

        XSDElementDeclaration id = Schemas.getChildElementDeclaration(element,
                new QName(MockData.CGF_URI, "id"));
        assertNotNull(id);

        XSDElementDeclaration lineStringProperty = Schemas
                .getChildElementDeclaration(element, new QName(
                        MockData.CGF_URI,
                        "lineStringProperty"));
        assertNotNull(lineStringProperty);

        XSDTypeDefinition lineStringPropertyType = lineStringProperty.getType();
        assertEquals(GML.NAMESPACE, lineStringPropertyType.getTargetNamespace());
        assertEquals(GML.LINESTRINGPROPERTYTYPE.getLocalPart(),
                lineStringPropertyType.getName());

        XSDTypeDefinition geometryAssociationType = lineStringPropertyType
                .getBaseType();
        assertNotNull(geometryAssociationType);
        assertEquals(GML.NAMESPACE, geometryAssociationType
                .getTargetNamespace());
        assertEquals(GML.GEOMETRYASSOCIATIONTYPE.getLocalPart(),
                geometryAssociationType.getName());
    }
}
