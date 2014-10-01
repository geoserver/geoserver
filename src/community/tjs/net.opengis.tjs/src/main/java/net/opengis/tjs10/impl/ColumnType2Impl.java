/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.ColumnType2;
import net.opengis.tjs10.Tjs10Package;
import net.opengis.tjs10.TypeType;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Column Type2</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.ColumnType2Impl#getDecimals <em>Decimals</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType2Impl#getLength <em>Length</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType2Impl#getName <em>Name</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnType2Impl#getType <em>Type</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ColumnType2Impl extends EObjectImpl implements ColumnType2 {
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
    protected ColumnType2Impl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getColumnType2();
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE2__DECIMALS, oldDecimals, decimals));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE2__LENGTH, oldLength, length));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE2__NAME, oldName, name));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMN_TYPE2__TYPE, oldType, type, !oldTypeESet));
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
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.COLUMN_TYPE2__TYPE, oldType, TYPE_EDEFAULT, oldTypeESet));
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
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.COLUMN_TYPE2__DECIMALS:
                return getDecimals();
            case Tjs10Package.COLUMN_TYPE2__LENGTH:
                return getLength();
            case Tjs10Package.COLUMN_TYPE2__NAME:
                return getName();
            case Tjs10Package.COLUMN_TYPE2__TYPE:
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
            case Tjs10Package.COLUMN_TYPE2__DECIMALS:
                setDecimals((BigInteger) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE2__LENGTH:
                setLength((BigInteger) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE2__NAME:
                setName((String) newValue);
                return;
            case Tjs10Package.COLUMN_TYPE2__TYPE:
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
            case Tjs10Package.COLUMN_TYPE2__DECIMALS:
                setDecimals(DECIMALS_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE2__LENGTH:
                setLength(LENGTH_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE2__NAME:
                setName(NAME_EDEFAULT);
                return;
            case Tjs10Package.COLUMN_TYPE2__TYPE:
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
            case Tjs10Package.COLUMN_TYPE2__DECIMALS:
                return DECIMALS_EDEFAULT == null ? decimals != null : !DECIMALS_EDEFAULT.equals(decimals);
            case Tjs10Package.COLUMN_TYPE2__LENGTH:
                return LENGTH_EDEFAULT == null ? length != null : !LENGTH_EDEFAULT.equals(length);
            case Tjs10Package.COLUMN_TYPE2__NAME:
                return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
            case Tjs10Package.COLUMN_TYPE2__TYPE:
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
        result.append(" (decimals: ");
        result.append(decimals);
        result.append(", length: ");
        result.append(length);
        result.append(", name: ");
        result.append(name);
        result.append(", type: ");
        if (typeESet) result.append(type);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //ColumnType2Impl
