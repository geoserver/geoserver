/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.ClassesType1;
import net.opengis.tjs10.NominalOrdinalExceptions;
import net.opengis.tjs10.NominalType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Nominal Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.NominalTypeImpl#getClasses <em>Classes</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.NominalTypeImpl#getExceptions <em>Exceptions</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class NominalTypeImpl extends EObjectImpl implements NominalType {
    /**
     * The cached value of the '{@link #getClasses() <em>Classes</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getClasses()
     */
    protected ClassesType1 classes;

    /**
     * The cached value of the '{@link #getExceptions() <em>Exceptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getExceptions()
     */
    protected NominalOrdinalExceptions exceptions;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected NominalTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getNominalType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ClassesType1 getClasses() {
        return classes;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetClasses(ClassesType1 newClasses, NotificationChain msgs) {
        ClassesType1 oldClasses = classes;
        classes = newClasses;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.NOMINAL_TYPE__CLASSES, oldClasses, newClasses);
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
    public void setClasses(ClassesType1 newClasses) {
        if (newClasses != classes) {
            NotificationChain msgs = null;
            if (classes != null)
                msgs = ((InternalEObject) classes).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.NOMINAL_TYPE__CLASSES, null, msgs);
            if (newClasses != null)
                msgs = ((InternalEObject) newClasses).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.NOMINAL_TYPE__CLASSES, null, msgs);
            msgs = basicSetClasses(newClasses, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.NOMINAL_TYPE__CLASSES, newClasses, newClasses));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NominalOrdinalExceptions getExceptions() {
        return exceptions;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetExceptions(NominalOrdinalExceptions newExceptions, NotificationChain msgs) {
        NominalOrdinalExceptions oldExceptions = exceptions;
        exceptions = newExceptions;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.NOMINAL_TYPE__EXCEPTIONS, oldExceptions, newExceptions);
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
    public void setExceptions(NominalOrdinalExceptions newExceptions) {
        if (newExceptions != exceptions) {
            NotificationChain msgs = null;
            if (exceptions != null)
                msgs = ((InternalEObject) exceptions).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.NOMINAL_TYPE__EXCEPTIONS, null, msgs);
            if (newExceptions != null)
                msgs = ((InternalEObject) newExceptions).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.NOMINAL_TYPE__EXCEPTIONS, null, msgs);
            msgs = basicSetExceptions(newExceptions, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.NOMINAL_TYPE__EXCEPTIONS, newExceptions, newExceptions));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.NOMINAL_TYPE__CLASSES:
                return basicSetClasses(null, msgs);
            case Tjs10Package.NOMINAL_TYPE__EXCEPTIONS:
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
            case Tjs10Package.NOMINAL_TYPE__CLASSES:
                return getClasses();
            case Tjs10Package.NOMINAL_TYPE__EXCEPTIONS:
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
            case Tjs10Package.NOMINAL_TYPE__CLASSES:
                setClasses((ClassesType1) newValue);
                return;
            case Tjs10Package.NOMINAL_TYPE__EXCEPTIONS:
                setExceptions((NominalOrdinalExceptions) newValue);
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
            case Tjs10Package.NOMINAL_TYPE__CLASSES:
                setClasses((ClassesType1) null);
                return;
            case Tjs10Package.NOMINAL_TYPE__EXCEPTIONS:
                setExceptions((NominalOrdinalExceptions) null);
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
            case Tjs10Package.NOMINAL_TYPE__CLASSES:
                return classes != null;
            case Tjs10Package.NOMINAL_TYPE__EXCEPTIONS:
                return exceptions != null;
        }
        return super.eIsSet(featureID);
    }

} //NominalTypeImpl
