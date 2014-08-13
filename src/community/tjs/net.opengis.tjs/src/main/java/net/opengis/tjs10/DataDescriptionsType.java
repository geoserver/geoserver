/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Data Descriptions Type</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.DataDescriptionsType#getFramework <em>Framework</em>}</li>
 * <li>{@link net.opengis.tjs10.DataDescriptionsType#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.DataDescriptionsType#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.DataDescriptionsType#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.DataDescriptionsType#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='DataDescriptions_._type' kind='elementOnly'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDataDescriptionsType()
 */
public interface DataDescriptionsType extends EObject {
    /**
     * Returns the value of the '<em><b>Framework</b></em>' containment reference list.
     * The list contents are of type {@link net.opengis.tjs10.FrameworkDatasetDescribeDataType}.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Spatial framework for which attribute data is housed on this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework</em>' containment reference list.
     * @model type="net.opengis.tjs10.FrameworkDatasetDescribeDataType" containment="true" required="true"
     * extendedMetaData="kind='element' name='Framework' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getDataDescriptionsType_Framework()
     */
    EList getFramework();

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
     * @see net.opengis.tjs10.Tjs10Package#getDataDescriptionsType_Capabilities()
     */
    String getCapabilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getCapabilities <em>Capabilities</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getDataDescriptionsType_Lang()
     */
    Object getLang();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getLang <em>Lang</em>}' attribute.
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
     * @see net.opengis.tjs10.Tjs10Package#getDataDescriptionsType_Service()
     */
    Object getService();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getService <em>Service</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getService <em>Service</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getService <em>Service</em>}' attribute is set.
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
     * @see net.opengis.tjs10.Tjs10Package#getDataDescriptionsType_Version()
     */
    Object getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getVersion <em>Version</em>}' attribute.
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
     * Unsets the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getVersion <em>Version</em>}' attribute.
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
     * Returns whether the value of the '{@link net.opengis.tjs10.DataDescriptionsType#getVersion <em>Version</em>}' attribute is set.
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

} // DataDescriptionsType
