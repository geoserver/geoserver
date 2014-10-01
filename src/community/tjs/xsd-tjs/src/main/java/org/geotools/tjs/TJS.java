package org.geotools.tjs;


import org.geotools.xml.XSD;

import javax.xml.namespace.QName;
import java.util.Set;

/**
 * This interface contains the qualified names of all the types,elements, and
 * attributes in the http://www.opengis.net/tjs/1.0 schema.
 *
 * @generated
 */
public final class TJS extends XSD {

    /**
     * singleton instance
     */
    private static final TJS instance = new TJS();

    /**
     * Returns the singleton instance.
     */
    public static final TJS getInstance() {
        return instance;
    }

    /**
     * private constructor
     */
    private TJS() {
    }

    protected void addDependencies(Set dependencies) {
        //TODO: add dependencies here
    }

    /**
     * Returns 'http://www.opengis.net/tjs/1.0'.
     */
    public String getNamespaceURI() {
        return NAMESPACE;
    }

    /**
     * Returns the location of 'tjsAll.xsd.'.
     */
    public String getSchemaLocation() {
        return getClass().getResource("tjsAll.xsd").toString();
    }

    /**
     * @generated
     */
    public static final String NAMESPACE = "http://www.opengis.net/tjs/1.0";

    /* Type Definitions */
    /**
     * @generated
     */
    public static final QName AbstractType =
            new QName("http://www.opengis.net/tjs/1.0", "AbstractType");
    /**
     * @generated
     */
    public static final QName AcceptLanguagesType =
            new QName("http://www.opengis.net/tjs/1.0", "AcceptLanguagesType");
    /**
     * @generated
     */
    public static final QName AcceptVersionsType =
            new QName("http://www.opengis.net/tjs/1.0", "AcceptVersionsType");
    /**
     * @generated
     */
    public static final QName AttributeDataType =
            new QName("http://www.opengis.net/tjs/1.0", "AttributeDataType");
    /**
     * @generated
     */
    public static final QName AttributesType =
            new QName("http://www.opengis.net/tjs/1.0", "AttributesType");
    /**
     * @generated
     */
    public static final QName BoundingCoordinatesType =
            new QName("http://www.opengis.net/tjs/1.0", "BoundingCoordinatesType");
    /**
     * @generated
     */
    public static final QName Classes1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Classes1Type");
    /**
     * @generated
     */
    public static final QName ClassesType =
            new QName("http://www.opengis.net/tjs/1.0", "ClassesType");
    /**
     * @generated
     */
    public static final QName Column1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Column1Type");
    /**
     * @generated
     */
    public static final QName Column2Type =
            new QName("http://www.opengis.net/tjs/1.0", "Column2Type");
    /**
     * @generated
     */
    public static final QName ColumnsetType =
            new QName("http://www.opengis.net/tjs/1.0", "ColumnsetType");
    /**
     * @generated
     */
    public static final QName ColumnType =
            new QName("http://www.opengis.net/tjs/1.0", "ColumnType");
    /**
     * @generated
     */
    public static final QName CountType =
            new QName("http://www.opengis.net/tjs/1.0", "CountType");
    /**
     * @generated
     */
    public static final QName DataClassType =
            new QName("http://www.opengis.net/tjs/1.0", "DataClassType");
    /**
     * @generated
     */
    public static final QName DataDescriptionsType =
            new QName("http://www.opengis.net/tjs/1.0", "DataDescriptionsType");
    /**
     * @generated
     */
    public static final QName DataInputsType =
            new QName("http://www.opengis.net/tjs/1.0", "DataInputsType");
    /**
     * @generated
     */
    public static final QName Dataset1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Dataset1Type");
    /**
     * @generated
     */
    public static final QName Dataset2Type =
            new QName("http://www.opengis.net/tjs/1.0", "Dataset2Type");
    /**
     * @generated
     */
    public static final QName Dataset3Type =
            new QName("http://www.opengis.net/tjs/1.0", "Dataset3Type");
    /**
     * @generated
     */
    public static final QName DatasetDescriptionsType =
            new QName("http://www.opengis.net/tjs/1.0", "DatasetDescriptionsType");
    /**
     * @generated
     */
    public static final QName DatasetType =
            new QName("http://www.opengis.net/tjs/1.0", "DatasetType");
    /**
     * @generated
     */
    public static final QName DescribeDataRequestType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDataRequestType");
    /**
     * @generated
     */
    public static final QName DescribeDatasetsRequestType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDatasetsRequestType");
    /**
     * @generated
     */
    public static final QName DescribeDatasetsType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDatasetsType");
    /**
     * @generated
     */
    public static final QName DescribeDatasetsValueType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDatasetsValueType");
    /**
     * @generated
     */
    public static final QName DescribeDataType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDataType");
    /**
     * @generated
     */
    public static final QName DescribeDataValueType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDataValueType");
    /**
     * @generated
     */
    public static final QName DescribeFrameworkKeyType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeFrameworkKeyType");
    /**
     * @generated
     */
    public static final QName DescribeFrameworksType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeFrameworksType");
    /**
     * @generated
     */
    public static final QName DescribeFrameworksValueType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeFrameworksValueType");
    /**
     * @generated
     */
    public static final QName DescribeJoinAbilitiesValueType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeJoinAbilitiesValueType");
    /**
     * @generated
     */
    public static final QName DescribeKeyType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeKeyType");
    /**
     * @generated
     */
    public static final QName DescribeKeyValueType =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeKeyValueType");
    /**
     * @generated
     */
    public static final QName ExceptionReportType =
            new QName("http://www.opengis.net/tjs/1.0", "ExceptionReportType");
    /**
     * @generated
     */
    public static final QName FailedType =
            new QName("http://www.opengis.net/tjs/1.0", "FailedType");
    /**
     * @generated
     */
    public static final QName Framework1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Framework1Type");
    /**
     * @generated
     */
    public static final QName Framework2Type =
            new QName("http://www.opengis.net/tjs/1.0", "Framework2Type");
    /**
     * @generated
     */
    public static final QName Framework3Type =
            new QName("http://www.opengis.net/tjs/1.0", "Framework3Type");
    /**
     * @generated
     */
    public static final QName Framework4Type =
            new QName("http://www.opengis.net/tjs/1.0", "Framework4Type");
    /**
     * @generated
     */
    public static final QName FrameworkDatasetDescribeDataType =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkDatasetDescribeDataType");
    /**
     * @generated
     */
    public static final QName FrameworkDescriptionsType =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkDescriptionsType");
    /**
     * @generated
     */
    public static final QName FrameworkKey1Type =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkKey1Type");
    /**
     * @generated
     */
    public static final QName FrameworkKeyDescriptionType =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkKeyDescriptionType");
    /**
     * @generated
     */
    public static final QName FrameworkKeyType =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkKeyType");
    /**
     * @generated
     */
    public static final QName FrameworkType =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkType");
    /**
     * @generated
     */
    public static final QName gaussianType =
            new QName("http://www.opengis.net/tjs/1.0", "gaussianType");
    /**
     * @generated
     */
    public static final QName GDASType =
            new QName("http://www.opengis.net/tjs/1.0", "GDASType");
    /**
     * @generated
     */
    public static final QName GetCapabilitiesType =
            new QName("http://www.opengis.net/tjs/1.0", "GetCapabilitiesType");
    /**
     * @generated
     */
    public static final QName GetCapabilitiesValueType =
            new QName("http://www.opengis.net/tjs/1.0", "GetCapabilitiesValueType");
    /**
     * @generated
     */
    public static final QName GetDataRequestType =
            new QName("http://www.opengis.net/tjs/1.0", "GetDataRequestType");
    /**
     * @generated
     */
    public static final QName GetDataType =
            new QName("http://www.opengis.net/tjs/1.0", "GetDataType");
    /**
     * @generated
     */
    public static final QName GetDataValueType =
            new QName("http://www.opengis.net/tjs/1.0", "GetDataValueType");
    /**
     * @generated
     */
    public static final QName GetDataXMLType =
            new QName("http://www.opengis.net/tjs/1.0", "GetDataXMLType");
    /**
     * @generated
     */
    public static final QName JoinAbilitiesType =
            new QName("http://www.opengis.net/tjs/1.0", "JoinAbilitiesType");
    /**
     * @generated
     */
    public static final QName JoinDataResponseType =
            new QName("http://www.opengis.net/tjs/1.0", "JoinDataResponseType");
    /**
     * @generated
     */
    public static final QName JoinDataType =
            new QName("http://www.opengis.net/tjs/1.0", "JoinDataType");
    /**
     * @generated
     */
    public static final QName JoinDataValueType =
            new QName("http://www.opengis.net/tjs/1.0", "JoinDataValueType");
    /**
     * @generated
     */
    public static final QName JoinedOutputsType =
            new QName("http://www.opengis.net/tjs/1.0", "JoinedOutputsType");
    /**
     * @generated
     */
    public static final QName KType =
            new QName("http://www.opengis.net/tjs/1.0", "KType");
    /**
     * @generated
     */
    public static final QName LanguagesType =
            new QName("http://www.opengis.net/tjs/1.0", "LanguagesType");
    /**
     * @generated
     */
    public static final QName MapStylingType =
            new QName("http://www.opengis.net/tjs/1.0", "MapStylingType");
    /**
     * @generated
     */
    public static final QName MeasureCountExceptions =
            new QName("http://www.opengis.net/tjs/1.0", "MeasureCountExceptions");
    /**
     * @generated
     */
    public static final QName MeasureType =
            new QName("http://www.opengis.net/tjs/1.0", "MeasureType");
    /**
     * @generated
     */
    public static final QName MechanismType =
            new QName("http://www.opengis.net/tjs/1.0", "MechanismType");
    /**
     * @generated
     */
    public static final QName NominalOrdinalExceptions =
            new QName("http://www.opengis.net/tjs/1.0", "NominalOrdinalExceptions");
    /**
     * @generated
     */
    public static final QName NominalType =
            new QName("http://www.opengis.net/tjs/1.0", "NominalType");
    /**
     * @generated
     */
    public static final QName Null1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Null1Type");
    /**
     * @generated
     */
    public static final QName NullType =
            new QName("http://www.opengis.net/tjs/1.0", "NullType");
    /**
     * @generated
     */
    public static final QName OrdinalType =
            new QName("http://www.opengis.net/tjs/1.0", "OrdinalType");
    /**
     * @generated
     */
    public static final QName OutputMechanismsType =
            new QName("http://www.opengis.net/tjs/1.0", "OutputMechanismsType");
    /**
     * @generated
     */
    public static final QName OutputStylingsType =
            new QName("http://www.opengis.net/tjs/1.0", "OutputStylingsType");
    /**
     * @generated
     */
    public static final QName OutputType =
            new QName("http://www.opengis.net/tjs/1.0", "OutputType");
    /**
     * @generated
     */
    public static final QName ParameterType =
            new QName("http://www.opengis.net/tjs/1.0", "ParameterType");
    /**
     * @generated
     */
    public static final QName purposeType =
            new QName("http://www.opengis.net/tjs/1.0", "purposeType");
    /**
     * @generated
     */
    public static final QName ReferenceDateType =
            new QName("http://www.opengis.net/tjs/1.0", "ReferenceDateType");
    /**
     * @generated
     */
    public static final QName RequestBaseType =
            new QName("http://www.opengis.net/tjs/1.0", "RequestBaseType");
    /**
     * @generated
     */
    public static final QName RequestServiceType =
            new QName("http://www.opengis.net/tjs/1.0", "RequestServiceType");
    /**
     * @generated
     */
    public static final QName ResourceType =
            new QName("http://www.opengis.net/tjs/1.0", "ResourceType");
    /**
     * @generated
     */
    public static final QName Row1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Row1Type");
    /**
     * @generated
     */
    public static final QName Rowset1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Rowset1Type");
    /**
     * @generated
     */
    public static final QName RowsetType =
            new QName("http://www.opengis.net/tjs/1.0", "RowsetType");
    /**
     * @generated
     */
    public static final QName RowType =
            new QName("http://www.opengis.net/tjs/1.0", "RowType");
    /**
     * @generated
     */
    public static final QName SectionsType =
            new QName("http://www.opengis.net/tjs/1.0", "SectionsType");
    /**
     * @generated
     */
    public static final QName SpatialFrameworksType =
            new QName("http://www.opengis.net/tjs/1.0", "SpatialFrameworksType");
    /**
     * @generated
     */
    public static final QName StatusType =
            new QName("http://www.opengis.net/tjs/1.0", "StatusType");
    /**
     * @generated
     */
    public static final QName StylingType =
            new QName("http://www.opengis.net/tjs/1.0", "StylingType");
    /**
     * @generated
     */
    public static final QName tjsCapabilitiesType =
            new QName("http://www.opengis.net/tjs/1.0", "tjsCapabilitiesType");
    /**
     * @generated
     */
    public static final QName typeType =
            new QName("http://www.opengis.net/tjs/1.0", "typeType");
    /**
     * @generated
     */
    public static final QName UncertaintyType =
            new QName("http://www.opengis.net/tjs/1.0", "UncertaintyType");
    /**
     * @generated
     */
    public static final QName UOMType =
            new QName("http://www.opengis.net/tjs/1.0", "UOMType");
    /**
     * @generated
     */
    public static final QName updateType =
            new QName("http://www.opengis.net/tjs/1.0", "updateType");
    /**
     * @generated
     */
    public static final QName Value1Type =
            new QName("http://www.opengis.net/tjs/1.0", "Value1Type");
    /**
     * @generated
     */
    public static final QName ValuesType =
            new QName("http://www.opengis.net/tjs/1.0", "ValuesType");
    /**
     * @generated
     */
    public static final QName ValueType =
            new QName("http://www.opengis.net/tjs/1.0", "ValueType");
    /**
     * @generated
     */
    public static final QName versionType =
            new QName("http://www.opengis.net/tjs/1.0", "versionType");
    /**
     * @generated
     */
    public static final QName VersionType =
            new QName("http://www.opengis.net/tjs/1.0", "VersionType");
    /**
     * @generated
     */
    public static final QName VType =
            new QName("http://www.opengis.net/tjs/1.0", "VType");
    /**
     * @generated
     */
    public static final QName WSDLType =
            new QName("http://www.opengis.net/tjs/1.0", "WSDLType");

    /* Elements */
    /**
     * @generated
     */
    public static final QName Abstract =
            new QName("http://www.opengis.net/tjs/1.0", "Abstract");
    /**
     * @generated
     */
    public static final QName AttributeLimit =
            new QName("http://www.opengis.net/tjs/1.0", "AttributeLimit");
    /**
     * @generated
     */
    public static final QName Attributes =
            new QName("http://www.opengis.net/tjs/1.0", "Attributes");
    /**
     * @generated
     */
    public static final QName BoundingCoordinates =
            new QName("http://www.opengis.net/tjs/1.0", "BoundingCoordinates");
    /**
     * @generated
     */
    public static final QName Capabilities =
            new QName("http://www.opengis.net/tjs/1.0", "Capabilities");
    /**
     * @generated
     */
    public static final QName Columnset =
            new QName("http://www.opengis.net/tjs/1.0", "Columnset");
    /**
     * @generated
     */
    public static final QName Count =
            new QName("http://www.opengis.net/tjs/1.0", "Count");
    /**
     * @generated
     */
    public static final QName DataClass =
            new QName("http://www.opengis.net/tjs/1.0", "DataClass");
    /**
     * @generated
     */
    public static final QName DataDescriptions =
            new QName("http://www.opengis.net/tjs/1.0", "DataDescriptions");
    /**
     * @generated
     */
    public static final QName Dataset =
            new QName("http://www.opengis.net/tjs/1.0", "Dataset");
    /**
     * @generated
     */
    public static final QName DatasetDescriptions =
            new QName("http://www.opengis.net/tjs/1.0", "DatasetDescriptions");
    /**
     * @generated
     */
    public static final QName DatasetURI =
            new QName("http://www.opengis.net/tjs/1.0", "DatasetURI");
    /**
     * @generated
     */
    public static final QName DescribeData =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeData");
    /**
     * @generated
     */
    public static final QName DescribeDataRequest =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDataRequest");
    /**
     * @generated
     */
    public static final QName DescribeDatasets =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDatasets");
    /**
     * @generated
     */
    public static final QName DescribeDatasetsRequest =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeDatasetsRequest");
    /**
     * @generated
     */
    public static final QName DescribeFrameworks =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeFrameworks");
    /**
     * @generated
     */
    public static final QName DescribeJoinAbilities =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeJoinAbilities");
    /**
     * @generated
     */
    public static final QName DescribeKey =
            new QName("http://www.opengis.net/tjs/1.0", "DescribeKey");
    /**
     * @generated
     */
    public static final QName Documentation =
            new QName("http://www.opengis.net/tjs/1.0", "Documentation");
    /**
     * @generated
     */
    public static final QName Framework =
            new QName("http://www.opengis.net/tjs/1.0", "Framework");
    /**
     * @generated
     */
    public static final QName FrameworkDescriptions =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkDescriptions");
    /**
     * @generated
     */
    public static final QName FrameworkKey =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkKey");
    /**
     * @generated
     */
    public static final QName FrameworkKeyDescription =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkKeyDescription");
    /**
     * @generated
     */
    public static final QName FrameworkURI =
            new QName("http://www.opengis.net/tjs/1.0", "FrameworkURI");
    /**
     * @generated
     */
    public static final QName GDAS =
            new QName("http://www.opengis.net/tjs/1.0", "GDAS");
    /**
     * @generated
     */
    public static final QName GetCapabilities =
            new QName("http://www.opengis.net/tjs/1.0", "GetCapabilities");
    /**
     * @generated
     */
    public static final QName GetData =
            new QName("http://www.opengis.net/tjs/1.0", "GetData");
    /**
     * @generated
     */
    public static final QName GetDataRequest =
            new QName("http://www.opengis.net/tjs/1.0", "GetDataRequest");
    /**
     * @generated
     */
    public static final QName Identifier =
            new QName("http://www.opengis.net/tjs/1.0", "Identifier");
    /**
     * @generated
     */
    public static final QName JoinAbilities =
            new QName("http://www.opengis.net/tjs/1.0", "JoinAbilities");
    /**
     * @generated
     */
    public static final QName JoinData =
            new QName("http://www.opengis.net/tjs/1.0", "JoinData");
    /**
     * @generated
     */
    public static final QName JoinDataResponse =
            new QName("http://www.opengis.net/tjs/1.0", "JoinDataResponse");
    /**
     * @generated
     */
    public static final QName K =
            new QName("http://www.opengis.net/tjs/1.0", "K");
    /**
     * @generated
     */
    public static final QName LinkageKeys =
            new QName("http://www.opengis.net/tjs/1.0", "LinkageKeys");
    /**
     * @generated
     */
    public static final QName Measure =
            new QName("http://www.opengis.net/tjs/1.0", "Measure");
    /**
     * @generated
     */
    public static final QName Mechanism =
            new QName("http://www.opengis.net/tjs/1.0", "Mechanism");
    /**
     * @generated
     */
    public static final QName Nominal =
            new QName("http://www.opengis.net/tjs/1.0", "Nominal");
    /**
     * @generated
     */
    public static final QName Ordinal =
            new QName("http://www.opengis.net/tjs/1.0", "Ordinal");
    /**
     * @generated
     */
    public static final QName Organization =
            new QName("http://www.opengis.net/tjs/1.0", "Organization");
    /**
     * @generated
     */
    public static final QName ReferenceDate =
            new QName("http://www.opengis.net/tjs/1.0", "ReferenceDate");
    /**
     * @generated
     */
    public static final QName Rowset =
            new QName("http://www.opengis.net/tjs/1.0", "Rowset");
    /**
     * @generated
     */
    public static final QName SpatialFrameworks =
            new QName("http://www.opengis.net/tjs/1.0", "SpatialFrameworks");
    /**
     * @generated
     */
    public static final QName Styling =
            new QName("http://www.opengis.net/tjs/1.0", "Styling");
    /**
     * @generated
     */
    public static final QName Title =
            new QName("http://www.opengis.net/tjs/1.0", "Title");
    /**
     * @generated
     */
    public static final QName Uncertainty =
            new QName("http://www.opengis.net/tjs/1.0", "Uncertainty");
    /**
     * @generated
     */
    public static final QName UOM =
            new QName("http://www.opengis.net/tjs/1.0", "UOM");
    /**
     * @generated
     */
    public static final QName Values =
            new QName("http://www.opengis.net/tjs/1.0", "Values");
    /**
     * @generated
     */
    public static final QName Version =
            new QName("http://www.opengis.net/tjs/1.0", "Version");


    /**
     * @generated
     */
    public static final QName ShortForm =
            new QName("http://www.opengis.net/tjs/1.0", "ShortForm");
    /**
     * @generated
     */
    public static final QName LongForm =
            new QName("http://www.opengis.net/tjs/1.0", "LongForm");
    /* Attributes */

}
