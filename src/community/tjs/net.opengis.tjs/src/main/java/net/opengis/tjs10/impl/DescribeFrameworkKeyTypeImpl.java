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
 * An implementation of the model object '<em><b>Describe Framework Key Type</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getVersion <em>Version</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getBoundingCoordinates <em>Bounding Coordinates</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl#getRowset <em>Rowset</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DescribeFrameworkKeyTypeImpl extends EObjectImpl implements DescribeFrameworkKeyType {
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
     * The cached value of the '{@link #getRowset() <em>Rowset</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getRowset()
     */
    protected RowsetType rowset;

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected DescribeFrameworkKeyTypeImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getDescribeFrameworkKeyType();
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI, oldFrameworkURI, frameworkURI));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION, oldOrganization, organization));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE, oldTitle, title));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT, oldAbstract, newAbstract);
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
                msgs = ((InternalEObject) abstract_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT, null, msgs);
            if (newAbstract != null)
                msgs = ((InternalEObject) newAbstract).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT, null, msgs);
            msgs = basicSetAbstract(newAbstract, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT, newAbstract, newAbstract));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE, oldReferenceDate, newReferenceDate);
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
                msgs = ((InternalEObject) referenceDate).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE, null, msgs);
            if (newReferenceDate != null)
                msgs = ((InternalEObject) newReferenceDate).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE, null, msgs);
            msgs = basicSetReferenceDate(newReferenceDate, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE, newReferenceDate, newReferenceDate));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION, oldVersion, version));
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
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION, oldDocumentation, documentation));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY, oldFrameworkKey, newFrameworkKey);
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
                msgs = ((InternalEObject) frameworkKey).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY, null, msgs);
            if (newFrameworkKey != null)
                msgs = ((InternalEObject) newFrameworkKey).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY, null, msgs);
            msgs = basicSetFrameworkKey(newFrameworkKey, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY, newFrameworkKey, newFrameworkKey));
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
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES, oldBoundingCoordinates, newBoundingCoordinates);
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
                msgs = ((InternalEObject) boundingCoordinates).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES, null, msgs);
            if (newBoundingCoordinates != null)
                msgs = ((InternalEObject) newBoundingCoordinates).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES, null, msgs);
            msgs = basicSetBoundingCoordinates(newBoundingCoordinates, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES, newBoundingCoordinates, newBoundingCoordinates));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RowsetType getRowset() {
        return rowset;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetRowset(RowsetType newRowset, NotificationChain msgs) {
        RowsetType oldRowset = rowset;
        rowset = newRowset;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET, oldRowset, newRowset);
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
    public void setRowset(RowsetType newRowset) {
        if (newRowset != rowset) {
            NotificationChain msgs = null;
            if (rowset != null)
                msgs = ((InternalEObject) rowset).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET, null, msgs);
            if (newRowset != null)
                msgs = ((InternalEObject) newRowset).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET, null, msgs);
            msgs = basicSetRowset(newRowset, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET, Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET, newRowset, newRowset));
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT:
                return basicSetAbstract(null, msgs);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE:
                return basicSetReferenceDate(null, msgs);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY:
                return basicSetFrameworkKey(null, msgs);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES:
                return basicSetBoundingCoordinates(null, msgs);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET:
                return basicSetRowset(null, msgs);
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
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI:
                return getFrameworkURI();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION:
                return getOrganization();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE:
                return getTitle();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT:
                return getAbstract();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE:
                return getReferenceDate();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION:
                return getVersion();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION:
                return getDocumentation();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY:
                return getFrameworkKey();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES:
                return getBoundingCoordinates();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET:
                return getRowset();
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
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI:
                setFrameworkURI((String) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION:
                setOrganization((String) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE:
                setTitle((String) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT:
                setAbstract((AbstractType) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION:
                setVersion((String) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION:
                setDocumentation((String) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES:
                setBoundingCoordinates((BoundingCoordinatesType) newValue);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET:
                setRowset((RowsetType) newValue);
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
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI:
                setFrameworkURI(FRAMEWORK_URI_EDEFAULT);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION:
                setOrganization(ORGANIZATION_EDEFAULT);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE:
                setTitle(TITLE_EDEFAULT);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT:
                setAbstract((AbstractType) null);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) null);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION:
                setVersion(VERSION_EDEFAULT);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION:
                setDocumentation(DOCUMENTATION_EDEFAULT);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType) null);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES:
                setBoundingCoordinates((BoundingCoordinatesType) null);
                return;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET:
                setRowset((RowsetType) null);
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
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI:
                return FRAMEWORK_URI_EDEFAULT == null ? frameworkURI != null : !FRAMEWORK_URI_EDEFAULT.equals(frameworkURI);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION:
                return ORGANIZATION_EDEFAULT == null ? organization != null : !ORGANIZATION_EDEFAULT.equals(organization);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE:
                return TITLE_EDEFAULT == null ? title != null : !TITLE_EDEFAULT.equals(title);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT:
                return abstract_ != null;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE:
                return referenceDate != null;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION:
                return VERSION_EDEFAULT == null ? version != null : !VERSION_EDEFAULT.equals(version);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION:
                return DOCUMENTATION_EDEFAULT == null ? documentation != null : !DOCUMENTATION_EDEFAULT.equals(documentation);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY:
                return frameworkKey != null;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES:
                return boundingCoordinates != null;
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET:
                return rowset != null;
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

} //DescribeFrameworkKeyTypeImpl
