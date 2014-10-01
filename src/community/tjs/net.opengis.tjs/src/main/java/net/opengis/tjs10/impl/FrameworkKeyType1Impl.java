/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.ColumnType2;
import net.opengis.tjs10.FrameworkKeyType1;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import java.util.Collection;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Framework Key Type1</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.FrameworkKeyType1Impl#getColumn <em>Column</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkKeyType1Impl#getComplete <em>Complete</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkKeyType1Impl#getRelationship <em>Relationship</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FrameworkKeyType1Impl extends EObjectImpl implements FrameworkKeyType1 {
    /**
     * The cached value of the '{@link #getColumn() <em>Column</em>}' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getColumn()
     */
    protected EList column;

    /**
     * The default value of the '{@link #getComplete() <em>Complete</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getComplete()
     */
    protected static final Object COMPLETE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getComplete() <em>Complete</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getComplete()
     */
    protected Object complete = COMPLETE_EDEFAULT;

    /**
     * The default value of the '{@link #getRelationship() <em>Relationship</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getRelationship()
     */
    protected static final Object RELATIONSHIP_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getRelationship() <em>Relationship</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getRelationship()
     */
    protected Object relationship = RELATIONSHIP_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected FrameworkKeyType1Impl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getFrameworkKeyType1();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EList getColumn() {
        if (column == null) {
            column = new EObjectContainmentEList(ColumnType2.class, this, Tjs10Package.FRAMEWORK_KEY_TYPE1__COLUMN);
        }
        return column;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getComplete() {
        return complete;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setComplete(Object newComplete) {
        Object oldComplete = complete;
        complete = newComplete;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_KEY_TYPE1__COMPLETE, oldComplete, complete));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getRelationship() {
        return relationship;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setRelationship(Object newRelationship) {
        Object oldRelationship = relationship;
        relationship = newRelationship;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_KEY_TYPE1__RELATIONSHIP, oldRelationship, relationship));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COLUMN:
                return ((InternalEList) getColumn()).basicRemove(otherEnd, msgs);
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
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COLUMN:
                return getColumn();
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COMPLETE:
                return getComplete();
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__RELATIONSHIP:
                return getRelationship();
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
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COLUMN:
                getColumn().clear();
                getColumn().addAll((Collection) newValue);
                return;
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COMPLETE:
                setComplete(newValue);
                return;
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__RELATIONSHIP:
                setRelationship(newValue);
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
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COLUMN:
                getColumn().clear();
                return;
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COMPLETE:
                setComplete(COMPLETE_EDEFAULT);
                return;
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__RELATIONSHIP:
                setRelationship(RELATIONSHIP_EDEFAULT);
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
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COLUMN:
                return column != null && !column.isEmpty();
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__COMPLETE:
                return COMPLETE_EDEFAULT == null ? complete != null : !COMPLETE_EDEFAULT.equals(complete);
            case Tjs10Package.FRAMEWORK_KEY_TYPE1__RELATIONSHIP:
                return RELATIONSHIP_EDEFAULT == null ? relationship != null : !RELATIONSHIP_EDEFAULT.equals(relationship);
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
        result.append(" (complete: ");
        result.append(complete);
        result.append(", relationship: ");
        result.append(relationship);
        result.append(')');
        return result.toString();
    }

} //FrameworkKeyType1Impl
