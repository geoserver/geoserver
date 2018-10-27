/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.Ows10Factory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/ows:ContactType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="ContactType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Information required to enable contact with the responsible person and/or organization. &lt;/documentation&gt;
 *          &lt;documentation&gt;For OWS use in the service metadata document, the optional hoursOfService and contactInstructions elements were retained, as possibly being useful in the ServiceProvider section. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element minOccurs="0" name="Phone" type="ows:TelephoneType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Telephone numbers at which the organization or individual may be contacted. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="Address" type="ows:AddressType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Physical and email address at which the organization or individual may be contacted. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="OnlineResource" type="ows:OnlineResourceType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;On-line information that can be used to contact the individual or organization. OWS specifics: The xlink:href attribute in the xlink:simpleLink attribute group shall be used to reference this resource. Whenever practical, the xlink:href attribute with type anyURI should be a URL from which more contact information can be electronically retrieved. The xlink:title attribute with type "string" can be used to name this set of information. The other attributes in the xlink:simpleLink attribute group should not be used. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="HoursOfService" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Time period (including time zone) when individuals can contact the organization or individual. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="ContactInstructions" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Supplemental instructions on how or when to contact the individual or organization. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *      &lt;/sequence&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class ContactTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public ContactTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.CONTACTTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return null;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        // TODO: implement
        return null;
    }
}
