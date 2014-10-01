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
 * An implementation of the model object '<em><b>Output Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.OutputTypeImpl#getMechanism <em>Mechanism</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.OutputTypeImpl#getResource <em>Resource</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.OutputTypeImpl#getExceptionReport <em>Exception Report</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class OutputTypeImpl extends EObjectImpl implements OutputType {
    /**
     * The cached value of the '{@link #getMechanism() <em>Mechanism</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getMechanism()
     */
    protected MechanismType mechanism;

    /**
     * The cached value of the '{@link #getResource() <em>Resource</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getResource()
     */
    protected ResourceType resource;

    /**
     * The cached value of the '{@link #getExceptionReport() <em>Exception Report</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getExceptionReport()
     */
    protected ExceptionReportType exceptionReport;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected OutputTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getOutputType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MechanismType getMechanism() {
        return mechanism;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetMechanism(MechanismType newMechanism, NotificationChain msgs) {
        MechanismType oldMechanism = mechanism;
        mechanism = newMechanism;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.OUTPUT_TYPE__MECHANISM, oldMechanism, newMechanism);
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
    public void setMechanism(MechanismType newMechanism) {
        if (newMechanism != mechanism) {
            NotificationChain msgs = null;
            if (mechanism != null)
                msgs = ((InternalEObject) mechanism).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.OUTPUT_TYPE__MECHANISM, null, msgs);
            if (newMechanism != null)
                msgs = ((InternalEObject) newMechanism).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.OUTPUT_TYPE__MECHANISM, null, msgs);
            msgs = basicSetMechanism(newMechanism, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.OUTPUT_TYPE__MECHANISM, newMechanism, newMechanism));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ResourceType getResource() {
        return resource;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetResource(ResourceType newResource, NotificationChain msgs) {
        ResourceType oldResource = resource;
        resource = newResource;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.OUTPUT_TYPE__RESOURCE, oldResource, newResource);
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
    public void setResource(ResourceType newResource) {
        if (newResource != resource) {
            NotificationChain msgs = null;
            if (resource != null)
                msgs = ((InternalEObject) resource).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.OUTPUT_TYPE__RESOURCE, null, msgs);
            if (newResource != null)
                msgs = ((InternalEObject) newResource).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.OUTPUT_TYPE__RESOURCE, null, msgs);
            msgs = basicSetResource(newResource, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.OUTPUT_TYPE__RESOURCE, newResource, newResource));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ExceptionReportType getExceptionReport() {
        return exceptionReport;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetExceptionReport(ExceptionReportType newExceptionReport, NotificationChain msgs) {
        ExceptionReportType oldExceptionReport = exceptionReport;
        exceptionReport = newExceptionReport;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT, oldExceptionReport, newExceptionReport);
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
    public void setExceptionReport(ExceptionReportType newExceptionReport) {
        if (newExceptionReport != exceptionReport) {
            NotificationChain msgs = null;
            if (exceptionReport != null)
                msgs = ((InternalEObject) exceptionReport).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT, null, msgs);
            if (newExceptionReport != null)
                msgs = ((InternalEObject) newExceptionReport).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT, null, msgs);
            msgs = basicSetExceptionReport(newExceptionReport, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT, newExceptionReport, newExceptionReport));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.OUTPUT_TYPE__MECHANISM:
                return basicSetMechanism(null, msgs);
            case Tjs10Package.OUTPUT_TYPE__RESOURCE:
                return basicSetResource(null, msgs);
            case Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT:
                return basicSetExceptionReport(null, msgs);
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
            case Tjs10Package.OUTPUT_TYPE__MECHANISM:
                return getMechanism();
            case Tjs10Package.OUTPUT_TYPE__RESOURCE:
                return getResource();
            case Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT:
                return getExceptionReport();
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
            case Tjs10Package.OUTPUT_TYPE__MECHANISM:
                setMechanism((MechanismType) newValue);
                return;
            case Tjs10Package.OUTPUT_TYPE__RESOURCE:
                setResource((ResourceType) newValue);
                return;
            case Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT:
                setExceptionReport((ExceptionReportType) newValue);
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
            case Tjs10Package.OUTPUT_TYPE__MECHANISM:
                setMechanism((MechanismType) null);
                return;
            case Tjs10Package.OUTPUT_TYPE__RESOURCE:
                setResource((ResourceType) null);
                return;
            case Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT:
                setExceptionReport((ExceptionReportType) null);
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
            case Tjs10Package.OUTPUT_TYPE__MECHANISM:
                return mechanism != null;
            case Tjs10Package.OUTPUT_TYPE__RESOURCE:
                return resource != null;
            case Tjs10Package.OUTPUT_TYPE__EXCEPTION_REPORT:
                return exceptionReport != null;
        }
        return super.eIsSet(featureID);
    }

} //OutputTypeImpl
