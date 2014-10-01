package org.geotools.tjs.bindings;


import net.opengis.tjs10.OutputMechanismsType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:OutputMechanismsType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType name="OutputMechanismsType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="mechanism" maxOccurs="unbounded" ref="tjs:Mechanism"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Mechanism by which the attribute data can be accessed once it has been joined to the spatial framework.&lt;/xsd:documentation&gt;
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
public class OutputMechanismsTypeBinding extends AbstractComplexEMFBinding {

    public OutputMechanismsTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.OutputMechanismsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return OutputMechanismsType.class;
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
