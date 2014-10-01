/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.*;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Join Data Response Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getStatus <em>Status</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getDataInputs <em>Data Inputs</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getJoinedOutputs <em>Joined Outputs</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class JoinDataResponseTypeImpl extends EObjectImpl implements JoinDataResponseType {
    /**
     * The cached value of the '{@link #getStatus() <em>Status</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getStatus()
     */
    protected StatusType status;

    /**
     * The cached value of the '{@link #getDataInputs() <em>Data Inputs</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDataInputs()
     */
    protected DataInputsType dataInputs;

    /**
     * The cached value of the '{@link #getJoinedOutputs() <em>Joined Outputs</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getJoinedOutputs()
     */
    protected JoinedOutputsType joinedOutputs;

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
    protected JoinDataResponseTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getJoinDataResponseType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public StatusType getStatus() {
        return status;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetStatus(StatusType newStatus, NotificationChain msgs) {
        StatusType oldStatus = status;
        status = newStatus;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS, oldStatus, newStatus);
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
    public void setStatus(StatusType newStatus) {
        if (newStatus != status) {
            NotificationChain msgs = null;
            if (status != null)
                msgs = ((InternalEObject) status).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS, null, msgs);
            if (newStatus != null)
                msgs = ((InternalEObject) newStatus).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS, null, msgs);
            msgs = basicSetStatus(newStatus, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS, newStatus, newStatus));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataInputsType getDataInputs() {
        return dataInputs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDataInputs(DataInputsType newDataInputs, NotificationChain msgs) {
        DataInputsType oldDataInputs = dataInputs;
        dataInputs = newDataInputs;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS, oldDataInputs, newDataInputs);
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
    public void setDataInputs(DataInputsType newDataInputs) {
        if (newDataInputs != dataInputs) {
            NotificationChain msgs = null;
            if (dataInputs != null)
                msgs = ((InternalEObject) dataInputs).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS, null, msgs);
            if (newDataInputs != null)
                msgs = ((InternalEObject) newDataInputs).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS, null, msgs);
            msgs = basicSetDataInputs(newDataInputs, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS, newDataInputs, newDataInputs));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinedOutputsType getJoinedOutputs() {
        return joinedOutputs;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetJoinedOutputs(JoinedOutputsType newJoinedOutputs, NotificationChain msgs) {
        JoinedOutputsType oldJoinedOutputs = joinedOutputs;
        joinedOutputs = newJoinedOutputs;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS, oldJoinedOutputs, newJoinedOutputs);
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
    public void setJoinedOutputs(JoinedOutputsType newJoinedOutputs) {
        if (newJoinedOutputs != joinedOutputs) {
            NotificationChain msgs = null;
            if (joinedOutputs != null)
                msgs = ((InternalEObject) joinedOutputs).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS, null, msgs);
            if (newJoinedOutputs != null)
                msgs = ((InternalEObject) newJoinedOutputs).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS, null, msgs);
            msgs = basicSetJoinedOutputs(newJoinedOutputs, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS, newJoinedOutputs, newJoinedOutputs));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__CAPABILITIES, oldCapabilities, capabilities));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__LANG, oldLang, lang));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__SERVICE, oldService, service, !oldServiceESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__SERVICE, oldService, SERVICE_EDEFAULT, oldServiceESet));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__VERSION, oldVersion, version, !oldVersionESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.JOIN_DATA_RESPONSE_TYPE__VERSION, oldVersion, VERSION_EDEFAULT, oldVersionESet));
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
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS:
                return basicSetStatus(null, msgs);
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS:
                return basicSetDataInputs(null, msgs);
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS:
                return basicSetJoinedOutputs(null, msgs);
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
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS:
                return getStatus();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS:
                return getDataInputs();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS:
                return getJoinedOutputs();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__CAPABILITIES:
                return getCapabilities();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__LANG:
                return getLang();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__SERVICE:
                return getService();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__VERSION:
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
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS:
                setStatus((StatusType) newValue);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS:
                setDataInputs((DataInputsType) newValue);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS:
                setJoinedOutputs((JoinedOutputsType) newValue);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__CAPABILITIES:
                setCapabilities((String) newValue);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__LANG:
                setLang(newValue);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__SERVICE:
                setService(newValue);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__VERSION:
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
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS:
                setStatus((StatusType) null);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS:
                setDataInputs((DataInputsType) null);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS:
                setJoinedOutputs((JoinedOutputsType) null);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__CAPABILITIES:
                setCapabilities(CAPABILITIES_EDEFAULT);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__LANG:
                setLang(LANG_EDEFAULT);
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__SERVICE:
                unsetService();
                return;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__VERSION:
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
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__STATUS:
                return status != null;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS:
                return dataInputs != null;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS:
                return joinedOutputs != null;
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__CAPABILITIES:
                return CAPABILITIES_EDEFAULT == null ? capabilities != null : !CAPABILITIES_EDEFAULT.equals(capabilities);
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__LANG:
                return LANG_EDEFAULT == null ? lang != null : !LANG_EDEFAULT.equals(lang);
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__SERVICE:
                return isSetService();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE__VERSION:
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

} //JoinDataResponseTypeImpl
