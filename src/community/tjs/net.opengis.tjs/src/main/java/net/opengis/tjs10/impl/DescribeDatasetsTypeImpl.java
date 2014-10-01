/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.DescribeDatasetsType;
import net.opengis.tjs10.Tjs10Package;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Describe Datasets Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.DescribeDatasetsTypeImpl#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeDatasetsTypeImpl#getDatasetURI <em>Dataset URI</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DescribeDatasetsTypeImpl extends RequestBaseTypeImpl implements DescribeDatasetsType {
    /**
     * The default value of the '{@link #getFrameworkURI() <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFrameworkURI()
     */
    protected static final String FRAMEWORK_URI_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getFrameworkURI() <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFrameworkURI()
     */
    protected String frameworkURI = FRAMEWORK_URI_EDEFAULT;

    /**
     * The default value of the '{@link #getDatasetURI() <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDatasetURI()
     */
    protected static final String DATASET_URI_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getDatasetURI() <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDatasetURI()
     */
    protected String datasetURI = DATASET_URI_EDEFAULT;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected DescribeDatasetsTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getDescribeDatasetsType();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getFrameworkURI() {
        return frameworkURI;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFrameworkURI(String newFrameworkURI) {
        String oldFrameworkURI = frameworkURI;
        frameworkURI = newFrameworkURI;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI, oldFrameworkURI, frameworkURI));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getDatasetURI() {
        return datasetURI;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDatasetURI(String newDatasetURI) {
        String oldDatasetURI = datasetURI;
        datasetURI = newDatasetURI;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_DATASETS_TYPE__DATASET_URI, oldDatasetURI, datasetURI));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI:
                return getFrameworkURI();
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__DATASET_URI:
                return getDatasetURI();
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
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI:
                setFrameworkURI((String) newValue);
                return;
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__DATASET_URI:
                setDatasetURI((String) newValue);
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
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI:
                setFrameworkURI(FRAMEWORK_URI_EDEFAULT);
                return;
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__DATASET_URI:
                setDatasetURI(DATASET_URI_EDEFAULT);
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
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI:
                return FRAMEWORK_URI_EDEFAULT == null ? frameworkURI != null : !FRAMEWORK_URI_EDEFAULT.equals(frameworkURI);
            case Tjs10Package.DESCRIBE_DATASETS_TYPE__DATASET_URI:
                return DATASET_URI_EDEFAULT == null ? datasetURI != null : !DATASET_URI_EDEFAULT.equals(datasetURI);
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
        result.append(" (frameworkURI: ");
        result.append(frameworkURI);
        result.append(", datasetURI: ");
        result.append(datasetURI);
        result.append(')');
        return result.toString();
    }

} //DescribeDatasetsTypeImpl
