/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import javax.xml.namespace.QName;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.WfsFactory;
import org.geotools.xsd.AbstractComplexEMFBinding;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;

/**
 * Binding object for the type http://www.opengis.net/wfs:TransactionResponseType.
 *
 * <p>
 *
 * <pre>
 *         <code>
 *  &lt;xsd:complexType name="TransactionResponseType"&gt;
 *      &lt;xsd:annotation&gt;
 *          &lt;xsd:documentation xml:lang="en"&gt;
 *              The response for a transaction request that was successfully
 *              completed. If the transaction failed for any reason, an
 *              exception report is returned instead.
 *           &lt;/xsd:documentation&gt;
 *      &lt;/xsd:annotation&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element name="TransactionSummary" type="wfs:TransactionSummaryType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation xml:lang="en"&gt;
 *                    The TransactionSummary element is used to summarize
 *                    the number of feature instances affected by the
 *                    transaction.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element minOccurs="0" name="TransactionResults" type="wfs:TransactionResultsType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation xml:lang="en"&gt;
 *                    For systems that do not support atomic transactions,
 *                    the TransactionResults element may be used to report
 *                    exception codes and messages for all actions of a
 *                    transaction that failed to execute successfully.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element name="InsertResults" type="wfs:InsertResultType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation xml:lang="en"&gt;
 *                    A transaction is a collection of Insert,Update and Delete
 *                    actions.  The Update and Delete actions modify features
 *                    that already exist.  The Insert action, however, creates
 *                    new features.  The InsertResults element is used to
 *                    report the identifiers of the newly created features.
 *                 &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute fixed="1.1.0" name="version" type="xsd:string" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;
 *                 The version attribute contains the version of the request
 *                 that generated this response.  So a V1.1.0 transaction
 *                 request generates a V1.1.0 transaction response.
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
public class TransactionResponseTypeBinding extends AbstractComplexEMFBinding {
    WfsFactory wfsfactory;

    public TransactionResponseTypeBinding(WfsFactory wfsfactory) {
        this.wfsfactory = wfsfactory;
    }

    /** @generated */
    public QName getTarget() {
        return WFS.TRANSACTIONRESPONSETYPE;
    }

    /**
     *
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return TransactionResponseType.class;
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
