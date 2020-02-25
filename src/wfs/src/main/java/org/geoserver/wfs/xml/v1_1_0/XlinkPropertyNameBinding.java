/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.math.BigInteger;
import javax.xml.namespace.QName;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.XlinkPropertyNameType;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the element http://www.opengis.net/wfs:XlinkPropertyName.
 *
 * <p>
 *
 * <pre>
 *  <code>
 *  &lt;xsd:element name="XlinkPropertyName"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              This element may be used in place of an wfs:PropertyName element
 *              in a wfs:Query element in a wfs:GetFeature element to selectively
 *              request the traversal of nested XLinks in the returned element for
 *              the named property. This element may not be used in other requests
 *              -- GetFeatureWithLock, LockFeature, Insert, Update, Delete -- in
 *              this version of the WFS specification.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:complexType&gt;
 *          &lt;xsd:simpleContent&gt;
 *              &lt;xsd:extension base="xsd:string"&gt;
 *                  &lt;xsd:attribute name="traverseXlinkDepth"
 *                      type="xsd:string" use="required"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;
 *                    This attribute indicates the depth to which nested property
 *                    XLink linking element locator attribute (href) XLinks are
 *                    traversed and resolved if possible.  A value of "1" indicates
 *                    that one linking element locator attribute (href) Xlink
 *                    will be traversed and the referenced element returned if
 *                    possible, but nested property XLink linking element locator
 *                    attribute (href) XLinks in the returned element are not
 *                    traversed.  A value of  "*" indicates that all nested property
 *                    XLink linking element locator attribute (href) XLinks will be
 *                    traversed and the referenced elements returned if possible.
 *                    The range of valid values for this attribute consists of
 *                    positive integers plus "*".
 *                       &lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:attribute&gt;
 *                  &lt;xsd:attribute name="traverseXlinkExpiry"
 *                      type="xsd:positiveInteger" use="optional"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;
 *                    The traverseXlinkExpiry attribute value is specified in
 *                    minutes It indicates how long a Web Feature Service should
 *                    wait to receive a response to a nested GetGmlObject request.
 *                       &lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:attribute&gt;
 *              &lt;/xsd:extension&gt;
 *          &lt;/xsd:simpleContent&gt;
 *      &lt;/xsd:complexType&gt;
 *  &lt;/xsd:element&gt;
 *
 *   </code>
 *  </pre>
 *
 * @generated
 */
public class XlinkPropertyNameBinding extends AbstractComplexBinding {

    WfsFactory factory;

    public XlinkPropertyNameBinding(WfsFactory factory) {
        this.factory = factory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.XLINKPROPERTYNAME;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return XlinkPropertyNameType.class;
    }

    public int getExecutionMode() {
        return OVERRIDE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {

        XlinkPropertyNameType property = factory.createXlinkPropertyNameType();

        property.setValue((String) value);

        // &lt;xsd:attribute name="traverseXlinkDepth"
        //      type="xsd:string" use="required"&gt;
        property.setTraverseXlinkDepth((String) node.getAttributeValue("traverseXlinkDepth"));

        // &lt;xsd:attribute name="traverseXlinkExpiry"
        //      type="xsd:positiveInteger" use="optional"&gt;
        if (node.hasAttribute("traverseXlinkExpiry")) {
            property.setTraverseXlinkExpiry(
                    (BigInteger) node.getAttributeValue("traverseXlinkExpiry"));
        }

        return property;
    }
}
