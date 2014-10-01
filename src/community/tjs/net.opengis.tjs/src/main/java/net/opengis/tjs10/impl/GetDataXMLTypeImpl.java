/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.GetDataXMLType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Get Data XML Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl#getAttributes <em>Attributes</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl#getLinkageKeys <em>Linkage Keys</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl#getGetDataHost <em>Get Data Host</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl#getLanguage <em>Language</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GetDataXMLTypeImpl extends EObjectImpl implements GetDataXMLType {
    /**
     * The default value of the '{@link #getFrameworkURI() <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFrameworkURI()
     */
    protected static final String FRAMEWORK_URI_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getFrameworkURI() <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFrameworkURI()
     */
    protected String frameworkURI = FRAMEWORK_URI_EDEFAULT;

    /**
     * The default value of the '{@link #getDatasetURI() <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDatasetURI()
     */
    protected static final String DATASET_URI_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getDatasetURI() <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDatasetURI()
     */
    protected String datasetURI = DATASET_URI_EDEFAULT;

    /**
     * The default value of the '{@link #getAttributes() <em>Attributes</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributes()
     */
    protected static final String ATTRIBUTES_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getAttributes() <em>Attributes</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributes()
     */
    protected String attributes = ATTRIBUTES_EDEFAULT;

    /**
     * The default value of the '{@link #getLinkageKeys() <em>Linkage Keys</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLinkageKeys()
     */
    protected static final String LINKAGE_KEYS_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getLinkageKeys() <em>Linkage Keys</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLinkageKeys()
     */
    protected String linkageKeys = LINKAGE_KEYS_EDEFAULT;

    /**
     * The default value of the '{@link #getGetDataHost() <em>Get Data Host</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGetDataHost()
     */
    protected static final String GET_DATA_HOST_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getGetDataHost() <em>Get Data Host</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGetDataHost()
     */
    protected String getDataHost = GET_DATA_HOST_EDEFAULT;

    /**
     * The default value of the '{@link #getLanguage() <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLanguage()
     */
    protected static final String LANGUAGE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getLanguage() <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLanguage()
     */
    protected String language = LANGUAGE_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected GetDataXMLTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getGetDataXMLType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getFrameworkURI() {
        return frameworkURI;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFrameworkURI(String newFrameworkURI) {
        String oldFrameworkURI = frameworkURI;
        frameworkURI = newFrameworkURI;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_XML_TYPE__FRAMEWORK_URI, oldFrameworkURI, frameworkURI));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getDatasetURI() {
        return datasetURI;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDatasetURI(String newDatasetURI) {
        String oldDatasetURI = datasetURI;
        datasetURI = newDatasetURI;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_XML_TYPE__DATASET_URI, oldDatasetURI, datasetURI));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getAttributes() {
        return attributes;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAttributes(String newAttributes) {
        String oldAttributes = attributes;
        attributes = newAttributes;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_XML_TYPE__ATTRIBUTES, oldAttributes, attributes));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getLinkageKeys() {
        return linkageKeys;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLinkageKeys(String newLinkageKeys) {
        String oldLinkageKeys = linkageKeys;
        linkageKeys = newLinkageKeys;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_XML_TYPE__LINKAGE_KEYS, oldLinkageKeys, linkageKeys));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getGetDataHost() {
        return getDataHost;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGetDataHost(String newGetDataHost) {
        String oldGetDataHost = getDataHost;
        getDataHost = newGetDataHost;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_XML_TYPE__GET_DATA_HOST, oldGetDataHost, getDataHost));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getLanguage() {
        return language;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLanguage(String newLanguage) {
        String oldLanguage = language;
        language = newLanguage;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_XML_TYPE__LANGUAGE, oldLanguage, language));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.GET_DATA_XML_TYPE__FRAMEWORK_URI:
                return getFrameworkURI();
            case Tjs10Package.GET_DATA_XML_TYPE__DATASET_URI:
                return getDatasetURI();
            case Tjs10Package.GET_DATA_XML_TYPE__ATTRIBUTES:
                return getAttributes();
            case Tjs10Package.GET_DATA_XML_TYPE__LINKAGE_KEYS:
                return getLinkageKeys();
            case Tjs10Package.GET_DATA_XML_TYPE__GET_DATA_HOST:
                return getGetDataHost();
            case Tjs10Package.GET_DATA_XML_TYPE__LANGUAGE:
                return getLanguage();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case Tjs10Package.GET_DATA_XML_TYPE__FRAMEWORK_URI:
                setFrameworkURI((String) newValue);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__DATASET_URI:
                setDatasetURI((String) newValue);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__ATTRIBUTES:
                setAttributes((String) newValue);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__LINKAGE_KEYS:
                setLinkageKeys((String) newValue);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__GET_DATA_HOST:
                setGetDataHost((String) newValue);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__LANGUAGE:
                setLanguage((String) newValue);
                return;
        }
        super.eSet(featureID, newValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void eUnset(int featureID) {
        switch (featureID) {
            case Tjs10Package.GET_DATA_XML_TYPE__FRAMEWORK_URI:
                setFrameworkURI(FRAMEWORK_URI_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__DATASET_URI:
                setDatasetURI(DATASET_URI_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__ATTRIBUTES:
                setAttributes(ATTRIBUTES_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__LINKAGE_KEYS:
                setLinkageKeys(LINKAGE_KEYS_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__GET_DATA_HOST:
                setGetDataHost(GET_DATA_HOST_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_XML_TYPE__LANGUAGE:
                setLanguage(LANGUAGE_EDEFAULT);
                return;
        }
        super.eUnset(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean eIsSet(int featureID) {
        switch (featureID) {
            case Tjs10Package.GET_DATA_XML_TYPE__FRAMEWORK_URI:
                return FRAMEWORK_URI_EDEFAULT == null ? frameworkURI != null : !FRAMEWORK_URI_EDEFAULT.equals(frameworkURI);
            case Tjs10Package.GET_DATA_XML_TYPE__DATASET_URI:
                return DATASET_URI_EDEFAULT == null ? datasetURI != null : !DATASET_URI_EDEFAULT.equals(datasetURI);
            case Tjs10Package.GET_DATA_XML_TYPE__ATTRIBUTES:
                return ATTRIBUTES_EDEFAULT == null ? attributes != null : !ATTRIBUTES_EDEFAULT.equals(attributes);
            case Tjs10Package.GET_DATA_XML_TYPE__LINKAGE_KEYS:
                return LINKAGE_KEYS_EDEFAULT == null ? linkageKeys != null : !LINKAGE_KEYS_EDEFAULT.equals(linkageKeys);
            case Tjs10Package.GET_DATA_XML_TYPE__GET_DATA_HOST:
                return GET_DATA_HOST_EDEFAULT == null ? getDataHost != null : !GET_DATA_HOST_EDEFAULT.equals(getDataHost);
            case Tjs10Package.GET_DATA_XML_TYPE__LANGUAGE:
                return LANGUAGE_EDEFAULT == null ? language != null : !LANGUAGE_EDEFAULT.equals(language);
        }
        return super.eIsSet(featureID);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String toString() {
        if (eIsProxy()) return super.toString();

        StringBuffer result = new StringBuffer(super.toString());
        result.append(" (frameworkURI: ");
        result.append(frameworkURI);
        result.append(", datasetURI: ");
        result.append(datasetURI);
        result.append(", attributes: ");
        result.append(attributes);
        result.append(", linkageKeys: ");
        result.append(linkageKeys);
        result.append(", getDataHost: ");
        result.append(getDataHost);
        result.append(", language: ");
        result.append(language);
        result.append(')');
        return result.toString();
    }

} //GetDataXMLTypeImpl
