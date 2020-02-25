/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import net.opengis.wfs.XlinkPropertyNameType;
import net.opengis.wfs20.ResultTypeType;
import net.opengis.wfs20.StoredQueryType;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.feature.TypeNameExtractingVisitor;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Lock;
import org.geoserver.wfs.request.LockFeatureRequest;
import org.geoserver.wfs.request.LockFeatureResponse;
import org.geoserver.wfs.request.Query;
import org.geoserver.wfs.request.RequestObject;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Join;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.filter.FilterCapabilities;
import org.geotools.filter.expression.AbstractExpressionVisitor;
import org.geotools.filter.v2_0.FES;
import org.geotools.filter.v2_0.FESConfiguration;
import org.geotools.filter.visitor.AbstractFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.LiteCoordinateSequenceFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.gml3.GML;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.factory.Hints;
import org.geotools.xsd.Encoder;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.LazyLoader;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Web Feature Service GetFeature operation.
 *
 * <p>This operation returns an array of {@link org.geotools.feature.FeatureCollection} instances.
 *
 * @author Rob Hranac, TOPP
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @version $Id$
 */
public class GetFeature {

    static final String GET_FEATURE_BY_ID_DEPRECATED = "urn:ogc:def:query:OGC-WFS::GetFeatureById";
    static final String GET_FEATURE_BY_ID =
            "http://www.opengis.net/def/query/OGC-WFS/0/GetFeatureById";

    /** Standard logging instance for class */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests");

    /** Describes the allowed filters we support for join queries. */
    private static final FilterCapabilities joinFilterCapabilities;

    static {
        joinFilterCapabilities = new FilterCapabilities();

        // simple comparisons
        joinFilterCapabilities.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);

        // simple comparisons
        joinFilterCapabilities.addType(PropertyIsNull.class);
        joinFilterCapabilities.addType(PropertyIsBetween.class);
        joinFilterCapabilities.addType(Id.class);
        joinFilterCapabilities.addType(IncludeFilter.class);
        joinFilterCapabilities.addType(ExcludeFilter.class);
        joinFilterCapabilities.addType(PropertyIsLike.class);

        // spatial
        joinFilterCapabilities.addType(BBOX.class);
        joinFilterCapabilities.addType(Contains.class);
        joinFilterCapabilities.addType(Crosses.class);
        joinFilterCapabilities.addType(Disjoint.class);
        joinFilterCapabilities.addType(Equals.class);
        joinFilterCapabilities.addType(Intersects.class);
        joinFilterCapabilities.addType(Overlaps.class);
        joinFilterCapabilities.addType(Touches.class);
        joinFilterCapabilities.addType(Within.class);
        joinFilterCapabilities.addType(DWithin.class);
        joinFilterCapabilities.addType(Beyond.class);

        // temporal
        joinFilterCapabilities.addType(After.class);
        joinFilterCapabilities.addType(Before.class);
        joinFilterCapabilities.addType(Begins.class);
        joinFilterCapabilities.addType(BegunBy.class);
        joinFilterCapabilities.addType(During.class);
        joinFilterCapabilities.addType(Ends.class);
        joinFilterCapabilities.addType(EndedBy.class);
        joinFilterCapabilities.addType(TContains.class);
        joinFilterCapabilities.addType(TEquals.class);

        // all logical combinations are supported too
        joinFilterCapabilities.addType(And.class);
        joinFilterCapabilities.addType(Or.class);
        joinFilterCapabilities.addType(Not.class);
    }

    /** The catalog */
    protected Catalog catalog;

    /** The wfs configuration */
    protected WFSInfo wfs;

    /** filter factory */
    protected FilterFactory2 filterFactory;

    /** stored query provider */
    StoredQueryProvider storedQueryProvider;

    /** Creates the WFS 1.0/1.1 GetFeature operation. */
    public GetFeature(WFSInfo wfs, Catalog catalog) {
        this.wfs = wfs;
        this.catalog = catalog;
    }

    /** @return The reference to the GeoServer catalog. */
    public Catalog getCatalog() {
        return catalog;
    }

    /** @return NamespaceSupport from Catalog */
    public NamespaceSupport getNamespaceSupport() {
        return new CatalogNamespaceSupport(catalog);
    }

    /** @return The reference to the WFS configuration. */
    public WFSInfo getWFS() {
        return wfs;
    }

    /** Sets the filter factory to use to create filters. */
    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }

    /** Sets the stored query provider */
    public void setStoredQueryProvider(StoredQueryProvider storedQueryProvider) {
        this.storedQueryProvider = storedQueryProvider;
    }

    public FeatureCollectionResponse run(GetFeatureRequest request) throws WFSException {
        List<Query> queries = request.getQueries();

        if (queries.isEmpty()) {
            throw new WFSException(request, "No query specified");
        }

        // WFS 2.0 validation, with locks "hits" is not allowed
        if (WFSInfo.Version.V_20.compareTo(request.getVersion()) >= 0
                && request.isLockRequest()
                && request.isResultTypeHits()) {
            throw new WFSException(
                    "GetFeatureWithLock cannot be used with result type 'hits'",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "resultType");
        }

        // stored queries, preprocess compile any stored queries into actual query objects
        boolean getFeatureById = processStoredQueries(request);
        queries = request.getQueries();

        if (request.isQueryTypeNamesUnset()) {
            expandTypeNames(request, queries, getFeatureById, getCatalog());
        }

        // locking, since WFS 2.0 there is a "some" mode that influences how the features should
        // be returned: "A value of SOME indicates that the WFS shall lock as many feature in the
        // result set as possible.
        // The response document shall only contain those features that were successfully locked
        // ..."
        String lockId = null;
        if (request.isLockRequest()) {
            LockFeatureRequest lockRequest = request.createLockRequest();
            lockRequest.setExpiry(request.getExpiry());
            lockRequest.setHandle(request.getHandle());
            if (request.isLockActionSome()) {
                lockRequest.setLockActionSome();
            } else {
                lockRequest.setLockActionAll();
            }

            for (int i = 0; i < queries.size(); i++) {
                Query query = queries.get(i);

                Lock lock = lockRequest.createLock();
                lock.setFilter(query.getFilter());
                lock.setHandle(query.getHandle());

                // TODO: joins?
                List<QName> typeNames = query.getTypeNames();
                lock.setTypeName(typeNames.get(0));
                lockRequest.addLock(lock);
            }

            LockFeature lockFeature = new LockFeature(wfs, catalog);
            lockFeature.setFilterFactory(filterFactory);

            LockFeatureResponse response = lockFeature.lockFeature(lockRequest);
            lockId = response.getLockId();

            // in this case we'll modify all queries to only return the locked features
            if (request.isLockActionSome()) {
                Filter lockedFeatureFilter = toFeatureIdFilter(response.getLockedFeatures());
                for (Query query : queries) {
                    Filter filter = query.getFilter();
                    if (filter == null || filter == Filter.INCLUDE) {
                        query.setFilter(lockedFeatureFilter);
                    } else {
                        Filter joined = Predicates.and(filter, lockedFeatureFilter);
                        query.setFilter(joined);
                    }
                }
            }
        }

        // Optimization Idea
        //
        // We should be able to reduce this to a two pass opperations.
        //
        // Pass #1 execute
        // - Attempt to Locks Fids during the first pass
        // - Also collect Bounds information during the first pass
        //
        // Pass #2 writeTo
        // - Using the Bounds to describe our FeatureCollections
        // - Iterate through FeatureResults producing GML
        //
        // And allways remember to release locks if we are failing:
        // - if we fail to aquire all the locks we will need to fail and
        //   itterate through the the FeatureSources to release the locks
        //
        BigInteger bi = request.getMaxFeatures();
        if (bi == null) {
            request.setMaxFeatures(BigInteger.valueOf(Integer.MAX_VALUE));
        }

        // take into consideration the wfs max features
        int maxFeatures = Math.min(request.getMaxFeatures().intValue(), wfs.getMaxFeatures());

        // if this is only a HITS request AND the wfs setting flag
        // hitsIgnoreMaxFeatures is set, then set the maxFeatures to be the
        // maximum supported value from geotools.  This is currently
        // the maximum value of java.lang.Integer.MAX_VALUE so it is impossible
        // to return more then this value even if there are matching values
        // without either changing geotools to use a long or paging the results.
        if (wfs.isHitsIgnoreMaxFeatures() && request.isResultTypeHits()) {
            maxFeatures = org.geotools.data.Query.DEFAULT_MAX;
        }

        // grab the view params is any
        List<Map<String, String>> viewParams = null;
        if (request.getViewParams() != null && request.getViewParams().size() > 0) {
            viewParams = request.getViewParams();
        }

        boolean isNumberMatchedSkipped = false;
        int count = 0; // should probably be long
        BigInteger totalCount = BigInteger.ZERO;

        // offset into result set in which to return features
        int totalOffset = request.getStartIndex() != null ? request.getStartIndex().intValue() : -1;
        if (totalOffset == -1
                && request.getVersion().startsWith("2")
                && (wfs.isCiteCompliant()
                        || (request.getMaxFeatures() != null
                                && request.getMaxFeatures().longValue() > 0
                                && request.isResultTypeHits()))) {
            // Strict compliance with the WFS 2.0 spec requires startindex to default to zero.
            // This is not enforced because startindex triggers sorting and reduces performance.
            // The CITE tests for WFS 2.0 do not yet exist; the CITE compliance setting is taken
            // as a request for strict(er) compliance with the WFS 2.0 spec.
            // See GEOS-5085.
            totalOffset = 0;
        }
        int offset = totalOffset;

        // feature collection size, we may need to calculate it
        // optimization: WFS 1.0 does not require count unless we have multiple query elements
        // and we are asked to perform a global limit on the results returned
        boolean calculateSize =
                !(("1.0".equals(request.getVersion()) || "1.0.0".equals(request.getVersion()))
                        && (queries.size() == 1 || maxFeatures == Integer.MAX_VALUE));

        List results = new ArrayList();
        final List<CountExecutor> totalCountExecutors = new ArrayList<CountExecutor>();
        try {
            for (int i = 0; (i < queries.size()) && (count < maxFeatures); i++) {

                Query query = queries.get(i);
                try {
                    // alias sanity check
                    if (!query.getAliases().isEmpty()) {
                        if (query.getAliases().size() != query.getTypeNames().size()) {
                            throw new WFSException(
                                    request,
                                    String.format(
                                            "Query specifies %d type names and %d "
                                                    + "aliases, must be equal",
                                            query.getTypeNames().size(),
                                            query.getAliases().size()));
                        }
                    }

                    List<FeatureTypeInfo> metas = new ArrayList();
                    for (QName typeName : query.getTypeNames()) {
                        metas.add(featureTypeInfo(typeName, request));
                    }

                    // first is the primary feature type
                    FeatureTypeInfo meta = metas.get(0);

                    // parse the requested property names and distribute among requested types
                    List<List<String>> reqPropertyNames = parsePropertyNames(query, metas);

                    NamespaceSupport ns = getNamespaceSupport();

                    // set up joins (if specified)
                    List<Join> joins = null;
                    String primaryAlias = null;
                    QName primaryTypeName = query.getTypeNames().get(0);
                    FeatureTypeInfo primaryMeta = metas.get(0);

                    // make sure filters are sane
                    //
                    // Validation of filters on non-simple feature types is not yet supported.
                    // FIXME: Support validation of filters on non-simple feature types:
                    // need to consider xpath properties and how to configure namespace prefixes in
                    // GeoTools app-schema FeaturePropertyAccessorFactory.
                    Filter filter = query.getFilter();

                    if (filter == null && metas.size() > 1) {
                        throw new WFSException(request, "Join query must specify a filter");
                    }

                    if (filter != null) {
                        if (meta.getFeatureType() instanceof SimpleFeatureType) {
                            if (metas.size() > 1) {
                                // sanitize aliases, they must not conflict with feature type names
                                // nor with their attributes
                                query = AliasedQuery.fixAliases(metas, query);
                                // the filter might have been rewritten
                                filter = query.getFilter();

                                // the join extracting visitor cannot handle negated filters,
                                // the simplifier handles most common case removing the negation,
                                // e.g., not(a < 10) -> a >= 10
                                filter = SimplifyingFilterVisitor.simplify(filter);

                                // join, need to separate the joining filter from other filters
                                JoinExtractingVisitor extractor =
                                        new JoinExtractingVisitor(metas, query.getAliases());
                                extractor.setQueriedTypes(query.getTypeNames());
                                filter.accept(extractor, null);

                                primaryAlias = extractor.getPrimaryAlias();
                                primaryMeta = extractor.getPrimaryFeatureType();
                                metas = extractor.getFeatureTypes();
                                primaryTypeName =
                                        new QName(
                                                primaryMeta.getNamespace().getURI(),
                                                primaryMeta.getName());
                                joins = extractor.getJoins();
                                if (joins.size() != metas.size() - 1) {
                                    throw new WFSException(
                                            request,
                                            String.format(
                                                    "Query specified %d types but %d "
                                                            + "join filters were found",
                                                    metas.size(), extractor.getJoins().size()));
                                }

                                // validate the filter for each join, as well as the join filter
                                for (int j = 1; j < metas.size(); j++) {
                                    Join join = joins.get(j - 1);
                                    if (!isValidJoinFilter(join.getJoinFilter())) {
                                        throw new WFSException(
                                                request,
                                                "Unable to perform join with specified join filter: "
                                                        + filter);
                                    }

                                    if (join.getFilter() != null) {
                                        validateFilter(
                                                join.getFilter(), query, metas.get(j), request);
                                    }
                                }

                                filter = extractor.getPrimaryFilter();
                                if (filter != null) {
                                    validateFilter(filter, query, primaryMeta, request);
                                }
                            } else {
                                validateFilter(filter, query, meta, request);
                            }
                        } else {
                            BBOXNamespaceSettingVisitor filterVisitor =
                                    new BBOXNamespaceSettingVisitor(ns);
                            filter.accept(filterVisitor, null);
                        }
                    }

                    List<List<PropertyName>> propNames = new ArrayList();
                    List<List<PropertyName>> allPropNames = new ArrayList();

                    for (int j = 0; j < metas.size(); j++) {
                        List<String> propertyNames = reqPropertyNames.get(j);
                        List<PropertyName> metaPropNames = null;
                        List<PropertyName> metaAllPropNames = null;
                        if (!propertyNames.isEmpty()) {

                            metaPropNames = new ArrayList<PropertyName>();

                            for (Iterator iter = propertyNames.iterator(); iter.hasNext(); ) {
                                PropertyName propName =
                                        createPropertyName((String) iter.next(), ns);

                                if (propName.evaluate(meta.getFeatureType()) == null) {
                                    String mesg =
                                            "Requested property: "
                                                    + propName
                                                    + " is "
                                                    + "not available "
                                                    + "for "
                                                    + meta.prefixedName()
                                                    + ".  ";

                                    if (meta.getFeatureType() instanceof SimpleFeatureType) {
                                        List<AttributeTypeInfo> atts = meta.attributes();
                                        List attNames = new ArrayList(atts.size());
                                        for (AttributeTypeInfo att : atts) {
                                            attNames.add(att.getName());
                                        }
                                        mesg += "The possible propertyName values are: " + attNames;
                                    }

                                    throw new WFSException(request, mesg, "InvalidParameterValue");
                                }

                                metaPropNames.add(propName);
                            }

                            // if we need to force feature bounds computation, we have to load
                            // all of the geometries, but we'll have to remove them in the
                            // returned feature type
                            if (wfs.isFeatureBounding()) {
                                metaAllPropNames = addGeometryProperties(meta, metaPropNames);
                            } else {
                                metaAllPropNames = metaPropNames;
                            }

                            // we must also include any properties that are mandatory ( even if not
                            // requested ),
                            // ie. those with minOccurs > 0
                            // only do this for simple features, complex mandatory features are
                            // handled by app-schema
                            if (meta.getFeatureType() instanceof SimpleFeatureType) {
                                metaAllPropNames =
                                        DataUtilities.addMandatoryProperties(
                                                (SimpleFeatureType) meta.getFeatureType(),
                                                metaAllPropNames);
                                metaPropNames =
                                        DataUtilities.addMandatoryProperties(
                                                (SimpleFeatureType) meta.getFeatureType(),
                                                metaPropNames);
                            }
                            // for complex features, mandatory properties need to be handled by
                            // datastore.
                        }
                        allPropNames.add(metaAllPropNames);
                        propNames.add(metaPropNames);
                    }

                    // validate sortby if present
                    List<SortBy> sortBy = query.getSortBy();
                    if (sortBy != null
                            && !sortBy.isEmpty()
                            && meta.getFeatureType() instanceof SimpleFeatureType) {
                        validateSortBy(sortBy, meta, request);
                    }

                    // load primary feature source
                    Hints hints = null;
                    if (joins != null) {
                        hints = new Hints(ResourcePool.JOINS, joins);
                    }

                    // for remote reprojection in case of WFS-NG datastore ONLY
                    if (meta.getStore()
                                            .getConnectionParameters()
                                            .get(WFSDataStoreFactory.USEDEFAULTSRS.key)
                                    != null
                            && meta.getMetadata().get(FeatureTypeInfo.OTHER_SRS) != null) {
                        // if wfs-ng datastore is NOT set to use default srs
                        // then find request SRS in OTHER_SRS list
                        if (!Boolean.valueOf(
                                meta.getStore()
                                        .getConnectionParameters()
                                        .get(WFSDataStoreFactory.USEDEFAULTSRS.key)
                                        .toString())) {
                            String otherSRS =
                                    (String) meta.getMetadata().get(FeatureTypeInfo.OTHER_SRS);
                            if (query.getSrsName() != null) {
                                if (otherSRS.contains(query.getSrsName().toString())) {
                                    if (hints == null) hints = new Hints();
                                    try {
                                        hints.put(
                                                ResourcePool.MAP_CRS,
                                                CRS.decode(query.getSrsName().toString()));
                                    } catch (NoSuchAuthorityCodeException ne) {
                                        LOGGER.log(Level.SEVERE, ne.getMessage(), ne);
                                    } catch (FactoryException e) {
                                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                                    }
                                }
                            }
                        }
                    }

                    FeatureSource<? extends FeatureType, ? extends Feature> source =
                            primaryMeta.getFeatureSource(null, hints);

                    // handle local maximum
                    int queryMaxFeatures = maxFeatures - count;
                    int metaMaxFeatures = maxFeatures(metas);
                    if (metaMaxFeatures > 0 && metaMaxFeatures < queryMaxFeatures) {
                        queryMaxFeatures = metaMaxFeatures;
                    }
                    Map<String, String> viewParam = viewParams != null ? viewParams.get(i) : null;
                    org.geotools.data.Query gtQuery =
                            toDataQuery(
                                    query,
                                    filter,
                                    offset,
                                    queryMaxFeatures,
                                    source,
                                    request,
                                    allPropNames.get(0),
                                    viewParam,
                                    joins,
                                    primaryTypeName,
                                    primaryAlias);

                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Query is " + query + "\n To gt2: " + gtQuery);
                    }

                    // allow extensions to alter the query being run
                    GetFeatureContext context =
                            new GetFeatureContext(request, meta, source, gtQuery);
                    List<GetFeatureCallback> callbacks =
                            GeoServerExtensions.extensions(GetFeatureCallback.class);
                    if (!callbacks.isEmpty()) {
                        for (GetFeatureCallback callback : callbacks) {
                            callback.beforeQuerying(context);
                        }
                        if (gtQuery != context.getQuery() && LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("Query after GetFeatureCallback changes: " + source);
                        }
                        gtQuery = context.getQuery();
                    }

                    FeatureCollection<? extends FeatureType, ? extends Feature> features =
                            getFeatures(request, source, gtQuery);

                    // For complex features, we need the targetCrs and version in scenario where we
                    // have
                    // a top level feature that does not contain a geometry(therefore no crs) and
                    // has a
                    // nested feature that contains geometry as its property.Furthermore it is
                    // possible
                    // for each nested feature to have different crs hence we need to reproject on
                    // each
                    // feature accordingly.
                    if (!(meta.getFeatureType() instanceof SimpleFeatureType)) {
                        features.getSchema().getUserData().put("targetCrs", query.getSrsName());
                        features.getSchema()
                                .getUserData()
                                .put("targetVersion", request.getVersion());
                    }

                    if (!calculateSize) {
                        // if offset was specified and we have more queries left in this request
                        // then we
                        // must calculate size in order to adjust the offset
                        calculateSize = offset > 0 && i < queries.size() - 1;
                    }

                    int size = 0;
                    if (calculateSize) {
                        size = features.size();
                    }

                    // update the count
                    count += size;

                    // collect queries required to return numberMatched/totalSize
                    // check maxFeatures and offset, if they are unset we can use the size we
                    // calculated above
                    isNumberMatchedSkipped =
                            meta.getSkipNumberMatched() && !request.isResultTypeHits();
                    if (!isNumberMatchedSkipped) {
                        if (calculateSize
                                && (queryMaxFeatures == Integer.MAX_VALUE
                                        || size < queryMaxFeatures)
                                && offset <= 0) {
                            totalCountExecutors.add(new CountExecutor(size));
                        } else {
                            org.geotools.data.Query qTotal =
                                    toDataQuery(
                                            query,
                                            filter,
                                            0,
                                            Integer.MAX_VALUE,
                                            source,
                                            request,
                                            allPropNames.get(0),
                                            viewParam,
                                            joins,
                                            primaryTypeName,
                                            primaryAlias);
                            totalCountExecutors.add(new CountExecutor(source, qTotal));
                        }
                    }

                    // if offset is present we need to check the size of this returned feature
                    // collection
                    // and adjust the offset for the next feature collection accordingly
                    if (offset > 0) {
                        if (size > 0) {
                            // features returned, offset can be set to zero
                            offset = 0;
                        } else {
                            // no features might have been because of the offset that was specified,
                            // check
                            // the size of the same query but with no offset
                            org.geotools.data.Query q2 =
                                    toDataQuery(
                                            query,
                                            filter,
                                            0,
                                            queryMaxFeatures,
                                            source,
                                            request,
                                            allPropNames.get(0),
                                            viewParam,
                                            joins,
                                            primaryTypeName,
                                            primaryAlias);

                            // int size2 = getFeatures(request, source, q2).size();
                            int size2 = source.getCount(q2);
                            if (size2 > 0) {
                                // adjust the offset for the next query
                                offset = Math.max(0, offset - size2);
                            }
                        }
                    }

                    // we may need to shave off geometries we did load only to make bounds
                    // computation happy
                    // TODO: support non-SimpleFeature geometry shaving
                    List<PropertyName> metaPropNames = propNames.get(0);
                    if (features.getSchema() instanceof SimpleFeatureType
                            && metaPropNames != null
                            && metaPropNames.size() < allPropNames.get(0).size()) {
                        String[] residualNames = new String[metaPropNames.size()];
                        Iterator<PropertyName> it = metaPropNames.iterator();
                        int j = 0;
                        while (it.hasNext()) {
                            residualNames[j] = it.next().getPropertyName();
                            j++;
                        }
                        SimpleFeatureType targetType =
                                DataUtilities.createSubType(
                                        (SimpleFeatureType) features.getSchema(), residualNames);
                        features =
                                new FeatureBoundsFeatureCollection(
                                        (SimpleFeatureCollection) features, targetType);
                    }

                    // allow encoders to grab information about this layer if needs be
                    if (primaryMeta != null) {
                        features = TypeInfoCollectionWrapper.wrap(features, primaryMeta);
                    }

                    results.add(features);
                } catch (WFSException e) {
                    // intercept and set locator to query handle if one was set, or if it simply set
                    // to GetFeature, which is the default
                    if (query.getHandle() != null
                            && (e.getLocator() == null
                                    || "GetFeature".equalsIgnoreCase(e.getLocator()))) {
                        e.setLocator(query.getHandle());
                    }
                    throw e;
                }
            }

            // total count represents the total count of the features matched for this query in
            // cases
            // where the client has limited the result set size, so we compute it lazily
            if (isNumberMatchedSkipped) {
                totalCount = BigInteger.valueOf(-1);
            } else if (count < maxFeatures && calculateSize && totalOffset == 0) {
                // optimization: if count < max features then total count == count
                // can't use this optimization for v2
                totalCount = BigInteger.valueOf(count);
            } else if (isPreComputed(totalCountExecutors)) {
                long total = getTotalCount(totalCountExecutors);
                totalCount = BigInteger.valueOf(total);
            } else {
                // ok, in this case we're forced to run the queries to discover the actual total
                // count
                // We do so lazily, not all output formats need it, leveraging the fact that
                // BigInteger
                // is not final to wrap it in a lazy loading proxy
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(BigInteger.class);
                enhancer.setCallback(
                        new LazyLoader() {

                            @Override
                            public Object loadObject() throws Exception {
                                long totalCount = getTotalCount(totalCountExecutors);
                                return BigInteger.valueOf(totalCount);
                            }
                        });
                totalCount =
                        (BigInteger)
                                enhancer.create(new Class[] {String.class}, new Object[] {"0"});
            }
        } catch (IOException e) {
            throw new WFSException(
                    request, "Error occurred getting features", e, request.getHandle());
        } catch (SchemaException e) {
            throw new WFSException(
                    request, "Error occurred getting features", e, request.getHandle());
        }

        return buildResults(
                request,
                totalOffset,
                maxFeatures,
                count,
                totalCount,
                results,
                lockId,
                getFeatureById);
    }

    /** Returns true if all count executors are given a static count value */
    private boolean isPreComputed(List<CountExecutor> totalCountExecutors) {
        for (CountExecutor q : totalCountExecutors) {
            if (!q.isCountSet()) {
                return false;
            }
        }
        return true;
    }

    private long getTotalCount(List<CountExecutor> totalCountExecutors) throws IOException {
        long totalCount = 0;
        for (CountExecutor q : totalCountExecutors) {
            int result = q.getCount();
            // if the count is unknown for one, we don't know the total,
            // period
            if (result == -1) {
                totalCount = -1;
                break;
            } else {
                totalCount += result;
            }
        }
        return totalCount;
    }

    private Filter toFeatureIdFilter(List<FeatureId> lockedFeatures) {
        if (lockedFeatures == null || lockedFeatures.isEmpty()) {
            return Filter.EXCLUDE;
        }
        Set<FeatureId> ids =
                lockedFeatures
                        .stream()
                        .map(fid -> filterFactory.featureId(fid.getID()))
                        .collect(Collectors.toSet());
        return filterFactory.id(ids);
    }

    static void expandTypeNames(
            RequestObject request, List<Query> queries, boolean getFeatureById, Catalog catalog) {
        // do a check for FeatureId filters in the queries and update the type names for the
        // queries accordingly
        for (Query q : queries) {
            if (!q.getTypeNames().isEmpty()) continue;

            if (q.getFilter() != null) {
                TypeNameExtractingVisitor v = new TypeNameExtractingVisitor(catalog);
                q.getFilter().accept(v, null);
                q.getTypeNames().addAll(v.getTypeNames());
            }

            if (q.getTypeNames().isEmpty()) {
                if (getFeatureById) {
                    // by spec, a 404 should be returned in this case
                    throw new WFSException(
                            request,
                            "Could not find feature with specified id",
                            WFSException.NOT_FOUND);
                } else {
                    String msg = "No feature types specified";
                    throw new WFSException(request, msg, ServiceException.INVALID_PARAMETER_VALUE);
                }
            }
        }
    }

    /**
     * Expands the stored queries, returns true if a single GetFeatureById stored query was found
     * (as a different GML encoding is required in that case)
     */
    protected boolean processStoredQueries(GetFeatureRequest request) {
        List queries = request.getAdaptedQueries();
        boolean foundGetFeatureById = expandStoredQueries(request, queries, storedQueryProvider);

        return queries.size() == 1 && foundGetFeatureById;
    }

    /** Replaces stored queries with actual ad-hoc queries */
    static boolean expandStoredQueries(
            RequestObject request, List queries, StoredQueryProvider storedQueryProvider) {
        boolean foundGetFeatureById = false;
        for (int i = 0; i < queries.size(); i++) {
            Object obj = queries.get(i);
            if (obj instanceof StoredQueryType) {

                if (storedQueryProvider == null) {
                    throw new WFSException(request, "Stored query not supported");
                }

                StoredQueryType sq = (StoredQueryType) obj;

                // look up the store query
                String storedQueryId = sq.getId();
                foundGetFeatureById |=
                        GET_FEATURE_BY_ID.equalsIgnoreCase(storedQueryId)
                                || GET_FEATURE_BY_ID_DEPRECATED.equals(storedQueryId);
                StoredQuery storedQuery = storedQueryProvider.getStoredQuery(storedQueryId);
                if (storedQuery == null) {
                    WFSException exception =
                            new WFSException(
                                    request,
                                    "Stored query '" + storedQueryId + "' does not" + " exist.",
                                    ServiceException.INVALID_PARAMETER_VALUE);
                    exception.setLocator("STOREDQUERY_ID");
                    throw exception;
                }

                List<net.opengis.wfs20.QueryType> compiled = storedQuery.compile(sq);
                queries.remove(i);
                queries.addAll(i, compiled);
                i += compiled.size();
            }
        }
        return foundGetFeatureById;
    }

    /** Allows subclasses to alter the result generation */
    protected FeatureCollectionResponse buildResults(
            GetFeatureRequest request,
            int offset,
            int maxFeatures,
            int count,
            BigInteger total,
            List results,
            String lockId,
            boolean getFeatureById) {

        FeatureCollectionResponse result = request.createResponse();
        result.setNumberOfFeatures(BigInteger.valueOf(count));
        result.setTotalNumberOfFeatures(total);
        result.setTimeStamp(Calendar.getInstance());
        result.setLockId(lockId);
        result.getFeature().addAll(results);
        result.setGetFeatureById(getFeatureById);

        if (offset > 0 || count < Integer.MAX_VALUE) {
            // paged request, set the values of previous and next

            // get the Request thread local since we need to know about the request, whether it is
            // GET or POST some kvp information if the former
            Request req = Dispatcher.REQUEST.get();

            // grab the original kvp params if this is a GET request
            // for POST, do nothing, make the client post the same content
            // TODO: try to encode the request as best we can in a GET request, only issue should
            // be the filter and encoding it property... especially for joins that might be
            // tricky, and it also may cause the request to be too large for a get request
            // TODO: figure out what the spec says about this...
            Map<String, String> kvp = null;
            if (req.isGet()) {
                kvp = new KvpMap(req.getRawKvp());
            } else {
                // generate kvp map from request object
                kvp = buildKvpFromRequest(request);
            }
            buildPrevNextLinks(request, offset, maxFeatures, count, result, kvp);
        }

        return result;
    }

    protected void buildPrevNextLinks(
            GetFeatureRequest request,
            int offset,
            int maxFeatures,
            int count,
            FeatureCollectionResponse result,
            Map<String, String> kvp) {
        // WFS 2.0 specific, must have a next and should point to the first result
        if (request.isResultTypeHits()
                && (request.getVersion() == null || request.getVersion().startsWith("2"))) {
            kvp = new KvpMap(kvp);
            kvp.put("RESULTTYPE", "results");
            kvp.put("STARTINDEX", "0");
        }

        // WFS 2.0 has specific requirements for hits, there is no previous link, the next
        // points to the first
        // page of results
        if (offset > 0 && !(request.isResultTypeHits() && request.getVersion().startsWith("2"))) {
            // previous

            // previous offset calculated as the current offset - maxFeatures, or 0 if this is a
            // negative value
            int prevOffset = Math.max(offset - maxFeatures, 0);
            kvp.put("startIndex", String.valueOf(prevOffset));

            // previous count should be current offset - previousOffset
            kvp.put("count", String.valueOf(offset - prevOffset));
            result.setPrevious(buildURL(request.getBaseUrl(), "wfs", kvp, URLType.SERVICE));
        }

        // don't return a next if we are at the end. But always do in WFS 2.0 HITS
        // (ie. are returning less results than requested)
        if (request.isResultTypeHits() && request.getVersion().startsWith("2")) {
            result.setNext(buildURL(request.getBaseUrl(), "wfs", kvp, URLType.SERVICE));
        } else if (count > 0 && offset > -1 && maxFeatures <= count) {
            kvp.put("startIndex", String.valueOf(offset > 0 ? offset + count : count));
            kvp.put("count", String.valueOf(maxFeatures));
            result.setNext(buildURL(request.getBaseUrl(), "wfs", kvp, URLType.SERVICE));
        }
    }

    protected KvpMap buildKvpFromRequest(GetFeatureRequest request) {

        // FILTER_LANGUAGE
        // RESOURCEID
        // BBOX
        // STOREDQUERY_ID
        KvpMap kvp = new KvpMap();

        // SERVICE
        // VERSION
        // REQUEST
        kvp.put("SERVICE", "WFS");
        kvp.put("REQUEST", "GetFeature");
        kvp.put("VERSION", request.getVersion());

        // OUTPUTFORMAT
        // RESULTTYPE
        kvp.put("OUTPUTFORMAT", request.getOutputFormat());
        kvp.put(
                "RESULTTYPE",
                request.isResultTypeHits()
                        ? ResultTypeType.HITS.name()
                        : ResultTypeType.RESULTS.name());

        // TYPENAMES
        // PROPERTYNAME
        // ALIASES
        // SRSNAME
        // FILTER
        // SORTBY
        List<Query> queries = request.getQueries();
        Query q = queries.get(0);
        if (q.getSrsName() != null) {
            kvp.put("SRSNAME", q.getSrsName().toString());
        }

        StringBuilder typeNames = new StringBuilder();
        StringBuilder propertyName = !q.getPropertyNames().isEmpty() ? new StringBuilder() : null;
        StringBuilder aliases = !q.getAliases().isEmpty() ? new StringBuilder() : null;
        StringBuilder filter =
                q.getFilter() != null && q.getFilter() != Filter.INCLUDE
                        ? new StringBuilder()
                        : null;

        encodeQueryAsKvp(q, typeNames, propertyName, aliases, filter, true);
        if (queries.size() > 1) {
            for (int i = 1; i < queries.size(); i++) {
                encodeQueryAsKvp(queries.get(i), typeNames, propertyName, aliases, filter, true);
            }
        }

        kvp.put("TYPENAMES", typeNames.toString());
        if (propertyName != null) {
            kvp.put("PROPERTYNAME", propertyName.toString());
        }
        if (aliases != null) {
            kvp.put("ALIASES", aliases.toString());
        }
        if (filter != null) {
            kvp.put("FILTER", filter.toString());
        }
        return kvp;
    }

    void encodeQueryAsKvp(
            Query q,
            StringBuilder typeNames,
            StringBuilder propertyName,
            StringBuilder aliases,
            StringBuilder filter,
            boolean useDelim) {

        // typenames
        if (useDelim) {
            typeNames.append("(");
        }
        for (QName qName : q.getTypeNames()) {
            typeNames
                    .append(qName.getPrefix())
                    .append(":")
                    .append(qName.getLocalPart())
                    .append(",");
        }
        typeNames.setLength(typeNames.length() - 1);
        if (useDelim) {
            typeNames.append(")");
        }

        // propertynames
        if (propertyName != null) {
            if (useDelim) {
                propertyName.append("(");
            }
            for (String pName : q.getPropertyNames()) {
                propertyName.append(pName).append(",");
            }
            propertyName.setLength(propertyName.length() - 1);
            if (useDelim) {
                propertyName.append(")");
            }
        }

        // aliases
        if (aliases != null) {
            if (useDelim) {
                aliases.append("(");
            }
            for (String alias : q.getAliases()) {
                aliases.append(alias).append(",");
            }
            aliases.setLength(aliases.length() - 1);
            if (useDelim) {
                aliases.append(")");
            }
        }

        // filter
        if (filter != null) {
            // TODO: check the length of the encoded filter and ensure it does not put us over the
            // edge of the limit for a GET request
            Filter f = q.getFilter();

            if (useDelim) {
                filter.append("(");
            }
            try {
                Encoder e = new Encoder(new FESConfiguration());
                e.setOmitXMLDeclaration(true);
                filter.append(e.encodeAsString(q.getFilter(), FES.Filter));
            } catch (Exception e) {
                throw new RuntimeException("Unable to encode filter " + f, e);
            }

            if (useDelim) {
                filter.append(")");
            }
        }
    }

    /**
     * Allows subclasses to poke with the feature collection extraction. The default behavior
     * attempts to wrap the feature collectio into a {@link FeatureSizeFeatureCollection}.
     */
    protected FeatureCollection<? extends FeatureType, ? extends Feature> getFeatures(
            Object request,
            FeatureSource<? extends FeatureType, ? extends Feature> source,
            org.geotools.data.Query gtQuery)
            throws IOException {
        FeatureCollection<? extends FeatureType, ? extends Feature> features =
                source.getFeatures(gtQuery);

        features = FeatureSizeFeatureCollection.wrap(features, source, gtQuery);
        return features;
    }

    /**
     * Get this query as a geotools Query.
     *
     * <p>if maxFeatures is a not positive value Query.DEFAULT_MAX will be used.
     *
     * <p>The method name is changed to toDataQuery since this is a one way conversion.
     *
     * @param maxFeatures number of features, or 0 for Query.DEFAULT_MAX
     * @return A Query for use with the FeatureSource interface
     */
    public org.geotools.data.Query toDataQuery(
            Query query,
            Filter filter,
            int offset,
            int maxFeatures,
            FeatureSource<? extends FeatureType, ? extends Feature> source,
            GetFeatureRequest request,
            List<PropertyName> props,
            Map<String, String> viewParams,
            List<Join> joins,
            QName primaryTypeName,
            String primaryAlias)
            throws WFSException {

        String wfsVersion = request.getVersion();

        if (maxFeatures <= 0) {
            maxFeatures = org.geotools.data.Query.DEFAULT_MAX;
        }

        if (filter == null) {
            filter = Filter.INCLUDE;
        } else {
            // Gentlemen, we can rebuild it. We have the technology!
            SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
            filter = (Filter) filter.accept(visitor, null);
        }

        // figure out the crs the data is in
        CoordinateReferenceSystem crs = source.getSchema().getCoordinateReferenceSystem();

        // gather declared CRS
        final FeatureTypeInfo featureTypeInfo =
                catalog.getFeatureTypeByName(
                        primaryTypeName.getPrefix(), primaryTypeName.getLocalPart());
        CoordinateReferenceSystem declaredCRS = WFSReprojectionUtil.getDeclaredCrs(crs, wfsVersion);

        // make sure every bbox and geometry that does not have an attached crs will use
        // the declared crs, and then reproject it to the native crs
        Filter transformedFilter = filter;

        if (declaredCRS != null) {
            transformedFilter =
                    WFSReprojectionUtil.normalizeFilterCRS(filter, source.getSchema(), declaredCRS);
        } else {
            // this may happen with complex features, let's try to use the feature type info CRS
            transformedFilter = buildFilterCRSFromInfo(filter, primaryTypeName, source, wfsVersion);
        }
        // evaluate reprojection on complex features case
        declaredCRS =
                replaceCRSIfComplexFeatures(source, wfsVersion, crs, featureTypeInfo, declaredCRS);

        // replace gml:boundedBy with an expression
        transformedFilter =
                (Filter)
                        transformedFilter.accept(
                                new DuplicatingFilterVisitor(filterFactory) {
                                    @Override
                                    public Object visit(PropertyName expression, Object extraData) {
                                        if (isGmlBoundedBy(expression)) {
                                            // envelope of the default geometry
                                            return filterFactory.function(
                                                    "boundedBy", filterFactory.property(""));
                                        } else {
                                            return super.visit(expression, extraData);
                                        }
                                    }

                                    @Override
                                    public Object visit(BBOX filter, Object extraData) {
                                        // BBOX must work against a propertyName, if boundedBy is
                                        // used we
                                        // need to switch to an intersects filter
                                        Expression expression1 = filter.getExpression1();
                                        if (expression1 instanceof PropertyName
                                                && isGmlBoundedBy((PropertyName) expression1)) {
                                            ReferencedEnvelope bounds =
                                                    ReferencedEnvelope.reference(
                                                            filter.getBounds());
                                            Polygon polygon = JTS.toGeometry(bounds);
                                            Function boundedBy =
                                                    filterFactory.function(
                                                            "boundedBy",
                                                            filterFactory.property(""));
                                            return filterFactory.intersects(
                                                    boundedBy, filterFactory.literal(polygon));
                                        }
                                        return super.visit(filter, extraData);
                                    }
                                },
                                null);

        // only handle non-joins for now
        QName typeName = primaryTypeName;
        org.geotools.data.Query dataQuery =
                new org.geotools.data.Query(
                        typeName.getLocalPart(),
                        transformedFilter,
                        maxFeatures,
                        props,
                        query.getHandle());
        if (primaryAlias != null) {
            dataQuery.setAlias(primaryAlias);
        }

        // handle reprojection
        CoordinateReferenceSystem target;
        URI srsName = query.getSrsName();
        if (srsName != null) {
            try {
                target = CRS.decode(srsName.toString());
            } catch (Exception e) {
                String msg = "Unable to support srsName: " + srsName;
                throw new WFSException(request, msg, e, "InvalidParameterValue").locator("srsName");
            }
        } else {
            target = declaredCRS;
        }
        // if the crs are not equal, then reproject
        if (target != null && declaredCRS != null && !CRS.equalsIgnoreMetadata(crs, target)) {
            dataQuery.setCoordinateSystemReproject(target);
        }

        // handle sorting
        List<SortBy> sortBy = query.getSortBy();
        if (sortBy != null) {
            dataQuery.setSortBy(sortBy.toArray(new SortBy[sortBy.size()]));
        }

        // handle version, datastore may be able to use it
        String featureVersion = query.getFeatureVersion();
        if (featureVersion != null) {
            dataQuery.setVersion(featureVersion);
        }

        // handle offset / start index
        if (offset > -1) {
            dataQuery.setStartIndex(offset);
        }

        // create the Hints to set at the end
        final Hints hints = new Hints();

        // handle xlink traversal depth
        String traverseXlinkDepth = request.getTraverseXlinkDepth();
        if (traverseXlinkDepth != null) {
            // TODO: make this an integer in the model, and have hte NumericKvpParser
            // handle '*' as max value
            Integer depth = traverseXlinkDepth(traverseXlinkDepth);

            // set the depth as a hint on the query
            hints.put(Hints.ASSOCIATION_TRAVERSAL_DEPTH, depth);
        }

        // handle resolve parameters
        hints.put(Hints.RESOLVE, request.getResolve());
        BigInteger resolveTimeOut = request.getResolveTimeOut();
        if (resolveTimeOut != null) {
            hints.put(Hints.RESOLVE_TIMEOUT, resolveTimeOut.intValue());
        }

        // handle xlink properties
        List<XlinkPropertyNameType> xlinkProperties = query.getXlinkPropertyNames();
        if (!xlinkProperties.isEmpty()) {
            for (Iterator x = xlinkProperties.iterator(); x.hasNext(); ) {
                XlinkPropertyNameType xlinkProperty = (XlinkPropertyNameType) x.next();

                Integer xlinkDepth = traverseXlinkDepth(xlinkProperty.getTraverseXlinkDepth());

                // set the depth and property as hints on the query
                hints.put(Hints.ASSOCIATION_TRAVERSAL_DEPTH, xlinkDepth);

                PropertyName xlinkPropertyName = filterFactory.property(xlinkProperty.getValue());
                hints.put(Hints.ASSOCIATION_PROPERTY, xlinkPropertyName);

                dataQuery.setHints(hints);

                // TODO: support multiple properties
                break;
            }
        }

        // tell the datastore to use a lite coordinate sequence factory, if possible
        hints.put(Hints.JTS_COORDINATE_SEQUENCE_FACTORY, new LiteCoordinateSequenceFactory());

        // check for sql view parameters
        if (viewParams != null) {
            hints.put(Hints.VIRTUAL_TABLE_PARAMETERS, viewParams);
        }

        // currently only used by app-schema, produce mandatory properties
        hints.put(org.geotools.data.Query.INCLUDE_MANDATORY_PROPS, true);

        // add the joins, if specified
        if (joins != null) {
            dataQuery.getJoins().addAll(joins);
        }

        // finally, set the hints
        dataQuery.setHints(hints);

        return dataQuery;
    }

    private CoordinateReferenceSystem replaceCRSIfComplexFeatures(
            FeatureSource<? extends FeatureType, ? extends Feature> source,
            String wfsVersion,
            CoordinateReferenceSystem crs,
            final FeatureTypeInfo featureTypeInfo,
            CoordinateReferenceSystem formerCrs) {
        // if not complex features
        if (source.getSchema() instanceof SimpleFeatureType) {
            return formerCrs;
        } else {
            // they are complex features, proceed with projection logic
            final ProjectionPolicy projectionPolicy = featureTypeInfo.getProjectionPolicy();
            switch (projectionPolicy) {
                case REPROJECT_TO_DECLARED:
                case FORCE_DECLARED:
                    return WFSReprojectionUtil.getDeclaredCrs(featureTypeInfo.getCRS(), wfsVersion);
                default:
                    return WFSReprojectionUtil.getDeclaredCrs(crs, wfsVersion);
            }
        }
    }

    static Integer traverseXlinkDepth(String raw) {
        Integer traverseXlinkDepth = null;
        try {
            traverseXlinkDepth = Integer.valueOf(raw);
        } catch (NumberFormatException nfe) {
            // try handling *
            if ("*".equals(raw)) {
                // TODO: JD: not sure what this value should be? i think it
                // might be reported in teh acapabilitis document, using
                // INteger.MAX_VALUE will result in stack overflow... for now
                // we just use 10
                traverseXlinkDepth = Integer.valueOf(2);
            } else {
                // not wildcard case, throw original exception
                throw nfe;
            }
        }

        return traverseXlinkDepth;
    }

    boolean isValidJoinFilter(Filter filter) {
        PostPreProcessFilterSplittingVisitor visitor =
                new PostPreProcessFilterSplittingVisitor(joinFilterCapabilities, null, null);
        filter.accept(visitor, null);
        return visitor.getFilterPost() == null || visitor.getFilterPost() == Filter.INCLUDE;
    }

    FeatureTypeInfo featureTypeInfo(QName name, GetFeatureRequest request)
            throws WFSException, IOException {
        FeatureTypeInfo meta =
                catalog.getFeatureTypeByName(name.getNamespaceURI(), name.getLocalPart());

        if (meta == null) {
            String msg = "Could not locate " + name + " in catalog.";
            throw new WFSException(request, msg, "InvalidParameterValue").locator("typeName");
        }

        return meta;
    }

    @SuppressWarnings("PMD.UnusedLocalVariable")
    List<List<String>> parsePropertyNames(Query query, List<FeatureTypeInfo> featureTypes) {
        List<List<String>> propNames = new ArrayList();
        for (FeatureTypeInfo featureType : featureTypes) {
            propNames.add(new ArrayList());
        }

        if (featureTypes.size() == 1) {
            // non join
            propNames.get(0).addAll(query.getPropertyNames());
            return propNames;
        }

        // go through all property names and distribute based on prefix accordingly
        O:
        for (String propName : query.getPropertyNames()) {
            // check for a full typename prefix
            for (int j = 0; j < featureTypes.size(); j++) {
                FeatureTypeInfo featureType = featureTypes.get(j);
                if (propName.startsWith(featureType.prefixedName() + "/")) {
                    propNames
                            .get(j)
                            .add(propName.substring((featureType.prefixedName() + "/").length()));
                    continue O;
                }
                if (propName.startsWith(featureType.getName() + "/")) {
                    propNames
                            .get(j)
                            .add(propName.substring((featureType.getName() + "/").length()));
                    continue O;
                }
            }

            // check for aliases
            for (int j = 0; j < query.getAliases().size(); j++) {
                String alias = query.getAliases().get(j);
                if (propName.startsWith(alias + "/")) {
                    propNames.get(j).add(propName.substring((alias + "/").length()));
                    continue O;
                }
            }

            // fallback on first
            propNames.get(0).add(propName);
        }

        return propNames;
    }

    void validateSortBy(List<SortBy> sortBys, FeatureTypeInfo meta, final GetFeatureRequest request)
            throws IOException {
        FeatureType featureType = meta.getFeatureType();
        for (SortBy sortBy : sortBys) {
            PropertyName name = sortBy.getPropertyName();
            if (name.evaluate(featureType) == null) {
                throw new WFSException(
                        request,
                        "Illegal property name: "
                                + name.getPropertyName()
                                + " for feature type "
                                + meta.prefixedName(),
                        "InvalidParameterValue");
            }
        }
    }

    void validateFilter(
            Filter filter, Query query, final FeatureTypeInfo meta, final GetFeatureRequest request)
            throws IOException {

        // 1. ensure any property name refers to a property that
        // actually exists
        final FeatureType featureType = meta.getFeatureType();
        ExpressionVisitor visitor =
                new AbstractExpressionVisitor() {
                    public Object visit(PropertyName name, Object data) {
                        // case of multiple geometries being returned
                        if (name.evaluate(featureType) == null && !isGmlBoundedBy(name)) {
                            throw new WFSException(
                                    request,
                                    "Illegal property name: "
                                            + name.getPropertyName()
                                            + " for feature type "
                                            + meta.prefixedName(),
                                    "InvalidParameterValue");
                        }

                        return name;
                    }
                };
        filter.accept(new AbstractFilterVisitor(visitor), null);

        // 2. ensure any spatial predicate is made against a property
        // that is actually spatial
        AbstractFilterVisitor fvisitor =
                new AbstractFilterVisitor() {

                    protected Object visit(BinarySpatialOperator filter, Object data) {
                        PropertyName name = null;
                        if (filter.getExpression1() instanceof PropertyName) {
                            name = (PropertyName) filter.getExpression1();
                        } else if (filter.getExpression2() instanceof PropertyName) {
                            name = (PropertyName) filter.getExpression2();
                        }

                        if (name != null) {
                            // check against feataure type to make sure its
                            // a geometric type
                            AttributeDescriptor att =
                                    (AttributeDescriptor) name.evaluate(featureType);
                            if (!(att instanceof GeometryDescriptor) && !isGmlBoundedBy(name)) {
                                throw new WFSException(
                                        request,
                                        "Property "
                                                + name
                                                + " is not geometric in feature type "
                                                + meta.prefixedName(),
                                        "InvalidParameterValue");
                            }
                        }

                        return filter;
                    }
                };
        filter.accept(fvisitor, null);

        // 3. ensure that any bounds specified as part of the query
        // are valid with respect to the srs defined on the query
        if (wfs.isCiteCompliant()) {

            if (query.getSrsName() != null) {
                final Query fquery = query;
                fvisitor = new CiteBBOXValidator(fquery, request);

                filter.accept(fvisitor, null);
            }
        }

        // 4. ensure that spatial properties are not used in non spatial comparisons (CITE WFS 2.0)
        if (wfs.isCiteCompliant()) {
            fvisitor =
                    new AbstractFilterVisitor() {
                        @Override
                        protected Object visit(BinaryComparisonOperator filter, Object data) {
                            Expression ex1 = filter.getExpression1();
                            Expression ex2 = filter.getExpression2();
                            if (ex1 instanceof PropertyName) {
                                checkNonSpatial((PropertyName) ex1);
                            }
                            if (ex2 instanceof PropertyName) {
                                checkNonSpatial((PropertyName) ex2);
                            }

                            return super.visit(filter, data);
                        }

                        private void checkNonSpatial(PropertyName pn) {
                            AttributeDescriptor ad = (AttributeDescriptor) pn.evaluate(featureType);
                            if (ad instanceof GeometryDescriptor || isGmlBoundedBy(pn)) {
                                throw new WFSException(
                                        request,
                                        "Cannot use a spatial property in a alphanumeric binary "
                                                + "comparison");
                            }
                        }
                    };

            filter.accept(fvisitor, null);
        }
    }

    boolean isGmlBoundedBy(PropertyName name) {
        String propertyName = name.getPropertyName();

        // we want two non empty parts
        int idx = propertyName.indexOf(':');
        if (idx > 1 && propertyName.indexOf(":") < propertyName.length() - 1) {
            String[] split = propertyName.split("\\:");
            String prefix = split[0];
            String localName = split[1];
            if (!"boundedBy".equals(localName)) {
                return false;
            }
            // lax match in case we don't have namespace support
            if (name.getNamespaceContext() == null && "gml".equals(prefix)) {
                return true;
            }
            String ns = name.getNamespaceContext().getURI(prefix);
            return ns == null && "gml".equals(prefix)
                    || (GML.NAMESPACE.equals(ns)
                            || org.geotools.gml3.v3_2.GML.NAMESPACE.equals(ns));
        }
        return false;
    }

    int maxFeatures(List<FeatureTypeInfo> metas) {
        int maxFeatures = Integer.MAX_VALUE;
        for (FeatureTypeInfo meta : metas) {
            if (meta.getMaxFeatures() > 0) {
                maxFeatures = Math.min(maxFeatures, meta.getMaxFeatures());
            }
        }
        return maxFeatures;
    }

    protected PropertyName createPropertyName(String path, NamespaceSupport namespaceContext) {
        if (path.contains("/")) {
            return filterFactory.property(path, namespaceContext);
        } else {
            if (path.contains(":")) {
                int i = path.indexOf(":");
                return filterFactory.property(
                        new NameImpl(
                                namespaceContext.getURI(path.substring(0, i)),
                                path.substring(i + 1)));
            } else {
                return filterFactory.property(path);
            }
        }
    }

    protected List<PropertyName> addGeometryProperties(
            FeatureTypeInfo meta, List<PropertyName> oldProperties) throws IOException {
        List<AttributeTypeInfo> atts = meta.attributes();
        Iterator ii = atts.iterator();

        List<PropertyName> properties = new ArrayList<PropertyName>(oldProperties);

        while (ii.hasNext()) {
            AttributeTypeInfo ati = (AttributeTypeInfo) ii.next();
            PropertyName propName = filterFactory.property(ati.getName());

            if (meta.getFeatureType().getDescriptor(ati.getName()) instanceof GeometryDescriptor
                    && !properties.contains(propName)) {
                properties.add(propName);
            }
        }

        return properties;
    }

    private Filter buildFilterCRSFromInfo(
            Filter filter,
            QName primaryTypeName,
            FeatureSource<? extends FeatureType, ? extends Feature> source,
            String wfsVersion) {
        FeatureTypeInfo featureTypeInfo =
                catalog.getFeatureTypeByName(
                        primaryTypeName.getPrefix(), primaryTypeName.getLocalPart());
        if (featureTypeInfo != null && featureTypeInfo.getCRS() != null) {
            // the feature type info has a CRS defined, so let's use it
            return WFSReprojectionUtil.normalizeFilterCRS(
                    filter,
                    source.getSchema(),
                    WFSReprojectionUtil.getDeclaredCrs(featureTypeInfo.getCRS(), wfsVersion),
                    featureTypeInfo.getCRS());
        } else {
            return filter;
        }
    }

    private static class CiteBBOXValidator extends AbstractFilterVisitor {
        private final Query fquery;
        private final GetFeatureRequest request;

        public CiteBBOXValidator(Query fquery, GetFeatureRequest request) {
            this.fquery = fquery;
            this.request = request;
        }

        public Object visit(BBOX filter, Object data) {
            ReferencedEnvelope ex2Envelope =
                    filter.getExpression2().evaluate(null, ReferencedEnvelope.class);
            try {
                CoordinateReferenceSystem queryCrs = CRS.decode(fquery.getSrsName().toString());
                if (ex2Envelope != null
                        && ex2Envelope.getCoordinateReferenceSystem() != null
                        && !queryCrs.equals(ex2Envelope.getCoordinateReferenceSystem())) {
                    // back project bounding box into geographic coordinates
                    CoordinateReferenceSystem geo = DefaultGeographicCRS.WGS84;

                    GeneralEnvelope e = new GeneralEnvelope(filter.getBounds());
                    e = CRS.transform(e, geo);

                    // ensure within bounds defined by srs specified on
                    // query
                    CoordinateReferenceSystem crs = queryCrs;

                    GeographicBoundingBox valid =
                            (GeographicBoundingBox)
                                    crs.getDomainOfValidity()
                                            .getGeographicElements()
                                            .iterator()
                                            .next();

                    if (e.getMinimum(0) < valid.getWestBoundLongitude()
                            || e.getMinimum(0) > valid.getEastBoundLongitude()
                            || e.getMaximum(0) < valid.getWestBoundLongitude()
                            || e.getMaximum(0) > valid.getEastBoundLongitude()
                            || e.getMinimum(1) < valid.getSouthBoundLatitude()
                            || e.getMinimum(1) > valid.getNorthBoundLatitude()
                            || e.getMaximum(1) < valid.getSouthBoundLatitude()
                            || e.getMaximum(1) > valid.getNorthBoundLatitude()) {

                        throw new WFSException(
                                request,
                                "bounding box out of valid range of crs",
                                "InvalidParameterValue");
                    }
                }
            } catch (Exception e) {
                throw new WFSException(request, e);
            }

            return data;
        }
    }
}
