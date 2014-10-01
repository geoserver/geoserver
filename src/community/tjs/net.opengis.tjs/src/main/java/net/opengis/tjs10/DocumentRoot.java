/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;

import java.math.BigInteger;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Document Root</b></em>'.
 * <!-- end-user-doc -->
 * <p/>
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getMixed <em>Mixed</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getXMLNSPrefixMap <em>XMLNS Prefix Map</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getXSISchemaLocation <em>XSI Schema Location</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getAbstract <em>Abstract</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getAttributeLimit <em>Attribute Limit</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getAttributes <em>Attributes</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getBoundingCoordinates <em>Bounding Coordinates</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getCapabilities <em>Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getColumnset <em>Columnset</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getCount <em>Count</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDataClass <em>Data Class</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDataDescriptions <em>Data Descriptions</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDataset <em>Dataset</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDatasetDescriptions <em>Dataset Descriptions</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDatasetURI <em>Dataset URI</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeData <em>Describe Data</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeDataRequest <em>Describe Data Request</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeDatasets <em>Describe Datasets</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeFrameworks <em>Describe Frameworks</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeJoinAbilities <em>Describe Join Abilities</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDescribeKey <em>Describe Key</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getDocumentation <em>Documentation</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getFramework <em>Framework</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getFrameworkDescriptions <em>Framework Descriptions</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getFrameworkKey <em>Framework Key</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getFrameworkKeyDescription <em>Framework Key Description</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getFrameworkURI <em>Framework URI</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getGDAS <em>GDAS</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getGetCapabilities <em>Get Capabilities</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getGetData <em>Get Data</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getGetDataRequest <em>Get Data Request</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getIdentifier <em>Identifier</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getJoinAbilities <em>Join Abilities</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getJoinData <em>Join Data</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getJoinDataResponse <em>Join Data Response</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getK <em>K</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getLinkageKeys <em>Linkage Keys</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getMeasure <em>Measure</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getMechanism <em>Mechanism</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getNominal <em>Nominal</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getOrdinal <em>Ordinal</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getOrganization <em>Organization</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getReferenceDate <em>Reference Date</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getRowset <em>Rowset</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getSpatialFrameworks <em>Spatial Frameworks</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getStyling <em>Styling</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getTitle <em>Title</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getUncertainty <em>Uncertainty</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getUOM <em>UOM</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getValues <em>Values</em>}</li>
 * <li>{@link net.opengis.tjs10.DocumentRoot#getVersion <em>Version</em>}</li>
 * </ul>
 * </p>
 *
 * @model extendedMetaData="name='' kind='mixed'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot()
 */
public interface DocumentRoot extends EObject {
    /**
     * Returns the value of the '<em><b>Mixed</b></em>' attribute list.
     * The list contents are of type {@link org.eclipse.emf.ecore.util.FeatureMap.Entry}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Mixed</em>' attribute list isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Mixed</em>' attribute list.
     * @model unique="false" dataType="org.eclipse.emf.ecore.EFeatureMapEntry" many="true"
     * extendedMetaData="kind='elementWildcard' name=':mixed'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Mixed()
     */
    FeatureMap getMixed();

    /**
     * Returns the value of the '<em><b>XMLNS Prefix Map</b></em>' map.
     * The key is of type {@link java.lang.String},
     * and the value is of type {@link java.lang.String},
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>XMLNS Prefix Map</em>' map isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>XMLNS Prefix Map</em>' map.
     * @model mapType="org.eclipse.emf.ecore.EStringToStringMapEntry" keyType="java.lang.String" valueType="java.lang.String" transient="true"
     * extendedMetaData="kind='attribute' name='xmlns:prefix'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_XMLNSPrefixMap()
     */
    EMap getXMLNSPrefixMap();

    /**
     * Returns the value of the '<em><b>XSI Schema Location</b></em>' map.
     * The key is of type {@link java.lang.String},
     * and the value is of type {@link java.lang.String},
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>XSI Schema Location</em>' map isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>XSI Schema Location</em>' map.
     * @model mapType="org.eclipse.emf.ecore.EStringToStringMapEntry" keyType="java.lang.String" valueType="java.lang.String" transient="true"
     * extendedMetaData="kind='attribute' name='xsi:schemaLocation'"
     * @generated
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_XSISchemaLocation()
     */
    EMap getXSISchemaLocation();

    /**
     * Returns the value of the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * One or more paragraphs of human-readable relevant text suitable for display in a pop-up window.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Abstract</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Abstract' namespace='##targetNamespace'"
     * @generated
     * @see #setAbstract(AbstractType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Abstract()
     */
    AbstractType getAbstract();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getAbstract <em>Abstract</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Abstract</em>' containment reference.
     * @generated
     * @see #getAbstract()
     */
    void setAbstract(AbstractType value);

    /**
     * Returns the value of the '<em><b>Attribute Limit</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Maximum number of attributes that can be joined simultaneously as part of a JoinData request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attribute Limit</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.PositiveInteger" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='AttributeLimit' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributeLimit(BigInteger)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_AttributeLimit()
     */
    BigInteger getAttributeLimit();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getAttributeLimit <em>Attribute Limit</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attribute Limit</em>' attribute.
     * @generated
     * @see #getAttributeLimit()
     */
    void setAttributeLimit(BigInteger value);

    /**
     * Returns the value of the '<em><b>Attributes</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The AttributeNames requested by the user, in comma-delimited format
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Attributes</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Attributes' namespace='##targetNamespace'"
     * @generated
     * @see #setAttributes(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Attributes()
     */
    String getAttributes();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getAttributes <em>Attributes</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Attributes</em>' attribute.
     * @generated
     * @see #getAttributes()
     */
    void setAttributes(String value);

    /**
     * Returns the value of the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Identifies the bounding coordinates of the spatial framework using the WGS84 CRS.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Bounding Coordinates</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='BoundingCoordinates' namespace='##targetNamespace'"
     * @generated
     * @see #setBoundingCoordinates(BoundingCoordinatesType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_BoundingCoordinates()
     */
    BoundingCoordinatesType getBoundingCoordinates();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getBoundingCoordinates <em>Bounding Coordinates</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Bounding Coordinates</em>' containment reference.
     * @generated
     * @see #getBoundingCoordinates()
     */
    void setBoundingCoordinates(BoundingCoordinatesType value);

    /**
     * Returns the value of the '<em><b>Capabilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * XML encoded tjs GetCapabilities operation response. This document provides clients with service metadata about a specific service instance, including metadata about the tightly-coupled data served. If the server does not implement the updateSequence parameter, the server shall always return the complete Capabilities document, without the updateSequence parameter. When the server implements the updateSequence parameter and the GetCapabilities operation request included the updateSequence parameter with the current value, the server shall return this element with only the "version" and "updateSequence" attributes. Otherwise, all optional elements shall be included or not depending on the actual value of the Sections parameter in the GetCapabilities operation request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Capabilities</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Capabilities' namespace='##targetNamespace'"
     * @generated
     * @see #setCapabilities(TjsCapabilitiesType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Capabilities()
     */
    TjsCapabilitiesType getCapabilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getCapabilities <em>Capabilities</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Capabilities</em>' containment reference.
     * @generated
     * @see #getCapabilities()
     */
    void setCapabilities(TjsCapabilitiesType value);

    /**
     * Returns the value of the '<em><b>Columnset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Ordered list of columns found in the dataset.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Columnset</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Columnset' namespace='##targetNamespace'"
     * @generated
     * @see #setColumnset(ColumnsetType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Columnset()
     */
    ColumnsetType getColumnset();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getColumnset <em>Columnset</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Columnset</em>' containment reference.
     * @generated
     * @see #getColumnset()
     */
    void setColumnset(ColumnsetType value);

    /**
     * Returns the value of the '<em><b>Count</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of the number of some observable elements present in the spatial features
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Count</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Count' namespace='##targetNamespace'"
     * @generated
     * @see #setCount(CountType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Count()
     */
    CountType getCount();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getCount <em>Count</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Count</em>' containment reference.
     * @generated
     * @see #getCount()
     */
    void setCount(CountType value);

    /**
     * Returns the value of the '<em><b>Data Class</b></em>' attribute.
     * The literals are from the enumeration {@link net.opengis.tjs10.DataClassType}.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Data Class</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Data Class</em>' attribute.
     * @model unique="false" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DataClass' namespace='##targetNamespace'"
     * @generated
     * @see net.opengis.tjs10.DataClassType
     * @see #setDataClass(DataClassType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DataClass()
     */
    DataClassType getDataClass();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDataClass <em>Data Class</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Data Class</em>' attribute.
     * @generated
     * @see net.opengis.tjs10.DataClassType
     * @see #getDataClass()
     */
    void setDataClass(DataClassType value);

    /**
     * Returns the value of the '<em><b>Data Descriptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Response containing full descriptions of data which can be joined to the identified spatial frameworks.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Data Descriptions</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DataDescriptions' namespace='##targetNamespace'"
     * @generated
     * @see #setDataDescriptions(DataDescriptionsType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DataDescriptions()
     */
    DataDescriptionsType getDataDescriptions();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDataDescriptions <em>Data Descriptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Data Descriptions</em>' containment reference.
     * @generated
     * @see #getDataDescriptions()
     */
    void setDataDescriptions(DataDescriptionsType value);

    /**
     * Returns the value of the '<em><b>Dataset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Dataset</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Dataset</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Dataset' namespace='##targetNamespace'"
     * @generated
     * @see #setDataset(DatasetType1)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Dataset()
     */
    DatasetType1 getDataset();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDataset <em>Dataset</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Dataset</em>' containment reference.
     * @generated
     * @see #getDataset()
     */
    void setDataset(DatasetType1 value);

    /**
     * Returns the value of the '<em><b>Dataset Descriptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Response containing full descriptions of all datasets containing data which can be joined to the identified spatial frameworks.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Dataset Descriptions</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DatasetDescriptions' namespace='##targetNamespace'"
     * @generated
     * @see #setDatasetDescriptions(DatasetDescriptionsType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DatasetDescriptions()
     */
    DatasetDescriptionsType getDatasetDescriptions();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDatasetDescriptions <em>Dataset Descriptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Dataset Descriptions</em>' containment reference.
     * @generated
     * @see #getDatasetDescriptions()
     */
    void setDatasetDescriptions(DatasetDescriptionsType value);

    /**
     * Returns the value of the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the attribute dataset.  Normally a resolvable URL or a URN.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Dataset URI</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DatasetURI' namespace='##targetNamespace'"
     * @generated
     * @see #setDatasetURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DatasetURI()
     */
    String getDatasetURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDatasetURI <em>Dataset URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Dataset URI</em>' attribute.
     * @generated
     * @see #getDatasetURI()
     */
    void setDatasetURI(String value);

    /**
     * Returns the value of the '<em><b>Describe Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to describe the attribute data which is available from this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Data</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeData' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeData(DescribeDataType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeData()
     */
    DescribeDataType getDescribeData();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeData <em>Describe Data</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Data</em>' containment reference.
     * @generated
     * @see #getDescribeData()
     */
    void setDescribeData(DescribeDataType value);

    /**
     * Returns the value of the '<em><b>Describe Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to the DescribeData request for this dataset.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Data Request</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeDataRequest' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeDataRequest(DescribeDataRequestType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeDataRequest()
     */
    DescribeDataRequestType getDescribeDataRequest();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeDataRequest <em>Describe Data Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Data Request</em>' containment reference.
     * @generated
     * @see #getDescribeDataRequest()
     */
    void setDescribeDataRequest(DescribeDataRequestType value);

    /**
     * Returns the value of the '<em><b>Describe Datasets</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to describe the attribute datasets from which data is available from this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Datasets</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeDatasets' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeDatasets(DescribeDatasetsType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeDatasets()
     */
    DescribeDatasetsType getDescribeDatasets();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeDatasets <em>Describe Datasets</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Datasets</em>' containment reference.
     * @generated
     * @see #getDescribeDatasets()
     */
    void setDescribeDatasets(DescribeDatasetsType value);

    /**
     * Returns the value of the '<em><b>Describe Datasets Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to the DescribeDatasets request for this framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Datasets Request</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeDatasetsRequest' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeDatasetsRequest(DescribeDatasetsRequestType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeDatasetsRequest()
     */
    DescribeDatasetsRequestType getDescribeDatasetsRequest();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Datasets Request</em>' containment reference.
     * @generated
     * @see #getDescribeDatasetsRequest()
     */
    void setDescribeDatasetsRequest(DescribeDatasetsRequestType value);

    /**
     * Returns the value of the '<em><b>Describe Frameworks</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to describe the spatial frameworks to which attribute data available from this server applies.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Frameworks</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeFrameworks' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeFrameworks(DescribeFrameworksType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeFrameworks()
     */
    DescribeFrameworksType getDescribeFrameworks();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeFrameworks <em>Describe Frameworks</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Frameworks</em>' containment reference.
     * @generated
     * @see #getDescribeFrameworks()
     */
    void setDescribeFrameworks(DescribeFrameworksType value);

    /**
     * Returns the value of the '<em><b>Describe Join Abilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to describe to which spatial frameworks this server can join attribute data.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Join Abilities</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeJoinAbilities' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeJoinAbilities(RequestBaseType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeJoinAbilities()
     */
    RequestBaseType getDescribeJoinAbilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeJoinAbilities <em>Describe Join Abilities</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Join Abilities</em>' containment reference.
     * @generated
     * @see #getDescribeJoinAbilities()
     */
    void setDescribeJoinAbilities(RequestBaseType value);

    /**
     * Returns the value of the '<em><b>Describe Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to describe the contents of the FrameworkKey field for a spatial framework housed on this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Describe Key</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='DescribeKey' namespace='##targetNamespace'"
     * @generated
     * @see #setDescribeKey(DescribeKeyType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_DescribeKey()
     */
    DescribeKeyType getDescribeKey();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDescribeKey <em>Describe Key</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Describe Key</em>' containment reference.
     * @generated
     * @see #getDescribeKey()
     */
    void setDescribeKey(DescribeKeyType value);

    /**
     * Returns the value of the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to a web-accessible resource which contains further information describing this object.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Documentation</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.AnyURI" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Documentation' namespace='##targetNamespace'"
     * @generated
     * @see #setDocumentation(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Documentation()
     */
    String getDocumentation();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getDocumentation <em>Documentation</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Documentation</em>' attribute.
     * @generated
     * @see #getDocumentation()
     */
    void setDocumentation(String value);

    /**
     * Returns the value of the '<em><b>Framework</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Spatial framework description and attribute data which applies to that framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Framework' namespace='##targetNamespace'"
     * @generated
     * @see #setFramework(FrameworkType1)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Framework()
     */
    FrameworkType1 getFramework();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getFramework <em>Framework</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework</em>' containment reference.
     * @generated
     * @see #getFramework()
     */
    void setFramework(FrameworkType1 value);

    /**
     * Returns the value of the '<em><b>Framework Descriptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Response containing full descriptions of all spatial frameworks for which data is available.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework Descriptions</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='FrameworkDescriptions' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkDescriptions(FrameworkDescriptionsType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_FrameworkDescriptions()
     */
    FrameworkDescriptionsType getFrameworkDescriptions();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getFrameworkDescriptions <em>Framework Descriptions</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework Descriptions</em>' containment reference.
     * @generated
     * @see #getFrameworkDescriptions()
     */
    void setFrameworkDescriptions(FrameworkDescriptionsType value);

    /**
     * Returns the value of the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Describes the common key field in the spatial framework dataset through which data can be joined.  The values of this key populate the 'Rowset/Row/K' elements in the GetData response.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework Key</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='FrameworkKey' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkKey(FrameworkKeyType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_FrameworkKey()
     */
    FrameworkKeyType getFrameworkKey();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getFrameworkKey <em>Framework Key</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework Key</em>' containment reference.
     * @generated
     * @see #getFrameworkKey()
     */
    void setFrameworkKey(FrameworkKeyType value);

    /**
     * Returns the value of the '<em><b>Framework Key Description</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Response containing full description the FrameworkKey of a spatial framework housed on this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework Key Description</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='FrameworkKeyDescription' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkKeyDescription(FrameworkKeyDescriptionType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_FrameworkKeyDescription()
     */
    FrameworkKeyDescriptionType getFrameworkKeyDescription();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getFrameworkKeyDescription <em>Framework Key Description</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework Key Description</em>' containment reference.
     * @generated
     * @see #getFrameworkKeyDescription()
     */
    void setFrameworkKeyDescription(FrameworkKeyDescriptionType value);

    /**
     * Returns the value of the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URI of the spatial framework.  Normally a resolvable URL or a URN.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Framework URI</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='FrameworkURI' namespace='##targetNamespace'"
     * @generated
     * @see #setFrameworkURI(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_FrameworkURI()
     */
    String getFrameworkURI();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getFrameworkURI <em>Framework URI</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Framework URI</em>' attribute.
     * @generated
     * @see #getFrameworkURI()
     */
    void setFrameworkURI(String value);

    /**
     * Returns the value of the '<em><b>GDAS</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Geospatial Data Attribute Set
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>GDAS</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='GDAS' namespace='##targetNamespace'"
     * @generated
     * @see #setGDAS(GDASType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_GDAS()
     */
    GDASType getGDAS();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getGDAS <em>GDAS</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>GDAS</em>' containment reference.
     * @generated
     * @see #getGDAS()
     */
    void setGDAS(GDASType value);

    /**
     * Returns the value of the '<em><b>Get Capabilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to a TJS to perform the GetCapabilities operation. This operation allows a client to retrieve a Capabilities XML document providing metadata for the specific TJS server. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Capabilities</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='GetCapabilities' namespace='##targetNamespace'"
     * @generated
     * @see #setGetCapabilities(GetCapabilitiesType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_GetCapabilities()
     */
    GetCapabilitiesType getGetCapabilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getGetCapabilities <em>Get Capabilities</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Capabilities</em>' containment reference.
     * @generated
     * @see #getGetCapabilities()
     */
    void setGetCapabilities(GetCapabilitiesType value);

    /**
     * Returns the value of the '<em><b>Get Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to get a specified portion of the data residing on this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Data</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='GetData' namespace='##targetNamespace'"
     * @generated
     * @see #setGetData(GetDataType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_GetData()
     */
    GetDataType getGetData();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getGetData <em>Get Data</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Data</em>' containment reference.
     * @generated
     * @see #getGetData()
     */
    void setGetData(GetDataType value);

    /**
     * Returns the value of the '<em><b>Get Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * URL reference to the GetData request for this attribute.  The request shall include any other descriptive columns (e.g. spatial component number, percentage, etc) that qualify this attribute column.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Get Data Request</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='GetDataRequest' namespace='##targetNamespace'"
     * @generated
     * @see #setGetDataRequest(GetDataRequestType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_GetDataRequest()
     */
    GetDataRequestType getGetDataRequest();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getGetDataRequest <em>Get Data Request</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Get Data Request</em>' containment reference.
     * @generated
     * @see #getGetDataRequest()
     */
    void setGetDataRequest(GetDataRequestType value);

    /**
     * Returns the value of the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Identifier</em>' attribute isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Identifier</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Identifier' namespace='##targetNamespace'"
     * @generated
     * @see #setIdentifier(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Identifier()
     */
    String getIdentifier();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getIdentifier <em>Identifier</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Identifier</em>' attribute.
     * @generated
     * @see #getIdentifier()
     */
    void setIdentifier(String value);

    /**
     * Returns the value of the '<em><b>Join Abilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Response describing all joining abilities of the tjs instance.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Join Abilities</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='JoinAbilities' namespace='##targetNamespace'"
     * @generated
     * @see #setJoinAbilities(JoinAbilitiesType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_JoinAbilities()
     */
    JoinAbilitiesType getJoinAbilities();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getJoinAbilities <em>Join Abilities</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Join Abilities</em>' containment reference.
     * @generated
     * @see #getJoinAbilities()
     */
    void setJoinAbilities(JoinAbilitiesType value);

    /**
     * Returns the value of the '<em><b>Join Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Request to a tjs to perform the JoinData operation. This operation allows a client to join attribute data to a spatial framework. In this XML encoding, no "request" parameter is included, since the element name specifies the specific operation.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Join Data</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='JoinData' namespace='##targetNamespace'"
     * @generated
     * @see #setJoinData(JoinDataType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_JoinData()
     */
    JoinDataType getJoinData();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getJoinData <em>Join Data</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Join Data</em>' containment reference.
     * @generated
     * @see #getJoinData()
     */
    void setJoinData(JoinDataType value);

    /**
     * Returns the value of the '<em><b>Join Data Response</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Response to a tjs JoinData request.  Includes the original JoinData request.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Join Data Response</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='JoinDataResponse' namespace='##targetNamespace'"
     * @generated
     * @see #setJoinDataResponse(JoinDataResponseType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_JoinDataResponse()
     */
    JoinDataResponseType getJoinDataResponse();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getJoinDataResponse <em>Join Data Response</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Join Data Response</em>' containment reference.
     * @generated
     * @see #getJoinDataResponse()
     */
    void setJoinDataResponse(JoinDataResponseType value);

    /**
     * Returns the value of the '<em><b>K</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Spatial Key for this row.  For the GetData response, when there is more than one "K" element they are ordered according to the same sequence as the "FrameworkKey" elements of the "Columnset" structure.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>K</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='K' namespace='##targetNamespace'"
     * @generated
     * @see #setK(KType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_K()
     */
    KType getK();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getK <em>K</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>K</em>' containment reference.
     * @generated
     * @see #getK()
     */
    void setK(KType value);

    /**
     * Returns the value of the '<em><b>Linkage Keys</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * The DatasetKey identifiers requested by the user.  Identifiers shall be in comma-delimited format, where ranges shall be indicated with a minimum value and maximum value separated by a dash ("-").  The same Identifier cannot be requested multiple times.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Linkage Keys</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='LinkageKeys' namespace='##targetNamespace'"
     * @generated
     * @see #setLinkageKeys(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_LinkageKeys()
     */
    String getLinkageKeys();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getLinkageKeys <em>Linkage Keys</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Linkage Keys</em>' attribute.
     * @generated
     * @see #getLinkageKeys()
     */
    void setLinkageKeys(String value);

    /**
     * Returns the value of the '<em><b>Measure</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of measurements of some characteristic attributable to the spatial features
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Measure</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Measure' namespace='##targetNamespace'"
     * @generated
     * @see #setMeasure(MeasureType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Measure()
     */
    MeasureType getMeasure();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getMeasure <em>Measure</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Measure</em>' containment reference.
     * @generated
     * @see #getMeasure()
     */
    void setMeasure(MeasureType value);

    /**
     * Returns the value of the '<em><b>Mechanism</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Mechanism by which the attribute data can be accessed once it has been joined to the spatial framework.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Mechanism</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Mechanism' namespace='##targetNamespace'"
     * @generated
     * @see #setMechanism(MechanismType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Mechanism()
     */
    MechanismType getMechanism();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getMechanism <em>Mechanism</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Mechanism</em>' containment reference.
     * @generated
     * @see #getMechanism()
     */
    void setMechanism(MechanismType value);

    /**
     * Returns the value of the '<em><b>Nominal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Data consists of unique names for spatial features
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Nominal</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Nominal' namespace='##targetNamespace'"
     * @generated
     * @see #setNominal(NominalType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Nominal()
     */
    NominalType getNominal();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getNominal <em>Nominal</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Nominal</em>' containment reference.
     * @generated
     * @see #getNominal()
     */
    void setNominal(NominalType value);

    /**
     * Returns the value of the '<em><b>Ordinal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Ordered classifications data
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Ordinal</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Ordinal' namespace='##targetNamespace'"
     * @generated
     * @see #setOrdinal(OrdinalType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Ordinal()
     */
    OrdinalType getOrdinal();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getOrdinal <em>Ordinal</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Ordinal</em>' containment reference.
     * @generated
     * @see #getOrdinal()
     */
    void setOrdinal(OrdinalType value);

    /**
     * Returns the value of the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable name of the organization responsible for maintaining this object.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Organization</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Organization' namespace='##targetNamespace'"
     * @generated
     * @see #setOrganization(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Organization()
     */
    String getOrganization();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getOrganization <em>Organization</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Organization</em>' attribute.
     * @generated
     * @see #getOrganization()
     */
    void setOrganization(String value);

    /**
     * Returns the value of the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Point in time to which the Framework/Dataset applies.  If the startDate attribute is included then the contents of this element describes a range of time (from "startDate" to "ReferenceDate") to which the framework/dataset applies.  Valid content is a date field of the form http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#gYear, gYearMonth, date, or dateTime.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Reference Date</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='ReferenceDate' namespace='##targetNamespace'"
     * @generated
     * @see #setReferenceDate(ReferenceDateType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_ReferenceDate()
     */
    ReferenceDateType getReferenceDate();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getReferenceDate <em>Reference Date</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Reference Date</em>' containment reference.
     * @generated
     * @see #getReferenceDate()
     */
    void setReferenceDate(ReferenceDateType value);

    /**
     * Returns the value of the '<em><b>Rowset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Dataset table with columns as defined in the "Columnset" structure above.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Rowset</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Rowset' namespace='##targetNamespace'"
     * @generated
     * @see #setRowset(RowsetType1)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Rowset()
     */
    RowsetType1 getRowset();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getRowset <em>Rowset</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Rowset</em>' containment reference.
     * @generated
     * @see #getRowset()
     */
    void setRowset(RowsetType1 value);

    /**
     * Returns the value of the '<em><b>Spatial Frameworks</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Full description of all spatial frameworks to which attribute data can be joined.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Spatial Frameworks</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='SpatialFrameworks' namespace='##targetNamespace'"
     * @generated
     * @see #setSpatialFrameworks(SpatialFrameworksType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_SpatialFrameworks()
     */
    SpatialFrameworksType getSpatialFrameworks();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getSpatialFrameworks <em>Spatial Frameworks</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Spatial Frameworks</em>' containment reference.
     * @generated
     * @see #getSpatialFrameworks()
     */
    void setSpatialFrameworks(SpatialFrameworksType value);

    /**
     * Returns the value of the '<em><b>Styling</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Describes a form of styling instruction supported by this server.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Styling</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Styling' namespace='##targetNamespace'"
     * @generated
     * @see #setStyling(StylingType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Styling()
     */
    StylingType getStyling();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getStyling <em>Styling</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Styling</em>' containment reference.
     * @generated
     * @see #getStyling()
     */
    void setStyling(StylingType value);

    /**
     * Returns the value of the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Human-readable short description suitable to display on a pick list, legend, and/or on mouseover.  Note that for attributes the unit of measure should not appear in the Title. Instead, it should appear in the UOM element.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Title</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Title' namespace='##targetNamespace'"
     * @generated
     * @see #setTitle(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Title()
     */
    String getTitle();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getTitle <em>Title</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Title</em>' attribute.
     * @generated
     * @see #getTitle()
     */
    void setTitle(String value);

    /**
     * Returns the value of the '<em><b>Uncertainty</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Standard Uncertainty, according to the generally agreed upon definition described at sites like http://physics.nist.gov/cuu/Uncertainty/index.html
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Uncertainty</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Uncertainty' namespace='##targetNamespace'"
     * @generated
     * @see #setUncertainty(UncertaintyType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Uncertainty()
     */
    UncertaintyType getUncertainty();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getUncertainty <em>Uncertainty</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Uncertainty</em>' containment reference.
     * @generated
     * @see #getUncertainty()
     */
    void setUncertainty(UncertaintyType value);

    /**
     * Returns the value of the '<em><b>UOM</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Unit of Measure
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>UOM</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='UOM' namespace='##targetNamespace'"
     * @generated
     * @see #setUOM(UOMType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_UOM()
     */
    UOMType getUOM();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getUOM <em>UOM</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>UOM</em>' containment reference.
     * @generated
     * @see #getUOM()
     */
    void setUOM(UOMType value);

    /**
     * Returns the value of the '<em><b>Values</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <p>
     * If the meaning of the '<em>Values</em>' containment reference isn't clear,
     * there really should be more of a description here...
     * </p>
     * <!-- end-user-doc -->
     *
     * @return the value of the '<em>Values</em>' containment reference.
     * @model containment="true" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Values' namespace='##targetNamespace'"
     * @generated
     * @see #setValues(ValuesType)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Values()
     */
    ValuesType getValues();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getValues <em>Values</em>}' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Values</em>' containment reference.
     * @generated
     * @see #getValues()
     */
    void setValues(ValuesType value);

    /**
     * Returns the value of the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * <!-- begin-model-doc -->
     * Version identifier for this Framework / Dataset.
     * <!-- end-model-doc -->
     *
     * @return the value of the '<em>Version</em>' attribute.
     * @model unique="false" dataType="org.eclipse.emf.ecore.xml.type.String" upper="-2" transient="true" volatile="true" derived="true"
     * extendedMetaData="kind='element' name='Version' namespace='##targetNamespace'"
     * @generated
     * @see #setVersion(String)
     * @see net.opengis.tjs10.Tjs10Package#getDocumentRoot_Version()
     */
    String getVersion();

    /**
     * Sets the value of the '{@link net.opengis.tjs10.DocumentRoot#getVersion <em>Version</em>}' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param value the new value of the '<em>Version</em>' attribute.
     * @generated
     * @see #getVersion()
     */
    void setVersion(String value);

} // DocumentRoot
