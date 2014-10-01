package org.geotools.tjs.bindings;


import net.opengis.tjs10.RequestBaseType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:RequestBaseType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType name="RequestBaseType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;TJS operation request base, for all TJS operations except GetCapabilities. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation.&lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:attribute name="language" type="xsd:string"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Language requested by the client for all human readable text in the response.  Consists of a two or five character RFC 4646 language code.  Must map to a language supported by the server.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute ecore:default="TJS" name="service"
 *          type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Service type identifier requested by the client.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="version" type="tjs:versionType"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Two-part version identifier requested by the client.  Must map to a version supported by the server.&lt;/xsd:documentation&gt;
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
public class RequestBaseTypeBinding extends AbstractComplexEMFBinding {

    public RequestBaseTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.RequestBaseType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return RequestBaseType.class;
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
