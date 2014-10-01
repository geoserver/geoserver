/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import net.opengis.ows11.OperationsMetadataType;
import net.opengis.ows11.ServiceIdentificationType;
import net.opengis.ows11.ServiceProviderType;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Tjs Capabilities Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getServiceIdentification <em>Service Identification</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getServiceProvider <em>Service Provider</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getOperationsMetadata <em>Operations Metadata</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getLanguages <em>Languages</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getWSDL <em>WSDL</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getUpdateSequence <em>Update Sequence</em>}</li>
 * <li>{@link net.opengis.tjs10.TjsCapabilitiesType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='tjsCapabilitiesType' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType()
 */
public interface TjsCapabilitiesType extends EObject {
    /**
     * Returns the value of the '<em><b>Service Identification</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * General metadata for this specific server. This XML Schema of this section shall be the same for all OWS.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Service Identification</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='ServiceIdentification' namespace='http://www.opengis.net/ows/1.1'"
     * @generated
     * @see #setServiceIdentification(ServiceIdentificationType)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_ServiceIdentification()
     */
    ServiceIdentificationType getServiceIdentification();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getServiceIdentification <em>Service Identification</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Service Identification</em>' containment reference.
     * @generated
     * @see #getServiceIdentification()
     */
    void setServiceIdentification(ServiceIdentificationType value);

    /**
     * Returns the value of the '<em><b>Service Provider</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Metadata about the organization that provides this specific service instance or server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Service Provider</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='ServiceProvider' namespace='http://www.opengis.net/ows/1.1'"
     * @generated
     * @see #setServiceProvider(ServiceProviderType)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_ServiceProvider()
     */
    ServiceProviderType getServiceProvider();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getServiceProvider <em>Service Provider</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Service Provider</em>' containment reference.
     * @generated
     * @see #getServiceProvider()
     */
    void setServiceProvider(ServiceProviderType value);

    /**
     * Returns the value of the '<em><b>Operations Metadata</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Metadata about the operations and related abilities specified by this service and implemented by this server, including the URLs for operation requests. The basic contents of this section shall be the same for all OWS types, but individual services can add elements and/or change the optionality of optional elements.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Operations Metadata</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='OperationsMetadata' namespace='http://www.opengis.net/ows/1.1'"
     * @generated
     * @see #setOperationsMetadata(OperationsMetadataType)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_OperationsMetadata()
     */
    OperationsMetadataType getOperationsMetadata();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getOperationsMetadata <em>Operations Metadata</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Operations Metadata</em>' containment reference.
     * @generated
     * @see #getOperationsMetadata()
     */
    void setOperationsMetadata(OperationsMetadataType value);

    /**
     * Returns the value of the '<em><b>Languages</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * A list of human languages that this server supports.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Languages</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Languages' namespace='##targetNamespace'"
     * @generated
     * @see #setLanguages(LanguagesType)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_Languages()
     */
    LanguagesType getLanguages();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getLanguages <em>Languages</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Languages</em>' containment reference.
     * @generated
     * @see #getLanguages()
     */
    void setLanguages(LanguagesType value);

    /**
     * Returns the value of the '<em><b>WSDL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Location of a WSDL document for this service.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>WSDL</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='WSDL' namespace='##targetNamespace'"
     * @generated
     * @see #setWSDL(WSDLType)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_WSDL()
     */
    WSDLType getWSDL();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getWSDL <em>WSDL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>WSDL</em>' containment reference.
     * @generated
     * @see #getWSDL()
     */
    void setWSDL(WSDLType value);

    /**
     * Returns the value of the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * RFC 4646 language code of the human-readable text (e.g. "en-CA")
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Lang</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='lang' namespace='http://www.w3.org/XML/1998/namespace'"
     * @generated
     * @see #setLang(Object)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_Lang()
     */
    Object getLang();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getLang <em>Lang</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_Service()
     */
    Object getService();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getService <em>Service</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getService <em>Service</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getService <em>Service</em>}' attribute is set.
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
     * Returns the value of the '<em><b>Update Sequence</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Update Sequence</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Update Sequence</em>' attribute.
     * @model dataType="net.opengis.ows11.UpdateSequenceType"
     * extendedMetaData="kind='attribute' name='updateSequence'"
     * @generated
     * @see #setUpdateSequence(String)
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_UpdateSequence()
     */
    String getUpdateSequence();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getUpdateSequence <em>Update Sequence</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Update Sequence</em>' attribute.
     * @generated
     * @see #getUpdateSequence()
     */
    void setUpdateSequence(String value);

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
     * @see net.opengis.tjs10.Tjs10Package#getTjsCapabilitiesType_Version()
     */
    Object getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getVersion <em>Version</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getVersion <em>Version</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.TjsCapabilitiesType#getVersion <em>Version</em>}' attribute is set.
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

} // TjsCapabilitiesType
