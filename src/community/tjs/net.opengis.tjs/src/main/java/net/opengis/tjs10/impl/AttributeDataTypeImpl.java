/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.AttributeDataType;
import net.opengis.tjs10.GetDataXMLType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Attribute Data Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.AttributeDataTypeImpl#getGetDataURL <em>Get Data URL</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.AttributeDataTypeImpl#getGetDataXML <em>Get Data XML</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class AttributeDataTypeImpl extends EObjectImpl implements AttributeDataType {
    /**
     * The default value of the '{@link #getGetDataURL() <em>Get Data URL</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGetDataURL()
     */
    protected static final String GET_DATA_URL_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getGetDataURL() <em>Get Data URL</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGetDataURL()
     */
    protected String getDataURL = GET_DATA_URL_EDEFAULT;

    /**
     * The cached value of the '{@link #getGetDataXML() <em>Get Data XML</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getGetDataXML()
     */
    protected GetDataXMLType getDataXML;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected AttributeDataTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getAttributeDataType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getGetDataURL() {
        return getDataURL;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGetDataURL(String newGetDataURL) {
        String oldGetDataURL = getDataURL;
        getDataURL = newGetDataURL;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_URL, oldGetDataURL, getDataURL));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataXMLType getGetDataXML() {
        return getDataXML;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetGetDataXML(GetDataXMLType newGetDataXML, NotificationChain msgs) {
        GetDataXMLType oldGetDataXML = getDataXML;
        getDataXML = newGetDataXML;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML, oldGetDataXML, newGetDataXML);
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
    public void setGetDataXML(GetDataXMLType newGetDataXML) {
        if (newGetDataXML != getDataXML) {
            NotificationChain msgs = null;
            if (getDataXML != null)
                msgs = ((InternalEObject) getDataXML).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML, null, msgs);
            if (newGetDataXML != null)
                msgs = ((InternalEObject) newGetDataXML).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML, null, msgs);
            msgs = basicSetGetDataXML(newGetDataXML, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML, newGetDataXML, newGetDataXML));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML:
                return basicSetGetDataXML(null, msgs);
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
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_URL:
                return getGetDataURL();
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML:
                return getGetDataXML();
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
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_URL:
                setGetDataURL((String) newValue);
                return;
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML:
                setGetDataXML((GetDataXMLType) newValue);
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
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_URL:
                setGetDataURL(GET_DATA_URL_EDEFAULT);
                return;
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML:
                setGetDataXML((GetDataXMLType) null);
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
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_URL:
                return GET_DATA_URL_EDEFAULT == null ? getDataURL != null : !GET_DATA_URL_EDEFAULT.equals(getDataURL);
            case Tjs10Package.ATTRIBUTE_DATA_TYPE__GET_DATA_XML:
                return getDataXML != null;
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
        result.append(" (getDataURL: ");
        result.append(getDataURL);
        result.append(')');
        return result.toString();
    }

} //AttributeDataTypeImpl
