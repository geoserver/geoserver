/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.DatasetDescriptionsType;
import net.opengis.tjs10.FrameworkType4;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import java.util.Collection;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Dataset Descriptions Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl#getFramework <em>Framework</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DatasetDescriptionsTypeImpl extends EObjectImpl implements DatasetDescriptionsType {
    /**
     * The cached value of the '{@link #getFramework() <em>Framework</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFramework()
     */
    protected EList framework;

    /**
     * The default value of the '{@link #getCapabilities() <em>Capabilities</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getCapabilities()
     */
    protected static final String CAPABILITIES_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getCapabilities() <em>Capabilities</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getCapabilities()
     */
    protected String capabilities = CAPABILITIES_EDEFAULT;

    /**
     * The default value of the '{@link #getLang() <em>Lang</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLang()
     */
    protected static final Object LANG_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getLang() <em>Lang</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLang()
     */
    protected Object lang = LANG_EDEFAULT;

    /**
     * The default value of the '{@link #getService() <em>Service</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getService()
     */
    protected static final Object SERVICE_EDEFAULT = "TJS";

    /**
     * The cached value of the '{@link #getService() <em>Service</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getService()
     */
    protected Object service = SERVICE_EDEFAULT;

    /**
     * This is true if the Service attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean serviceESet;

    /**
     * The default value of the '{@link #getVersion() <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getVersion()
     */
    protected static final Object VERSION_EDEFAULT = "1.0";

    /**
     * The cached value of the '{@link #getVersion() <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getVersion()
     */
    protected Object version = VERSION_EDEFAULT;

    /**
     * This is true if the Version attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean versionESet;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected DatasetDescriptionsTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getDatasetDescriptionsType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EList getFramework() {
        if (framework == null) {
            framework = new EObjectContainmentEList(FrameworkType4.class, this, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__FRAMEWORK);
        }
        return framework;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getCapabilities() {
        return capabilities;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setCapabilities(String newCapabilities) {
        String oldCapabilities = capabilities;
        capabilities = newCapabilities;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__CAPABILITIES, oldCapabilities, capabilities));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getLang() {
        return lang;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLang(Object newLang) {
        Object oldLang = lang;
        lang = newLang;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__LANG, oldLang, lang));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getService() {
        return service;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setService(Object newService) {
        Object oldService = service;
        service = newService;
        boolean oldServiceESet = serviceESet;
        serviceESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__SERVICE, oldService, service, !oldServiceESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetService() {
        Object oldService = service;
        boolean oldServiceESet = serviceESet;
        service = SERVICE_EDEFAULT;
        serviceESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__SERVICE, oldService, SERVICE_EDEFAULT, oldServiceESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetService() {
        return serviceESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getVersion() {
        return version;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setVersion(Object newVersion) {
        Object oldVersion = version;
        version = newVersion;
        boolean oldVersionESet = versionESet;
        versionESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__VERSION, oldVersion, version, !oldVersionESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetVersion() {
        Object oldVersion = version;
        boolean oldVersionESet = versionESet;
        version = VERSION_EDEFAULT;
        versionESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.DATASET_DESCRIPTIONS_TYPE__VERSION, oldVersion, VERSION_EDEFAULT, oldVersionESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetVersion() {
        return versionESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__FRAMEWORK:
                return ((InternalEList) getFramework()).basicRemove(otherEnd, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__FRAMEWORK:
                return getFramework();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__CAPABILITIES:
                return getCapabilities();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__LANG:
                return getLang();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__SERVICE:
                return getService();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__VERSION:
                return getVersion();
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
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__FRAMEWORK:
                getFramework().clear();
                getFramework().addAll((Collection) newValue);
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__CAPABILITIES:
                setCapabilities((String) newValue);
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__LANG:
                setLang(newValue);
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__SERVICE:
                setService(newValue);
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__VERSION:
                setVersion(newValue);
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
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__FRAMEWORK:
                getFramework().clear();
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__CAPABILITIES:
                setCapabilities(CAPABILITIES_EDEFAULT);
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__LANG:
                setLang(LANG_EDEFAULT);
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__SERVICE:
                unsetService();
                return;
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__VERSION:
                unsetVersion();
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
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__FRAMEWORK:
                return framework != null && !framework.isEmpty();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__CAPABILITIES:
                return CAPABILITIES_EDEFAULT == null ? capabilities != null : !CAPABILITIES_EDEFAULT.equals(capabilities);
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__LANG:
                return LANG_EDEFAULT == null ? lang != null : !LANG_EDEFAULT.equals(lang);
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__SERVICE:
                return isSetService();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE__VERSION:
                return isSetVersion();
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
        result.append(" (capabilities: ");
        result.append(capabilities);
        result.append(", lang: ");
        result.append(lang);
        result.append(", service: ");
        if (serviceESet) result.append(service);
        else result.append("<unset>");
        result.append(", version: ");
        if (versionESet) result.append(version);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //DatasetDescriptionsTypeImpl
