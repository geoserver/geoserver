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
 * A representation of the model object '<em><b>Join Data Response Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getStatus <em>Status</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getDataInputs <em>Data Inputs</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getJoinedOutputs <em>Joined Outputs</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.JoinDataResponseType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='JoinDataResponse_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType()
 */
public interface JoinDataResponseType extends EObject {
    /**
     * Returns the value of the '<em><b>Status</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Execution status of the JoinData request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Status</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='Status' namespace='##targetNamespace'"
     * @generated
     * @see #setStatus(StatusType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_Status()
     */
    StatusType getStatus();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getStatus <em>Status</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Status</em>' containment reference.
     * @generated
     * @see #getStatus()
     */
    void setStatus(StatusType value);

    /**
     * Returns the value of the '<em><b>Data Inputs</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Descriptions of the framework, dataset, and attributes used to generate the outputs of the JoinData operation.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Data Inputs</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='DataInputs' namespace='##targetNamespace'"
     * @generated
     * @see #setDataInputs(DataInputsType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_DataInputs()
     */
    DataInputsType getDataInputs();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getDataInputs <em>Data Inputs</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Data Inputs</em>' containment reference.
     * @generated
     * @see #getDataInputs()
     */
    void setDataInputs(DataInputsType value);

    /**
     * Returns the value of the '<em><b>Joined Outputs</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * List of outputs resulting from the JoinData operation. There must be at least one output when the operation has completed successfully.   Each output mechanism advertised for this framework in the DescribeJoinAbiities response shall be represented here.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Joined Outputs</em>' containment reference.
     * @model containment="true" required="true"
     * extendedMetaData="kind='element' name='JoinedOutputs' namespace='##targetNamespace'"
     * @generated
     * @see #setJoinedOutputs(JoinedOutputsType)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_JoinedOutputs()
     */
    JoinedOutputsType getJoinedOutputs();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getJoinedOutputs <em>Joined Outputs</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Joined Outputs</em>' containment reference.
     * @generated
     * @see #getJoinedOutputs()
     */
    void setJoinedOutputs(JoinedOutputsType value);

    /**
     * Returns the value of the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * GetCapabilities URL of the TJS server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Capabilities</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String" required="true"
     * extendedMetaData="kind='attribute' name='capabilities'"
     * @generated
     * @see #setCapabilities(String)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_Capabilities()
     */
    String getCapabilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getCapabilities <em>Capabilities</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Capabilities</em>' attribute.
     * @generated
     * @see #getCapabilities()
     */
    void setCapabilities(String value);

    /**
     * Returns the value of the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * RFC 4646 language code of the human-readable text (e.g. "en-CA").
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Lang</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='lang' namespace='http://www.w3.org/XML/1998/namespace'"
     * @generated
     * @see #setLang(Object)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_Lang()
     */
    Object getLang();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getLang <em>Lang</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Lang</em>' attribute.
     * @generated
     * @see #getLang()
     */
    void setLang(Object value);

    /**
     * Returns the value of the '<em><b>Service</b></em>' attribute.
     * The default value is <code>"TJS"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Service type identifier
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Service</em>' attribute.
     * @model default="TJS" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='service'"
     * @generated
     * @see #isSetService()
     * @see #unsetService()
     * @see #setService(Object)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_Service()
     */
    Object getService();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getService <em>Service</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Service</em>' attribute.
     * @generated
     * @see #isSetService()
     * @see #unsetService()
     * @see #getService()
     */
    void setService(Object value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getService <em>Service</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetService()
     * @see #getService()
     * @see #setService(Object)
     */
    void unsetService();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getService <em>Service</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Service</em>' attribute is set.
     * @generated
     * @see #unsetService()
     * @see #getService()
     * @see #setService(Object)
     */
    boolean isSetService();

    /**
     * Returns the value of the '<em><b>Version</b></em>' attribute.
     * The default value is <code>"1.0"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Version of the TJS interface specification implemented by the server (1.0)
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Version</em>' attribute.
     * @model default="1.0" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='version'"
     * @generated
     * @see #isSetVersion()
     * @see #unsetVersion()
     * @see #setVersion(Object)
     * @see net.opengis.tjs10.Tjs10Package#getJoinDataResponseType_Version()
     */
    Object getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Version</em>' attribute.
     * @generated
     * @see #isSetVersion()
     * @see #unsetVersion()
     * @see #getVersion()
     */
    void setVersion(Object value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetVersion()
     * @see #getVersion()
     * @see #setVersion(Object)
     */
    void unsetVersion();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.JoinDataResponseType#getVersion <em>Version</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Version</em>' attribute is set.
     * @generated
     * @see #unsetVersion()
     * @see #getVersion()
     * @see #setVersion(Object)
     */
    boolean isSetVersion();

} // JoinDataResponseType
