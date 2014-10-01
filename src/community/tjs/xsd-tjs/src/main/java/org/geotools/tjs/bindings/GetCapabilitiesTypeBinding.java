package org.geotools.tjs.bindings;


import net.opengis.tjs10.GetCapabilitiesType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:GetCapabilitiesType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="GetCapabilitiesType" name="GetCapabilitiesType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="acceptVersions" form="qualified"
 *              minOccurs="0" name="AcceptVersions" type="tjs:AcceptVersionsType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Prioritized sequence of one or more specification versions accepted by client, with preferred versions listed first.  Version negotiation is similar to that specified by the OWS 1.1 Version Negotiation subclause except that the form of the TJS version number differs slightly.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="sections" form="qualified"
 *              minOccurs="0" name="Sections" type="ows:SectionsType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;When omitted or not supported by server, server shall return complete service metadata (Capabilities) document. &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="acceptFormats" form="qualified"
 *              minOccurs="0" name="AcceptFormats" type="ows:AcceptFormatsType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;When omitted or not supported by server, server shall return service metadata document using the MIME type "text/xml". &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="language" type="xsd:anySimpleType"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Language requested by the client for all human readable text in the response.  Consists of a two or five character RFC 4646 language code.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute ecore:default="TJS" name="service"
 *          type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;Service type identifier requested by the client.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute name="updateSequence" type="ows:UpdateSequenceType"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;When omitted or not supported by server, server shall return latest complete service metadata document. &lt;/xsd:documentation&gt;
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
public class GetCapabilitiesTypeBinding extends AbstractComplexEMFBinding {

    public GetCapabilitiesTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.GetCapabilitiesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetCapabilitiesType.class;
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
