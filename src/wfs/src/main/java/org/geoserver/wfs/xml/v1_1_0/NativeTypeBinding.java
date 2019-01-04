/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.NativeType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:NativeType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="NativeType"&gt;
 *      &lt;xsd:seq uence>
 *          &lt;xsd:any processContents="lax" namespace="##other" minOccurs="0"/>
 *      &lt;/xsd:sequence>
 *      &lt;xsd:attribute name="vendorId" type="xsd:string" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The vendorId attribute is used to specify the name of
 *                 vendor who's vendor specific command the client
 *                 application wishes to execute.
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="safeToIgnore" type="xsd:boolean" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 In the event that a Web Feature Service does not recognize
 *                 the vendorId or does not recognize the vendor specific command,
 *                 the safeToIgnore attribute is used to indicate whether the
 *                 exception can be safely ignored.  A value of TRUE means that
 *                 the Web Feature Service may ignore the command.  A value of
 *                 FALSE means that a Web Feature Service cannot ignore the
 *                 command and an exception should be raised if a problem is
 *                 encountered.
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class NativeTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public NativeTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.NATIVETYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return NativeType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        NativeType nativ = wfsfactory.createNativeType();

        // &lt;xsd:attribute name="vendorId" type="xsd:string" use="required"&gt;
        nativ.setVendorId((String) node.getAttributeValue("vendorId"));

        // &lt;xsd:attribute name="safeToIgnore" type="xsd:boolean" use="required"&gt;
        nativ.setSafeToIgnore(((Boolean) node.getAttributeValue("safeToIgnore")).booleanValue());

        // &lt;xsd:any processContents="lax" namespace="##other" minOccurs="0"/>
        if (instance.getText() != null && instance.getText().length() != 0) {
            nativ.setValue(instance.getText());
        }
        return nativ;
    }
}
