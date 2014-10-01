package org.geotools.tjs.bindings;


import net.opengis.tjs10.NullType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:NullType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="NullType" name="NullType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="identifier" ref="tjs:Identifier"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Text string representing a null value, found in the "V" elements of this attribute.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="title" ref="tjs:Title"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="abstract" ref="tjs:Abstract"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;One or more paragraphs of human-readable relevant text suitable for display in a pop-up window.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="documentation" minOccurs="0" ref="tjs:Documentation"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL reference to a web-accessible resource which contains further information describing this object.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class NullTypeBinding extends AbstractComplexEMFBinding {

    public NullTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.NullType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return NullType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {

        //TODO: implement and remove call to super
        return super.parse(instance, node, value);
    }

}
