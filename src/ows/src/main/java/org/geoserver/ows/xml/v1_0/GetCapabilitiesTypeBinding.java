/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.AcceptFormatsType;
import net.opengis.ows10.AcceptVersionsType;
import net.opengis.ows10.GetCapabilitiesType;
import net.opengis.ows10.Ows10Factory;
import net.opengis.ows10.SectionsType;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.Binding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/ows:GetCapabilitiesType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="GetCapabilitiesType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;XML encoded GetCapabilities operation request. This operation allows clients to retrieve service metadata about a specific service instance. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation. This base type shall be extended by each specific OWS to include the additional required "service" attribute, with the correct value for that OWS. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element minOccurs="0" name="AcceptVersions" type="ows:AcceptVersionsType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;When omitted, server shall return latest supported version. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="Sections" type="ows:SectionsType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;When omitted or not supported by server, server shall return complete service metadata (Capabilities) document. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="AcceptFormats" type="ows:AcceptFormatsType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;When omitted or not supported by server, server shall return service metadata document using the MIME type "text/xml". &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute name="updateSequence" type="ows:UpdateSequenceType" use="optional"&gt;
 *          &lt;annotation&gt;
 *              &lt;documentation&gt;When omitted or not supported by server, server shall return latest complete service metadata document. &lt;/documentation&gt;
 *          &lt;/annotation&gt;
 *      &lt;/attribute&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class GetCapabilitiesTypeBinding extends AbstractComplexEMFBinding {
    Ows10Factory owsfactory;

    public GetCapabilitiesTypeBinding(Ows10Factory owsfactory) {
        super(owsfactory);
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.GETCAPABILITIESTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return GetCapabilitiesType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     *
     * @param value an instance of {@link GetCapabilitiesType} (possibly a subclass) if a binding
     *     for a specific service's GetCapabilities request used {@link Binding#BEFORE} {@link
     *     #getExecutionMode() execution mode}, and thus relies on this binding to fill the common
     *     properties. <code>null</code> otherwise.
     *     <!-- end-user-doc -->
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        GetCapabilitiesType getCapabilities;

        if ((value != null) && value instanceof GetCapabilitiesType) {
            getCapabilities = (GetCapabilitiesType) value;
        } else {
            getCapabilities = owsfactory.createGetCapabilitiesType();
        }

        getCapabilities.setAcceptVersions(
                (AcceptVersionsType) node.getChildValue(AcceptVersionsType.class));
        getCapabilities.setSections((SectionsType) node.getChildValue(SectionsType.class));
        getCapabilities.setAcceptFormats(
                (AcceptFormatsType) node.getChildValue(AcceptFormatsType.class));
        getCapabilities.setUpdateSequence((String) node.getAttributeValue("updateSequence"));

        return getCapabilities;
    }
}
