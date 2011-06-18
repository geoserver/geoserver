/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_1_0;

import javax.xml.namespace.QName;

import net.opengis.wfsv.RollbackType;
import net.opengis.wfsv.WfsvFactory;

import org.geoserver.wfs.WFSException;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.filter.Filter;


/**
 * Binding object for the type http://www.opengis.net/wfsv:RollbackType.
 *
 * <p>
 *        <pre>
 *         <code>
 *  &lt;xsd:complexType name="RollbackType"&gt;
 *      &lt;xsd:complexContent&gt;
 *          &lt;xsd:extension base="wfs:NativeType"&gt;
 *              &lt;xsd:sequence&gt;
 *                  &lt;xsd:element maxOccurs="1" minOccurs="1" ref="wfsv:DifferenceQuery"&gt;
 *                      &lt;xsd:annotation&gt;
 *                          &lt;xsd:documentation&gt;
 *                          The difference filter will be used to compute a diff to be applied
 *                          in order to perform the rollback. A rollback is conceptually just
 *                          a Transaction applied on the result of a back-diff between two
 *                          revisions.
 *                       &lt;/xsd:documentation&gt;
 *                      &lt;/xsd:annotation&gt;
 *                  &lt;/xsd:element&gt;
 *              &lt;/xsd:sequence&gt;
 *              &lt;xsd:attribute name="handle" type="xsd:string" use="optional"&gt;
 *                  &lt;xsd:annotation&gt;
 *                      &lt;xsd:documentation&gt;
 *                       The handle attribute allows a client application
 *                       to assign a client-generated request identifier
 *                       to an Insert action.  The handle is included to
 *                       facilitate error reporting.  If a Rollback action
 *                       in a Transaction request fails, then a Versioning WFS may
 *                       include the handle in an exception report to localize
 *                       the error.  If no handle is included of the offending
 *                       Rollback element then a WFS may employee other means of
 *                       localizing the error (e.g. line number).
 *                    &lt;/xsd:documentation&gt;
 *                  &lt;/xsd:annotation&gt;
 *              &lt;/xsd:attribute&gt;
 *          &lt;/xsd:extension&gt;
 *      &lt;/xsd:complexContent&gt;
 *  &lt;/xsd:complexType&gt;
 *
 *          </code>
 *         </pre>
 * </p>
 *
 * @generated
 */
public class RollbackTypeBinding extends AbstractComplexBinding {
    private WfsvFactory wfsvFactory;

    public RollbackTypeBinding(WfsvFactory wfsvFactory) {
        this.wfsvFactory = wfsvFactory;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return WFSV.RollbackType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return RollbackType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        RollbackType rollback = wfsvFactory.createRollbackType();

        if (node.hasAttribute("handle")) {
            rollback.setHandle((String) node.getAttributeValue("handle"));
        }

        rollback.setFilter((Filter) node.getChildValue(Filter.class));
        if (!node.hasAttribute("typeName")) 
            throw new WFSException("The typeName attribute is mandatory");
        rollback.setTypeName((QName) node.getAttributeValue("typeName"));
        rollback.setToFeatureVersion((String) node.getAttributeValue("toFeatureVersion"));
        rollback.setVendorId((String) node.getAttributeValue("vendorId"));
        if (!node.hasAttribute("safeToIgnore")) 
            throw new WFSException("The attribute safeToIgnore=true|false is mandatory");
        rollback.setSafeToIgnore(((Boolean) node.getAttributeValue("safeToIgnore")).booleanValue());
        rollback.setUser((String) node.getAttributeValue("user"));

        return rollback;
    }
}
