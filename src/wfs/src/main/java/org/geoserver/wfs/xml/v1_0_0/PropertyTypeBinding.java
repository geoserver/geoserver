/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xs.bindings.XSQNameBinding;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:PropertyType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="PropertyType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element name="Name" type="xsd:string"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The Name element contains the name of a feature property
 *                    to be updated.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element minOccurs="0" name="Value"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The Value element contains the replacement value for the
 *                    named property.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class PropertyTypeBinding extends AbstractComplexBinding {
    /** The wfs factory */
    WfsFactory wfsfactory;

    /** Namespace support */
    NamespaceContext namespaceContext;

    public PropertyTypeBinding(WfsFactory wfsfactory, NamespaceContext namespaceContext) {
        this.wfsfactory = wfsfactory;
        this.namespaceContext = namespaceContext;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.PROPERTYTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return PropertyType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        PropertyType property = wfsfactory.createPropertyType();

        // &lt;xsd:element name="Name" type="xsd:string"&gt;
        String name = (String) node.getChildValue("Name");

        // turn into qname
        QName qName = (QName) new XSQNameBinding(namespaceContext).parse(null, name);
        property.setName(qName);

        // &lt;xsd:element minOccurs="0" name="Value"&gt;
        if (node.hasChild("Value")) {
            Object object = node.getChildValue("Value");

            // check for a map
            if (object instanceof Map) {
                Map map = (Map) object;

                // this means a complex element parsed by xs:AnyType binding
                // try to pull out some text
                if (!map.isEmpty()) {
                    // first check for some text
                    if (map.containsKey(null)) {
                        property.setValue(map.get(null));
                    } else {
                        // perhaps some other value
                        property.setValue(map.values().iterator().next());
                    }
                }
            } else {
                property.setValue(object);
            }
        }

        return property;
    }
}
