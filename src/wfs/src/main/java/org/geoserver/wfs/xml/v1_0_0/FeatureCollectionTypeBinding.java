/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:FeatureCollectionType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="FeatureCollectionType"&gt;       &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;             This type defines a
 *              container for the response to a              GetFeature or
 *              GetFeatureWithLock request.  If the             request is
 *              GetFeatureWithLock, the lockId attribute             must be
 *              populated.  The lockId attribute can otherwise
 *              be safely ignored.          &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;      &lt;xsd:complexContent&gt;        &lt;xsd:extension
 *              base="gml:AbstractFeatureCollectionType"&gt;
 *                  &lt;xsd:attribute name="lockId" type="xsd:string"
 *                  use="optional"&gt;             &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;                   The value
 *                          of the lockId attribute is an identifier
 *                          that a Web Feature Service generates and which a
 *                          client application can use in subsequent
 *                          operations                   (such as a
 *                          Transaction request) to reference the set
 *                          of locked features.
 *                      &lt;/xsd:documentation&gt;             &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;        &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;    &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class FeatureCollectionTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public FeatureCollectionTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.FEATURECOLLECTIONTYPE;
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
