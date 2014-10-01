package org.geotools.tjs.bindings;


import net.opengis.tjs10.PurposeType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.InstanceComponent;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:purposeType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:simpleType ecore:name="PurposeType" name="purposeType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration value="SpatialComponentIdentifier"/&gt;
 *          &lt;xsd:enumeration value="SpatialComponentProportion"/&gt;
 *          &lt;xsd:enumeration value="SpatialComponentPercentage"/&gt;
 *          &lt;xsd:enumeration value="TemporalIdentifier"/&gt;
 *          &lt;xsd:enumeration value="TemporalValue"/&gt;
 *          &lt;xsd:enumeration value="VerticalIdentifier"/&gt;
 *          &lt;xsd:enumeration value="VerticalValue"/&gt;
 *          &lt;xsd:enumeration value="OtherSpatialIdentifier"/&gt;
 *          &lt;xsd:enumeration value="NonSpatialIdentifier"/&gt;
 *          &lt;xsd:enumeration value="Attribute"/&gt;
 *      &lt;/xsd:restriction&gt;
 *  &lt;/xsd:simpleType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class PurposeTypeBinding extends AbstractSimpleBinding {
    Tjs10Factory factory;

    public PurposeTypeBinding(Tjs10Factory factory) {
        this.factory = factory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.purposeType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return PurposeType.class;
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
        return PurposeType.getByName(value.toString());
    }

}
