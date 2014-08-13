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
 * An implementation of the model object '<em><b>Measure Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.MeasureTypeImpl#getUOM <em>UOM</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.MeasureTypeImpl#getUncertainty <em>Uncertainty</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.MeasureTypeImpl#getExceptions <em>Exceptions</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MeasureTypeImpl extends EObjectImpl implements MeasureType {
    /**
     * The cached value of the '{@link #getUOM() <em>UOM</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getUOM()
     */
    protected UOMType uOM;

    /**
     * The cached value of the '{@link #getUncertainty() <em>Uncertainty</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getUncertainty()
     */
    protected UncertaintyType uncertainty;

    /**
     * The cached value of the '{@link #getExceptions() <em>Exceptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getExceptions()
     */
    protected MeasureCountExceptions exceptions;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected MeasureTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getMeasureType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UOMType getUOM() {
        return uOM;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetUOM(UOMType newUOM, NotificationChain msgs) {
        UOMType oldUOM = uOM;
        uOM = newUOM;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.MEASURE_TYPE__UOM, oldUOM, newUOM);
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
    public void setUOM(UOMType newUOM) {
        if (newUOM != uOM) {
            NotificationChain msgs = null;
            if (uOM != null)
                msgs = ((InternalEObject) uOM).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MEASURE_TYPE__UOM, null, msgs);
            if (newUOM != null)
                msgs = ((InternalEObject) newUOM).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MEASURE_TYPE__UOM, null, msgs);
            msgs = basicSetUOM(newUOM, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.MEASURE_TYPE__UOM, newUOM, newUOM));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UncertaintyType getUncertainty() {
        return uncertainty;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetUncertainty(UncertaintyType newUncertainty, NotificationChain msgs) {
        UncertaintyType oldUncertainty = uncertainty;
        uncertainty = newUncertainty;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.MEASURE_TYPE__UNCERTAINTY, oldUncertainty, newUncertainty);
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
    public void setUncertainty(UncertaintyType newUncertainty) {
        if (newUncertainty != uncertainty) {
            NotificationChain msgs = null;
            if (uncertainty != null)
                msgs = ((InternalEObject) uncertainty).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MEASURE_TYPE__UNCERTAINTY, null, msgs);
            if (newUncertainty != null)
                msgs = ((InternalEObject) newUncertainty).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MEASURE_TYPE__UNCERTAINTY, null, msgs);
            msgs = basicSetUncertainty(newUncertainty, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.MEASURE_TYPE__UNCERTAINTY, newUncertainty, newUncertainty));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MeasureCountExceptions getExceptions() {
        return exceptions;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetExceptions(MeasureCountExceptions newExceptions, NotificationChain msgs) {
        MeasureCountExceptions oldExceptions = exceptions;
        exceptions = newExceptions;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.MEASURE_TYPE__EXCEPTIONS, oldExceptions, newExceptions);
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
    public void setExceptions(MeasureCountExceptions newExceptions) {
        if (newExceptions != exceptions) {
            NotificationChain msgs = null;
            if (exceptions != null)
                msgs = ((InternalEObject) exceptions).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MEASURE_TYPE__EXCEPTIONS, null, msgs);
            if (newExceptions != null)
                msgs = ((InternalEObject) newExceptions).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MEASURE_TYPE__EXCEPTIONS, null, msgs);
            msgs = basicSetExceptions(newExceptions, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.MEASURE_TYPE__EXCEPTIONS, newExceptions, newExceptions));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.MEASURE_TYPE__UOM:
                return basicSetUOM(null, msgs);
            case Tjs10Package.MEASURE_TYPE__UNCERTAINTY:
                return basicSetUncertainty(null, msgs);
            case Tjs10Package.MEASURE_TYPE__EXCEPTIONS:
                return basicSetExceptions(null, msgs);
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
            case Tjs10Package.MEASURE_TYPE__UOM:
                return getUOM();
            case Tjs10Package.MEASURE_TYPE__UNCERTAINTY:
                return getUncertainty();
            case Tjs10Package.MEASURE_TYPE__EXCEPTIONS:
                return getExceptions();
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
            case Tjs10Package.MEASURE_TYPE__UOM:
                setUOM((UOMType) newValue);
                return;
            case Tjs10Package.MEASURE_TYPE__UNCERTAINTY:
                setUncertainty((UncertaintyType) newValue);
                return;
            case Tjs10Package.MEASURE_TYPE__EXCEPTIONS:
                setExceptions((MeasureCountExceptions) newValue);
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
            case Tjs10Package.MEASURE_TYPE__UOM:
                setUOM((UOMType) null);
                return;
            case Tjs10Package.MEASURE_TYPE__UNCERTAINTY:
                setUncertainty((UncertaintyType) null);
                return;
            case Tjs10Package.MEASURE_TYPE__EXCEPTIONS:
                setExceptions((MeasureCountExceptions) null);
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
            case Tjs10Package.MEASURE_TYPE__UOM:
                return uOM != null;
            case Tjs10Package.MEASURE_TYPE__UNCERTAINTY:
                return uncertainty != null;
            case Tjs10Package.MEASURE_TYPE__EXCEPTIONS:
                return exceptions != null;
        }
        return super.eIsSet(featureID);
    }

} //MeasureTypeImpl
