/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 * <li>each class,</li>
 * <li>each feature of each class,</li>
 * <li>each enum,</li>
 * <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * <!-- begin-model-doc -->
 * <p/>
 * <description>This XML Schema encodes the TJS GetData response.</description>
 * <copyright>
 * TJS is an OGC Standard.
 * Copyright (c) 2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * </copyright>
 * <p/>
 * <p/>
 * GML 3.0 candidate xlinks schema. Copyright (c) 2001 Open Geospatial Consortium.
 * <p/>
 * This XML Schema Document includes and imports, directly and indirectly, all the XML Schemas defined by the OWS Common Implemetation Specification.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the GetResourceByID operation request message. This typical operation is specified as a base for profiling in specific OWS specifications. For information on the allowed changes and limitations in such profiling, see Subclause 9.4.1 of the OWS Common specification.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the Exception Report response to all OWS operations.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the allowed values (or domain) of a quantity, often for an input or output parameter to an OWS. Such a parameter is sometimes called a variable, quantity, literal, or typed literal. Such a parameter can use one of many data types, including double, integer, boolean, string, or URI. The allowed values can also be encoded for a quantity that is not explicit or not transferred, but is constrained by a server implementation.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema  Document encodes the typical Contents section of an OWS service metadata (Capabilities) document. This  Schema can be built upon to define the Contents section for a specific OWS. If the ContentsBaseType in this XML Schema cannot be restricted and extended to define the Contents section for a specific OWS, all other relevant parts defined in owsContents.xsd shall be used by the "ContentsType" in the wxsContents.xsd prepared for the specific OWS.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document specifies types and elements for input and output of operation data, allowing including multiple data items with each data item either included or referenced. The contents of each type and element specified here can be restricted and/or extended for each use in a specific OWS specification.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document specifies types and elements for document or resource references and for package manifests that contain multiple references. The contents of each type and element specified here can be restricted and/or extended for each use in a specific OWS specification.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the parts of the MD_DataIdentification class of ISO 19115 (OGC Abstract Specification Topic 11) which are expected to be used for most datasets. This Schema also encodes the parts of this class that are expected to be useful for other metadata. Both may be used within the Contents section of OWS service metadata (Capabilities) documents.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes various parameters and parameter types that can be used in OWS operation requests and responses.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document defines the GetCapabilities operation request and response XML elements and types, which are common to all OWSs. This XML Schema shall be edited by each OWS, for example, to specify a specific value for the "service" attribute.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the common "ServiceIdentification" section of the GetCapabilities operation response, known as the Capabilities XML document. This section encodes the SV_ServiceIdentification class of ISO 19119 (OGC Abstract Specification Topic 12).
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the common "ServiceProvider" section of the GetCapabilities operation response, known as the Capabilities XML document. This section encodes the SV_ServiceProvider class of ISO 19119 (OGC Abstract Specification Topic 12).
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the basic contents of the "OperationsMetadata" section of the GetCapabilities operation response, also known as the Capabilities XML document.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * This XML Schema Document encodes the parts of ISO 19115 used by the common "ServiceIdentification" and "ServiceProvider" sections of the GetCapabilities operation response, known as the service metadata XML document. The parts encoded here are the MD_Keywords, CI_ResponsibleParty, and related classes. The UML package prefixes were omitted from XML names, and the XML element names were all capitalized, for consistency with other OWS Schemas. This document also provides a simple coding of text in multiple languages, simplified from Annex J of ISO 19115.
 * <p/>
 * OWS is an OGC Standard.
 * Copyright (c) 2006,2010 Open Geospatial Consortium.
 * To obtain additional rights of use, visit http://www.opengeospatial.org/legal/ .
 * <p/>
 * <!-- end-model-doc -->
 *
 * @model kind="package"
 * annotation="urn:opengis:specification:gml:schema-xlinks:v3.0c2 appinfo='xlinks.xsd v3.0b2 2001-07'"
 * annotation="http://www.w3.org/XML/1998/namespace lang='en'"
 * @generated
 * @see net.opengis.tjs10.Tjs10Factory
 */
public interface Tjs10Package extends EPackage {
    /**
     * The package name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    String eNAME = "tjs10";

    /**
     * The package namespace URI.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    String eNS_URI = "http://www.opengis.net/tjs/1.0";

    /**
     * The package namespace name.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    String eNS_PREFIX = "tjs";

    /**
     * The singleton instance of the package.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    Tjs10Package eINSTANCE = net.opengis.tjs10.impl.Tjs10PackageImpl.init();

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.AbstractTypeImpl <em>Abstract Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.AbstractTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getAbstractType()
     */
    int ABSTRACT_TYPE = 0;

    /**
     * The feature id for the '<em><b>Mixed</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ABSTRACT_TYPE__MIXED = XMLTypePackage.ANY_TYPE__MIXED;

    /**
     * The feature id for the '<em><b>Any</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ABSTRACT_TYPE__ANY = XMLTypePackage.ANY_TYPE__ANY;

    /**
     * The feature id for the '<em><b>Any Attribute</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ABSTRACT_TYPE__ANY_ATTRIBUTE = XMLTypePackage.ANY_TYPE__ANY_ATTRIBUTE;

    /**
     * The number of structural features of the '<em>Abstract Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ABSTRACT_TYPE_FEATURE_COUNT = XMLTypePackage.ANY_TYPE_FEATURE_COUNT + 0;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.AcceptVersionsTypeImpl <em>Accept Versions Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.AcceptVersionsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getAcceptVersionsType()
     */
    int ACCEPT_VERSIONS_TYPE = 1;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ACCEPT_VERSIONS_TYPE__VERSION = 0;

    /**
     * The number of structural features of the '<em>Accept Versions Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ACCEPT_VERSIONS_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.AttributeDataTypeImpl <em>Attribute Data Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.AttributeDataTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getAttributeDataType()
     */
    int ATTRIBUTE_DATA_TYPE = 2;

    /**
     * The feature id for the '<em><b>Get Data URL</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ATTRIBUTE_DATA_TYPE__GET_DATA_URL = 0;

    /**
     * The feature id for the '<em><b>Get Data XML</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ATTRIBUTE_DATA_TYPE__GET_DATA_XML = 1;

    /**
     * The number of structural features of the '<em>Attribute Data Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ATTRIBUTE_DATA_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.AttributesTypeImpl <em>Attributes Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.AttributesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getAttributesType()
     */
    int ATTRIBUTES_TYPE = 3;

    /**
     * The feature id for the '<em><b>Column</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ATTRIBUTES_TYPE__COLUMN = 0;

    /**
     * The number of structural features of the '<em>Attributes Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ATTRIBUTES_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.BoundingCoordinatesTypeImpl <em>Bounding Coordinates Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.BoundingCoordinatesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getBoundingCoordinatesType()
     */
    int BOUNDING_COORDINATES_TYPE = 4;

    /**
     * The feature id for the '<em><b>North</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int BOUNDING_COORDINATES_TYPE__NORTH = 0;

    /**
     * The feature id for the '<em><b>South</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int BOUNDING_COORDINATES_TYPE__SOUTH = 1;

    /**
     * The feature id for the '<em><b>East</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int BOUNDING_COORDINATES_TYPE__EAST = 2;

    /**
     * The feature id for the '<em><b>West</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int BOUNDING_COORDINATES_TYPE__WEST = 3;

    /**
     * The number of structural features of the '<em>Bounding Coordinates Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int BOUNDING_COORDINATES_TYPE_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ClassesTypeImpl <em>Classes Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ClassesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getClassesType()
     */
    int CLASSES_TYPE = 5;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE__TITLE = 0;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE__ABSTRACT = 1;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE__DOCUMENTATION = 2;

    /**
     * The feature id for the '<em><b>Value</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE__VALUE = 3;

    /**
     * The number of structural features of the '<em>Classes Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ClassesType1Impl <em>Classes Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ClassesType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getClassesType1()
     */
    int CLASSES_TYPE1 = 6;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE1__TITLE = 0;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE1__ABSTRACT = 1;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE1__DOCUMENTATION = 2;

    /**
     * The feature id for the '<em><b>Value</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE1__VALUE = 3;

    /**
     * The number of structural features of the '<em>Classes Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int CLASSES_TYPE1_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ColumnsetTypeImpl <em>Columnset Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ColumnsetTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getColumnsetType()
     */
    int COLUMNSET_TYPE = 7;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMNSET_TYPE__FRAMEWORK_KEY = 0;

    /**
     * The feature id for the '<em><b>Attributes</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMNSET_TYPE__ATTRIBUTES = 1;

    /**
     * The number of structural features of the '<em>Columnset Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMNSET_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ColumnTypeImpl <em>Column Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ColumnTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getColumnType()
     */
    int COLUMN_TYPE = 8;

    /**
     * The feature id for the '<em><b>Decimals</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE__DECIMALS = 0;

    /**
     * The feature id for the '<em><b>Length</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE__LENGTH = 1;

    /**
     * The feature id for the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE__NAME = 2;

    /**
     * The feature id for the '<em><b>Type</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE__TYPE = 3;

    /**
     * The number of structural features of the '<em>Column Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ColumnType1Impl <em>Column Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ColumnType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getColumnType1()
     */
    int COLUMN_TYPE1 = 9;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__TITLE = 0;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__ABSTRACT = 1;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__DOCUMENTATION = 2;

    /**
     * The feature id for the '<em><b>Values</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__VALUES = 3;

    /**
     * The feature id for the '<em><b>Get Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__GET_DATA_REQUEST = 4;

    /**
     * The feature id for the '<em><b>Decimals</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__DECIMALS = 5;

    /**
     * The feature id for the '<em><b>Length</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__LENGTH = 6;

    /**
     * The feature id for the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__NAME = 7;

    /**
     * The feature id for the '<em><b>Purpose</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__PURPOSE = 8;

    /**
     * The feature id for the '<em><b>Type</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1__TYPE = 9;

    /**
     * The number of structural features of the '<em>Column Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE1_FEATURE_COUNT = 10;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ColumnType2Impl <em>Column Type2</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ColumnType2Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getColumnType2()
     */
    int COLUMN_TYPE2 = 10;

    /**
     * The feature id for the '<em><b>Decimals</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE2__DECIMALS = 0;

    /**
     * The feature id for the '<em><b>Length</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE2__LENGTH = 1;

    /**
     * The feature id for the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE2__NAME = 2;

    /**
     * The feature id for the '<em><b>Type</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE2__TYPE = 3;

    /**
     * The number of structural features of the '<em>Column Type2</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COLUMN_TYPE2_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.CountTypeImpl <em>Count Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.CountTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getCountType()
     */
    int COUNT_TYPE = 11;

    /**
     * The feature id for the '<em><b>UOM</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COUNT_TYPE__UOM = 0;

    /**
     * The feature id for the '<em><b>Uncertainty</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COUNT_TYPE__UNCERTAINTY = 1;

    /**
     * The feature id for the '<em><b>Exceptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COUNT_TYPE__EXCEPTIONS = 2;

    /**
     * The number of structural features of the '<em>Count Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int COUNT_TYPE_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DataDescriptionsTypeImpl <em>Data Descriptions Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DataDescriptionsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDataDescriptionsType()
     */
    int DATA_DESCRIPTIONS_TYPE = 12;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_DESCRIPTIONS_TYPE__FRAMEWORK = 0;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_DESCRIPTIONS_TYPE__CAPABILITIES = 1;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_DESCRIPTIONS_TYPE__LANG = 2;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_DESCRIPTIONS_TYPE__SERVICE = 3;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_DESCRIPTIONS_TYPE__VERSION = 4;

    /**
     * The number of structural features of the '<em>Data Descriptions Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_DESCRIPTIONS_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DataInputsTypeImpl <em>Data Inputs Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DataInputsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDataInputsType()
     */
    int DATA_INPUTS_TYPE = 13;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_INPUTS_TYPE__FRAMEWORK = 0;

    /**
     * The number of structural features of the '<em>Data Inputs Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATA_INPUTS_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl <em>Dataset Descriptions Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DatasetDescriptionsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDatasetDescriptionsType()
     */
    int DATASET_DESCRIPTIONS_TYPE = 14;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_DESCRIPTIONS_TYPE__FRAMEWORK = 0;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_DESCRIPTIONS_TYPE__CAPABILITIES = 1;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_DESCRIPTIONS_TYPE__LANG = 2;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_DESCRIPTIONS_TYPE__SERVICE = 3;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_DESCRIPTIONS_TYPE__VERSION = 4;

    /**
     * The number of structural features of the '<em>Dataset Descriptions Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_DESCRIPTIONS_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DatasetTypeImpl <em>Dataset Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DatasetTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDatasetType()
     */
    int DATASET_TYPE = 15;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__DATASET_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Describe Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE__DESCRIBE_DATA_REQUEST = 7;

    /**
     * The number of structural features of the '<em>Dataset Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE_FEATURE_COUNT = 8;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DatasetType1Impl <em>Dataset Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DatasetType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDatasetType1()
     */
    int DATASET_TYPE1 = 16;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__DATASET_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Columnset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__COLUMNSET = 7;

    /**
     * The feature id for the '<em><b>Rowset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1__ROWSET = 8;

    /**
     * The number of structural features of the '<em>Dataset Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE1_FEATURE_COUNT = 9;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DatasetType2Impl <em>Dataset Type2</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DatasetType2Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDatasetType2()
     */
    int DATASET_TYPE2 = 17;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__DATASET_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Describe Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__DESCRIBE_DATA_REQUEST = 7;

    /**
     * The feature id for the '<em><b>Columnset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__COLUMNSET = 8;

    /**
     * The feature id for the '<em><b>Rowset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2__ROWSET = 9;

    /**
     * The number of structural features of the '<em>Dataset Type2</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE2_FEATURE_COUNT = 10;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DatasetType3Impl <em>Dataset Type3</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DatasetType3Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDatasetType3()
     */
    int DATASET_TYPE3 = 18;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__DATASET_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Describe Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__DESCRIBE_DATA_REQUEST = 7;

    /**
     * The feature id for the '<em><b>Columnset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3__COLUMNSET = 8;

    /**
     * The number of structural features of the '<em>Dataset Type3</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DATASET_TYPE3_FEATURE_COUNT = 9;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeDataRequestTypeImpl <em>Describe Data Request Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeDataRequestTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDataRequestType()
     */
    int DESCRIBE_DATA_REQUEST_TYPE = 19;

    /**
     * The feature id for the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_REQUEST_TYPE__HREF = 0;

    /**
     * The number of structural features of the '<em>Describe Data Request Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_REQUEST_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeDatasetsRequestTypeImpl <em>Describe Datasets Request Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeDatasetsRequestTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDatasetsRequestType()
     */
    int DESCRIBE_DATASETS_REQUEST_TYPE = 20;

    /**
     * The feature id for the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_REQUEST_TYPE__HREF = 0;

    /**
     * The number of structural features of the '<em>Describe Datasets Request Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_REQUEST_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.RequestBaseTypeImpl <em>Request Base Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.RequestBaseTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRequestBaseType()
     */
    int REQUEST_BASE_TYPE = 65;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REQUEST_BASE_TYPE__LANGUAGE = 0;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REQUEST_BASE_TYPE__SERVICE = 1;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REQUEST_BASE_TYPE__VERSION = 2;

    /**
     * The number of structural features of the '<em>Request Base Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REQUEST_BASE_TYPE_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeDatasetsTypeImpl <em>Describe Datasets Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeDatasetsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDatasetsType()
     */
    int DESCRIBE_DATASETS_TYPE = 21;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_TYPE__LANGUAGE = REQUEST_BASE_TYPE__LANGUAGE;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_TYPE__SERVICE = REQUEST_BASE_TYPE__SERVICE;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_TYPE__VERSION = REQUEST_BASE_TYPE__VERSION;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_TYPE__FRAMEWORK_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 0;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_TYPE__DATASET_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 1;

    /**
     * The number of structural features of the '<em>Describe Datasets Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATASETS_TYPE_FEATURE_COUNT = REQUEST_BASE_TYPE_FEATURE_COUNT + 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeDataTypeImpl <em>Describe Data Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeDataTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDataType()
     */
    int DESCRIBE_DATA_TYPE = 22;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE__LANGUAGE = REQUEST_BASE_TYPE__LANGUAGE;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE__SERVICE = REQUEST_BASE_TYPE__SERVICE;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE__VERSION = REQUEST_BASE_TYPE__VERSION;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE__FRAMEWORK_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 0;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE__DATASET_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 1;

    /**
     * The feature id for the '<em><b>Attributes</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE__ATTRIBUTES = REQUEST_BASE_TYPE_FEATURE_COUNT + 2;

    /**
     * The number of structural features of the '<em>Describe Data Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_DATA_TYPE_FEATURE_COUNT = REQUEST_BASE_TYPE_FEATURE_COUNT + 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl <em>Describe Framework Key Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeFrameworkKeyTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeFrameworkKeyType()
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE = 23;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__BOUNDING_COORDINATES = 8;

    /**
     * The feature id for the '<em><b>Rowset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE__ROWSET = 9;

    /**
     * The number of structural features of the '<em>Describe Framework Key Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORK_KEY_TYPE_FEATURE_COUNT = 10;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeFrameworksTypeImpl <em>Describe Frameworks Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeFrameworksTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeFrameworksType()
     */
    int DESCRIBE_FRAMEWORKS_TYPE = 24;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORKS_TYPE__LANGUAGE = REQUEST_BASE_TYPE__LANGUAGE;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORKS_TYPE__SERVICE = REQUEST_BASE_TYPE__SERVICE;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORKS_TYPE__VERSION = REQUEST_BASE_TYPE__VERSION;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORKS_TYPE__FRAMEWORK_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 0;

    /**
     * The number of structural features of the '<em>Describe Frameworks Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_FRAMEWORKS_TYPE_FEATURE_COUNT = REQUEST_BASE_TYPE_FEATURE_COUNT + 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DescribeKeyTypeImpl <em>Describe Key Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DescribeKeyTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeKeyType()
     */
    int DESCRIBE_KEY_TYPE = 25;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_KEY_TYPE__LANGUAGE = REQUEST_BASE_TYPE__LANGUAGE;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_KEY_TYPE__SERVICE = REQUEST_BASE_TYPE__SERVICE;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_KEY_TYPE__VERSION = REQUEST_BASE_TYPE__VERSION;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_KEY_TYPE__FRAMEWORK_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 0;

    /**
     * The number of structural features of the '<em>Describe Key Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DESCRIBE_KEY_TYPE_FEATURE_COUNT = REQUEST_BASE_TYPE_FEATURE_COUNT + 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.DocumentRootImpl <em>Document Root</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.DocumentRootImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDocumentRoot()
     */
    int DOCUMENT_ROOT = 26;

    /**
     * The feature id for the '<em><b>Mixed</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__MIXED = 0;

    /**
     * The feature id for the '<em><b>XMLNS Prefix Map</b></em>' map.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__XMLNS_PREFIX_MAP = 1;

    /**
     * The feature id for the '<em><b>XSI Schema Location</b></em>' map.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Attribute Limit</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__ATTRIBUTE_LIMIT = 4;

    /**
     * The feature id for the '<em><b>Attributes</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__ATTRIBUTES = 5;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__BOUNDING_COORDINATES = 6;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__CAPABILITIES = 7;

    /**
     * The feature id for the '<em><b>Columnset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__COLUMNSET = 8;

    /**
     * The feature id for the '<em><b>Count</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__COUNT = 9;

    /**
     * The feature id for the '<em><b>Data Class</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DATA_CLASS = 10;

    /**
     * The feature id for the '<em><b>Data Descriptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DATA_DESCRIPTIONS = 11;

    /**
     * The feature id for the '<em><b>Dataset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DATASET = 12;

    /**
     * The feature id for the '<em><b>Dataset Descriptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DATASET_DESCRIPTIONS = 13;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DATASET_URI = 14;

    /**
     * The feature id for the '<em><b>Describe Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_DATA = 15;

    /**
     * The feature id for the '<em><b>Describe Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_DATA_REQUEST = 16;

    /**
     * The feature id for the '<em><b>Describe Datasets</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_DATASETS = 17;

    /**
     * The feature id for the '<em><b>Describe Datasets Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_DATASETS_REQUEST = 18;

    /**
     * The feature id for the '<em><b>Describe Frameworks</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_FRAMEWORKS = 19;

    /**
     * The feature id for the '<em><b>Describe Join Abilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_JOIN_ABILITIES = 20;

    /**
     * The feature id for the '<em><b>Describe Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DESCRIBE_KEY = 21;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__DOCUMENTATION = 22;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__FRAMEWORK = 23;

    /**
     * The feature id for the '<em><b>Framework Descriptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__FRAMEWORK_DESCRIPTIONS = 24;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__FRAMEWORK_KEY = 25;

    /**
     * The feature id for the '<em><b>Framework Key Description</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__FRAMEWORK_KEY_DESCRIPTION = 26;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__FRAMEWORK_URI = 27;

    /**
     * The feature id for the '<em><b>GDAS</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__GDAS = 28;

    /**
     * The feature id for the '<em><b>Get Capabilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__GET_CAPABILITIES = 29;

    /**
     * The feature id for the '<em><b>Get Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__GET_DATA = 30;

    /**
     * The feature id for the '<em><b>Get Data Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__GET_DATA_REQUEST = 31;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__IDENTIFIER = 32;

    /**
     * The feature id for the '<em><b>Join Abilities</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__JOIN_ABILITIES = 33;

    /**
     * The feature id for the '<em><b>Join Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__JOIN_DATA = 34;

    /**
     * The feature id for the '<em><b>Join Data Response</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__JOIN_DATA_RESPONSE = 35;

    /**
     * The feature id for the '<em><b>K</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__K = 36;

    /**
     * The feature id for the '<em><b>Linkage Keys</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__LINKAGE_KEYS = 37;

    /**
     * The feature id for the '<em><b>Measure</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__MEASURE = 38;

    /**
     * The feature id for the '<em><b>Mechanism</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__MECHANISM = 39;

    /**
     * The feature id for the '<em><b>Nominal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__NOMINAL = 40;

    /**
     * The feature id for the '<em><b>Ordinal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__ORDINAL = 41;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__ORGANIZATION = 42;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__REFERENCE_DATE = 43;

    /**
     * The feature id for the '<em><b>Rowset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__ROWSET = 44;

    /**
     * The feature id for the '<em><b>Spatial Frameworks</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__SPATIAL_FRAMEWORKS = 45;

    /**
     * The feature id for the '<em><b>Styling</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__STYLING = 46;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__TITLE = 47;

    /**
     * The feature id for the '<em><b>Uncertainty</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__UNCERTAINTY = 48;

    /**
     * The feature id for the '<em><b>UOM</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__UOM = 49;

    /**
     * The feature id for the '<em><b>Values</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__VALUES = 50;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT__VERSION = 51;

    /**
     * The number of structural features of the '<em>Document Root</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int DOCUMENT_ROOT_FEATURE_COUNT = 52;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ExceptionReportTypeImpl <em>Exception Report Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ExceptionReportTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getExceptionReportType()
     */
    int EXCEPTION_REPORT_TYPE = 27;

    /**
     * The feature id for the '<em><b>Exception</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int EXCEPTION_REPORT_TYPE__EXCEPTION = 0;

    /**
     * The number of structural features of the '<em>Exception Report Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int EXCEPTION_REPORT_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FailedTypeImpl <em>Failed Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FailedTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFailedType()
     */
    int FAILED_TYPE = 28;

    /**
     * The number of structural features of the '<em>Failed Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FAILED_TYPE_FEATURE_COUNT = 0;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkDatasetDescribeDataTypeImpl <em>Framework Dataset Describe Data Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkDatasetDescribeDataTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkDatasetDescribeDataType()
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE = 29;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__BOUNDING_COORDINATES = 8;

    /**
     * The feature id for the '<em><b>Describe Datasets Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__DESCRIBE_DATASETS_REQUEST = 9;

    /**
     * The feature id for the '<em><b>Dataset</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE__DATASET = 10;

    /**
     * The number of structural features of the '<em>Framework Dataset Describe Data Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE_FEATURE_COUNT = 11;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkDescriptionsTypeImpl <em>Framework Descriptions Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkDescriptionsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkDescriptionsType()
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE = 30;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE__FRAMEWORK = 0;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE__CAPABILITIES = 1;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE__LANG = 2;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE__SERVICE = 3;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE__VERSION = 4;

    /**
     * The number of structural features of the '<em>Framework Descriptions Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_DESCRIPTIONS_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkKeyDescriptionTypeImpl <em>Framework Key Description Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkKeyDescriptionTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkKeyDescriptionType()
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE = 31;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE__FRAMEWORK = 0;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE__CAPABILITIES = 1;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE__LANG = 2;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE__SERVICE = 3;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE__VERSION = 4;

    /**
     * The number of structural features of the '<em>Framework Key Description Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_DESCRIPTION_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkKeyTypeImpl <em>Framework Key Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkKeyTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkKeyType()
     */
    int FRAMEWORK_KEY_TYPE = 32;

    /**
     * The feature id for the '<em><b>Column</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_TYPE__COLUMN = 0;

    /**
     * The number of structural features of the '<em>Framework Key Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkKeyType1Impl <em>Framework Key Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkKeyType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkKeyType1()
     */
    int FRAMEWORK_KEY_TYPE1 = 33;

    /**
     * The feature id for the '<em><b>Column</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_TYPE1__COLUMN = 0;

    /**
     * The feature id for the '<em><b>Complete</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_TYPE1__COMPLETE = 1;

    /**
     * The feature id for the '<em><b>Relationship</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_TYPE1__RELATIONSHIP = 2;

    /**
     * The number of structural features of the '<em>Framework Key Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_KEY_TYPE1_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkTypeImpl <em>Framework Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkType()
     */
    int FRAMEWORK_TYPE = 34;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE__BOUNDING_COORDINATES = 8;

    /**
     * The number of structural features of the '<em>Framework Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE_FEATURE_COUNT = 9;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkType1Impl <em>Framework Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkType1()
     */
    int FRAMEWORK_TYPE1 = 35;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__BOUNDING_COORDINATES = 8;

    /**
     * The feature id for the '<em><b>Dataset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1__DATASET = 9;

    /**
     * The number of structural features of the '<em>Framework Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE1_FEATURE_COUNT = 10;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkType2Impl <em>Framework Type2</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkType2Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkType2()
     */
    int FRAMEWORK_TYPE2 = 36;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__BOUNDING_COORDINATES = 8;

    /**
     * The feature id for the '<em><b>Describe Datasets Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2__DESCRIBE_DATASETS_REQUEST = 9;

    /**
     * The number of structural features of the '<em>Framework Type2</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE2_FEATURE_COUNT = 10;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkType3Impl <em>Framework Type3</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkType3Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkType3()
     */
    int FRAMEWORK_TYPE3 = 37;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__BOUNDING_COORDINATES = 8;

    /**
     * The feature id for the '<em><b>Describe Datasets Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__DESCRIBE_DATASETS_REQUEST = 9;

    /**
     * The feature id for the '<em><b>Dataset</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3__DATASET = 10;

    /**
     * The number of structural features of the '<em>Framework Type3</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE3_FEATURE_COUNT = 11;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.FrameworkType4Impl <em>Framework Type4</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.FrameworkType4Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getFrameworkType4()
     */
    int FRAMEWORK_TYPE4 = 38;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Organization</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__ORGANIZATION = 1;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__TITLE = 2;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__ABSTRACT = 3;

    /**
     * The feature id for the '<em><b>Reference Date</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__REFERENCE_DATE = 4;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__VERSION = 5;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__DOCUMENTATION = 6;

    /**
     * The feature id for the '<em><b>Framework Key</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__FRAMEWORK_KEY = 7;

    /**
     * The feature id for the '<em><b>Bounding Coordinates</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__BOUNDING_COORDINATES = 8;

    /**
     * The feature id for the '<em><b>Describe Datasets Request</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__DESCRIBE_DATASETS_REQUEST = 9;

    /**
     * The feature id for the '<em><b>Dataset</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4__DATASET = 10;

    /**
     * The number of structural features of the '<em>Framework Type4</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int FRAMEWORK_TYPE4_FEATURE_COUNT = 11;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.GDASTypeImpl <em>GDAS Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.GDASTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGDASType()
     */
    int GDAS_TYPE = 39;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GDAS_TYPE__FRAMEWORK = 0;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GDAS_TYPE__CAPABILITIES = 1;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GDAS_TYPE__LANG = 2;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GDAS_TYPE__SERVICE = 3;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GDAS_TYPE__VERSION = 4;

    /**
     * The number of structural features of the '<em>GDAS Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GDAS_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.GetCapabilitiesTypeImpl <em>Get Capabilities Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.GetCapabilitiesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetCapabilitiesType()
     */
    int GET_CAPABILITIES_TYPE = 40;

    /**
     * The feature id for the '<em><b>Accept Versions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE__ACCEPT_VERSIONS = 0;

    /**
     * The feature id for the '<em><b>Sections</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE__SECTIONS = 1;

    /**
     * The feature id for the '<em><b>Accept Formats</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE__ACCEPT_FORMATS = 2;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE__LANGUAGE = 3;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE__SERVICE = 4;

    /**
     * The feature id for the '<em><b>Update Sequence</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE__UPDATE_SEQUENCE = 5;

    /**
     * The number of structural features of the '<em>Get Capabilities Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_CAPABILITIES_TYPE_FEATURE_COUNT = 6;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.GetDataRequestTypeImpl <em>Get Data Request Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.GetDataRequestTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetDataRequestType()
     */
    int GET_DATA_REQUEST_TYPE = 41;

    /**
     * The feature id for the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_REQUEST_TYPE__HREF = 0;

    /**
     * The number of structural features of the '<em>Get Data Request Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_REQUEST_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.GetDataTypeImpl <em>Get Data Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.GetDataTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetDataType()
     */
    int GET_DATA_TYPE = 42;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__LANGUAGE = REQUEST_BASE_TYPE__LANGUAGE;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__SERVICE = REQUEST_BASE_TYPE__SERVICE;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__VERSION = REQUEST_BASE_TYPE__VERSION;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__FRAMEWORK_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 0;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__DATASET_URI = REQUEST_BASE_TYPE_FEATURE_COUNT + 1;

    /**
     * The feature id for the '<em><b>Attributes</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__ATTRIBUTES = REQUEST_BASE_TYPE_FEATURE_COUNT + 2;

    /**
     * The feature id for the '<em><b>Linkage Keys</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__LINKAGE_KEYS = REQUEST_BASE_TYPE_FEATURE_COUNT + 3;

    /**
     * The feature id for the '<em><b>Filter Column</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__FILTER_COLUMN = REQUEST_BASE_TYPE_FEATURE_COUNT + 4;

    /**
     * The feature id for the '<em><b>Filter Value</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__FILTER_VALUE = REQUEST_BASE_TYPE_FEATURE_COUNT + 5;

    /**
     * The feature id for the '<em><b>XSL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__XSL = REQUEST_BASE_TYPE_FEATURE_COUNT + 6;

    /**
     * The feature id for the '<em><b>Aid</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE__AID = REQUEST_BASE_TYPE_FEATURE_COUNT + 7;

    /**
     * The number of structural features of the '<em>Get Data Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_TYPE_FEATURE_COUNT = REQUEST_BASE_TYPE_FEATURE_COUNT + 8;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.GetDataXMLTypeImpl <em>Get Data XML Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.GetDataXMLTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetDataXMLType()
     */
    int GET_DATA_XML_TYPE = 43;

    /**
     * The feature id for the '<em><b>Framework URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE__FRAMEWORK_URI = 0;

    /**
     * The feature id for the '<em><b>Dataset URI</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE__DATASET_URI = 1;

    /**
     * The feature id for the '<em><b>Attributes</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE__ATTRIBUTES = 2;

    /**
     * The feature id for the '<em><b>Linkage Keys</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE__LINKAGE_KEYS = 3;

    /**
     * The feature id for the '<em><b>Get Data Host</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE__GET_DATA_HOST = 4;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE__LANGUAGE = 5;

    /**
     * The number of structural features of the '<em>Get Data XML Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int GET_DATA_XML_TYPE_FEATURE_COUNT = 6;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.JoinAbilitiesTypeImpl <em>Join Abilities Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.JoinAbilitiesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getJoinAbilitiesType()
     */
    int JOIN_ABILITIES_TYPE = 44;

    /**
     * The feature id for the '<em><b>Spatial Frameworks</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__SPATIAL_FRAMEWORKS = 0;

    /**
     * The feature id for the '<em><b>Attribute Limit</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__ATTRIBUTE_LIMIT = 1;

    /**
     * The feature id for the '<em><b>Output Mechanisms</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__OUTPUT_MECHANISMS = 2;

    /**
     * The feature id for the '<em><b>Output Stylings</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__OUTPUT_STYLINGS = 3;

    /**
     * The feature id for the '<em><b>Classification Schema URL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__CLASSIFICATION_SCHEMA_URL = 4;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__CAPABILITIES = 5;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__LANG = 6;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__SERVICE = 7;

    /**
     * The feature id for the '<em><b>Update Supported</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__UPDATE_SUPPORTED = 8;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE__VERSION = 9;

    /**
     * The number of structural features of the '<em>Join Abilities Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_ABILITIES_TYPE_FEATURE_COUNT = 10;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.JoinDataResponseTypeImpl <em>Join Data Response Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.JoinDataResponseTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getJoinDataResponseType()
     */
    int JOIN_DATA_RESPONSE_TYPE = 45;

    /**
     * The feature id for the '<em><b>Status</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__STATUS = 0;

    /**
     * The feature id for the '<em><b>Data Inputs</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__DATA_INPUTS = 1;

    /**
     * The feature id for the '<em><b>Joined Outputs</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__JOINED_OUTPUTS = 2;

    /**
     * The feature id for the '<em><b>Capabilities</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__CAPABILITIES = 3;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__LANG = 4;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__SERVICE = 5;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE__VERSION = 6;

    /**
     * The number of structural features of the '<em>Join Data Response Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_RESPONSE_TYPE_FEATURE_COUNT = 7;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.JoinDataTypeImpl <em>Join Data Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.JoinDataTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getJoinDataType()
     */
    int JOIN_DATA_TYPE = 46;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__LANGUAGE = REQUEST_BASE_TYPE__LANGUAGE;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__SERVICE = REQUEST_BASE_TYPE__SERVICE;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__VERSION = REQUEST_BASE_TYPE__VERSION;

    /**
     * The feature id for the '<em><b>Attribute Data</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__ATTRIBUTE_DATA = REQUEST_BASE_TYPE_FEATURE_COUNT + 0;

    /**
     * The feature id for the '<em><b>Map Styling</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__MAP_STYLING = REQUEST_BASE_TYPE_FEATURE_COUNT + 1;

    /**
     * The feature id for the '<em><b>Classification URL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__CLASSIFICATION_URL = REQUEST_BASE_TYPE_FEATURE_COUNT + 2;

    /**
     * The feature id for the '<em><b>Update</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE__UPDATE = REQUEST_BASE_TYPE_FEATURE_COUNT + 3;

    /**
     * The number of structural features of the '<em>Join Data Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOIN_DATA_TYPE_FEATURE_COUNT = REQUEST_BASE_TYPE_FEATURE_COUNT + 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.JoinedOutputsTypeImpl <em>Joined Outputs Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.JoinedOutputsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getJoinedOutputsType()
     */
    int JOINED_OUTPUTS_TYPE = 47;

    /**
     * The feature id for the '<em><b>Output</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOINED_OUTPUTS_TYPE__OUTPUT = 0;

    /**
     * The number of structural features of the '<em>Joined Outputs Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int JOINED_OUTPUTS_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.KTypeImpl <em>KType</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.KTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getKType()
     */
    int KTYPE = 48;

    int SHORTFORM_TYPE = 100;

    int LONGFORM_TYPE = 102;

    /**
     * The feature id for the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int KTYPE__VALUE = 0;

    int SHORTFORM_TYPE__VALUE = 0;

    int LONGFORM_TYPE__VALUE = 0;

    /**
     * The feature id for the '<em><b>Aid</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int KTYPE__AID = 1;

    /**
     * The number of structural features of the '<em>KType</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int KTYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.LanguagesTypeImpl <em>Languages Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.LanguagesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getLanguagesType()
     */
    int LANGUAGES_TYPE = 49;

    /**
     * The feature id for the '<em><b>Language</b></em>' attribute list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int LANGUAGES_TYPE__LANGUAGE = 0;

    /**
     * The number of structural features of the '<em>Languages Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int LANGUAGES_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.MapStylingTypeImpl <em>Map Styling Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.MapStylingTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getMapStylingType()
     */
    int MAP_STYLING_TYPE = 50;

    /**
     * The feature id for the '<em><b>Styling Identifier</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MAP_STYLING_TYPE__STYLING_IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Styling URL</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MAP_STYLING_TYPE__STYLING_URL = 1;

    /**
     * The number of structural features of the '<em>Map Styling Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MAP_STYLING_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.MeasureCountExceptionsImpl <em>Measure Count Exceptions</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.MeasureCountExceptionsImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getMeasureCountExceptions()
     */
    int MEASURE_COUNT_EXCEPTIONS = 51;

    /**
     * The feature id for the '<em><b>Null</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MEASURE_COUNT_EXCEPTIONS__NULL = 0;

    /**
     * The number of structural features of the '<em>Measure Count Exceptions</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MEASURE_COUNT_EXCEPTIONS_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.MeasureTypeImpl <em>Measure Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.MeasureTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getMeasureType()
     */
    int MEASURE_TYPE = 52;

    /**
     * The feature id for the '<em><b>UOM</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MEASURE_TYPE__UOM = 0;

    /**
     * The feature id for the '<em><b>Uncertainty</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MEASURE_TYPE__UNCERTAINTY = 1;

    /**
     * The feature id for the '<em><b>Exceptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MEASURE_TYPE__EXCEPTIONS = 2;

    /**
     * The number of structural features of the '<em>Measure Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MEASURE_TYPE_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.MechanismTypeImpl <em>Mechanism Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.MechanismTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getMechanismType()
     */
    int MECHANISM_TYPE = 53;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MECHANISM_TYPE__IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MECHANISM_TYPE__TITLE = 1;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MECHANISM_TYPE__ABSTRACT = 2;

    /**
     * The feature id for the '<em><b>Reference</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MECHANISM_TYPE__REFERENCE = 3;

    /**
     * The number of structural features of the '<em>Mechanism Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int MECHANISM_TYPE_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.NominalOrdinalExceptionsImpl <em>Nominal Ordinal Exceptions</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.NominalOrdinalExceptionsImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getNominalOrdinalExceptions()
     */
    int NOMINAL_ORDINAL_EXCEPTIONS = 54;

    /**
     * The feature id for the '<em><b>Null</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NOMINAL_ORDINAL_EXCEPTIONS__NULL = 0;

    /**
     * The number of structural features of the '<em>Nominal Ordinal Exceptions</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NOMINAL_ORDINAL_EXCEPTIONS_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.NominalTypeImpl <em>Nominal Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.NominalTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getNominalType()
     */
    int NOMINAL_TYPE = 55;

    /**
     * The feature id for the '<em><b>Classes</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NOMINAL_TYPE__CLASSES = 0;

    /**
     * The feature id for the '<em><b>Exceptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NOMINAL_TYPE__EXCEPTIONS = 1;

    /**
     * The number of structural features of the '<em>Nominal Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NOMINAL_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.NullTypeImpl <em>Null Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.NullTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getNullType()
     */
    int NULL_TYPE = 56;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE__IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE__TITLE = 1;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE__ABSTRACT = 2;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE__DOCUMENTATION = 3;

    /**
     * The number of structural features of the '<em>Null Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.NullType1Impl <em>Null Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.NullType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getNullType1()
     */
    int NULL_TYPE1 = 57;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE1__IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE1__TITLE = 1;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE1__ABSTRACT = 2;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE1__DOCUMENTATION = 3;

    /**
     * The feature id for the '<em><b>Color</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE1__COLOR = 4;

    /**
     * The number of structural features of the '<em>Null Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int NULL_TYPE1_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.OrdinalTypeImpl <em>Ordinal Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.OrdinalTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getOrdinalType()
     */
    int ORDINAL_TYPE = 58;

    /**
     * The feature id for the '<em><b>Classes</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ORDINAL_TYPE__CLASSES = 0;

    /**
     * The feature id for the '<em><b>Exceptions</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ORDINAL_TYPE__EXCEPTIONS = 1;

    /**
     * The number of structural features of the '<em>Ordinal Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ORDINAL_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.OutputMechanismsTypeImpl <em>Output Mechanisms Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.OutputMechanismsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getOutputMechanismsType()
     */
    int OUTPUT_MECHANISMS_TYPE = 59;

    /**
     * The feature id for the '<em><b>Mechanism</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_MECHANISMS_TYPE__MECHANISM = 0;

    /**
     * The number of structural features of the '<em>Output Mechanisms Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_MECHANISMS_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.OutputStylingsTypeImpl <em>Output Stylings Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.OutputStylingsTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getOutputStylingsType()
     */
    int OUTPUT_STYLINGS_TYPE = 60;

    /**
     * The feature id for the '<em><b>Styling</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_STYLINGS_TYPE__STYLING = 0;

    /**
     * The number of structural features of the '<em>Output Stylings Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_STYLINGS_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.OutputStylingsType1Impl <em>Output Stylings Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.OutputStylingsType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getOutputStylingsType1()
     */
    int OUTPUT_STYLINGS_TYPE1 = 61;

    /**
     * The feature id for the '<em><b>Styling</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_STYLINGS_TYPE1__STYLING = OUTPUT_STYLINGS_TYPE__STYLING;

    /**
     * The number of structural features of the '<em>Output Stylings Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_STYLINGS_TYPE1_FEATURE_COUNT = OUTPUT_STYLINGS_TYPE_FEATURE_COUNT + 0;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.OutputTypeImpl <em>Output Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.OutputTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getOutputType()
     */
    int OUTPUT_TYPE = 62;

    /**
     * The feature id for the '<em><b>Mechanism</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_TYPE__MECHANISM = 0;

    /**
     * The feature id for the '<em><b>Resource</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_TYPE__RESOURCE = 1;

    /**
     * The feature id for the '<em><b>Exception Report</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_TYPE__EXCEPTION_REPORT = 2;

    /**
     * The number of structural features of the '<em>Output Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int OUTPUT_TYPE_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ParameterTypeImpl <em>Parameter Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ParameterTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getParameterType()
     */
    int PARAMETER_TYPE = 63;

    /**
     * The feature id for the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int PARAMETER_TYPE__VALUE = 0;

    /**
     * The feature id for the '<em><b>Name</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int PARAMETER_TYPE__NAME = 1;

    /**
     * The number of structural features of the '<em>Parameter Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int PARAMETER_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ReferenceDateTypeImpl <em>Reference Date Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ReferenceDateTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getReferenceDateType()
     */
    int REFERENCE_DATE_TYPE = 64;

    /**
     * The feature id for the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REFERENCE_DATE_TYPE__VALUE = 0;

    /**
     * The feature id for the '<em><b>Start Date</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REFERENCE_DATE_TYPE__START_DATE = 1;

    /**
     * The number of structural features of the '<em>Reference Date Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int REFERENCE_DATE_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ResourceTypeImpl <em>Resource Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ResourceTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getResourceType()
     */
    int RESOURCE_TYPE = 66;

    /**
     * The feature id for the '<em><b>URL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int RESOURCE_TYPE__URL = 0;

    /**
     * The feature id for the '<em><b>Parameter</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int RESOURCE_TYPE__PARAMETER = 1;

    /**
     * The number of structural features of the '<em>Resource Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int RESOURCE_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.RowsetTypeImpl <em>Rowset Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.RowsetTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRowsetType()
     */
    int ROWSET_TYPE = 67;

    /**
     * The feature id for the '<em><b>Row</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROWSET_TYPE__ROW = 0;

    /**
     * The number of structural features of the '<em>Rowset Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROWSET_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.RowsetType1Impl <em>Rowset Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.RowsetType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRowsetType1()
     */
    int ROWSET_TYPE1 = 68;

    /**
     * The feature id for the '<em><b>Row</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROWSET_TYPE1__ROW = 0;

    /**
     * The number of structural features of the '<em>Rowset Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROWSET_TYPE1_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.RowTypeImpl <em>Row Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.RowTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRowType()
     */
    int ROW_TYPE = 69;

    /**
     * The feature id for the '<em><b>K</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROW_TYPE__K = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROW_TYPE__TITLE = 1;

    /**
     * The number of structural features of the '<em>Row Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROW_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.RowType1Impl <em>Row Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.RowType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRowType1()
     */
    int ROW_TYPE1 = 70;

    /**
     * The feature id for the '<em><b>K</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROW_TYPE1__K = 0;

    /**
     * The feature id for the '<em><b>V</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROW_TYPE1__V = 1;

    /**
     * The number of structural features of the '<em>Row Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int ROW_TYPE1_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.SpatialFrameworksTypeImpl <em>Spatial Frameworks Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.SpatialFrameworksTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getSpatialFrameworksType()
     */
    int SPATIAL_FRAMEWORKS_TYPE = 71;

    /**
     * The feature id for the '<em><b>Framework</b></em>' containment reference list.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int SPATIAL_FRAMEWORKS_TYPE__FRAMEWORK = 0;

    /**
     * The number of structural features of the '<em>Spatial Frameworks Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int SPATIAL_FRAMEWORKS_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.StatusTypeImpl <em>Status Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.StatusTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getStatusType()
     */
    int STATUS_TYPE = 72;

    /**
     * The feature id for the '<em><b>Accepted</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STATUS_TYPE__ACCEPTED = 0;

    /**
     * The feature id for the '<em><b>Completed</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STATUS_TYPE__COMPLETED = 1;

    /**
     * The feature id for the '<em><b>Failed</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STATUS_TYPE__FAILED = 2;

    /**
     * The feature id for the '<em><b>Creation Time</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STATUS_TYPE__CREATION_TIME = 3;

    /**
     * The feature id for the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STATUS_TYPE__HREF = 4;

    /**
     * The number of structural features of the '<em>Status Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STATUS_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.StylingTypeImpl <em>Styling Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.StylingTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getStylingType()
     */
    int STYLING_TYPE = 73;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STYLING_TYPE__IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STYLING_TYPE__TITLE = 1;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STYLING_TYPE__ABSTRACT = 2;

    /**
     * The feature id for the '<em><b>Reference</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STYLING_TYPE__REFERENCE = 3;

    /**
     * The feature id for the '<em><b>Schema</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STYLING_TYPE__SCHEMA = 4;

    /**
     * The number of structural features of the '<em>Styling Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int STYLING_TYPE_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl <em>Tjs Capabilities Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.TjsCapabilitiesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getTjsCapabilitiesType()
     */
    int TJS_CAPABILITIES_TYPE = 74;

    /**
     * The feature id for the '<em><b>Service Identification</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__SERVICE_IDENTIFICATION = 0;

    /**
     * The feature id for the '<em><b>Service Provider</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__SERVICE_PROVIDER = 1;

    /**
     * The feature id for the '<em><b>Operations Metadata</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__OPERATIONS_METADATA = 2;

    /**
     * The feature id for the '<em><b>Languages</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__LANGUAGES = 3;

    /**
     * The feature id for the '<em><b>WSDL</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__WSDL = 4;

    /**
     * The feature id for the '<em><b>Lang</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__LANG = 5;

    /**
     * The feature id for the '<em><b>Service</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__SERVICE = 6;

    /**
     * The feature id for the '<em><b>Update Sequence</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__UPDATE_SEQUENCE = 7;

    /**
     * The feature id for the '<em><b>Version</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE__VERSION = 8;

    /**
     * The number of structural features of the '<em>Tjs Capabilities Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int TJS_CAPABILITIES_TYPE_FEATURE_COUNT = 9;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.UncertaintyTypeImpl <em>Uncertainty Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.UncertaintyTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getUncertaintyType()
     */
    int UNCERTAINTY_TYPE = 75;

    /**
     * The feature id for the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UNCERTAINTY_TYPE__VALUE = 0;

    /**
     * The feature id for the '<em><b>Gaussian</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UNCERTAINTY_TYPE__GAUSSIAN = 1;

    /**
     * The number of structural features of the '<em>Uncertainty Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UNCERTAINTY_TYPE_FEATURE_COUNT = 2;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.UOMTypeImpl <em>UOM Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.UOMTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getUOMType()
     */
    int UOM_TYPE = 76;

    /**
     * The feature id for the '<em><b>Short Form</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UOM_TYPE__SHORT_FORM = 0;

    /**
     * The feature id for the '<em><b>Long Form</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UOM_TYPE__LONG_FORM = 1;

    /**
     * The feature id for the '<em><b>Reference</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UOM_TYPE__REFERENCE = 2;

    /**
     * The number of structural features of the '<em>UOM Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int UOM_TYPE_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ValuesTypeImpl <em>Values Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ValuesTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getValuesType()
     */
    int VALUES_TYPE = 77;

    /**
     * The feature id for the '<em><b>Nominal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUES_TYPE__NOMINAL = 0;

    /**
     * The feature id for the '<em><b>Ordinal</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUES_TYPE__ORDINAL = 1;

    /**
     * The feature id for the '<em><b>Count</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUES_TYPE__COUNT = 2;

    /**
     * The feature id for the '<em><b>Measure</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUES_TYPE__MEASURE = 3;

    /**
     * The number of structural features of the '<em>Values Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUES_TYPE_FEATURE_COUNT = 4;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ValueTypeImpl <em>Value Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ValueTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getValueType()
     */
    int VALUE_TYPE = 78;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE__IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE__TITLE = 1;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE__ABSTRACT = 2;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE__DOCUMENTATION = 3;

    /**
     * The feature id for the '<em><b>Color</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE__COLOR = 4;

    /**
     * The feature id for the '<em><b>Rank</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE__RANK = 5;

    /**
     * The number of structural features of the '<em>Value Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE_FEATURE_COUNT = 6;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.ValueType1Impl <em>Value Type1</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.ValueType1Impl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getValueType1()
     */
    int VALUE_TYPE1 = 79;

    /**
     * The feature id for the '<em><b>Identifier</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE1__IDENTIFIER = 0;

    /**
     * The feature id for the '<em><b>Title</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE1__TITLE = 1;

    /**
     * The feature id for the '<em><b>Abstract</b></em>' containment reference.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE1__ABSTRACT = 2;

    /**
     * The feature id for the '<em><b>Documentation</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE1__DOCUMENTATION = 3;

    /**
     * The feature id for the '<em><b>Color</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE1__COLOR = 4;

    /**
     * The number of structural features of the '<em>Value Type1</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VALUE_TYPE1_FEATURE_COUNT = 5;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.VTypeImpl <em>VType</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.VTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVType()
     */
    int VTYPE = 80;

    /**
     * The feature id for the '<em><b>Value</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VTYPE__VALUE = 0;

    /**
     * The feature id for the '<em><b>Aid</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VTYPE__AID = 1;

    /**
     * The feature id for the '<em><b>Null</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VTYPE__NULL = 2;

    /**
     * The number of structural features of the '<em>VType</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int VTYPE_FEATURE_COUNT = 3;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.impl.WSDLTypeImpl <em>WSDL Type</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.impl.WSDLTypeImpl
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getWSDLType()
     */
    int WSDL_TYPE = 81;

    /**
     * The feature id for the '<em><b>Href</b></em>' attribute.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int WSDL_TYPE__HREF = 0;

    /**
     * The number of structural features of the '<em>WSDL Type</em>' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @ordered
     */
    int WSDL_TYPE_FEATURE_COUNT = 1;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.DataClassType <em>Data Class Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DataClassType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDataClassType()
     */
    int DATA_CLASS_TYPE = 82;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.DescribeDatasetsValueType <em>Describe Datasets Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDatasetsValueType()
     */
    int DESCRIBE_DATASETS_VALUE_TYPE = 83;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.DescribeDataValueType <em>Describe Data Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeDataValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDataValueType()
     */
    int DESCRIBE_DATA_VALUE_TYPE = 84;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.DescribeFrameworksValueType <em>Describe Frameworks Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeFrameworksValueType()
     */
    int DESCRIBE_FRAMEWORKS_VALUE_TYPE = 85;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.DescribeJoinAbilitiesValueType <em>Describe Join Abilities Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeJoinAbilitiesValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeJoinAbilitiesValueType()
     */
    int DESCRIBE_JOIN_ABILITIES_VALUE_TYPE = 86;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.DescribeKeyValueType <em>Describe Key Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeKeyValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeKeyValueType()
     */
    int DESCRIBE_KEY_VALUE_TYPE = 87;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.GaussianType <em>Gaussian Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.GaussianType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGaussianType()
     */
    int GAUSSIAN_TYPE = 88;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.GetCapabilitiesValueType <em>Get Capabilities Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetCapabilitiesValueType()
     */
    int GET_CAPABILITIES_VALUE_TYPE = 89;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.GetDataValueType <em>Get Data Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.GetDataValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetDataValueType()
     */
    int GET_DATA_VALUE_TYPE = 90;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.JoinDataValueType <em>Join Data Value Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.JoinDataValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getJoinDataValueType()
     */
    int JOIN_DATA_VALUE_TYPE = 91;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.PurposeType <em>Purpose Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.PurposeType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getPurposeType()
     */
    int PURPOSE_TYPE = 92;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.RequestServiceType <em>Request Service Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.RequestServiceType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRequestServiceType()
     */
    int REQUEST_SERVICE_TYPE = 93;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.TypeType <em>Type Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.TypeType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getTypeType()
     */
    int TYPE_TYPE = 94;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.UpdateType <em>Update Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.UpdateType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getUpdateType()
     */
    int UPDATE_TYPE = 95;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.VersionType <em>Version Type</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.VersionType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVersionType()
     */
    int VERSION_TYPE = 96;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.VersionType1 <em>Version Type1</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.VersionType1
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVersionType1()
     */
    int VERSION_TYPE1 = 97;

    /**
     * The meta object id for the '{@link net.opengis.tjs10.VersionType2 <em>Version Type2</em>}' enum.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.VersionType2
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVersionType2()
     */
    int VERSION_TYPE2 = 98;

    /**
     * The meta object id for the '<em>Accept Languages Type</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see java.lang.String
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getAcceptLanguagesType()
     */
    int ACCEPT_LANGUAGES_TYPE = 99;

    /**
     * The meta object id for the '<em>Data Class Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DataClassType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDataClassTypeObject()
     */
    int DATA_CLASS_TYPE_OBJECT = 100;

    /**
     * The meta object id for the '<em>Describe Datasets Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDatasetsValueTypeObject()
     */
    int DESCRIBE_DATASETS_VALUE_TYPE_OBJECT = 101;

    /**
     * The meta object id for the '<em>Describe Data Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeDataValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeDataValueTypeObject()
     */
    int DESCRIBE_DATA_VALUE_TYPE_OBJECT = 102;

    /**
     * The meta object id for the '<em>Describe Frameworks Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeFrameworksValueTypeObject()
     */
    int DESCRIBE_FRAMEWORKS_VALUE_TYPE_OBJECT = 103;

    /**
     * The meta object id for the '<em>Describe Join Abilities Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeJoinAbilitiesValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeJoinAbilitiesValueTypeObject()
     */
    int DESCRIBE_JOIN_ABILITIES_VALUE_TYPE_OBJECT = 104;

    /**
     * The meta object id for the '<em>Describe Key Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.DescribeKeyValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getDescribeKeyValueTypeObject()
     */
    int DESCRIBE_KEY_VALUE_TYPE_OBJECT = 105;

    /**
     * The meta object id for the '<em>Gaussian Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.GaussianType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGaussianTypeObject()
     */
    int GAUSSIAN_TYPE_OBJECT = 106;

    /**
     * The meta object id for the '<em>Get Capabilities Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetCapabilitiesValueTypeObject()
     */
    int GET_CAPABILITIES_VALUE_TYPE_OBJECT = 107;

    /**
     * The meta object id for the '<em>Get Data Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.GetDataValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getGetDataValueTypeObject()
     */
    int GET_DATA_VALUE_TYPE_OBJECT = 108;

    /**
     * The meta object id for the '<em>Join Data Value Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.JoinDataValueType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getJoinDataValueTypeObject()
     */
    int JOIN_DATA_VALUE_TYPE_OBJECT = 109;

    /**
     * The meta object id for the '<em>Purpose Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.PurposeType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getPurposeTypeObject()
     */
    int PURPOSE_TYPE_OBJECT = 110;

    /**
     * The meta object id for the '<em>Request Service Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.RequestServiceType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getRequestServiceTypeObject()
     */
    int REQUEST_SERVICE_TYPE_OBJECT = 111;

    /**
     * The meta object id for the '<em>Sections Type</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see java.lang.String
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getSectionsType()
     */
    int SECTIONS_TYPE = 112;

    /**
     * The meta object id for the '<em>Type Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.TypeType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getTypeTypeObject()
     */
    int TYPE_TYPE_OBJECT = 113;

    /**
     * The meta object id for the '<em>Update Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.UpdateType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getUpdateTypeObject()
     */
    int UPDATE_TYPE_OBJECT = 114;

    /**
     * The meta object id for the '<em>Version Type Object</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.VersionType
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVersionTypeObject()
     */
    int VERSION_TYPE_OBJECT = 115;

    /**
     * The meta object id for the '<em>Version Type Object1</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.VersionType1
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVersionTypeObject1()
     */
    int VERSION_TYPE_OBJECT1 = 116;

    /**
     * The meta object id for the '<em>Version Type Object2</em>' data type.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see net.opengis.tjs10.VersionType2
     * @see net.opengis.tjs10.impl.Tjs10PackageImpl#getVersionTypeObject2()
     */
    int VERSION_TYPE_OBJECT2 = 117;


    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.AbstractType <em>Abstract Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Abstract Type</em>'.
     * @generated
     * @see net.opengis.tjs10.AbstractType
     */
    EClass getAbstractType();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.AcceptVersionsType <em>Accept Versions Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Accept Versions Type</em>'.
     * @generated
     * @see net.opengis.tjs10.AcceptVersionsType
     */
    EClass getAcceptVersionsType();

    /**
     * Returns the meta object for the attribute list '{@link net.opengis.tjs10.AcceptVersionsType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute list '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.AcceptVersionsType#getVersion()
     * @see #getAcceptVersionsType()
     */
    EAttribute getAcceptVersionsType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.AttributeDataType <em>Attribute Data Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Attribute Data Type</em>'.
     * @generated
     * @see net.opengis.tjs10.AttributeDataType
     */
    EClass getAttributeDataType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.AttributeDataType#getGetDataURL <em>Get Data URL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Get Data URL</em>'.
     * @generated
     * @see net.opengis.tjs10.AttributeDataType#getGetDataURL()
     * @see #getAttributeDataType()
     */
    EAttribute getAttributeDataType_GetDataURL();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.AttributeDataType#getGetDataXML <em>Get Data XML</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Get Data XML</em>'.
     * @generated
     * @see net.opengis.tjs10.AttributeDataType#getGetDataXML()
     * @see #getAttributeDataType()
     */
    EReference getAttributeDataType_GetDataXML();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.AttributesType <em>Attributes Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Attributes Type</em>'.
     * @generated
     * @see net.opengis.tjs10.AttributesType
     */
    EClass getAttributesType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.AttributesType#getColumn <em>Column</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Column</em>'.
     * @generated
     * @see net.opengis.tjs10.AttributesType#getColumn()
     * @see #getAttributesType()
     */
    EReference getAttributesType_Column();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.BoundingCoordinatesType <em>Bounding Coordinates Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Bounding Coordinates Type</em>'.
     * @generated
     * @see net.opengis.tjs10.BoundingCoordinatesType
     */
    EClass getBoundingCoordinatesType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.BoundingCoordinatesType#getNorth <em>North</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>North</em>'.
     * @generated
     * @see net.opengis.tjs10.BoundingCoordinatesType#getNorth()
     * @see #getBoundingCoordinatesType()
     */
    EAttribute getBoundingCoordinatesType_North();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.BoundingCoordinatesType#getSouth <em>South</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>South</em>'.
     * @generated
     * @see net.opengis.tjs10.BoundingCoordinatesType#getSouth()
     * @see #getBoundingCoordinatesType()
     */
    EAttribute getBoundingCoordinatesType_South();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.BoundingCoordinatesType#getEast <em>East</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>East</em>'.
     * @generated
     * @see net.opengis.tjs10.BoundingCoordinatesType#getEast()
     * @see #getBoundingCoordinatesType()
     */
    EAttribute getBoundingCoordinatesType_East();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.BoundingCoordinatesType#getWest <em>West</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>West</em>'.
     * @generated
     * @see net.opengis.tjs10.BoundingCoordinatesType#getWest()
     * @see #getBoundingCoordinatesType()
     */
    EAttribute getBoundingCoordinatesType_West();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ClassesType <em>Classes Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Classes Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType
     */
    EClass getClassesType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ClassesType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType#getTitle()
     * @see #getClassesType()
     */
    EAttribute getClassesType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ClassesType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType#getAbstract()
     * @see #getClassesType()
     */
    EReference getClassesType_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ClassesType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType#getDocumentation()
     * @see #getClassesType()
     */
    EAttribute getClassesType_Documentation();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.ClassesType#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType#getValue()
     * @see #getClassesType()
     */
    EReference getClassesType_Value();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ClassesType1 <em>Classes Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Classes Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType1
     */
    EClass getClassesType1();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ClassesType1#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType1#getTitle()
     * @see #getClassesType1()
     */
    EAttribute getClassesType1_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ClassesType1#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType1#getAbstract()
     * @see #getClassesType1()
     */
    EReference getClassesType1_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ClassesType1#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType1#getDocumentation()
     * @see #getClassesType1()
     */
    EAttribute getClassesType1_Documentation();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.ClassesType1#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.ClassesType1#getValue()
     * @see #getClassesType1()
     */
    EReference getClassesType1_Value();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ColumnsetType <em>Columnset Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Columnset Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnsetType
     */
    EClass getColumnsetType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ColumnsetType#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnsetType#getFrameworkKey()
     * @see #getColumnsetType()
     */
    EReference getColumnsetType_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ColumnsetType#getAttributes <em>Attributes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Attributes</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnsetType#getAttributes()
     * @see #getColumnsetType()
     */
    EReference getColumnsetType_Attributes();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ColumnType <em>Column Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Column Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType
     */
    EClass getColumnType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType#getDecimals <em>Decimals</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Decimals</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType#getDecimals()
     * @see #getColumnType()
     */
    EAttribute getColumnType_Decimals();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType#getLength <em>Length</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Length</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType#getLength()
     * @see #getColumnType()
     */
    EAttribute getColumnType_Length();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType#getName <em>Name</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Name</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType#getName()
     * @see #getColumnType()
     */
    EAttribute getColumnType_Name();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType#getType <em>Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType#getType()
     * @see #getColumnType()
     */
    EAttribute getColumnType_Type();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ColumnType1 <em>Column Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Column Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1
     */
    EClass getColumnType1();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getTitle()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ColumnType1#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getAbstract()
     * @see #getColumnType1()
     */
    EReference getColumnType1_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getDocumentation()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ColumnType1#getValues <em>Values</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Values</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getValues()
     * @see #getColumnType1()
     */
    EReference getColumnType1_Values();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ColumnType1#getGetDataRequest <em>Get Data Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Get Data Request</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getGetDataRequest()
     * @see #getColumnType1()
     */
    EReference getColumnType1_GetDataRequest();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getDecimals <em>Decimals</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Decimals</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getDecimals()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Decimals();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getLength <em>Length</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Length</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getLength()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Length();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getName <em>Name</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Name</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getName()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Name();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getPurpose <em>Purpose</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Purpose</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getPurpose()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Purpose();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType1#getType <em>Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType1#getType()
     * @see #getColumnType1()
     */
    EAttribute getColumnType1_Type();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ColumnType2 <em>Column Type2</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Column Type2</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType2
     */
    EClass getColumnType2();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType2#getDecimals <em>Decimals</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Decimals</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType2#getDecimals()
     * @see #getColumnType2()
     */
    EAttribute getColumnType2_Decimals();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType2#getLength <em>Length</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Length</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType2#getLength()
     * @see #getColumnType2()
     */
    EAttribute getColumnType2_Length();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType2#getName <em>Name</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Name</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType2#getName()
     * @see #getColumnType2()
     */
    EAttribute getColumnType2_Name();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ColumnType2#getType <em>Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ColumnType2#getType()
     * @see #getColumnType2()
     */
    EAttribute getColumnType2_Type();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.CountType <em>Count Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Count Type</em>'.
     * @generated
     * @see net.opengis.tjs10.CountType
     */
    EClass getCountType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.CountType#getUOM <em>UOM</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>UOM</em>'.
     * @generated
     * @see net.opengis.tjs10.CountType#getUOM()
     * @see #getCountType()
     */
    EReference getCountType_UOM();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.CountType#getUncertainty <em>Uncertainty</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Uncertainty</em>'.
     * @generated
     * @see net.opengis.tjs10.CountType#getUncertainty()
     * @see #getCountType()
     */
    EReference getCountType_Uncertainty();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.CountType#getExceptions <em>Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Exceptions</em>'.
     * @generated
     * @see net.opengis.tjs10.CountType#getExceptions()
     * @see #getCountType()
     */
    EReference getCountType_Exceptions();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DataDescriptionsType <em>Data Descriptions Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Data Descriptions Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType
     */
    EClass getDataDescriptionsType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.DataDescriptionsType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType#getFramework()
     * @see #getDataDescriptionsType()
     */
    EReference getDataDescriptionsType_Framework();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DataDescriptionsType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType#getCapabilities()
     * @see #getDataDescriptionsType()
     */
    EAttribute getDataDescriptionsType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DataDescriptionsType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType#getLang()
     * @see #getDataDescriptionsType()
     */
    EAttribute getDataDescriptionsType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DataDescriptionsType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType#getService()
     * @see #getDataDescriptionsType()
     */
    EAttribute getDataDescriptionsType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DataDescriptionsType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType#getVersion()
     * @see #getDataDescriptionsType()
     */
    EAttribute getDataDescriptionsType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DataInputsType <em>Data Inputs Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Data Inputs Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DataInputsType
     */
    EClass getDataInputsType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DataInputsType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.DataInputsType#getFramework()
     * @see #getDataInputsType()
     */
    EReference getDataInputsType_Framework();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DatasetDescriptionsType <em>Dataset Descriptions Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Dataset Descriptions Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType
     */
    EClass getDatasetDescriptionsType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.DatasetDescriptionsType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType#getFramework()
     * @see #getDatasetDescriptionsType()
     */
    EReference getDatasetDescriptionsType_Framework();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetDescriptionsType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType#getCapabilities()
     * @see #getDatasetDescriptionsType()
     */
    EAttribute getDatasetDescriptionsType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetDescriptionsType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType#getLang()
     * @see #getDatasetDescriptionsType()
     */
    EAttribute getDatasetDescriptionsType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetDescriptionsType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType#getService()
     * @see #getDatasetDescriptionsType()
     */
    EAttribute getDatasetDescriptionsType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetDescriptionsType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType#getVersion()
     * @see #getDatasetDescriptionsType()
     */
    EAttribute getDatasetDescriptionsType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DatasetType <em>Dataset Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Dataset Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType
     */
    EClass getDatasetType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getDatasetURI()
     * @see #getDatasetType()
     */
    EAttribute getDatasetType_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getOrganization()
     * @see #getDatasetType()
     */
    EAttribute getDatasetType_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getTitle()
     * @see #getDatasetType()
     */
    EAttribute getDatasetType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getAbstract()
     * @see #getDatasetType()
     */
    EReference getDatasetType_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getReferenceDate()
     * @see #getDatasetType()
     */
    EReference getDatasetType_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getVersion()
     * @see #getDatasetType()
     */
    EAttribute getDatasetType_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getDocumentation()
     * @see #getDatasetType()
     */
    EAttribute getDatasetType_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType#getDescribeDataRequest <em>Describe Data Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Data Request</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType#getDescribeDataRequest()
     * @see #getDatasetType()
     */
    EReference getDatasetType_DescribeDataRequest();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DatasetType1 <em>Dataset Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Dataset Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1
     */
    EClass getDatasetType1();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType1#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getDatasetURI()
     * @see #getDatasetType1()
     */
    EAttribute getDatasetType1_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType1#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getOrganization()
     * @see #getDatasetType1()
     */
    EAttribute getDatasetType1_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType1#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getTitle()
     * @see #getDatasetType1()
     */
    EAttribute getDatasetType1_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType1#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getAbstract()
     * @see #getDatasetType1()
     */
    EReference getDatasetType1_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType1#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getReferenceDate()
     * @see #getDatasetType1()
     */
    EReference getDatasetType1_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType1#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getVersion()
     * @see #getDatasetType1()
     */
    EAttribute getDatasetType1_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType1#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getDocumentation()
     * @see #getDatasetType1()
     */
    EAttribute getDatasetType1_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType1#getColumnset <em>Columnset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Columnset</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getColumnset()
     * @see #getDatasetType1()
     */
    EReference getDatasetType1_Columnset();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType1#getRowset <em>Rowset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Rowset</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType1#getRowset()
     * @see #getDatasetType1()
     */
    EReference getDatasetType1_Rowset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DatasetType2 <em>Dataset Type2</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Dataset Type2</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2
     */
    EClass getDatasetType2();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType2#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getDatasetURI()
     * @see #getDatasetType2()
     */
    EAttribute getDatasetType2_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType2#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getOrganization()
     * @see #getDatasetType2()
     */
    EAttribute getDatasetType2_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType2#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getTitle()
     * @see #getDatasetType2()
     */
    EAttribute getDatasetType2_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType2#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getAbstract()
     * @see #getDatasetType2()
     */
    EReference getDatasetType2_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType2#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getReferenceDate()
     * @see #getDatasetType2()
     */
    EReference getDatasetType2_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType2#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getVersion()
     * @see #getDatasetType2()
     */
    EAttribute getDatasetType2_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType2#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getDocumentation()
     * @see #getDatasetType2()
     */
    EAttribute getDatasetType2_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType2#getDescribeDataRequest <em>Describe Data Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Data Request</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getDescribeDataRequest()
     * @see #getDatasetType2()
     */
    EReference getDatasetType2_DescribeDataRequest();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType2#getColumnset <em>Columnset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Columnset</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getColumnset()
     * @see #getDatasetType2()
     */
    EReference getDatasetType2_Columnset();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType2#getRowset <em>Rowset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Rowset</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType2#getRowset()
     * @see #getDatasetType2()
     */
    EReference getDatasetType2_Rowset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DatasetType3 <em>Dataset Type3</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Dataset Type3</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3
     */
    EClass getDatasetType3();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType3#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getDatasetURI()
     * @see #getDatasetType3()
     */
    EAttribute getDatasetType3_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType3#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getOrganization()
     * @see #getDatasetType3()
     */
    EAttribute getDatasetType3_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType3#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getTitle()
     * @see #getDatasetType3()
     */
    EAttribute getDatasetType3_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType3#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getAbstract()
     * @see #getDatasetType3()
     */
    EReference getDatasetType3_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType3#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getReferenceDate()
     * @see #getDatasetType3()
     */
    EReference getDatasetType3_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType3#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getVersion()
     * @see #getDatasetType3()
     */
    EAttribute getDatasetType3_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DatasetType3#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getDocumentation()
     * @see #getDatasetType3()
     */
    EAttribute getDatasetType3_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType3#getDescribeDataRequest <em>Describe Data Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Data Request</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getDescribeDataRequest()
     * @see #getDatasetType3()
     */
    EReference getDatasetType3_DescribeDataRequest();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DatasetType3#getColumnset <em>Columnset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Columnset</em>'.
     * @generated
     * @see net.opengis.tjs10.DatasetType3#getColumnset()
     * @see #getDatasetType3()
     */
    EReference getDatasetType3_Columnset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeDataRequestType <em>Describe Data Request Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Data Request Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataRequestType
     */
    EClass getDescribeDataRequestType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDataRequestType#getHref <em>Href</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Href</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataRequestType#getHref()
     * @see #getDescribeDataRequestType()
     */
    EAttribute getDescribeDataRequestType_Href();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeDatasetsRequestType <em>Describe Datasets Request Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Datasets Request Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsRequestType
     */
    EClass getDescribeDatasetsRequestType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDatasetsRequestType#getHref <em>Href</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Href</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsRequestType#getHref()
     * @see #getDescribeDatasetsRequestType()
     */
    EAttribute getDescribeDatasetsRequestType_Href();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeDatasetsType <em>Describe Datasets Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Datasets Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsType
     */
    EClass getDescribeDatasetsType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDatasetsType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsType#getFrameworkURI()
     * @see #getDescribeDatasetsType()
     */
    EAttribute getDescribeDatasetsType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDatasetsType#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsType#getDatasetURI()
     * @see #getDescribeDatasetsType()
     */
    EAttribute getDescribeDatasetsType_DatasetURI();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeDataType <em>Describe Data Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Data Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataType
     */
    EClass getDescribeDataType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDataType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataType#getFrameworkURI()
     * @see #getDescribeDataType()
     */
    EAttribute getDescribeDataType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDataType#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataType#getDatasetURI()
     * @see #getDescribeDataType()
     */
    EAttribute getDescribeDataType_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeDataType#getAttributes <em>Attributes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Attributes</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataType#getAttributes()
     * @see #getDescribeDataType()
     */
    EAttribute getDescribeDataType_Attributes();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeFrameworkKeyType <em>Describe Framework Key Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Framework Key Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType
     */
    EClass getDescribeFrameworkKeyType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getFrameworkURI()
     * @see #getDescribeFrameworkKeyType()
     */
    EAttribute getDescribeFrameworkKeyType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getOrganization()
     * @see #getDescribeFrameworkKeyType()
     */
    EAttribute getDescribeFrameworkKeyType_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getTitle()
     * @see #getDescribeFrameworkKeyType()
     */
    EAttribute getDescribeFrameworkKeyType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getAbstract()
     * @see #getDescribeFrameworkKeyType()
     */
    EReference getDescribeFrameworkKeyType_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getReferenceDate()
     * @see #getDescribeFrameworkKeyType()
     */
    EReference getDescribeFrameworkKeyType_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getVersion()
     * @see #getDescribeFrameworkKeyType()
     */
    EAttribute getDescribeFrameworkKeyType_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getDocumentation()
     * @see #getDescribeFrameworkKeyType()
     */
    EAttribute getDescribeFrameworkKeyType_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getFrameworkKey()
     * @see #getDescribeFrameworkKeyType()
     */
    EReference getDescribeFrameworkKeyType_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getBoundingCoordinates()
     * @see #getDescribeFrameworkKeyType()
     */
    EReference getDescribeFrameworkKeyType_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DescribeFrameworkKeyType#getRowset <em>Rowset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Rowset</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType#getRowset()
     * @see #getDescribeFrameworkKeyType()
     */
    EReference getDescribeFrameworkKeyType_Rowset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeFrameworksType <em>Describe Frameworks Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Frameworks Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksType
     */
    EClass getDescribeFrameworksType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeFrameworksType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksType#getFrameworkURI()
     * @see #getDescribeFrameworksType()
     */
    EAttribute getDescribeFrameworksType_FrameworkURI();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DescribeKeyType <em>Describe Key Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Describe Key Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeKeyType
     */
    EClass getDescribeKeyType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DescribeKeyType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeKeyType#getFrameworkURI()
     * @see #getDescribeKeyType()
     */
    EAttribute getDescribeKeyType_FrameworkURI();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.DocumentRoot <em>Document Root</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Document Root</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot
     */
    EClass getDocumentRoot();

    /**
     * Returns the meta object for the attribute list '{@link net.opengis.tjs10.DocumentRoot#getMixed <em>Mixed</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute list '<em>Mixed</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getMixed()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Mixed();

    /**
     * Returns the meta object for the map '{@link net.opengis.tjs10.DocumentRoot#getXMLNSPrefixMap <em>XMLNS Prefix Map</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the map '<em>XMLNS Prefix Map</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getXMLNSPrefixMap()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_XMLNSPrefixMap();

    /**
     * Returns the meta object for the map '{@link net.opengis.tjs10.DocumentRoot#getXSISchemaLocation <em>XSI Schema Location</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the map '<em>XSI Schema Location</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getXSISchemaLocation()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_XSISchemaLocation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getAbstract()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getAttributeLimit <em>Attribute Limit</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Attribute Limit</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getAttributeLimit()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_AttributeLimit();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getAttributes <em>Attributes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Attributes</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getAttributes()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Attributes();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getBoundingCoordinates()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getCapabilities()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Capabilities();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getColumnset <em>Columnset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Columnset</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getColumnset()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Columnset();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getCount <em>Count</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Count</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getCount()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Count();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getDataClass <em>Data Class</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Data Class</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDataClass()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_DataClass();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDataDescriptions <em>Data Descriptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Data Descriptions</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDataDescriptions()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DataDescriptions();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDataset <em>Dataset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Dataset</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDataset()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Dataset();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDatasetDescriptions <em>Dataset Descriptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Dataset Descriptions</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDatasetDescriptions()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DatasetDescriptions();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDatasetURI()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_DatasetURI();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeData <em>Describe Data</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Data</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeData()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeData();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeDataRequest <em>Describe Data Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Data Request</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeDataRequest()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeDataRequest();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeDatasets <em>Describe Datasets</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Datasets</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeDatasets()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeDatasets();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Datasets Request</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeDatasetsRequest()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeDatasetsRequest();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeFrameworks <em>Describe Frameworks</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Frameworks</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeFrameworks()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeFrameworks();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeJoinAbilities <em>Describe Join Abilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Join Abilities</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeJoinAbilities()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeJoinAbilities();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getDescribeKey <em>Describe Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Key</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDescribeKey()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_DescribeKey();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getDocumentation()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getFramework()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Framework();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getFrameworkDescriptions <em>Framework Descriptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Descriptions</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getFrameworkDescriptions()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_FrameworkDescriptions();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getFrameworkKey()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getFrameworkKeyDescription <em>Framework Key Description</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key Description</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getFrameworkKeyDescription()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_FrameworkKeyDescription();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getFrameworkURI()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_FrameworkURI();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getGDAS <em>GDAS</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>GDAS</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getGDAS()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_GDAS();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getGetCapabilities <em>Get Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Get Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getGetCapabilities()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_GetCapabilities();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getGetData <em>Get Data</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Get Data</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getGetData()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_GetData();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getGetDataRequest <em>Get Data Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Get Data Request</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getGetDataRequest()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_GetDataRequest();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getIdentifier()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Identifier();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getJoinAbilities <em>Join Abilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Join Abilities</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getJoinAbilities()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_JoinAbilities();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getJoinData <em>Join Data</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Join Data</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getJoinData()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_JoinData();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getJoinDataResponse <em>Join Data Response</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Join Data Response</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getJoinDataResponse()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_JoinDataResponse();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getK <em>K</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>K</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getK()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_K();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getLinkageKeys <em>Linkage Keys</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Linkage Keys</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getLinkageKeys()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_LinkageKeys();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getMeasure <em>Measure</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Measure</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getMeasure()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Measure();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getMechanism <em>Mechanism</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Mechanism</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getMechanism()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Mechanism();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getNominal <em>Nominal</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Nominal</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getNominal()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Nominal();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getOrdinal <em>Ordinal</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Ordinal</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getOrdinal()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Ordinal();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getOrganization()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Organization();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getReferenceDate()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_ReferenceDate();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getRowset <em>Rowset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Rowset</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getRowset()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Rowset();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getSpatialFrameworks <em>Spatial Frameworks</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Spatial Frameworks</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getSpatialFrameworks()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_SpatialFrameworks();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getStyling <em>Styling</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Styling</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getStyling()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Styling();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getTitle()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getUncertainty <em>Uncertainty</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Uncertainty</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getUncertainty()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Uncertainty();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getUOM <em>UOM</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>UOM</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getUOM()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_UOM();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.DocumentRoot#getValues <em>Values</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Values</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getValues()
     * @see #getDocumentRoot()
     */
    EReference getDocumentRoot_Values();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.DocumentRoot#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot#getVersion()
     * @see #getDocumentRoot()
     */
    EAttribute getDocumentRoot_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ExceptionReportType <em>Exception Report Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Exception Report Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ExceptionReportType
     */
    EClass getExceptionReportType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ExceptionReportType#getException <em>Exception</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Exception</em>'.
     * @generated
     * @see net.opengis.tjs10.ExceptionReportType#getException()
     * @see #getExceptionReportType()
     */
    EReference getExceptionReportType_Exception();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FailedType <em>Failed Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Failed Type</em>'.
     * @generated
     * @see net.opengis.tjs10.FailedType
     */
    EClass getFailedType();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType <em>Framework Dataset Describe Data Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Dataset Describe Data Type</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType
     */
    EClass getFrameworkDatasetDescribeDataType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getFrameworkURI()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EAttribute getFrameworkDatasetDescribeDataType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getOrganization()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EAttribute getFrameworkDatasetDescribeDataType_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getTitle()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EAttribute getFrameworkDatasetDescribeDataType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getAbstract()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EReference getFrameworkDatasetDescribeDataType_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getReferenceDate()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EReference getFrameworkDatasetDescribeDataType_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getVersion()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EAttribute getFrameworkDatasetDescribeDataType_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getDocumentation()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EAttribute getFrameworkDatasetDescribeDataType_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getFrameworkKey()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EReference getFrameworkDatasetDescribeDataType_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getBoundingCoordinates()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EReference getFrameworkDatasetDescribeDataType_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Datasets Request</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getDescribeDatasetsRequest()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EReference getFrameworkDatasetDescribeDataType_DescribeDatasetsRequest();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType#getDataset <em>Dataset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Dataset</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType#getDataset()
     * @see #getFrameworkDatasetDescribeDataType()
     */
    EReference getFrameworkDatasetDescribeDataType_Dataset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkDescriptionsType <em>Framework Descriptions Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Descriptions Type</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType
     */
    EClass getFrameworkDescriptionsType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.FrameworkDescriptionsType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType#getFramework()
     * @see #getFrameworkDescriptionsType()
     */
    EReference getFrameworkDescriptionsType_Framework();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDescriptionsType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType#getCapabilities()
     * @see #getFrameworkDescriptionsType()
     */
    EAttribute getFrameworkDescriptionsType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDescriptionsType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType#getLang()
     * @see #getFrameworkDescriptionsType()
     */
    EAttribute getFrameworkDescriptionsType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDescriptionsType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType#getService()
     * @see #getFrameworkDescriptionsType()
     */
    EAttribute getFrameworkDescriptionsType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkDescriptionsType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType#getVersion()
     * @see #getFrameworkDescriptionsType()
     */
    EAttribute getFrameworkDescriptionsType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkKeyDescriptionType <em>Framework Key Description Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Key Description Type</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType
     */
    EClass getFrameworkKeyDescriptionType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkKeyDescriptionType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType#getFramework()
     * @see #getFrameworkKeyDescriptionType()
     */
    EReference getFrameworkKeyDescriptionType_Framework();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkKeyDescriptionType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType#getCapabilities()
     * @see #getFrameworkKeyDescriptionType()
     */
    EAttribute getFrameworkKeyDescriptionType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkKeyDescriptionType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType#getLang()
     * @see #getFrameworkKeyDescriptionType()
     */
    EAttribute getFrameworkKeyDescriptionType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkKeyDescriptionType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType#getService()
     * @see #getFrameworkKeyDescriptionType()
     */
    EAttribute getFrameworkKeyDescriptionType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkKeyDescriptionType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType#getVersion()
     * @see #getFrameworkKeyDescriptionType()
     */
    EAttribute getFrameworkKeyDescriptionType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkKeyType <em>Framework Key Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Key Type</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType
     */
    EClass getFrameworkKeyType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.FrameworkKeyType#getColumn <em>Column</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Column</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType#getColumn()
     * @see #getFrameworkKeyType()
     */
    EReference getFrameworkKeyType_Column();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkKeyType1 <em>Framework Key Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Key Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType1
     */
    EClass getFrameworkKeyType1();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.FrameworkKeyType1#getColumn <em>Column</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Column</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType1#getColumn()
     * @see #getFrameworkKeyType1()
     */
    EReference getFrameworkKeyType1_Column();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkKeyType1#getComplete <em>Complete</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Complete</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType1#getComplete()
     * @see #getFrameworkKeyType1()
     */
    EAttribute getFrameworkKeyType1_Complete();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkKeyType1#getRelationship <em>Relationship</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Relationship</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType1#getRelationship()
     * @see #getFrameworkKeyType1()
     */
    EAttribute getFrameworkKeyType1_Relationship();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkType <em>Framework Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Type</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType
     */
    EClass getFrameworkType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getFrameworkURI()
     * @see #getFrameworkType()
     */
    EAttribute getFrameworkType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getOrganization()
     * @see #getFrameworkType()
     */
    EAttribute getFrameworkType_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getTitle()
     * @see #getFrameworkType()
     */
    EAttribute getFrameworkType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getAbstract()
     * @see #getFrameworkType()
     */
    EReference getFrameworkType_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getReferenceDate()
     * @see #getFrameworkType()
     */
    EReference getFrameworkType_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getVersion()
     * @see #getFrameworkType()
     */
    EAttribute getFrameworkType_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getDocumentation()
     * @see #getFrameworkType()
     */
    EAttribute getFrameworkType_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getFrameworkKey()
     * @see #getFrameworkType()
     */
    EReference getFrameworkType_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType#getBoundingCoordinates()
     * @see #getFrameworkType()
     */
    EReference getFrameworkType_BoundingCoordinates();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkType1 <em>Framework Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1
     */
    EClass getFrameworkType1();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType1#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getFrameworkURI()
     * @see #getFrameworkType1()
     */
    EAttribute getFrameworkType1_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType1#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getOrganization()
     * @see #getFrameworkType1()
     */
    EAttribute getFrameworkType1_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType1#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getTitle()
     * @see #getFrameworkType1()
     */
    EAttribute getFrameworkType1_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType1#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getAbstract()
     * @see #getFrameworkType1()
     */
    EReference getFrameworkType1_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType1#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getReferenceDate()
     * @see #getFrameworkType1()
     */
    EReference getFrameworkType1_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType1#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getVersion()
     * @see #getFrameworkType1()
     */
    EAttribute getFrameworkType1_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType1#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getDocumentation()
     * @see #getFrameworkType1()
     */
    EAttribute getFrameworkType1_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType1#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getFrameworkKey()
     * @see #getFrameworkType1()
     */
    EReference getFrameworkType1_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType1#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getBoundingCoordinates()
     * @see #getFrameworkType1()
     */
    EReference getFrameworkType1_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType1#getDataset <em>Dataset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Dataset</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1#getDataset()
     * @see #getFrameworkType1()
     */
    EReference getFrameworkType1_Dataset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkType2 <em>Framework Type2</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Type2</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2
     */
    EClass getFrameworkType2();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType2#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getFrameworkURI()
     * @see #getFrameworkType2()
     */
    EAttribute getFrameworkType2_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType2#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getOrganization()
     * @see #getFrameworkType2()
     */
    EAttribute getFrameworkType2_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType2#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getTitle()
     * @see #getFrameworkType2()
     */
    EAttribute getFrameworkType2_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType2#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getAbstract()
     * @see #getFrameworkType2()
     */
    EReference getFrameworkType2_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType2#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getReferenceDate()
     * @see #getFrameworkType2()
     */
    EReference getFrameworkType2_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType2#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getVersion()
     * @see #getFrameworkType2()
     */
    EAttribute getFrameworkType2_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType2#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getDocumentation()
     * @see #getFrameworkType2()
     */
    EAttribute getFrameworkType2_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType2#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getFrameworkKey()
     * @see #getFrameworkType2()
     */
    EReference getFrameworkType2_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType2#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getBoundingCoordinates()
     * @see #getFrameworkType2()
     */
    EReference getFrameworkType2_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType2#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Datasets Request</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2#getDescribeDatasetsRequest()
     * @see #getFrameworkType2()
     */
    EReference getFrameworkType2_DescribeDatasetsRequest();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkType3 <em>Framework Type3</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Type3</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3
     */
    EClass getFrameworkType3();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType3#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getFrameworkURI()
     * @see #getFrameworkType3()
     */
    EAttribute getFrameworkType3_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType3#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getOrganization()
     * @see #getFrameworkType3()
     */
    EAttribute getFrameworkType3_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType3#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getTitle()
     * @see #getFrameworkType3()
     */
    EAttribute getFrameworkType3_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType3#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getAbstract()
     * @see #getFrameworkType3()
     */
    EReference getFrameworkType3_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType3#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getReferenceDate()
     * @see #getFrameworkType3()
     */
    EReference getFrameworkType3_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType3#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getVersion()
     * @see #getFrameworkType3()
     */
    EAttribute getFrameworkType3_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType3#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getDocumentation()
     * @see #getFrameworkType3()
     */
    EAttribute getFrameworkType3_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType3#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getFrameworkKey()
     * @see #getFrameworkType3()
     */
    EReference getFrameworkType3_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType3#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getBoundingCoordinates()
     * @see #getFrameworkType3()
     */
    EReference getFrameworkType3_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType3#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Datasets Request</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getDescribeDatasetsRequest()
     * @see #getFrameworkType3()
     */
    EReference getFrameworkType3_DescribeDatasetsRequest();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType3#getDataset <em>Dataset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Dataset</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3#getDataset()
     * @see #getFrameworkType3()
     */
    EReference getFrameworkType3_Dataset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.FrameworkType4 <em>Framework Type4</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Framework Type4</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4
     */
    EClass getFrameworkType4();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType4#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getFrameworkURI()
     * @see #getFrameworkType4()
     */
    EAttribute getFrameworkType4_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType4#getOrganization <em>Organization</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Organization</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getOrganization()
     * @see #getFrameworkType4()
     */
    EAttribute getFrameworkType4_Organization();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType4#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getTitle()
     * @see #getFrameworkType4()
     */
    EAttribute getFrameworkType4_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType4#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getAbstract()
     * @see #getFrameworkType4()
     */
    EReference getFrameworkType4_Abstract();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType4#getReferenceDate <em>Reference Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Reference Date</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getReferenceDate()
     * @see #getFrameworkType4()
     */
    EReference getFrameworkType4_ReferenceDate();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType4#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getVersion()
     * @see #getFrameworkType4()
     */
    EAttribute getFrameworkType4_Version();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.FrameworkType4#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getDocumentation()
     * @see #getFrameworkType4()
     */
    EAttribute getFrameworkType4_Documentation();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType4#getFrameworkKey <em>Framework Key</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework Key</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getFrameworkKey()
     * @see #getFrameworkType4()
     */
    EReference getFrameworkType4_FrameworkKey();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType4#getBoundingCoordinates <em>Bounding Coordinates</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Bounding Coordinates</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getBoundingCoordinates()
     * @see #getFrameworkType4()
     */
    EReference getFrameworkType4_BoundingCoordinates();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.FrameworkType4#getDescribeDatasetsRequest <em>Describe Datasets Request</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Describe Datasets Request</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getDescribeDatasetsRequest()
     * @see #getFrameworkType4()
     */
    EReference getFrameworkType4_DescribeDatasetsRequest();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.FrameworkType4#getDataset <em>Dataset</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Dataset</em>'.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4#getDataset()
     * @see #getFrameworkType4()
     */
    EReference getFrameworkType4_Dataset();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.GDASType <em>GDAS Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>GDAS Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GDASType
     */
    EClass getGDASType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GDASType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.GDASType#getFramework()
     * @see #getGDASType()
     */
    EReference getGDASType_Framework();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GDASType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.GDASType#getCapabilities()
     * @see #getGDASType()
     */
    EAttribute getGDASType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GDASType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.GDASType#getLang()
     * @see #getGDASType()
     */
    EAttribute getGDASType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GDASType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.GDASType#getService()
     * @see #getGDASType()
     */
    EAttribute getGDASType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GDASType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.GDASType#getVersion()
     * @see #getGDASType()
     */
    EAttribute getGDASType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.GetCapabilitiesType <em>Get Capabilities Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Get Capabilities Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType
     */
    EClass getGetCapabilitiesType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GetCapabilitiesType#getAcceptVersions <em>Accept Versions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Accept Versions</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType#getAcceptVersions()
     * @see #getGetCapabilitiesType()
     */
    EReference getGetCapabilitiesType_AcceptVersions();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GetCapabilitiesType#getSections <em>Sections</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Sections</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType#getSections()
     * @see #getGetCapabilitiesType()
     */
    EReference getGetCapabilitiesType_Sections();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GetCapabilitiesType#getAcceptFormats <em>Accept Formats</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Accept Formats</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType#getAcceptFormats()
     * @see #getGetCapabilitiesType()
     */
    EReference getGetCapabilitiesType_AcceptFormats();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetCapabilitiesType#getLanguage <em>Language</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Language</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType#getLanguage()
     * @see #getGetCapabilitiesType()
     */
    EAttribute getGetCapabilitiesType_Language();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetCapabilitiesType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType#getService()
     * @see #getGetCapabilitiesType()
     */
    EAttribute getGetCapabilitiesType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetCapabilitiesType#getUpdateSequence <em>Update Sequence</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Update Sequence</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType#getUpdateSequence()
     * @see #getGetCapabilitiesType()
     */
    EAttribute getGetCapabilitiesType_UpdateSequence();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.GetDataRequestType <em>Get Data Request Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Get Data Request Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataRequestType
     */
    EClass getGetDataRequestType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataRequestType#getHref <em>Href</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Href</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataRequestType#getHref()
     * @see #getGetDataRequestType()
     */
    EAttribute getGetDataRequestType_Href();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.GetDataType <em>Get Data Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Get Data Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType
     */
    EClass getGetDataType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getFrameworkURI()
     * @see #getGetDataType()
     */
    EAttribute getGetDataType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataType#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getDatasetURI()
     * @see #getGetDataType()
     */
    EAttribute getGetDataType_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataType#getAttributes <em>Attributes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Attributes</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getAttributes()
     * @see #getGetDataType()
     */
    EAttribute getGetDataType_Attributes();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataType#getLinkageKeys <em>Linkage Keys</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Linkage Keys</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getLinkageKeys()
     * @see #getGetDataType()
     */
    EAttribute getGetDataType_LinkageKeys();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GetDataType#getFilterColumn <em>Filter Column</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Filter Column</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getFilterColumn()
     * @see #getGetDataType()
     */
    EReference getGetDataType_FilterColumn();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GetDataType#getFilterValue <em>Filter Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Filter Value</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getFilterValue()
     * @see #getGetDataType()
     */
    EReference getGetDataType_FilterValue();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.GetDataType#getXSL <em>XSL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>XSL</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#getXSL()
     * @see #getGetDataType()
     */
    EReference getGetDataType_XSL();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataType#isAid <em>Aid</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Aid</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataType#isAid()
     * @see #getGetDataType()
     */
    EAttribute getGetDataType_Aid();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.GetDataXMLType <em>Get Data XML Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Get Data XML Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType
     */
    EClass getGetDataXMLType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataXMLType#getFrameworkURI <em>Framework URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Framework URI</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType#getFrameworkURI()
     * @see #getGetDataXMLType()
     */
    EAttribute getGetDataXMLType_FrameworkURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataXMLType#getDatasetURI <em>Dataset URI</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Dataset URI</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType#getDatasetURI()
     * @see #getGetDataXMLType()
     */
    EAttribute getGetDataXMLType_DatasetURI();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataXMLType#getAttributes <em>Attributes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Attributes</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType#getAttributes()
     * @see #getGetDataXMLType()
     */
    EAttribute getGetDataXMLType_Attributes();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataXMLType#getLinkageKeys <em>Linkage Keys</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Linkage Keys</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType#getLinkageKeys()
     * @see #getGetDataXMLType()
     */
    EAttribute getGetDataXMLType_LinkageKeys();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataXMLType#getGetDataHost <em>Get Data Host</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Get Data Host</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType#getGetDataHost()
     * @see #getGetDataXMLType()
     */
    EAttribute getGetDataXMLType_GetDataHost();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.GetDataXMLType#getLanguage <em>Language</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Language</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType#getLanguage()
     * @see #getGetDataXMLType()
     */
    EAttribute getGetDataXMLType_Language();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.JoinAbilitiesType <em>Join Abilities Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Join Abilities Type</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType
     */
    EClass getJoinAbilitiesType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinAbilitiesType#getSpatialFrameworks <em>Spatial Frameworks</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Spatial Frameworks</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getSpatialFrameworks()
     * @see #getJoinAbilitiesType()
     */
    EReference getJoinAbilitiesType_SpatialFrameworks();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinAbilitiesType#getAttributeLimit <em>Attribute Limit</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Attribute Limit</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getAttributeLimit()
     * @see #getJoinAbilitiesType()
     */
    EAttribute getJoinAbilitiesType_AttributeLimit();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinAbilitiesType#getOutputMechanisms <em>Output Mechanisms</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Output Mechanisms</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getOutputMechanisms()
     * @see #getJoinAbilitiesType()
     */
    EReference getJoinAbilitiesType_OutputMechanisms();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinAbilitiesType#getOutputStylings <em>Output Stylings</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Output Stylings</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getOutputStylings()
     * @see #getJoinAbilitiesType()
     */
    EReference getJoinAbilitiesType_OutputStylings();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinAbilitiesType#getClassificationSchemaURL <em>Classification Schema URL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Classification Schema URL</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getClassificationSchemaURL()
     * @see #getJoinAbilitiesType()
     */
    EReference getJoinAbilitiesType_ClassificationSchemaURL();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinAbilitiesType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getCapabilities()
     * @see #getJoinAbilitiesType()
     */
    EAttribute getJoinAbilitiesType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinAbilitiesType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getLang()
     * @see #getJoinAbilitiesType()
     */
    EAttribute getJoinAbilitiesType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinAbilitiesType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getService()
     * @see #getJoinAbilitiesType()
     */
    EAttribute getJoinAbilitiesType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinAbilitiesType#isUpdateSupported <em>Update Supported</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Update Supported</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#isUpdateSupported()
     * @see #getJoinAbilitiesType()
     */
    EAttribute getJoinAbilitiesType_UpdateSupported();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinAbilitiesType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType#getVersion()
     * @see #getJoinAbilitiesType()
     */
    EAttribute getJoinAbilitiesType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.JoinDataResponseType <em>Join Data Response Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Join Data Response Type</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType
     */
    EClass getJoinDataResponseType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinDataResponseType#getStatus <em>Status</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Status</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getStatus()
     * @see #getJoinDataResponseType()
     */
    EReference getJoinDataResponseType_Status();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinDataResponseType#getDataInputs <em>Data Inputs</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Data Inputs</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getDataInputs()
     * @see #getJoinDataResponseType()
     */
    EReference getJoinDataResponseType_DataInputs();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinDataResponseType#getJoinedOutputs <em>Joined Outputs</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Joined Outputs</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getJoinedOutputs()
     * @see #getJoinDataResponseType()
     */
    EReference getJoinDataResponseType_JoinedOutputs();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinDataResponseType#getCapabilities <em>Capabilities</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Capabilities</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getCapabilities()
     * @see #getJoinDataResponseType()
     */
    EAttribute getJoinDataResponseType_Capabilities();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinDataResponseType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getLang()
     * @see #getJoinDataResponseType()
     */
    EAttribute getJoinDataResponseType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinDataResponseType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getService()
     * @see #getJoinDataResponseType()
     */
    EAttribute getJoinDataResponseType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinDataResponseType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType#getVersion()
     * @see #getJoinDataResponseType()
     */
    EAttribute getJoinDataResponseType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.JoinDataType <em>Join Data Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Join Data Type</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataType
     */
    EClass getJoinDataType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinDataType#getAttributeData <em>Attribute Data</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Attribute Data</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataType#getAttributeData()
     * @see #getJoinDataType()
     */
    EReference getJoinDataType_AttributeData();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinDataType#getMapStyling <em>Map Styling</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Map Styling</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataType#getMapStyling()
     * @see #getJoinDataType()
     */
    EReference getJoinDataType_MapStyling();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.JoinDataType#getClassificationURL <em>Classification URL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Classification URL</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataType#getClassificationURL()
     * @see #getJoinDataType()
     */
    EReference getJoinDataType_ClassificationURL();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.JoinDataType#getUpdate <em>Update</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Update</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataType#getUpdate()
     * @see #getJoinDataType()
     */
    EAttribute getJoinDataType_Update();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.JoinedOutputsType <em>Joined Outputs Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Joined Outputs Type</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinedOutputsType
     */
    EClass getJoinedOutputsType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.JoinedOutputsType#getOutput <em>Output</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Output</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinedOutputsType#getOutput()
     * @see #getJoinedOutputsType()
     */
    EReference getJoinedOutputsType_Output();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.KType <em>KType</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>KType</em>'.
     * @generated
     * @see net.opengis.tjs10.KType
     */
    EClass getKType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.KType#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.KType#getValue()
     * @see #getKType()
     */
    EAttribute getKType_Value();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.KType#getAid <em>Aid</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Aid</em>'.
     * @generated
     * @see net.opengis.tjs10.KType#getAid()
     * @see #getKType()
     */
    EAttribute getKType_Aid();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.LanguagesType <em>Languages Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Languages Type</em>'.
     * @generated
     * @see net.opengis.tjs10.LanguagesType
     */
    EClass getLanguagesType();

    /**
     * Returns the meta object for the attribute list '{@link net.opengis.tjs10.LanguagesType#getLanguage <em>Language</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute list '<em>Language</em>'.
     * @generated
     * @see net.opengis.tjs10.LanguagesType#getLanguage()
     * @see #getLanguagesType()
     */
    EAttribute getLanguagesType_Language();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.MapStylingType <em>Map Styling Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Map Styling Type</em>'.
     * @generated
     * @see net.opengis.tjs10.MapStylingType
     */
    EClass getMapStylingType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.MapStylingType#getStylingIdentifier <em>Styling Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Styling Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.MapStylingType#getStylingIdentifier()
     * @see #getMapStylingType()
     */
    EReference getMapStylingType_StylingIdentifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.MapStylingType#getStylingURL <em>Styling URL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Styling URL</em>'.
     * @generated
     * @see net.opengis.tjs10.MapStylingType#getStylingURL()
     * @see #getMapStylingType()
     */
    EAttribute getMapStylingType_StylingURL();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.MeasureCountExceptions <em>Measure Count Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Measure Count Exceptions</em>'.
     * @generated
     * @see net.opengis.tjs10.MeasureCountExceptions
     */
    EClass getMeasureCountExceptions();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.MeasureCountExceptions#getNull <em>Null</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Null</em>'.
     * @generated
     * @see net.opengis.tjs10.MeasureCountExceptions#getNull()
     * @see #getMeasureCountExceptions()
     */
    EReference getMeasureCountExceptions_Null();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.MeasureType <em>Measure Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Measure Type</em>'.
     * @generated
     * @see net.opengis.tjs10.MeasureType
     */
    EClass getMeasureType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.MeasureType#getUOM <em>UOM</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>UOM</em>'.
     * @generated
     * @see net.opengis.tjs10.MeasureType#getUOM()
     * @see #getMeasureType()
     */
    EReference getMeasureType_UOM();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.MeasureType#getUncertainty <em>Uncertainty</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Uncertainty</em>'.
     * @generated
     * @see net.opengis.tjs10.MeasureType#getUncertainty()
     * @see #getMeasureType()
     */
    EReference getMeasureType_Uncertainty();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.MeasureType#getExceptions <em>Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Exceptions</em>'.
     * @generated
     * @see net.opengis.tjs10.MeasureType#getExceptions()
     * @see #getMeasureType()
     */
    EReference getMeasureType_Exceptions();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.MechanismType <em>Mechanism Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Mechanism Type</em>'.
     * @generated
     * @see net.opengis.tjs10.MechanismType
     */
    EClass getMechanismType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.MechanismType#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.MechanismType#getIdentifier()
     * @see #getMechanismType()
     */
    EAttribute getMechanismType_Identifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.MechanismType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.MechanismType#getTitle()
     * @see #getMechanismType()
     */
    EAttribute getMechanismType_Title();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.MechanismType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.MechanismType#getAbstract()
     * @see #getMechanismType()
     */
    EAttribute getMechanismType_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.MechanismType#getReference <em>Reference</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Reference</em>'.
     * @generated
     * @see net.opengis.tjs10.MechanismType#getReference()
     * @see #getMechanismType()
     */
    EAttribute getMechanismType_Reference();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.NominalOrdinalExceptions <em>Nominal Ordinal Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Nominal Ordinal Exceptions</em>'.
     * @generated
     * @see net.opengis.tjs10.NominalOrdinalExceptions
     */
    EClass getNominalOrdinalExceptions();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.NominalOrdinalExceptions#getNull <em>Null</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Null</em>'.
     * @generated
     * @see net.opengis.tjs10.NominalOrdinalExceptions#getNull()
     * @see #getNominalOrdinalExceptions()
     */
    EReference getNominalOrdinalExceptions_Null();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.NominalType <em>Nominal Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Nominal Type</em>'.
     * @generated
     * @see net.opengis.tjs10.NominalType
     */
    EClass getNominalType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.NominalType#getClasses <em>Classes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Classes</em>'.
     * @generated
     * @see net.opengis.tjs10.NominalType#getClasses()
     * @see #getNominalType()
     */
    EReference getNominalType_Classes();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.NominalType#getExceptions <em>Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Exceptions</em>'.
     * @generated
     * @see net.opengis.tjs10.NominalType#getExceptions()
     * @see #getNominalType()
     */
    EReference getNominalType_Exceptions();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.NullType <em>Null Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Null Type</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType
     */
    EClass getNullType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType#getIdentifier()
     * @see #getNullType()
     */
    EAttribute getNullType_Identifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType#getTitle()
     * @see #getNullType()
     */
    EAttribute getNullType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.NullType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType#getAbstract()
     * @see #getNullType()
     */
    EReference getNullType_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType#getDocumentation()
     * @see #getNullType()
     */
    EAttribute getNullType_Documentation();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.NullType1 <em>Null Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Null Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType1
     */
    EClass getNullType1();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType1#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType1#getIdentifier()
     * @see #getNullType1()
     */
    EAttribute getNullType1_Identifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType1#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType1#getTitle()
     * @see #getNullType1()
     */
    EAttribute getNullType1_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.NullType1#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType1#getAbstract()
     * @see #getNullType1()
     */
    EReference getNullType1_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType1#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType1#getDocumentation()
     * @see #getNullType1()
     */
    EAttribute getNullType1_Documentation();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.NullType1#getColor <em>Color</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Color</em>'.
     * @generated
     * @see net.opengis.tjs10.NullType1#getColor()
     * @see #getNullType1()
     */
    EAttribute getNullType1_Color();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.OrdinalType <em>Ordinal Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Ordinal Type</em>'.
     * @generated
     * @see net.opengis.tjs10.OrdinalType
     */
    EClass getOrdinalType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.OrdinalType#getClasses <em>Classes</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Classes</em>'.
     * @generated
     * @see net.opengis.tjs10.OrdinalType#getClasses()
     * @see #getOrdinalType()
     */
    EReference getOrdinalType_Classes();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.OrdinalType#getExceptions <em>Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Exceptions</em>'.
     * @generated
     * @see net.opengis.tjs10.OrdinalType#getExceptions()
     * @see #getOrdinalType()
     */
    EReference getOrdinalType_Exceptions();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.OutputMechanismsType <em>Output Mechanisms Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Output Mechanisms Type</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputMechanismsType
     */
    EClass getOutputMechanismsType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.OutputMechanismsType#getMechanism <em>Mechanism</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Mechanism</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputMechanismsType#getMechanism()
     * @see #getOutputMechanismsType()
     */
    EReference getOutputMechanismsType_Mechanism();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.OutputStylingsType <em>Output Stylings Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Output Stylings Type</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputStylingsType
     */
    EClass getOutputStylingsType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.OutputStylingsType#getStyling <em>Styling</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Styling</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputStylingsType#getStyling()
     * @see #getOutputStylingsType()
     */
    EReference getOutputStylingsType_Styling();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.OutputStylingsType1 <em>Output Stylings Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Output Stylings Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputStylingsType1
     */
    EClass getOutputStylingsType1();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.OutputType <em>Output Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Output Type</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputType
     */
    EClass getOutputType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.OutputType#getMechanism <em>Mechanism</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Mechanism</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputType#getMechanism()
     * @see #getOutputType()
     */
    EReference getOutputType_Mechanism();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.OutputType#getResource <em>Resource</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Resource</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputType#getResource()
     * @see #getOutputType()
     */
    EReference getOutputType_Resource();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.OutputType#getExceptionReport <em>Exception Report</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Exception Report</em>'.
     * @generated
     * @see net.opengis.tjs10.OutputType#getExceptionReport()
     * @see #getOutputType()
     */
    EReference getOutputType_ExceptionReport();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ParameterType <em>Parameter Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Parameter Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ParameterType
     */
    EClass getParameterType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ParameterType#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.ParameterType#getValue()
     * @see #getParameterType()
     */
    EAttribute getParameterType_Value();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ParameterType#getName <em>Name</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Name</em>'.
     * @generated
     * @see net.opengis.tjs10.ParameterType#getName()
     * @see #getParameterType()
     */
    EAttribute getParameterType_Name();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ReferenceDateType <em>Reference Date Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Reference Date Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ReferenceDateType
     */
    EClass getReferenceDateType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ReferenceDateType#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.ReferenceDateType#getValue()
     * @see #getReferenceDateType()
     */
    EAttribute getReferenceDateType_Value();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ReferenceDateType#getStartDate <em>Start Date</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Start Date</em>'.
     * @generated
     * @see net.opengis.tjs10.ReferenceDateType#getStartDate()
     * @see #getReferenceDateType()
     */
    EAttribute getReferenceDateType_StartDate();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.RequestBaseType <em>Request Base Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Request Base Type</em>'.
     * @generated
     * @see net.opengis.tjs10.RequestBaseType
     */
    EClass getRequestBaseType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.RequestBaseType#getLanguage <em>Language</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Language</em>'.
     * @generated
     * @see net.opengis.tjs10.RequestBaseType#getLanguage()
     * @see #getRequestBaseType()
     */
    EAttribute getRequestBaseType_Language();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.RequestBaseType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.RequestBaseType#getService()
     * @see #getRequestBaseType()
     */
    EAttribute getRequestBaseType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.RequestBaseType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.RequestBaseType#getVersion()
     * @see #getRequestBaseType()
     */
    EAttribute getRequestBaseType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ResourceType <em>Resource Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Resource Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ResourceType
     */
    EClass getResourceType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ResourceType#getURL <em>URL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>URL</em>'.
     * @generated
     * @see net.opengis.tjs10.ResourceType#getURL()
     * @see #getResourceType()
     */
    EReference getResourceType_URL();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.ResourceType#getParameter <em>Parameter</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Parameter</em>'.
     * @generated
     * @see net.opengis.tjs10.ResourceType#getParameter()
     * @see #getResourceType()
     */
    EReference getResourceType_Parameter();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.RowsetType <em>Rowset Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Rowset Type</em>'.
     * @generated
     * @see net.opengis.tjs10.RowsetType
     */
    EClass getRowsetType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.RowsetType#getRow <em>Row</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Row</em>'.
     * @generated
     * @see net.opengis.tjs10.RowsetType#getRow()
     * @see #getRowsetType()
     */
    EReference getRowsetType_Row();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.RowsetType1 <em>Rowset Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Rowset Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.RowsetType1
     */
    EClass getRowsetType1();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.RowsetType1#getRow <em>Row</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Row</em>'.
     * @generated
     * @see net.opengis.tjs10.RowsetType1#getRow()
     * @see #getRowsetType1()
     */
    EReference getRowsetType1_Row();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.RowType <em>Row Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Row Type</em>'.
     * @generated
     * @see net.opengis.tjs10.RowType
     */
    EClass getRowType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.RowType#getK <em>K</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>K</em>'.
     * @generated
     * @see net.opengis.tjs10.RowType#getK()
     * @see #getRowType()
     */
    EReference getRowType_K();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.RowType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.RowType#getTitle()
     * @see #getRowType()
     */
    EAttribute getRowType_Title();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.RowType1 <em>Row Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Row Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.RowType1
     */
    EClass getRowType1();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.RowType1#getK <em>K</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>K</em>'.
     * @generated
     * @see net.opengis.tjs10.RowType1#getK()
     * @see #getRowType1()
     */
    EReference getRowType1_K();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.RowType1#getV <em>V</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>V</em>'.
     * @generated
     * @see net.opengis.tjs10.RowType1#getV()
     * @see #getRowType1()
     */
    EReference getRowType1_V();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.SpatialFrameworksType <em>Spatial Frameworks Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Spatial Frameworks Type</em>'.
     * @generated
     * @see net.opengis.tjs10.SpatialFrameworksType
     */
    EClass getSpatialFrameworksType();

    /**
     * Returns the meta object for the containment reference list '{@link net.opengis.tjs10.SpatialFrameworksType#getFramework <em>Framework</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference list '<em>Framework</em>'.
     * @generated
     * @see net.opengis.tjs10.SpatialFrameworksType#getFramework()
     * @see #getSpatialFrameworksType()
     */
    EReference getSpatialFrameworksType_Framework();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.StatusType <em>Status Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Status Type</em>'.
     * @generated
     * @see net.opengis.tjs10.StatusType
     */
    EClass getStatusType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.StatusType#getAccepted <em>Accepted</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Accepted</em>'.
     * @generated
     * @see net.opengis.tjs10.StatusType#getAccepted()
     * @see #getStatusType()
     */
    EReference getStatusType_Accepted();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.StatusType#getCompleted <em>Completed</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Completed</em>'.
     * @generated
     * @see net.opengis.tjs10.StatusType#getCompleted()
     * @see #getStatusType()
     */
    EReference getStatusType_Completed();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.StatusType#getFailed <em>Failed</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Failed</em>'.
     * @generated
     * @see net.opengis.tjs10.StatusType#getFailed()
     * @see #getStatusType()
     */
    EReference getStatusType_Failed();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StatusType#getCreationTime <em>Creation Time</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Creation Time</em>'.
     * @generated
     * @see net.opengis.tjs10.StatusType#getCreationTime()
     * @see #getStatusType()
     */
    EAttribute getStatusType_CreationTime();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StatusType#getHref <em>Href</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Href</em>'.
     * @generated
     * @see net.opengis.tjs10.StatusType#getHref()
     * @see #getStatusType()
     */
    EAttribute getStatusType_Href();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.StylingType <em>Styling Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Styling Type</em>'.
     * @generated
     * @see net.opengis.tjs10.StylingType
     */
    EClass getStylingType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StylingType#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.StylingType#getIdentifier()
     * @see #getStylingType()
     */
    EAttribute getStylingType_Identifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StylingType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.StylingType#getTitle()
     * @see #getStylingType()
     */
    EAttribute getStylingType_Title();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StylingType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.StylingType#getAbstract()
     * @see #getStylingType()
     */
    EAttribute getStylingType_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StylingType#getReference <em>Reference</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Reference</em>'.
     * @generated
     * @see net.opengis.tjs10.StylingType#getReference()
     * @see #getStylingType()
     */
    EAttribute getStylingType_Reference();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.StylingType#getSchema <em>Schema</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Schema</em>'.
     * @generated
     * @see net.opengis.tjs10.StylingType#getSchema()
     * @see #getStylingType()
     */
    EAttribute getStylingType_Schema();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.TjsCapabilitiesType <em>Tjs Capabilities Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Tjs Capabilities Type</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType
     */
    EClass getTjsCapabilitiesType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.TjsCapabilitiesType#getServiceIdentification <em>Service Identification</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Service Identification</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getServiceIdentification()
     * @see #getTjsCapabilitiesType()
     */
    EReference getTjsCapabilitiesType_ServiceIdentification();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.TjsCapabilitiesType#getServiceProvider <em>Service Provider</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Service Provider</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getServiceProvider()
     * @see #getTjsCapabilitiesType()
     */
    EReference getTjsCapabilitiesType_ServiceProvider();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.TjsCapabilitiesType#getOperationsMetadata <em>Operations Metadata</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Operations Metadata</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getOperationsMetadata()
     * @see #getTjsCapabilitiesType()
     */
    EReference getTjsCapabilitiesType_OperationsMetadata();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.TjsCapabilitiesType#getLanguages <em>Languages</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Languages</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getLanguages()
     * @see #getTjsCapabilitiesType()
     */
    EReference getTjsCapabilitiesType_Languages();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.TjsCapabilitiesType#getWSDL <em>WSDL</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>WSDL</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getWSDL()
     * @see #getTjsCapabilitiesType()
     */
    EReference getTjsCapabilitiesType_WSDL();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.TjsCapabilitiesType#getLang <em>Lang</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Lang</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getLang()
     * @see #getTjsCapabilitiesType()
     */
    EAttribute getTjsCapabilitiesType_Lang();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.TjsCapabilitiesType#getService <em>Service</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Service</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getService()
     * @see #getTjsCapabilitiesType()
     */
    EAttribute getTjsCapabilitiesType_Service();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.TjsCapabilitiesType#getUpdateSequence <em>Update Sequence</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Update Sequence</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getUpdateSequence()
     * @see #getTjsCapabilitiesType()
     */
    EAttribute getTjsCapabilitiesType_UpdateSequence();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.TjsCapabilitiesType#getVersion <em>Version</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Version</em>'.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType#getVersion()
     * @see #getTjsCapabilitiesType()
     */
    EAttribute getTjsCapabilitiesType_Version();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.UncertaintyType <em>Uncertainty Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Uncertainty Type</em>'.
     * @generated
     * @see net.opengis.tjs10.UncertaintyType
     */
    EClass getUncertaintyType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.UncertaintyType#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.UncertaintyType#getValue()
     * @see #getUncertaintyType()
     */
    EAttribute getUncertaintyType_Value();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.UncertaintyType#getGaussian <em>Gaussian</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Gaussian</em>'.
     * @generated
     * @see net.opengis.tjs10.UncertaintyType#getGaussian()
     * @see #getUncertaintyType()
     */
    EAttribute getUncertaintyType_Gaussian();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.UOMType <em>UOM Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>UOM Type</em>'.
     * @generated
     * @see net.opengis.tjs10.UOMType
     */
    EClass getUOMType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.UOMType#getShortForm <em>Short Form</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Short Form</em>'.
     * @generated
     * @see net.opengis.tjs10.UOMType#getShortForm()
     * @see #getUOMType()
     */
    EReference getUOMType_ShortForm();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.UOMType#getLongForm <em>Long Form</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Long Form</em>'.
     * @generated
     * @see net.opengis.tjs10.UOMType#getLongForm()
     * @see #getUOMType()
     */
    EReference getUOMType_LongForm();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.UOMType#getReference <em>Reference</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Reference</em>'.
     * @generated
     * @see net.opengis.tjs10.UOMType#getReference()
     * @see #getUOMType()
     */
    EAttribute getUOMType_Reference();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ValuesType <em>Values Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Values Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ValuesType
     */
    EClass getValuesType();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ValuesType#getNominal <em>Nominal</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Nominal</em>'.
     * @generated
     * @see net.opengis.tjs10.ValuesType#getNominal()
     * @see #getValuesType()
     */
    EReference getValuesType_Nominal();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ValuesType#getOrdinal <em>Ordinal</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Ordinal</em>'.
     * @generated
     * @see net.opengis.tjs10.ValuesType#getOrdinal()
     * @see #getValuesType()
     */
    EReference getValuesType_Ordinal();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ValuesType#getCount <em>Count</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Count</em>'.
     * @generated
     * @see net.opengis.tjs10.ValuesType#getCount()
     * @see #getValuesType()
     */
    EReference getValuesType_Count();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ValuesType#getMeasure <em>Measure</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Measure</em>'.
     * @generated
     * @see net.opengis.tjs10.ValuesType#getMeasure()
     * @see #getValuesType()
     */
    EReference getValuesType_Measure();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ValueType <em>Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType
     */
    EClass getValueType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType#getIdentifier()
     * @see #getValueType()
     */
    EAttribute getValueType_Identifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType#getTitle()
     * @see #getValueType()
     */
    EAttribute getValueType_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ValueType#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType#getAbstract()
     * @see #getValueType()
     */
    EReference getValueType_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType#getDocumentation()
     * @see #getValueType()
     */
    EAttribute getValueType_Documentation();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType#getColor <em>Color</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Color</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType#getColor()
     * @see #getValueType()
     */
    EAttribute getValueType_Color();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType#getRank <em>Rank</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Rank</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType#getRank()
     * @see #getValueType()
     */
    EAttribute getValueType_Rank();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.ValueType1 <em>Value Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>Value Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType1
     */
    EClass getValueType1();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType1#getIdentifier <em>Identifier</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Identifier</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType1#getIdentifier()
     * @see #getValueType1()
     */
    EAttribute getValueType1_Identifier();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType1#getTitle <em>Title</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Title</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType1#getTitle()
     * @see #getValueType1()
     */
    EAttribute getValueType1_Title();

    /**
     * Returns the meta object for the containment reference '{@link net.opengis.tjs10.ValueType1#getAbstract <em>Abstract</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the containment reference '<em>Abstract</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType1#getAbstract()
     * @see #getValueType1()
     */
    EReference getValueType1_Abstract();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType1#getDocumentation <em>Documentation</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Documentation</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType1#getDocumentation()
     * @see #getValueType1()
     */
    EAttribute getValueType1_Documentation();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.ValueType1#getColor <em>Color</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Color</em>'.
     * @generated
     * @see net.opengis.tjs10.ValueType1#getColor()
     * @see #getValueType1()
     */
    EAttribute getValueType1_Color();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.VType <em>VType</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>VType</em>'.
     * @generated
     * @see net.opengis.tjs10.VType
     */
    EClass getVType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.VType#getValue <em>Value</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Value</em>'.
     * @generated
     * @see net.opengis.tjs10.VType#getValue()
     * @see #getVType()
     */
    EAttribute getVType_Value();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.VType#getAid <em>Aid</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Aid</em>'.
     * @generated
     * @see net.opengis.tjs10.VType#getAid()
     * @see #getVType()
     */
    EAttribute getVType_Aid();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.VType#isNull <em>Null</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Null</em>'.
     * @generated
     * @see net.opengis.tjs10.VType#isNull()
     * @see #getVType()
     */
    EAttribute getVType_Null();

    /**
     * Returns the meta object for class '{@link net.opengis.tjs10.WSDLType <em>WSDL Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for class '<em>WSDL Type</em>'.
     * @generated
     * @see net.opengis.tjs10.WSDLType
     */
    EClass getWSDLType();

    /**
     * Returns the meta object for the attribute '{@link net.opengis.tjs10.WSDLType#getHref <em>Href</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for the attribute '<em>Href</em>'.
     * @generated
     * @see net.opengis.tjs10.WSDLType#getHref()
     * @see #getWSDLType()
     */
    EAttribute getWSDLType_Href();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.DataClassType <em>Data Class Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Data Class Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DataClassType
     */
    EEnum getDataClassType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.DescribeDatasetsValueType <em>Describe Datasets Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Describe Datasets Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsValueType
     */
    EEnum getDescribeDatasetsValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.DescribeDataValueType <em>Describe Data Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Describe Data Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeDataValueType
     */
    EEnum getDescribeDataValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.DescribeFrameworksValueType <em>Describe Frameworks Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Describe Frameworks Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksValueType
     */
    EEnum getDescribeFrameworksValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.DescribeJoinAbilitiesValueType <em>Describe Join Abilities Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Describe Join Abilities Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeJoinAbilitiesValueType
     */
    EEnum getDescribeJoinAbilitiesValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.DescribeKeyValueType <em>Describe Key Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Describe Key Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.DescribeKeyValueType
     */
    EEnum getDescribeKeyValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.GaussianType <em>Gaussian Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Gaussian Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GaussianType
     */
    EEnum getGaussianType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.GetCapabilitiesValueType <em>Get Capabilities Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Get Capabilities Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesValueType
     */
    EEnum getGetCapabilitiesValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.GetDataValueType <em>Get Data Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Get Data Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.GetDataValueType
     */
    EEnum getGetDataValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.JoinDataValueType <em>Join Data Value Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Join Data Value Type</em>'.
     * @generated
     * @see net.opengis.tjs10.JoinDataValueType
     */
    EEnum getJoinDataValueType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.PurposeType <em>Purpose Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Purpose Type</em>'.
     * @generated
     * @see net.opengis.tjs10.PurposeType
     */
    EEnum getPurposeType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.RequestServiceType <em>Request Service Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Request Service Type</em>'.
     * @generated
     * @see net.opengis.tjs10.RequestServiceType
     */
    EEnum getRequestServiceType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.TypeType <em>Type Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Type Type</em>'.
     * @generated
     * @see net.opengis.tjs10.TypeType
     */
    EEnum getTypeType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.UpdateType <em>Update Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Update Type</em>'.
     * @generated
     * @see net.opengis.tjs10.UpdateType
     */
    EEnum getUpdateType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.VersionType <em>Version Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Version Type</em>'.
     * @generated
     * @see net.opengis.tjs10.VersionType
     */
    EEnum getVersionType();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.VersionType1 <em>Version Type1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Version Type1</em>'.
     * @generated
     * @see net.opengis.tjs10.VersionType1
     */
    EEnum getVersionType1();

    /**
     * Returns the meta object for enum '{@link net.opengis.tjs10.VersionType2 <em>Version Type2</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for enum '<em>Version Type2</em>'.
     * @generated
     * @see net.opengis.tjs10.VersionType2
     */
    EEnum getVersionType2();

    /**
     * Returns the meta object for data type '{@link java.lang.String <em>Accept Languages Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Accept Languages Type</em>'.
     * @model instanceClass="java.lang.String"
     * extendedMetaData="name='AcceptLanguagesType' baseType='http://www.eclipse.org/emf/2003/XMLType#string'"
     * @generated
     * @see java.lang.String
     */
    EDataType getAcceptLanguagesType();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.DataClassType <em>Data Class Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Data Class Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.DataClassType"
     * extendedMetaData="name='DataClass_._type:Object' baseType='DataClass_._type'"
     * @generated
     * @see net.opengis.tjs10.DataClassType
     */
    EDataType getDataClassTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.DescribeDatasetsValueType <em>Describe Datasets Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Describe Datasets Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.DescribeDatasetsValueType"
     * extendedMetaData="name='DescribeDatasetsValueType:Object' baseType='DescribeDatasetsValueType'"
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsValueType
     */
    EDataType getDescribeDatasetsValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.DescribeDataValueType <em>Describe Data Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Describe Data Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.DescribeDataValueType"
     * extendedMetaData="name='DescribeDataValueType:Object' baseType='DescribeDataValueType'"
     * @generated
     * @see net.opengis.tjs10.DescribeDataValueType
     */
    EDataType getDescribeDataValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.DescribeFrameworksValueType <em>Describe Frameworks Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Describe Frameworks Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.DescribeFrameworksValueType"
     * extendedMetaData="name='DescribeFrameworksValueType:Object' baseType='DescribeFrameworksValueType'"
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksValueType
     */
    EDataType getDescribeFrameworksValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.DescribeJoinAbilitiesValueType <em>Describe Join Abilities Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Describe Join Abilities Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.DescribeJoinAbilitiesValueType"
     * extendedMetaData="name='DescribeJoinAbilitiesValueType:Object' baseType='DescribeJoinAbilitiesValueType'"
     * @generated
     * @see net.opengis.tjs10.DescribeJoinAbilitiesValueType
     */
    EDataType getDescribeJoinAbilitiesValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.DescribeKeyValueType <em>Describe Key Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Describe Key Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.DescribeKeyValueType"
     * extendedMetaData="name='DescribeKeyValueType:Object' baseType='DescribeKeyValueType'"
     * @generated
     * @see net.opengis.tjs10.DescribeKeyValueType
     */
    EDataType getDescribeKeyValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.GaussianType <em>Gaussian Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Gaussian Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.GaussianType"
     * extendedMetaData="name='gaussian_._type:Object' baseType='gaussian_._type'"
     * @generated
     * @see net.opengis.tjs10.GaussianType
     */
    EDataType getGaussianTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.GetCapabilitiesValueType <em>Get Capabilities Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Get Capabilities Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.GetCapabilitiesValueType"
     * extendedMetaData="name='GetCapabilitiesValueType:Object' baseType='GetCapabilitiesValueType'"
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesValueType
     */
    EDataType getGetCapabilitiesValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.GetDataValueType <em>Get Data Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Get Data Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.GetDataValueType"
     * extendedMetaData="name='GetDataValueType:Object' baseType='GetDataValueType'"
     * @generated
     * @see net.opengis.tjs10.GetDataValueType
     */
    EDataType getGetDataValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.JoinDataValueType <em>Join Data Value Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Join Data Value Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.JoinDataValueType"
     * extendedMetaData="name='JoinDataValueType:Object' baseType='JoinDataValueType'"
     * @generated
     * @see net.opengis.tjs10.JoinDataValueType
     */
    EDataType getJoinDataValueTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.PurposeType <em>Purpose Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Purpose Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.PurposeType"
     * extendedMetaData="name='purpose_._type:Object' baseType='purpose_._type'"
     * @generated
     * @see net.opengis.tjs10.PurposeType
     */
    EDataType getPurposeTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.RequestServiceType <em>Request Service Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Request Service Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.RequestServiceType"
     * extendedMetaData="name='RequestServiceType:Object' baseType='RequestServiceType'"
     * @generated
     * @see net.opengis.tjs10.RequestServiceType
     */
    EDataType getRequestServiceTypeObject();

    /**
     * Returns the meta object for data type '{@link java.lang.String <em>Sections Type</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Sections Type</em>'.
     * @model instanceClass="java.lang.String"
     * extendedMetaData="name='SectionsType' baseType='http://www.eclipse.org/emf/2003/XMLType#string' pattern='(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes)(,(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes))*'"
     * @generated
     * @see java.lang.String
     */
    EDataType getSectionsType();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.TypeType <em>Type Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Type Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.TypeType"
     * extendedMetaData="name='type_._type:Object' baseType='type_._type'"
     * @generated
     * @see net.opengis.tjs10.TypeType
     */
    EDataType getTypeTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.UpdateType <em>Update Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Update Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.UpdateType"
     * extendedMetaData="name='update_._type:Object' baseType='update_._type'"
     * @generated
     * @see net.opengis.tjs10.UpdateType
     */
    EDataType getUpdateTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.VersionType <em>Version Type Object</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Version Type Object</em>'.
     * @model instanceClass="net.opengis.tjs10.VersionType"
     * extendedMetaData="name='VersionType:Object' baseType='VersionType'"
     * @generated
     * @see net.opengis.tjs10.VersionType
     */
    EDataType getVersionTypeObject();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.VersionType1 <em>Version Type Object1</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Version Type Object1</em>'.
     * @model instanceClass="net.opengis.tjs10.VersionType1"
     * extendedMetaData="name='Version_._type:Object' baseType='Version_._type'"
     * @generated
     * @see net.opengis.tjs10.VersionType1
     */
    EDataType getVersionTypeObject1();

    /**
     * Returns the meta object for data type '{@link net.opengis.tjs10.VersionType2 <em>Version Type Object2</em>}'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the meta object for data type '<em>Version Type Object2</em>'.
     * @model instanceClass="net.opengis.tjs10.VersionType2"
     * extendedMetaData="name='version_._type:Object' baseType='version_._type'"
     * @generated
     * @see net.opengis.tjs10.VersionType2
     */
    EDataType getVersionTypeObject2();

    /**
     * Returns the factory that creates the instances of the model.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the factory that creates the instances of the model.
     * @generated
     */
    Tjs10Factory getTjs10Factory();

} //Tjs10Package
