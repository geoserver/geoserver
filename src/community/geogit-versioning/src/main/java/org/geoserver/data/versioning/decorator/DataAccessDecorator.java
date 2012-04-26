package org.geoserver.data.versioning.decorator;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.api.GeoGIT;
import org.geogit.repository.Repository;
import org.geoserver.data.versioning.VersioningDataAccess;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.ServiceInfo;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Id;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.ResourceId;
import org.springframework.util.Assert;

import com.google.common.base.Throwables;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DataAccessDecorator<T extends FeatureType, F extends Feature> implements
        VersioningDataAccess<T, F> {

    protected DataAccess<T, F> unversioned;

    protected Repository repository;

    public DataAccessDecorator(DataAccess unversioned, Repository versioningRepo) {
        Assert.notNull(unversioned);
        Assert.notNull(versioningRepo);
        Assert.isTrue(!(unversioned instanceof DataAccessDecorator));
        this.unversioned = unversioned;
        this.repository = versioningRepo;
    }

    public boolean isVersioned(Name name) {
        boolean isVersioned = repository.getWorkingTree().hasRoot(name);
        return isVersioned;
    }

    /**
     * @see org.geotools.data.DataAccess#dispose()
     */
    @Override
    public void dispose() {
        if (unversioned != null) {
            unversioned.dispose();
            unversioned = null;
        }
    }

    /**
     * @see org.geotools.data.DataAccess#getFeatureSource(org.opengis.feature.type.Name)
     */
    @Override
    public FeatureSource<T, F> getFeatureSource(Name typeName) throws IOException {
        FeatureSource source = unversioned.getFeatureSource(typeName);
        if (source instanceof FeatureLocking) {
            return createFeatureLocking((FeatureLocking) source);
        } else if (source instanceof FeatureStore) {
            return createFeatureStore((FeatureStore) source);
        }
        return createFeatureSource(source);
    }

    /**
     * @see org.geotools.data.DataAccess#getInfo()
     */
    @Override
    public ServiceInfo getInfo() {
        return unversioned.getInfo();
    }

    /**
     * @see org.geotools.data.DataAccess#createSchema(org.opengis.feature.type.FeatureType)
     */
    @Override
    public void createSchema(T featureType) throws IOException {
        unversioned.createSchema(featureType);
        try {
            repository.getWorkingTree().init(featureType);
            GeoGIT ggit = new GeoGIT(repository);
            ggit.add().call();
            ggit.commit().call();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
    }

    /**
     * @see org.geotools.data.DataAccess#updateSchema(org.opengis.feature.type.Name,
     *      org.opengis.feature.type.FeatureType)
     */
    @Override
    public void updateSchema(Name typeName, T featureType) throws IOException {
        unversioned.updateSchema(typeName, featureType);
    }

    /**
     * @see org.geotools.data.DataAccess#getNames()
     */
    @Override
    public List<Name> getNames() throws IOException {
        return unversioned.getNames();
    }

    /**
     * @see org.geotools.data.DataAccess#getSchema(org.opengis.feature.type.Name)
     */
    @Override
    public T getSchema(Name name) throws IOException {
        return unversioned.getSchema(name);
    }

    /**
     * @precondition {@code typeName != null && versioningFilter != null}
     * @precondition {@code versioningFilter.getIdentifiers().size() > 0}
     * @postcondition {@code $return != null}
     * 
     * @param typeName
     * @param versioningFilter
     * @param extraQuery
     * @return
     * @throws IOException
     */
    public FeatureCollection getFeatures(final Name typeName, final Id versioningFilter,
            final Query extraQuery) throws IOException {
        Assert.notNull(typeName);
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

        final FeatureType featureType = this.getSchema(typeName);
        ResourceIdFeatureCollector versionQuery;
        versionQuery = new ResourceIdFeatureCollector(repository, featureType, resourceIds);

        DefaultFeatureCollection features = new DefaultFeatureCollection(null,
                (SimpleFeatureType) featureType);
        for (Feature f : versionQuery) {
            features.add((SimpleFeature) f);
        }
        return features;
    }

    protected FeatureSource<T, F> createFeatureSource(FeatureSource<T, F> source) {
        return new FeatureSourceDecorator(source, repository);
    }

    protected FeatureStore<T, F> createFeatureStore(FeatureStore<T, F> store) {
        return new FeatureStoreDecorator(store, repository);
    }

    protected FeatureLocking<T, F> createFeatureLocking(FeatureLocking<T, F> locking) {
        return new FeatureLockingDecorator(locking, repository);
    }
}
