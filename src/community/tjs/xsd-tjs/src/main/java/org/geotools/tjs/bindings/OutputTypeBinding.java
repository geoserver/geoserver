package org.geotools.tjs.bindings;


import net.opengis.tjs10.OutputType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:OutputType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="OutputType" name="OutputType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="mechanism" ref="tjs:Mechanism"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;The access mechanism by which the joined data has been made available.  &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="resource" form="qualified"
 *              minOccurs="0" name="Resource" type="tjs:ResourceType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Reference to a web-accessible resource that was created by the JoinData operation.  This element shall be populated once the output has been successfully produced.  Prior to that time the content of the subelements may be empty.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="exceptionReport" form="qualified"
 *              minOccurs="0" name="ExceptionReport" type="tjs:ExceptionReportType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Unordered list of one or more errors encountered during the JoinData operation for this output.  These Exception elements shall be interpreted by clients as being independent of one another (not hierarchical).  This element is populated when the production of this output did not succeed.&lt;/xsd:documentation&gt;
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
public class OutputTypeBinding extends AbstractComplexEMFBinding {

    public OutputTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.OutputType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return OutputType.class;
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
