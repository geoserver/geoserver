/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.Tjs10Package;
import net.opengis.tjs10.UOMType;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>UOM Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.UOMTypeImpl#getShortForm <em>Short Form</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.UOMTypeImpl#getLongForm <em>Long Form</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.UOMTypeImpl#getReference <em>Reference</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class UOMTypeImpl extends EObjectImpl implements UOMType {
    /**
     * The cached value of the '{@link #getShortForm() <em>Short Form</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getShortForm()
     */
    protected EObject shortForm;

    /**
     * The cached value of the '{@link #getLongForm() <em>Long Form</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLongForm()
     */
    protected EObject longForm;

    /**
     * The default value of the '{@link #getReference() <em>Reference</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getReference()
     */
    protected static final String REFERENCE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getReference() <em>Reference</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getReference()
     */
    protected String reference = REFERENCE_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected UOMTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getUOMType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getShortForm() {
        return shortForm;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetShortForm(EObject newShortForm, NotificationChain msgs) {
        EObject oldShortForm = shortForm;
        shortForm = newShortForm;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.UOM_TYPE__SHORT_FORM, oldShortForm, newShortForm);
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
    public void setShortForm(EObject newShortForm) {
        if (newShortForm != shortForm) {
            NotificationChain msgs = null;
            if (shortForm != null)
                msgs = ((InternalEObject) shortForm).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.UOM_TYPE__SHORT_FORM, null, msgs);
            if (newShortForm != null)
                msgs = ((InternalEObject) newShortForm).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.UOM_TYPE__SHORT_FORM, null, msgs);
            msgs = basicSetShortForm(newShortForm, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.UOM_TYPE__SHORT_FORM, newShortForm, newShortForm));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getLongForm() {
        return longForm;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetLongForm(EObject newLongForm, NotificationChain msgs) {
        EObject oldLongForm = longForm;
        longForm = newLongForm;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.UOM_TYPE__LONG_FORM, oldLongForm, newLongForm);
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
    public void setLongForm(EObject newLongForm) {
        if (newLongForm != longForm) {
            NotificationChain msgs = null;
            if (longForm != null)
                msgs = ((InternalEObject) longForm).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.UOM_TYPE__LONG_FORM, null, msgs);
            if (newLongForm != null)
                msgs = ((InternalEObject) newLongForm).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.UOM_TYPE__LONG_FORM, null, msgs);
            msgs = basicSetLongForm(newLongForm, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.UOM_TYPE__LONG_FORM, newLongForm, newLongForm));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getReference() {
        return reference;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setReference(String newReference) {
        String oldReference = reference;
        reference = newReference;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.UOM_TYPE__REFERENCE, oldReference, reference));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.UOM_TYPE__SHORT_FORM:
                return basicSetShortForm(null, msgs);
            case Tjs10Package.UOM_TYPE__LONG_FORM:
                return basicSetLongForm(null, msgs);
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
            case Tjs10Package.UOM_TYPE__SHORT_FORM:
                return getShortForm();
            case Tjs10Package.UOM_TYPE__LONG_FORM:
                return getLongForm();
            case Tjs10Package.UOM_TYPE__REFERENCE:
                return getReference();
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
            case Tjs10Package.UOM_TYPE__SHORT_FORM:
                setShortForm((EObject) newValue);
                return;
            case Tjs10Package.UOM_TYPE__LONG_FORM:
                setLongForm((EObject) newValue);
                return;
            case Tjs10Package.UOM_TYPE__REFERENCE:
                setReference((String) newValue);
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
            case Tjs10Package.UOM_TYPE__SHORT_FORM:
                setShortForm((EObject) null);
                return;
            case Tjs10Package.UOM_TYPE__LONG_FORM:
                setLongForm((EObject) null);
                return;
            case Tjs10Package.UOM_TYPE__REFERENCE:
                setReference(REFERENCE_EDEFAULT);
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
            case Tjs10Package.UOM_TYPE__SHORT_FORM:
                return shortForm != null;
            case Tjs10Package.UOM_TYPE__LONG_FORM:
                return longForm != null;
            case Tjs10Package.UOM_TYPE__REFERENCE:
                return REFERENCE_EDEFAULT == null ? reference != null : !REFERENCE_EDEFAULT.equals(reference);
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
        result.append(" (reference: ");
        result.append(reference);
        result.append(')');
        return result.toString();
    }

} //UOMTypeImpl
