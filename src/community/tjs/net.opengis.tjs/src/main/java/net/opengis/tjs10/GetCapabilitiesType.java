/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import net.opengis.ows11.AcceptFormatsType;
import net.opengis.ows11.SectionsType;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Get Capabilities Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.GetCapabilitiesType#getAcceptVersions <em>Accept Versions</em>}</li>
 * <li>{@link net.opengis.tjs10.GetCapabilitiesType#getSections <em>Sections</em>}</li>
 * <li>{@link net.opengis.tjs10.GetCapabilitiesType#getAcceptFormats <em>Accept Formats</em>}</li>
 * <li>{@link net.opengis.tjs10.GetCapabilitiesType#getLanguage <em>Language</em>}</li>
 * <li>{@link net.opengis.tjs10.GetCapabilitiesType#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.GetCapabilitiesType#getUpdateSequence <em>Update Sequence</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='GetCapabilities_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType()
 */
public interface GetCapabilitiesType extends EObject {
    /**
     * Returns the value of the '<em><b>Accept Versions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Prioritized sequence of one or more specification versions accepted by client, with preferred versions listed first.  Version negotiation is similar to that specified by the OWS 1.1 Version Negotiation subclause except that the form of the TJS version number differs slightly.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Accept Versions</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='AcceptVersions' namespace='##targetNamespace'"
     * @generated
     * @see #setAcceptVersions(AcceptVersionsType)
     * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType_AcceptVersions()
     */
    AcceptVersionsType getAcceptVersions();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getAcceptVersions <em>Accept Versions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Accept Versions</em>' containment reference.
     * @generated
     * @see #getAcceptVersions()
     */
    void setAcceptVersions(AcceptVersionsType value);

    /**
     * Returns the value of the '<em><b>Sections</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * When omitted or not supported by server, server shall return complete service metadata (Capabilities) document.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Sections</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='Sections' namespace='##targetNamespace'"
     * @generated
     * @see #setSections(SectionsType)
     * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType_Sections()
     */
    SectionsType getSections();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getSections <em>Sections</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Sections</em>' containment reference.
     * @generated
     * @see #getSections()
     */
    void setSections(SectionsType value);

    /**
     * Returns the value of the '<em><b>Accept Formats</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * When omitted or not supported by server, server shall return service metadata document using the MIME type "text/xml".
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Accept Formats</em>' containment reference.
     * @model containment="true"
     * extendedMetaData="kind='element' name='AcceptFormats' namespace='##targetNamespace'"
     * @generated
     * @see #setAcceptFormats(AcceptFormatsType)
     * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType_AcceptFormats()
     */
    AcceptFormatsType getAcceptFormats();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getAcceptFormats <em>Accept Formats</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Accept Formats</em>' containment reference.
     * @generated
     * @see #getAcceptFormats()
     */
    void setAcceptFormats(AcceptFormatsType value);

    /**
     * Returns the value of the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Language requested by the client for all human readable text in the response.  Consists of a two or five character RFC 4646 language code.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Language</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType"
     * extendedMetaData="kind='attribute' name='language'"
     * @generated
     * @see #setLanguage(Object)
     * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType_Language()
     */
    Object getLanguage();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getLanguage <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Language</em>' attribute.
     * @generated
     * @see #getLanguage()
     */
    void setLanguage(Object value);

    /**
     * Returns the value of the '<em><b>Service</b></em>' attribute.
     * The default value is <code>"TJS"</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Service type identifier requested by the client.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Service</em>' attribute.
     * @model default="TJS" unsettable="true" dataType="org.eclipse.emf.ecore.xml.type.AnySimpleType" required="true"
     * extendedMetaData="kind='attribute' name='service'"
     * @generated
     * @see #isSetService()
     * @see #unsetService()
     * @see #setService(Object)
     * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType_Service()
     */
    Object getService();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getService <em>Service</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getService <em>Service</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getService <em>Service</em>}' attribute is set.
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
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * When omitted or not supported by server, server shall return latest complete service metadata document.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Update Sequence</em>' attribute.
     * @model dataType="net.opengis.ows11.UpdateSequenceType"
     * extendedMetaData="kind='attribute' name='updateSequence'"
     * @generated
     * @see #setUpdateSequence(String)
     * @see net.opengis.tjs10.Tjs10Package#getGetCapabilitiesType_UpdateSequence()
     */
    String getUpdateSequence();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.GetCapabilitiesType#getUpdateSequence <em>Update Sequence</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Update Sequence</em>' attribute.
     * @generated
     * @see #getUpdateSequence()
     */
    void setUpdateSequence(String value);

} // GetCapabilitiesType
