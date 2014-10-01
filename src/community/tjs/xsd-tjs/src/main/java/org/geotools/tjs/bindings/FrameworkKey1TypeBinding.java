package org.geotools.tjs.bindings;


import net.opengis.tjs10.FrameworkKeyType1;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:FrameworkKey1Type.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="FrameworkKeyType1" name="FrameworkKey1Type"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="column" form="qualified"
 *              maxOccurs="unbounded" name="Column" type="tjs:Column2Type"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Identifies a column that is used to form the framework key.  Where more than one of these elements is present then all of these columns are required to join the data table to the spatial framework.  The order of these elements defines the order of the "K" elements in the Rowset/Row structures below.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="complete" type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Identifies if there is at least one record in the Attribute dataset for every record in the Framework dataset.  “true” indicates that this is the case. “false” indicates that some Keys in the Framework dataset cannot be found in the Attribute dataset.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="relationship" type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Identifies if the relationship between the Framework and the Attribute datasets are 1:1 or 1:many.  “one” indicates that there is at most one record in the Attribute dataset for every key in the Framework dataset.  “many” indicates that there may be more than one record in the Attribute dataset for every key in the Framework dataset, in which case some preliminary processing is required before the join operation can take place.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class FrameworkKey1TypeBinding extends AbstractComplexEMFBinding {

    public FrameworkKey1TypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.FrameworkKey1Type;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return FrameworkKeyType1.class;
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
