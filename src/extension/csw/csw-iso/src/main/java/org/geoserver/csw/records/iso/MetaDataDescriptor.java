/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.csw.records.RecordFeatureTypeRegistryConfiguration;
import org.geoserver.csw.records.SpatialFilterChecker;
import org.geoserver.csw.store.internal.CatalogStoreMapping;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.csw.CSW;
import org.geotools.data.complex.feature.type.FeatureTypeRegistry;
import org.geotools.data.complex.util.EmfComplexFeatureReader;
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.feature.NameImpl;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.xsd.SchemaIndex;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes the ISO MetaData records and provides some handy constants to help building features
 * representing MD_Metadata.
 *
 * @author Niels Charlier
 */
public class MetaDataDescriptor extends QueryableMappingRecordDescriptor {

    public static final NamespaceSupport NAMESPACES;

    public static final String NAMESPACE_GFC = "http://www.isotc211.org/2005/gfc";
    public static final String NAMESPACE_GMX = "http://www.isotc211.org/2005/gmx";
    public static final String NAMESPACE_GML = "http://www.opengis.net/gml/3.2";

    public static final String NAMESPACE_GCO = "http://www.isotc211.org/2005/gco";
    public static final String NAMESPACE_GMD = "http://www.isotc211.org/2005/gmd";
    public static final String NAMESPACE_APISO = "http://www.opengis.net/cat/csw/apiso/1.0";
    public static final String NAMESPACE_SRV = "http://www.isotc211.org/2005/srv";

    public static final String NAMESPACE_XLINK = "http://www.w3.org/1999/xlink";

    public static final String QUERYABLE_BBOX = "BoundingBox";

    public static FeatureType METADATA_TYPE;
    public static AttributeDescriptor METADATA_DESCRIPTOR;

    public static final NameImpl RECORD_BBOX_NAME =
            new NameImpl(NAMESPACE_GMD, "EX_GeographicBoundingBox");

    public static final List<Name> BRIEF_ELEMENTS;
    public static final List<Name> SUMMARY_ELEMENTS;

    public static final List<Name> QUERYABLES;

    static {

        // prepare the common namespace support
        NAMESPACES = new NamespaceSupport();
        NAMESPACES.declarePrefix("", NAMESPACE_APISO);
        NAMESPACES.declarePrefix("csw", CSW.NAMESPACE);
        NAMESPACES.declarePrefix("gco", NAMESPACE_GCO);
        NAMESPACES.declarePrefix("gmd", NAMESPACE_GMD);
        NAMESPACES.declarePrefix("gmx", NAMESPACE_GMX);
        NAMESPACES.declarePrefix("xlink", NAMESPACE_XLINK);
        NAMESPACES.declarePrefix("gfc", NAMESPACE_GFC);
        NAMESPACES.declarePrefix("gml", NAMESPACE_GML);
        NAMESPACES.declarePrefix("srv", NAMESPACE_SRV);

        FeatureTypeFactory typeFactory = new FeatureTypeFactoryImpl();

        EmfComplexFeatureReader reader = EmfComplexFeatureReader.newInstance();

        SchemaIndex index = null;
        SchemaIndex indexGMX = null;
        SchemaIndex indexService = null;
        try {
            index =
                    reader.parse(
                            new URL("http://schemas.opengis.net/iso/19139/20070417/gmd/gmd.xsd"));
            indexGMX =
                    reader.parse(
                            new URL("http://schemas.opengis.net/iso/19139/20070417/gmx/gmx.xsd"));
            indexService =
                    reader.parse(
                            new URL(
                                    "http://schemas.opengis.net/iso/19139/20070417/srv/serviceMetadata.xsd"));
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
        featureTypeRegistry.addSchemas(indexGMX);
        featureTypeRegistry.addSchemas(indexService);

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
                        QUERYABLE_BBOX,
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
                        QUERYABLE_BBOX,
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
    }

    public MetaDataDescriptor(GeoServer geoServer) {
        super(geoServer);
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
    public AttributeDescriptor getFeatureDescriptor() {
        return METADATA_DESCRIPTOR;
    }

    @Override
    public String getBoundingBoxPropertyName() {
        XPathUtil.StepList steps =
                XPathUtil.steps(
                        getFeatureDescriptor(),
                        queryableMapping.get(QUERYABLE_BBOX).getPropertyName(),
                        getNamespaceSupport());

        return CatalogStoreMapping.toDotPath(steps);
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
