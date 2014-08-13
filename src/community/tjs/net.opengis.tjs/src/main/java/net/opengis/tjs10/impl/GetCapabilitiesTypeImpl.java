/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.ows11.AcceptFormatsType;
import net.opengis.ows11.SectionsType;
import net.opengis.tjs10.AcceptVersionsType;
import net.opengis.tjs10.GetCapabilitiesType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Get Capabilities Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl#getAcceptVersions <em>Accept Versions</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl#getSections <em>Sections</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl#getAcceptFormats <em>Accept Formats</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl#getLanguage <em>Language</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl#getService <em>Service</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl#getUpdateSequence <em>Update Sequence</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GetCapabilitiesTypeImpl extends EObjectImpl implements GetCapabilitiesType {
    /**
     * The cached value of the '{@link #getAcceptVersions() <em>Accept Versions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAcceptVersions()
     */
    protected AcceptVersionsType acceptVersions;

    /**
     * The cached value of the '{@link #getSections() <em>Sections</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getSections()
     */
    protected SectionsType sections;

    /**
     * The cached value of the '{@link #getAcceptFormats() <em>Accept Formats</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAcceptFormats()
     */
    protected AcceptFormatsType acceptFormats;

    /**
     * The default value of the '{@link #getLanguage() <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLanguage()
     */
    protected static final Object LANGUAGE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getLanguage() <em>Language</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLanguage()
     */
    protected Object language = LANGUAGE_EDEFAULT;

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
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected GetCapabilitiesTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getGetCapabilitiesType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AcceptVersionsType getAcceptVersions() {
        return acceptVersions;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAcceptVersions(AcceptVersionsType newAcceptVersions, NotificationChain msgs) {
        AcceptVersionsType oldAcceptVersions = acceptVersions;
        acceptVersions = newAcceptVersions;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS, oldAcceptVersions, newAcceptVersions);
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
    public void setAcceptVersions(AcceptVersionsType newAcceptVersions) {
        if (newAcceptVersions != acceptVersions) {
            NotificationChain msgs = null;
            if (acceptVersions != null)
                msgs = ((InternalEObject) acceptVersions).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS, null, msgs);
            if (newAcceptVersions != null)
                msgs = ((InternalEObject) newAcceptVersions).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS, null, msgs);
            msgs = basicSetAcceptVersions(newAcceptVersions, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS, newAcceptVersions, newAcceptVersions));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public SectionsType getSections() {
        return sections;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetSections(SectionsType newSections, NotificationChain msgs) {
        SectionsType oldSections = sections;
        sections = newSections;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS, oldSections, newSections);
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
    public void setSections(SectionsType newSections) {
        if (newSections != sections) {
            NotificationChain msgs = null;
            if (sections != null)
                msgs = ((InternalEObject) sections).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS, null, msgs);
            if (newSections != null)
                msgs = ((InternalEObject) newSections).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS, null, msgs);
            msgs = basicSetSections(newSections, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS, newSections, newSections));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AcceptFormatsType getAcceptFormats() {
        return acceptFormats;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAcceptFormats(AcceptFormatsType newAcceptFormats, NotificationChain msgs) {
        AcceptFormatsType oldAcceptFormats = acceptFormats;
        acceptFormats = newAcceptFormats;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS, oldAcceptFormats, newAcceptFormats);
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
    public void setAcceptFormats(AcceptFormatsType newAcceptFormats) {
        if (newAcceptFormats != acceptFormats) {
            NotificationChain msgs = null;
            if (acceptFormats != null)
                msgs = ((InternalEObject) acceptFormats).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS, null, msgs);
            if (newAcceptFormats != null)
                msgs = ((InternalEObject) newAcceptFormats).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS, null, msgs);
            msgs = basicSetAcceptFormats(newAcceptFormats, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS, newAcceptFormats, newAcceptFormats));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getLanguage() {
        return language;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLanguage(Object newLanguage) {
        Object oldLanguage = language;
        language = newLanguage;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__LANGUAGE, oldLanguage, language));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__SERVICE, oldService, service, !oldServiceESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.GET_CAPABILITIES_TYPE__SERVICE, oldService, SERVICE_EDEFAULT, oldServiceESet));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE, oldUpdateSequence, updateSequence));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS:
                return basicSetAcceptVersions(null, msgs);
            case Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS:
                return basicSetSections(null, msgs);
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS:
                return basicSetAcceptFormats(null, msgs);
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
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS:
                return getAcceptVersions();
            case Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS:
                return getSections();
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS:
                return getAcceptFormats();
            case Tjs10Package.GET_CAPABILITIES_TYPE__LANGUAGE:
                return getLanguage();
            case Tjs10Package.GET_CAPABILITIES_TYPE__SERVICE:
                return getService();
            case Tjs10Package.GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                return getUpdateSequence();
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
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS:
                setAcceptVersions((AcceptVersionsType) newValue);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS:
                setSections((SectionsType) newValue);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS:
                setAcceptFormats((AcceptFormatsType) newValue);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__LANGUAGE:
                setLanguage(newValue);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__SERVICE:
                setService(newValue);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                setUpdateSequence((String) newValue);
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
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS:
                setAcceptVersions((AcceptVersionsType) null);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS:
                setSections((SectionsType) null);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS:
                setAcceptFormats((AcceptFormatsType) null);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__LANGUAGE:
                setLanguage(LANGUAGE_EDEFAULT);
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__SERVICE:
                unsetService();
                return;
            case Tjs10Package.GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                setUpdateSequence(UPDATE_SEQUENCE_EDEFAULT);
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
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS:
                return acceptVersions != null;
            case Tjs10Package.GET_CAPABILITIES_TYPE__SECTIONS:
                return sections != null;
            case Tjs10Package.GET_CAPABILITIES_TYPE__ACCEPT_FORMATS:
                return acceptFormats != null;
            case Tjs10Package.GET_CAPABILITIES_TYPE__LANGUAGE:
                return LANGUAGE_EDEFAULT == null ? language != null : !LANGUAGE_EDEFAULT.equals(language);
            case Tjs10Package.GET_CAPABILITIES_TYPE__SERVICE:
                return isSetService();
            case Tjs10Package.GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE:
                return UPDATE_SEQUENCE_EDEFAULT == null ? updateSequence != null : !UPDATE_SEQUENCE_EDEFAULT.equals(updateSequence);
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
        result.append(" (language: ");
        result.append(language);
        result.append(", service: ");
        if (serviceESet) result.append(service);
        else result.append("<unset>");
        result.append(", updateSequence: ");
        result.append(updateSequence);
        result.append(')');
        return result.toString();
    }

} //GetCapabilitiesTypeImpl
