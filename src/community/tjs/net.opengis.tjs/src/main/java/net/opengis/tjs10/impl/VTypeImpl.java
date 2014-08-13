/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.Tjs10Package;
import net.opengis.tjs10.VType;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>VType</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.VTypeImpl#getValue <em>Value</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.VTypeImpl#getAid <em>Aid</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.VTypeImpl#isNull <em>Null</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class VTypeImpl extends EObjectImpl implements VType {
    /**
     * The default value of the '{@link #getValue() <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getValue()
     */
    protected static final String VALUE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getValue() <em>Value</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getValue()
     */
    protected String value = VALUE_EDEFAULT;

    /**
     * The default value of the '{@link #getAid() <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAid()
     */
    protected static final String AID_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getAid() <em>Aid</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAid()
     */
    protected String aid = AID_EDEFAULT;

    /**
     * The default value of the '{@link #isNull() <em>Null</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #isNull()
     */
    protected static final boolean NULL_EDEFAULT = false;

    /**
     * The cached value of the '{@link #isNull() <em>Null</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #isNull()
     */
    protected boolean null_ = NULL_EDEFAULT;

    /**
     * This is true if the Null attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean nullESet;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected VTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getVType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getValue() {
        return value;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setValue(String newValue) {
        String oldValue = value;
        value = newValue;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VTYPE__VALUE, oldValue, value));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getAid() {
        return aid;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAid(String newAid) {
        String oldAid = aid;
        aid = newAid;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VTYPE__AID, oldAid, aid));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isNull() {
        return null_;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setNull(boolean newNull) {
        boolean oldNull = null_;
        null_ = newNull;
        boolean oldNullESet = nullESet;
        nullESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VTYPE__NULL, oldNull, null_, !oldNullESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetNull() {
        boolean oldNull = null_;
        boolean oldNullESet = nullESet;
        null_ = NULL_EDEFAULT;
        nullESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.VTYPE__NULL, oldNull, NULL_EDEFAULT, oldNullESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetNull() {
        return nullESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.VTYPE__VALUE:
                return getValue();
            case Tjs10Package.VTYPE__AID:
                return getAid();
            case Tjs10Package.VTYPE__NULL:
                return isNull() ? Boolean.TRUE : Boolean.FALSE;
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
            case Tjs10Package.VTYPE__VALUE:
                setValue((String) newValue);
                return;
            case Tjs10Package.VTYPE__AID:
                setAid((String) newValue);
                return;
            case Tjs10Package.VTYPE__NULL:
                setNull(((Boolean) newValue).booleanValue());
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
            case Tjs10Package.VTYPE__VALUE:
                setValue(VALUE_EDEFAULT);
                return;
            case Tjs10Package.VTYPE__AID:
                setAid(AID_EDEFAULT);
                return;
            case Tjs10Package.VTYPE__NULL:
                unsetNull();
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
            case Tjs10Package.VTYPE__VALUE:
                return VALUE_EDEFAULT == null ? value != null : !VALUE_EDEFAULT.equals(value);
            case Tjs10Package.VTYPE__AID:
                return AID_EDEFAULT == null ? aid != null : !AID_EDEFAULT.equals(aid);
            case Tjs10Package.VTYPE__NULL:
                return isSetNull();
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
        result.append(" (value: ");
        result.append(value);
        result.append(", aid: ");
        result.append(aid);
        result.append(", null: ");
        if (nullESet) result.append(null_);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //VTypeImpl
