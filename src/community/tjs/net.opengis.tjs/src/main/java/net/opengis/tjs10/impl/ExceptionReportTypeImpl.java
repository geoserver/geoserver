/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.ows11.ExceptionType;
import net.opengis.tjs10.ExceptionReportType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Exception Report Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.ExceptionReportTypeImpl#getException <em>Exception</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ExceptionReportTypeImpl extends EObjectImpl implements ExceptionReportType {
    /**
     * The cached value of the '{@link #getException() <em>Exception</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getException()
     */
    protected ExceptionType exception;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected ExceptionReportTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getExceptionReportType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ExceptionType getException() {
        return exception;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetException(ExceptionType newException, NotificationChain msgs) {
        ExceptionType oldException = exception;
        exception = newException;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION, oldException, newException);
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
    public void setException(ExceptionType newException) {
        if (newException != exception) {
            NotificationChain msgs = null;
            if (exception != null)
                msgs = ((InternalEObject) exception).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION, null, msgs);
            if (newException != null)
                msgs = ((InternalEObject) newException).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION, null, msgs);
            msgs = basicSetException(newException, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION, newException, newException));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION:
                return basicSetException(null, msgs);
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
            case Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION:
                return getException();
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
            case Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION:
                setException((ExceptionType) newValue);
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
            case Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION:
                setException((ExceptionType) null);
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
            case Tjs10Package.EXCEPTION_REPORT_TYPE__EXCEPTION:
                return exception != null;
        }
        return super.eIsSet(featureID);
    }

} //ExceptionReportTypeImpl
