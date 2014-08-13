package org.geotools.tjs.bindings;


import net.opengis.tjs10.AttributeDataType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:AttributeDataType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="AttributeDataType" name="AttributeDataType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="getDataURL" form="qualified"
 *              minOccurs="0" name="GetDataURL" type="xsd:anyURI"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL which returns a valid tjs 0.12 GetData response.  Note that this may be a tjs GetData request (via HTTP GET), a stored response to a GetData request, or a web process that returns content compliant with the GetData response schema.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="getDataXML" form="qualified"
 *              minOccurs="0" name="GetDataXML" type="tjs:GetDataXMLType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;GetData request in XML encoding, including the name of the tjs server to be queried.  Note that since XML encoding of the GetData request is optional for tjs servers, this choice should not be used unless it is known that the tjs server supports this request method.&lt;/xsd:documentation&gt;
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
public class AttributeDataTypeBinding extends AbstractComplexEMFBinding {

    public AttributeDataTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.AttributeDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return AttributeDataType.class;
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
