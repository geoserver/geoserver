package org.geotools.tjs.bindings;


import net.opengis.tjs10.StatusType;
import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.TJS;
import org.geotools.xml.AbstractComplexEMFBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;

import javax.xml.namespace.QName;

/**
 * Binding object for the type http://www.opengis.net/tjs/1.0:StatusType.
 * <p/>
 * <p>
 * <pre>
 * 	 <code>
 *  &lt;xsd:complexType ecore:name="StatusType" name="StatusType"&gt;
 *      &lt;xsd:sequence&gt;
 *          &lt;xsd:element ecore:name="accepted" form="qualified"
 *              minOccurs="0" name="Accepted" type="xsd:anyType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Indicates that this request has been accepted by the server, but has not yet completed. The contents of this human-readable text string is left open to definition by each server implementation, but is expected to include any messages the server may wish to let the clients know. Such information could include when completion is expected, or any warning conditions that may have been encountered. The client may display this text to a human user. &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="completed" form="qualified"
 *              minOccurs="0" name="Completed" type="xsd:anyType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Indicates that this request has completed execution with at lease partial success. The contents of this human-readable text string is left open to definition by each server, but is expected to include any messages the server may wish to let the client know, such as how long the operation took to execute, or any warning conditions that may have been encountered. The client may display this text string to a human user. The client should make use of the presence of this element to trigger automated or manual access to the results of the operation.  If manual access is intended, the client should use the presence of this element to present the results as downloadable links to the user. &lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *          &lt;xsd:element ecore:name="failed" form="qualified" minOccurs="0"
 *              name="Failed" type="tjs:FailedType"&gt;
 *              &lt;xsd:annotation&gt;
 *                  &lt;xsd:documentation&gt;Indicates that execution of the JoinData operation failed, and includes error information.  The client may display this text string to a human user.  The presence of this element indicates that the operation completely failed and no Outputs were produced.&lt;/xsd:documentation&gt;
 *              &lt;/xsd:annotation&gt;
 *          &lt;/xsd:element&gt;
 *      &lt;/xsd:sequence&gt;
 *      &lt;xsd:attribute name="creationTime" type="xsd:anySimpleType" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;The time (UTC) that the JoinData operation finished.  If the operation is still in progress, this element shall contain the creation time of this document.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *      &lt;xsd:attribute ref="xlink:href" use="required"&gt;
 *          &lt;xsd:annotation&gt;
 *              &lt;xsd:documentation&gt;HTTP reference to location where current JoinDataResponse document is stored.&lt;/xsd:documentation&gt;
 *          &lt;/xsd:annotation&gt;
 *      &lt;/xsd:attribute&gt;
 *  &lt;/xsd:complexType&gt;
 *
 * 	  </code>
 * 	 </pre>
 * </p>
 *
 * @generated
 */
public class StatusTypeBinding extends AbstractComplexEMFBinding {

    public StatusTypeBinding(Tjs10Factory factory) {
        super(factory);
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return TJS.StatusType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return StatusType.class;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Object parse(ElementInstance instance, Node node, Object value)
            throws Exception {

        //TODO: implement and remove call to super
        return super.parse(instance, node, value);
    }

}
