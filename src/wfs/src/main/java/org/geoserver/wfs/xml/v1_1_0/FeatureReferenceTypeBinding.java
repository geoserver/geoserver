/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.geotools.api.feature.Association;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.Name;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://cite.opengeospatial.org/gmlsf:FeatureReferenceType.
 *
 * <p>This is a special binding for wfs 1.1 cite tests. Its is a type defined in the application schema for the test
 * suite.
 *
 * <p>
 *
 * <pre>
 *  <code>
 *  &lt;xsd:complexType name=&quot;FeatureReferenceType&quot;&gt;
 *     &lt;xsd:sequence minOccurs=&quot;0&quot;&gt;
 *       &lt;xsd:element ref=&quot;gml:_Feature&quot; /&gt;
 *     &lt;/xsd:sequence&gt;
 *    &lt;xsd:attributeGroup ref=&quot;gml:AssociationAttributeGroup&quot;/&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * </code>
 *  </pre>
 *
 * @generated
 */
public class FeatureReferenceTypeBinding extends AbstractComplexBinding {

    public static final QName FeatureReferenceType =
            new QName("http://cite.opengeospatial.org/gmlsf", "FeatureReferenceType");

    /** @generated */
    @Override
    public QName getTarget() {
        return FeatureReferenceType;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @Override
    public Class<Association> getType() {
        return Association.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {

        // TODO: implement and remove call to super
        return super.parse(instance, node, value);
    }

    @Override
    public Object getProperty(Object object, QName name) throws Exception {
        Association association = (Association) object;

        if (association.getValue() == null) {
            // non resolveed, return the xlink:href
            if (XLINK.HREF.equals(name)) {
                String id = (String) association.getUserData().get("gml:id");

                return "#" + id;
            }
        }

        return null;
    }

    public List<Object[]> getProperties(Object object) throws Exception {
        Association association = (Association) object;

        if (association.getValue() != null) {
            // associated value was resolved, return it
            Object associated = association.getValue();

            // check for feature
            if (associated instanceof SimpleFeature feature) {
                Name typeName = feature.getType().getName();
                QName name = new QName(typeName.getNamespaceURI(), typeName.getLocalPart());

                List<Object[]> properties = new ArrayList<>();

                // return a comment which is hte xlink href
                properties.add(new Object[] {Encoder.COMMENT, "#" + feature.getID()});

                // first return the feature
                properties.add(new Object[] {name, feature});

                return properties;
            }
        }

        return null;
    }
}
