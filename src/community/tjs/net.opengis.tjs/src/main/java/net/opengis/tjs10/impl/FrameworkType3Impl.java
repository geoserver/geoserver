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
 * An implementation of the model object '<em><b>Framework Type3</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getVersion <em>Version</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getBoundingCoordinates <em>Bounding Coordinates</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.FrameworkType3Impl#getDataset <em>Dataset</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class FrameworkType3Impl extends EObjectImpl implements FrameworkType3 {
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
     * The cached value of the '{@link #getFrameworkKey() <em>Framework Key</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getFrameworkKey()
     */
    protected FrameworkKeyType frameworkKey;

    /**
     * The cached value of the '{@link #getBoundingCoordinates() <em>Bounding Coordinates</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getBoundingCoordinates()
     */
    protected BoundingCoordinatesType boundingCoordinates;

    /**
     * The cached value of the '{@link #getDescribeDatasetsRequest() <em>Describe Datasets Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDescribeDatasetsRequest()
     */
    protected DescribeDatasetsRequestType describeDatasetsRequest;

    /**
     * The cached value of the '{@link #getDataset() <em>Dataset</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDataset()
     */
    protected DatasetType2 dataset;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected FrameworkType3Impl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getFrameworkType3();
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_URI, oldFrameworkURI, frameworkURI));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__ORGANIZATION, oldOrganization, organization));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__TITLE, oldTitle, title));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT, oldAbstract, newAbstract);
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
                msgs = ((InternalEObject) abstract_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT, null, msgs);
            if (newAbstract != null)
                msgs = ((InternalEObject) newAbstract).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT, null, msgs);
            msgs = basicSetAbstract(newAbstract, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT, newAbstract, newAbstract));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE, oldReferenceDate, newReferenceDate);
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
                msgs = ((InternalEObject) referenceDate).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE, null, msgs);
            if (newReferenceDate != null)
                msgs = ((InternalEObject) newReferenceDate).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE, null, msgs);
            msgs = basicSetReferenceDate(newReferenceDate, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE, newReferenceDate, newReferenceDate));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__VERSION, oldVersion, version));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__DOCUMENTATION, oldDocumentation, documentation));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyType getFrameworkKey() {
        return frameworkKey;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFrameworkKey(FrameworkKeyType newFrameworkKey, NotificationChain msgs) {
        FrameworkKeyType oldFrameworkKey = frameworkKey;
        frameworkKey = newFrameworkKey;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY, oldFrameworkKey, newFrameworkKey);
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
    public void setFrameworkKey(FrameworkKeyType newFrameworkKey) {
        if (newFrameworkKey != frameworkKey) {
            NotificationChain msgs = null;
            if (frameworkKey != null)
                msgs = ((InternalEObject) frameworkKey).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY, null, msgs);
            if (newFrameworkKey != null)
                msgs = ((InternalEObject) newFrameworkKey).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY, null, msgs);
            msgs = basicSetFrameworkKey(newFrameworkKey, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY, newFrameworkKey, newFrameworkKey));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BoundingCoordinatesType getBoundingCoordinates() {
        return boundingCoordinates;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetBoundingCoordinates(BoundingCoordinatesType newBoundingCoordinates, NotificationChain msgs) {
        BoundingCoordinatesType oldBoundingCoordinates = boundingCoordinates;
        boundingCoordinates = newBoundingCoordinates;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES, oldBoundingCoordinates, newBoundingCoordinates);
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
    public void setBoundingCoordinates(BoundingCoordinatesType newBoundingCoordinates) {
        if (newBoundingCoordinates != boundingCoordinates) {
            NotificationChain msgs = null;
            if (boundingCoordinates != null)
                msgs = ((InternalEObject) boundingCoordinates).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES, null, msgs);
            if (newBoundingCoordinates != null)
                msgs = ((InternalEObject) newBoundingCoordinates).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES, null, msgs);
            msgs = basicSetBoundingCoordinates(newBoundingCoordinates, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES, newBoundingCoordinates, newBoundingCoordinates));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsRequestType getDescribeDatasetsRequest() {
        return describeDatasetsRequest;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeDatasetsRequest(DescribeDatasetsRequestType newDescribeDatasetsRequest, NotificationChain msgs) {
        DescribeDatasetsRequestType oldDescribeDatasetsRequest = describeDatasetsRequest;
        describeDatasetsRequest = newDescribeDatasetsRequest;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST, oldDescribeDatasetsRequest, newDescribeDatasetsRequest);
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
    public void setDescribeDatasetsRequest(DescribeDatasetsRequestType newDescribeDatasetsRequest) {
        if (newDescribeDatasetsRequest != describeDatasetsRequest) {
            NotificationChain msgs = null;
            if (describeDatasetsRequest != null)
                msgs = ((InternalEObject) describeDatasetsRequest).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST, null, msgs);
            if (newDescribeDatasetsRequest != null)
                msgs = ((InternalEObject) newDescribeDatasetsRequest).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST, null, msgs);
            msgs = basicSetDescribeDatasetsRequest(newDescribeDatasetsRequest, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST, newDescribeDatasetsRequest, newDescribeDatasetsRequest));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetType2 getDataset() {
        return dataset;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDataset(DatasetType2 newDataset, NotificationChain msgs) {
        DatasetType2 oldDataset = dataset;
        dataset = newDataset;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__DATASET, oldDataset, newDataset);
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
    public void setDataset(DatasetType2 newDataset) {
        if (newDataset != dataset) {
            NotificationChain msgs = null;
            if (dataset != null)
                msgs = ((InternalEObject) dataset).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__DATASET, null, msgs);
            if (newDataset != null)
                msgs = ((InternalEObject) newDataset).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.FRAMEWORK_TYPE3__DATASET, null, msgs);
            msgs = basicSetDataset(newDataset, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.FRAMEWORK_TYPE3__DATASET, newDataset, newDataset));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT:
                return basicSetAbstract(null, msgs);
            case Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE:
                return basicSetReferenceDate(null, msgs);
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY:
                return basicSetFrameworkKey(null, msgs);
            case Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES:
                return basicSetBoundingCoordinates(null, msgs);
            case Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST:
                return basicSetDescribeDatasetsRequest(null, msgs);
            case Tjs10Package.FRAMEWORK_TYPE3__DATASET:
                return basicSetDataset(null, msgs);
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
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_URI:
                return getFrameworkURI();
            case Tjs10Package.FRAMEWORK_TYPE3__ORGANIZATION:
                return getOrganization();
            case Tjs10Package.FRAMEWORK_TYPE3__TITLE:
                return getTitle();
            case Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT:
                return getAbstract();
            case Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE:
                return getReferenceDate();
            case Tjs10Package.FRAMEWORK_TYPE3__VERSION:
                return getVersion();
            case Tjs10Package.FRAMEWORK_TYPE3__DOCUMENTATION:
                return getDocumentation();
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY:
                return getFrameworkKey();
            case Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES:
                return getBoundingCoordinates();
            case Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST:
                return getDescribeDatasetsRequest();
            case Tjs10Package.FRAMEWORK_TYPE3__DATASET:
                return getDataset();
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
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_URI:
                setFrameworkURI((String) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__ORGANIZATION:
                setOrganization((String) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__TITLE:
                setTitle((String) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT:
                setAbstract((AbstractType) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__VERSION:
                setVersion((String) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__DOCUMENTATION:
                setDocumentation((String) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES:
                setBoundingCoordinates((BoundingCoordinatesType) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST:
                setDescribeDatasetsRequest((DescribeDatasetsRequestType) newValue);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__DATASET:
                setDataset((DatasetType2) newValue);
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
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_URI:
                setFrameworkURI(FRAMEWORK_URI_EDEFAULT);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__ORGANIZATION:
                setOrganization(ORGANIZATION_EDEFAULT);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__TITLE:
                setTitle(TITLE_EDEFAULT);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT:
                setAbstract((AbstractType) null);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) null);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__VERSION:
                setVersion(VERSION_EDEFAULT);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__DOCUMENTATION:
                setDocumentation(DOCUMENTATION_EDEFAULT);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType) null);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES:
                setBoundingCoordinates((BoundingCoordinatesType) null);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST:
                setDescribeDatasetsRequest((DescribeDatasetsRequestType) null);
                return;
            case Tjs10Package.FRAMEWORK_TYPE3__DATASET:
                setDataset((DatasetType2) null);
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
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_URI:
                return FRAMEWORK_URI_EDEFAULT == null ? frameworkURI != null : !FRAMEWORK_URI_EDEFAULT.equals(frameworkURI);
            case Tjs10Package.FRAMEWORK_TYPE3__ORGANIZATION:
                return ORGANIZATION_EDEFAULT == null ? organization != null : !ORGANIZATION_EDEFAULT.equals(organization);
            case Tjs10Package.FRAMEWORK_TYPE3__TITLE:
                return TITLE_EDEFAULT == null ? title != null : !TITLE_EDEFAULT.equals(title);
            case Tjs10Package.FRAMEWORK_TYPE3__ABSTRACT:
                return abstract_ != null;
            case Tjs10Package.FRAMEWORK_TYPE3__REFERENCE_DATE:
                return referenceDate != null;
            case Tjs10Package.FRAMEWORK_TYPE3__VERSION:
                return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
            case Tjs10Package.FRAMEWORK_TYPE3__DOCUMENTATION:
                return DOCUMENTATION_EDEFAULT == null ? documentation != null : !DOCUMENTATION_EDEFAULT.equals(documentation);
            case Tjs10Package.FRAMEWORK_TYPE3__FRAMEWORK_KEY:
                return frameworkKey != null;
            case Tjs10Package.FRAMEWORK_TYPE3__BOUNDING_COORDINATES:
                return boundingCoordinates != null;
            case Tjs10Package.FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST:
                return describeDatasetsRequest != null;
            case Tjs10Package.FRAMEWORK_TYPE3__DATASET:
                return dataset != null;
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

} //FrameworkType3Impl
