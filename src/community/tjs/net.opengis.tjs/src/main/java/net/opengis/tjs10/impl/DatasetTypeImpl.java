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
 * An implementation of the model object '<em><b>Dataset Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getVersion <em>Version</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DatasetTypeImpl#getDescribeDataRequest <em>Describe Data Request</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DatasetTypeImpl extends EObjectImpl implements DatasetType {
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
     * The default value of the '{@link #getOrganization() <em>Organization</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getOrganization()
     */
    protected static final String ORGANIZATION_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getOrganization() <em>Organization</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getOrganization()
     */
    protected String organization = ORGANIZATION_EDEFAULT;

    /**
     * The default value of the '{@link #getTitle() <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getTitle()
     */
    protected static final String TITLE_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getTitle() <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getTitle()
     */
    protected String title = TITLE_EDEFAULT;

    /**
     * The cached value of the '{@link #getAbstract() <em>Abstract</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAbstract()
     */
    protected AbstractType abstract_;

    /**
     * The cached value of the '{@link #getReferenceDate() <em>Reference Date</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getReferenceDate()
     */
    protected ReferenceDateType referenceDate;

    /**
     * The default value of the '{@link #getVersion() <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getVersion()
     */
    protected static final String VERSION_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getVersion() <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getVersion()
     */
    protected String version = VERSION_EDEFAULT;

    /**
     * The default value of the '{@link #getDocumentation() <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDocumentation()
     */
    protected static final String DOCUMENTATION_EDEFAULT = null;

    /**
     * The cached value of the '{@link #getDocumentation() <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDocumentation()
     */
    protected String documentation = DOCUMENTATION_EDEFAULT;

    /**
     * The cached value of the '{@link #getDescribeDataRequest() <em>Describe Data Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDescribeDataRequest()
     */
    protected DescribeDataRequestType describeDataRequest;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected DatasetTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getDatasetType();
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__DATASET_URI, oldDatasetURI, datasetURI));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setOrganization(String newOrganization) {
        String oldOrganization = organization;
        organization = newOrganization;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__ORGANIZATION, oldOrganization, organization));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getTitle() {
        return title;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setTitle(String newTitle) {
        String oldTitle = title;
        title = newTitle;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__TITLE, oldTitle, title));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AbstractType getAbstract() {
        return abstract_;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAbstract(AbstractType newAbstract, NotificationChain msgs) {
        AbstractType oldAbstract = abstract_;
        abstract_ = newAbstract;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__ABSTRACT, oldAbstract, newAbstract);
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
    public void setAbstract(AbstractType newAbstract) {
        if (newAbstract != abstract_) {
            NotificationChain msgs = null;
            if (abstract_ != null)
                msgs = ((InternalEObject) abstract_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATASET_TYPE__ABSTRACT, null, msgs);
            if (newAbstract != null)
                msgs = ((InternalEObject) newAbstract).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATASET_TYPE__ABSTRACT, null, msgs);
            msgs = basicSetAbstract(newAbstract, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__ABSTRACT, newAbstract, newAbstract));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ReferenceDateType getReferenceDate() {
        return referenceDate;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetReferenceDate(ReferenceDateType newReferenceDate, NotificationChain msgs) {
        ReferenceDateType oldReferenceDate = referenceDate;
        referenceDate = newReferenceDate;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__REFERENCE_DATE, oldReferenceDate, newReferenceDate);
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
    public void setReferenceDate(ReferenceDateType newReferenceDate) {
        if (newReferenceDate != referenceDate) {
            NotificationChain msgs = null;
            if (referenceDate != null)
                msgs = ((InternalEObject) referenceDate).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATASET_TYPE__REFERENCE_DATE, null, msgs);
            if (newReferenceDate != null)
                msgs = ((InternalEObject) newReferenceDate).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATASET_TYPE__REFERENCE_DATE, null, msgs);
            msgs = basicSetReferenceDate(newReferenceDate, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__REFERENCE_DATE, newReferenceDate, newReferenceDate));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getVersion() {
        return version;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setVersion(String newVersion) {
        String oldVersion = version;
        version = newVersion;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__VERSION, oldVersion, version));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDocumentation(String newDocumentation) {
        String oldDocumentation = documentation;
        documentation = newDocumentation;
        if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__DOCUMENTATION, oldDocumentation, documentation));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataRequestType getDescribeDataRequest() {
        return describeDataRequest;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeDataRequest(DescribeDataRequestType newDescribeDataRequest, NotificationChain msgs) {
        DescribeDataRequestType oldDescribeDataRequest = describeDataRequest;
        describeDataRequest = newDescribeDataRequest;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST, oldDescribeDataRequest, newDescribeDataRequest);
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
    public void setDescribeDataRequest(DescribeDataRequestType newDescribeDataRequest) {
        if (newDescribeDataRequest != describeDataRequest) {
            NotificationChain msgs = null;
            if (describeDataRequest != null)
                msgs = ((InternalEObject) describeDataRequest).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST, null, msgs);
            if (newDescribeDataRequest != null)
                msgs = ((InternalEObject) newDescribeDataRequest).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST, null, msgs);
            msgs = basicSetDescribeDataRequest(newDescribeDataRequest, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST, newDescribeDataRequest, newDescribeDataRequest));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.DATASET_TYPE__ABSTRACT:
                return basicSetAbstract(null, msgs);
            case Tjs10Package.DATASET_TYPE__REFERENCE_DATE:
                return basicSetReferenceDate(null, msgs);
            case Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST:
                return basicSetDescribeDataRequest(null, msgs);
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
            case Tjs10Package.DATASET_TYPE__DATASET_URI:
                return getDatasetURI();
            case Tjs10Package.DATASET_TYPE__ORGANIZATION:
                return getOrganization();
            case Tjs10Package.DATASET_TYPE__TITLE:
                return getTitle();
            case Tjs10Package.DATASET_TYPE__ABSTRACT:
                return getAbstract();
            case Tjs10Package.DATASET_TYPE__REFERENCE_DATE:
                return getReferenceDate();
            case Tjs10Package.DATASET_TYPE__VERSION:
                return getVersion();
            case Tjs10Package.DATASET_TYPE__DOCUMENTATION:
                return getDocumentation();
            case Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST:
                return getDescribeDataRequest();
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
            case Tjs10Package.DATASET_TYPE__DATASET_URI:
                setDatasetURI((String) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__ORGANIZATION:
                setOrganization((String) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__TITLE:
                setTitle((String) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__ABSTRACT:
                setAbstract((AbstractType) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__VERSION:
                setVersion((String) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__DOCUMENTATION:
                setDocumentation((String) newValue);
                return;
            case Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST:
                setDescribeDataRequest((DescribeDataRequestType) newValue);
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
            case Tjs10Package.DATASET_TYPE__DATASET_URI:
                setDatasetURI(DATASET_URI_EDEFAULT);
                return;
            case Tjs10Package.DATASET_TYPE__ORGANIZATION:
                setOrganization(ORGANIZATION_EDEFAULT);
                return;
            case Tjs10Package.DATASET_TYPE__TITLE:
                setTitle(TITLE_EDEFAULT);
                return;
            case Tjs10Package.DATASET_TYPE__ABSTRACT:
                setAbstract((AbstractType) null);
                return;
            case Tjs10Package.DATASET_TYPE__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) null);
                return;
            case Tjs10Package.DATASET_TYPE__VERSION:
                setVersion(VERSION_EDEFAULT);
                return;
            case Tjs10Package.DATASET_TYPE__DOCUMENTATION:
                setDocumentation(DOCUMENTATION_EDEFAULT);
                return;
            case Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST:
                setDescribeDataRequest((DescribeDataRequestType) null);
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
            case Tjs10Package.DATASET_TYPE__DATASET_URI:
                return DATASET_URI_EDEFAULT == null ? datasetURI != null : !DATASET_URI_EDEFAULT.equals(datasetURI);
            case Tjs10Package.DATASET_TYPE__ORGANIZATION:
                return ORGANIZATION_EDEFAULT == null ? organization != null : !ORGANIZATION_EDEFAULT.equals(organization);
            case Tjs10Package.DATASET_TYPE__TITLE:
                return TITLE_EDEFAULT == null ? title != null : !TITLE_EDEFAULT.equals(title);
            case Tjs10Package.DATASET_TYPE__ABSTRACT:
                return abstract_ != null;
            case Tjs10Package.DATASET_TYPE__REFERENCE_DATE:
                return referenceDate != null;
            case Tjs10Package.DATASET_TYPE__VERSION:
                return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
            case Tjs10Package.DATASET_TYPE__DOCUMENTATION:
                return DOCUMENTATION_EDEFAULT == null ? documentation != null : !DOCUMENTATION_EDEFAULT.equals(documentation);
            case Tjs10Package.DATASET_TYPE__DESCRIBE_DATA_REQUEST:
                return describeDataRequest != null;
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
        result.append(" (datasetURI: ");
        result.append(datasetURI);
        result.append(", organization: ");
        result.append(organization);
        result.append(", title: ");
        result.append(title);
        result.append(", version: ");
        result.append(version);
        result.append(", documentation: ");
        result.append(documentation);
        result.append(')');
        return result.toString();
    }

} //DatasetTypeImpl
