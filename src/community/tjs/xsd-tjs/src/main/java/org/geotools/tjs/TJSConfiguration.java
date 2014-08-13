package org.geotools.tjs;

import net.opengis.tjs10.Tjs10Factory;
import org.geotools.tjs.bindings.*;
import org.geotools.xml.Configuration;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for the http://www.opengis.net/tjs/1.0 schema.
 *
 * @generated
 */
public class TJSConfiguration extends Configuration {

    /**
     * Creates a new configuration.
     *
     * @generated
     */
    public TJSConfiguration() {
        super(TJS.getInstance());

        //TODO: add dependencies here
    }

    /**
     * Registers an instanceof {@link Tjs10Factory} in the context.
     */
    protected void configureContext(MutablePicoContainer container) {
        container.registerComponentInstance(Tjs10Factory.eINSTANCE);
    }

    /**
     * Registers the bindings for the configuration.
     *
     * @generated
     */
    protected final void registerBindings(MutablePicoContainer container) {
        //Types
        container.registerComponentImplementation(TJS.AbstractType, AbstractTypeBinding.class);
        container.registerComponentImplementation(TJS.AcceptLanguagesType, AcceptLanguagesTypeBinding.class);
        container.registerComponentImplementation(TJS.AcceptVersionsType, AcceptVersionsTypeBinding.class);
        container.registerComponentImplementation(TJS.AttributeDataType, AttributeDataTypeBinding.class);
        container.registerComponentImplementation(TJS.AttributesType, AttributesTypeBinding.class);
        container.registerComponentImplementation(TJS.BoundingCoordinatesType, BoundingCoordinatesTypeBinding.class);
        container.registerComponentImplementation(TJS.Classes1Type, Classes1TypeBinding.class);
        container.registerComponentImplementation(TJS.ClassesType, ClassesTypeBinding.class);
        container.registerComponentImplementation(TJS.Column1Type, Column1TypeBinding.class);
        container.registerComponentImplementation(TJS.Column2Type, Column2TypeBinding.class);
        container.registerComponentImplementation(TJS.ColumnsetType, ColumnsetTypeBinding.class);
        container.registerComponentImplementation(TJS.ColumnType, ColumnTypeBinding.class);
        container.registerComponentImplementation(TJS.CountType, CountTypeBinding.class);
        container.registerComponentImplementation(TJS.DataClassType, DataClassTypeBinding.class);
        container.registerComponentImplementation(TJS.DataDescriptionsType, DataDescriptionsTypeBinding.class);
        container.registerComponentImplementation(TJS.DataInputsType, DataInputsTypeBinding.class);
        container.registerComponentImplementation(TJS.Dataset1Type, Dataset1TypeBinding.class);
        container.registerComponentImplementation(TJS.Dataset2Type, Dataset2TypeBinding.class);
        container.registerComponentImplementation(TJS.Dataset3Type, Dataset3TypeBinding.class);
        container.registerComponentImplementation(TJS.DatasetDescriptionsType, DatasetDescriptionsTypeBinding.class);
        container.registerComponentImplementation(TJS.DatasetType, DatasetTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeDataRequestType, DescribeDataRequestTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeDatasetsRequestType, DescribeDatasetsRequestTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeDatasetsType, DescribeDatasetsTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeDatasetsValueType, DescribeDatasetsValueTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeDataType, DescribeDataTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeDataValueType, DescribeDataValueTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeFrameworkKeyType, DescribeFrameworkKeyTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeFrameworksType, DescribeFrameworksTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeFrameworksValueType, DescribeFrameworksValueTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeJoinAbilitiesValueType, DescribeJoinAbilitiesValueTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeKeyType, DescribeKeyTypeBinding.class);
        container.registerComponentImplementation(TJS.DescribeKeyValueType, DescribeKeyValueTypeBinding.class);
        container.registerComponentImplementation(TJS.ExceptionReportType, ExceptionReportTypeBinding.class);
        container.registerComponentImplementation(TJS.FailedType, FailedTypeBinding.class);
        container.registerComponentImplementation(TJS.Framework1Type, Framework1TypeBinding.class);
        container.registerComponentImplementation(TJS.Framework2Type, Framework2TypeBinding.class);
        container.registerComponentImplementation(TJS.Framework3Type, Framework3TypeBinding.class);
        container.registerComponentImplementation(TJS.Framework4Type, Framework4TypeBinding.class);
        container.registerComponentImplementation(TJS.FrameworkDatasetDescribeDataType, FrameworkDatasetDescribeDataTypeBinding.class);
        container.registerComponentImplementation(TJS.FrameworkDescriptionsType, FrameworkDescriptionsTypeBinding.class);
        container.registerComponentImplementation(TJS.FrameworkKey1Type, FrameworkKey1TypeBinding.class);
        container.registerComponentImplementation(TJS.FrameworkKeyDescriptionType, FrameworkKeyDescriptionTypeBinding.class);
        container.registerComponentImplementation(TJS.FrameworkKeyType, FrameworkKeyTypeBinding.class);
        container.registerComponentImplementation(TJS.FrameworkType, FrameworkTypeBinding.class);
        container.registerComponentImplementation(TJS.gaussianType, GaussianTypeBinding.class);
        container.registerComponentImplementation(TJS.GDASType, GDASTypeBinding.class);
        container.registerComponentImplementation(TJS.GetCapabilitiesType, GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(TJS.GetCapabilitiesValueType, GetCapabilitiesValueTypeBinding.class);
        container.registerComponentImplementation(TJS.GetDataRequestType, GetDataRequestTypeBinding.class);
        container.registerComponentImplementation(TJS.GetDataType, GetDataTypeBinding.class);
        container.registerComponentImplementation(TJS.GetDataValueType, GetDataValueTypeBinding.class);
        container.registerComponentImplementation(TJS.GetDataXMLType, GetDataXMLTypeBinding.class);
        container.registerComponentImplementation(TJS.JoinAbilitiesType, JoinAbilitiesTypeBinding.class);
        container.registerComponentImplementation(TJS.JoinDataResponseType, JoinDataResponseTypeBinding.class);
        container.registerComponentImplementation(TJS.JoinDataType, JoinDataTypeBinding.class);
        container.registerComponentImplementation(TJS.JoinDataValueType, JoinDataValueTypeBinding.class);
        container.registerComponentImplementation(TJS.JoinedOutputsType, JoinedOutputsTypeBinding.class);
        container.registerComponentImplementation(TJS.KType, KTypeBinding.class);
        container.registerComponentImplementation(TJS.LanguagesType, LanguagesTypeBinding.class);
        container.registerComponentImplementation(TJS.MapStylingType, MapStylingTypeBinding.class);
        container.registerComponentImplementation(TJS.MeasureCountExceptions, MeasureCountExceptionsBinding.class);
        container.registerComponentImplementation(TJS.MeasureType, MeasureTypeBinding.class);
        container.registerComponentImplementation(TJS.MechanismType, MechanismTypeBinding.class);
        container.registerComponentImplementation(TJS.NominalOrdinalExceptions, NominalOrdinalExceptionsBinding.class);
        container.registerComponentImplementation(TJS.NominalType, NominalTypeBinding.class);
        container.registerComponentImplementation(TJS.Null1Type, Null1TypeBinding.class);
        container.registerComponentImplementation(TJS.NullType, NullTypeBinding.class);
        container.registerComponentImplementation(TJS.OrdinalType, OrdinalTypeBinding.class);
        container.registerComponentImplementation(TJS.OutputMechanismsType, OutputMechanismsTypeBinding.class);
        container.registerComponentImplementation(TJS.OutputStylingsType, OutputStylingsTypeBinding.class);
        container.registerComponentImplementation(TJS.OutputStylingsType, OutputStylingsTypeBinding.class);
        container.registerComponentImplementation(TJS.OutputType, OutputTypeBinding.class);
        container.registerComponentImplementation(TJS.ParameterType, ParameterTypeBinding.class);
        container.registerComponentImplementation(TJS.purposeType, PurposeTypeBinding.class);
        container.registerComponentImplementation(TJS.ReferenceDateType, ReferenceDateTypeBinding.class);
        container.registerComponentImplementation(TJS.RequestBaseType, RequestBaseTypeBinding.class);
        container.registerComponentImplementation(TJS.RequestServiceType, RequestServiceTypeBinding.class);
        container.registerComponentImplementation(TJS.ResourceType, ResourceTypeBinding.class);
        container.registerComponentImplementation(TJS.Row1Type, Row1TypeBinding.class);
        container.registerComponentImplementation(TJS.Rowset1Type, Rowset1TypeBinding.class);
        container.registerComponentImplementation(TJS.RowsetType, RowsetTypeBinding.class);
        container.registerComponentImplementation(TJS.RowType, RowTypeBinding.class);
        container.registerComponentImplementation(TJS.SectionsType, SectionsTypeBinding.class);
        container.registerComponentImplementation(TJS.SpatialFrameworksType, SpatialFrameworksTypeBinding.class);
        container.registerComponentImplementation(TJS.StatusType, StatusTypeBinding.class);
        container.registerComponentImplementation(TJS.StylingType, StylingTypeBinding.class);
        container.registerComponentImplementation(TJS.tjsCapabilitiesType, TjsCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(TJS.typeType, TypeTypeBinding.class);
        container.registerComponentImplementation(TJS.UncertaintyType, UncertaintyTypeBinding.class);
        container.registerComponentImplementation(TJS.UOMType, UOMTypeBinding.class);
        container.registerComponentImplementation(TJS.updateType, UpdateTypeBinding.class);
        container.registerComponentImplementation(TJS.Value1Type, Value1TypeBinding.class);
        container.registerComponentImplementation(TJS.ValuesType, ValuesTypeBinding.class);
        container.registerComponentImplementation(TJS.ValueType, ValueTypeBinding.class);
        container.registerComponentImplementation(TJS.versionType, VersionTypeBinding.class);
        container.registerComponentImplementation(TJS.VersionType, VersionTypeBinding.class);
        container.registerComponentImplementation(TJS.VersionType, VersionTypeBinding.class);
        container.registerComponentImplementation(TJS.VType, VTypeBinding.class);
        container.registerComponentImplementation(TJS.WSDLType, WSDLTypeBinding.class);

        //Elements
        container.registerComponentImplementation(TJS.Abstract, AbstractBinding.class);
        container.registerComponentImplementation(TJS.AttributeLimit, AttributeLimitBinding.class);
//        container.registerComponentImplementation(TJS.Attributes,AttributesBinding.class);
        container.registerComponentImplementation(TJS.BoundingCoordinates, BoundingCoordinatesBinding.class);
        container.registerComponentImplementation(TJS.Capabilities, CapabilitiesBinding.class);
        container.registerComponentImplementation(TJS.Columnset, ColumnsetBinding.class);
        container.registerComponentImplementation(TJS.Count, CountBinding.class);
        container.registerComponentImplementation(TJS.DataClass, DataClassBinding.class);
        container.registerComponentImplementation(TJS.DataDescriptions, DataDescriptionsBinding.class);
        //container.registerComponentImplementation(TJS.Dataset,DatasetBinding.class);
        container.registerComponentImplementation(TJS.DatasetDescriptions, DatasetDescriptionsBinding.class);
        container.registerComponentImplementation(TJS.DatasetURI, DatasetURIBinding.class);
        container.registerComponentImplementation(TJS.DescribeData, DescribeDataBinding.class);
        container.registerComponentImplementation(TJS.DescribeDataRequest, DescribeDataRequestBinding.class);
        container.registerComponentImplementation(TJS.DescribeDatasets, DescribeDatasetsBinding.class);
        container.registerComponentImplementation(TJS.DescribeDatasetsRequest, DescribeDatasetsRequestBinding.class);
        container.registerComponentImplementation(TJS.DescribeFrameworks, DescribeFrameworksBinding.class);
        container.registerComponentImplementation(TJS.DescribeJoinAbilities, DescribeJoinAbilitiesBinding.class);
        container.registerComponentImplementation(TJS.DescribeKey, DescribeKeyBinding.class);
        container.registerComponentImplementation(TJS.Documentation, DocumentationBinding.class);
        //container.registerComponentImplementation(TJS.Framework,FrameworkBinding.class);
        //container.registerComponentImplementation(TJS.FrameworkDescriptions,FrameworkDescriptionsBinding.class);
        //container.registerComponentImplementation(TJS.FrameworkKey,FrameworkKeyBinding.class);
        container.registerComponentImplementation(TJS.FrameworkKeyDescription, FrameworkKeyDescriptionBinding.class);
        container.registerComponentImplementation(TJS.FrameworkURI, FrameworkURIBinding.class);
//        container.registerComponentImplementation(TJS.GDAS,GDASBinding.class);
        container.registerComponentImplementation(TJS.GetCapabilities, GetCapabilitiesBinding.class);
        container.registerComponentImplementation(TJS.GetData, GetDataBinding.class);
        container.registerComponentImplementation(TJS.GetDataRequest, GetDataRequestBinding.class);
        container.registerComponentImplementation(TJS.Identifier, IdentifierBinding.class);
        container.registerComponentImplementation(TJS.JoinAbilities, JoinAbilitiesBinding.class);
        container.registerComponentImplementation(TJS.JoinData, JoinDataBinding.class);
        container.registerComponentImplementation(TJS.JoinDataResponse, JoinDataResponseBinding.class);
        container.registerComponentImplementation(TJS.K, KBinding.class);
        container.registerComponentImplementation(TJS.LinkageKeys, LinkageKeysBinding.class);
        container.registerComponentImplementation(TJS.Measure, MeasureBinding.class);
        container.registerComponentImplementation(TJS.Mechanism, MechanismBinding.class);
        container.registerComponentImplementation(TJS.Nominal, NominalBinding.class);
        container.registerComponentImplementation(TJS.Ordinal, OrdinalBinding.class);
        container.registerComponentImplementation(TJS.Organization, OrganizationBinding.class);
        container.registerComponentImplementation(TJS.ReferenceDate, ReferenceDateBinding.class);
        container.registerComponentImplementation(TJS.Rowset, RowsetBinding.class);
        container.registerComponentImplementation(TJS.SpatialFrameworks, SpatialFrameworksBinding.class);
        container.registerComponentImplementation(TJS.Styling, StylingBinding.class);
        container.registerComponentImplementation(TJS.Title, TitleBinding.class);
        container.registerComponentImplementation(TJS.Uncertainty, UncertaintyBinding.class);
        container.registerComponentImplementation(TJS.UOM, UOMBinding.class);
        container.registerComponentImplementation(TJS.Values, ValuesBinding.class);
        container.registerComponentImplementation(TJS.Version, VersionBinding.class);

        container.registerComponentImplementation(TJS.ShortForm, ShortFormBinding.class);
        container.registerComponentImplementation(TJS.LongForm, LongFormBinding.class);
    }
}
