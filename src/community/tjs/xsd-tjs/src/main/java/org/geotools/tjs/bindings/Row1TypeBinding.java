package org.geotools.tjs.bindings;


import net.opengis.tjs10.RowType1;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:Row1Type.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="RowType1" name="Row1Type"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="k" maxOccurs="unbounded" ref="tjs:K"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Spatial Key for this row.  For the GetData response, when there is more than one "K" element they are ordered according to the same sequence as the "FrameworkKey" elements of the "Columnset" structure.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="v" form="qualified"
 *              maxOccurs="unbounded" name="V" type="tjs:VType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Value of a attribute (i.e. data) applicable to the spatial feature identified by the "K" elements of this row. When there is more than one "V" element, they are ordered according to the same sequence as the "Column" elements of the "Columnset" structure above.  When this value is null (indicated with the null attribute) an identification of the reason may be included in the content of this element.&lt;/xsd:documentation&gt;
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
public class Row1TypeBinding extends AbstractComplexEMFBinding {

    public Row1TypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.Row1Type;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return RowType1.class;
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
