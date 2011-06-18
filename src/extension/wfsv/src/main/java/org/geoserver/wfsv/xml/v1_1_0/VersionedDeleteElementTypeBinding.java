/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfsv.VersionedDeleteElementType;
import net.opengis.wfsv.WfsvFactory;

import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;


/**
 * Binding object for the type http://www.opengis.net/wfsv:VersionedDeleteElementType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="VersionedDeleteElementType"&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="wfs:DeleteElementType"&gt;
 *              &lt;xsd:attribute name="featureVersion" type="xsd:string" use="required"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                       See VersionedUpdateElementType featureVersion attribute.
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
public class VersionedDeleteElementTypeBinding extends AbstractComplexBinding {
    private WfsvFactory wfsvFactory;

    public VersionedDeleteElementTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.VersionedDeleteElementType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return VersionedDeleteElementType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        VersionedDeleteElementType delete = wfsvFactory.createVersionedDeleteElementType();
        delete.setFeatureVersion((String) node.getAttributeValue("featureVersion"));

        return delete;
    }
}
