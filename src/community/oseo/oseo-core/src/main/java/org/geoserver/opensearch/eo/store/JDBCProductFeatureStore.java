/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess.FF;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.COLLECTION_PROPERTY_NAME;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.EO_IDENTIFIER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.featurestemplating.builders.JSONFieldSupport;
import org.geoserver.ows.LocalWorkspace;
import org.geotools.api.data.Join;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.logging.Logging;

/**
 * Maps joined simple features up to a complex Collection feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCProductFeatureStore extends AbstractMappingStore {

    static final Logger LOGGER = Logging.getLogger(JDBCProductFeatureStore.class);

    String granuleForeignKey;

    /** The list of properties that come from JSONB fields and will need to be sorted by key */
    Set<Name> jsonBProperties;

    public JDBCProductFeatureStore(JDBCOpenSearchAccess openSearchAccess, FeatureType schema)
            throws IOException {
        super(openSearchAccess, schema);
        jsonBProperties =
                schema.getDescriptors().stream()
                        .filter(JSONFieldSupport::isJSONBField)
                        .map(ad -> ad.getName())
                        .collect(Collectors.toSet());
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

    @Override
    public SimpleFeatureSource getDelegateSource() throws IOException {
        WorkspaceInfo workspaceInfo = LocalWorkspace.get();
        SimpleFeatureSource delegate =
                openSearchAccess.getDelegateStore().getFeatureSource(JDBCOpenSearchAccess.PRODUCT);
        return new WorkspaceFeatureSource(delegate, workspaceInfo, openSearchAccess);
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

        // join to quicklook table if necessary (use an outer join as it might be missing)
        if (addJoins && hasOutputProperty(query, OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, false)) {
            Filter filter = FF.equal(FF.property("id"), FF.property("quicklook.tid"), true);
            Join join = new Join("product_thumb", filter);
            join.setType(Join.Type.OUTER);
            join.setAlias("quicklook");
            result.getJoins().add(join);
        }

        return result;
    }

    @Override
    protected void mapPropertiesToComplex(
            ComplexFeatureBuilder builder, SimpleFeature fi, Map<String, Object> mapperState) {
        // JSONB Keys are Unsorted, so we sort them here
        jsonBProperties.stream()
                .filter(n -> fi.getAttribute(n) != null && fi.getAttribute(n) instanceof String)
                .forEach(n -> sortJSONBKeys(fi, n));
        // basic mappings
        super.mapPropertiesToComplex(builder, fi, mapperState);

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

        // collection extraction
        String parentIdentifier = (String) fi.getAttribute("eoParentIdentifier");
        if (parentIdentifier != null) {
            Feature collectionFeature = getCollectionFeature(mapperState, parentIdentifier);
            builder.append(COLLECTION_PROPERTY_NAME, collectionFeature);
        }
    }

    private Feature getCollectionFeature(Map<String, Object> mapperState, String parentIdentifier) {
        Feature collectionFeature = (Feature) mapperState.get(parentIdentifier);
        if (collectionFeature == null) {
            try {
                JDBCCollectionFeatureStore collectionSource =
                        (JDBCCollectionFeatureStore)
                                ((JDBCOpenSearchAccess) getDataStore()).getCollectionSource();
                Query q = new Query(Query.ALL);
                q.setFilter(FF.equals(FF.property(EO_IDENTIFIER), FF.literal(parentIdentifier)));
                Feature first = DataUtilities.first(collectionSource.getFeatures(q));

                // remap the feature, it's using the wrong namespace
                collectionFeature = remapCollectionToEONamespace(parentIdentifier, first);

                mapperState.put(parentIdentifier, collectionFeature);
            } catch (IOException e) {
                // not impossible, but unexpected
                throw new RuntimeException(e);
            }
        }
        return collectionFeature;
    }

    /**
     * The collection feature is generated in the store provided namespaceURI, but the collection
     * property in the product uses the EO namespace instead (to have a stable namespace for usage
     * in JSON templates). So we need to remap, cannot have a feature with a namespaceURI different
     * from the one of its type descriptor....
     */
    private Feature remapCollectionToEONamespace(String parentIdentifier, Feature first) {
        FeatureType collectionType =
                (FeatureType) getSchema().getDescriptor(COLLECTION_PROPERTY_NAME).getType();
        ComplexFeatureBuilder cb = new ComplexFeatureBuilder(collectionType, FEATURE_FACTORY);
        for (Property p : first.getProperties()) {
            if (p instanceof Feature) {
                cb.append(p.getName(), p);
            } else {
                cb.append(p.getName().getLocalPart(), p.getValue());
            }
        }
        Feature feature = cb.buildFeature(parentIdentifier);
        return feature;
    }

    private static void sortJSONBKeys(SimpleFeature fi, Name n) {
        try {
            // convert from string to JSON
            JsonNode sortedJsonNode =
                    JSONFieldSupport.SORT_BY_KEY_MAPPER.readTree((String) fi.getAttribute(n));
            // convert back to string and set the attribute
            fi.setAttribute(n, sortedJsonNode.toString());
        } catch (JsonProcessingException e) {
            LOGGER.log(
                    java.util.logging.Level.WARNING,
                    "Error sorting JSONB field, could not parse JSON from field: " + n,
                    e);
        }
    }

    @Override
    protected void removeChildFeatures(List<String> collectionIdentifiers) throws IOException {
        super.removeChildFeatures(collectionIdentifiers);

        // remove thumbnail
        List<Filter> filters =
                collectionIdentifiers.stream()
                        .map(id -> FF.equal(FF.property("tid"), FF.literal(id), false))
                        .collect(Collectors.toList());
        Filter metadataFilter = FF.or(filters);
        SimpleFeatureStore thumbStore = getFeatureStoreForTable("product_thumb");
        thumbStore.setTransaction(getTransaction());
        thumbStore.removeFeatures(metadataFilter);

        // remove granules
        filters =
                collectionIdentifiers.stream()
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
