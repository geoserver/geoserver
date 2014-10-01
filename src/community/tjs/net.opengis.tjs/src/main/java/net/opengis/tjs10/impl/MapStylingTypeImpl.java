/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.MapStylingType;
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
 * An implementation of the model object '<em><b>Map Styling Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.MapStylingTypeImpl#getStylingIdentifier <em>Styling Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.MapStylingTypeImpl#getStylingURL <em>Styling URL</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class MapStylingTypeImpl extends EObjectImpl implements MapStylingType {
    /**
     * The cached value of the '{@link #getStylingIdentifier() <em>Styling Identifier</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getStylingIdentifier()
     */
    protected EObject stylingIdentifier;

    /**
     * The default value of the '{@link #getStylingURL() <em>Styling URL</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getStylingURL()
     */
    protected static final String STYLING_URL_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getStylingURL() <em>Styling URL</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getStylingURL()
     */
    protected String stylingURL = STYLING_URL_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected MapStylingTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getMapStylingType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject getStylingIdentifier() {
        return stylingIdentifier;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetStylingIdentifier(EObject newStylingIdentifier, NotificationChain msgs) {
        EObject oldStylingIdentifier = stylingIdentifier;
        stylingIdentifier = newStylingIdentifier;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER, oldStylingIdentifier, newStylingIdentifier);
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
    public void setStylingIdentifier(EObject newStylingIdentifier) {
        if (newStylingIdentifier != stylingIdentifier) {
            NotificationChain msgs = null;
            if (stylingIdentifier != null)
                msgs = ((InternalEObject) stylingIdentifier).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER, null, msgs);
            if (newStylingIdentifier != null)
                msgs = ((InternalEObject) newStylingIdentifier).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER, null, msgs);
            msgs = basicSetStylingIdentifier(newStylingIdentifier, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER, newStylingIdentifier, newStylingIdentifier));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getStylingURL() {
        return stylingURL;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setStylingURL(String newStylingURL) {
        String oldStylingURL = stylingURL;
        stylingURL = newStylingURL;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.MAP_STYLING_TYPE__STYLING_URL, oldStylingURL, stylingURL));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER:
                return basicSetStylingIdentifier(null, msgs);
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
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER:
                return getStylingIdentifier();
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_URL:
                return getStylingURL();
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
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER:
                setStylingIdentifier((EObject) newValue);
                return;
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_URL:
                setStylingURL((String) newValue);
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
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER:
                setStylingIdentifier((EObject) null);
                return;
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_URL:
                setStylingURL(STYLING_URL_EDEFAULT);
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
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_IDENTIFIER:
                return stylingIdentifier != null;
            case Tjs10Package.MAP_STYLING_TYPE__STYLING_URL:
                return STYLING_URL_EDEFAULT == null ? stylingURL != null : !STYLING_URL_EDEFAULT.equals(stylingURL);
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
        result.append(" (stylingURL: ");
        result.append(stylingURL);
        result.append(')');
        return result.toString();
    }

} //MapStylingTypeImpl
