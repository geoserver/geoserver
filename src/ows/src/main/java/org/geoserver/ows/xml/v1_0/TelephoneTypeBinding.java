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
 * Binding object for the type http://www.opengis.net/ows:TelephoneType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="TelephoneType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Telephone numbers for contacting the responsible individual or organization. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" name="Voice" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Telephone number by which individuals can speak to the responsible organization or individual. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element maxOccurs="unbounded" minOccurs="0" name="Facsimile" type="string"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Telephone number of a facsimile machine for the responsible
 *  organization or individual. &lt;/documentation&gt;
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
public class TelephoneTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public TelephoneTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.TELEPHONETYPE;
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
