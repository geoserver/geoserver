/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.GaussianType;
import net.opengis.tjs10.Tjs10Package;
import net.opengis.tjs10.UncertaintyType;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Uncertainty Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.UncertaintyTypeImpl#getValue <em>Value</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.UncertaintyTypeImpl#getGaussian <em>Gaussian</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class UncertaintyTypeImpl extends EObjectImpl implements UncertaintyType {
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
     * The default value of the '{@link #getGaussian() <em>Gaussian</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGaussian()
     */
    protected static final GaussianType GAUSSIAN_EDEFAULT = GaussianType.TRUE_LITERAL;

    /**
     * The cached value of the '{@link #getGaussian() <em>Gaussian</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGaussian()
     */
    protected GaussianType gaussian = GAUSSIAN_EDEFAULT;

    /**
     * This is true if the Gaussian attribute has been set.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    protected boolean gaussianESet;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected UncertaintyTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getUncertaintyType();
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.UNCERTAINTY_TYPE__VALUE, oldValue, value));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GaussianType getGaussian() {
        return gaussian;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGaussian(GaussianType newGaussian) {
        GaussianType oldGaussian = gaussian;
        gaussian = newGaussian == null ? GAUSSIAN_EDEFAULT : newGaussian;
        boolean oldGaussianESet = gaussianESet;
        gaussianESet = true;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.UNCERTAINTY_TYPE__GAUSSIAN, oldGaussian, gaussian, !oldGaussianESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void unsetGaussian() {
        GaussianType oldGaussian = gaussian;
        boolean oldGaussianESet = gaussianESet;
        gaussian = GAUSSIAN_EDEFAULT;
        gaussianESet = false;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.UNSET, Tjs10Package.UNCERTAINTY_TYPE__GAUSSIAN, oldGaussian, GAUSSIAN_EDEFAULT, oldGaussianESet));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean isSetGaussian() {
        return gaussianESet;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.UNCERTAINTY_TYPE__VALUE:
                return getValue();
            case Tjs10Package.UNCERTAINTY_TYPE__GAUSSIAN:
                return getGaussian();
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
            case Tjs10Package.UNCERTAINTY_TYPE__VALUE:
                setValue((String) newValue);
                return;
            case Tjs10Package.UNCERTAINTY_TYPE__GAUSSIAN:
                setGaussian((GaussianType) newValue);
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
            case Tjs10Package.UNCERTAINTY_TYPE__VALUE:
                setValue(VALUE_EDEFAULT);
                return;
            case Tjs10Package.UNCERTAINTY_TYPE__GAUSSIAN:
                unsetGaussian();
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
            case Tjs10Package.UNCERTAINTY_TYPE__VALUE:
                return VALUE_EDEFAULT == null ? value != null : !VALUE_EDEFAULT.equals(value);
            case Tjs10Package.UNCERTAINTY_TYPE__GAUSSIAN:
                return isSetGaussian();
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
        result.append(", gaussian: ");
        if (gaussianESet) result.append(gaussian);
        else result.append("<unset>");
        result.append(')');
        return result.toString();
    }

} //UncertaintyTypeImpl
