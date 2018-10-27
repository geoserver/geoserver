/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.util.Iterator;
import javax.xml.namespace.QName;
import net.opengis.wfs.AllSomeType;
import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.NativeType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:TransactionType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="TransactionType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation&gt;
 *              The TranactionType defines the Transaction operation.  A
 *              Transaction element contains one or more Insert, Update
 *              Delete and Native elements that allow a client application
 *              to create, modify or remove feature instances from the
 *              feature repository that a Web Feature Service controls.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element minOccurs="0" ref="wfs:LockId"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;
 *                    In order for a client application to operate upon locked
 *                    feature instances, the Transaction request must include
 *                    the LockId element.  The content of this element must be
 *                    the lock identifier the client application obtained from
 *                    a previous GetFeatureWithLock or LockFeature operation.
 *
 *                    If the correct lock identifier is specified the Web
 *                    Feature Service knows that the client application may
 *                    operate upon the locked feature instances.
 *
 *                    No LockId element needs to be specified to operate upon
 *                    unlocked features.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:choice maxOccurs="unbounded" minOccurs="0"&gt;
 *              &lt;xsd:element ref="wfs:Insert"/&gt;
 *              &lt;xsd:element ref="wfs:Update"/&gt;
 *              &lt;xsd:element ref="wfs:Delete"/&gt;
 *              &lt;xsd:element ref="wfs:Native"/&gt;
 *          &lt;/xsd:choice&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute fixed="1.0.0" name="version" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute fixed="WFS" name="service" type="xsd:string" use="required"/&gt;
 *      &lt;xsd:attribute name="handle" type="xsd:string" use="optional"/&gt;
 *      &lt;xsd:attribute name="releaseAction" type="wfs:AllSomeType" use="optional"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The releaseAction attribute is used to control how a Web
 *                 Feature service releases locks on feature instances after
 *                 a Transaction request has been processed.
 *
 *                 Valid values are ALL or SOME.
 *
 *                 A value of ALL means that the Web Feature Service should
 *                 release the locks of all feature instances locked with the
 *                 specified lockId, regardless or whether or not the features
 *                 were actually modified.
 *
 *                 A value of SOME means that the Web Feature Service will
 *                 only release the locks held on feature instances that
 *                 were actually operated upon by the transaction.  The lockId
 *                 that the client application obtained shall remain valid and
 *                 the other, unmodified, feature instances shall remain locked.
 *                 If the expiry attribute was specified in the original operation
 *                 that locked the feature instances, then the expiry counter
 *                 will be reset to give the client application that same amount
 *                 of time to post subsequent transactions against the locked
 *                 features.
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
public class TransactionTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public TransactionTypeBinding(WfsFactory wfsfactory) {
        super(wfsfactory);
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.TRANSACTIONTYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return TransactionType.class;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        TransactionType transaction = wfsfactory.createTransactionType();

        // lock id
        if (node.hasChild("LockId")) {
            transaction.setLockId((String) node.getChildValue("LockId"));
        }

        // transactions, need to maintain order
        for (Iterator itr = node.getChildren().iterator(); itr.hasNext(); ) {
            Node child = (Node) itr.next();
            Object cv = child.getValue();

            if (cv instanceof InsertElementType) {
                transaction.getInsert().add(cv);
            } else if (cv instanceof UpdateElementType) {
                transaction.getUpdate().add(cv);
            } else if (cv instanceof DeleteElementType) {
                transaction.getDelete().add(cv);
            } else if (cv instanceof NativeType) {
                transaction.getNative().add(cv);
            }
        }

        // service + version
        WFSBindingUtils.service(transaction, node);
        WFSBindingUtils.version(transaction, node);

        // handle
        if (node.hasAttribute("handle")) {
            transaction.setHandle((String) node.getAttributeValue("handle"));
        }

        // release action
        if (node.hasAttribute(AllSomeType.class)) {
            transaction.setReleaseAction((AllSomeType) node.getAttributeValue(AllSomeType.class));
        }

        return transaction;
    }
}
