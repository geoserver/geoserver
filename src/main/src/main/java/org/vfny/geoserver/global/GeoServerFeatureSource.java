/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ProjectionPolicy;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.SchemaException;
import org.geotools.feature.collection.MaxSimpleFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.filter.spatial.DefaultCRSFilterVisitor;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.geotools.util.factory.Hints.ConfigurationMetadataKey;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.OperationNotFoundException;
import org.opengis.referencing.operation.TransformException;

/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>Support FeatureSource decorator for FeatureTypeInfo that takes care of mapping the
 * FeatureTypeInfo's FeatureSource with the schema and definition query configured for it.
 *
 * <p>Because GeoServer requires that attributes always be returned in the same order we need a way
 * to smoothly inforce this. Could we use this class to do so?
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GeoServerFeatureSource implements SimpleFeatureSource {
    /** Shared package logger */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.global");

    /** FeatureSource being served up */
    protected SimpleFeatureSource source;

    /** The single filter factory for this source (grabbing it has a high sync penalty */
    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    /**
     * GeoTools2 Schema information
     *
     * <p>Is this the same as source.getSchema() or is it used supply the order that GeoServer
     * requires attributes to be returned in?
     */
    protected SimpleFeatureType schema;

    /** Used to constrain the Feature made available to GeoServer. */
    protected Filter definitionQuery = Filter.INCLUDE;

    /** Geometries will be forced to this CRS (or null, if no forcing is needed) */
    protected CoordinateReferenceSystem declaredCRS;

    /** How to handle SRS */
    protected ProjectionPolicy srsHandling;

    /** FeatureTypeInfo metadata to pass to extensions within the Query * */
    protected MetadataMap metadata;

    /**
     * Distance used for curve linearization tolerance, as an absolute value expressed in the data
     * native CRS
     */
    protected Double linearizationTolerance;

    /**
     * Creates a new GeoServerFeatureSource object.
     *
     * @param source GeoTools2 FeatureSource
     * @param schema SimpleFeatureType returned by this FeatureSource
     * @param definitionQuery Filter used to limit results
     * @param declaredCRS Geometries will be forced or projected to this CRS
     * @param linearizationTolerance Distance used for curve linearization tolerance, as an absolute
     *     value expressed in the data native CRS
     */
    GeoServerFeatureSource(
            FeatureSource<SimpleFeatureType, SimpleFeature> source,
            SimpleFeatureType schema,
            Filter definitionQuery,
            CoordinateReferenceSystem declaredCRS,
            int srsHandling,
            Double linearizationTolerance,
            MetadataMap metadata) {
        this(
                source,
                new Settings(
                        schema,
                        definitionQuery,
                        declaredCRS,
                        srsHandling,
                        linearizationTolerance,
                        metadata));
    }

    /**
     * Creates a new GeoServerFeatureSource object.
     *
     * @param source GeoTools2 FeatureSource
     * @param settings Settings for this source
     */
    GeoServerFeatureSource(
            FeatureSource<SimpleFeatureType, SimpleFeature> source, Settings settings) {
        this.source = DataUtilities.simple(source);
        this.schema = settings.schema;
        this.definitionQuery = settings.definitionQuery;
        this.declaredCRS = settings.declaredCRS;
        this.srsHandling = ProjectionPolicy.get(settings.srsHandling);
        this.linearizationTolerance = settings.linearizationTolerance;
        this.metadata = settings.metadata;

        if (this.definitionQuery == null) {
            this.definitionQuery = Filter.INCLUDE;
        }
    }

    /**
     * Returns the same name than the feature type (ie, {@code getSchema().getName()} to honor the
     * simple feature land common practice of calling the same both the Features produces and their
     * types
     *
     * @since 1.7
     * @see FeatureSource#getName()
     */
    public Name getName() {
        return getSchema().getName();
    }

    /**
     * Factory that make the correct decorator for the provided featureSource.
     *
     * <p>This factory method is public and will be used to create all required subclasses. By
     * comparison the constructors for this class have package visibility.
     *
     * @param settings Settings for this store
     */
    public static GeoServerFeatureSource create(
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource, Settings settings) {
        if (featureSource instanceof FeatureLocking) {
            return new GeoServerFeatureLocking(
                    (FeatureLocking<SimpleFeatureType, SimpleFeature>) featureSource, settings);
        } else if (featureSource instanceof FeatureStore) {
            return new GeoServerFeatureStore(
                    (FeatureStore<SimpleFeatureType, SimpleFeature>) featureSource, settings);
        }

        return new GeoServerFeatureSource(featureSource, settings);
    }

    /**
     * Takes a query and adapts it to match re definitionQuery filter configured for a feature type.
     *
     * @param query Query against this DataStore
     * @param schema TODO
     * @return Query restricted to the limits of definitionQuery
     * @throws IOException See DataSourceException
     * @throws DataSourceException If query could not meet the restrictions of definitionQuery
     */
    protected Query makeDefinitionQuery(Query query, SimpleFeatureType schema) throws IOException {
        if (definitionQuery == null && linearizationTolerance == null) {
            return query;
        }

        try {
            String[] propNames = extractAllowedAttributes(query, schema);
            Filter filter = query.getFilter();
            filter = makeDefinitionFilter(filter);

            Query defQuery = new Query(query);
            defQuery.setFilter(filter);
            defQuery.setPropertyNames(propNames);
            defQuery.setCoordinateSystem(query.getCoordinateSystem());
            // set sort by
            if (query.getSortBy() != null) {
                defQuery.setSortBy(query.getSortBy());
            }

            // tell the data sources about the default linearization tolerance for curved
            // geometries they might be reading
            if (linearizationTolerance != null) {
                query.getHints().put(Hints.LINEARIZATION_TOLERANCE, linearizationTolerance);
            }

            return defQuery;
        } catch (Exception ex) {
            throw new DataSourceException(
                    "Could not restrict the query to the definition criteria: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * List of allowed attributes.
     *
     * <p>Creates a list of FeatureTypeInfo's attribute names based on the attributes requested by
     * <code>query</code> and making sure they not contain any non exposed attribute.
     *
     * <p>Exposed attributes are those configured in the "attributes" element of the
     * FeatureTypeInfo's configuration
     *
     * @param query User's origional query
     * @param schema TODO
     * @return List of allowed attribute types
     */
    private String[] extractAllowedAttributes(Query query, SimpleFeatureType schema) {
        String[] propNames = null;

        if (query.retrieveAllProperties()) {
            List<String> props = new ArrayList();

            for (int i = 0; i < schema.getAttributeCount(); i++) {
                AttributeDescriptor att = schema.getDescriptor(i);

                // if this is a joined attribute, don't include it
                // TODO: make this a better check, actually verify it vs the query object
                if (Feature.class.isAssignableFrom(att.getType().getBinding())
                        && !query.getJoins().isEmpty()) {
                    continue;
                }

                props.add(att.getLocalName());
            }
            propNames = props.toArray(new String[props.size()]);
        } else {
            String[] queriedAtts = query.getPropertyNames();
            int queriedAttCount = queriedAtts.length;
            List allowedAtts = new LinkedList();

            for (int i = 0; i < queriedAttCount; i++) {
                if (schema.getDescriptor(queriedAtts[i]) != null) {
                    allowedAtts.add(queriedAtts[i]);
                } else {
                    LOGGER.info(
                            "queried a not allowed property: "
                                    + queriedAtts[i]
                                    + ". Ommitting it from query");
                }
            }

            propNames = (String[]) allowedAtts.toArray(new String[allowedAtts.size()]);
        }

        return propNames;
    }

    /**
     * If a definition query has been configured for the FeatureTypeInfo, makes and return a new
     * Filter that contains both the query's filter and the layer's definition one, by logic AND'ing
     * them.
     *
     * @param filter Origional user supplied Filter
     * @return Filter adjusted to the limitations of definitionQuery
     * @throws DataSourceException If the filter could not meet the limitations of definitionQuery
     */
    protected Filter makeDefinitionFilter(Filter filter) throws DataSourceException {
        Filter newFilter = filter;

        try {
            if (definitionQuery == Filter.INCLUDE) {
                return filter;
            }
            SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
            Filter simplifiedDefinitionQuery = (Filter) definitionQuery.accept(visitor, null);
            if (filter == Filter.INCLUDE) {
                newFilter = simplifiedDefinitionQuery;
            } else if (simplifiedDefinitionQuery != Filter.INCLUDE) {
                // expand eventual env vars before hitting the store machinery
                newFilter = ff.and(simplifiedDefinitionQuery, filter);
            }
        } catch (Exception ex) {
            throw new DataSourceException("Can't create the definition filter", ex);
        }

        return newFilter;
    }

    /**
     * Implement getDataStore.
     *
     * <p>Description ...
     *
     * @see org.geotools.data.FeatureSource#getDataStore()
     */
    public DataStore getDataStore() {
        return (DataStore) source.getDataStore();
    }

    /**
     * Implement addFeatureListener.
     *
     * <p>Description ...
     *
     * @see org.geotools.data.FeatureSource#addFeatureListener(org.geotools.data.FeatureListener)
     */
    public void addFeatureListener(FeatureListener listener) {
        source.addFeatureListener(listener);
    }

    /**
     * Implement removeFeatureListener.
     *
     * <p>Description ...
     *
     * @see org.geotools.data.FeatureSource#removeFeatureListener(org.geotools.data.FeatureListener)
     */
    public void removeFeatureListener(FeatureListener listener) {
        source.removeFeatureListener(listener);
    }

    /**
     * Implement getFeatures.
     *
     * <p>Description ...
     *
     * @see org.geotools.data.FeatureSource#getFeatures(org.geotools.data.Query)
     */
    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
        // check for a sort in the query, if the underlying store does not do sorting
        // then we need to apply it after the fact
        SortBy[] sortBy = query.getSortBy();
        Integer offset = null, maxFeatures = null;
        if (sortBy != null && sortBy != SortBy.UNSORTED) {
            if (!source.getQueryCapabilities().supportsSorting(sortBy)) {
                query.setSortBy(null);

                // if paging is in and we cannot do sorting natively
                // we should not let the datastore handle it: we need to sort first, then
                // page on it
                offset = query.getStartIndex();
                maxFeatures =
                        query.getMaxFeatures() == Integer.MAX_VALUE ? null : query.getMaxFeatures();

                query.setStartIndex(null);
                query.setMaxFeatures(Query.DEFAULT_MAX);
            } else {
                sortBy = null;
            }
        }

        // check for an offset in the query, if the underlying store does not do offsets then
        // we need to apply it after the fact along with max features
        if (query.getStartIndex() != null) {
            if (!source.getQueryCapabilities().isOffsetSupported()) {
                offset = query.getStartIndex();
                maxFeatures =
                        query.getMaxFeatures() == Integer.MAX_VALUE ? null : query.getMaxFeatures();

                query.setStartIndex(null);
                query.setMaxFeatures(Query.DEFAULT_MAX);
            }
        }

        Query reprojected = reprojectFilter(query);
        Query newQuery = adaptQuery(reprojected, schema);

        // Merge configuration metadata into query hints. This ensures that all
        // metadata for a particular FeatureType is available in the actual data store.
        // All String keys in the featuretype.xml metadata will be transformed into
        // ConfigurationMetadataKey instances
        for (Entry<String, Serializable> e : metadata.entrySet()) {
            try {
                ConfigurationMetadataKey key = ConfigurationMetadataKey.get(e.getKey());
                newQuery.getHints().put(key, e.getValue());
            } catch (IllegalArgumentException ignore) {
                LOGGER.fine("Hint " + e.getKey() + ": " + ignore);
            }
        }

        CoordinateReferenceSystem targetCRS = query.getCoordinateSystemReproject();
        try {
            // this is the raw "unprojected" feature collection
            SimpleFeatureCollection fc = source.getFeatures(newQuery);

            // apply sorting if necessary
            if (sortBy != null && sortBy != SortBy.UNSORTED) {
                fc = new SortedSimpleFeatureCollection(fc, sortBy);
            }

            // apply limit offset if necessary
            if (offset != null || maxFeatures != null) {
                fc =
                        new MaxSimpleFeatureCollection(
                                fc,
                                offset == null ? 0 : offset,
                                maxFeatures == null ? Integer.MAX_VALUE : maxFeatures);
            }

            // apply reprojection
            return applyProjectionPolicies(targetCRS, fc);
        } catch (Exception e) {
            throw new DataSourceException(e);
        }
    }

    private Query reprojectFilter(Query query) throws IOException {
        SimpleFeatureType nativeFeatureType = source.getSchema();
        final GeometryDescriptor geom = nativeFeatureType.getGeometryDescriptor();
        // if no geometry involved, no reprojection needed
        if (geom == null) return query;

        try {
            // default CRS: the CRS we can assume geometry and bbox elements in filter are
            // that is, usually the declared one, but the native one in the leave case
            CoordinateReferenceSystem defaultCRS = null;
            // we need to reproject all bbox and geometries to a target crs, which is
            // the native one usually, but it's the declared on in the force case (since in
            // that case we completely ignore the native one)
            CoordinateReferenceSystem nativeCRS = geom.getCoordinateReferenceSystem();

            if (srsHandling == ProjectionPolicy.NONE
                    && metadata.get(FeatureTypeInfo.OTHER_SRS) != null) {
                // a feature type with multiple native srs (cascaded feature from WFS-NG or
                // WMSStore)
                // and policy is set to keep native
                // do not re-project at query
                defaultCRS = source.getInfo().getCRS();
                query.setCoordinateSystem(declaredCRS);
                return query;
            } else if (srsHandling == ProjectionPolicy.FORCE_DECLARED) {
                defaultCRS = declaredCRS;
                nativeFeatureType = FeatureTypes.transform(nativeFeatureType, declaredCRS);
            } else if (srsHandling == ProjectionPolicy.REPROJECT_TO_DECLARED) {
                defaultCRS = declaredCRS;
            } else { // FeatureTypeInfo.LEAVE
                defaultCRS = nativeCRS;
            }

            // now we apply a default to all geometries and bbox in the filter
            DefaultCRSFilterVisitor defaultCRSVisitor = new DefaultCRSFilterVisitor(ff, defaultCRS);
            Filter filter = query.getFilter() != null ? query.getFilter() : Filter.INCLUDE;
            Filter defaultedFilter = (Filter) filter.accept(defaultCRSVisitor, null);

            // and then we reproject all geometries so that the datastore receives
            // them in the native projection system (or the forced one, in case of force)
            ReprojectingFilterVisitor reprojectingVisitor =
                    new ReprojectingFilterVisitor(ff, nativeFeatureType);
            Filter reprojectedFilter = (Filter) defaultedFilter.accept(reprojectingVisitor, null);

            Query reprojectedQuery = new Query(query);
            reprojectedQuery.setFilter(reprojectedFilter);
            reprojectedQuery.setCoordinateSystem(declaredCRS);
            return reprojectedQuery;
        } catch (Exception e) {
            throw new DataSourceException("Had troubles handling filter reprojection...", e);
        }
    }

    /**
     * Wraps feature collection as needed in order to respect the current projection policy and the
     * target CRS, if any (can be null, in that case only the projection policy is applied)
     */
    protected SimpleFeatureCollection applyProjectionPolicies(
            CoordinateReferenceSystem targetCRS, SimpleFeatureCollection fc)
            throws IOException, SchemaException, TransformException, OperationNotFoundException,
                    FactoryException {
        if (fc.getSchema().getGeometryDescriptor() == null) {
            // reprojection and crs forcing do not make sense, bail out
            return fc;
        }
        CoordinateReferenceSystem nativeCRS =
                fc.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();

        if (nativeCRS == null) {
            if (declaredCRS != null) {
                fc = new ForceCoordinateSystemFeatureResults(fc, declaredCRS);
                nativeCRS = declaredCRS;
            }
        } else if (srsHandling == ProjectionPolicy.FORCE_DECLARED
                && !nativeCRS.equals(declaredCRS)) {
            fc = new ForceCoordinateSystemFeatureResults(fc, declaredCRS);
            nativeCRS = declaredCRS;
        } else if (srsHandling == ProjectionPolicy.REPROJECT_TO_DECLARED
                && targetCRS == null
                && !nativeCRS.equals(declaredCRS)) {
            fc = new ReprojectFeatureResults(fc, declaredCRS);
        }

        // was reproject specified as part of the query?
        if (targetCRS != null) {
            // reprojection is occuring
            if (nativeCRS == null) {
                // we do not know what the native crs which means we can
                // not be sure if we should reproject or not... so we go
                // ahead and reproject regardless
                fc = new ReprojectFeatureResults(fc, targetCRS);
            } else {
                // only reproject if native != target
                if (!CRS.equalsIgnoreMetadata(nativeCRS, targetCRS)) {
                    fc = new ReprojectFeatureResults(fc, targetCRS);
                }
            }
        }
        return fc;
    }

    /**
     * Transforms the query applying the definition query in this layer, removes reprojection since
     * data stores cannot be trusted
     *
     * @param schema TODO
     */
    protected Query adaptQuery(Query query, SimpleFeatureType schema) throws IOException {
        // if needed, reproject the filter to the native srs

        Query newQuery = makeDefinitionQuery(query, schema);

        //        // see if the CRS got xfered over
        //        // a. old had a CRS, new doesnt
        //        boolean requireXferCRS = (newQuery.getCoordinateSystem() == null)
        //            && (query.getCoordinateSystem() != null);
        //
        //        if ((newQuery.getCoordinateSystem() != null) && (query.getCoordinateSystem() !=
        // null)) {
        //            //b. both have CRS, but they're different
        //            requireXferCRS =
        // !(newQuery.getCoordinateSystem().equals(query.getCoordinateSystem()));
        //        }
        //
        //        if (requireXferCRS) {
        //            //carry along the CRS
        //            if (!(newQuery instanceof Query)) {
        //                newQuery = new Query(newQuery);
        //            }
        //
        //            ((Query) newQuery).setCoordinateSystem(query.getCoordinateSystem());
        //        }

        // JD: this is a huge hack... but its the only way to ensure that we
        // we get what we ask for ... which is not reprojection, since
        // datastores are unreliable in this aspect we dont know if they will
        // reproject or not.
        // AA: added force coordinate system reset as well, since we cannot
        // trust GT2 datastores there neither.
        if (newQuery.getCoordinateSystemReproject() != null) {
            newQuery.setCoordinateSystemReproject(null);
        }
        if (newQuery.getCoordinateSystem() != null
                && metadata.get(FeatureTypeInfo.OTHER_SRS) == null) {
            newQuery.setCoordinateSystem(null);
        }
        return newQuery;
    }

    public SimpleFeatureCollection getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(schema.getTypeName(), filter));
    }

    public SimpleFeatureCollection getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    /**
     * Implement getSchema.
     *
     * <p>Description ...
     *
     * @see org.geotools.data.FeatureSource#getSchema()
     */
    public SimpleFeatureType getSchema() {
        return schema;
    }

    /**
     * Retrieves the total extent of this FeatureSource.
     *
     * <p>Please note this extent will reflect the provided definitionQuery.
     *
     * @return Extent of this FeatureSource, or <code>null</code> if no optimizations exist.
     * @throws IOException If bounds of definitionQuery
     */
    public ReferencedEnvelope getBounds() throws IOException {
        // since CRS is at most forced, we don't need to change this code
        if (definitionQuery == Filter.INCLUDE) {
            return source.getBounds();
        } else {
            Query query = new Query(getSchema().getTypeName(), definitionQuery);

            return source.getBounds(query);
        }
    }

    /**
     * Retrive the extent of the Query.
     *
     * <p>This method provides access to an optimized getBounds opperation. If no optimized
     * opperation is available <code>null</code> will be returned.
     *
     * <p>You may still make use of getFeatures( Query ).getCount() which will return the correct
     * answer (even if it has to itterate through all the results to do so.
     *
     * @param query User's query
     * @return Extend of Query or <code>null</code> if no optimization is available
     * @throws IOException If a problem is encountered with source
     */
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        // since CRS is at most forced, we don't need to change this code
        try {
            query = makeDefinitionQuery(query, schema);
        } catch (IOException ex) {
            return null;
        }

        return source.getBounds(query);
    }

    /**
     * Adjust query and forward to source.
     *
     * <p>This method provides access to an optimized getCount opperation. If no optimized
     * opperation is available <code>-1</code> will be returned.
     *
     * <p>You may still make use of getFeatures( Query ).getCount() which will return the correct
     * answer (even if it has to itterate through all the results to do so).
     *
     * @param query User's query.
     * @return Number of Features for Query, or -1 if no optimization is available.
     */
    public int getCount(Query query) {
        try {
            query = makeDefinitionQuery(query, schema);
        } catch (IOException ex) {
            return -1;
        }

        try {
            return source.getCount(query);
        } catch (IOException e) {
            return 0;
        }
    }

    public Set getSupportedHints() {
        return source.getSupportedHints();
    }

    public ResourceInfo getInfo() {
        return source.getInfo();
    }

    public QueryCapabilities getQueryCapabilities() {
        // we can do both sorting and offset locally if necessary
        return new QueryCapabilitiesDecorator(source.getQueryCapabilities()) {
            @Override
            public boolean isOffsetSupported() {
                return true;
            }

            @Override
            public boolean supportsSorting(SortBy[] sortAttributes) {
                return true;
            }
        };
    }

    public static class Settings {
        protected SimpleFeatureType schema;
        protected Filter definitionQuery;
        protected CoordinateReferenceSystem declaredCRS;
        protected int srsHandling;
        protected Double linearizationTolerance;
        protected MetadataMap metadata;

        /**
         * Constructor parameter for GeoServerFeatureSource.
         *
         * @param metadata Feature type metadata
         */
        public Settings(
                SimpleFeatureType schema,
                Filter definitionQuery,
                CoordinateReferenceSystem declaredCRS,
                int srsHandling,
                Double linearizationTolerance,
                MetadataMap metadata) {
            this.schema = schema;
            this.definitionQuery = definitionQuery;
            this.declaredCRS = declaredCRS;
            this.srsHandling = srsHandling;
            this.linearizationTolerance = linearizationTolerance;
            this.metadata = metadata;
        }
    }
}
