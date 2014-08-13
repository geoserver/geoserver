/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.*;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.eclipse.emf.ecore.util.BasicFeatureMap;
import org.eclipse.emf.ecore.util.EcoreEMap;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Document Root</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getMixed <em>Mixed</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getXMLNSPrefixMap <em>XMLNS Prefix Map</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getXSISchemaLocation <em>XSI Schema Location</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getAttributeLimit <em>Attribute Limit</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getAttributes <em>Attributes</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getBoundingCoordinates <em>Bounding Coordinates</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getColumnset <em>Columnset</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getCount <em>Count</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDataClass <em>Data Class</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDataDescriptions <em>Data Descriptions</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDataset <em>Dataset</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDatasetDescriptions <em>Dataset Descriptions</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeData <em>Describe Data</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeDataRequest <em>Describe Data Request</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeDatasets <em>Describe Datasets</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeFrameworks <em>Describe Frameworks</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeJoinAbilities <em>Describe Join Abilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDescribeKey <em>Describe Key</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getFramework <em>Framework</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getFrameworkDescriptions <em>Framework Descriptions</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getFrameworkKeyDescription <em>Framework Key Description</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getGDAS <em>GDAS</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getGetCapabilities <em>Get Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getGetData <em>Get Data</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getGetDataRequest <em>Get Data Request</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getIdentifier <em>Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getJoinAbilities <em>Join Abilities</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getJoinData <em>Join Data</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getJoinDataResponse <em>Join Data Response</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getK <em>K</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getLinkageKeys <em>Linkage Keys</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getMeasure <em>Measure</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getMechanism <em>Mechanism</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getNominal <em>Nominal</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getOrdinal <em>Ordinal</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getRowset <em>Rowset</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getSpatialFrameworks <em>Spatial Frameworks</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getStyling <em>Styling</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getUncertainty <em>Uncertainty</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getUOM <em>UOM</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getValues <em>Values</em>}</li>
 * <li>{@link net.opengis.tjs10.impl.DocumentRootImpl#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class DocumentRootImpl extends EObjectImpl implements DocumentRoot {
    /**
     * The cached value of the '{@link #getMixed() <em>Mixed</em>}' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getMixed()
     */
    protected FeatureMap mixed;

    /**
     * The cached value of the '{@link #getXMLNSPrefixMap() <em>XMLNS Prefix Map</em>}' map.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getXMLNSPrefixMap()
     */
    protected EMap xMLNSPrefixMap;

    /**
     * The cached value of the '{@link #getXSISchemaLocation() <em>XSI Schema Location</em>}' map.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getXSISchemaLocation()
     */
    protected EMap xSISchemaLocation;

    /**
     * The default value of the '{@link #getAttributeLimit() <em>Attribute Limit</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributeLimit()
     */
    protected static final BigInteger ATTRIBUTE_LIMIT_EDEFAULT = null;

    /**
     * The default value of the '{@link #getAttributes() <em>Attributes</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getAttributes()
     */
    protected static final String ATTRIBUTES_EDEFAULT = null;

    /**
     * The default value of the '{@link #getDataClass() <em>Data Class</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getDataClass()
     */
    protected static final DataClassType DATA_CLASS_EDEFAULT = DataClassType.NOMINAL_LITERAL;

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
     * The default value of the '{@link #getIdentifier() <em>Identifier</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getIdentifier()
     */
    protected static final String IDENTIFIER_EDEFAULT = null;

    /**
     * The default value of the '{@link #getLinkageKeys() <em>Linkage Keys</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     * @see #getLinkageKeys()
     */
    protected static final String LINKAGE_KEYS_EDEFAULT = null;

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
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected DocumentRootImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EClass eStaticClass() {
        return Tjs10Package.eINSTANCE.getDocumentRoot();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FeatureMap getMixed() {
        if (mixed == null) {
            mixed = new BasicFeatureMap(this, Tjs10Package.DOCUMENT_ROOT__MIXED);
        }
        return mixed;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EMap getXMLNSPrefixMap() {
        if (xMLNSPrefixMap == null) {
            xMLNSPrefixMap = new EcoreEMap(EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY, EStringToStringMapEntryImpl.class, this, Tjs10Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP);
        }
        return xMLNSPrefixMap;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EMap getXSISchemaLocation() {
        if (xSISchemaLocation == null) {
            xSISchemaLocation = new EcoreEMap(EcorePackage.Literals.ESTRING_TO_STRING_MAP_ENTRY, EStringToStringMapEntryImpl.class, this, Tjs10Package.DOCUMENT_ROOT__XSI_SCHEMA_LOCATION);
        }
        return xSISchemaLocation;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AbstractType getAbstract() {
        return (AbstractType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Abstract(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetAbstract(AbstractType newAbstract, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Abstract(), newAbstract, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAbstract(AbstractType newAbstract) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Abstract(), newAbstract);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BigInteger getAttributeLimit() {
        return (BigInteger) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_AttributeLimit(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAttributeLimit(BigInteger newAttributeLimit) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_AttributeLimit(), newAttributeLimit);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getAttributes() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Attributes(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setAttributes(String newAttributes) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Attributes(), newAttributes);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BoundingCoordinatesType getBoundingCoordinates() {
        return (BoundingCoordinatesType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_BoundingCoordinates(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetBoundingCoordinates(BoundingCoordinatesType newBoundingCoordinates, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_BoundingCoordinates(), newBoundingCoordinates, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setBoundingCoordinates(BoundingCoordinatesType newBoundingCoordinates) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_BoundingCoordinates(), newBoundingCoordinates);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public TjsCapabilitiesType getCapabilities() {
        return (TjsCapabilitiesType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Capabilities(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetCapabilities(TjsCapabilitiesType newCapabilities, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Capabilities(), newCapabilities, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setCapabilities(TjsCapabilitiesType newCapabilities) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Capabilities(), newCapabilities);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ColumnsetType getColumnset() {
        return (ColumnsetType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Columnset(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetColumnset(ColumnsetType newColumnset, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Columnset(), newColumnset, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setColumnset(ColumnsetType newColumnset) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Columnset(), newColumnset);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public CountType getCount() {
        return (CountType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Count(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetCount(CountType newCount, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Count(), newCount, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setCount(CountType newCount) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Count(), newCount);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataClassType getDataClass() {
        return (DataClassType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DataClass(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDataClass(DataClassType newDataClass) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DataClass(), newDataClass);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataDescriptionsType getDataDescriptions() {
        return (DataDescriptionsType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DataDescriptions(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDataDescriptions(DataDescriptionsType newDataDescriptions, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DataDescriptions(), newDataDescriptions, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDataDescriptions(DataDescriptionsType newDataDescriptions) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DataDescriptions(), newDataDescriptions);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetType1 getDataset() {
        return (DatasetType1) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Dataset(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDataset(DatasetType1 newDataset, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Dataset(), newDataset, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDataset(DatasetType1 newDataset) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Dataset(), newDataset);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetDescriptionsType getDatasetDescriptions() {
        return (DatasetDescriptionsType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DatasetDescriptions(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDatasetDescriptions(DatasetDescriptionsType newDatasetDescriptions, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DatasetDescriptions(), newDatasetDescriptions, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDatasetDescriptions(DatasetDescriptionsType newDatasetDescriptions) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DatasetDescriptions(), newDatasetDescriptions);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getDatasetURI() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DatasetURI(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDatasetURI(String newDatasetURI) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DatasetURI(), newDatasetURI);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataType getDescribeData() {
        return (DescribeDataType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeData(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeData(DescribeDataType newDescribeData, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeData(), newDescribeData, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeData(DescribeDataType newDescribeData) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeData(), newDescribeData);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataRequestType getDescribeDataRequest() {
        return (DescribeDataRequestType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDataRequest(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeDataRequest(DescribeDataRequestType newDescribeDataRequest, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDataRequest(), newDescribeDataRequest, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeDataRequest(DescribeDataRequestType newDescribeDataRequest) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDataRequest(), newDescribeDataRequest);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsType getDescribeDatasets() {
        return (DescribeDatasetsType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDatasets(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeDatasets(DescribeDatasetsType newDescribeDatasets, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDatasets(), newDescribeDatasets, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeDatasets(DescribeDatasetsType newDescribeDatasets) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDatasets(), newDescribeDatasets);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsRequestType getDescribeDatasetsRequest() {
        return (DescribeDatasetsRequestType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDatasetsRequest(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeDatasetsRequest(DescribeDatasetsRequestType newDescribeDatasetsRequest, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDatasetsRequest(), newDescribeDatasetsRequest, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeDatasetsRequest(DescribeDatasetsRequestType newDescribeDatasetsRequest) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeDatasetsRequest(), newDescribeDatasetsRequest);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeFrameworksType getDescribeFrameworks() {
        return (DescribeFrameworksType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeFrameworks(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeFrameworks(DescribeFrameworksType newDescribeFrameworks, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeFrameworks(), newDescribeFrameworks, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeFrameworks(DescribeFrameworksType newDescribeFrameworks) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeFrameworks(), newDescribeFrameworks);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RequestBaseType getDescribeJoinAbilities() {
        return (RequestBaseType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeJoinAbilities(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeJoinAbilities(RequestBaseType newDescribeJoinAbilities, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeJoinAbilities(), newDescribeJoinAbilities, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeJoinAbilities(RequestBaseType newDescribeJoinAbilities) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeJoinAbilities(), newDescribeJoinAbilities);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeKeyType getDescribeKey() {
        return (DescribeKeyType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeKey(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetDescribeKey(DescribeKeyType newDescribeKey, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeKey(), newDescribeKey, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDescribeKey(DescribeKeyType newDescribeKey) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_DescribeKey(), newDescribeKey);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getDocumentation() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Documentation(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setDocumentation(String newDocumentation) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Documentation(), newDocumentation);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkType1 getFramework() {
        return (FrameworkType1) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Framework(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFramework(FrameworkType1 newFramework, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Framework(), newFramework, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFramework(FrameworkType1 newFramework) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Framework(), newFramework);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkDescriptionsType getFrameworkDescriptions() {
        return (FrameworkDescriptionsType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkDescriptions(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFrameworkDescriptions(FrameworkDescriptionsType newFrameworkDescriptions, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkDescriptions(), newFrameworkDescriptions, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFrameworkDescriptions(FrameworkDescriptionsType newFrameworkDescriptions) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkDescriptions(), newFrameworkDescriptions);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyType getFrameworkKey() {
        return (FrameworkKeyType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkKey(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFrameworkKey(FrameworkKeyType newFrameworkKey, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkKey(), newFrameworkKey, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFrameworkKey(FrameworkKeyType newFrameworkKey) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkKey(), newFrameworkKey);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyDescriptionType getFrameworkKeyDescription() {
        return (FrameworkKeyDescriptionType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkKeyDescription(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetFrameworkKeyDescription(FrameworkKeyDescriptionType newFrameworkKeyDescription, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkKeyDescription(), newFrameworkKeyDescription, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFrameworkKeyDescription(FrameworkKeyDescriptionType newFrameworkKeyDescription) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkKeyDescription(), newFrameworkKeyDescription);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getFrameworkURI() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkURI(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setFrameworkURI(String newFrameworkURI) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_FrameworkURI(), newFrameworkURI);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GDASType getGDAS() {
        return (GDASType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_GDAS(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetGDAS(GDASType newGDAS, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_GDAS(), newGDAS, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGDAS(GDASType newGDAS) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_GDAS(), newGDAS);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetCapabilitiesType getGetCapabilities() {
        return (GetCapabilitiesType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_GetCapabilities(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetGetCapabilities(GetCapabilitiesType newGetCapabilities, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_GetCapabilities(), newGetCapabilities, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGetCapabilities(GetCapabilitiesType newGetCapabilities) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_GetCapabilities(), newGetCapabilities);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataType getGetData() {
        return (GetDataType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_GetData(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetGetData(GetDataType newGetData, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_GetData(), newGetData, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGetData(GetDataType newGetData) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_GetData(), newGetData);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataRequestType getGetDataRequest() {
        return (GetDataRequestType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_GetDataRequest(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetGetDataRequest(GetDataRequestType newGetDataRequest, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_GetDataRequest(), newGetDataRequest, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setGetDataRequest(GetDataRequestType newGetDataRequest) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_GetDataRequest(), newGetDataRequest);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getIdentifier() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Identifier(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setIdentifier(String newIdentifier) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Identifier(), newIdentifier);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinAbilitiesType getJoinAbilities() {
        return (JoinAbilitiesType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_JoinAbilities(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetJoinAbilities(JoinAbilitiesType newJoinAbilities, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_JoinAbilities(), newJoinAbilities, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setJoinAbilities(JoinAbilitiesType newJoinAbilities) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_JoinAbilities(), newJoinAbilities);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinDataType getJoinData() {
        return (JoinDataType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_JoinData(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetJoinData(JoinDataType newJoinData, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_JoinData(), newJoinData, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setJoinData(JoinDataType newJoinData) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_JoinData(), newJoinData);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinDataResponseType getJoinDataResponse() {
        return (JoinDataResponseType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_JoinDataResponse(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetJoinDataResponse(JoinDataResponseType newJoinDataResponse, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_JoinDataResponse(), newJoinDataResponse, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setJoinDataResponse(JoinDataResponseType newJoinDataResponse) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_JoinDataResponse(), newJoinDataResponse);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public KType getK() {
        return (KType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_K(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetK(KType newK, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_K(), newK, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setK(KType newK) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_K(), newK);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getLinkageKeys() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_LinkageKeys(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setLinkageKeys(String newLinkageKeys) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_LinkageKeys(), newLinkageKeys);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MeasureType getMeasure() {
        return (MeasureType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Measure(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetMeasure(MeasureType newMeasure, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Measure(), newMeasure, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setMeasure(MeasureType newMeasure) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Measure(), newMeasure);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MechanismType getMechanism() {
        return (MechanismType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Mechanism(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetMechanism(MechanismType newMechanism, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Mechanism(), newMechanism, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setMechanism(MechanismType newMechanism) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Mechanism(), newMechanism);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NominalType getNominal() {
        return (NominalType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Nominal(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetNominal(NominalType newNominal, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Nominal(), newNominal, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setNominal(NominalType newNominal) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Nominal(), newNominal);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OrdinalType getOrdinal() {
        return (OrdinalType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Ordinal(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetOrdinal(OrdinalType newOrdinal, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Ordinal(), newOrdinal, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setOrdinal(OrdinalType newOrdinal) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Ordinal(), newOrdinal);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getOrganization() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Organization(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setOrganization(String newOrganization) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Organization(), newOrganization);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ReferenceDateType getReferenceDate() {
        return (ReferenceDateType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_ReferenceDate(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetReferenceDate(ReferenceDateType newReferenceDate, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_ReferenceDate(), newReferenceDate, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setReferenceDate(ReferenceDateType newReferenceDate) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_ReferenceDate(), newReferenceDate);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RowsetType1 getRowset() {
        return (RowsetType1) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Rowset(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetRowset(RowsetType1 newRowset, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Rowset(), newRowset, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setRowset(RowsetType1 newRowset) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Rowset(), newRowset);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public SpatialFrameworksType getSpatialFrameworks() {
        return (SpatialFrameworksType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_SpatialFrameworks(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetSpatialFrameworks(SpatialFrameworksType newSpatialFrameworks, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_SpatialFrameworks(), newSpatialFrameworks, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setSpatialFrameworks(SpatialFrameworksType newSpatialFrameworks) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_SpatialFrameworks(), newSpatialFrameworks);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public StylingType getStyling() {
        return (StylingType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Styling(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetStyling(StylingType newStyling, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Styling(), newStyling, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setStyling(StylingType newStyling) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Styling(), newStyling);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getTitle() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Title(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setTitle(String newTitle) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Title(), newTitle);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UncertaintyType getUncertainty() {
        return (UncertaintyType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Uncertainty(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetUncertainty(UncertaintyType newUncertainty, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Uncertainty(), newUncertainty, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setUncertainty(UncertaintyType newUncertainty) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Uncertainty(), newUncertainty);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UOMType getUOM() {
        return (UOMType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_UOM(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetUOM(UOMType newUOM, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_UOM(), newUOM, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setUOM(UOMType newUOM) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_UOM(), newUOM);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ValuesType getValues() {
        return (ValuesType) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Values(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain basicSetValues(ValuesType newValues, NotificationChain msgs) {
        return ((FeatureMap.Internal) getMixed()).basicAdd(Tjs10Package.eINSTANCE.getDocumentRoot_Values(), newValues, msgs);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setValues(ValuesType newValues) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Values(), newValues);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String getVersion() {
        return (String) getMixed().get(Tjs10Package.eINSTANCE.getDocumentRoot_Version(), true);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public void setVersion(String newVersion) {
        ((FeatureMap.Internal) getMixed()).set(Tjs10Package.eINSTANCE.getDocumentRoot_Version(), newVersion);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case Tjs10Package.DOCUMENT_ROOT__MIXED:
                return ((InternalEList) getMixed()).basicRemove(otherEnd, msgs);
            case Tjs10Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP:
                return ((InternalEList) getXMLNSPrefixMap()).basicRemove(otherEnd, msgs);
            case Tjs10Package.DOCUMENT_ROOT__XSI_SCHEMA_LOCATION:
                return ((InternalEList) getXSISchemaLocation()).basicRemove(otherEnd, msgs);
            case Tjs10Package.DOCUMENT_ROOT__ABSTRACT:
                return basicSetAbstract(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__BOUNDING_COORDINATES:
                return basicSetBoundingCoordinates(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__CAPABILITIES:
                return basicSetCapabilities(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__COLUMNSET:
                return basicSetColumnset(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__COUNT:
                return basicSetCount(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DATA_DESCRIPTIONS:
                return basicSetDataDescriptions(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DATASET:
                return basicSetDataset(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DATASET_DESCRIPTIONS:
                return basicSetDatasetDescriptions(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA:
                return basicSetDescribeData(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST:
                return basicSetDescribeDataRequest(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS:
                return basicSetDescribeDatasets(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST:
                return basicSetDescribeDatasetsRequest(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS:
                return basicSetDescribeFrameworks(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES:
                return basicSetDescribeJoinAbilities(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_KEY:
                return basicSetDescribeKey(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK:
                return basicSetFramework(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS:
                return basicSetFrameworkDescriptions(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY:
                return basicSetFrameworkKey(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION:
                return basicSetFrameworkKeyDescription(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__GDAS:
                return basicSetGDAS(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__GET_CAPABILITIES:
                return basicSetGetCapabilities(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA:
                return basicSetGetData(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA_REQUEST:
                return basicSetGetDataRequest(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__JOIN_ABILITIES:
                return basicSetJoinAbilities(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA:
                return basicSetJoinData(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA_RESPONSE:
                return basicSetJoinDataResponse(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__K:
                return basicSetK(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__MEASURE:
                return basicSetMeasure(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__MECHANISM:
                return basicSetMechanism(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__NOMINAL:
                return basicSetNominal(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__ORDINAL:
                return basicSetOrdinal(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__REFERENCE_DATE:
                return basicSetReferenceDate(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__ROWSET:
                return basicSetRowset(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__SPATIAL_FRAMEWORKS:
                return basicSetSpatialFrameworks(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__STYLING:
                return basicSetStyling(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__UNCERTAINTY:
                return basicSetUncertainty(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__UOM:
                return basicSetUOM(null, msgs);
            case Tjs10Package.DOCUMENT_ROOT__VALUES:
                return basicSetValues(null, msgs);
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
            case Tjs10Package.DOCUMENT_ROOT__MIXED:
                if (coreType) return getMixed();
                return ((FeatureMap.Internal) getMixed()).getWrapper();
            case Tjs10Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP:
                if (coreType) return getXMLNSPrefixMap();
                else return getXMLNSPrefixMap().map();
            case Tjs10Package.DOCUMENT_ROOT__XSI_SCHEMA_LOCATION:
                if (coreType) return getXSISchemaLocation();
                else return getXSISchemaLocation().map();
            case Tjs10Package.DOCUMENT_ROOT__ABSTRACT:
                return getAbstract();
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTE_LIMIT:
                return getAttributeLimit();
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTES:
                return getAttributes();
            case Tjs10Package.DOCUMENT_ROOT__BOUNDING_COORDINATES:
                return getBoundingCoordinates();
            case Tjs10Package.DOCUMENT_ROOT__CAPABILITIES:
                return getCapabilities();
            case Tjs10Package.DOCUMENT_ROOT__COLUMNSET:
                return getColumnset();
            case Tjs10Package.DOCUMENT_ROOT__COUNT:
                return getCount();
            case Tjs10Package.DOCUMENT_ROOT__DATA_CLASS:
                return getDataClass();
            case Tjs10Package.DOCUMENT_ROOT__DATA_DESCRIPTIONS:
                return getDataDescriptions();
            case Tjs10Package.DOCUMENT_ROOT__DATASET:
                return getDataset();
            case Tjs10Package.DOCUMENT_ROOT__DATASET_DESCRIPTIONS:
                return getDatasetDescriptions();
            case Tjs10Package.DOCUMENT_ROOT__DATASET_URI:
                return getDatasetURI();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA:
                return getDescribeData();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST:
                return getDescribeDataRequest();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS:
                return getDescribeDatasets();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST:
                return getDescribeDatasetsRequest();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS:
                return getDescribeFrameworks();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES:
                return getDescribeJoinAbilities();
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_KEY:
                return getDescribeKey();
            case Tjs10Package.DOCUMENT_ROOT__DOCUMENTATION:
                return getDocumentation();
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK:
                return getFramework();
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS:
                return getFrameworkDescriptions();
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY:
                return getFrameworkKey();
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION:
                return getFrameworkKeyDescription();
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_URI:
                return getFrameworkURI();
            case Tjs10Package.DOCUMENT_ROOT__GDAS:
                return getGDAS();
            case Tjs10Package.DOCUMENT_ROOT__GET_CAPABILITIES:
                return getGetCapabilities();
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA:
                return getGetData();
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA_REQUEST:
                return getGetDataRequest();
            case Tjs10Package.DOCUMENT_ROOT__IDENTIFIER:
                return getIdentifier();
            case Tjs10Package.DOCUMENT_ROOT__JOIN_ABILITIES:
                return getJoinAbilities();
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA:
                return getJoinData();
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA_RESPONSE:
                return getJoinDataResponse();
            case Tjs10Package.DOCUMENT_ROOT__K:
                return getK();
            case Tjs10Package.DOCUMENT_ROOT__LINKAGE_KEYS:
                return getLinkageKeys();
            case Tjs10Package.DOCUMENT_ROOT__MEASURE:
                return getMeasure();
            case Tjs10Package.DOCUMENT_ROOT__MECHANISM:
                return getMechanism();
            case Tjs10Package.DOCUMENT_ROOT__NOMINAL:
                return getNominal();
            case Tjs10Package.DOCUMENT_ROOT__ORDINAL:
                return getOrdinal();
            case Tjs10Package.DOCUMENT_ROOT__ORGANIZATION:
                return getOrganization();
            case Tjs10Package.DOCUMENT_ROOT__REFERENCE_DATE:
                return getReferenceDate();
            case Tjs10Package.DOCUMENT_ROOT__ROWSET:
                return getRowset();
            case Tjs10Package.DOCUMENT_ROOT__SPATIAL_FRAMEWORKS:
                return getSpatialFrameworks();
            case Tjs10Package.DOCUMENT_ROOT__STYLING:
                return getStyling();
            case Tjs10Package.DOCUMENT_ROOT__TITLE:
                return getTitle();
            case Tjs10Package.DOCUMENT_ROOT__UNCERTAINTY:
                return getUncertainty();
            case Tjs10Package.DOCUMENT_ROOT__UOM:
                return getUOM();
            case Tjs10Package.DOCUMENT_ROOT__VALUES:
                return getValues();
            case Tjs10Package.DOCUMENT_ROOT__VERSION:
                return getVersion();
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
            case Tjs10Package.DOCUMENT_ROOT__MIXED:
                ((FeatureMap.Internal) getMixed()).set(newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP:
                ((EStructuralFeature.Setting) getXMLNSPrefixMap()).set(newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__XSI_SCHEMA_LOCATION:
                ((EStructuralFeature.Setting) getXSISchemaLocation()).set(newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ABSTRACT:
                setAbstract((AbstractType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTE_LIMIT:
                setAttributeLimit((BigInteger) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTES:
                setAttributes((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__BOUNDING_COORDINATES:
                setBoundingCoordinates((BoundingCoordinatesType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__CAPABILITIES:
                setCapabilities((TjsCapabilitiesType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__COLUMNSET:
                setColumnset((ColumnsetType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__COUNT:
                setCount((CountType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATA_CLASS:
                setDataClass((DataClassType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATA_DESCRIPTIONS:
                setDataDescriptions((DataDescriptionsType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATASET:
                setDataset((DatasetType1) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATASET_DESCRIPTIONS:
                setDatasetDescriptions((DatasetDescriptionsType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATASET_URI:
                setDatasetURI((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA:
                setDescribeData((DescribeDataType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST:
                setDescribeDataRequest((DescribeDataRequestType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS:
                setDescribeDatasets((DescribeDatasetsType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST:
                setDescribeDatasetsRequest((DescribeDatasetsRequestType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS:
                setDescribeFrameworks((DescribeFrameworksType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES:
                setDescribeJoinAbilities((RequestBaseType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_KEY:
                setDescribeKey((DescribeKeyType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DOCUMENTATION:
                setDocumentation((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK:
                setFramework((FrameworkType1) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS:
                setFrameworkDescriptions((FrameworkDescriptionsType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION:
                setFrameworkKeyDescription((FrameworkKeyDescriptionType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_URI:
                setFrameworkURI((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GDAS:
                setGDAS((GDASType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GET_CAPABILITIES:
                setGetCapabilities((GetCapabilitiesType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA:
                setGetData((GetDataType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA_REQUEST:
                setGetDataRequest((GetDataRequestType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__IDENTIFIER:
                setIdentifier((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_ABILITIES:
                setJoinAbilities((JoinAbilitiesType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA:
                setJoinData((JoinDataType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA_RESPONSE:
                setJoinDataResponse((JoinDataResponseType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__K:
                setK((KType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__LINKAGE_KEYS:
                setLinkageKeys((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__MEASURE:
                setMeasure((MeasureType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__MECHANISM:
                setMechanism((MechanismType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__NOMINAL:
                setNominal((NominalType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ORDINAL:
                setOrdinal((OrdinalType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ORGANIZATION:
                setOrganization((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ROWSET:
                setRowset((RowsetType1) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__SPATIAL_FRAMEWORKS:
                setSpatialFrameworks((SpatialFrameworksType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__STYLING:
                setStyling((StylingType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__TITLE:
                setTitle((String) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__UNCERTAINTY:
                setUncertainty((UncertaintyType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__UOM:
                setUOM((UOMType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__VALUES:
                setValues((ValuesType) newValue);
                return;
            case Tjs10Package.DOCUMENT_ROOT__VERSION:
                setVersion((String) newValue);
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
            case Tjs10Package.DOCUMENT_ROOT__MIXED:
                getMixed().clear();
                return;
            case Tjs10Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP:
                getXMLNSPrefixMap().clear();
                return;
            case Tjs10Package.DOCUMENT_ROOT__XSI_SCHEMA_LOCATION:
                getXSISchemaLocation().clear();
                return;
            case Tjs10Package.DOCUMENT_ROOT__ABSTRACT:
                setAbstract((AbstractType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTE_LIMIT:
                setAttributeLimit(ATTRIBUTE_LIMIT_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTES:
                setAttributes(ATTRIBUTES_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__BOUNDING_COORDINATES:
                setBoundingCoordinates((BoundingCoordinatesType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__CAPABILITIES:
                setCapabilities((TjsCapabilitiesType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__COLUMNSET:
                setColumnset((ColumnsetType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__COUNT:
                setCount((CountType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATA_CLASS:
                setDataClass(DATA_CLASS_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATA_DESCRIPTIONS:
                setDataDescriptions((DataDescriptionsType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATASET:
                setDataset((DatasetType1) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATASET_DESCRIPTIONS:
                setDatasetDescriptions((DatasetDescriptionsType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DATASET_URI:
                setDatasetURI(DATASET_URI_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA:
                setDescribeData((DescribeDataType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST:
                setDescribeDataRequest((DescribeDataRequestType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS:
                setDescribeDatasets((DescribeDatasetsType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST:
                setDescribeDatasetsRequest((DescribeDatasetsRequestType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS:
                setDescribeFrameworks((DescribeFrameworksType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES:
                setDescribeJoinAbilities((RequestBaseType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_KEY:
                setDescribeKey((DescribeKeyType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__DOCUMENTATION:
                setDocumentation(DOCUMENTATION_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK:
                setFramework((FrameworkType1) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS:
                setFrameworkDescriptions((FrameworkDescriptionsType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY:
                setFrameworkKey((FrameworkKeyType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION:
                setFrameworkKeyDescription((FrameworkKeyDescriptionType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_URI:
                setFrameworkURI(FRAMEWORK_URI_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GDAS:
                setGDAS((GDASType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GET_CAPABILITIES:
                setGetCapabilities((GetCapabilitiesType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA:
                setGetData((GetDataType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA_REQUEST:
                setGetDataRequest((GetDataRequestType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__IDENTIFIER:
                setIdentifier(IDENTIFIER_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_ABILITIES:
                setJoinAbilities((JoinAbilitiesType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA:
                setJoinData((JoinDataType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA_RESPONSE:
                setJoinDataResponse((JoinDataResponseType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__K:
                setK((KType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__LINKAGE_KEYS:
                setLinkageKeys(LINKAGE_KEYS_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__MEASURE:
                setMeasure((MeasureType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__MECHANISM:
                setMechanism((MechanismType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__NOMINAL:
                setNominal((NominalType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ORDINAL:
                setOrdinal((OrdinalType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ORGANIZATION:
                setOrganization(ORGANIZATION_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__REFERENCE_DATE:
                setReferenceDate((ReferenceDateType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__ROWSET:
                setRowset((RowsetType1) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__SPATIAL_FRAMEWORKS:
                setSpatialFrameworks((SpatialFrameworksType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__STYLING:
                setStyling((StylingType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__TITLE:
                setTitle(TITLE_EDEFAULT);
                return;
            case Tjs10Package.DOCUMENT_ROOT__UNCERTAINTY:
                setUncertainty((UncertaintyType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__UOM:
                setUOM((UOMType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__VALUES:
                setValues((ValuesType) null);
                return;
            case Tjs10Package.DOCUMENT_ROOT__VERSION:
                setVersion(VERSION_EDEFAULT);
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
            case Tjs10Package.DOCUMENT_ROOT__MIXED:
                return mixed != null && !mixed.isEmpty();
            case Tjs10Package.DOCUMENT_ROOT__XMLNS_PREFIX_MAP:
                return xMLNSPrefixMap != null && !xMLNSPrefixMap.isEmpty();
            case Tjs10Package.DOCUMENT_ROOT__XSI_SCHEMA_LOCATION:
                return xSISchemaLocation != null && !xSISchemaLocation.isEmpty();
            case Tjs10Package.DOCUMENT_ROOT__ABSTRACT:
                return getAbstract() != null;
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTE_LIMIT:
                return ATTRIBUTE_LIMIT_EDEFAULT == null ? getAttributeLimit() != null : !ATTRIBUTE_LIMIT_EDEFAULT.equals(getAttributeLimit());
            case Tjs10Package.DOCUMENT_ROOT__ATTRIBUTES:
                return ATTRIBUTES_EDEFAULT == null ? getAttributes() != null : !ATTRIBUTES_EDEFAULT.equals(getAttributes());
            case Tjs10Package.DOCUMENT_ROOT__BOUNDING_COORDINATES:
                return getBoundingCoordinates() != null;
            case Tjs10Package.DOCUMENT_ROOT__CAPABILITIES:
                return getCapabilities() != null;
            case Tjs10Package.DOCUMENT_ROOT__COLUMNSET:
                return getColumnset() != null;
            case Tjs10Package.DOCUMENT_ROOT__COUNT:
                return getCount() != null;
            case Tjs10Package.DOCUMENT_ROOT__DATA_CLASS:
                return getDataClass() != DATA_CLASS_EDEFAULT;
            case Tjs10Package.DOCUMENT_ROOT__DATA_DESCRIPTIONS:
                return getDataDescriptions() != null;
            case Tjs10Package.DOCUMENT_ROOT__DATASET:
                return getDataset() != null;
            case Tjs10Package.DOCUMENT_ROOT__DATASET_DESCRIPTIONS:
                return getDatasetDescriptions() != null;
            case Tjs10Package.DOCUMENT_ROOT__DATASET_URI:
                return DATASET_URI_EDEFAULT == null ? getDatasetURI() != null : !DATASET_URI_EDEFAULT.equals(getDatasetURI());
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA:
                return getDescribeData() != null;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST:
                return getDescribeDataRequest() != null;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS:
                return getDescribeDatasets() != null;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST:
                return getDescribeDatasetsRequest() != null;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS:
                return getDescribeFrameworks() != null;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES:
                return getDescribeJoinAbilities() != null;
            case Tjs10Package.DOCUMENT_ROOT__DESCRIBE_KEY:
                return getDescribeKey() != null;
            case Tjs10Package.DOCUMENT_ROOT__DOCUMENTATION:
                return DOCUMENTATION_EDEFAULT == null ? getDocumentation() != null : !DOCUMENTATION_EDEFAULT.equals(getDocumentation());
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK:
                return getFramework() != null;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS:
                return getFrameworkDescriptions() != null;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY:
                return getFrameworkKey() != null;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION:
                return getFrameworkKeyDescription() != null;
            case Tjs10Package.DOCUMENT_ROOT__FRAMEWORK_URI:
                return FRAMEWORK_URI_EDEFAULT == null ? getFrameworkURI() != null : !FRAMEWORK_URI_EDEFAULT.equals(getFrameworkURI());
            case Tjs10Package.DOCUMENT_ROOT__GDAS:
                return getGDAS() != null;
            case Tjs10Package.DOCUMENT_ROOT__GET_CAPABILITIES:
                return getGetCapabilities() != null;
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA:
                return getGetData() != null;
            case Tjs10Package.DOCUMENT_ROOT__GET_DATA_REQUEST:
                return getGetDataRequest() != null;
            case Tjs10Package.DOCUMENT_ROOT__IDENTIFIER:
                return IDENTIFIER_EDEFAULT == null ? getIdentifier() != null : !IDENTIFIER_EDEFAULT.equals(getIdentifier());
            case Tjs10Package.DOCUMENT_ROOT__JOIN_ABILITIES:
                return getJoinAbilities() != null;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA:
                return getJoinData() != null;
            case Tjs10Package.DOCUMENT_ROOT__JOIN_DATA_RESPONSE:
                return getJoinDataResponse() != null;
            case Tjs10Package.DOCUMENT_ROOT__K:
                return getK() != null;
            case Tjs10Package.DOCUMENT_ROOT__LINKAGE_KEYS:
                return LINKAGE_KEYS_EDEFAULT == null ? getLinkageKeys() != null : !LINKAGE_KEYS_EDEFAULT.equals(getLinkageKeys());
            case Tjs10Package.DOCUMENT_ROOT__MEASURE:
                return getMeasure() != null;
            case Tjs10Package.DOCUMENT_ROOT__MECHANISM:
                return getMechanism() != null;
            case Tjs10Package.DOCUMENT_ROOT__NOMINAL:
                return getNominal() != null;
            case Tjs10Package.DOCUMENT_ROOT__ORDINAL:
                return getOrdinal() != null;
            case Tjs10Package.DOCUMENT_ROOT__ORGANIZATION:
                return ORGANIZATION_EDEFAULT == null ? getOrganization() != null : !ORGANIZATION_EDEFAULT.equals(getOrganization());
            case Tjs10Package.DOCUMENT_ROOT__REFERENCE_DATE:
                return getReferenceDate() != null;
            case Tjs10Package.DOCUMENT_ROOT__ROWSET:
                return getRowset() != null;
            case Tjs10Package.DOCUMENT_ROOT__SPATIAL_FRAMEWORKS:
                return getSpatialFrameworks() != null;
            case Tjs10Package.DOCUMENT_ROOT__STYLING:
                return getStyling() != null;
            case Tjs10Package.DOCUMENT_ROOT__TITLE:
                return TITLE_EDEFAULT == null ? getTitle() != null : !TITLE_EDEFAULT.equals(getTitle());
            case Tjs10Package.DOCUMENT_ROOT__UNCERTAINTY:
                return getUncertainty() != null;
            case Tjs10Package.DOCUMENT_ROOT__UOM:
                return getUOM() != null;
            case Tjs10Package.DOCUMENT_ROOT__VALUES:
                return getValues() != null;
            case Tjs10Package.DOCUMENT_ROOT__VERSION:
                return VERSION_EDEFAULT == null ? getVersion() != null : !VERSION_EDEFAULT.equals(getVersion());
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
        result.append(" (mixed: ");
        result.append(mixed);
        result.append(')');
        return result.toString();
    }

} //DocumentRootImpl
