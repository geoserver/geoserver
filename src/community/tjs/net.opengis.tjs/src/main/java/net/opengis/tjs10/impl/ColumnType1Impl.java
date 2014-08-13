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

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Column Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getValues <em>Values</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getGetDataRequest <em>Get Data Request</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getDecimals <em>Decimals</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getLength <em>Length</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getName <em>Name</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getPurpose <em>Purpose</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType1Impl#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ColumnType1Impl extends EObjectImpl implements ColumnType1 {
    /**
     * The default value of the '{@link #getTitle() <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getTitle()
     */
    protected static final String TITLE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getTitle() <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getTitle()
     */
    protected String title = TITLE_EDEFAULT;

    /**
     * The cached value of the '{@link #getAbstract() <em>Abstract</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAbstract()
     */
    protected AbstractType abstract_;

    /**
     * The default value of the '{@link #getDocumentation() <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDocumentation()
     */
    protected static final String DOCUMENTATION_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getDocumentation() <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDocumentation()
     */
    protected String documentation = DOCUMENTATION_EDEFAULT;

    /**
     * The cached value of the '{@link #getValues() <em>Values</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getValues()
     */
    protected ValuesType values;

    /**
     * The cached value of the '{@link #getGetDataRequest() <em>Get Data Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGetDataRequest()
     */
    protected GetDataRequestType getDataRequest;

    /**
     * The default value of the '{@link #getDecimals() <em>Decimals</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDecimals()
     */
    protected static final BigInteger DECIMALS_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getDecimals() <em>Decimals</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDecimals()
     */
    protected BigInteger decimals = DECIMALS_EDEFAULT;

    /**
     * The default value of the '{@link #getLength() <em>Length</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLength()
     */
    protected static final BigInteger LENGTH_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getLength() <em>Length</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLength()
     */
    protected BigInteger length = LENGTH_EDEFAULT;

    /**
     * The default value of the '{@link #getName() <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getName()
     */
    protected static final String NAME_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getName()
     */
    protected String name = NAME_EDEFAULT;

    /**
     * The default value of the '{@link #getPurpose() <em>Purpose</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getPurpose()
     */
    protected static final PurposeType PURPOSE_EDEFAULT = PurposeType.SPATIAL_COMPONENT_IDENTIFIER_LITERAL;

    /**
     * The cached value of the '{@link #getPurpose() <em>Purpose</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getPurpose()
     */
    protected PurposeType purpose = PURPOSE_EDEFAULT;

    /**
     * This is true if the Purpose attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean purposeESet;

    /**
     * The default value of the '{@link #getType() <em>Type</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getType()
     */
    protected static final TypeType TYPE_EDEFAULT = TypeType.HTTP_WWW_W3_ORG_TR_XMLSCHEMA2_STRING_LITERAL;

    /**
     * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getType()
     */
    protected TypeType type = TYPE_EDEFAULT;

    /**
     * This is true if the Type attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean typeESet;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected ColumnType1Impl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getColumnType1();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getTitle() {
        return title;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setTitle(String newTitle) {
        String oldTitle = title;
        title = newTitle;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__TITLE, oldTitle, title));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AbstractType getAbstract() {
        return abstract_;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAbstract(AbstractType newAbstract, NotificationChain msgs) {
        AbstractType oldAbstract = abstract_;
        abstract_ = newAbstract;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__ABSTRACT, oldAbstract, newAbstract);
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
    public void setAbstract(AbstractType newAbstract) {
        if (newAbstract != abstract_) {
            NotificationChain msgs = null;
            if (abstract_ != null)
                msgs = ((InternalEObject) abstract_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMN_TYPE1__ABSTRACT, null, msgs);
            if (newAbstract != null)
                msgs = ((InternalEObject) newAbstract).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMN_TYPE1__ABSTRACT, null, msgs);
            msgs = basicSetAbstract(newAbstract, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__ABSTRACT, newAbstract, newAbstract));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDocumentation(String newDocumentation) {
        String oldDocumentation = documentation;
        documentation = newDocumentation;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__DOCUMENTATION, oldDocumentation, documentation));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ValuesType getValues() {
        return values;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetValues(ValuesType newValues, NotificationChain msgs) {
        ValuesType oldValues = values;
        values = newValues;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__VALUES, oldValues, newValues);
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
    public void setValues(ValuesType newValues) {
        if (newValues != values) {
            NotificationChain msgs = null;
            if (values != null)
                msgs = ((InternalEObject) values).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMN_TYPE1__VALUES, null, msgs);
            if (newValues != null)
                msgs = ((InternalEObject) newValues).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMN_TYPE1__VALUES, null, msgs);
            msgs = basicSetValues(newValues, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__VALUES, newValues, newValues));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataRequestType getGetDataRequest() {
        return getDataRequest;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetGetDataRequest(GetDataRequestType newGetDataRequest, NotificationChain msgs) {
        GetDataRequestType oldGetDataRequest = getDataRequest;
        getDataRequest = newGetDataRequest;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST, oldGetDataRequest, newGetDataRequest);
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
    public void setGetDataRequest(GetDataRequestType newGetDataRequest) {
        if (newGetDataRequest != getDataRequest) {
            NotificationChain msgs = null;
            if (getDataRequest != null)
                msgs = ((InternalEObject) getDataRequest).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST, null, msgs);
            if (newGetDataRequest != null)
                msgs = ((InternalEObject) newGetDataRequest).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST, null, msgs);
            msgs = basicSetGetDataRequest(newGetDataRequest, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST, newGetDataRequest, newGetDataRequest));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigInteger getDecimals() {
        return decimals;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDecimals(BigInteger newDecimals) {
        BigInteger oldDecimals = decimals;
        decimals = newDecimals;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__DECIMALS, oldDecimals, decimals));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigInteger getLength() {
        return length;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLength(BigInteger newLength) {
        BigInteger oldLength = length;
        length = newLength;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__LENGTH, oldLength, length));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getName() {
        return name;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setName(String newName) {
        String oldName = name;
        name = newName;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__NAME, oldName, name));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public PurposeType getPurpose() {
        return purpose;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setPurpose(PurposeType newPurpose) {
        PurposeType oldPurpose = purpose;
        purpose = newPurpose == null ? PURPOSE_EDEFAULT : newPurpose;
        boolean oldPurposeESet = purposeESet;
        purposeESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__PURPOSE, oldPurpose, purpose, !oldPurposeESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetPurpose() {
        PurposeType oldPurpose = purpose;
        boolean oldPurposeESet = purposeESet;
        purpose = PURPOSE_EDEFAULT;
        purposeESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.COLUMN_TYPE1__PURPOSE, oldPurpose, PURPOSE_EDEFAULT, oldPurposeESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetPurpose() {
        return purposeESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public TypeType getType() {
        return type;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setType(TypeType newType) {
        TypeType oldType = type;
        type = newType == null ? TYPE_EDEFAULT : newType;
        boolean oldTypeESet = typeESet;
        typeESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE1__TYPE, oldType, type, !oldTypeESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetType() {
        TypeType oldType = type;
        boolean oldTypeESet = typeESet;
        type = TYPE_EDEFAULT;
        typeESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.COLUMN_TYPE1__TYPE, oldType, TYPE_EDEFAULT, oldTypeESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetType() {
        return typeESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.COLUMN_TYPE1__ABSTRACT:
                return basicSetAbstract(null, msgs);
            case Tjs10Package.COLUMN_TYPE1__VALUES:
                return basicSetValues(null, msgs);
            case Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST:
                return basicSetGetDataRequest(null, msgs);
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
            case Tjs10Package.COLUMN_TYPE1__TITLE:
                return getTitle();
            case Tjs10Package.COLUMN_TYPE1__ABSTRACT:
                return getAbstract();
            case Tjs10Package.COLUMN_TYPE1__DOCUMENTATION:
                return getDocumentation();
            case Tjs10Package.COLUMN_TYPE1__VALUES:
                return getValues();
            case Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST:
                return getGetDataRequest();
            case Tjs10Package.COLUMN_TYPE1__DECIMALS:
                return getDecimals();
            case Tjs10Package.COLUMN_TYPE1__LENGTH:
                return getLength();
            case Tjs10Package.COLUMN_TYPE1__NAME:
                return getName();
            case Tjs10Package.COLUMN_TYPE1__PURPOSE:
                return getPurpose();
            case Tjs10Package.COLUMN_TYPE1__TYPE:
                return getType();
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
            case Tjs10Package.COLUMN_TYPE1__TITLE:
                setTitle((String) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__ABSTRACT:
                setAbstract((AbstractType) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__DOCUMENTATION:
                setDocumentation((String) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__VALUES:
                setValues((ValuesType) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST:
                setGetDataRequest((GetDataRequestType) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__DECIMALS:
                setDecimals((BigInteger) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__LENGTH:
                setLength((BigInteger) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__NAME:
                setName((String) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__PURPOSE:
                setPurpose((PurposeType) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE1__TYPE:
                if (newValue instanceof String) {
                    setType(TypeType.get((String) newValue));
                } else {
                    setType((TypeType) newValue);
                }
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
            case Tjs10Package.COLUMN_TYPE1__TITLE:
                setTitle(TITLE_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE1__ABSTRACT:
                setAbstract((AbstractType) null);
                return;
            case Tjs10Package.COLUMN_TYPE1__DOCUMENTATION:
                setDocumentation(DOCUMENTATION_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE1__VALUES:
                setValues((ValuesType) null);
                return;
            case Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST:
                setGetDataRequest((GetDataRequestType) null);
                return;
            case Tjs10Package.COLUMN_TYPE1__DECIMALS:
                setDecimals(DECIMALS_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE1__LENGTH:
                setLength(LENGTH_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE1__NAME:
                setName(NAME_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE1__PURPOSE:
                unsetPurpose();
                return;
            case Tjs10Package.COLUMN_TYPE1__TYPE:
                unsetType();
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
            case Tjs10Package.COLUMN_TYPE1__TITLE:
                return TITLE_EDEFAULT == null ? title != null : !TITLE_EDEFAULT.equals(title);
            case Tjs10Package.COLUMN_TYPE1__ABSTRACT:
                return abstract_ != null;
            case Tjs10Package.COLUMN_TYPE1__DOCUMENTATION:
                return DOCUMENTATION_EDEFAULT == null ? documentation != null : !DOCUMENTATION_EDEFAULT.equals(documentation);
            case Tjs10Package.COLUMN_TYPE1__VALUES:
                return values != null;
            case Tjs10Package.COLUMN_TYPE1__GET_DATA_REQUEST:
                return getDataRequest != null;
            case Tjs10Package.COLUMN_TYPE1__DECIMALS:
                return DECIMALS_EDEFAULT == null ? decimals != null : !DECIMALS_EDEFAULT.equals(decimals);
            case Tjs10Package.COLUMN_TYPE1__LENGTH:
                return LENGTH_EDEFAULT == null ? length != null : !LENGTH_EDEFAULT.equals(length);
            case Tjs10Package.COLUMN_TYPE1__NAME:
                return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
            case Tjs10Package.COLUMN_TYPE1__PURPOSE:
                return isSetPurpose();
            case Tjs10Package.COLUMN_TYPE1__TYPE:
                return isSetType();
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
        result.append(" (title: ");
        result.append(title);
        result.append(", documentation: ");
        result.append(documentation);
        result.append(", decimals: ");
        result.append(decimals);
        result.append(", length: ");
        result.append(length);
        result.append(", name: ");
        result.append(name);
        result.append(", purpose: ");
        if (purposeESet) result.append(purpose);
        else result.append("<unset>");
        result.append(", type: ");
        if (typeESet) result.append(type);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //ColumnType1Impl
