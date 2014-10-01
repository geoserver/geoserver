package org.geotools.tjs.bindings;


import net.opengis.tjs10.ResourceType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:ResourceType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="ResourceType" name="ResourceType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="uRL" form="qualified" name="URL" type="xsd:anyType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL from which this resource can be electronically retrieved, or from which a document can be retrieved that indicates access details for the resource (such as a OGC Capabilities document).  For OGC web services this shall be the complete GetCapabilities URL.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="parameter" form="qualified"
 *              maxOccurs="unbounded" minOccurs="0" name="Parameter" type="tjs:ParameterType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Parameter that may need to be included the HTTP requests to a web service identified by the URL parameter above.  For a WMS output there shall be one occurance of this element, and it shall be populated with the name of the layer produced by the JoinData operation.&lt;/xsd:documentation&gt;
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
public class ResourceTypeBinding extends AbstractComplexEMFBinding {

    public ResourceTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.ResourceType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return ResourceType.class;
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
