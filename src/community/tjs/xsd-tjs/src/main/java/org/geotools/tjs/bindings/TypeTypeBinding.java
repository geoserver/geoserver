package org.geotools.tjs.bindings;


import net.opengis.tjs10.Tjs10Factory;
import net.opengis.tjs10.TypeType;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractSimpleBinding;
import org.geotools.xml.InstanceComponent;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:typeType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:simpleType ecore:name="TypeType" name="typeType"&gt;
 *      &lt;xsd:restriction base="xsd:string"&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2String" value="http://www.w3.org/TR/xmlschema-2/#string"/&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Boolean" value="http://www.w3.org/TR/xmlschema-2/#boolean"/&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Integer" value="http://www.w3.org/TR/xmlschema-2/#integer"/&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Decimal" value="http://www.w3.org/TR/xmlschema-2/#decimal"/&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Float" value="http://www.w3.org/TR/xmlschema-2/#float"/&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Double" value="http://www.w3.org/TR/xmlschema-2/#double"/&gt;
 *          &lt;xsd:enumeration ecore:name="httpWwwW3OrgTRXmlschema2Datetime" value="http://www.w3.org/TR/xmlschema-2/#datetime"/&gt;
 *      &lt;/xsd:restriction&gt;
 *  &lt;/xsd:simpleType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class TypeTypeBinding extends AbstractSimpleBinding {

    Tjs10Factory factory;

    public TypeTypeBinding(Tjs10Factory factory) {
        this.factory = factory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.typeType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return TypeType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(InstanceComponent instance, Object value)
            throws Exception {
        return TypeType.get(value.toString());
    }

}
