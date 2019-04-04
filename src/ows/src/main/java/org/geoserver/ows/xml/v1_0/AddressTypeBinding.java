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
 * Binding object for the type http://www.opengis.net/ows:AddressType.
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="AddressType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Location of the responsible individual or organization. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0"
 *              name="DeliveryPoint" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Address line for the location. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="City" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;City of the location. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="AdministrativeArea" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;State or province of the location. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="PostalCode" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;ZIP or other postal code. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element minOccurs="0" name="Country" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Country of the physical address. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0"
 *              name="ElectronicMailAddress" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Address of the electronic mailbox of the responsible organization or individual. &lt;/documentation&gt;
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
public class AddressTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public AddressTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.ADDRESSTYPE;
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
