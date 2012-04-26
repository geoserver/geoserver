package org.geoserver.data.versioning.decorator;

import static org.geotools.data.Transaction.AUTO_COMMIT;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geoserver.data.geogit.VersioningTransactionState;
import org.geoserver.data.versioning.VersioningFeatureStore;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.identity.ResourceIdImpl;
import org.opengis.feature.Feature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.ResourceId;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

@SuppressWarnings("rawtypes")
public class FeatureStoreDecorator<T extends FeatureType, F extends Feature> extends
        FeatureSourceDecorator<T, F> implements VersioningFeatureStore<T, F> {

    public FeatureStoreDecorator(final FeatureStore unversioned, final Repository repo) {
        super(unversioned, repo);
    }

    @Override
    public RevTree getCurrentVersion() {
        final Transaction transaction = getTransaction();
        if (null == transaction || Transaction.AUTO_COMMIT.equals(transaction)) {
            return super.getCurrentVersion();
        }
        final Name name = getName();
        RevTree headVersion = repository.getWorkingTree().getStagedVersion(name);
        return headVersion;
    }

    /**
     * @see org.geotools.data.FeatureStore#getTransaction()
     */
    @Override
    public Transaction getTransaction() {
        return ((FeatureStore) unversioned).getTransaction();
    }

    /**
     * @see org.geotools.data.FeatureStore#setTransaction(org.geotools.data.Transaction)
     */
    @Override
    public void setTransaction(final Transaction transaction) {
        ((FeatureStore) unversioned).setTransaction(transaction);
    }

    private FeatureStore<T, F> getUnversionedStore() {
        return ((FeatureStore<T, F>) unversioned);
    }

    /**
     * @see org.geotools.data.FeatureStore#addFeatures(org.geotools.feature.FeatureCollection)
     */
    @Override
    public List<FeatureId> addFeatures(FeatureCollection<T, F> collection) throws IOException {
        if (isVersioned()) {
            checkTransaction();
        }

        final FeatureStore<T, F> unversioned = getUnversionedStore();
        List<FeatureId> unversionedIds = unversioned.addFeatures(collection);

        if (isVersioned()) {
            try {
                final Name typeName = getSchema().getName();
                VersioningTransactionState versioningState = getVersioningState();

                FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

                Id id = ff.id(new HashSet<Identifier>(unversionedIds));
                FeatureCollection<T, F> inserted = unversioned.getFeatures(id);
                List<FeatureId> versionedIds;
                versionedIds = versioningState.stageInsert(typeName, inserted, true);
                return versionedIds;

            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        return unversionedIds;
    }

    /**
     * @see org.geotools.data.FeatureStore#removeFeatures(org.opengis.filter.Filter)
     */
    @Override
    public void removeFeatures(Filter filter) throws IOException {
        final FeatureStore<T, F> unversioned = getUnversionedStore();
        if (isVersioned()) {
            checkTransaction();

            FeatureCollection<T, F> removed = unversioned.getFeatures(filter);
            try {
                Name typeName = getSchema().getName();
                getVersioningState().stageDelete(typeName, filter, removed);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
        unversioned.removeFeatures(filter);
    }

    /**
     * @see org.geotools.data.FeatureStore#modifyFeatures(org.opengis.feature.type.Name[],
     *      java.lang.Object[], org.opengis.filter.Filter)
     */
    @Override
    public void modifyFeatures(final Name[] attributeNames, final Object[] attributeValues,
            final Filter filter) throws IOException {

        final FeatureStore<T, F> unversioned = getUnversionedStore();
        final boolean versioned = isVersioned();
        Id affectedFeaturesFitler = null;
        Filter unversionedFilter = filter;
        if (versioned) {
            checkTransaction();
            // throws exception if filter has a resourceid that doesn't match the current version
            checkEditFilterMatchesCurrentVersion(filter);

            unversionedFilter = VersionFilters.getUnversioningFilter(filter);
            if (unversionedFilter instanceof Id) {
                affectedFeaturesFitler = (Id) unversionedFilter;
            } else {
                FeatureCollection<T, F> affectedFeatures;

                affectedFeatures = unversioned.getFeatures(unversionedFilter);
                FeatureIterator<F> iterator = affectedFeatures.features();
                Set<Identifier> affectedIds = new HashSet<Identifier>();
                try {
                    while (iterator.hasNext()) {
                        affectedIds.add(iterator.next().getIdentifier());
                    }
                } finally {
                    iterator.close();
                }
                final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
                affectedFeaturesFitler = ff.id(affectedIds);
            }
        }

        unversioned.modifyFeatures(attributeNames, attributeValues, unversionedFilter);

        if (versioned && affectedFeaturesFitler != null
                && affectedFeaturesFitler.getIdentifiers().size() > 0) {
            try {
                FeatureCollection newValues = unversioned.getFeatures(affectedFeaturesFitler);
                getVersioningState().stageUpdate(getSchema().getName(), newValues);
            } catch (Exception e) {
                Throwables.propagate(e);
            }
        }
    }

    /**
     * Throws an IllegalArgumentException if {@code filter} contains a resource filter that doesn't
     * match the current version of a feature
     * 
     * @param filter
     *            original upate filter
     */
    private void checkEditFilterMatchesCurrentVersion(final Filter filter) {
        final Id versionFilter = VersionFilters.getVersioningFilter(filter);
        if (versionFilter == null) {
            return;
        }
        // don't allow non current versions
        GeoGIT ggit = new GeoGIT(repository);
        VersionQuery query = new VersionQuery(ggit, getSchema().getName());
        for (Identifier id : versionFilter.getIdentifiers()) {
            ResourceId rid = (ResourceId) id;
            List<Ref> requested;
            List<Ref> current;
            try {
                requested = Lists.newArrayList(query.get(rid));
                current = Lists.newArrayList(query.get(new ResourceIdImpl(rid.getID(), null)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!current.equals(requested)) {
                throw new IllegalArgumentException(
                        "Requested resource id filter doesn't match curent version for feature "
                                + rid.getID());
            }
        }
    }

    /**
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(AttributeDescriptor[] type, Object[] value, Filter filter)
            throws IOException {

        Name[] attributeNames = new Name[type.length];
        for (int i = 0; i < type.length; i++) {
            attributeNames[i] = type[i].getName();
        }
        modifyFeatures(attributeNames, value, filter);
    }

    /**
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {

        modifyFeatures(new Name[] { attributeName }, new Object[] { attributeValue }, filter);
    }

    /**
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(AttributeDescriptor type, Object value, Filter filter)
            throws IOException {

        modifyFeatures(new Name[] { type.getName() }, new Object[] { value }, filter);

    }

    /**
     * @see org.geotools.data.FeatureStore#setFeatures(org.geotools.data.FeatureReader)
     */
    @Override
    public void setFeatures(FeatureReader<T, F> reader) throws IOException {
        final FeatureStore<T, F> unversioned = getUnversionedStore();
        unversioned.setFeatures(reader);
        if (isVersioned()) {
            checkTransaction();
            throw new UnsupportedOperationException("do versioning!");
        }
    }

    private void checkTransaction() {
        if (Transaction.AUTO_COMMIT.equals(getTransaction())) {
            throw new UnsupportedOperationException(
                    "AUTO_COMMIT is not supported for versioned Feature Types");
        }
    }

    protected VersioningTransactionState getVersioningState() {
        Transaction transaction = getTransaction();
        if (AUTO_COMMIT.equals(transaction)) {
            return VersioningTransactionState.VOID;
        }

        Object key = "WHAT_WOULD_BE_A_GOOD_KEY?";
        VersioningTransactionState state = (VersioningTransactionState) transaction.getState(key);
        if (state == null) {
            state = new VersioningTransactionState(new GeoGIT(repository));
            transaction.putState(key, state);
        }
        return state;
    }

}
