/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:FeatureTypeType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="FeatureTypeType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              An element of this type that describes a feature in an application
 *              namespace shall have an xml xmlns specifier, e.g.
 *              xmlns:bo="http://www.BlueOx.org/BlueOx"
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element name="Name" type="xsd:QName"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    Name of this feature type, including any namespace prefix
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element name="Title" type="xsd:string"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    Title of this feature type, normally used for display
 *                    to a human.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element minOccurs="0" name="Abstract" type="xsd:string"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    Brief narrative description of this feature type, normally
 *                    used for display to a human.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element maxOccurs="unbounded" minOccurs="0" ref="ows:Keywords"/&gt;
 *          &lt;xsd:choice&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element name="DefaultSRS" type="xsd:anyURI"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;
 *                          The DefaultSRS element indicated which spatial
 *                          reference system shall be used by a WFS to
 *                          express the state of a spatial feature if not
 *                          otherwise explicitly identified within a query
 *                          or transaction request.  The SRS may be indicated
 *                          using either the EPSG form (EPSG:posc code) or
 *                          the URL form defined in subclause 4.3.2 of
 *                          refernce[2].
 *                       &lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *                  &lt;xsd:element maxOccurs="unbounded" minOccurs="0"
 *                      name="OtherSRS" type="xsd:anyURI"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;
 *                          The OtherSRS element is used to indicate other
 *                          supported SRSs within query and transaction
 *                          operations.  A supported SRS means that the
 *                          WFS supports the transformation of spatial
 *                          properties between the OtherSRS and the internal
 *                          storage SRS.  The effects of such transformations
 *                          must be considered when determining and declaring
 *                          the guaranteed data accuracy.
 *                       &lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *              &lt;/xsd:sequence&gt;
 *              &lt;xsd:element name="NoSRS"&gt;
 *                  &lt;xsd:complexType/&gt;
 *              &lt;/xsd:element&gt;
 *          &lt;/xsd:choice&gt;
 *          &lt;xsd:element minOccurs="0" name="Operations" type="wfs:OperationsType"/&gt;
 *          &lt;xsd:element minOccurs="0" name="OutputFormats" type="wfs:OutputFormatListType"/&gt;
 *          &lt;xsd:element maxOccurs="unbounded" minOccurs="1" ref="ows:WGS84BoundingBox"/&gt;
 *          &lt;xsd:element maxOccurs="unbounded" minOccurs="0"
 *              name="MetadataURL" type="wfs:MetadataURLType"/&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class FeatureTypeTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public FeatureTypeTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.FEATURETYPETYPE;
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
