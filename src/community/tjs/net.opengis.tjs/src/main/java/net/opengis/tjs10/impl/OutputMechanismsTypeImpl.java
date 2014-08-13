/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.MechanismType;
import net.opengis.tjs10.OutputMechanismsType;
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
 * An implementation of the model object '<em><b>Output Mechanisms Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.OutputMechanismsTypeImpl#getMechanism <em>Mechanism</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class OutputMechanismsTypeImpl extends EObjectImpl implements OutputMechanismsType {
    /**
     * The cached value of the '{@link #getMechanism() <em>Mechanism</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getMechanism()
     */
    protected EList mechanism;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected OutputMechanismsTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getOutputMechanismsType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EList getMechanism() {
        if (mechanism == null) {
            mechanism = new EObjectContainmentEList(MechanismType.class, this, Tjs10Package.OUTPUT_MECHANISMS_TYPE__MECHANISM);
        }
        return mechanism;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE__MECHANISM:
                return ((InternalEList) getMechanism()).basicRemove(otherEnd, msgs);
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
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE__MECHANISM:
                return getMechanism();
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
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE__MECHANISM:
                getMechanism().clear();
                getMechanism().addAll((Collection) newValue);
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
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE__MECHANISM:
                getMechanism().clear();
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
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE__MECHANISM:
                return mechanism != null && !mechanism.isEmpty();
        }
        return super.eIsSet(featureID);
    }

} //OutputMechanismsTypeImpl
