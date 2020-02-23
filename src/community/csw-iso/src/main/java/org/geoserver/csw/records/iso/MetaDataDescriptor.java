/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.opengis.cat.csw20.ElementSetType;
import org.geoserver.csw.records.AbstractRecordDescriptor;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordFeatureTypeRegistryConfiguration;
import org.geoserver.csw.records.SpatialFilterChecker;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.csw.CSW;
import org.geotools.data.Query;
import org.geotools.data.complex.feature.type.FeatureTypeRegistry;
import org.geotools.data.complex.util.EmfComplexFeatureReader;
import org.geotools.feature.NameImpl;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.filter.SortByImpl;
import org.geotools.xsd.SchemaIndex;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes the ISO MetaData records and provides some handy constants to help building features
 * representing MD_Metadata.
 *
 * @author Niels Charlier
 */
public class MetaDataDescriptor extends AbstractRecordDescriptor {

    public static final NamespaceSupport NAMESPACES;

    public static final String NAMESPACE_GCO = "http://www.isotc211.org/2005/gco";
    public static final String NAMESPACE_GMD = "http://www.isotc211.org/2005/gmd";
    public static final String NAMESPACE_APISO = "http://www.opengis.net/cat/csw/apiso/1.0";

    public static FeatureType METADATA_TYPE;
    public static AttributeDescriptor METADATA_DESCRIPTOR;

    public static final NameImpl RECORD_BBOX_NAME =
            new NameImpl(NAMESPACE_GMD, "EX_GeographicBoundingBox");

    public static final List<Name> BRIEF_ELEMENTS;
    public static final List<Name> SUMMARY_ELEMENTS;

    public static final List<Name> QUERYABLES;

    public static Map<String, PropertyName> QUERYABLE_MAPPING = new HashMap<String, PropertyName>();

    static {

        // prepare the common namespace support
        NAMESPACES = new NamespaceSupport();
        NAMESPACES.declarePrefix("", NAMESPACE_APISO);
        NAMESPACES.declarePrefix("csw", CSW.NAMESPACE);
        NAMESPACES.declarePrefix("gco", NAMESPACE_GCO);
        NAMESPACES.declarePrefix("gmd", NAMESPACE_GMD);

        FeatureTypeFactory typeFactory = new FeatureTypeFactoryImpl();

        EmfComplexFeatureReader reader = EmfComplexFeatureReader.newInstance();

        SchemaIndex index = null;
        try {
            index =
                    reader.parse(
                            new URL(
                                    "http://schemas.opengis.net/iso/19139/20070417/gmd/metadataEntity.xsd"));
        } catch (IOException e) {
            // this is fatal
            throw new RuntimeException(e);
        }

        FeatureTypeRegistry featureTypeRegistry =
                new FeatureTypeRegistry(
                        NAMESPACES,
                        typeFactory,
                        new RecordFeatureTypeRegistryConfiguration("MD_Metadata_Type"),
                        true);

        TypeBuilder builder = new TypeBuilder(typeFactory);
        builder.setNamespaceURI(NAMESPACE_GMD);
        builder.setName("EX_GeographicBoundingBox_Type");
        builder.setBinding(MultiPolygon.class);
        builder.crs(CSWRecordDescriptor.DEFAULT_CRS);
        AttributeType bboxType = builder.geometry();
        featureTypeRegistry.register(bboxType);

        featureTypeRegistry.addSchemas(index);

        METADATA_TYPE =
                (FeatureType)
                        featureTypeRegistry.getAttributeType(
                                new NameImpl(NAMESPACE_GMD, "MD_Metadata_Type"));
        METADATA_DESCRIPTOR =
                featureTypeRegistry.getDescriptor(new NameImpl(NAMESPACE_GMD, "MD_Metadata"), null);

        BRIEF_ELEMENTS =
                createNameList(
                        NAMESPACES,
                        "Identifier",
                        "Title",
                        "Type",
                        "BoundingBox",
                        "GraphicOverview",
                        "ServiceType",
                        "ServiceTypeVersion");
        SUMMARY_ELEMENTS =
                createNameList(
                        NAMESPACES,
                        "Abstract",
                        "CharacterSet",
                        "Creator",
                        "Contributor",
                        "CouplingType",
                        "BoundingBox",
                        "Format",
                        "FormatVersion",
                        "GraphicOverview",
                        "HierarchyLevelName",
                        "Identifier",
                        "Language",
                        "Lineage",
                        "MetadataCharacterSet",
                        "MetadataStandardName",
                        "MetadataStandardVersion",
                        "Modified",
                        "OnlineResource",
                        "ParentIdentifier",
                        "Publisher",
                        "ResourceLanguage",
                        "ReferenceSystem",
                        "RevisionDate",
                        "Rights",
                        "ServiceOperation",
                        "ServiceType",
                        "ServiceTypeVersion",
                        "SpatialResolution",
                        "SpatialRepresentationType",
                        "Title",
                        "TopicCategory",
                        "Type");

        QUERYABLES = SUMMARY_ELEMENTS;

        // brief
        addQueryableMapping("Identifier", "gmd:fileIdentifier/gco:CharacterString");
        addQueryableMapping(
                "Title",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString");
        addQueryableMapping("Type", "gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue");
        addQueryableMapping(
                "BoundingBox",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox");
        addQueryableMapping(
                "GraphicOverview",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:graphicOverview/gmd:MD_BrowseGraphic/gmd:fileName/gco:CharacterString");
        addQueryableMapping(
                "ServiceType",
                "gmd:identificationInfo/gmd:SV_ServiceIdentification/gmd:serviceType/gco:CharacterString");
        addQueryableMapping(
                "ServiceTypeVersion",
                "gmd:identificationInfo/gmd:SV_ServiceIdentification/gmd:serviceTypeVersion/gco:CharacterString");
        // :summary
        addQueryableMapping(
                "Abstract",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:abstract/gco:CharacterString");
        addQueryableMapping(
                "CharacterSet",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:characterSet/gmd:MD_CharacterSetCode/@codeListValue");
        addQueryableMapping(
                "Creator",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName[role/gmd:CI_RoleCode/gmd:@codeListValue=’originator’]/gco:CharacterString");
        addQueryableMapping(
                "Contributor",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName[role/gmd:CI_RoleCode/gmd:@codeListValue=’author’]/gco:CharacterString");
        addQueryableMapping(
                "CouplingType",
                "gmd:identificationInfo/gmd:SV_ServiceIdentification/gmd:couplingType/gmd:SV_CouplingType/gmd:code/@codeListValue");
        addQueryableMapping(
                "Format",
                "gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:name/gco:CharacterString");
        addQueryableMapping(
                "FormatVersion",
                "gmd:distributionInfo/gmd:MD_Distribution/gmd:distributionFormat/gmd:MD_Format/gmd:version/gco:CharacterString");
        addQueryableMapping("HierarchyLevelName", "gmd:hierarchyLevelName/gco:CharacterString");
        addQueryableMapping("Language", "gmd:language/gco:CharacterString");
        addQueryableMapping(
                "Lineage",
                "gmd:dataQualityInfo/gmd:DQ_DataQuality/gmd:lineage/gmd:LI_Lineage/gmd:statement/gco:CharacterString");
        addQueryableMapping(
                "MetadataCharacterSet", "gmd:characterSet/gmd:MD_ScopeCode/@codeListValue");
        addQueryableMapping("MetadataStandardName", "gmd:metadataStandardName/gco:CharacterString");
        addQueryableMapping(
                "MetadataStandardVersion", "gmd:metadataStandardVersion/gco:CharacterString");
        addQueryableMapping("Modified", "gmd:dateStamp/gmd:Date");
        addQueryableMapping(
                "OnlineResource",
                "gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOption/gmd:onLine/gmd:CI_OnlineResource/gmd:linkage/gmd:URL/gco:CharacterString");
        addQueryableMapping("ParentIdentifier", "gmd:parentIdentifier/gco:CharacterString");
        addQueryableMapping(
                "Publisher",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:pointOfContact/gmd:CI_ResponsibleParty/gmd:organisationName[role/gmd:CI_RoleCode/gmd:@codeListValue=’publisher’]/gco:CharacterString");
        addQueryableMapping(
                "ResourceLanguage",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:language/gco:CharacterString");
        addQueryableMapping(
                "ReferenceSystem",
                "gmd:referenceSystemInfo/gmd:MD_ReferenceSystem/gmd:referenceSystemIdentifier/gmd:RS_Identifier/gco:CharacterString");
        addQueryableMapping(
                "RevisionDate",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:citation/gmd:CI_Citation/gmd:date/gmd:CI_Date[dateType/gmd:CI_DateTypeCode/gmd:@codeListValue='revision']/gmd:date/gmd:Date");
        addQueryableMapping(
                "Rights",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:resourceConstraints/gmd:MD_LegalConstraints/gmd:accessConstraints/@codeListValue");
        addQueryableMapping(
                "ServiceOperation",
                "gmd:identificationInfo/gmd:SV_ServiceIdentification/gmd:containsOperations/gmd:SV_OperationMetadata/gco:CharacterString");
        addQueryableMapping(
                "SpatialResolution",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialResolution/gco:CharacterString");
        addQueryableMapping(
                "SpatialRepresentationType",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:spatialRepresentationType/gmd:MD_SpatialRepresentationTypeCode/@codeListValue");
        addQueryableMapping(
                "TopicCategory",
                "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:topicCategory/gco:CharacterString");
        // required
        addQueryableMapping(
                "Contact",
                "gmd:contact/gmd:CI_ResponsibleParty/gmd:individualName/gco:CharacterString");
    }

    /** Helper method to add Mapping in to Queryables */
    protected static void addQueryableMapping(String key, String path) {
        QUERYABLE_MAPPING.put(key, ff.property(path, NAMESPACES));
    }

    @Override
    public String getOutputSchema() {
        return NAMESPACE_GMD;
    }

    @Override
    public List<Name> getPropertiesForElementSet(ElementSetType elementSet) {
        switch (elementSet) {
            case BRIEF:
                return MetaDataDescriptor.BRIEF_ELEMENTS;
            case SUMMARY:
                return MetaDataDescriptor.SUMMARY_ELEMENTS;
            default:
                return null;
        }
    }

    @Override
    public NamespaceSupport getNamespaceSupport() {
        return NAMESPACES;
    }

    @Override
    public Query adaptQuery(Query query) {
        Filter filter = query.getFilter();
        if (filter != null && !Filter.INCLUDE.equals(filter)) {
            query.setFilter((Filter) filter.accept(new MDQueryableFilterVisitor(), null));
        }

        SortBy[] sortBy = query.getSortBy();
        if (sortBy != null && sortBy.length > 0) {
            for (int i = 0; i < sortBy.length; i++) {
                SortBy sb = sortBy[i];
                if (!SortBy.NATURAL_ORDER.equals(sb) && !SortBy.REVERSE_ORDER.equals(sb)) {
                    sortBy[i] =
                            new SortByImpl(
                                    MDQueryableFilterVisitor.property(sb.getPropertyName()),
                                    sb.getSortOrder());
                }
            }
            query.setSortBy(sortBy);
        }

        return query;
    }

    @Override
    public AttributeDescriptor getFeatureDescriptor() {
        return METADATA_DESCRIPTOR;
    }

    @Override
    public String getBoundingBoxPropertyName() {
        return "identificationInfo.MD_DataIdentification.extent.EX_Extent.geographicElement.EX_GeographicBoundingBox";
    }

    @Override
    public List<Name> getQueryables() {
        return QUERYABLES;
    }

    @Override
    public String getQueryablesDescription() {
        return "SupportedISOQueryables";
    }

    @Override
    public PropertyName translateProperty(Name name) {
        return QUERYABLE_MAPPING.get(name.getLocalPart());
    }

    public void verifySpatialFilters(Filter filter) {
        filter.accept(new SpatialFilterChecker(getFeatureType()), null);
    }

    // singleton

    private MetaDataDescriptor() {}

    private static MetaDataDescriptor INSTANCE;

    public static MetaDataDescriptor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = GeoServerExtensions.bean(MetaDataDescriptor.class);
            if (INSTANCE == null) {
                INSTANCE = new MetaDataDescriptor();
            }
        }
        return INSTANCE;
    }
}
