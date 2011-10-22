/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import java.net.URI;

import javax.xml.namespace.QName;

import net.opengis.wfs.PropertyType;
import net.opengis.wfsv.VersionedUpdateElementType;
import net.opengis.wfsv.WfsvFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.filter.Filter;


/**
 * Binding object for the type http://www.opengis.net/wfsv:VersionedUpdateElementType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="VersionedUpdateElementType"&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="wfs:UpdateElementType"&gt;
 *              &lt;xsd:attribute name="featureVersion" type="xsd:string" use="required"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                       The syntax and semantics is the same as featureVersion in Query. If specified,
 *                       update will check that every updated feature is still at the specified version before
 *                       executing, and will fail if a change occurred on the server in the meantime.
 *                    &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class VersionedUpdateElementTypeBinding extends AbstractComplexBinding {
    private WfsvFactory wfsvFactory;

    public VersionedUpdateElementTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.VersionedUpdateElementType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return VersionedUpdateElementType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        VersionedUpdateElementType update = wfsvFactory.createVersionedUpdateElementType();

        //&lt;xsd:element maxOccurs="unbounded" ref="wfs:Property"&gt;
        update.getProperty().addAll(node.getChildValues(PropertyType.class));

        //&lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"&gt;
        update.setFilter((Filter) node.getChildValue(Filter.class));

        //&lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("handle")) {
            update.setHandle((String) node.getAttributeValue("handle"));
        }

        //&lt;xsd:attribute name="typeName" type="xsd:QName" use="required"&gt;
        update.setTypeName((QName) node.getAttributeValue("typeName"));

        //&lt;xsd:attribute default="x-application/gml:3" name="inputFormat"
        //      type="xsd:string" use="optional"&gt;
        if (node.hasAttribute("inputFormat")) {
            update.setInputFormat((String) node.getAttributeValue("inputFormat"));
        }

        //&lt;xsd:attribute name="srsName" type="xsd:anyURI" use="optional"&gt;
        if (node.hasAttribute("srsName")) {
            update.setSrsName((URI) node.getAttributeValue("srsName"));
        }

        update.setFeatureVersion((String) node.getAttributeValue("featureVersion"));

        return update;
    }
}
