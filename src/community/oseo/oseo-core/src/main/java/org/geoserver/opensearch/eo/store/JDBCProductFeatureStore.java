/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess.FF;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.data.Join;
import org.geotools.data.Query;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

/**
 * Maps joined simple features up to a complex Collection feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCProductFeatureStore extends AbstractMappingStore {

    static final Logger LOGGER = Logging.getLogger(JDBCProductFeatureStore.class);

    String granuleForeignKey;

    public JDBCProductFeatureStore(
            JDBCOpenSearchAccess openSearchAccess, FeatureType collectionFeatureType)
            throws IOException {
        super(openSearchAccess, collectionFeatureType);

        for (AttributeDescriptor ad :
                getFeatureStoreForTable("granule").getSchema().getAttributeDescriptors()) {
            if (ad.getLocalName().equalsIgnoreCase("product_id")) {
                granuleForeignKey = ad.getLocalName();
            }
        }
        if (granuleForeignKey == null) {
            throw new IllegalStateException(
                    "Could not locate a column named 'product'_id in table 'granule'");
        }
    }

    protected SimpleFeatureSource getDelegateCollectionSource() throws IOException {
        return openSearchAccess.getDelegateStore().getFeatureSource(JDBCOpenSearchAccess.PRODUCT);
    }

    @Override
    protected String getMetadataTable() {
        return "product_metadata";
    }

    @Override
    protected String getLinkTable() {
        return "product_ogclink";
    }

    @Override
    protected String getLinkForeignKey() {
        return "product_id";
    }

    @Override
    protected Query mapToSimpleCollectionQuery(Query query, boolean addJoins) throws IOException {
        Query result = super.mapToSimpleCollectionQuery(query, addJoins);

        // join to quicklook table if necessary
        if (addJoins && hasOutputProperty(query, OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, false)) {
            Filter filter = FF.equal(FF.property("id"), FF.property("quicklook.tid"), true);
            Join join = new Join("product_thumb", filter);
            join.setAlias("quicklook");
            result.getJoins().add(join);
        }

        return result;
    }

    @Override
    protected void mapPropertiesToComplex(ComplexFeatureBuilder builder, SimpleFeature fi) {
        // basic mappings
        super.mapPropertiesToComplex(builder, fi);

        // quicklook extraction
        Object metadataValue = fi.getAttribute("quicklook");
        if (metadataValue instanceof SimpleFeature) {
            SimpleFeature quicklookFeature = (SimpleFeature) metadataValue;
            AttributeBuilder ab = new AttributeBuilder(CommonFactoryFinder.getFeatureFactory(null));
            ab.setDescriptor(
                    (AttributeDescriptor)
                            schema.getDescriptor(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME));
            Attribute attribute = ab.buildSimple(null, quicklookFeature.getAttribute("thumb"));
            builder.append(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, attribute);
        }
    }

    @Override
    protected void removeChildFeatures(List<String> collectionIdentifiers) throws IOException {
        super.removeChildFeatures(collectionIdentifiers);

        // remove thumbnail
        List<Filter> filters =
                collectionIdentifiers
                        .stream()
                        .map(id -> FF.equal(FF.property("tid"), FF.literal(id), false))
                        .collect(Collectors.toList());
        Filter metadataFilter = FF.or(filters);
        SimpleFeatureStore thumbStore = getFeatureStoreForTable("product_thumb");
        thumbStore.setTransaction(getTransaction());
        thumbStore.removeFeatures(metadataFilter);

        // remove granules
        filters =
                collectionIdentifiers
                        .stream()
                        .map(id -> FF.equal(FF.property(granuleForeignKey), FF.literal(id), false))
                        .collect(Collectors.toList());
        Filter granulesFilter = FF.or(filters);
        SimpleFeatureStore granuleStore = getFeatureStoreForTable("granule");
        granuleStore.setTransaction(getTransaction());
        granuleStore.removeFeatures(granulesFilter);
    }

    @Override
    protected boolean modifySecondaryAttribute(Name name, Object value, Filter mappedFilter)
            throws IOException {
        if (OpenSearchAccess.GRANULES.equals(name.getLocalPart())) {
            final String tableName = "granule";
            modifySecondaryTable(
                    mappedFilter,
                    value,
                    tableName,
                    id -> FF.equal(FF.property("product_id"), FF.literal(id), true),
                    (id, granulesStore) -> {
                        SimpleFeatureCollection granules = (SimpleFeatureCollection) value;
                        SimpleFeatureBuilder fb =
                                new SimpleFeatureBuilder(granulesStore.getSchema());
                        ListFeatureCollection mappedGranules =
                                new ListFeatureCollection(granulesStore.getSchema());
                        granules.accepts(
                                f -> {
                                    SimpleFeature sf = (SimpleFeature) f;
                                    for (AttributeDescriptor ad :
                                            granulesStore.getSchema().getAttributeDescriptors()) {
                                        fb.set(
                                                ad.getLocalName(),
                                                sf.getAttribute(ad.getLocalName()));
                                    }
                                    fb.set("the_geom", sf.getDefaultGeometry());
                                    fb.set("product_id", id);
                                    SimpleFeature mappedGranule = fb.buildFeature(null);
                                    mappedGranules.add(mappedGranule);
                                },
                                null);
                        return mappedGranules;
                    });

            // this one has been handled
            return true;
        }

        return false;
    }

    @Override
    protected String getThumbnailTable() {
        return "product_thumb";
    }
}
