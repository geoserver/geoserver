/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import javax.xml.namespace.QName;
import net.opengis.ows10.Ows10Factory;
import org.geotools.xsd.AbstractSimpleBinding;
import org.geotools.xsd.InstanceComponent;

/**
 * Binding object for the type http://www.opengis.net/ows:ServiceType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;simpleType name="ServiceType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Service type identifier, where the string value is the OWS type abbreviation, such as "WMS" or "WFS". &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;restriction base="string"/&gt;
 *  &lt;/simpleType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class ServiceTypeBinding extends AbstractSimpleBinding {
    Ows10Factory owsfactory;

    public ServiceTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.SERVICETYPE;
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
    public Object parse(InstanceComponent instance, Object value) throws Exception {
        // TODO: implement
        return null;
    }
}
