/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.ows11.OperationsMetadataType;
import net.opengis.ows11.ServiceIdentificationType;
import net.opengis.ows11.ServiceProviderType;
import net.opengis.tjs10.LanguagesType;
import net.opengis.tjs10.Tjs10Package;
import net.opengis.tjs10.TjsCapabilitiesType;
import net.opengis.tjs10.WSDLType;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tjs Capabilities Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getServiceIdentification <em>Service Identification</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getServiceProvider <em>Service Provider</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getOperationsMetadata <em>Operations Metadata</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getLanguages <em>Languages</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getWSDL <em>WSDL</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getUpdateSequence <em>Update Sequence</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class TjsCapabilitiesTypeImpl extends EObjectImpl implements TjsCapabilitiesType {
    /**
     * The cached value of the '{@link #getServiceIdentification() <em>Service Identification</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getServiceIdentification()
     */
    protected ServiceIdentificationType serviceIdentification;

    /**
     * The cached value of the '{@link #getServiceProvider() <em>Service Provider</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getServiceProvider()
     */
    protected ServiceProviderType serviceProvider;

    /**
     * The cached value of the '{@link #getOperationsMetadata() <em>Operations Metadata</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getOperationsMetadata()
     */
    protected OperationsMetadataType operationsMetadata;

    /**
     * The cached value of the '{@link #getLanguages() <em>Languages</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLanguages()
     */
    protected LanguagesType languages;

    /**
     * The cached value of the '{@link #getWSDL() <em>WSDL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getWSDL()
     */
    protected WSDLType wSDL;

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
     * The default value of the '{@link #getUpdateSequence() <em>Update Sequence</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getUpdateSequence()
     */
    protected static final String UPDATE_SEQUENCE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getUpdateSequence() <em>Update Sequence</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getUpdateSequence()
     */
    protected String updateSequence = UPDATE_SEQUENCE_EDEFAULT;

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
    protected TjsCapabilitiesTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getTjsCapabilitiesType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ServiceIdentificationType getServiceIdentification() {
        return serviceIdentification;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetServiceIdentification(ServiceIdentificationType newServiceIdentification, NotificationChain msgs) {
        ServiceIdentificationType oldServiceIdentification = serviceIdentification;
        serviceIdentification = newServiceIdentification;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION, oldServiceIdentification, newServiceIdentification);
            if (msgs == null) msgs = notification;
            else msgs.add(notification);
        }
        return msgs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setServiceIdentification(ServiceIdentificationType newServiceIdentification) {
        if (newServiceIdentification != serviceIdentification) {
            NotificationChain msgs = null;
            if (serviceIdentification != null)
                msgs = ((InternalEObject) serviceIdentification).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION, null, msgs);
            if (newServiceIdentification != null)
                msgs = ((InternalEObject) newServiceIdentification).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION, null, msgs);
            msgs = basicSetServiceIdentification(newServiceIdentification, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION, newServiceIdentification, newServiceIdentification));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ServiceProviderType getServiceProvider() {
        return serviceProvider;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetServiceProvider(ServiceProviderType newServiceProvider, NotificationChain msgs) {
        ServiceProviderType oldServiceProvider = serviceProvider;
        serviceProvider = newServiceProvider;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER, oldServiceProvider, newServiceProvider);
            if (msgs == null) msgs = notification;
            else msgs.add(notification);
        }
        return msgs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setServiceProvider(ServiceProviderType newServiceProvider) {
        if (newServiceProvider != serviceProvider) {
            NotificationChain msgs = null;
            if (serviceProvider != null)
                msgs = ((InternalEObject) serviceProvider).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER, null, msgs);
            if (newServiceProvider != null)
                msgs = ((InternalEObject) newServiceProvider).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER, null, msgs);
            msgs = basicSetServiceProvider(newServiceProvider, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER, newServiceProvider, newServiceProvider));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OperationsMetadataType getOperationsMetadata() {
        return operationsMetadata;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetOperationsMetadata(OperationsMetadataType newOperationsMetadata, NotificationChain msgs) {
        OperationsMetadataType oldOperationsMetadata = operationsMetadata;
        operationsMetadata = newOperationsMetadata;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA, oldOperationsMetadata, newOperationsMetadata);
            if (msgs == null) msgs = notification;
            else msgs.add(notification);
        }
        return msgs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setOperationsMetadata(OperationsMetadataType newOperationsMetadata) {
        if (newOperationsMetadata != operationsMetadata) {
            NotificationChain msgs = null;
            if (operationsMetadata != null)
                msgs = ((InternalEObject) operationsMetadata).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA, null, msgs);
            if (newOperationsMetadata != null)
                msgs = ((InternalEObject) newOperationsMetadata).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA, null, msgs);
            msgs = basicSetOperationsMetadata(newOperationsMetadata, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA, newOperationsMetadata, newOperationsMetadata));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public LanguagesType getLanguages() {
        return languages;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetLanguages(LanguagesType newLanguages, NotificationChain msgs) {
        LanguagesType oldLanguages = languages;
        languages = newLanguages;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES, oldLanguages, newLanguages);
            if (msgs == null) msgs = notification;
            else msgs.add(notification);
        }
        return msgs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLanguages(LanguagesType newLanguages) {
        if (newLanguages != languages) {
            NotificationChain msgs = null;
            if (languages != null)
                msgs = ((InternalEObject) languages).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES, null, msgs);
            if (newLanguages != null)
                msgs = ((InternalEObject) newLanguages).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES, null, msgs);
            msgs = basicSetLanguages(newLanguages, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES, newLanguages, newLanguages));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public WSDLType getWSDL() {
        return wSDL;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetWSDL(WSDLType newWSDL, NotificationChain msgs) {
        WSDLType oldWSDL = wSDL;
        wSDL = newWSDL;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL, oldWSDL, newWSDL);
            if (msgs == null) msgs = notification;
            else msgs.add(notification);
        }
        return msgs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setWSDL(WSDLType newWSDL) {
        if (newWSDL != wSDL) {
            NotificationChain msgs = null;
            if (wSDL != null)
                msgs = ((InternalEObject) wSDL).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL, null, msgs);
            if (newWSDL != null)
                msgs = ((InternalEObject) newWSDL).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL, null, msgs);
            msgs = basicSetWSDL(newWSDL, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL, newWSDL, newWSDL));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__LANG, oldLang, lang));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE, oldService, service, !oldServiceESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE, oldService, SERVICE_EDEFAULT, oldServiceESet));
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
    public String getUpdateSequence() {
        return updateSequence;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setUpdateSequence(String newUpdateSequence) {
        String oldUpdateSequence = updateSequence;
        updateSequence = newUpdateSequence;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE, oldUpdateSequence, updateSequence));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.TJS_CAPABILITIES_TYPE__VERSION, oldVersion, version, !oldVersionESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.TJS_CAPABILITIES_TYPE__VERSION, oldVersion, VERSION_EDEFAULT, oldVersionESet));
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
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION:
                return basicSetServiceIdentification(null, msgs);
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER:
                return basicSetServiceProvider(null, msgs);
            case Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA:
                return basicSetOperationsMetadata(null, msgs);
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES:
                return basicSetLanguages(null, msgs);
            case Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL:
                return basicSetWSDL(null, msgs);
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
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION:
                return getServiceIdentification();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER:
                return getServiceProvider();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA:
                return getOperationsMetadata();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES:
                return getLanguages();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL:
                return getWSDL();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANG:
                return getLang();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE:
                return getService();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                return getUpdateSequence();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__VERSION:
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
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION:
                setServiceIdentification((ServiceIdentificationType) newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER:
                setServiceProvider((ServiceProviderType) newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA:
                setOperationsMetadata((OperationsMetadataType) newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES:
                setLanguages((LanguagesType) newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL:
                setWSDL((WSDLType) newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANG:
                setLang(newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE:
                setService(newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                setUpdateSequence((String) newValue);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__VERSION:
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
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION:
                setServiceIdentification((ServiceIdentificationType) null);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER:
                setServiceProvider((ServiceProviderType) null);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA:
                setOperationsMetadata((OperationsMetadataType) null);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES:
                setLanguages((LanguagesType) null);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL:
                setWSDL((WSDLType) null);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANG:
                setLang(LANG_EDEFAULT);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE:
                unsetService();
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                setUpdateSequence(UPDATE_SEQUENCE_EDEFAULT);
                return;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__VERSION:
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
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION:
                return serviceIdentification != null;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER:
                return serviceProvider != null;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA:
                return operationsMetadata != null;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANGUAGES:
                return languages != null;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__WSDL:
                return wSDL != null;
            case Tjs10Package.TJS_CAPABILITIES_TYPE__LANG:
                return LANG_EDEFAULT == null ? lang != null : !LANG_EDEFAULT.equals(lang);
            case Tjs10Package.TJS_CAPABILITIES_TYPE__SERVICE:
                return isSetService();
            case Tjs10Package.TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                return UPDATE_SEQUENCE_EDEFAULT == null ? updateSequence != null : !UPDATE_SEQUENCE_EDEFAULT.equals(updateSequence);
            case Tjs10Package.TJS_CAPABILITIES_TYPE__VERSION:
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
        result.append(" (lang: ");
        result.append(lang);
        result.append(", service: ");
        if (serviceESet) result.append(service);
        else result.append("<unset>");
        result.append(", updateSequence: ");
        result.append(updateSequence);
        result.append(", version: ");
        if (versionESet) result.append(version);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //TjsCapabilitiesTypeImpl
