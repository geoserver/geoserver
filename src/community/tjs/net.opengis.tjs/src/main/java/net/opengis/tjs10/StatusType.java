/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Status Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.StatusType#getAccepted <em>Accepted</em>}</li>
 * <li>{@link net.opengis.tjs10.StatusType#getCompleted <em>Completed</em>}</li>
 * <li>{@link net.opengis.tjs10.StatusType#getFailed <em>Failed</em>}</li>
 * <li>{@link net.opengis.tjs10.StatusType#getCreationTime <em>Creation Time</em>}</li>
 * <li>{@link net.opengis.tjs10.StatusType#getHref <em>Href</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='Status_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getStatusType()
 */
public interface StatusType extends EObject {
    /**
     * Returns the value of the '<em><b>Accepted</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Indicates that this request has been accepted by the server, but has not yet completed. The contents of this human-readable text string is left open to definition by each server implementation, but is expected to include any messages the server may wish to let the clients know. Such information could include when completion is expected, or any warning conditions that may have been encountered. The client may display this text to a human user.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Accepted</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Accepted' namespace='##targetNamespace'"
     * @generated
     * @see #setAccepted(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getStatusType_Accepted()
     */
    EObject getAccepted();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StatusType#getAccepted <em>Accepted</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Accepted</em>' containment reference.
     * @generated
     * @see #getAccepted()
     */
    void setAccepted(EObject value);

    /**
     * Returns the value of the '<em><b>Completed</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Indicates that this request has completed execution with at lease partial success. The contents of this human-readable text string is left open to definition by each server, but is expected to include any messages the server may wish to let the client know, such as how long the operation took to execute, or any warning conditions that may have been encountered. The client may display this text string to a human user. The client should make use of the presence of this element to trigger automated or manual access to the results of the operation.  If manual access is intended, the client should use the presence of this element to present the results as downloadable links to the user.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Completed</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Completed' namespace='##targetNamespace'"
     * @generated
     * @see #setCompleted(EObject)
     * @see net.opengis.tjs10.Tjs10Package#getStatusType_Completed()
     */
    EObject getCompleted();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StatusType#getCompleted <em>Completed</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Completed</em>' containment reference.
     * @generated
     * @see #getCompleted()
     */
    void setCompleted(EObject value);

    /**
     * Returns the value of the '<em><b>Failed</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Indicates that execution of the JoinData operation failed, and includes error information.  The client may display this text string to a human user.  The presence of this element indicates that the operation completely failed and no Outputs were produced.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Failed</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Failed' namespace='##targetNamespace'"
     * @generated
     * @see #setFailed(FailedType)
     * @see net.opengis.tjs10.Tjs10Package#getStatusType_Failed()
     */
    FailedType getFailed();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StatusType#getFailed <em>Failed</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Failed</em>' containment reference.
     * @generated
     * @see #getFailed()
     */
    void setFailed(FailedType value);

    /**
     * Returns the value of the '<em><b>Creation Time</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The time (UTC) that the JoinData operation finished.  If the operation is still in progress, this element shall contain the creation time of this document.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Creation Time</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='creationTime'"
     * @generated
     * @see #setCreationTime(Object)
     * @see net.opengis.tjs10.Tjs10Package#getStatusType_CreationTime()
     */
    Object getCreationTime();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StatusType#getCreationTime <em>Creation Time</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Creation Time</em>' attribute.
     * @generated
     * @see #getCreationTime()
     */
    void setCreationTime(Object value);

    /**
     * Returns the value of the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * HTTP reference to location where current JoinDataResponse document is stored.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Href</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnyURI" required="true"
     * extendedMetaData="kind='attribute' name='href' namespace='http://www.w3.org/1999/xlink'"
     * @generated
     * @see #setHref(String)
     * @see net.opengis.tjs10.Tjs10Package#getStatusType_Href()
     */
    String getHref();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.StatusType#getHref <em>Href</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Href</em>' attribute.
     * @generated
     * @see #getHref()
     */
    void setHref(String value);

} // StatusType
