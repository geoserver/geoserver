package org.geotools.tjs.bindings;


import net.opengis.tjs10.GaussianType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.InstanceComponent;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:gaussianType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:simpleType ecore:name="GaussianType" name="gaussianType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration value="true"/&gt;
 *          &lt;xsd:enumeration value="false"/&gt;
 *          &lt;xsd:enumeration value="unknown"/&gt;
 *      &lt;/xsd:restriction&gt;
 *  &lt;/xsd:simpleType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class GaussianTypeBinding extends AbstractSimpleBinding {
    Tjs10Factory factory;

    public GaussianTypeBinding(Tjs10Factory factory) {
        this.factory = factory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.gaussianType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GaussianType.class;
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
        return super.parse(instance, value);
    }

}
