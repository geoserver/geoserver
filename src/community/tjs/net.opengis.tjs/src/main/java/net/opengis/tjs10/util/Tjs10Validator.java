/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.util;

import net.opengis.tjs10.*;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EObjectValidator;
import org.eclipse.emf.ecore.xml.type.util.XMLTypeUtil;
import org.eclipse.emf.ecore.xml.type.util.XMLTypeValidator;

import java.util.Map;

/**
 * <!-- begin-user-doc -->
 * The <b>Validator</b> for the model.
 * <!-- end-user-doc -->
 *
 * @generated
 * @see net.opengis.tjs10.Tjs10Package
 */
public class Tjs10Validator extends EObjectValidator {
    /**
     * The cached model package
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public static final Tjs10Validator INSTANCE = new Tjs10Validator();

    /**
     * A constant for the {@link org.eclipse.emf.common.util.Diagnostic#getSource() source} of diagnostic {@link org.eclipse.emf.common.util.Diagnostic#getCode() codes} from this package.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see org.eclipse.emf.common.util.Diagnostic#getSource()
     * @see org.eclipse.emf.common.util.Diagnostic#getCode()
     */
    public static final String DIAGNOSTIC_SOURCE = "net.opengis.tjs10";

    /**
     * A constant with a fixed name that can be used as the base value for additional hand written constants.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    private static final int GENERATED_DIAGNOSTIC_CODE_COUNT = 0;

    /**
     * A constant with a fixed name that can be used as the base value for additional hand written constants in a derived class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected static final int DIAGNOSTIC_CODE_COUNT = GENERATED_DIAGNOSTIC_CODE_COUNT;

    /**
     * The cached base package validator.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected XMLTypeValidator xmlTypeValidator;

    /**
     * Creates an instance of the switch.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Tjs10Validator() {
        super();
        xmlTypeValidator = XMLTypeValidator.INSTANCE;
    }

    /**
     * Returns the package of this validator switch.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected EPackage getEPackage() {
        return Tjs10Package.eINSTANCE;
    }

    /**
     * Calls <code>validateXXX</code> for the corresponding classifier of the model.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected boolean validate(int classifierID, Object value, DiagnosticChain diagnostics, Map context) {
        switch (classifierID) {
            case Tjs10Package.ABSTRACT_TYPE:
                return validateAbstractType((AbstractType) value, diagnostics, context);
            case Tjs10Package.ACCEPT_VERSIONS_TYPE:
                return validateAcceptVersionsType((AcceptVersionsType) value, diagnostics, context);
            case Tjs10Package.ATTRIBUTE_DATA_TYPE:
                return validateAttributeDataType((AttributeDataType) value, diagnostics, context);
            case Tjs10Package.ATTRIBUTES_TYPE:
                return validateAttributesType((AttributesType) value, diagnostics, context);
            case Tjs10Package.BOUNDING_COORDINATES_TYPE:
                return validateBoundingCoordinatesType((BoundingCoordinatesType) value, diagnostics, context);
            case Tjs10Package.CLASSES_TYPE:
                return validateClassesType((ClassesType) value, diagnostics, context);
            case Tjs10Package.CLASSES_TYPE1:
                return validateClassesType1((ClassesType1) value, diagnostics, context);
            case Tjs10Package.COLUMNSET_TYPE:
                return validateColumnsetType((ColumnsetType) value, diagnostics, context);
            case Tjs10Package.COLUMN_TYPE:
                return validateColumnType((ColumnType) value, diagnostics, context);
            case Tjs10Package.COLUMN_TYPE1:
                return validateColumnType1((ColumnType1) value, diagnostics, context);
            case Tjs10Package.COLUMN_TYPE2:
                return validateColumnType2((ColumnType2) value, diagnostics, context);
            case Tjs10Package.COUNT_TYPE:
                return validateCountType((CountType) value, diagnostics, context);
            case Tjs10Package.DATA_DESCRIPTIONS_TYPE:
                return validateDataDescriptionsType((DataDescriptionsType) value, diagnostics, context);
            case Tjs10Package.DATA_INPUTS_TYPE:
                return validateDataInputsType((DataInputsType) value, diagnostics, context);
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE:
                return validateDatasetDescriptionsType((DatasetDescriptionsType) value, diagnostics, context);
            case Tjs10Package.DATASET_TYPE:
                return validateDatasetType((DatasetType) value, diagnostics, context);
            case Tjs10Package.DATASET_TYPE1:
                return validateDatasetType1((DatasetType1) value, diagnostics, context);
            case Tjs10Package.DATASET_TYPE2:
                return validateDatasetType2((DatasetType2) value, diagnostics, context);
            case Tjs10Package.DATASET_TYPE3:
                return validateDatasetType3((DatasetType3) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATA_REQUEST_TYPE:
                return validateDescribeDataRequestType((DescribeDataRequestType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATASETS_REQUEST_TYPE:
                return validateDescribeDatasetsRequestType((DescribeDatasetsRequestType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATASETS_TYPE:
                return validateDescribeDatasetsType((DescribeDatasetsType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATA_TYPE:
                return validateDescribeDataType((DescribeDataType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE:
                return validateDescribeFrameworkKeyType((DescribeFrameworkKeyType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_TYPE:
                return validateDescribeFrameworksType((DescribeFrameworksType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_KEY_TYPE:
                return validateDescribeKeyType((DescribeKeyType) value, diagnostics, context);
            case Tjs10Package.DOCUMENT_ROOT:
                return validateDocumentRoot((DocumentRoot) value, diagnostics, context);
            case Tjs10Package.EXCEPTION_REPORT_TYPE:
                return validateExceptionReportType((ExceptionReportType) value, diagnostics, context);
            case Tjs10Package.FAILED_TYPE:
                return validateFailedType((FailedType) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE:
                return validateFrameworkDatasetDescribeDataType((FrameworkDatasetDescribeDataType) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_DESCRIPTIONS_TYPE:
                return validateFrameworkDescriptionsType((FrameworkDescriptionsType) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_KEY_DESCRIPTION_TYPE:
                return validateFrameworkKeyDescriptionType((FrameworkKeyDescriptionType) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_KEY_TYPE:
                return validateFrameworkKeyType((FrameworkKeyType) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_KEY_TYPE1:
                return validateFrameworkKeyType1((FrameworkKeyType1) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_TYPE:
                return validateFrameworkType((FrameworkType) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_TYPE1:
                return validateFrameworkType1((FrameworkType1) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_TYPE2:
                return validateFrameworkType2((FrameworkType2) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_TYPE3:
                return validateFrameworkType3((FrameworkType3) value, diagnostics, context);
            case Tjs10Package.FRAMEWORK_TYPE4:
                return validateFrameworkType4((FrameworkType4) value, diagnostics, context);
            case Tjs10Package.GDAS_TYPE:
                return validateGDASType((GDASType) value, diagnostics, context);
            case Tjs10Package.GET_CAPABILITIES_TYPE:
                return validateGetCapabilitiesType((GetCapabilitiesType) value, diagnostics, context);
            case Tjs10Package.GET_DATA_REQUEST_TYPE:
                return validateGetDataRequestType((GetDataRequestType) value, diagnostics, context);
            case Tjs10Package.GET_DATA_TYPE:
                return validateGetDataType((GetDataType) value, diagnostics, context);
            case Tjs10Package.GET_DATA_XML_TYPE:
                return validateGetDataXMLType((GetDataXMLType) value, diagnostics, context);
            case Tjs10Package.JOIN_ABILITIES_TYPE:
                return validateJoinAbilitiesType((JoinAbilitiesType) value, diagnostics, context);
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE:
                return validateJoinDataResponseType((JoinDataResponseType) value, diagnostics, context);
            case Tjs10Package.JOIN_DATA_TYPE:
                return validateJoinDataType((JoinDataType) value, diagnostics, context);
            case Tjs10Package.JOINED_OUTPUTS_TYPE:
                return validateJoinedOutputsType((JoinedOutputsType) value, diagnostics, context);
            case Tjs10Package.KTYPE:
                return validateKType((KType) value, diagnostics, context);
            case Tjs10Package.LANGUAGES_TYPE:
                return validateLanguagesType((LanguagesType) value, diagnostics, context);
            case Tjs10Package.MAP_STYLING_TYPE:
                return validateMapStylingType((MapStylingType) value, diagnostics, context);
            case Tjs10Package.MEASURE_COUNT_EXCEPTIONS:
                return validateMeasureCountExceptions((MeasureCountExceptions) value, diagnostics, context);
            case Tjs10Package.MEASURE_TYPE:
                return validateMeasureType((MeasureType) value, diagnostics, context);
            case Tjs10Package.MECHANISM_TYPE:
                return validateMechanismType((MechanismType) value, diagnostics, context);
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS:
                return validateNominalOrdinalExceptions((NominalOrdinalExceptions) value, diagnostics, context);
            case Tjs10Package.NOMINAL_TYPE:
                return validateNominalType((NominalType) value, diagnostics, context);
            case Tjs10Package.NULL_TYPE:
                return validateNullType((NullType) value, diagnostics, context);
            case Tjs10Package.NULL_TYPE1:
                return validateNullType1((NullType1) value, diagnostics, context);
            case Tjs10Package.ORDINAL_TYPE:
                return validateOrdinalType((OrdinalType) value, diagnostics, context);
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE:
                return validateOutputMechanismsType((OutputMechanismsType) value, diagnostics, context);
            case Tjs10Package.OUTPUT_STYLINGS_TYPE:
                return validateOutputStylingsType((OutputStylingsType) value, diagnostics, context);
            case Tjs10Package.OUTPUT_STYLINGS_TYPE1:
                return validateOutputStylingsType1((OutputStylingsType1) value, diagnostics, context);
            case Tjs10Package.OUTPUT_TYPE:
                return validateOutputType((OutputType) value, diagnostics, context);
            case Tjs10Package.PARAMETER_TYPE:
                return validateParameterType((ParameterType) value, diagnostics, context);
            case Tjs10Package.REFERENCE_DATE_TYPE:
                return validateReferenceDateType((ReferenceDateType) value, diagnostics, context);
            case Tjs10Package.REQUEST_BASE_TYPE:
                return validateRequestBaseType((RequestBaseType) value, diagnostics, context);
            case Tjs10Package.RESOURCE_TYPE:
                return validateResourceType((ResourceType) value, diagnostics, context);
            case Tjs10Package.ROWSET_TYPE:
                return validateRowsetType((RowsetType) value, diagnostics, context);
            case Tjs10Package.ROWSET_TYPE1:
                return validateRowsetType1((RowsetType1) value, diagnostics, context);
            case Tjs10Package.ROW_TYPE:
                return validateRowType((RowType) value, diagnostics, context);
            case Tjs10Package.ROW_TYPE1:
                return validateRowType1((RowType1) value, diagnostics, context);
            case Tjs10Package.SPATIAL_FRAMEWORKS_TYPE:
                return validateSpatialFrameworksType((SpatialFrameworksType) value, diagnostics, context);
            case Tjs10Package.STATUS_TYPE:
                return validateStatusType((StatusType) value, diagnostics, context);
            case Tjs10Package.STYLING_TYPE:
                return validateStylingType((StylingType) value, diagnostics, context);
            case Tjs10Package.TJS_CAPABILITIES_TYPE:
                return validateTjsCapabilitiesType((TjsCapabilitiesType) value, diagnostics, context);
            case Tjs10Package.UNCERTAINTY_TYPE:
                return validateUncertaintyType((UncertaintyType) value, diagnostics, context);
            case Tjs10Package.UOM_TYPE:
                return validateUOMType((UOMType) value, diagnostics, context);
            case Tjs10Package.VALUES_TYPE:
                return validateValuesType((ValuesType) value, diagnostics, context);
            case Tjs10Package.VALUE_TYPE:
                return validateValueType((ValueType) value, diagnostics, context);
            case Tjs10Package.VALUE_TYPE1:
                return validateValueType1((ValueType1) value, diagnostics, context);
            case Tjs10Package.VTYPE:
                return validateVType((VType) value, diagnostics, context);
            case Tjs10Package.WSDL_TYPE:
                return validateWSDLType((WSDLType) value, diagnostics, context);
            case Tjs10Package.DATA_CLASS_TYPE:
                return validateDataClassType((DataClassType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATASETS_VALUE_TYPE:
                return validateDescribeDatasetsValueType((DescribeDatasetsValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATA_VALUE_TYPE:
                return validateDescribeDataValueType((DescribeDataValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_VALUE_TYPE:
                return validateDescribeFrameworksValueType((DescribeFrameworksValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_JOIN_ABILITIES_VALUE_TYPE:
                return validateDescribeJoinAbilitiesValueType((DescribeJoinAbilitiesValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_KEY_VALUE_TYPE:
                return validateDescribeKeyValueType((DescribeKeyValueType) value, diagnostics, context);
            case Tjs10Package.GAUSSIAN_TYPE:
                return validateGaussianType((GaussianType) value, diagnostics, context);
            case Tjs10Package.GET_CAPABILITIES_VALUE_TYPE:
                return validateGetCapabilitiesValueType((GetCapabilitiesValueType) value, diagnostics, context);
            case Tjs10Package.GET_DATA_VALUE_TYPE:
                return validateGetDataValueType((GetDataValueType) value, diagnostics, context);
            case Tjs10Package.JOIN_DATA_VALUE_TYPE:
                return validateJoinDataValueType((JoinDataValueType) value, diagnostics, context);
            case Tjs10Package.PURPOSE_TYPE:
                return validatePurposeType((PurposeType) value, diagnostics, context);
            case Tjs10Package.REQUEST_SERVICE_TYPE:
                return validateRequestServiceType((RequestServiceType) value, diagnostics, context);
            case Tjs10Package.TYPE_TYPE:
                return validateTypeType((TypeType) value, diagnostics, context);
            case Tjs10Package.UPDATE_TYPE:
                return validateUpdateType((UpdateType) value, diagnostics, context);
            case Tjs10Package.VERSION_TYPE:
                return validateVersionType((VersionType) value, diagnostics, context);
            case Tjs10Package.VERSION_TYPE1:
                return validateVersionType1((VersionType1) value, diagnostics, context);
            case Tjs10Package.VERSION_TYPE2:
                return validateVersionType2((VersionType2) value, diagnostics, context);
            case Tjs10Package.ACCEPT_LANGUAGES_TYPE:
                return validateAcceptLanguagesType((String) value, diagnostics, context);
            case Tjs10Package.DATA_CLASS_TYPE_OBJECT:
                return validateDataClassTypeObject((DataClassType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATASETS_VALUE_TYPE_OBJECT:
                return validateDescribeDatasetsValueTypeObject((DescribeDatasetsValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_DATA_VALUE_TYPE_OBJECT:
                return validateDescribeDataValueTypeObject((DescribeDataValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_FRAMEWORKS_VALUE_TYPE_OBJECT:
                return validateDescribeFrameworksValueTypeObject((DescribeFrameworksValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_JOIN_ABILITIES_VALUE_TYPE_OBJECT:
                return validateDescribeJoinAbilitiesValueTypeObject((DescribeJoinAbilitiesValueType) value, diagnostics, context);
            case Tjs10Package.DESCRIBE_KEY_VALUE_TYPE_OBJECT:
                return validateDescribeKeyValueTypeObject((DescribeKeyValueType) value, diagnostics, context);
            case Tjs10Package.GAUSSIAN_TYPE_OBJECT:
                return validateGaussianTypeObject((GaussianType) value, diagnostics, context);
            case Tjs10Package.GET_CAPABILITIES_VALUE_TYPE_OBJECT:
                return validateGetCapabilitiesValueTypeObject((GetCapabilitiesValueType) value, diagnostics, context);
            case Tjs10Package.GET_DATA_VALUE_TYPE_OBJECT:
                return validateGetDataValueTypeObject((GetDataValueType) value, diagnostics, context);
            case Tjs10Package.JOIN_DATA_VALUE_TYPE_OBJECT:
                return validateJoinDataValueTypeObject((JoinDataValueType) value, diagnostics, context);
            case Tjs10Package.PURPOSE_TYPE_OBJECT:
                return validatePurposeTypeObject((PurposeType) value, diagnostics, context);
            case Tjs10Package.REQUEST_SERVICE_TYPE_OBJECT:
                return validateRequestServiceTypeObject((RequestServiceType) value, diagnostics, context);
            case Tjs10Package.SECTIONS_TYPE:
                return validateSectionsType((String) value, diagnostics, context);
            case Tjs10Package.TYPE_TYPE_OBJECT:
                return validateTypeTypeObject((TypeType) value, diagnostics, context);
            case Tjs10Package.UPDATE_TYPE_OBJECT:
                return validateUpdateTypeObject((UpdateType) value, diagnostics, context);
            case Tjs10Package.VERSION_TYPE_OBJECT:
                return validateVersionTypeObject((VersionType) value, diagnostics, context);
            case Tjs10Package.VERSION_TYPE_OBJECT1:
                return validateVersionTypeObject1((VersionType1) value, diagnostics, context);
            case Tjs10Package.VERSION_TYPE_OBJECT2:
                return validateVersionTypeObject2((VersionType2) value, diagnostics, context);
            default:
                return true;
        }
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateAbstractType(AbstractType abstractType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(abstractType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateAcceptVersionsType(AcceptVersionsType acceptVersionsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(acceptVersionsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateAttributeDataType(AttributeDataType attributeDataType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(attributeDataType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateAttributesType(AttributesType attributesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(attributesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateBoundingCoordinatesType(BoundingCoordinatesType boundingCoordinatesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(boundingCoordinatesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateClassesType(ClassesType classesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(classesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateClassesType1(ClassesType1 classesType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(classesType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateColumnsetType(ColumnsetType columnsetType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(columnsetType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateColumnType(ColumnType columnType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(columnType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateColumnType1(ColumnType1 columnType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(columnType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateColumnType2(ColumnType2 columnType2, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(columnType2, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateCountType(CountType countType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(countType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDataDescriptionsType(DataDescriptionsType dataDescriptionsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(dataDescriptionsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDataInputsType(DataInputsType dataInputsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(dataInputsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDatasetDescriptionsType(DatasetDescriptionsType datasetDescriptionsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(datasetDescriptionsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDatasetType(DatasetType datasetType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(datasetType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDatasetType1(DatasetType1 datasetType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(datasetType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDatasetType2(DatasetType2 datasetType2, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(datasetType2, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDatasetType3(DatasetType3 datasetType3, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(datasetType3, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDataRequestType(DescribeDataRequestType describeDataRequestType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeDataRequestType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDatasetsRequestType(DescribeDatasetsRequestType describeDatasetsRequestType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeDatasetsRequestType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDatasetsType(DescribeDatasetsType describeDatasetsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeDatasetsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDataType(DescribeDataType describeDataType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeDataType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeFrameworkKeyType(DescribeFrameworkKeyType describeFrameworkKeyType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeFrameworkKeyType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeFrameworksType(DescribeFrameworksType describeFrameworksType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeFrameworksType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeKeyType(DescribeKeyType describeKeyType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(describeKeyType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDocumentRoot(DocumentRoot documentRoot, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(documentRoot, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateExceptionReportType(ExceptionReportType exceptionReportType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(exceptionReportType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFailedType(FailedType failedType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(failedType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkDatasetDescribeDataType(FrameworkDatasetDescribeDataType frameworkDatasetDescribeDataType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkDatasetDescribeDataType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkDescriptionsType(FrameworkDescriptionsType frameworkDescriptionsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkDescriptionsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkKeyDescriptionType(FrameworkKeyDescriptionType frameworkKeyDescriptionType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkKeyDescriptionType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkKeyType(FrameworkKeyType frameworkKeyType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkKeyType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkKeyType1(FrameworkKeyType1 frameworkKeyType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkKeyType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkType(FrameworkType frameworkType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkType1(FrameworkType1 frameworkType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkType2(FrameworkType2 frameworkType2, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkType2, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkType3(FrameworkType3 frameworkType3, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkType3, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateFrameworkType4(FrameworkType4 frameworkType4, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(frameworkType4, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGDASType(GDASType gdasType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(gdasType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetCapabilitiesType(GetCapabilitiesType getCapabilitiesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(getCapabilitiesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetDataRequestType(GetDataRequestType getDataRequestType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(getDataRequestType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetDataType(GetDataType getDataType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(getDataType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetDataXMLType(GetDataXMLType getDataXMLType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(getDataXMLType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateJoinAbilitiesType(JoinAbilitiesType joinAbilitiesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(joinAbilitiesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateJoinDataResponseType(JoinDataResponseType joinDataResponseType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(joinDataResponseType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateJoinDataType(JoinDataType joinDataType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(joinDataType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateJoinedOutputsType(JoinedOutputsType joinedOutputsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(joinedOutputsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateKType(KType kType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(kType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateLanguagesType(LanguagesType languagesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(languagesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateMapStylingType(MapStylingType mapStylingType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(mapStylingType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateMeasureCountExceptions(MeasureCountExceptions measureCountExceptions, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(measureCountExceptions, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateMeasureType(MeasureType measureType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(measureType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateMechanismType(MechanismType mechanismType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(mechanismType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateNominalOrdinalExceptions(NominalOrdinalExceptions nominalOrdinalExceptions, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(nominalOrdinalExceptions, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateNominalType(NominalType nominalType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(nominalType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateNullType(NullType nullType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(nullType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateNullType1(NullType1 nullType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(nullType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateOrdinalType(OrdinalType ordinalType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(ordinalType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateOutputMechanismsType(OutputMechanismsType outputMechanismsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(outputMechanismsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateOutputStylingsType(OutputStylingsType outputStylingsType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(outputStylingsType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateOutputStylingsType1(OutputStylingsType1 outputStylingsType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(outputStylingsType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateOutputType(OutputType outputType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(outputType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateParameterType(ParameterType parameterType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(parameterType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateReferenceDateType(ReferenceDateType referenceDateType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(referenceDateType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRequestBaseType(RequestBaseType requestBaseType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(requestBaseType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateResourceType(ResourceType resourceType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(resourceType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRowsetType(RowsetType rowsetType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(rowsetType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRowsetType1(RowsetType1 rowsetType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(rowsetType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRowType(RowType rowType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(rowType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRowType1(RowType1 rowType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(rowType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateSpatialFrameworksType(SpatialFrameworksType spatialFrameworksType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(spatialFrameworksType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateStatusType(StatusType statusType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(statusType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateStylingType(StylingType stylingType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(stylingType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateTjsCapabilitiesType(TjsCapabilitiesType tjsCapabilitiesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(tjsCapabilitiesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateUncertaintyType(UncertaintyType uncertaintyType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(uncertaintyType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateUOMType(UOMType uomType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(uomType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateValuesType(ValuesType valuesType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(valuesType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateValueType(ValueType valueType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(valueType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateValueType1(ValueType1 valueType1, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(valueType1, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVType(VType vType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(vType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateWSDLType(WSDLType wsdlType, DiagnosticChain diagnostics, Map context) {
        return validate_EveryDefaultConstraint(wsdlType, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDataClassType(DataClassType dataClassType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDatasetsValueType(DescribeDatasetsValueType describeDatasetsValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDataValueType(DescribeDataValueType describeDataValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeFrameworksValueType(DescribeFrameworksValueType describeFrameworksValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeJoinAbilitiesValueType(DescribeJoinAbilitiesValueType describeJoinAbilitiesValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeKeyValueType(DescribeKeyValueType describeKeyValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGaussianType(GaussianType gaussianType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetCapabilitiesValueType(GetCapabilitiesValueType getCapabilitiesValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetDataValueType(GetDataValueType getDataValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateJoinDataValueType(JoinDataValueType joinDataValueType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validatePurposeType(PurposeType purposeType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRequestServiceType(RequestServiceType requestServiceType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateTypeType(TypeType typeType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateUpdateType(UpdateType updateType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVersionType(VersionType versionType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVersionType1(VersionType1 versionType1, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVersionType2(VersionType2 versionType2, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateAcceptLanguagesType(String acceptLanguagesType, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDataClassTypeObject(DataClassType dataClassTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDatasetsValueTypeObject(DescribeDatasetsValueType describeDatasetsValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeDataValueTypeObject(DescribeDataValueType describeDataValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeFrameworksValueTypeObject(DescribeFrameworksValueType describeFrameworksValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeJoinAbilitiesValueTypeObject(DescribeJoinAbilitiesValueType describeJoinAbilitiesValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateDescribeKeyValueTypeObject(DescribeKeyValueType describeKeyValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGaussianTypeObject(GaussianType gaussianTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetCapabilitiesValueTypeObject(GetCapabilitiesValueType getCapabilitiesValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateGetDataValueTypeObject(GetDataValueType getDataValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateJoinDataValueTypeObject(JoinDataValueType joinDataValueTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validatePurposeTypeObject(PurposeType purposeTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateRequestServiceTypeObject(RequestServiceType requestServiceTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateSectionsType(String sectionsType, DiagnosticChain diagnostics, Map context) {
        boolean result = validateSectionsType_Pattern(sectionsType, diagnostics, context);
        return result;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     * @see #validateSectionsType_Pattern
     */
    public static final PatternMatcher[][] SECTIONS_TYPE__PATTERN__VALUES =
            new PatternMatcher[][]{
                                          new PatternMatcher[]{
                                                                      XMLTypeUtil.createPatternMatcher("(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes)(,(ServiceIdentification|ServiceProvider|OperationsMetadata|Contents|Themes))*")
                                          }
            };

    /**
     * Validates the Pattern constraint of '<em>Sections Type</em>'.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateSectionsType_Pattern(String sectionsType, DiagnosticChain diagnostics, Map context) {
        return validatePattern(Tjs10Package.eINSTANCE.getSectionsType(), sectionsType, SECTIONS_TYPE__PATTERN__VALUES, diagnostics, context);
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateTypeTypeObject(TypeType typeTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateUpdateTypeObject(UpdateType updateTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVersionTypeObject(VersionType versionTypeObject, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVersionTypeObject1(VersionType1 versionTypeObject1, DiagnosticChain diagnostics, Map context) {
        return true;
    }

    /**
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public boolean validateVersionTypeObject2(VersionType2 versionTypeObject2, DiagnosticChain diagnostics, Map context) {
        return true;
    }

} //Tjs10Validator
