package org.geoserver.data.versioning.decorator;

import static org.geoserver.data.versioning.decorator.VersionFilters.getVersioningFilter;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geoserver.data.versioning.VersioningFeatureSource;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.ResourceId;
import org.springframework.util.Assert;

/**
 * Provides support for {@link ResourceId} filtering by means of wrapping an unversioned feature
 * source and accessing the versioning information in the versioning subsystem provided by the
 * argument {@link DataAccessDecorator}.
 * 
 * @author groldan
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FeatureSourceDecorator<T extends FeatureType, F extends Feature> implements
        VersioningFeatureSource<T, F> {

    protected final FeatureSource<T, F> unversioned;

    protected final Repository repository;

    public FeatureSourceDecorator(final FeatureSource unversioned, final Repository repository) {
        this.unversioned = unversioned;
        this.repository = repository;
    }

    /**
     * @return {@code true} if this is a versioned Feature Type, {@code false} otherwise.
     */
    public boolean isVersioned() {
        final Name name = getSchema().getName();
        return isVersioned(name, repository);
    }

    public static boolean isVersioned(final Name typeName, final Repository repository) {
        final WorkingTree workingTree = repository.getWorkingTree();
        final boolean isVersioned = workingTree.hasRoot(typeName);
        return isVersioned;
    }

    /**
     * @return the object id of the current HEAD's commit
     */
    public RevTree getCurrentVersion() {
        final Name name = getName();
        RevTree headVersion = repository.getWorkingTree().getHeadVersion(name);
        return headVersion;
    }

    /**
     * @precondition {@code typeName != null && versioningFilter != null}
     * @precondition {@code versioningFilter.getIdentifiers().size() > 0}
     * @postcondition {@code $return != null}
     * 
     * @param versioningFilter
     * @param extraQuery
     * @return
     * @throws IOException
     */
    protected FeatureCollection getFeatures(final Id versioningFilter, final Query extraQuery)
            throws IOException {

        Assert.notNull(versioningFilter);
        Assert.isTrue(versioningFilter.getIdentifiers().size() > 0);

        final Set<Identifier> identifiers = versioningFilter.getIdentifiers();
        final Set<ResourceId> resourceIds = new HashSet<ResourceId>();
        for (Identifier id : identifiers) {
            if (id instanceof ResourceId) {
                resourceIds.add((ResourceId) id);
            }
        }
        if (resourceIds.size() == 0) {
            throw new IllegalArgumentException("At least one " + ResourceId.class.getName()
                    + " should be provided: " + identifiers);
        }

        final FeatureType featureType = getSchema();
        ResourceIdFeatureCollector versionQuery;
        versionQuery = new ResourceIdFeatureCollector(repository, featureType, resourceIds);

        DefaultFeatureCollection features = new DefaultFeatureCollection(null,
                (SimpleFeatureType) featureType);
        
        for (Feature f : versionQuery) {
            features.add((SimpleFeature) f);
        }
        return features;
    }

    /**
     * @see org.geotools.data.FeatureSource#getDataStore()
     */
    @Override
    public DataAccess<T, F> getDataStore() {
        return unversioned.getDataStore();
    }

    /**
     * @see org.geotools.data.FeatureSource#getBounds(org.geotools.data.Query)
     */
    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        if (!isVersioned() || null == getVersioningFilter(query.getFilter())) {
            return unversioned.getBounds(query);
        }
        return getFeatures(query).getBounds();
    }

    /**
     * @see org.geotools.data.FeatureSource#getCount(org.geotools.data.Query)
     */
    @Override
    public int getCount(Query query) throws IOException {
        if (!isVersioned() || null == getVersioningFilter(query.getFilter())) {
            return unversioned.getCount(query);
        }
        return getFeatures(query).size();
    }

    /**
     * @see org.geotools.data.FeatureSource#getFeatures()
     * @see #getFeatures(Query)
     */
    @Override
    public FeatureCollection<T, F> getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    /**
     * @see org.geotools.data.FeatureSource#getFeatures(org.opengis.filter.Filter)
     * @see #getFeatures(Query)
     */
    @Override
    public FeatureCollection<T, F> getFeatures(Filter filter) throws IOException {
        return getFeatures(namedQuery(filter));
    }

    private Query namedQuery(Filter filter) {
        Name name = getName();
        String typeName = name.getLocalPart();
        URI namespace;
        try {
            namespace = new URI(name.getNamespaceURI());
        } catch (URISyntaxException e) {
            namespace = null;
        }
        int maxFeartures = Integer.MAX_VALUE;
        String[] propNames = null;
        String handle = null;
        Query query = new Query(typeName, namespace, filter, maxFeartures, propNames, handle);
        return query;
    }

    /**
     * Performs the given query with knowledge of feature versioning.
     * <p>
     * In case the feature type this source refers to is not versioned, defers to the underlying
     * {@link FeatureSource}.
     * </p>
     * If the Feature Type is versioned, and the Query filter contains an {@link Id} filter with
     * {@link ResourceId} predicates, defers to the versioning backend (GeoGIT) to spply the
     * requested versions of the feature identified by the {@link ResourceId}s; othwewise just wraps
     * the wrapped FeatureSource results into a decorating FeatureCollection that assigns
     * {@link ResourceId} instead of {@link FeatureId} to returned Features, containing the current
     * version hash, as in {@code <original feature id>@<current version id>}. </p>
     * 
     * @see org.geotools.data.FeatureSource#getFeatures(org.geotools.data.Query)
     */
    @Override
    public FeatureCollection<T, F> getFeatures(Query query) throws IOException {
        Id versioningFilter;
        if (!isVersioned()) {
            return unversioned.getFeatures(query);
        }
        versioningFilter = getVersioningFilter(query.getFilter());
        if (versioningFilter == null) {
            FeatureCollection<T, F> delegate = unversioned.getFeatures(query);
            final RevTree currentTypeTree = getCurrentVersion();

            return createFeatureCollection(delegate, currentTypeTree);
        }

        return getFeatures(versioningFilter, query);
    }

    protected FeatureCollection<T, F> createFeatureCollection(FeatureCollection<T, F> delegate,
            RevTree currentTypeTree) {
        return new ResourceIdAssigningFeatureCollection(delegate, this, currentTypeTree);
    }

    // / directly deferred methods
    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#getName()
     */
    @Override
    public Name getName() {
        return unversioned.getName();
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#getInfo()
     */
    @Override
    public ResourceInfo getInfo() {
        return unversioned.getInfo();
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#getQueryCapabilities()
     */
    @Override
    public QueryCapabilities getQueryCapabilities() {
        return unversioned.getQueryCapabilities();
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#addFeatureListener(org.geotools.data.FeatureListener)
     */
    @Override
    public void addFeatureListener(FeatureListener listener) {
        unversioned.addFeatureListener(listener);
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#removeFeatureListener(org.geotools.data.FeatureListener)
     */
    @Override
    public void removeFeatureListener(FeatureListener listener) {
        unversioned.removeFeatureListener(listener);
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#getSchema()
     */
    @Override
    public T getSchema() {
        return unversioned.getSchema();
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#getBounds()
     */
    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return unversioned.getBounds();
    }

    /**
     * Defers to the same method on the wrapped unversioned FeatureSource
     * 
     * @see org.geotools.data.FeatureSource#getSupportedHints()
     */
    @Override
    public Set<Key> getSupportedHints() {
        return unversioned.getSupportedHints();
    }

}
