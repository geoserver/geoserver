/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.GetDataType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Get Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getAttributes <em>Attributes</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getLinkageKeys <em>Linkage Keys</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getFilterColumn <em>Filter Column</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getFilterValue <em>Filter Value</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#getXSL <em>XSL</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.GetDataTypeImpl#isAid <em>Aid</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class GetDataTypeImpl extends RequestBaseTypeImpl implements GetDataType {
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
     * The cached value of the '{@link #getFilterColumn() <em>Filter Column</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFilterColumn()
     */
    protected EObject filterColumn;

    /**
     * The cached value of the '{@link #getFilterValue() <em>Filter Value</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFilterValue()
     */
    protected EObject filterValue;

    /**
     * The cached value of the '{@link #getXSL() <em>XSL</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getXSL()
     */
    protected EObject xSL;

    /**
     * The default value of the '{@link #isAid() <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #isAid()
     */
    protected static final boolean AID_EDEFAULT = false;

    /**
     * The cached value of the '{@link #isAid() <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #isAid()
     */
    protected boolean aid = AID_EDEFAULT;

    /**
     * This is true if the Aid attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean aidESet;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected GetDataTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getGetDataType();
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__FRAMEWORK_URI, oldFrameworkURI, frameworkURI));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__DATASET_URI, oldDatasetURI, datasetURI));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__ATTRIBUTES, oldAttributes, attributes));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__LINKAGE_KEYS, oldLinkageKeys, linkageKeys));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getFilterColumn() {
        return filterColumn;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFilterColumn(EObject newFilterColumn, NotificationChain msgs) {
        EObject oldFilterColumn = filterColumn;
        filterColumn = newFilterColumn;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN, oldFilterColumn, newFilterColumn);
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
    public void setFilterColumn(EObject newFilterColumn) {
        if (newFilterColumn != filterColumn) {
            NotificationChain msgs = null;
            if (filterColumn != null)
                msgs = ((InternalEObject) filterColumn).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN, null, msgs);
            if (newFilterColumn != null)
                msgs = ((InternalEObject) newFilterColumn).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN, null, msgs);
            msgs = basicSetFilterColumn(newFilterColumn, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN, newFilterColumn, newFilterColumn));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getFilterValue() {
        return filterValue;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFilterValue(EObject newFilterValue, NotificationChain msgs) {
        EObject oldFilterValue = filterValue;
        filterValue = newFilterValue;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__FILTER_VALUE, oldFilterValue, newFilterValue);
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
    public void setFilterValue(EObject newFilterValue) {
        if (newFilterValue != filterValue) {
            NotificationChain msgs = null;
            if (filterValue != null)
                msgs = ((InternalEObject) filterValue).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_DATA_TYPE__FILTER_VALUE, null, msgs);
            if (newFilterValue != null)
                msgs = ((InternalEObject) newFilterValue).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_DATA_TYPE__FILTER_VALUE, null, msgs);
            msgs = basicSetFilterValue(newFilterValue, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__FILTER_VALUE, newFilterValue, newFilterValue));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getXSL() {
        return xSL;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetXSL(EObject newXSL, NotificationChain msgs) {
        EObject oldXSL = xSL;
        xSL = newXSL;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__XSL, oldXSL, newXSL);
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
    public void setXSL(EObject newXSL) {
        if (newXSL != xSL) {
            NotificationChain msgs = null;
            if (xSL != null)
                msgs = ((InternalEObject) xSL).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_DATA_TYPE__XSL, null, msgs);
            if (newXSL != null)
                msgs = ((InternalEObject) newXSL).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.GET_DATA_TYPE__XSL, null, msgs);
            msgs = basicSetXSL(newXSL, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__XSL, newXSL, newXSL));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isAid() {
        return aid;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAid(boolean newAid) {
        boolean oldAid = aid;
        aid = newAid;
        boolean oldAidESet = aidESet;
        aidESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.GET_DATA_TYPE__AID, oldAid, aid, !oldAidESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetAid() {
        boolean oldAid = aid;
        boolean oldAidESet = aidESet;
        aid = AID_EDEFAULT;
        aidESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.GET_DATA_TYPE__AID, oldAid, AID_EDEFAULT, oldAidESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetAid() {
        return aidESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN:
                return basicSetFilterColumn(null, msgs);
            case Tjs10Package.GET_DATA_TYPE__FILTER_VALUE:
                return basicSetFilterValue(null, msgs);
            case Tjs10Package.GET_DATA_TYPE__XSL:
                return basicSetXSL(null, msgs);
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
            case Tjs10Package.GET_DATA_TYPE__FRAMEWORK_URI:
                return getFrameworkURI();
            case Tjs10Package.GET_DATA_TYPE__DATASET_URI:
                return getDatasetURI();
            case Tjs10Package.GET_DATA_TYPE__ATTRIBUTES:
                return getAttributes();
            case Tjs10Package.GET_DATA_TYPE__LINKAGE_KEYS:
                return getLinkageKeys();
            case Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN:
                return getFilterColumn();
            case Tjs10Package.GET_DATA_TYPE__FILTER_VALUE:
                return getFilterValue();
            case Tjs10Package.GET_DATA_TYPE__XSL:
                return getXSL();
            case Tjs10Package.GET_DATA_TYPE__AID:
                return isAid() ? Boolean.TRUE : Boolean.FALSE;
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
            case Tjs10Package.GET_DATA_TYPE__FRAMEWORK_URI:
                setFrameworkURI((String) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__DATASET_URI:
                setDatasetURI((String) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__ATTRIBUTES:
                setAttributes((String) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__LINKAGE_KEYS:
                setLinkageKeys((String) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN:
                setFilterColumn((EObject) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__FILTER_VALUE:
                setFilterValue((EObject) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__XSL:
                setXSL((EObject) newValue);
                return;
            case Tjs10Package.GET_DATA_TYPE__AID:
                setAid(((Boolean) newValue).booleanValue());
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
            case Tjs10Package.GET_DATA_TYPE__FRAMEWORK_URI:
                setFrameworkURI(FRAMEWORK_URI_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_TYPE__DATASET_URI:
                setDatasetURI(DATASET_URI_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_TYPE__ATTRIBUTES:
                setAttributes(ATTRIBUTES_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_TYPE__LINKAGE_KEYS:
                setLinkageKeys(LINKAGE_KEYS_EDEFAULT);
                return;
            case Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN:
                setFilterColumn((EObject) null);
                return;
            case Tjs10Package.GET_DATA_TYPE__FILTER_VALUE:
                setFilterValue((EObject) null);
                return;
            case Tjs10Package.GET_DATA_TYPE__XSL:
                setXSL((EObject) null);
                return;
            case Tjs10Package.GET_DATA_TYPE__AID:
                unsetAid();
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
            case Tjs10Package.GET_DATA_TYPE__FRAMEWORK_URI:
                return FRAMEWORK_URI_EDEFAULT == null ? frameworkURI != null : !FRAMEWORK_URI_EDEFAULT.equals(frameworkURI);
            case Tjs10Package.GET_DATA_TYPE__DATASET_URI:
                return DATASET_URI_EDEFAULT == null ? datasetURI != null : !DATASET_URI_EDEFAULT.equals(datasetURI);
            case Tjs10Package.GET_DATA_TYPE__ATTRIBUTES:
                return ATTRIBUTES_EDEFAULT == null ? attributes != null : !ATTRIBUTES_EDEFAULT.equals(attributes);
            case Tjs10Package.GET_DATA_TYPE__LINKAGE_KEYS:
                return LINKAGE_KEYS_EDEFAULT == null ? linkageKeys != null : !LINKAGE_KEYS_EDEFAULT.equals(linkageKeys);
            case Tjs10Package.GET_DATA_TYPE__FILTER_COLUMN:
                return filterColumn != null;
            case Tjs10Package.GET_DATA_TYPE__FILTER_VALUE:
                return filterValue != null;
            case Tjs10Package.GET_DATA_TYPE__XSL:
                return xSL != null;
            case Tjs10Package.GET_DATA_TYPE__AID:
                return isSetAid();
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
        result.append(", aid: ");
        if (aidESet) result.append(aid);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //GetDataTypeImpl
