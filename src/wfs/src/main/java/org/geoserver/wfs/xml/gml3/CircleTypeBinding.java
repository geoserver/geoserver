/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.gml3;

import javax.xml.namespace.QName;
import org.geoserver.wfs.WFSException;
import org.geotools.gml3.Circle;
import org.geotools.gml3.GML;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/gml:CircleType.
 *
 * <p>We include this here, and not part of gml because its job is to fail, its a cite compliance
 * thing :).
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="CircleType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;A Circle is an arc whose ends coincide to form a simple closed loop. The "start" and "end" bearing are equal and shall be the bearing for the first controlPoint listed. The three control points must be distinct non-co-linear points for the Circle to be unambiguously defined. The arc is simply extended past the third control point until the first control point is encountered.&lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;complexContent&gt;
 *          &lt;extension base="gml:ArcType"/&gt;
 *      &lt;/complexContent&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class CircleTypeBinding extends AbstractComplexBinding {
    /** @generated */
    public QName getTarget() {
        return GML.CircleType;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return Circle.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        throw new WFSException("Circle is not supported", "InvalidParameterValue");
    }
}
