/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.opengis.tjs10.util;

import net.opengis.tjs10.*;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xml.type.AnyType;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 *
 * @generated
 * @see net.opengis.tjs10.Tjs10Package
 */
public class Tjs10AdapterFactory extends AdapterFactoryImpl {
    /**
     * The cached model package.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected static Tjs10Package modelPackage;

    /**
     * Creates an instance of the adapter factory.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    public Tjs10AdapterFactory() {
        if (modelPackage == null) {
            modelPackage = Tjs10Package.eINSTANCE;
        }
    }

    /**
     * Returns whether this factory is applicable for the type of the object.
     * <!-- begin-user-doc -->
     * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
     * <!-- end-user-doc -->
     *
     * @return whether this factory is applicable for the type of the object.
     * @generated
     */
    public boolean isFactoryForType(Object object) {
        if (object == modelPackage) {
            return true;
        }
        if (object instanceof EObject) {
            return ((EObject) object).eClass().getEPackage() == modelPackage;
        }
        return false;
    }

    /**
     * The switch that delegates to the <code>createXXX</code> methods.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @generated
     */
    protected Tjs10Switch modelSwitch =
            new Tjs10Switch() {
                public Object caseAbstractType(AbstractType object) {
                    return createAbstractTypeAdapter();
                }

                public Object caseAcceptVersionsType(AcceptVersionsType object) {
                    return createAcceptVersionsTypeAdapter();
                }

                public Object caseAttributeDataType(AttributeDataType object) {
                    return createAttributeDataTypeAdapter();
                }

                public Object caseAttributesType(AttributesType object) {
                    return createAttributesTypeAdapter();
                }

                public Object caseBoundingCoordinatesType(BoundingCoordinatesType object) {
                    return createBoundingCoordinatesTypeAdapter();
                }

                public Object caseClassesType(ClassesType object) {
                    return createClassesTypeAdapter();
                }

                public Object caseClassesType1(ClassesType1 object) {
                    return createClassesType1Adapter();
                }

                public Object caseColumnsetType(ColumnsetType object) {
                    return createColumnsetTypeAdapter();
                }

                public Object caseColumnType(ColumnType object) {
                    return createColumnTypeAdapter();
                }

                public Object caseColumnType1(ColumnType1 object) {
                    return createColumnType1Adapter();
                }

                public Object caseColumnType2(ColumnType2 object) {
                    return createColumnType2Adapter();
                }

                public Object caseCountType(CountType object) {
                    return createCountTypeAdapter();
                }

                public Object caseDataDescriptionsType(DataDescriptionsType object) {
                    return createDataDescriptionsTypeAdapter();
                }

                public Object caseDataInputsType(DataInputsType object) {
                    return createDataInputsTypeAdapter();
                }

                public Object caseDatasetDescriptionsType(DatasetDescriptionsType object) {
                    return createDatasetDescriptionsTypeAdapter();
                }

                public Object caseDatasetType(DatasetType object) {
                    return createDatasetTypeAdapter();
                }

                public Object caseDatasetType1(DatasetType1 object) {
                    return createDatasetType1Adapter();
                }

                public Object caseDatasetType2(DatasetType2 object) {
                    return createDatasetType2Adapter();
                }

                public Object caseDatasetType3(DatasetType3 object) {
                    return createDatasetType3Adapter();
                }

                public Object caseDescribeDataRequestType(DescribeDataRequestType object) {
                    return createDescribeDataRequestTypeAdapter();
                }

                public Object caseDescribeDatasetsRequestType(DescribeDatasetsRequestType object) {
                    return createDescribeDatasetsRequestTypeAdapter();
                }

                public Object caseDescribeDatasetsType(DescribeDatasetsType object) {
                    return createDescribeDatasetsTypeAdapter();
                }

                public Object caseDescribeDataType(DescribeDataType object) {
                    return createDescribeDataTypeAdapter();
                }

                public Object caseDescribeFrameworkKeyType(DescribeFrameworkKeyType object) {
                    return createDescribeFrameworkKeyTypeAdapter();
                }

                public Object caseDescribeFrameworksType(DescribeFrameworksType object) {
                    return createDescribeFrameworksTypeAdapter();
                }

                public Object caseDescribeKeyType(DescribeKeyType object) {
                    return createDescribeKeyTypeAdapter();
                }

                public Object caseDocumentRoot(DocumentRoot object) {
                    return createDocumentRootAdapter();
                }

                public Object caseExceptionReportType(ExceptionReportType object) {
                    return createExceptionReportTypeAdapter();
                }

                public Object caseFailedType(FailedType object) {
                    return createFailedTypeAdapter();
                }

                public Object caseFrameworkDatasetDescribeDataType(FrameworkDatasetDescribeDataType object) {
                    return createFrameworkDatasetDescribeDataTypeAdapter();
                }

                public Object caseFrameworkDescriptionsType(FrameworkDescriptionsType object) {
                    return createFrameworkDescriptionsTypeAdapter();
                }

                public Object caseFrameworkKeyDescriptionType(FrameworkKeyDescriptionType object) {
                    return createFrameworkKeyDescriptionTypeAdapter();
                }

                public Object caseFrameworkKeyType(FrameworkKeyType object) {
                    return createFrameworkKeyTypeAdapter();
                }

                public Object caseFrameworkKeyType1(FrameworkKeyType1 object) {
                    return createFrameworkKeyType1Adapter();
                }

                public Object caseFrameworkType(FrameworkType object) {
                    return createFrameworkTypeAdapter();
                }

                public Object caseFrameworkType1(FrameworkType1 object) {
                    return createFrameworkType1Adapter();
                }

                public Object caseFrameworkType2(FrameworkType2 object) {
                    return createFrameworkType2Adapter();
                }

                public Object caseFrameworkType3(FrameworkType3 object) {
                    return createFrameworkType3Adapter();
                }

                public Object caseFrameworkType4(FrameworkType4 object) {
                    return createFrameworkType4Adapter();
                }

                public Object caseGDASType(GDASType object) {
                    return createGDASTypeAdapter();
                }

                public Object caseGetCapabilitiesType(GetCapabilitiesType object) {
                    return createGetCapabilitiesTypeAdapter();
                }

                public Object caseGetDataRequestType(GetDataRequestType object) {
                    return createGetDataRequestTypeAdapter();
                }

                public Object caseGetDataType(GetDataType object) {
                    return createGetDataTypeAdapter();
                }

                public Object caseGetDataXMLType(GetDataXMLType object) {
                    return createGetDataXMLTypeAdapter();
                }

                public Object caseJoinAbilitiesType(JoinAbilitiesType object) {
                    return createJoinAbilitiesTypeAdapter();
                }

                public Object caseJoinDataResponseType(JoinDataResponseType object) {
                    return createJoinDataResponseTypeAdapter();
                }

                public Object caseJoinDataType(JoinDataType object) {
                    return createJoinDataTypeAdapter();
                }

                public Object caseJoinedOutputsType(JoinedOutputsType object) {
                    return createJoinedOutputsTypeAdapter();
                }

                public Object caseKType(KType object) {
                    return createKTypeAdapter();
                }

                public Object caseLanguagesType(LanguagesType object) {
                    return createLanguagesTypeAdapter();
                }

                public Object caseMapStylingType(MapStylingType object) {
                    return createMapStylingTypeAdapter();
                }

                public Object caseMeasureCountExceptions(MeasureCountExceptions object) {
                    return createMeasureCountExceptionsAdapter();
                }

                public Object caseMeasureType(MeasureType object) {
                    return createMeasureTypeAdapter();
                }

                public Object caseMechanismType(MechanismType object) {
                    return createMechanismTypeAdapter();
                }

                public Object caseNominalOrdinalExceptions(NominalOrdinalExceptions object) {
                    return createNominalOrdinalExceptionsAdapter();
                }

                public Object caseNominalType(NominalType object) {
                    return createNominalTypeAdapter();
                }

                public Object caseNullType(NullType object) {
                    return createNullTypeAdapter();
                }

                public Object caseNullType1(NullType1 object) {
                    return createNullType1Adapter();
                }

                public Object caseOrdinalType(OrdinalType object) {
                    return createOrdinalTypeAdapter();
                }

                public Object caseOutputMechanismsType(OutputMechanismsType object) {
                    return createOutputMechanismsTypeAdapter();
                }

                public Object caseOutputStylingsType(OutputStylingsType object) {
                    return createOutputStylingsTypeAdapter();
                }

                public Object caseOutputStylingsType1(OutputStylingsType1 object) {
                    return createOutputStylingsType1Adapter();
                }

                public Object caseOutputType(OutputType object) {
                    return createOutputTypeAdapter();
                }

                public Object caseParameterType(ParameterType object) {
                    return createParameterTypeAdapter();
                }

                public Object caseReferenceDateType(ReferenceDateType object) {
                    return createReferenceDateTypeAdapter();
                }

                public Object caseRequestBaseType(RequestBaseType object) {
                    return createRequestBaseTypeAdapter();
                }

                public Object caseResourceType(ResourceType object) {
                    return createResourceTypeAdapter();
                }

                public Object caseRowsetType(RowsetType object) {
                    return createRowsetTypeAdapter();
                }

                public Object caseRowsetType1(RowsetType1 object) {
                    return createRowsetType1Adapter();
                }

                public Object caseRowType(RowType object) {
                    return createRowTypeAdapter();
                }

                public Object caseRowType1(RowType1 object) {
                    return createRowType1Adapter();
                }

                public Object caseSpatialFrameworksType(SpatialFrameworksType object) {
                    return createSpatialFrameworksTypeAdapter();
                }

                public Object caseStatusType(StatusType object) {
                    return createStatusTypeAdapter();
                }

                public Object caseStylingType(StylingType object) {
                    return createStylingTypeAdapter();
                }

                public Object caseTjsCapabilitiesType(TjsCapabilitiesType object) {
                    return createTjsCapabilitiesTypeAdapter();
                }

                public Object caseUncertaintyType(UncertaintyType object) {
                    return createUncertaintyTypeAdapter();
                }

                public Object caseUOMType(UOMType object) {
                    return createUOMTypeAdapter();
                }

                public Object caseValuesType(ValuesType object) {
                    return createValuesTypeAdapter();
                }

                public Object caseValueType(ValueType object) {
                    return createValueTypeAdapter();
                }

                public Object caseValueType1(ValueType1 object) {
                    return createValueType1Adapter();
                }

                public Object caseVType(VType object) {
                    return createVTypeAdapter();
                }

                public Object caseWSDLType(WSDLType object) {
                    return createWSDLTypeAdapter();
                }

                public Object caseAnyType(AnyType object) {
                    return createAnyTypeAdapter();
                }

                public Object defaultCase(EObject object) {
                    return createEObjectAdapter();
                }
            };

    /**
     * Creates an adapter for the <code>target</code>.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     *
     * @param target the object to adapt.
     * @return the adapter for the <code>target</code>.
     * @generated
     */
    public Adapter createAdapter(Notifier target) {
        return (Adapter) modelSwitch.doSwitch((EObject) target);
    }


    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.AbstractType <em>Abstract Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.AbstractType
     */
    public Adapter createAbstractTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.AcceptVersionsType <em>Accept Versions Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.AcceptVersionsType
     */
    public Adapter createAcceptVersionsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.AttributeDataType <em>Attribute Data Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.AttributeDataType
     */
    public Adapter createAttributeDataTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.AttributesType <em>Attributes Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.AttributesType
     */
    public Adapter createAttributesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.BoundingCoordinatesType <em>Bounding Coordinates Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.BoundingCoordinatesType
     */
    public Adapter createBoundingCoordinatesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ClassesType <em>Classes Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ClassesType
     */
    public Adapter createClassesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ClassesType1 <em>Classes Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ClassesType1
     */
    public Adapter createClassesType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ColumnsetType <em>Columnset Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ColumnsetType
     */
    public Adapter createColumnsetTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ColumnType <em>Column Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ColumnType
     */
    public Adapter createColumnTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ColumnType1 <em>Column Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ColumnType1
     */
    public Adapter createColumnType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ColumnType2 <em>Column Type2</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ColumnType2
     */
    public Adapter createColumnType2Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.CountType <em>Count Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.CountType
     */
    public Adapter createCountTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DataDescriptionsType <em>Data Descriptions Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DataDescriptionsType
     */
    public Adapter createDataDescriptionsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DataInputsType <em>Data Inputs Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DataInputsType
     */
    public Adapter createDataInputsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DatasetDescriptionsType <em>Dataset Descriptions Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DatasetDescriptionsType
     */
    public Adapter createDatasetDescriptionsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DatasetType <em>Dataset Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DatasetType
     */
    public Adapter createDatasetTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DatasetType1 <em>Dataset Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DatasetType1
     */
    public Adapter createDatasetType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DatasetType2 <em>Dataset Type2</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DatasetType2
     */
    public Adapter createDatasetType2Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DatasetType3 <em>Dataset Type3</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DatasetType3
     */
    public Adapter createDatasetType3Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeDataRequestType <em>Describe Data Request Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeDataRequestType
     */
    public Adapter createDescribeDataRequestTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeDatasetsRequestType <em>Describe Datasets Request Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsRequestType
     */
    public Adapter createDescribeDatasetsRequestTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeDatasetsType <em>Describe Datasets Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeDatasetsType
     */
    public Adapter createDescribeDatasetsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeDataType <em>Describe Data Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeDataType
     */
    public Adapter createDescribeDataTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeFrameworkKeyType <em>Describe Framework Key Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworkKeyType
     */
    public Adapter createDescribeFrameworkKeyTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeFrameworksType <em>Describe Frameworks Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeFrameworksType
     */
    public Adapter createDescribeFrameworksTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DescribeKeyType <em>Describe Key Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DescribeKeyType
     */
    public Adapter createDescribeKeyTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.DocumentRoot <em>Document Root</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.DocumentRoot
     */
    public Adapter createDocumentRootAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ExceptionReportType <em>Exception Report Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ExceptionReportType
     */
    public Adapter createExceptionReportTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FailedType <em>Failed Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FailedType
     */
    public Adapter createFailedTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkDatasetDescribeDataType <em>Framework Dataset Describe Data Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkDatasetDescribeDataType
     */
    public Adapter createFrameworkDatasetDescribeDataTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkDescriptionsType <em>Framework Descriptions Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkDescriptionsType
     */
    public Adapter createFrameworkDescriptionsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkKeyDescriptionType <em>Framework Key Description Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyDescriptionType
     */
    public Adapter createFrameworkKeyDescriptionTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkKeyType <em>Framework Key Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType
     */
    public Adapter createFrameworkKeyTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkKeyType1 <em>Framework Key Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkKeyType1
     */
    public Adapter createFrameworkKeyType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkType <em>Framework Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkType
     */
    public Adapter createFrameworkTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkType1 <em>Framework Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkType1
     */
    public Adapter createFrameworkType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkType2 <em>Framework Type2</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkType2
     */
    public Adapter createFrameworkType2Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkType3 <em>Framework Type3</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkType3
     */
    public Adapter createFrameworkType3Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.FrameworkType4 <em>Framework Type4</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.FrameworkType4
     */
    public Adapter createFrameworkType4Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.GDASType <em>GDAS Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.GDASType
     */
    public Adapter createGDASTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.GetCapabilitiesType <em>Get Capabilities Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.GetCapabilitiesType
     */
    public Adapter createGetCapabilitiesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.GetDataRequestType <em>Get Data Request Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.GetDataRequestType
     */
    public Adapter createGetDataRequestTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.GetDataType <em>Get Data Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.GetDataType
     */
    public Adapter createGetDataTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.GetDataXMLType <em>Get Data XML Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.GetDataXMLType
     */
    public Adapter createGetDataXMLTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.JoinAbilitiesType <em>Join Abilities Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.JoinAbilitiesType
     */
    public Adapter createJoinAbilitiesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.JoinDataResponseType <em>Join Data Response Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.JoinDataResponseType
     */
    public Adapter createJoinDataResponseTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.JoinDataType <em>Join Data Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.JoinDataType
     */
    public Adapter createJoinDataTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.JoinedOutputsType <em>Joined Outputs Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.JoinedOutputsType
     */
    public Adapter createJoinedOutputsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.KType <em>KType</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.KType
     */
    public Adapter createKTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.LanguagesType <em>Languages Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.LanguagesType
     */
    public Adapter createLanguagesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.MapStylingType <em>Map Styling Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.MapStylingType
     */
    public Adapter createMapStylingTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.MeasureCountExceptions <em>Measure Count Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.MeasureCountExceptions
     */
    public Adapter createMeasureCountExceptionsAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.MeasureType <em>Measure Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.MeasureType
     */
    public Adapter createMeasureTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.MechanismType <em>Mechanism Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.MechanismType
     */
    public Adapter createMechanismTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.NominalOrdinalExceptions <em>Nominal Ordinal Exceptions</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.NominalOrdinalExceptions
     */
    public Adapter createNominalOrdinalExceptionsAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.NominalType <em>Nominal Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.NominalType
     */
    public Adapter createNominalTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.NullType <em>Null Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.NullType
     */
    public Adapter createNullTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.NullType1 <em>Null Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.NullType1
     */
    public Adapter createNullType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.OrdinalType <em>Ordinal Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.OrdinalType
     */
    public Adapter createOrdinalTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.OutputMechanismsType <em>Output Mechanisms Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.OutputMechanismsType
     */
    public Adapter createOutputMechanismsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.OutputStylingsType <em>Output Stylings Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.OutputStylingsType
     */
    public Adapter createOutputStylingsTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.OutputStylingsType1 <em>Output Stylings Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.OutputStylingsType1
     */
    public Adapter createOutputStylingsType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.OutputType <em>Output Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.OutputType
     */
    public Adapter createOutputTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ParameterType <em>Parameter Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ParameterType
     */
    public Adapter createParameterTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ReferenceDateType <em>Reference Date Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ReferenceDateType
     */
    public Adapter createReferenceDateTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.RequestBaseType <em>Request Base Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.RequestBaseType
     */
    public Adapter createRequestBaseTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ResourceType <em>Resource Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ResourceType
     */
    public Adapter createResourceTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.RowsetType <em>Rowset Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.RowsetType
     */
    public Adapter createRowsetTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.RowsetType1 <em>Rowset Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.RowsetType1
     */
    public Adapter createRowsetType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.RowType <em>Row Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.RowType
     */
    public Adapter createRowTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.RowType1 <em>Row Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.RowType1
     */
    public Adapter createRowType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.SpatialFrameworksType <em>Spatial Frameworks Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.SpatialFrameworksType
     */
    public Adapter createSpatialFrameworksTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.StatusType <em>Status Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.StatusType
     */
    public Adapter createStatusTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.StylingType <em>Styling Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.StylingType
     */
    public Adapter createStylingTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.TjsCapabilitiesType <em>Tjs Capabilities Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.TjsCapabilitiesType
     */
    public Adapter createTjsCapabilitiesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.UncertaintyType <em>Uncertainty Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.UncertaintyType
     */
    public Adapter createUncertaintyTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.UOMType <em>UOM Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.UOMType
     */
    public Adapter createUOMTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ValuesType <em>Values Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ValuesType
     */
    public Adapter createValuesTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ValueType <em>Value Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ValueType
     */
    public Adapter createValueTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.ValueType1 <em>Value Type1</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.ValueType1
     */
    public Adapter createValueType1Adapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.VType <em>VType</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.VType
     */
    public Adapter createVTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link net.opengis.tjs10.WSDLType <em>WSDL Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see net.opengis.tjs10.WSDLType
     */
    public Adapter createWSDLTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for an object of class '{@link org.eclipse.emf.ecore.xml.type.AnyType <em>Any Type</em>}'.
     * <!-- begin-user-doc -->
     * This default implementation returns null so that we can easily ignore cases;
     * it's useful to ignore a case when inheritance will catch all the cases anyway.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     * @see org.eclipse.emf.ecore.xml.type.AnyType
     */
    public Adapter createAnyTypeAdapter() {
        return null;
    }

    /**
     * Creates a new adapter for the default case.
     * <!-- begin-user-doc -->
     * This default implementation returns null.
     * <!-- end-user-doc -->
     *
     * @return the new adapter.
     * @generated
     */
    public Adapter createEObjectAdapter() {
        return null;
    }

} //Tjs10AdapterFactory
