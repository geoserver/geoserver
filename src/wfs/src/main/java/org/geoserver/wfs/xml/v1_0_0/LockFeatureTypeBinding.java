/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.math.BigInteger;
import javax.xml.namespace.QName;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.LockFeatureType;
import net.opengis.wfs.LockType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:LockFeatureType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="LockFeatureType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              This type defines the LockFeature operation.  The LockFeature
 *              element contains one or more Lock elements that define
 *              which features of a particular type should be locked.  A lock
 *              identifier (lockId) is returned to the client application which
 *              can be used by subsequent operations to reference the locked
 *              features.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element maxOccurs="unbounded" name="Lock" type="wfs:LockType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    The lock element is used to indicate which feature
 *                    instances of particular type are to be locked.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute fixed="1.0.0" name="version" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute fixed="WFS" name="service" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute name="expiry" type="xsd:positiveInteger" use="optional"/&gt;
 *      &lt;xsd:attribute name="lockAction" type="wfs:AllSomeType" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The lockAction attribute is used to indicate what
 *                 a Web Feature Service should do when it encounters
 *                 a feature instance that has already been locked by
 *                 another client application.
 *
 *                 Valid values are ALL or SOME.
 *
 *                 ALL means that the Web Feature Service must acquire
 *                 locks on all the requested feature instances.  If it
 *                 cannot acquire those locks then the request should
 *                 fail.  In this instance, all locks acquired by the
 *                 operation should be released.
 *
 *                 SOME means that the Web Feature Service should lock
 *                 as many of the requested features as it can.
 *              &lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class LockFeatureTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public LockFeatureTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.LOCKFEATURETYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return LockFeatureType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        LockFeatureType lockFeature = wfsfactory.createLockFeatureType();

        // &lt;xsd:element maxOccurs="unbounded" name="Lock" type="wfs:LockType"&gt;
        lockFeature.getLock().addAll(node.getChildValues(LockType.class));

        // &lt;xsd:attribute fixed="1.0.0" name="version" type="xsd:string" use="required"/&gt;
        // &lt;xsd:attribute fixed="WFS" name="service" type="xsd:string" use="required"/&gt;
        WFSBindingUtils.version(lockFeature, node);
        WFSBindingUtils.service(lockFeature, node);

        // &lt;xsd:attribute name="expiry" type="xsd:positiveInteger" use="optional"/&gt;
        if (node.hasAttribute("expiry")) {
            lockFeature.setExpiry(
                    BigInteger.valueOf(((Number) node.getAttributeValue("expiry")).longValue()));
        }

        // &lt;xsd:attribute name="lockAction" type="wfs:AllSomeType" use="optional"&gt;
        if (node.hasAttribute(AllSomeType.class)) {
            lockFeature.setLockAction((AllSomeType) node.getAttributeValue(AllSomeType.class));
        }

        return lockFeature;
    }
}
