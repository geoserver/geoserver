/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.util;

import net.opengis.tjs10.*;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xml.type.AnyType;

import java.util.List;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 *
 * @generated
 * @see net.opengis.tjs10.Tjs10Package
 */
public class Tjs10Switch {
    /**
     * The cached model package
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected static Tjs10Package modelPackage;

    /**
     * Creates an instance of the switch.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Tjs10Switch() {
        if (modelPackage == null) {
            modelPackage = Tjs10Package.eINSTANCE;
        }
    }

    /**
     * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the first non-null result returned by a <code>caseXXX</code> call.
     * @generated
     */
    public Object doSwitch(EObject theEObject) {
        return doSwitch(theEObject.eClass(), theEObject);
    }

    /**
     * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the first non-null result returned by a <code>caseXXX</code> call.
     * @generated
     */
    protected Object doSwitch(EClass theEClass, EObject theEObject) {
        if (theEClass.eContainer() == modelPackage) {
            return doSwitch(theEClass.getClassifierID(), theEObject);
        } else {
            List eSuperTypes = theEClass.getESuperTypes();
            return
                    eSuperTypes.isEmpty() ?
                            defaultCase(theEObject) :
                            doSwitch((EClass) eSuperTypes.get(0), theEObject);
        }
    }

    /**
     * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @return the first non-null result returned by a <code>caseXXX</code> call.
     * @generated
     */
    protected Object doSwitch(int classifierID, EObject theEObject) {
        switch (classifierID) {
            case Tjs10Package.ABSTRACT_TYPE: {
                AbstractType abstractType = (AbstractType) theEObject;
                Object result = caseAbstractType(abstractType);
                if (result == null) result = caseAnyType(abstractType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ACCEPT_VERSIONS_TYPE: {
                AcceptVersionsType acceptVersionsType = (AcceptVersionsType) theEObject;
                Object result = caseAcceptVersionsType(acceptVersionsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ATTRIBUTE_DATA_TYPE: {
                AttributeDataType attributeDataType = (AttributeDataType) theEObject;
                Object result = caseAttributeDataType(attributeDataType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ATTRIBUTES_TYPE: {
                AttributesType attributesType = (AttributesType) theEObject;
                Object result = caseAttributesType(attributesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.BOUNDING_COORDINATES_TYPE: {
                BoundingCoordinatesType boundingCoordinatesType = (BoundingCoordinatesType) theEObject;
                Object result = caseBoundingCoordinatesType(boundingCoordinatesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.CLASSES_TYPE: {
                ClassesType classesType = (ClassesType) theEObject;
                Object result = caseClassesType(classesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.CLASSES_TYPE1: {
                ClassesType1 classesType1 = (ClassesType1) theEObject;
                Object result = caseClassesType1(classesType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.COLUMNSET_TYPE: {
                ColumnsetType columnsetType = (ColumnsetType) theEObject;
                Object result = caseColumnsetType(columnsetType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.COLUMN_TYPE: {
                ColumnType columnType = (ColumnType) theEObject;
                Object result = caseColumnType(columnType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.COLUMN_TYPE1: {
                ColumnType1 columnType1 = (ColumnType1) theEObject;
                Object result = caseColumnType1(columnType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.COLUMN_TYPE2: {
                ColumnType2 columnType2 = (ColumnType2) theEObject;
                Object result = caseColumnType2(columnType2);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.COUNT_TYPE: {
                CountType countType = (CountType) theEObject;
                Object result = caseCountType(countType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATA_DESCRIPTIONS_TYPE: {
                DataDescriptionsType dataDescriptionsType = (DataDescriptionsType) theEObject;
                Object result = caseDataDescriptionsType(dataDescriptionsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATA_INPUTS_TYPE: {
                DataInputsType dataInputsType = (DataInputsType) theEObject;
                Object result = caseDataInputsType(dataInputsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATASET_DESCRIPTIONS_TYPE: {
                DatasetDescriptionsType datasetDescriptionsType = (DatasetDescriptionsType) theEObject;
                Object result = caseDatasetDescriptionsType(datasetDescriptionsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATASET_TYPE: {
                DatasetType datasetType = (DatasetType) theEObject;
                Object result = caseDatasetType(datasetType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATASET_TYPE1: {
                DatasetType1 datasetType1 = (DatasetType1) theEObject;
                Object result = caseDatasetType1(datasetType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATASET_TYPE2: {
                DatasetType2 datasetType2 = (DatasetType2) theEObject;
                Object result = caseDatasetType2(datasetType2);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DATASET_TYPE3: {
                DatasetType3 datasetType3 = (DatasetType3) theEObject;
                Object result = caseDatasetType3(datasetType3);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_DATA_REQUEST_TYPE: {
                DescribeDataRequestType describeDataRequestType = (DescribeDataRequestType) theEObject;
                Object result = caseDescribeDataRequestType(describeDataRequestType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_DATASETS_REQUEST_TYPE: {
                DescribeDatasetsRequestType describeDatasetsRequestType = (DescribeDatasetsRequestType) theEObject;
                Object result = caseDescribeDatasetsRequestType(describeDatasetsRequestType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_DATASETS_TYPE: {
                DescribeDatasetsType describeDatasetsType = (DescribeDatasetsType) theEObject;
                Object result = caseDescribeDatasetsType(describeDatasetsType);
                if (result == null) result = caseRequestBaseType(describeDatasetsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_DATA_TYPE: {
                DescribeDataType describeDataType = (DescribeDataType) theEObject;
                Object result = caseDescribeDataType(describeDataType);
                if (result == null) result = caseRequestBaseType(describeDataType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_FRAMEWORK_KEY_TYPE: {
                DescribeFrameworkKeyType describeFrameworkKeyType = (DescribeFrameworkKeyType) theEObject;
                Object result = caseDescribeFrameworkKeyType(describeFrameworkKeyType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_FRAMEWORKS_TYPE: {
                DescribeFrameworksType describeFrameworksType = (DescribeFrameworksType) theEObject;
                Object result = caseDescribeFrameworksType(describeFrameworksType);
                if (result == null) result = caseRequestBaseType(describeFrameworksType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DESCRIBE_KEY_TYPE: {
                DescribeKeyType describeKeyType = (DescribeKeyType) theEObject;
                Object result = caseDescribeKeyType(describeKeyType);
                if (result == null) result = caseRequestBaseType(describeKeyType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.DOCUMENT_ROOT: {
                DocumentRoot documentRoot = (DocumentRoot) theEObject;
                Object result = caseDocumentRoot(documentRoot);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.EXCEPTION_REPORT_TYPE: {
                ExceptionReportType exceptionReportType = (ExceptionReportType) theEObject;
                Object result = caseExceptionReportType(exceptionReportType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FAILED_TYPE: {
                FailedType failedType = (FailedType) theEObject;
                Object result = caseFailedType(failedType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_DATASET_DESCRIBE_DATA_TYPE: {
                FrameworkDatasetDescribeDataType frameworkDatasetDescribeDataType = (FrameworkDatasetDescribeDataType) theEObject;
                Object result = caseFrameworkDatasetDescribeDataType(frameworkDatasetDescribeDataType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_DESCRIPTIONS_TYPE: {
                FrameworkDescriptionsType frameworkDescriptionsType = (FrameworkDescriptionsType) theEObject;
                Object result = caseFrameworkDescriptionsType(frameworkDescriptionsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_KEY_DESCRIPTION_TYPE: {
                FrameworkKeyDescriptionType frameworkKeyDescriptionType = (FrameworkKeyDescriptionType) theEObject;
                Object result = caseFrameworkKeyDescriptionType(frameworkKeyDescriptionType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_KEY_TYPE: {
                FrameworkKeyType frameworkKeyType = (FrameworkKeyType) theEObject;
                Object result = caseFrameworkKeyType(frameworkKeyType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_KEY_TYPE1: {
                FrameworkKeyType1 frameworkKeyType1 = (FrameworkKeyType1) theEObject;
                Object result = caseFrameworkKeyType1(frameworkKeyType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_TYPE: {
                FrameworkType frameworkType = (FrameworkType) theEObject;
                Object result = caseFrameworkType(frameworkType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_TYPE1: {
                FrameworkType1 frameworkType1 = (FrameworkType1) theEObject;
                Object result = caseFrameworkType1(frameworkType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_TYPE2: {
                FrameworkType2 frameworkType2 = (FrameworkType2) theEObject;
                Object result = caseFrameworkType2(frameworkType2);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_TYPE3: {
                FrameworkType3 frameworkType3 = (FrameworkType3) theEObject;
                Object result = caseFrameworkType3(frameworkType3);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.FRAMEWORK_TYPE4: {
                FrameworkType4 frameworkType4 = (FrameworkType4) theEObject;
                Object result = caseFrameworkType4(frameworkType4);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.GDAS_TYPE: {
                GDASType gdasType = (GDASType) theEObject;
                Object result = caseGDASType(gdasType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.GET_CAPABILITIES_TYPE: {
                GetCapabilitiesType getCapabilitiesType = (GetCapabilitiesType) theEObject;
                Object result = caseGetCapabilitiesType(getCapabilitiesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.GET_DATA_REQUEST_TYPE: {
                GetDataRequestType getDataRequestType = (GetDataRequestType) theEObject;
                Object result = caseGetDataRequestType(getDataRequestType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.GET_DATA_TYPE: {
                GetDataType getDataType = (GetDataType) theEObject;
                Object result = caseGetDataType(getDataType);
                if (result == null) result = caseRequestBaseType(getDataType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.GET_DATA_XML_TYPE: {
                GetDataXMLType getDataXMLType = (GetDataXMLType) theEObject;
                Object result = caseGetDataXMLType(getDataXMLType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.JOIN_ABILITIES_TYPE: {
                JoinAbilitiesType joinAbilitiesType = (JoinAbilitiesType) theEObject;
                Object result = caseJoinAbilitiesType(joinAbilitiesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.JOIN_DATA_RESPONSE_TYPE: {
                JoinDataResponseType joinDataResponseType = (JoinDataResponseType) theEObject;
                Object result = caseJoinDataResponseType(joinDataResponseType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.JOIN_DATA_TYPE: {
                JoinDataType joinDataType = (JoinDataType) theEObject;
                Object result = caseJoinDataType(joinDataType);
                if (result == null) result = caseRequestBaseType(joinDataType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.JOINED_OUTPUTS_TYPE: {
                JoinedOutputsType joinedOutputsType = (JoinedOutputsType) theEObject;
                Object result = caseJoinedOutputsType(joinedOutputsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.KTYPE: {
                KType kType = (KType) theEObject;
                Object result = caseKType(kType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.LANGUAGES_TYPE: {
                LanguagesType languagesType = (LanguagesType) theEObject;
                Object result = caseLanguagesType(languagesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.MAP_STYLING_TYPE: {
                MapStylingType mapStylingType = (MapStylingType) theEObject;
                Object result = caseMapStylingType(mapStylingType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.MEASURE_COUNT_EXCEPTIONS: {
                MeasureCountExceptions measureCountExceptions = (MeasureCountExceptions) theEObject;
                Object result = caseMeasureCountExceptions(measureCountExceptions);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.MEASURE_TYPE: {
                MeasureType measureType = (MeasureType) theEObject;
                Object result = caseMeasureType(measureType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.MECHANISM_TYPE: {
                MechanismType mechanismType = (MechanismType) theEObject;
                Object result = caseMechanismType(mechanismType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.NOMINAL_ORDINAL_EXCEPTIONS: {
                NominalOrdinalExceptions nominalOrdinalExceptions = (NominalOrdinalExceptions) theEObject;
                Object result = caseNominalOrdinalExceptions(nominalOrdinalExceptions);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.NOMINAL_TYPE: {
                NominalType nominalType = (NominalType) theEObject;
                Object result = caseNominalType(nominalType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.NULL_TYPE: {
                NullType nullType = (NullType) theEObject;
                Object result = caseNullType(nullType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.NULL_TYPE1: {
                NullType1 nullType1 = (NullType1) theEObject;
                Object result = caseNullType1(nullType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ORDINAL_TYPE: {
                OrdinalType ordinalType = (OrdinalType) theEObject;
                Object result = caseOrdinalType(ordinalType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.OUTPUT_MECHANISMS_TYPE: {
                OutputMechanismsType outputMechanismsType = (OutputMechanismsType) theEObject;
                Object result = caseOutputMechanismsType(outputMechanismsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.OUTPUT_STYLINGS_TYPE: {
                OutputStylingsType outputStylingsType = (OutputStylingsType) theEObject;
                Object result = caseOutputStylingsType(outputStylingsType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.OUTPUT_STYLINGS_TYPE1: {
                OutputStylingsType1 outputStylingsType1 = (OutputStylingsType1) theEObject;
                Object result = caseOutputStylingsType1(outputStylingsType1);
                if (result == null) result = caseOutputStylingsType(outputStylingsType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.OUTPUT_TYPE: {
                OutputType outputType = (OutputType) theEObject;
                Object result = caseOutputType(outputType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.PARAMETER_TYPE: {
                ParameterType parameterType = (ParameterType) theEObject;
                Object result = caseParameterType(parameterType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.REFERENCE_DATE_TYPE: {
                ReferenceDateType referenceDateType = (ReferenceDateType) theEObject;
                Object result = caseReferenceDateType(referenceDateType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.REQUEST_BASE_TYPE: {
                RequestBaseType requestBaseType = (RequestBaseType) theEObject;
                Object result = caseRequestBaseType(requestBaseType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.RESOURCE_TYPE: {
                ResourceType resourceType = (ResourceType) theEObject;
                Object result = caseResourceType(resourceType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ROWSET_TYPE: {
                RowsetType rowsetType = (RowsetType) theEObject;
                Object result = caseRowsetType(rowsetType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ROWSET_TYPE1: {
                RowsetType1 rowsetType1 = (RowsetType1) theEObject;
                Object result = caseRowsetType1(rowsetType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ROW_TYPE: {
                RowType rowType = (RowType) theEObject;
                Object result = caseRowType(rowType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.ROW_TYPE1: {
                RowType1 rowType1 = (RowType1) theEObject;
                Object result = caseRowType1(rowType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.SPATIAL_FRAMEWORKS_TYPE: {
                SpatialFrameworksType spatialFrameworksType = (SpatialFrameworksType) theEObject;
                Object result = caseSpatialFrameworksType(spatialFrameworksType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.STATUS_TYPE: {
                StatusType statusType = (StatusType) theEObject;
                Object result = caseStatusType(statusType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.STYLING_TYPE: {
                StylingType stylingType = (StylingType) theEObject;
                Object result = caseStylingType(stylingType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.TJS_CAPABILITIES_TYPE: {
                TjsCapabilitiesType tjsCapabilitiesType = (TjsCapabilitiesType) theEObject;
                Object result = caseTjsCapabilitiesType(tjsCapabilitiesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.UNCERTAINTY_TYPE: {
                UncertaintyType uncertaintyType = (UncertaintyType) theEObject;
                Object result = caseUncertaintyType(uncertaintyType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.UOM_TYPE: {
                UOMType uomType = (UOMType) theEObject;
                Object result = caseUOMType(uomType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.VALUES_TYPE: {
                ValuesType valuesType = (ValuesType) theEObject;
                Object result = caseValuesType(valuesType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.VALUE_TYPE: {
                ValueType valueType = (ValueType) theEObject;
                Object result = caseValueType(valueType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.VALUE_TYPE1: {
                ValueType1 valueType1 = (ValueType1) theEObject;
                Object result = caseValueType1(valueType1);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.VTYPE: {
                VType vType = (VType) theEObject;
                Object result = caseVType(vType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            case Tjs10Package.WSDL_TYPE: {
                WSDLType wsdlType = (WSDLType) theEObject;
                Object result = caseWSDLType(wsdlType);
                if (result == null) result = defaultCase(theEObject);
                return result;
            }
            default:
                return defaultCase(theEObject);
        }
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Abstract Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Abstract Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseAbstractType(AbstractType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Accept Versions Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Accept Versions Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseAcceptVersionsType(AcceptVersionsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Attribute Data Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Attribute Data Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseAttributeDataType(AttributeDataType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Attributes Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Attributes Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseAttributesType(AttributesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Bounding Coordinates Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Bounding Coordinates Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseBoundingCoordinatesType(BoundingCoordinatesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Classes Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Classes Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseClassesType(ClassesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Classes Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Classes Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseClassesType1(ClassesType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Columnset Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Columnset Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseColumnsetType(ColumnsetType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Column Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Column Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseColumnType(ColumnType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Column Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Column Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseColumnType1(ColumnType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Column Type2</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Column Type2</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseColumnType2(ColumnType2 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Count Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Count Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseCountType(CountType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Data Descriptions Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Data Descriptions Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDataDescriptionsType(DataDescriptionsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Data Inputs Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Data Inputs Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDataInputsType(DataInputsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Dataset Descriptions Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Dataset Descriptions Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDatasetDescriptionsType(DatasetDescriptionsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Dataset Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Dataset Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDatasetType(DatasetType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Dataset Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Dataset Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDatasetType1(DatasetType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Dataset Type2</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Dataset Type2</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDatasetType2(DatasetType2 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Dataset Type3</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Dataset Type3</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDatasetType3(DatasetType3 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Data Request Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Data Request Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeDataRequestType(DescribeDataRequestType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Datasets Request Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Datasets Request Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeDatasetsRequestType(DescribeDatasetsRequestType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Datasets Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Datasets Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeDatasetsType(DescribeDatasetsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Data Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Data Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeDataType(DescribeDataType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Framework Key Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Framework Key Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeFrameworkKeyType(DescribeFrameworkKeyType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Frameworks Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Frameworks Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeFrameworksType(DescribeFrameworksType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Describe Key Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Describe Key Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDescribeKeyType(DescribeKeyType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Document Root</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Document Root</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseDocumentRoot(DocumentRoot object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Exception Report Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Exception Report Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseExceptionReportType(ExceptionReportType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Failed Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Failed Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFailedType(FailedType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Dataset Describe Data Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Dataset Describe Data Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkDatasetDescribeDataType(FrameworkDatasetDescribeDataType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Descriptions Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Descriptions Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkDescriptionsType(FrameworkDescriptionsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Key Description Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Key Description Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkKeyDescriptionType(FrameworkKeyDescriptionType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Key Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Key Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkKeyType(FrameworkKeyType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Key Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Key Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkKeyType1(FrameworkKeyType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkType(FrameworkType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkType1(FrameworkType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Type2</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Type2</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkType2(FrameworkType2 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Type3</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Type3</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkType3(FrameworkType3 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Framework Type4</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Framework Type4</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseFrameworkType4(FrameworkType4 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>GDAS Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>GDAS Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseGDASType(GDASType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Get Capabilities Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Get Capabilities Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseGetCapabilitiesType(GetCapabilitiesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Get Data Request Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Get Data Request Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseGetDataRequestType(GetDataRequestType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Get Data Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Get Data Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseGetDataType(GetDataType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Get Data XML Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Get Data XML Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseGetDataXMLType(GetDataXMLType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Join Abilities Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Join Abilities Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseJoinAbilitiesType(JoinAbilitiesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Join Data Response Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Join Data Response Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseJoinDataResponseType(JoinDataResponseType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Join Data Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Join Data Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseJoinDataType(JoinDataType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Joined Outputs Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Joined Outputs Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseJoinedOutputsType(JoinedOutputsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>KType</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>KType</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseKType(KType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Languages Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Languages Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseLanguagesType(LanguagesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Map Styling Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Map Styling Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseMapStylingType(MapStylingType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Measure Count Exceptions</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Measure Count Exceptions</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseMeasureCountExceptions(MeasureCountExceptions object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Measure Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Measure Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseMeasureType(MeasureType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Mechanism Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Mechanism Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseMechanismType(MechanismType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Nominal Ordinal Exceptions</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Nominal Ordinal Exceptions</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseNominalOrdinalExceptions(NominalOrdinalExceptions object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Nominal Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Nominal Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseNominalType(NominalType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Null Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Null Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseNullType(NullType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Null Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Null Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseNullType1(NullType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Ordinal Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Ordinal Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseOrdinalType(OrdinalType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Output Mechanisms Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Output Mechanisms Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseOutputMechanismsType(OutputMechanismsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Output Stylings Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Output Stylings Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseOutputStylingsType(OutputStylingsType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Output Stylings Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Output Stylings Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseOutputStylingsType1(OutputStylingsType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Output Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Output Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseOutputType(OutputType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Parameter Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Parameter Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseParameterType(ParameterType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Reference Date Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Reference Date Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseReferenceDateType(ReferenceDateType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Request Base Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Request Base Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseRequestBaseType(RequestBaseType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Resource Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Resource Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseResourceType(ResourceType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Rowset Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Rowset Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseRowsetType(RowsetType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Rowset Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Rowset Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseRowsetType1(RowsetType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Row Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Row Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseRowType(RowType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Row Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Row Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseRowType1(RowType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Spatial Frameworks Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Spatial Frameworks Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseSpatialFrameworksType(SpatialFrameworksType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Status Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Status Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseStatusType(StatusType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Styling Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Styling Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseStylingType(StylingType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Tjs Capabilities Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Tjs Capabilities Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseTjsCapabilitiesType(TjsCapabilitiesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Uncertainty Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Uncertainty Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseUncertaintyType(UncertaintyType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>UOM Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>UOM Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseUOMType(UOMType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Values Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Values Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseValuesType(ValuesType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Value Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Value Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseValueType(ValueType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Value Type1</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Value Type1</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseValueType1(ValueType1 object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>VType</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>VType</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseVType(VType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>WSDL Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>WSDL Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseWSDLType(WSDLType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>Any Type</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>Any Type</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
     */
    public Object caseAnyType(AnyType object) {
        return null;
    }

    /**
     * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
     * <!-- begin-user-doc -->
     * This implementation returns null;
     * returning a non-null result will terminate the switch, but this is the last case anyway.
     * <!-- end-user-doc -->
     *
     * @param object the target of the switch.
     * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
     * @generated
     * @see #doSwitch(org.eclipse.emf.ecore.EObject)
     */
    public Object defaultCase(EObject object) {
        return null;
    }

} //Tjs10Switch
