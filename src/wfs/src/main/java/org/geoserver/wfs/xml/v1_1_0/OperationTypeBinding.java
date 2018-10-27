/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractSimpleBinding;
import org.geotools.xsd.InstanceComponent;

/**
 * Binding object for the type http://www.opengis.net/wfs:OperationType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:simpleType name="OperationType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration value="Insert"/&gt;
 *          &lt;xsd:enumeration value="Unsert"/&gt;
 *          &lt;xsd:enumeration value="Delete"/&gt;
 *          &lt;xsd:enumeration value="Query"/&gt;
 *          &lt;xsd:enumeration value="Lock"/&gt;
 *          &lt;xsd:enumeration value="GetGmlObject"/&gt;
 *      &lt;/xsd:restriction&gt;
 *  &lt;/xsd:simpleType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class OperationTypeBinding extends AbstractSimpleBinding {
    WfsFactory wfsfactory;

    public OperationTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.OPERATIONTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return null;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value) throws Exception {
        // TODO: implement
        return null;
    }
}
