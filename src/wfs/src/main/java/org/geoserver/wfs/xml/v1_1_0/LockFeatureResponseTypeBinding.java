/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.LockFeatureResponseType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:LockFeatureResponseType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="LockFeatureResponseType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              The LockFeatureResponseType is used to define an
 *              element to contains the response to a LockFeature
 *              operation.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ref="wfs:LockId"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The LockFeatureResponse includes a LockId element
 *                    that contains a lock identifier.  The lock identifier
 *                    can be used by a client, in subsequent operations, to
 *                    operate upon the locked feature instances.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element minOccurs="0" name="FeaturesLocked" type="wfs:FeaturesLockedType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The LockFeature or GetFeatureWithLock operations
 *                    identify and attempt to lock a set of feature
 *                    instances that satisfy the constraints specified
 *                    in the request.  In the event that the lockAction
 *                    attribute (on the LockFeature or GetFeatureWithLock
 *                    elements) is set to SOME, a Web Feature Service will
 *                    attempt to lock as many of the feature instances from
 *                    the result set as possible.
 *
 *                    The FeaturesLocked element contains list of ogc:FeatureId
 *                    elements enumerating the feature instances that a WFS
 *                    actually managed to lock.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element minOccurs="0" name="FeaturesNotLocked" type="wfs:FeaturesNotLockedType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    In contrast to the FeaturesLocked element, the
 *                    FeaturesNotLocked element contains a list of
 *                    ogc:Filter elements identifying feature instances
 *                    that a WFS did not manage to lock because they were
 *                    already locked by another process.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class LockFeatureResponseTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public LockFeatureResponseTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.LOCKFEATURERESPONSETYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return LockFeatureResponseType.class;
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
