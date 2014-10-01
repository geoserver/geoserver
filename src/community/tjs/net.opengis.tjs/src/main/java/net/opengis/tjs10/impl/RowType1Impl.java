/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.KType;
import net.opengis.tjs10.RowType1;
import net.opengis.tjs10.Tjs10Package;
import net.opengis.tjs10.VType;
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
 * An implementation of the model object '<em><b>Row Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.RowType1Impl#getK <em>K</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.RowType1Impl#getV <em>V</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RowType1Impl extends EObjectImpl implements RowType1 {
    /**
     * The cached value of the '{@link #getK() <em>K</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getK()
     */
    protected EList k;

    /**
     * The cached value of the '{@link #getV() <em>V</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getV()
     */
    protected EList v;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected RowType1Impl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getRowType1();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EList getK() {
        if (k == null) {
            k = new EObjectContainmentEList(KType.class, this, Tjs10Package.ROW_TYPE1__K);
        }
        return k;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EList getV() {
        if (v == null) {
            v = new EObjectContainmentEList(VType.class, this, Tjs10Package.ROW_TYPE1__V);
        }
        return v;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.ROW_TYPE1__K:
                return ((InternalEList) getK()).basicRemove(otherEnd, msgs);
            case Tjs10Package.ROW_TYPE1__V:
                return ((InternalEList) getV()).basicRemove(otherEnd, msgs);
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
            case Tjs10Package.ROW_TYPE1__K:
                return getK();
            case Tjs10Package.ROW_TYPE1__V:
                return getV();
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
            case Tjs10Package.ROW_TYPE1__K:
                getK().clear();
                getK().addAll((Collection) newValue);
                return;
            case Tjs10Package.ROW_TYPE1__V:
                getV().clear();
                getV().addAll((Collection) newValue);
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
            case Tjs10Package.ROW_TYPE1__K:
                getK().clear();
                return;
            case Tjs10Package.ROW_TYPE1__V:
                getV().clear();
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
            case Tjs10Package.ROW_TYPE1__K:
                return k != null && !k.isEmpty();
            case Tjs10Package.ROW_TYPE1__V:
                return v != null && !v.isEmpty();
        }
        return super.eIsSet(featureID);
    }

} //RowType1Impl
