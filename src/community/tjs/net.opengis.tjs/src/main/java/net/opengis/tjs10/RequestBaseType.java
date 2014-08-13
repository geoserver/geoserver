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
 * A representation of the model object '<em><b>Request Base Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <!-- begin-model-doc -->
 * TJS operation request base, for all TJS operations except GetCapabilities. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation.
 * <!-- end-model-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.RequestBaseType#getLanguage <em>Language</em>}</li>
 * <li>{@link net.opengis.tjs10.RequestBaseType#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.RequestBaseType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='RequestBaseType' kind='empty'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getRequestBaseType()
 */
public interface RequestBaseType extends EObject {
    /**
     * Returns the value of the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Language requested by the client for all human readable text in the response.  Consists of a two or five character RFC 4646 language code.  Must map to a language supported by the server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Language</em>' attribute.
     * @model dataType="org.eclipse.emf.ecore.xml.type.String"
     * extendedMetaData="kind='attribute' name='language'"
     * @generated
     * @see #setLanguage(String)
     * @see net.opengis.tjs10.Tjs10Package#getRequestBaseType_Language()
     */
    String getLanguage();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.RequestBaseType#getLanguage <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Language</em>' attribute.
     * @generated
     * @see #getLanguage()
     */
    void setLanguage(String value);

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
     * @see net.opengis.tjs10.Tjs10Package#getRequestBaseType_Service()
     */
    Object getService();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.RequestBaseType#getService <em>Service</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.RequestBaseType#getService <em>Service</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.RequestBaseType#getService <em>Service</em>}' attribute is set.
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
     * The literals are from the enumeration {@link net.opengis.tjs10.VersionType2}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Two-part version identifier requested by the client.  Must map to a version supported by the server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Version</em>' attribute.
     * @model unsettable="true"
     * extendedMetaData="kind='attribute' name='version'"
     * @generated
     * @see net.opengis.tjs10.VersionType2
     * @see #isSetVersion()
     * @see #unsetVersion()
     * @see #setVersion(VersionType2)
     * @see net.opengis.tjs10.Tjs10Package#getRequestBaseType_Version()
     */
    VersionType2 getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.RequestBaseType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Version</em>' attribute.
     * @generated
     * @see net.opengis.tjs10.VersionType2
     * @see #isSetVersion()
     * @see #unsetVersion()
     * @see #getVersion()
     */
    void setVersion(VersionType2 value);

    /**
     * Unsets the value of the '{@link net.opengis.tjs10.RequestBaseType#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #isSetVersion()
     * @see #getVersion()
     * @see #setVersion(VersionType2)
     */
    void unsetVersion();

    /**
     * Returns whether the value of the '{@link net.opengis.tjs10.RequestBaseType#getVersion <em>Version</em>}' attribute is set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return whether the value of the '<em>Version</em>' attribute is set.
     * @generated
     * @see #unsetVersion()
     * @see #getVersion()
     * @see #setVersion(VersionType2)
     */
    boolean isSetVersion();

} // RequestBaseType
