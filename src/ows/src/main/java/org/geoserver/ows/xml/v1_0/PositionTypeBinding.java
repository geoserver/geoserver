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
 * Binding object for the type http://www.opengis.net/ows:PositionType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;simpleType name="PositionType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;Position instances hold the coordinates of a position in a coordinate reference system (CRS) referenced by the related "crs" attribute or elsewhere. For an angular coordinate axis that is physically continuous for multiple revolutions, but whose recorded values can be discontinuous, special conditions apply when the bounding box is continuous across the value discontinuity:
 *  a)  If the bounding box is continuous clear around this angular axis, then ordinate values of minus and plus infinity shall be used.
 *  b)  If the bounding box is continuous across the value discontinuity but is not continuous clear around this angular axis, then some non-normal value can be used if specified for a specific OWS use of the BoundingBoxType. For more information, see Subclauses 10.2.5 and C.13. &lt;/documentation&gt;
 *          &lt;documentation&gt;This type is adapted from DirectPositionType and doubleList of GML 3.1. The adaptations include omission of all the attributes, since the needed information is included in the BoundingBoxType. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;list itemType="double"/&gt;
 *  &lt;/simpleType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class PositionTypeBinding extends AbstractSimpleBinding {
    Ows10Factory owsfactory;

    public PositionTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.POSITIONTYPE;
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
