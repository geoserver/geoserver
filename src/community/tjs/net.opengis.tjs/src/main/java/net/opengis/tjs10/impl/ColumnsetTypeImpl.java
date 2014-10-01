/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.AttributesType;
import net.opengis.tjs10.ColumnsetType;
import net.opengis.tjs10.FrameworkKeyType1;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Columnset Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.ColumnsetTypeImpl#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.ColumnsetTypeImpl#getAttributes <em>Attributes</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ColumnsetTypeImpl extends EObjectImpl implements ColumnsetType {
    /**
     * The cached value of the '{@link #getFrameworkKey() <em>Framework Key</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFrameworkKey()
     */
    protected FrameworkKeyType1 frameworkKey;

    /**
     * The cached value of the '{@link #getAttributes() <em>Attributes</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributes()
     */
    protected AttributesType attributes;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected ColumnsetTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getColumnsetType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyType1 getFrameworkKey() {
        return frameworkKey;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFrameworkKey(FrameworkKeyType1 newFrameworkKey, NotificationChain msgs) {
        FrameworkKeyType1 oldFrameworkKey = frameworkKey;
        frameworkKey = newFrameworkKey;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY, oldFrameworkKey, newFrameworkKey);
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
    public void setFrameworkKey(FrameworkKeyType1 newFrameworkKey) {
        if (newFrameworkKey != frameworkKey) {
            NotificationChain msgs = null;
            if (frameworkKey != null)
                msgs = ((InternalEObject) frameworkKey).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY, null, msgs);
            if (newFrameworkKey != null)
                msgs = ((InternalEObject) newFrameworkKey).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY, null, msgs);
            msgs = basicSetFrameworkKey(newFrameworkKey, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY, newFrameworkKey, newFrameworkKey));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AttributesType getAttributes() {
        return attributes;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAttributes(AttributesType newAttributes, NotificationChain msgs) {
        AttributesType oldAttributes = attributes;
        attributes = newAttributes;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES, oldAttributes, newAttributes);
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
    public void setAttributes(AttributesType newAttributes) {
        if (newAttributes != attributes) {
            NotificationChain msgs = null;
            if (attributes != null)
                msgs = ((InternalEObject) attributes).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES, null, msgs);
            if (newAttributes != null)
                msgs = ((InternalEObject) newAttributes).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES, null, msgs);
            msgs = basicSetAttributes(newAttributes, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES, newAttributes, newAttributes));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY:
                return basicSetFrameworkKey(null, msgs);
            case Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES:
                return basicSetAttributes(null, msgs);
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
            case Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY:
                return getFrameworkKey();
            case Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES:
                return getAttributes();
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
            case Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType1) newValue);
                return;
            case Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES:
                setAttributes((AttributesType) newValue);
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
            case Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType1) null);
                return;
            case Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES:
                setAttributes((AttributesType) null);
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
            case Tjs10Package.COLUMNSET_TYPE__FRAMEWORK_KEY:
                return frameworkKey != null;
            case Tjs10Package.COLUMNSET_TYPE__ATTRIBUTES:
                return attributes != null;
        }
        return super.eIsSet(featureID);
    }

} //ColumnsetTypeImpl
