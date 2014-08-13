/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.DataInputsType;
import net.opengis.tjs10.FrameworkDatasetDescribeDataType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Data Inputs Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.DataInputsTypeImpl#getFramework <em>Framework</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DataInputsTypeImpl extends EObjectImpl implements DataInputsType {
    /**
     * The cached value of the '{@link #getFramework() <em>Framework</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFramework()
     */
    protected FrameworkDatasetDescribeDataType framework;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected DataInputsTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getDataInputsType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkDatasetDescribeDataType getFramework() {
        return framework;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFramework(FrameworkDatasetDescribeDataType newFramework, NotificationChain msgs) {
        FrameworkDatasetDescribeDataType oldFramework = framework;
        framework = newFramework;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK, oldFramework, newFramework);
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
    public void setFramework(FrameworkDatasetDescribeDataType newFramework) {
        if (newFramework != framework) {
            NotificationChain msgs = null;
            if (framework != null)
                msgs = ((InternalEObject) framework).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK, null, msgs);
            if (newFramework != null)
                msgs = ((InternalEObject) newFramework).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK, null, msgs);
            msgs = basicSetFramework(newFramework, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK, newFramework, newFramework));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK:
                return basicSetFramework(null, msgs);
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
            case Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK:
                return getFramework();
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
            case Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK:
                setFramework((FrameworkDatasetDescribeDataType) newValue);
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
            case Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK:
                setFramework((FrameworkDatasetDescribeDataType) null);
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
            case Tjs10Package.DATA_INPUTS_TYPE__FRAMEWORK:
                return framework != null;
        }
        return super.eIsSet(featureID);
    }

} //DataInputsTypeImpl
