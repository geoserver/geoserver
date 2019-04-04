/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.LockType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.opengis.filter.Filter;

/**
 * Binding object for the type http://www.opengis.net/wfs:LockType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="LockType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              This type defines the Lock element.  The Lock element
 *              defines a locking operation on feature instances of
 *              a single type. An OGC Filter is used to constrain the
 *              scope of the operation.  Features to be locked can be
 *              identified individually by using their feature identifier
 *              or they can be locked by satisfying the spatial and
 *              non-spatial constraints defined in the filter.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"/&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"/&gt;
 *      &lt;xsd:attribute name="typeName" type="xsd:QName" use="required"/&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 *
 * @generated
 */
public class LockTypeBinding extends AbstractComplexBinding {
    WfsFactory wfsfactory;

    public LockTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.LOCKTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return LockType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        LockType lock = wfsfactory.createLockType();

        // &lt;xsd:element maxOccurs="1" minOccurs="0" ref="ogc:Filter"/&gt;
        if (node.hasChild(Filter.class)) {
            lock.setFilter((Filter) node.getChildValue(Filter.class));
        }

        // &lt;xsd:attribute name="handle" type="xsd:string" use="optional"/&gt;
        if (node.hasAttribute("handle")) {
            lock.setHandle((String) node.getAttributeValue("handle"));
        }

        // &lt;xsd:attribute name="typeName" type="xsd:QName" use="required"/&gt;
        lock.setTypeName((QName) node.getAttributeValue("typeName"));

        return lock;
    }
}
