/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.NominalOrdinalExceptions;
import net.opengis.tjs10.NullType1;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import java.util.Collection;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Nominal Ordinal Exceptions</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.NominalOrdinalExceptionsImpl#getNull <em>Null</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class NominalOrdinalExceptionsImpl extends EObjectImpl implements NominalOrdinalExceptions {
    /**
     * The cached value of the '{@link #getNull() <em>Null</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getNull()
     */
    protected EList null_;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected NominalOrdinalExceptionsImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getNominalOrdinalExceptions();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EList getNull() {
        if (null_ == null) {
            null_ = new EObjectContainmentEList(NullType1.class, this, Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS__NULL);
        }
        return null_;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS__NULL:
                return ((InternalEList) getNull()).basicRemove(otherEnd, msgs);
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
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS__NULL:
                return getNull();
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
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS__NULL:
                getNull().clear();
                getNull().addAll((Collection) newValue);
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
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS__NULL:
                getNull().clear();
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
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS__NULL:
                return null_ != null && !null_.isEmpty();
        }
        return super.eIsSet(featureID);
    }

} //NominalOrdinalExceptionsImpl
