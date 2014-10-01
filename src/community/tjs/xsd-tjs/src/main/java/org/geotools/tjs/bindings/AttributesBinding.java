package org.geotools.tjs.bindings;


import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.InstanceComponent;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/tjs/1.0:Attributes.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:element name="Attributes" type="xsd:string"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;The AttributeNames requested by the user, in comma-delimited format&lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *  &lt;/xsd:element&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class AttributesBinding extends AbstractSimpleBinding {

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.Attributes;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return String.class;
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
        return value.toString();
    }

}
