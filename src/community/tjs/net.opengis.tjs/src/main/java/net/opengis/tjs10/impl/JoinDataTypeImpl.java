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

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Join Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.JoinDataTypeImpl#getAttributeData <em>Attribute Data</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataTypeImpl#getMapStyling <em>Map Styling</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataTypeImpl#getClassificationURL <em>Classification URL</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.JoinDataTypeImpl#getUpdate <em>Update</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class JoinDataTypeImpl extends RequestBaseTypeImpl implements JoinDataType {
    /**
     * The cached value of the '{@link #getAttributeData() <em>Attribute Data</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributeData()
     */
    protected AttributeDataType attributeData;

    /**
     * The cached value of the '{@link #getMapStyling() <em>Map Styling</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getMapStyling()
     */
    protected MapStylingType mapStyling;

    /**
     * The cached value of the '{@link #getClassificationURL() <em>Classification URL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getClassificationURL()
     */
    protected EObject classificationURL;

    /**
     * The default value of the '{@link #getUpdate() <em>Update</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getUpdate()
     */
    protected static final UpdateType UPDATE_EDEFAULT = UpdateType.TRUE_LITERAL;

    /**
     * The cached value of the '{@link #getUpdate() <em>Update</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getUpdate()
     */
    protected UpdateType update = UPDATE_EDEFAULT;

    /**
     * This is true if the Update attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean updateESet;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected JoinDataTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getJoinDataType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AttributeDataType getAttributeData() {
        return attributeData;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAttributeData(AttributeDataType newAttributeData, NotificationChain msgs) {
        AttributeDataType oldAttributeData = attributeData;
        attributeData = newAttributeData;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA, oldAttributeData, newAttributeData);
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
    public void setAttributeData(AttributeDataType newAttributeData) {
        if (newAttributeData != attributeData) {
            NotificationChain msgs = null;
            if (attributeData != null)
                msgs = ((InternalEObject) attributeData).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA, null, msgs);
            if (newAttributeData != null)
                msgs = ((InternalEObject) newAttributeData).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA, null, msgs);
            msgs = basicSetAttributeData(newAttributeData, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA, newAttributeData, newAttributeData));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MapStylingType getMapStyling() {
        return mapStyling;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetMapStyling(MapStylingType newMapStyling, NotificationChain msgs) {
        MapStylingType oldMapStyling = mapStyling;
        mapStyling = newMapStyling;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING, oldMapStyling, newMapStyling);
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
    public void setMapStyling(MapStylingType newMapStyling) {
        if (newMapStyling != mapStyling) {
            NotificationChain msgs = null;
            if (mapStyling != null)
                msgs = ((InternalEObject) mapStyling).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING, null, msgs);
            if (newMapStyling != null)
                msgs = ((InternalEObject) newMapStyling).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING, null, msgs);
            msgs = basicSetMapStyling(newMapStyling, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING, newMapStyling, newMapStyling));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getClassificationURL() {
        return classificationURL;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetClassificationURL(EObject newClassificationURL, NotificationChain msgs) {
        EObject oldClassificationURL = classificationURL;
        classificationURL = newClassificationURL;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL, oldClassificationURL, newClassificationURL);
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
    public void setClassificationURL(EObject newClassificationURL) {
        if (newClassificationURL != classificationURL) {
            NotificationChain msgs = null;
            if (classificationURL != null)
                msgs = ((InternalEObject) classificationURL).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL, null, msgs);
            if (newClassificationURL != null)
                msgs = ((InternalEObject) newClassificationURL).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL, null, msgs);
            msgs = basicSetClassificationURL(newClassificationURL, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL, newClassificationURL, newClassificationURL));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UpdateType getUpdate() {
        return update;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setUpdate(UpdateType newUpdate) {
        UpdateType oldUpdate = update;
        update = newUpdate == null ? UPDATE_EDEFAULT : newUpdate;
        boolean oldUpdateESet = updateESet;
        updateESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.JOIN_DATA_TYPE__UPDATE, oldUpdate, update, !oldUpdateESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetUpdate() {
        UpdateType oldUpdate = update;
        boolean oldUpdateESet = updateESet;
        update = UPDATE_EDEFAULT;
        updateESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.JOIN_DATA_TYPE__UPDATE, oldUpdate, UPDATE_EDEFAULT, oldUpdateESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetUpdate() {
        return updateESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA:
                return basicSetAttributeData(null, msgs);
            case Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING:
                return basicSetMapStyling(null, msgs);
            case Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL:
                return basicSetClassificationURL(null, msgs);
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
            case Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA:
                return getAttributeData();
            case Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING:
                return getMapStyling();
            case Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL:
                return getClassificationURL();
            case Tjs10Package.JOIN_DATA_TYPE__UPDATE:
                return getUpdate();
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
            case Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA:
                setAttributeData((AttributeDataType) newValue);
                return;
            case Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING:
                setMapStyling((MapStylingType) newValue);
                return;
            case Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL:
                setClassificationURL((EObject) newValue);
                return;
            case Tjs10Package.JOIN_DATA_TYPE__UPDATE:
                setUpdate((UpdateType) newValue);
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
            case Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA:
                setAttributeData((AttributeDataType) null);
                return;
            case Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING:
                setMapStyling((MapStylingType) null);
                return;
            case Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL:
                setClassificationURL((EObject) null);
                return;
            case Tjs10Package.JOIN_DATA_TYPE__UPDATE:
                unsetUpdate();
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
            case Tjs10Package.JOIN_DATA_TYPE__ATTRIBUTE_DATA:
                return attributeData != null;
            case Tjs10Package.JOIN_DATA_TYPE__MAP_STYLING:
                return mapStyling != null;
            case Tjs10Package.JOIN_DATA_TYPE__CLASSIFICATION_URL:
                return classificationURL != null;
            case Tjs10Package.JOIN_DATA_TYPE__UPDATE:
                return isSetUpdate();
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
        result.append(" (update: ");
        if (updateESet) result.append(update);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //JoinDataTypeImpl
