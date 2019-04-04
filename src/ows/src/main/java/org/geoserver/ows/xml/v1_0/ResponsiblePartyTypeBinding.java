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
 * Binding object for the type http://www.opengis.net/ows:ResponsiblePartyType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="ResponsiblePartyType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Identification of, and means of communication with, person responsible for the server. At least one of IndividualName, OrganisationName, or PositionName shall be included. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element minOccurs="0" ref="ows:IndividualName"/&gt;
 *          &lt;element minOccurs="0" ref="ows:OrganisationName"/&gt;
 *          &lt;element minOccurs="0" ref="ows:PositionName"/&gt;
 *          &lt;element minOccurs="0" ref="ows:ContactInfo"/&gt;
 *          &lt;element ref="ows:Role"/&gt;
 *      &lt;/sequence&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class ResponsiblePartyTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public ResponsiblePartyTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.RESPONSIBLEPARTYTYPE;
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
