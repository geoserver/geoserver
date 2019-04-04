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
 * Binding object for the type http://www.opengis.net/ows:WGS84BoundingBoxType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;complexType name="WGS84BoundingBoxType"&gt;
 *      &lt;annotation&gt;
 *          &lt;documentation&gt;XML encoded minimum rectangular bounding box (or region) parameter, surrounding all the associated data. This box is specialized for use with the 2D WGS 84 coordinate reference system with decimal values of longitude and latitude. &lt;/documentation&gt;
 *          &lt;documentation&gt;This type is adapted from the general BoundingBoxType, with modified contents and documentation for use with the 2D WGS 84 coordinate reference system. &lt;/documentation&gt;
 *      &lt;/annotation&gt;
 *      &lt;complexContent&gt;
 *          &lt;restriction base="ows:BoundingBoxType"&gt;
 *              &lt;sequence&gt;
 *                  &lt;element name="LowerCorner" type="ows:PositionType2D"&gt;
 *                      &lt;annotation&gt;
 *                          &lt;documentation&gt;Position of the bounding box corner at which the values of longitude and latitude normally are the algebraic minimums within this bounding box. For more information, see Subclauses 10.4.5 and C.13. &lt;/documentation&gt;
 *                      &lt;/annotation&gt;
 *                  &lt;/element&gt;
 *                  &lt;element name="UpperCorner" type="ows:PositionType2D"&gt;
 *                      &lt;annotation&gt;
 *                          &lt;documentation&gt;Position of the bounding box corner at which the values of longitude and latitude normally are the algebraic minimums within this bounding box. For more information, see Subclauses 10.4.5 and C.13. &lt;/documentation&gt;
 *                      &lt;/annotation&gt;
 *                  &lt;/element&gt;
 *              &lt;/sequence&gt;
 *              &lt;attribute fixed="urn:ogc:def:crs:OGC:2:84" name="crs"
 *                  type="anyURI" use="optional"&gt;
 *                  &lt;annotation&gt;
 *                      &lt;documentation&gt;This attribute can be included when considered useful. When included, this attribute shall reference the 2D WGS 84 coordinate reference system with longitude before latitude and decimal values of longitude and latitude. &lt;/documentation&gt;
 *                  &lt;/annotation&gt;
 *              &lt;/attribute&gt;
 *              &lt;attribute fixed="2" name="dimensions"
 *                  type="positiveInteger" use="optional"&gt;
 *                  &lt;annotation&gt;
 *                      &lt;documentation&gt;The number of dimensions in this CRS (the length of a coordinate sequence in this use of the PositionType). This number is specified by the CRS definition, but can also be specified here. &lt;/documentation&gt;
 *                  &lt;/annotation&gt;
 *              &lt;/attribute&gt;
 *          &lt;/restriction&gt;
 *      &lt;/complexContent&gt;
 *  &lt;/complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class WGS84BoundingBoxTypeBinding extends AbstractComplexBinding {
    Ows10Factory owsfactory;

    public WGS84BoundingBoxTypeBinding(Ows10Factory owsfactory) {
        this.owsfactory = owsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return OWS.WGS84BOUNDINGBOXTYPE;
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
