package org.geotools.tjs.bindings;


import net.opengis.tjs10.JoinAbilitiesType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:JoinAbilitiesType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="JoinAbilitiesType" name="JoinAbilitiesType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="spatialFrameworks" ref="tjs:SpatialFrameworks"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Full description of all spatial frameworks to which attribute data can be joined.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="attributeLimit" ref="tjs:AttributeLimit"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Maximum number of attributes that can be joined simultaneously as part of a JoinData request.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="outputMechanisms" form="qualified"
 *              name="OutputMechanisms" type="tjs:OutputMechanismsType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;List of mechanisms by which the attribute data will be accessible once it has been joined to the spatial framework.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="outputStylings" form="qualified"
 *              minOccurs="0" name="OutputStylings" type="tjs:OutputStylingsType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Unordered list of display or content styling instructions supported by the server and that can be applied if the AccessMechanisms of the requested output includes WMS.  If WMS is not supported by the server then this element shall not be present.  If WMS is supported and this element is not present, a default styling will be applied to the WMS layer.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="classificationSchemaURL"
 *              form="qualified" minOccurs="0"
 *              name="ClassificationSchemaURL" type="xsd:anyType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;URL that returns an XML Schema document that specifying an XML structure for describing data classifications.  When included with a JoinData request, the contents of such an XML file can be applied by the server in order to produce the data classes used in the JoinData output.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="capabilities" type="xsd:string" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;GetCapabilities URL of the TJS server.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute ref="xml:lang" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;RFC 4646 language code of the human-readable text (e.g. "en-CA").&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute ecore:default="TJS" name="service"
 *          type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Service type identifier&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="updateSupported" type="xsd:boolean" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Boolean that identifies if existing JoinData products can be updated by this service.  If "true" then subsequent identical JoinData requests will update existing JoinData products that were created by this service.  These updated products will then be available via the existing URLs of those products.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute ecore:default="1.0" name="version"
 *          type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Version of the TJS interface specification implemented by the server (1.0)&lt;/xsd:documentation&gt;
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
public class JoinAbilitiesTypeBinding extends AbstractComplexEMFBinding {

    public JoinAbilitiesTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.JoinAbilitiesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return JoinAbilitiesType.class;
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
