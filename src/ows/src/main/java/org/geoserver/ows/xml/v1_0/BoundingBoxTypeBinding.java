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
 * Binding object for the type http://www.opengis.net/ows:BoundingBoxType.
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="BoundingBoxType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;XML encoded minimum rectangular bounding box (or region) parameter, surrounding all the associated data. &lt;/documentation&gt;
 *          &lt;documentation&gt;This type is adapted from the EnvelopeType of GML 3.1, with modified contents and documentation for encoding a MINIMUM size box SURROUNDING all associated data. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;sequence&gt;
 *          &lt;element name="LowerCorner" type="ows:PositionType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Position of the bounding box corner at which the value of each coordinate normally is the algebraic minimum within this bounding box. In some cases, this position is normally displayed at the top, such as the top left for some image coordinates. For more information, see Subclauses 10.2.5 and C.13. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *          &lt;element name="UpperCorner" type="ows:PositionType"&gt;
 *              &lt;annotation&gt;
 *                  &lt;documentation&gt;Position of the bounding box corner at which the value of each coordinate normally is the algebraic maximum within this bounding box. In some cases, this position is normally displayed at the bottom, such as the bottom right for some image coordinates. For more information, see Subclauses 10.2.5 and C.13. &lt;/documentation&gt;
 *              &lt;/annotation&gt;
 *          &lt;/element&gt;
 *      &lt;/sequence&gt;
 *      &lt;attribute name="crs" type="anyURI" use="optional"&gt;
 *          &lt;annotation&gt;
 *              &lt;documentation&gt;Usually references the definition of a CRS, as specified in [OGC Topic 2]. Such a CRS definition can be XML encoded using the gml:CoordinateReferenceSystemType in [GML 3.1]. For well known references, it is not required that a CRS definition exist at the location the URI points to. If no anyURI value is included, the applicable CRS must be either:
 *  a)        Specified outside the bounding box, but inside a data structure that includes this bounding box, as specified for a specific OWS use of this bounding box type.
 *  b)        Fixed and specified in the Implementation Specification for a specific OWS use of the bounding box type. &lt;/documentation&gt;
 *          &lt;/annotation&gt;
 *      &lt;/attribute&gt;
 *      &lt;attribute name="dimensions" type="positiveInteger" use="optional"&gt;
 *          &lt;annotation&gt;
 *              &lt;documentation&gt;The number of dimensions in this CRS (the length of a coordinate sequence in this use of the PositionType). This number is specified by the CRS definition, but can also be specified here. &lt;/documentation&gt;
 *          &lt;/annotation&gt;
 *      &lt;/attribute&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class BoundingBoxTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public BoundingBoxTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.BOUNDINGBOXTYPE;
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
