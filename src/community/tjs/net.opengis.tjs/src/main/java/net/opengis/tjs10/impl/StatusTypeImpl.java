/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.FailedType;
import net.opengis.tjs10.StatusType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Status Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.StatusTypeImpl#getAccepted <em>Accepted</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.StatusTypeImpl#getCompleted <em>Completed</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.StatusTypeImpl#getFailed <em>Failed</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.StatusTypeImpl#getCreationTime <em>Creation Time</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.StatusTypeImpl#getHref <em>Href</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class StatusTypeImpl extends EObjectImpl implements StatusType {
    /**
     * The cached value of the '{@link #getAccepted() <em>Accepted</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAccepted()
     */
    protected EObject accepted;

    /**
     * The cached value of the '{@link #getCompleted() <em>Completed</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getCompleted()
     */
    protected EObject completed;

    /**
     * The cached value of the '{@link #getFailed() <em>Failed</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFailed()
     */
    protected FailedType failed;

    /**
     * The default value of the '{@link #getCreationTime() <em>Creation Time</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getCreationTime()
     */
    protected static final Object CREATION_TIME_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getCreationTime() <em>Creation Time</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getCreationTime()
     */
    protected Object creationTime = CREATION_TIME_EDEFAULT;

    /**
     * The default value of the '{@link #getHref() <em>Href</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getHref()
     */
    protected static final String HREF_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getHref() <em>Href</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getHref()
     */
    protected String href = HREF_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected StatusTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getStatusType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getAccepted() {
        return accepted;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAccepted(EObject newAccepted, NotificationChain msgs) {
        EObject oldAccepted = accepted;
        accepted = newAccepted;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__ACCEPTED, oldAccepted, newAccepted);
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
    public void setAccepted(EObject newAccepted) {
        if (newAccepted != accepted) {
            NotificationChain msgs = null;
            if (accepted != null)
                msgs = ((InternalEObject) accepted).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.STATUS_TYPE__ACCEPTED, null, msgs);
            if (newAccepted != null)
                msgs = ((InternalEObject) newAccepted).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.STATUS_TYPE__ACCEPTED, null, msgs);
            msgs = basicSetAccepted(newAccepted, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__ACCEPTED, newAccepted, newAccepted));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getCompleted() {
        return completed;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetCompleted(EObject newCompleted, NotificationChain msgs) {
        EObject oldCompleted = completed;
        completed = newCompleted;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__COMPLETED, oldCompleted, newCompleted);
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
    public void setCompleted(EObject newCompleted) {
        if (newCompleted != completed) {
            NotificationChain msgs = null;
            if (completed != null)
                msgs = ((InternalEObject) completed).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.STATUS_TYPE__COMPLETED, null, msgs);
            if (newCompleted != null)
                msgs = ((InternalEObject) newCompleted).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.STATUS_TYPE__COMPLETED, null, msgs);
            msgs = basicSetCompleted(newCompleted, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__COMPLETED, newCompleted, newCompleted));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FailedType getFailed() {
        return failed;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFailed(FailedType newFailed, NotificationChain msgs) {
        FailedType oldFailed = failed;
        failed = newFailed;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__FAILED, oldFailed, newFailed);
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
    public void setFailed(FailedType newFailed) {
        if (newFailed != failed) {
            NotificationChain msgs = null;
            if (failed != null)
                msgs = ((InternalEObject) failed).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.STATUS_TYPE__FAILED, null, msgs);
            if (newFailed != null)
                msgs = ((InternalEObject) newFailed).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.STATUS_TYPE__FAILED, null, msgs);
            msgs = basicSetFailed(newFailed, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__FAILED, newFailed, newFailed));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object getCreationTime() {
        return creationTime;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setCreationTime(Object newCreationTime) {
        Object oldCreationTime = creationTime;
        creationTime = newCreationTime;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__CREATION_TIME, oldCreationTime, creationTime));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getHref() {
        return href;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setHref(String newHref) {
        String oldHref = href;
        href = newHref;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.STATUS_TYPE__HREF, oldHref, href));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.STATUS_TYPE__ACCEPTED:
                return basicSetAccepted(null, msgs);
            case Tjs10Package.STATUS_TYPE__COMPLETED:
                return basicSetCompleted(null, msgs);
            case Tjs10Package.STATUS_TYPE__FAILED:
                return basicSetFailed(null, msgs);
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
            case Tjs10Package.STATUS_TYPE__ACCEPTED:
                return getAccepted();
            case Tjs10Package.STATUS_TYPE__COMPLETED:
                return getCompleted();
            case Tjs10Package.STATUS_TYPE__FAILED:
                return getFailed();
            case Tjs10Package.STATUS_TYPE__CREATION_TIME:
                return getCreationTime();
            case Tjs10Package.STATUS_TYPE__HREF:
                return getHref();
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
            case Tjs10Package.STATUS_TYPE__ACCEPTED:
                setAccepted((EObject) newValue);
                return;
            case Tjs10Package.STATUS_TYPE__COMPLETED:
                setCompleted((EObject) newValue);
                return;
            case Tjs10Package.STATUS_TYPE__FAILED:
                setFailed((FailedType) newValue);
                return;
            case Tjs10Package.STATUS_TYPE__CREATION_TIME:
                setCreationTime(newValue);
                return;
            case Tjs10Package.STATUS_TYPE__HREF:
                setHref((String) newValue);
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
            case Tjs10Package.STATUS_TYPE__ACCEPTED:
                setAccepted((EObject) null);
                return;
            case Tjs10Package.STATUS_TYPE__COMPLETED:
                setCompleted((EObject) null);
                return;
            case Tjs10Package.STATUS_TYPE__FAILED:
                setFailed((FailedType) null);
                return;
            case Tjs10Package.STATUS_TYPE__CREATION_TIME:
                setCreationTime(CREATION_TIME_EDEFAULT);
                return;
            case Tjs10Package.STATUS_TYPE__HREF:
                setHref(HREF_EDEFAULT);
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
            case Tjs10Package.STATUS_TYPE__ACCEPTED:
                return accepted != null;
            case Tjs10Package.STATUS_TYPE__COMPLETED:
                return completed != null;
            case Tjs10Package.STATUS_TYPE__FAILED:
                return failed != null;
            case Tjs10Package.STATUS_TYPE__CREATION_TIME:
                return CREATION_TIME_EDEFAULT == null ? creationTime != null : !CREATION_TIME_EDEFAULT.equals(creationTime);
            case Tjs10Package.STATUS_TYPE__HREF:
                return HREF_EDEFAULT == null ? href != null : !HREF_EDEFAULT.equals(href);
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
        result.append(" (creationTime: ");
        result.append(creationTime);
        result.append(", href: ");
        result.append(href);
        result.append(')');
        return result.toString();
    }

} //StatusTypeImpl
