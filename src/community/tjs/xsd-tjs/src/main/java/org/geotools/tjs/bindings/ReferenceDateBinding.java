package org.geotools.tjs.bindings;


import net.opengis.tjs10.ReferenceDateType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the element http://www.opengis.net/tjs/1.0:ReferenceDate.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:element name="ReferenceDate" type="tjs:ReferenceDateType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;Point in time to which the Framework/Dataset applies.  If the startDate attribute is included then the contents of this element describes a range of time (from "startDate" to "ReferenceDate") to which the framework/dataset applies.  Valid content is a date field of the form http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#gYear, gYearMonth, date, or dateTime.&lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *  &lt;/xsd:element&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class ReferenceDateBinding extends AbstractComplexEMFBinding {

    public ReferenceDateBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.ReferenceDate;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return ReferenceDateType.class;
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
