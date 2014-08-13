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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Join Abilities Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getSpatialFrameworks <em>Spatial Frameworks</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getAttributeLimit <em>Attribute Limit</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getOutputMechanisms <em>Output Mechanisms</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getOutputStylings <em>Output Stylings</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getClassificationSchemaURL <em>Classification Schema URL</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getLang <em>Lang</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#isUpdateSupported <em>Update Supported</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class JoinAbilitiesTypeImpl extends EObjectImpl implements JoinAbilitiesType {
    /**
     * The cached value of the '{@link #getSpatialFrameworks() <em>Spatial Frameworks</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getSpatialFrameworks()
     */
    protected SpatialFrameworksType spatialFrameworks;

    /**
     * The default value of the '{@link #getAttributeLimit() <em>Attribute Limit</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributeLimit()
     */
    protected static final BigInteger ATTRIBUTE_LIMIT_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getAttributeLimit() <em>Attribute Limit</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributeLimit()
     */
    protected BigInteger attributeLimit = ATTRIBUTE_LIMIT_EDEFAULT;

    /**
     * The cached value of the '{@link #getOutputMechanisms() <em>Output Mechanisms</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getOutputMechanisms()
     */
    protected OutputMechanismsType outputMechanisms;

    /**
     * The cached value of the '{@link #getOutputStylings() <em>Output Stylings</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getOutputStylings()
     */
    protected OutputStylingsType1 outputStylings;

    /**
     * The cached value of the '{@link #getClassificationSchemaURL() <em>Classification Schema URL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getClassificationSchemaURL()
     */
    protected EObject classificationSchemaURL;

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
     * The default value of the '{@link #isUpdateSupported() <em>Update Supported</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #isUpdateSupported()
     */
    protected static final boolean UPDATE_SUPPORTED_EDEFAULT = false;

    /**
     * The cached value of the '{@link #isUpdateSupported() <em>Update Supported</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #isUpdateSupported()
     */
    protected boolean updateSupported = UPDATE_SUPPORTED_EDEFAULT;

    /**
     * This is true if the Update Supported attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean updateSupportedESet;

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
    protected JoinAbilitiesTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getJoinAbilitiesType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public SpatialFrameworksType getSpatialFrameworks() {
        return spatialFrameworks;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetSpatialFrameworks(SpatialFrameworksType newSpatialFrameworks, NotificationChain msgs) {
        SpatialFrameworksType oldSpatialFrameworks = spatialFrameworks;
        spatialFrameworks = newSpatialFrameworks;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS, oldSpatialFrameworks, newSpatialFrameworks);
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
    public void setSpatialFrameworks(SpatialFrameworksType newSpatialFrameworks) {
        if (newSpatialFrameworks != spatialFrameworks) {
            NotificationChain msgs = null;
            if (spatialFrameworks != null)
                msgs = ((InternalEObject) spatialFrameworks).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS, null, msgs);
            if (newSpatialFrameworks != null)
                msgs = ((InternalEObject) newSpatialFrameworks).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS, null, msgs);
            msgs = basicSetSpatialFrameworks(newSpatialFrameworks, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS, newSpatialFrameworks, newSpatialFrameworks));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigInteger getAttributeLimit() {
        return attributeLimit;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAttributeLimit(BigInteger newAttributeLimit) {
        BigInteger oldAttributeLimit = attributeLimit;
        attributeLimit = newAttributeLimit;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT, oldAttributeLimit, attributeLimit));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OutputMechanismsType getOutputMechanisms() {
        return outputMechanisms;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetOutputMechanisms(OutputMechanismsType newOutputMechanisms, NotificationChain msgs) {
        OutputMechanismsType oldOutputMechanisms = outputMechanisms;
        outputMechanisms = newOutputMechanisms;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS, oldOutputMechanisms, newOutputMechanisms);
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
    public void setOutputMechanisms(OutputMechanismsType newOutputMechanisms) {
        if (newOutputMechanisms != outputMechanisms) {
            NotificationChain msgs = null;
            if (outputMechanisms != null)
                msgs = ((InternalEObject) outputMechanisms).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS, null, msgs);
            if (newOutputMechanisms != null)
                msgs = ((InternalEObject) newOutputMechanisms).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS, null, msgs);
            msgs = basicSetOutputMechanisms(newOutputMechanisms, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS, newOutputMechanisms, newOutputMechanisms));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OutputStylingsType1 getOutputStylings() {
        return outputStylings;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetOutputStylings(OutputStylingsType1 newOutputStylings, NotificationChain msgs) {
        OutputStylingsType1 oldOutputStylings = outputStylings;
        outputStylings = newOutputStylings;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS, oldOutputStylings, newOutputStylings);
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
    public void setOutputStylings(OutputStylingsType1 newOutputStylings) {
        if (newOutputStylings != outputStylings) {
            NotificationChain msgs = null;
            if (outputStylings != null)
                msgs = ((InternalEObject) outputStylings).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS, null, msgs);
            if (newOutputStylings != null)
                msgs = ((InternalEObject) newOutputStylings).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS, null, msgs);
            msgs = basicSetOutputStylings(newOutputStylings, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS, newOutputStylings, newOutputStylings));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getClassificationSchemaURL() {
        return classificationSchemaURL;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetClassificationSchemaURL(EObject newClassificationSchemaURL, NotificationChain msgs) {
        EObject oldClassificationSchemaURL = classificationSchemaURL;
        classificationSchemaURL = newClassificationSchemaURL;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL, oldClassificationSchemaURL, newClassificationSchemaURL);
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
    public void setClassificationSchemaURL(EObject newClassificationSchemaURL) {
        if (newClassificationSchemaURL != classificationSchemaURL) {
            NotificationChain msgs = null;
            if (classificationSchemaURL != null)
                msgs = ((InternalEObject) classificationSchemaURL).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL, null, msgs);
            if (newClassificationSchemaURL != null)
                msgs = ((InternalEObject) newClassificationSchemaURL).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL, null, msgs);
            msgs = basicSetClassificationSchemaURL(newClassificationSchemaURL, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL, newClassificationSchemaURL, newClassificationSchemaURL));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__CAPABILITIES, oldCapabilities, capabilities));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__LANG, oldLang, lang));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__SERVICE, oldService, service, !oldServiceESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.JOIN_ABILITIES_TYPE__SERVICE, oldService, SERVICE_EDEFAULT, oldServiceESet));
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
    public boolean isUpdateSupported() {
        return updateSupported;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setUpdateSupported(boolean newUpdateSupported) {
        boolean oldUpdateSupported = updateSupported;
        updateSupported = newUpdateSupported;
        boolean oldUpdateSupportedESet = updateSupportedESet;
        updateSupportedESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED, oldUpdateSupported, updateSupported, !oldUpdateSupportedESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetUpdateSupported() {
        boolean oldUpdateSupported = updateSupported;
        boolean oldUpdateSupportedESet = updateSupportedESet;
        updateSupported = UPDATE_SUPPORTED_EDEFAULT;
        updateSupportedESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED, oldUpdateSupported, UPDATE_SUPPORTED_EDEFAULT, oldUpdateSupportedESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetUpdateSupported() {
        return updateSupportedESet;
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_ABILITIES_TYPE__VERSION, oldVersion, version, !oldVersionESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.JOIN_ABILITIES_TYPE__VERSION, oldVersion, VERSION_EDEFAULT, oldVersionESet));
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
            case Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS:
                return basicSetSpatialFrameworks(null, msgs);
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS:
                return basicSetOutputMechanisms(null, msgs);
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS:
                return basicSetOutputStylings(null, msgs);
            case Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL:
                return basicSetClassificationSchemaURL(null, msgs);
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
            case Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS:
                return getSpatialFrameworks();
            case Tjs10Package.JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT:
                return getAttributeLimit();
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS:
                return getOutputMechanisms();
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS:
                return getOutputStylings();
            case Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL:
                return getClassificationSchemaURL();
            case Tjs10Package.JOIN_ABILITIES_TYPE__CAPABILITIES:
                return getCapabilities();
            case Tjs10Package.JOIN_ABILITIES_TYPE__LANG:
                return getLang();
            case Tjs10Package.JOIN_ABILITIES_TYPE__SERVICE:
                return getService();
            case Tjs10Package.JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED:
                return isUpdateSupported() ? Boolean.TRUE : Boolean.FALSE;
            case Tjs10Package.JOIN_ABILITIES_TYPE__VERSION:
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
            case Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS:
                setSpatialFrameworks((SpatialFrameworksType) newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT:
                setAttributeLimit((BigInteger) newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS:
                setOutputMechanisms((OutputMechanismsType) newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS:
                setOutputStylings((OutputStylingsType1) newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL:
                setClassificationSchemaURL((EObject) newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__CAPABILITIES:
                setCapabilities((String) newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__LANG:
                setLang(newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__SERVICE:
                setService(newValue);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED:
                setUpdateSupported(((Boolean) newValue).booleanValue());
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__VERSION:
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
            case Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS:
                setSpatialFrameworks((SpatialFrameworksType) null);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT:
                setAttributeLimit(ATTRIBUTE_LIMIT_EDEFAULT);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS:
                setOutputMechanisms((OutputMechanismsType) null);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS:
                setOutputStylings((OutputStylingsType1) null);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL:
                setClassificationSchemaURL((EObject) null);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__CAPABILITIES:
                setCapabilities(CAPABILITIES_EDEFAULT);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__LANG:
                setLang(LANG_EDEFAULT);
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__SERVICE:
                unsetService();
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED:
                unsetUpdateSupported();
                return;
            case Tjs10Package.JOIN_ABILITIES_TYPE__VERSION:
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
            case Tjs10Package.JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS:
                return spatialFrameworks != null;
            case Tjs10Package.JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT:
                return ATTRIBUTE_LIMIT_EDEFAULT == null ? attributeLimit != null : !ATTRIBUTE_LIMIT_EDEFAULT.equals(attributeLimit);
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS:
                return outputMechanisms != null;
            case Tjs10Package.JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS:
                return outputStylings != null;
            case Tjs10Package.JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL:
                return classificationSchemaURL != null;
            case Tjs10Package.JOIN_ABILITIES_TYPE__CAPABILITIES:
                return CAPABILITIES_EDEFAULT == null ? capabilities != null : !CAPABILITIES_EDEFAULT.equals(capabilities);
            case Tjs10Package.JOIN_ABILITIES_TYPE__LANG:
                return LANG_EDEFAULT == null ? lang != null : !LANG_EDEFAULT.equals(lang);
            case Tjs10Package.JOIN_ABILITIES_TYPE__SERVICE:
                return isSetService();
            case Tjs10Package.JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED:
                return isSetUpdateSupported();
            case Tjs10Package.JOIN_ABILITIES_TYPE__VERSION:
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
        result.append(" (attributeLimit: ");
        result.append(attributeLimit);
        result.append(", capabilities: ");
        result.append(capabilities);
        result.append(", lang: ");
        result.append(lang);
        result.append(", service: ");
        if (serviceESet) result.append(service);
        else result.append("<unset>");
        result.append(", updateSupported: ");
        if (updateSupportedESet) result.append(updateSupported);
        else result.append("<unset>");
        result.append(", version: ");
        if (versionESet) result.append(version);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //JoinAbilitiesTypeImpl
