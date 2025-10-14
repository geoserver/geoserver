/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records.iso;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import net.opengis.cat.csw20.ElementSetType;
import org.geoserver.csw.records.AbstractRecordDescriptor;
import org.geoserver.csw.records.RecordFeatureTypeRegistryConfiguration;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.data.Query;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.FeatureTypeFactory;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.complex.feature.type.FeatureTypeRegistry;
import org.geotools.data.complex.util.EmfComplexFeatureReader;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.xsd.SchemaIndex;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Describes the ISO FeatureCatalogue records and provides some handy constants to help building features representing
 * FC_FeatureCatalogue.
 */
public class FeatureCatalogueDescriptor extends AbstractRecordDescriptor {

    public static FeatureType FEATURECATALOGUE_TYPE;
    public static AttributeDescriptor FEATURECATALOGUE_DESCRIPTOR;

    static {
        FeatureTypeFactory typeFactory = new FeatureTypeFactoryImpl();

        EmfComplexFeatureReader reader = EmfComplexFeatureReader.newInstance();

        SchemaIndex index = null;
        try {
            index = reader.parse(new URL("http://schemas.opengis.net/iso/19139/20070417/gfc/gfc.xsd"));
        } catch (IOException e) {
            // this is fatal
            throw new RuntimeException(e);
        }

        FeatureTypeRegistry featureTypeRegistry = new FeatureTypeRegistry(
                MetaDataDescriptor.NAMESPACES,
                typeFactory,
                new RecordFeatureTypeRegistryConfiguration("FC_FeatureCatalogue_Type"),
                true);

        featureTypeRegistry.addSchemas(index);

        FEATURECATALOGUE_TYPE = (FeatureType) featureTypeRegistry.getAttributeType(
                new NameImpl(MetaDataDescriptor.NAMESPACE_GFC, "FC_FeatureCatalogue_Type"));
        FEATURECATALOGUE_DESCRIPTOR = featureTypeRegistry.getDescriptor(
                new NameImpl(MetaDataDescriptor.NAMESPACE_GFC, "FC_FeatureCatalogue"), null);
    }

    @Override
    public String getOutputSchema() {
        return MetaDataDescriptor.NAMESPACE_GMD;
    }

    @Override
    public List<Name> getPropertiesForElementSet(ElementSetType elementSet) {
        switch (elementSet) {
            case BRIEF:
                return Collections.emptyList();
            case SUMMARY:
                return Collections.emptyList();
            default:
                return null;
        }
    }

    @Override
    public NamespaceSupport getNamespaceSupport() {
        return MetaDataDescriptor.NAMESPACES;
    }

    @Override
    public Query adaptQuery(Query query) {

        return query;
    }

    @Override
    public AttributeDescriptor getFeatureDescriptor() {
        return FEATURECATALOGUE_DESCRIPTOR;
    }

    @Override
    public String getBoundingBoxPropertyName() {
        return null;
    }

    @Override
    public List<Name> getQueryables() {
        return Collections.emptyList();
    }

    @Override
    public String getQueryablesDescription() {
        return null;
    }

    @Override
    public List<PropertyName> translateProperty(Name name) {
        return null;
    }

    @Override
    public void verifySpatialFilters(Filter filter) {}

    // singleton

    private FeatureCatalogueDescriptor() {}

    private static FeatureCatalogueDescriptor INSTANCE;

    public static FeatureCatalogueDescriptor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = GeoServerExtensions.bean(FeatureCatalogueDescriptor.class);
            if (INSTANCE == null) {
                INSTANCE = new FeatureCatalogueDescriptor();
            }
        }
        return INSTANCE;
    }
}
