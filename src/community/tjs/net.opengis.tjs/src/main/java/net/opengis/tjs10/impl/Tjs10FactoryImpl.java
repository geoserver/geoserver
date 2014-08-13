/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.impl;

import net.opengis.tjs10.*;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.xml.type.XMLTypeFactory;
import org.eclipse.emf.ecore.xml.type.XMLTypePackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 *
 * @generated
 */
public class Tjs10FactoryImpl extends EFactoryImpl implements Tjs10Factory {
    /**
     * Creates the default factory implementation.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static Tjs10Factory init() {
        try {
            Tjs10Factory theTjs10Factory = (Tjs10Factory) EPackage.Registry.INSTANCE.getEFactory("http://www.opengis.net/tjs/1.0");
            if (theTjs10Factory != null) {
                return theTjs10Factory;
            }
        } catch (Exception exception) {
            EcorePlugin.INSTANCE.log(exception);
        }
        return new Tjs10FactoryImpl();
    }

    /**
     * Creates an instance of the factory.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Tjs10FactoryImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public EObject create(EClass eClass) {
        switch (eClass.getClassifierID()) {
            case Tjs10Package.ABSTRACT_TYPE:
                return createAbstractType();
            case Tjs10Package.ACCEPT_VERSIONS_TYPE:
                return createAcceptVersionsType();
            case Tjs10Package.ATTRIBUTE_DATA_TYPE:
                return createAttributeDataType();
            case Tjs10Package.ATTRIBUTES_TYPE:
                return createAttributesType();
            case Tjs10Package.BOUNDING_COORDINATES_TYPE:
                return createBoundingCoordinatesType();
            case Tjs10Package.CLASSES_TYPE:
                return createClassesType();
            case Tjs10Package.CLASSES_TYPE1:
                return createClassesType1();
            case Tjs10Package.COLUMNSET_TYPE:
                return createColumnsetType();
            case Tjs10Package.COLUMN_TYPE:
                return createColumnType();
            case Tjs10Package.COLUMN_TYPE1:
                return createColumnType1();
            case Tjs10Package.COLUMN_TYPE2:
                return createColumnType2();
            case Tjs10Package.COUNT_TYPE:
                return createCountType();
            case Tjs10Package.DATA_DESCRIPTIONS_TYPE:
                return createDataDescriptionsType();
            case Tjs10Package.DATA_INPUTS_TYPE:
                return createDataInputsType();
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE:
                return createDatasetDescriptionsType();
            case Tjs10Package.DATASET_TYPE:
                return createDatasetType();
            case Tjs10Package.DATASET_TYPE1:
                return createDatasetType1();
            case Tjs10Package.DATASET_TYPE2:
                return createDatasetType2();
            case Tjs10Package.DATASET_TYPE3:
                return createDatasetType3();
            case Tjs10Package.DESCRIBE_DATA_REQUEST_TYPE:
                return createDescribeDataRequestType();
            case Tjs10Package.DESCRIBE_DATASETS_REQUEST_TYPE:
                return createDescribeDatasetsRequestType();
            case Tjs10Package.DESCRIBE_DATASETS_TYPE:
                return createDescribeDatasetsType();
            case Tjs10Package.DESCRIBE_DATA_TYPE:
                return createDescribeDataType();
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE:
                return createDescribeFrameworkKeyType();
            case Tjs10Package.DESCRIBE_FRAMEWORKS_TYPE:
                return createDescribeFrameworksType();
            case Tjs10Package.DESCRIBE_KEY_TYPE:
                return createDescribeKeyType();
            case Tjs10Package.DOCUMENT_ROOT:
                return createDocumentRoot();
            case Tjs10Package.EXCEPTION_REPORT_TYPE:
                return createExceptionReportType();
            case Tjs10Package.FAILED_TYPE:
                return createFailedType();
            case Tjs10Package.FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE:
                return createFrameworkDatasetDescribeDataType();
            case Tjs10Package.FRAMEWORK_DESCRIPTIONS_TYPE:
                return createFrameworkDescriptionsType();
            case Tjs10Package.FRAMEWORK_KEY_DESCRIPTION_TYPE:
                return createFrameworkKeyDescriptionType();
            case Tjs10Package.FRAMEWORK_KEY_TYPE:
                return createFrameworkKeyType();
            case Tjs10Package.FRAMEWORK_KEY_TYPE1:
                return createFrameworkKeyType1();
            case Tjs10Package.FRAMEWORK_TYPE:
                return createFrameworkType();
            case Tjs10Package.FRAMEWORK_TYPE1:
                return createFrameworkType1();
            case Tjs10Package.FRAMEWORK_TYPE2:
                return createFrameworkType2();
            case Tjs10Package.FRAMEWORK_TYPE3:
                return createFrameworkType3();
            case Tjs10Package.FRAMEWORK_TYPE4:
                return createFrameworkType4();
            case Tjs10Package.GDAS_TYPE:
                return createGDASType();
            case Tjs10Package.GET_CAPABILITIES_TYPE:
                return createGetCapabilitiesType();
            case Tjs10Package.GET_DATA_REQUEST_TYPE:
                return createGetDataRequestType();
            case Tjs10Package.GET_DATA_TYPE:
                return createGetDataType();
            case Tjs10Package.GET_DATA_XML_TYPE:
                return createGetDataXMLType();
            case Tjs10Package.JOIN_ABILITIES_TYPE:
                return createJoinAbilitiesType();
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE:
                return createJoinDataResponseType();
            case Tjs10Package.JOIN_DATA_TYPE:
                return createJoinDataType();
            case Tjs10Package.JOINED_OUTPUTS_TYPE:
                return createJoinedOutputsType();
            case Tjs10Package.KTYPE:
                return createKType();

            case Tjs10Package.SHORTFORM_TYPE:
                return createShortForm();
            case Tjs10Package.LONGFORM_TYPE:
                return createLongForm();

            case Tjs10Package.LANGUAGES_TYPE:
                return createLanguagesType();
            case Tjs10Package.MAP_STYLING_TYPE:
                return createMapStylingType();
            case Tjs10Package.MEASURE_COUNT_EXCEPTIONS:
                return createMeasureCountExceptions();
            case Tjs10Package.MEASURE_TYPE:
                return createMeasureType();
            case Tjs10Package.MECHANISM_TYPE:
                return createMechanismType();
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS:
                return createNominalOrdinalExceptions();
            case Tjs10Package.NOMINAL_TYPE:
                return createNominalType();
            case Tjs10Package.NULL_TYPE:
                return createNullType();
            case Tjs10Package.NULL_TYPE1:
                return createNullType1();
            case Tjs10Package.ORDINAL_TYPE:
                return createOrdinalType();
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE:
                return createOutputMechanismsType();
            case Tjs10Package.OUTPUT_STYLINGS_TYPE:
                return createOutputStylingsType();
            case Tjs10Package.OUTPUT_STYLINGS_TYPE1:
                return createOutputStylingsType1();
            case Tjs10Package.OUTPUT_TYPE:
                return createOutputType();
            case Tjs10Package.PARAMETER_TYPE:
                return createParameterType();
            case Tjs10Package.REFERENCE_DATE_TYPE:
                return createReferenceDateType();
            case Tjs10Package.REQUEST_BASE_TYPE:
                return createRequestBaseType();
            case Tjs10Package.RESOURCE_TYPE:
                return createResourceType();
            case Tjs10Package.ROWSET_TYPE:
                return createRowsetType();
            case Tjs10Package.ROWSET_TYPE1:
                return createRowsetType1();
            case Tjs10Package.ROW_TYPE:
                return createRowType();
            case Tjs10Package.ROW_TYPE1:
                return createRowType1();
            case Tjs10Package.SPATIAL_FRAMEWORKS_TYPE:
                return createSpatialFrameworksType();
            case Tjs10Package.STATUS_TYPE:
                return createStatusType();
            case Tjs10Package.STYLING_TYPE:
                return createStylingType();
            case Tjs10Package.TJS_CAPABILITIES_TYPE:
                return createTjsCapabilitiesType();
            case Tjs10Package.UNCERTAINTY_TYPE:
                return createUncertaintyType();
            case Tjs10Package.UOM_TYPE:
                return createUOMType();
            case Tjs10Package.VALUES_TYPE:
                return createValuesType();
            case Tjs10Package.VALUE_TYPE:
                return createValueType();
            case Tjs10Package.VALUE_TYPE1:
                return createValueType1();
            case Tjs10Package.VTYPE:
                return createVType();
            case Tjs10Package.WSDL_TYPE:
                return createWSDLType();
            default:
                throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Object createFromString(EDataType eDataType, String initialValue) {
        switch (eDataType.getClassifierID()) {
            case Tjs10Package.DATA_CLASS_TYPE:
                return createDataClassTypeFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_DATASETS_VALUE_TYPE:
                return createDescribeDatasetsValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_DATA_VALUE_TYPE:
                return createDescribeDataValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_VALUE_TYPE:
                return createDescribeFrameworksValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_JOIN_ABILITIES_VALUE_TYPE:
                return createDescribeJoinAbilitiesValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_KEY_VALUE_TYPE:
                return createDescribeKeyValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.GAUSSIAN_TYPE:
                return createGaussianTypeFromString(eDataType, initialValue);
            case Tjs10Package.GET_CAPABILITIES_VALUE_TYPE:
                return createGetCapabilitiesValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.GET_DATA_VALUE_TYPE:
                return createGetDataValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.JOIN_DATA_VALUE_TYPE:
                return createJoinDataValueTypeFromString(eDataType, initialValue);
            case Tjs10Package.PURPOSE_TYPE:
                return createPurposeTypeFromString(eDataType, initialValue);
            case Tjs10Package.REQUEST_SERVICE_TYPE:
                return createRequestServiceTypeFromString(eDataType, initialValue);
            case Tjs10Package.TYPE_TYPE:
                return createTypeTypeFromString(eDataType, initialValue);
            case Tjs10Package.UPDATE_TYPE:
                return createUpdateTypeFromString(eDataType, initialValue);
            case Tjs10Package.VERSION_TYPE:
                return createVersionTypeFromString(eDataType, initialValue);
            case Tjs10Package.VERSION_TYPE1:
                return createVersionType1FromString(eDataType, initialValue);
            case Tjs10Package.VERSION_TYPE2:
                return createVersionType2FromString(eDataType, initialValue);
            case Tjs10Package.ACCEPT_LANGUAGES_TYPE:
                return createAcceptLanguagesTypeFromString(eDataType, initialValue);
            case Tjs10Package.DATA_CLASS_TYPE_OBJECT:
                return createDataClassTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_DATASETS_VALUE_TYPE_OBJECT:
                return createDescribeDatasetsValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_DATA_VALUE_TYPE_OBJECT:
                return createDescribeDataValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_VALUE_TYPE_OBJECT:
                return createDescribeFrameworksValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_JOIN_ABILITIES_VALUE_TYPE_OBJECT:
                return createDescribeJoinAbilitiesValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.DESCRIBE_KEY_VALUE_TYPE_OBJECT:
                return createDescribeKeyValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.GAUSSIAN_TYPE_OBJECT:
                return createGaussianTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.GET_CAPABILITIES_VALUE_TYPE_OBJECT:
                return createGetCapabilitiesValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.GET_DATA_VALUE_TYPE_OBJECT:
                return createGetDataValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.JOIN_DATA_VALUE_TYPE_OBJECT:
                return createJoinDataValueTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.PURPOSE_TYPE_OBJECT:
                return createPurposeTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.REQUEST_SERVICE_TYPE_OBJECT:
                return createRequestServiceTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.SECTIONS_TYPE:
                return createSectionsTypeFromString(eDataType, initialValue);
            case Tjs10Package.TYPE_TYPE_OBJECT:
                return createTypeTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.UPDATE_TYPE_OBJECT:
                return createUpdateTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.VERSION_TYPE_OBJECT:
                return createVersionTypeObjectFromString(eDataType, initialValue);
            case Tjs10Package.VERSION_TYPE_OBJECT1:
                return createVersionTypeObject1FromString(eDataType, initialValue);
            case Tjs10Package.VERSION_TYPE_OBJECT2:
                return createVersionTypeObject2FromString(eDataType, initialValue);
            default:
                throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertToString(EDataType eDataType, Object instanceValue) {
        switch (eDataType.getClassifierID()) {
            case Tjs10Package.DATA_CLASS_TYPE:
                return convertDataClassTypeToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_DATASETS_VALUE_TYPE:
                return convertDescribeDatasetsValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_DATA_VALUE_TYPE:
                return convertDescribeDataValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_VALUE_TYPE:
                return convertDescribeFrameworksValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_JOIN_ABILITIES_VALUE_TYPE:
                return convertDescribeJoinAbilitiesValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_KEY_VALUE_TYPE:
                return convertDescribeKeyValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.GAUSSIAN_TYPE:
                return convertGaussianTypeToString(eDataType, instanceValue);
            case Tjs10Package.GET_CAPABILITIES_VALUE_TYPE:
                return convertGetCapabilitiesValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.GET_DATA_VALUE_TYPE:
                return convertGetDataValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.JOIN_DATA_VALUE_TYPE:
                return convertJoinDataValueTypeToString(eDataType, instanceValue);
            case Tjs10Package.PURPOSE_TYPE:
                return convertPurposeTypeToString(eDataType, instanceValue);
            case Tjs10Package.REQUEST_SERVICE_TYPE:
                return convertRequestServiceTypeToString(eDataType, instanceValue);
            case Tjs10Package.TYPE_TYPE:
                return convertTypeTypeToString(eDataType, instanceValue);
            case Tjs10Package.UPDATE_TYPE:
                return convertUpdateTypeToString(eDataType, instanceValue);
            case Tjs10Package.VERSION_TYPE:
                return convertVersionTypeToString(eDataType, instanceValue);
            case Tjs10Package.VERSION_TYPE1:
                return convertVersionType1ToString(eDataType, instanceValue);
            case Tjs10Package.VERSION_TYPE2:
                return convertVersionType2ToString(eDataType, instanceValue);
            case Tjs10Package.ACCEPT_LANGUAGES_TYPE:
                return convertAcceptLanguagesTypeToString(eDataType, instanceValue);
            case Tjs10Package.DATA_CLASS_TYPE_OBJECT:
                return convertDataClassTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_DATASETS_VALUE_TYPE_OBJECT:
                return convertDescribeDatasetsValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_DATA_VALUE_TYPE_OBJECT:
                return convertDescribeDataValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_VALUE_TYPE_OBJECT:
                return convertDescribeFrameworksValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_JOIN_ABILITIES_VALUE_TYPE_OBJECT:
                return convertDescribeJoinAbilitiesValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.DESCRIBE_KEY_VALUE_TYPE_OBJECT:
                return convertDescribeKeyValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.GAUSSIAN_TYPE_OBJECT:
                return convertGaussianTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.GET_CAPABILITIES_VALUE_TYPE_OBJECT:
                return convertGetCapabilitiesValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.GET_DATA_VALUE_TYPE_OBJECT:
                return convertGetDataValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.JOIN_DATA_VALUE_TYPE_OBJECT:
                return convertJoinDataValueTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.PURPOSE_TYPE_OBJECT:
                return convertPurposeTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.REQUEST_SERVICE_TYPE_OBJECT:
                return convertRequestServiceTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.SECTIONS_TYPE:
                return convertSectionsTypeToString(eDataType, instanceValue);
            case Tjs10Package.TYPE_TYPE_OBJECT:
                return convertTypeTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.UPDATE_TYPE_OBJECT:
                return convertUpdateTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.VERSION_TYPE_OBJECT:
                return convertVersionTypeObjectToString(eDataType, instanceValue);
            case Tjs10Package.VERSION_TYPE_OBJECT1:
                return convertVersionTypeObject1ToString(eDataType, instanceValue);
            case Tjs10Package.VERSION_TYPE_OBJECT2:
                return convertVersionTypeObject2ToString(eDataType, instanceValue);
            default:
                throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AbstractType createAbstractType() {
        AbstractTypeImpl abstractType = new AbstractTypeImpl();
        return abstractType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AcceptVersionsType createAcceptVersionsType() {
        AcceptVersionsTypeImpl acceptVersionsType = new AcceptVersionsTypeImpl();
        return acceptVersionsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AttributeDataType createAttributeDataType() {
        AttributeDataTypeImpl attributeDataType = new AttributeDataTypeImpl();
        return attributeDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public AttributesType createAttributesType() {
        AttributesTypeImpl attributesType = new AttributesTypeImpl();
        return attributesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public BoundingCoordinatesType createBoundingCoordinatesType() {
        BoundingCoordinatesTypeImpl boundingCoordinatesType = new BoundingCoordinatesTypeImpl();
        return boundingCoordinatesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ClassesType createClassesType() {
        ClassesTypeImpl classesType = new ClassesTypeImpl();
        return classesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ClassesType1 createClassesType1() {
        ClassesType1Impl classesType1 = new ClassesType1Impl();
        return classesType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ColumnsetType createColumnsetType() {
        ColumnsetTypeImpl columnsetType = new ColumnsetTypeImpl();
        return columnsetType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ColumnType createColumnType() {
        ColumnTypeImpl columnType = new ColumnTypeImpl();
        return columnType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ColumnType1 createColumnType1() {
        ColumnType1Impl columnType1 = new ColumnType1Impl();
        return columnType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ColumnType2 createColumnType2() {
        ColumnType2Impl columnType2 = new ColumnType2Impl();
        return columnType2;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public CountType createCountType() {
        CountTypeImpl countType = new CountTypeImpl();
        return countType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataDescriptionsType createDataDescriptionsType() {
        DataDescriptionsTypeImpl dataDescriptionsType = new DataDescriptionsTypeImpl();
        return dataDescriptionsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataInputsType createDataInputsType() {
        DataInputsTypeImpl dataInputsType = new DataInputsTypeImpl();
        return dataInputsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetDescriptionsType createDatasetDescriptionsType() {
        DatasetDescriptionsTypeImpl datasetDescriptionsType = new DatasetDescriptionsTypeImpl();
        return datasetDescriptionsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetType createDatasetType() {
        DatasetTypeImpl datasetType = new DatasetTypeImpl();
        return datasetType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetType1 createDatasetType1() {
        DatasetType1Impl datasetType1 = new DatasetType1Impl();
        return datasetType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetType2 createDatasetType2() {
        DatasetType2Impl datasetType2 = new DatasetType2Impl();
        return datasetType2;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DatasetType3 createDatasetType3() {
        DatasetType3Impl datasetType3 = new DatasetType3Impl();
        return datasetType3;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataRequestType createDescribeDataRequestType() {
        DescribeDataRequestTypeImpl describeDataRequestType = new DescribeDataRequestTypeImpl();
        return describeDataRequestType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsRequestType createDescribeDatasetsRequestType() {
        DescribeDatasetsRequestTypeImpl describeDatasetsRequestType = new DescribeDatasetsRequestTypeImpl();
        return describeDatasetsRequestType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsType createDescribeDatasetsType() {
        DescribeDatasetsTypeImpl describeDatasetsType = new DescribeDatasetsTypeImpl();
        return describeDatasetsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataType createDescribeDataType() {
        DescribeDataTypeImpl describeDataType = new DescribeDataTypeImpl();
        return describeDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeFrameworkKeyType createDescribeFrameworkKeyType() {
        DescribeFrameworkKeyTypeImpl describeFrameworkKeyType = new DescribeFrameworkKeyTypeImpl();
        return describeFrameworkKeyType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeFrameworksType createDescribeFrameworksType() {
        DescribeFrameworksTypeImpl describeFrameworksType = new DescribeFrameworksTypeImpl();
        return describeFrameworksType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeKeyType createDescribeKeyType() {
        DescribeKeyTypeImpl describeKeyType = new DescribeKeyTypeImpl();
        return describeKeyType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DocumentRoot createDocumentRoot() {
        DocumentRootImpl documentRoot = new DocumentRootImpl();
        return documentRoot;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ExceptionReportType createExceptionReportType() {
        ExceptionReportTypeImpl exceptionReportType = new ExceptionReportTypeImpl();
        return exceptionReportType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FailedType createFailedType() {
        FailedTypeImpl failedType = new FailedTypeImpl();
        return failedType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkDatasetDescribeDataType createFrameworkDatasetDescribeDataType() {
        FrameworkDatasetDescribeDataTypeImpl frameworkDatasetDescribeDataType = new FrameworkDatasetDescribeDataTypeImpl();
        return frameworkDatasetDescribeDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkDescriptionsType createFrameworkDescriptionsType() {
        FrameworkDescriptionsTypeImpl frameworkDescriptionsType = new FrameworkDescriptionsTypeImpl();
        return frameworkDescriptionsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyDescriptionType createFrameworkKeyDescriptionType() {
        FrameworkKeyDescriptionTypeImpl frameworkKeyDescriptionType = new FrameworkKeyDescriptionTypeImpl();
        return frameworkKeyDescriptionType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyType createFrameworkKeyType() {
        FrameworkKeyTypeImpl frameworkKeyType = new FrameworkKeyTypeImpl();
        return frameworkKeyType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkKeyType1 createFrameworkKeyType1() {
        FrameworkKeyType1Impl frameworkKeyType1 = new FrameworkKeyType1Impl();
        return frameworkKeyType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkType createFrameworkType() {
        FrameworkTypeImpl frameworkType = new FrameworkTypeImpl();
        return frameworkType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkType1 createFrameworkType1() {
        FrameworkType1Impl frameworkType1 = new FrameworkType1Impl();
        return frameworkType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkType2 createFrameworkType2() {
        FrameworkType2Impl frameworkType2 = new FrameworkType2Impl();
        return frameworkType2;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkType3 createFrameworkType3() {
        FrameworkType3Impl frameworkType3 = new FrameworkType3Impl();
        return frameworkType3;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public FrameworkType4 createFrameworkType4() {
        FrameworkType4Impl frameworkType4 = new FrameworkType4Impl();
        return frameworkType4;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GDASType createGDASType() {
        GDASTypeImpl gdasType = new GDASTypeImpl();
        return gdasType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetCapabilitiesType createGetCapabilitiesType() {
        GetCapabilitiesTypeImpl getCapabilitiesType = new GetCapabilitiesTypeImpl();
        return getCapabilitiesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataRequestType createGetDataRequestType() {
        GetDataRequestTypeImpl getDataRequestType = new GetDataRequestTypeImpl();
        return getDataRequestType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataType createGetDataType() {
        GetDataTypeImpl getDataType = new GetDataTypeImpl();
        return getDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataXMLType createGetDataXMLType() {
        GetDataXMLTypeImpl getDataXMLType = new GetDataXMLTypeImpl();
        return getDataXMLType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinAbilitiesType createJoinAbilitiesType() {
        JoinAbilitiesTypeImpl joinAbilitiesType = new JoinAbilitiesTypeImpl();
        return joinAbilitiesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinDataResponseType createJoinDataResponseType() {
        JoinDataResponseTypeImpl joinDataResponseType = new JoinDataResponseTypeImpl();
        return joinDataResponseType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinDataType createJoinDataType() {
        JoinDataTypeImpl joinDataType = new JoinDataTypeImpl();
        return joinDataType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinedOutputsType createJoinedOutputsType() {
        JoinedOutputsTypeImpl joinedOutputsType = new JoinedOutputsTypeImpl();
        return joinedOutputsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public KType createKType() {
        KTypeImpl kType = new KTypeImpl();
        return kType;
    }

    public ShortForm createShortForm() {
        ShortFormImpl shortForm = new ShortFormImpl();
        return shortForm;
    }

    public LongForm createLongForm() {
        LongFormImpl longForm = new LongFormImpl();
        return longForm;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public LanguagesType createLanguagesType() {
        LanguagesTypeImpl languagesType = new LanguagesTypeImpl();
        return languagesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MapStylingType createMapStylingType() {
        MapStylingTypeImpl mapStylingType = new MapStylingTypeImpl();
        return mapStylingType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MeasureCountExceptions createMeasureCountExceptions() {
        MeasureCountExceptionsImpl measureCountExceptions = new MeasureCountExceptionsImpl();
        return measureCountExceptions;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MeasureType createMeasureType() {
        MeasureTypeImpl measureType = new MeasureTypeImpl();
        return measureType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public MechanismType createMechanismType() {
        MechanismTypeImpl mechanismType = new MechanismTypeImpl();
        return mechanismType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NominalOrdinalExceptions createNominalOrdinalExceptions() {
        NominalOrdinalExceptionsImpl nominalOrdinalExceptions = new NominalOrdinalExceptionsImpl();
        return nominalOrdinalExceptions;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NominalType createNominalType() {
        NominalTypeImpl nominalType = new NominalTypeImpl();
        return nominalType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NullType createNullType() {
        NullTypeImpl nullType = new NullTypeImpl();
        return nullType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public NullType1 createNullType1() {
        NullType1Impl nullType1 = new NullType1Impl();
        return nullType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OrdinalType createOrdinalType() {
        OrdinalTypeImpl ordinalType = new OrdinalTypeImpl();
        return ordinalType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OutputMechanismsType createOutputMechanismsType() {
        OutputMechanismsTypeImpl outputMechanismsType = new OutputMechanismsTypeImpl();
        return outputMechanismsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OutputStylingsType createOutputStylingsType() {
        OutputStylingsTypeImpl outputStylingsType = new OutputStylingsTypeImpl();
        return outputStylingsType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OutputStylingsType1 createOutputStylingsType1() {
        OutputStylingsType1Impl outputStylingsType1 = new OutputStylingsType1Impl();
        return outputStylingsType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public OutputType createOutputType() {
        OutputTypeImpl outputType = new OutputTypeImpl();
        return outputType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ParameterType createParameterType() {
        ParameterTypeImpl parameterType = new ParameterTypeImpl();
        return parameterType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ReferenceDateType createReferenceDateType() {
        ReferenceDateTypeImpl referenceDateType = new ReferenceDateTypeImpl();
        return referenceDateType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RequestBaseType createRequestBaseType() {
        RequestBaseTypeImpl requestBaseType = new RequestBaseTypeImpl();
        return requestBaseType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ResourceType createResourceType() {
        ResourceTypeImpl resourceType = new ResourceTypeImpl();
        return resourceType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RowsetType createRowsetType() {
        RowsetTypeImpl rowsetType = new RowsetTypeImpl();
        return rowsetType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RowsetType1 createRowsetType1() {
        RowsetType1Impl rowsetType1 = new RowsetType1Impl();
        return rowsetType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RowType createRowType() {
        RowTypeImpl rowType = new RowTypeImpl();
        return rowType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RowType1 createRowType1() {
        RowType1Impl rowType1 = new RowType1Impl();
        return rowType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public SpatialFrameworksType createSpatialFrameworksType() {
        SpatialFrameworksTypeImpl spatialFrameworksType = new SpatialFrameworksTypeImpl();
        return spatialFrameworksType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public StatusType createStatusType() {
        StatusTypeImpl statusType = new StatusTypeImpl();
        return statusType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public StylingType createStylingType() {
        StylingTypeImpl stylingType = new StylingTypeImpl();
        return stylingType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public TjsCapabilitiesType createTjsCapabilitiesType() {
        TjsCapabilitiesTypeImpl tjsCapabilitiesType = new TjsCapabilitiesTypeImpl();
        return tjsCapabilitiesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UncertaintyType createUncertaintyType() {
        UncertaintyTypeImpl uncertaintyType = new UncertaintyTypeImpl();
        return uncertaintyType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UOMType createUOMType() {
        UOMTypeImpl uomType = new UOMTypeImpl();
        return uomType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ValuesType createValuesType() {
        ValuesTypeImpl valuesType = new ValuesTypeImpl();
        return valuesType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ValueType createValueType() {
        ValueTypeImpl valueType = new ValueTypeImpl();
        return valueType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public ValueType1 createValueType1() {
        ValueType1Impl valueType1 = new ValueType1Impl();
        return valueType1;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VType createVType() {
        VTypeImpl vType = new VTypeImpl();
        return vType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public WSDLType createWSDLType() {
        WSDLTypeImpl wsdlType = new WSDLTypeImpl();
        return wsdlType;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataClassType createDataClassTypeFromString(EDataType eDataType, String initialValue) {
        DataClassType result = DataClassType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDataClassTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsValueType createDescribeDatasetsValueTypeFromString(EDataType eDataType, String initialValue) {
        DescribeDatasetsValueType result = DescribeDatasetsValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeDatasetsValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataValueType createDescribeDataValueTypeFromString(EDataType eDataType, String initialValue) {
        DescribeDataValueType result = DescribeDataValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeDataValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeFrameworksValueType createDescribeFrameworksValueTypeFromString(EDataType eDataType, String initialValue) {
        DescribeFrameworksValueType result = DescribeFrameworksValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeFrameworksValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeJoinAbilitiesValueType createDescribeJoinAbilitiesValueTypeFromString(EDataType eDataType, String initialValue) {
        DescribeJoinAbilitiesValueType result = DescribeJoinAbilitiesValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeJoinAbilitiesValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeKeyValueType createDescribeKeyValueTypeFromString(EDataType eDataType, String initialValue) {
        DescribeKeyValueType result = DescribeKeyValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeKeyValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GaussianType createGaussianTypeFromString(EDataType eDataType, String initialValue) {
        GaussianType result = GaussianType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertGaussianTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetCapabilitiesValueType createGetCapabilitiesValueTypeFromString(EDataType eDataType, String initialValue) {
        GetCapabilitiesValueType result = GetCapabilitiesValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertGetCapabilitiesValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataValueType createGetDataValueTypeFromString(EDataType eDataType, String initialValue) {
        GetDataValueType result = GetDataValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertGetDataValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinDataValueType createJoinDataValueTypeFromString(EDataType eDataType, String initialValue) {
        JoinDataValueType result = JoinDataValueType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertJoinDataValueTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public PurposeType createPurposeTypeFromString(EDataType eDataType, String initialValue) {
        PurposeType result = PurposeType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertPurposeTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RequestServiceType createRequestServiceTypeFromString(EDataType eDataType, String initialValue) {
        RequestServiceType result = RequestServiceType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertRequestServiceTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public TypeType createTypeTypeFromString(EDataType eDataType, String initialValue) {
        TypeType result = TypeType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertTypeTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UpdateType createUpdateTypeFromString(EDataType eDataType, String initialValue) {
        UpdateType result = UpdateType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertUpdateTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VersionType createVersionTypeFromString(EDataType eDataType, String initialValue) {
        VersionType result = VersionType.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertVersionTypeToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VersionType1 createVersionType1FromString(EDataType eDataType, String initialValue) {
        VersionType1 result = VersionType1.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertVersionType1ToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VersionType2 createVersionType2FromString(EDataType eDataType, String initialValue) {
        VersionType2 result = VersionType2.get(initialValue);
        if (result == null)
            throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertVersionType2ToString(EDataType eDataType, Object instanceValue) {
        return instanceValue == null ? null : instanceValue.toString();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String createAcceptLanguagesTypeFromString(EDataType eDataType, String initialValue) {
        return (String) XMLTypeFactory.eINSTANCE.createFromString(XMLTypePackage.Literals.STRING, initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertAcceptLanguagesTypeToString(EDataType eDataType, Object instanceValue) {
        return XMLTypeFactory.eINSTANCE.convertToString(XMLTypePackage.Literals.STRING, instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DataClassType createDataClassTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createDataClassTypeFromString(Tjs10Package.eINSTANCE.getDataClassType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDataClassTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertDataClassTypeToString(Tjs10Package.eINSTANCE.getDataClassType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDatasetsValueType createDescribeDatasetsValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createDescribeDatasetsValueTypeFromString(Tjs10Package.eINSTANCE.getDescribeDatasetsValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeDatasetsValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertDescribeDatasetsValueTypeToString(Tjs10Package.eINSTANCE.getDescribeDatasetsValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeDataValueType createDescribeDataValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createDescribeDataValueTypeFromString(Tjs10Package.eINSTANCE.getDescribeDataValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeDataValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertDescribeDataValueTypeToString(Tjs10Package.eINSTANCE.getDescribeDataValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeFrameworksValueType createDescribeFrameworksValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createDescribeFrameworksValueTypeFromString(Tjs10Package.eINSTANCE.getDescribeFrameworksValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeFrameworksValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertDescribeFrameworksValueTypeToString(Tjs10Package.eINSTANCE.getDescribeFrameworksValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeJoinAbilitiesValueType createDescribeJoinAbilitiesValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createDescribeJoinAbilitiesValueTypeFromString(Tjs10Package.eINSTANCE.getDescribeJoinAbilitiesValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeJoinAbilitiesValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertDescribeJoinAbilitiesValueTypeToString(Tjs10Package.eINSTANCE.getDescribeJoinAbilitiesValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public DescribeKeyValueType createDescribeKeyValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createDescribeKeyValueTypeFromString(Tjs10Package.eINSTANCE.getDescribeKeyValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertDescribeKeyValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertDescribeKeyValueTypeToString(Tjs10Package.eINSTANCE.getDescribeKeyValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GaussianType createGaussianTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createGaussianTypeFromString(Tjs10Package.eINSTANCE.getGaussianType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertGaussianTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertGaussianTypeToString(Tjs10Package.eINSTANCE.getGaussianType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetCapabilitiesValueType createGetCapabilitiesValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createGetCapabilitiesValueTypeFromString(Tjs10Package.eINSTANCE.getGetCapabilitiesValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertGetCapabilitiesValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertGetCapabilitiesValueTypeToString(Tjs10Package.eINSTANCE.getGetCapabilitiesValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public GetDataValueType createGetDataValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createGetDataValueTypeFromString(Tjs10Package.eINSTANCE.getGetDataValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertGetDataValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertGetDataValueTypeToString(Tjs10Package.eINSTANCE.getGetDataValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public JoinDataValueType createJoinDataValueTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createJoinDataValueTypeFromString(Tjs10Package.eINSTANCE.getJoinDataValueType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertJoinDataValueTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertJoinDataValueTypeToString(Tjs10Package.eINSTANCE.getJoinDataValueType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public PurposeType createPurposeTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createPurposeTypeFromString(Tjs10Package.eINSTANCE.getPurposeType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertPurposeTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertPurposeTypeToString(Tjs10Package.eINSTANCE.getPurposeType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public RequestServiceType createRequestServiceTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createRequestServiceTypeFromString(Tjs10Package.eINSTANCE.getRequestServiceType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertRequestServiceTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertRequestServiceTypeToString(Tjs10Package.eINSTANCE.getRequestServiceType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String createSectionsTypeFromString(EDataType eDataType, String initialValue) {
        return (String) XMLTypeFactory.eINSTANCE.createFromString(XMLTypePackage.Literals.STRING, initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertSectionsTypeToString(EDataType eDataType, Object instanceValue) {
        return XMLTypeFactory.eINSTANCE.convertToString(XMLTypePackage.Literals.STRING, instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public TypeType createTypeTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createTypeTypeFromString(Tjs10Package.eINSTANCE.getTypeType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertTypeTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertTypeTypeToString(Tjs10Package.eINSTANCE.getTypeType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public UpdateType createUpdateTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createUpdateTypeFromString(Tjs10Package.eINSTANCE.getUpdateType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertUpdateTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertUpdateTypeToString(Tjs10Package.eINSTANCE.getUpdateType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VersionType createVersionTypeObjectFromString(EDataType eDataType, String initialValue) {
        return createVersionTypeFromString(Tjs10Package.eINSTANCE.getVersionType(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertVersionTypeObjectToString(EDataType eDataType, Object instanceValue) {
        return convertVersionTypeToString(Tjs10Package.eINSTANCE.getVersionType(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VersionType1 createVersionTypeObject1FromString(EDataType eDataType, String initialValue) {
        return createVersionType1FromString(Tjs10Package.eINSTANCE.getVersionType1(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertVersionTypeObject1ToString(EDataType eDataType, Object instanceValue) {
        return convertVersionType1ToString(Tjs10Package.eINSTANCE.getVersionType1(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public VersionType2 createVersionTypeObject2FromString(EDataType eDataType, String initialValue) {
        return createVersionType2FromString(Tjs10Package.eINSTANCE.getVersionType2(), initialValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public String convertVersionTypeObject2ToString(EDataType eDataType, Object instanceValue) {
        return convertVersionType2ToString(Tjs10Package.eINSTANCE.getVersionType2(), instanceValue);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Tjs10Package getTjs10Package() {
        return (Tjs10Package) getEPackage();
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @deprecated
     */
    public static Tjs10Package getPackage() {
        return Tjs10Package.eINSTANCE;
    }

} //Tjs10FactoryImpl
