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
 * An implementation of the model object '<em><b>Values Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.ValuesTypeImpl#getNominal <em>Nominal</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ValuesTypeImpl#getOrdinal <em>Ordinal</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ValuesTypeImpl#getCount <em>Count</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ValuesTypeImpl#getMeasure <em>Measure</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ValuesTypeImpl extends EObjectImpl implements ValuesType {
    /**
     * The cached value of the '{@link #getNominal() <em>Nominal</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getNominal()
     */
    protected NominalType nominal;

    /**
     * The cached value of the '{@link #getOrdinal() <em>Ordinal</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getOrdinal()
     */
    protected OrdinalType ordinal;

    /**
     * The cached value of the '{@link #getCount() <em>Count</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getCount()
     */
    protected CountType count;

    /**
     * The cached value of the '{@link #getMeasure() <em>Measure</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getMeasure()
     */
    protected MeasureType measure;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected ValuesTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getValuesType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NominalType getNominal() {
        return nominal;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetNominal(NominalType newNominal, NotificationChain msgs) {
        NominalType oldNominal = nominal;
        nominal = newNominal;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__NOMINAL, oldNominal, newNominal);
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
    public void setNominal(NominalType newNominal) {
        if (newNominal != nominal) {
            NotificationChain msgs = null;
            if (nominal != null)
                msgs = ((InternalEObject) nominal).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__NOMINAL, null, msgs);
            if (newNominal != null)
                msgs = ((InternalEObject) newNominal).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__NOMINAL, null, msgs);
            msgs = basicSetNominal(newNominal, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__NOMINAL, newNominal, newNominal));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OrdinalType getOrdinal() {
        return ordinal;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetOrdinal(OrdinalType newOrdinal, NotificationChain msgs) {
        OrdinalType oldOrdinal = ordinal;
        ordinal = newOrdinal;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__ORDINAL, oldOrdinal, newOrdinal);
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
    public void setOrdinal(OrdinalType newOrdinal) {
        if (newOrdinal != ordinal) {
            NotificationChain msgs = null;
            if (ordinal != null)
                msgs = ((InternalEObject) ordinal).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__ORDINAL, null, msgs);
            if (newOrdinal != null)
                msgs = ((InternalEObject) newOrdinal).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__ORDINAL, null, msgs);
            msgs = basicSetOrdinal(newOrdinal, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__ORDINAL, newOrdinal, newOrdinal));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public CountType getCount() {
        return count;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetCount(CountType newCount, NotificationChain msgs) {
        CountType oldCount = count;
        count = newCount;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__COUNT, oldCount, newCount);
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
    public void setCount(CountType newCount) {
        if (newCount != count) {
            NotificationChain msgs = null;
            if (count != null)
                msgs = ((InternalEObject) count).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__COUNT, null, msgs);
            if (newCount != null)
                msgs = ((InternalEObject) newCount).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__COUNT, null, msgs);
            msgs = basicSetCount(newCount, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__COUNT, newCount, newCount));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MeasureType getMeasure() {
        return measure;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetMeasure(MeasureType newMeasure, NotificationChain msgs) {
        MeasureType oldMeasure = measure;
        measure = newMeasure;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__MEASURE, oldMeasure, newMeasure);
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
    public void setMeasure(MeasureType newMeasure) {
        if (newMeasure != measure) {
            NotificationChain msgs = null;
            if (measure != null)
                msgs = ((InternalEObject) measure).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__MEASURE, null, msgs);
            if (newMeasure != null)
                msgs = ((InternalEObject) newMeasure).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.VALUES_TYPE__MEASURE, null, msgs);
            msgs = basicSetMeasure(newMeasure, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.VALUES_TYPE__MEASURE, newMeasure, newMeasure));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.VALUES_TYPE__NOMINAL:
                return basicSetNominal(null, msgs);
            case Tjs10Package.VALUES_TYPE__ORDINAL:
                return basicSetOrdinal(null, msgs);
            case Tjs10Package.VALUES_TYPE__COUNT:
                return basicSetCount(null, msgs);
            case Tjs10Package.VALUES_TYPE__MEASURE:
                return basicSetMeasure(null, msgs);
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
            case Tjs10Package.VALUES_TYPE__NOMINAL:
                return getNominal();
            case Tjs10Package.VALUES_TYPE__ORDINAL:
                return getOrdinal();
            case Tjs10Package.VALUES_TYPE__COUNT:
                return getCount();
            case Tjs10Package.VALUES_TYPE__MEASURE:
                return getMeasure();
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
            case Tjs10Package.VALUES_TYPE__NOMINAL:
                setNominal((NominalType) newValue);
                return;
            case Tjs10Package.VALUES_TYPE__ORDINAL:
                setOrdinal((OrdinalType) newValue);
                return;
            case Tjs10Package.VALUES_TYPE__COUNT:
                setCount((CountType) newValue);
                return;
            case Tjs10Package.VALUES_TYPE__MEASURE:
                setMeasure((MeasureType) newValue);
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
            case Tjs10Package.VALUES_TYPE__NOMINAL:
                setNominal((NominalType) null);
                return;
            case Tjs10Package.VALUES_TYPE__ORDINAL:
                setOrdinal((OrdinalType) null);
                return;
            case Tjs10Package.VALUES_TYPE__COUNT:
                setCount((CountType) null);
                return;
            case Tjs10Package.VALUES_TYPE__MEASURE:
                setMeasure((MeasureType) null);
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
            case Tjs10Package.VALUES_TYPE__NOMINAL:
                return nominal != null;
            case Tjs10Package.VALUES_TYPE__ORDINAL:
                return ordinal != null;
            case Tjs10Package.VALUES_TYPE__COUNT:
                return count != null;
            case Tjs10Package.VALUES_TYPE__MEASURE:
                return measure != null;
        }
        return super.eIsSet(featureID);
    }

} //ValuesTypeImpl
