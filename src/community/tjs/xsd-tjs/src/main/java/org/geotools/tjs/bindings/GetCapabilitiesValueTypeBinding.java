package org.geotools.tjs.bindings;


import net.opengis.tjs10.GetCapabilitiesValueType;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.InstanceComponent;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:GetCapabilitiesValueType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:simpleType name="GetCapabilitiesValueType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration value="GetCapabilities"/&gt;
 *      &lt;/xsd:restriction&gt;
 *  &lt;/xsd:simpleType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class GetCapabilitiesValueTypeBinding extends AbstractSimpleBinding {

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.GetCapabilitiesValueType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetCapabilitiesValueType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value)
            throws Exception {

        //TODO: implement and remove call to super
        return GetCapabilitiesValueType.getByName(value.toString());
    }

}
