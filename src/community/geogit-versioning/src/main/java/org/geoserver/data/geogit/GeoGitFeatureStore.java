package org.geoserver.data.geogit;

import static org.geotools.data.Transaction.AUTO_COMMIT;

import java.io.IOException;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geogit.repository.WorkingTree;
import org.geoserver.data.versioning.SimpleVersioningFeatureStore;
import org.geoserver.data.versioning.decorator.FeatureSourceDecorator;
import org.geoserver.data.versioning.decorator.VersionFilters;
import org.geoserver.data.versioning.decorator.VersionQuery;
import org.geotools.data.FeatureReader;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.filter.identity.ResourceIdImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.identity.ResourceId;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

@SuppressWarnings("rawtypes")
public class GeoGitFeatureStore extends GeoGitFeatureSource implements SimpleVersioningFeatureStore {

    private Transaction transaction;

    public GeoGitFeatureStore(final SimpleFeatureType type, final GeoGitDataStore store) {
        super(type, store);
        transaction = Transaction.AUTO_COMMIT;
    }

    /**
     * @return the object id of the current HEAD's commit if we're not inside a transaction, or
     *         {@code null} if we're inside a transaction
     */
    @Override
    public RevTree getCurrentVersion() {
        if (getTransaction() == null || Transaction.AUTO_COMMIT.equals(getTransaction())) {
            return super.getCurrentVersion();
        }

        // assume HEAD is at MASTER
        try {
            final Name typeName = this.type.getName();
            Repository repository = dataStore.getRepository();
            WorkingTree workingTree = repository.getWorkingTree();
            RevTree typeTree = workingTree.getStagedVersion(typeName);
            return typeTree;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ObjectId getRootTreeId() {
        final Transaction transaction = getTransaction();
        if (null == transaction || Transaction.AUTO_COMMIT.equals(transaction)) {
            // no transaction in progress, return the repository's root
            return super.getRootTreeId();
        }
        // transaction in progress, return null to signal querying the index first
        return null;
        // Repository repository = dataStore.getRepository();
        // StagingArea index = repository.getIndex();
        // RevTree staged = index.getStaged();
        // ObjectId stagedRootId = staged.getId();
        // return stagedRootId;
    }

    /**
     * @see org.geotools.data.FeatureStore#getTransaction()
     */
    @Override
    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * @see org.geotools.data.FeatureStore#setTransaction(org.geotools.data.Transaction)
     */
    @Override
    public void setTransaction(final Transaction transaction) {
        Preconditions.checkArgument(transaction != null,
                "Transaction is null, did you mean Transaction.AUTO_COMMIT?");
        this.transaction = transaction;
    }

    /**
     * @see org.geotools.data.FeatureStore#addFeatures(org.geotools.feature.FeatureCollection)
     */
    @Override
    public List<FeatureId> addFeatures(
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection) throws IOException {
        Preconditions.checkNotNull(collection);
        Preconditions.checkNotNull(collection.getSchema());
        Preconditions.checkArgument(type.getName().equals(collection.getSchema().getName()));

        checkTransaction();

        final Name typeName = getSchema().getName();
        final VersioningTransactionState versioningState = getVersioningState();

        List<FeatureId> insertedResourceIds;
        try {
            insertedResourceIds = versioningState.stageInsert(typeName, collection);
        } catch (Exception e) {
            throw new IOException(e);
        }

        return insertedResourceIds;
    }

    /**
     * @see org.geotools.data.FeatureStore#modifyFeatures(org.opengis.feature.type.Name[],
     *      java.lang.Object[], org.opengis.filter.Filter)
     */
    @Override
    public void modifyFeatures(final Name[] attributeNames, final Object[] attributeValues,
            final Filter filter) throws IOException {

        checkTransaction();
        // throws exception if filter has a resourceid that doesn't match the current version
        checkEditFilterMatchesCurrentVersion(filter);

        final SimpleFeatureCollection affectedFeatures = getFeatures(filter);
        final FeatureCollection newValues = new ModifyingFeatureCollection(affectedFeatures,
                attributeNames, attributeValues);

        VersioningTransactionState versioningState = getVersioningState();
        try {
            versioningState.stageUpdate(type.getName(), newValues);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * @see org.geotools.data.FeatureStore#removeFeatures(org.opengis.filter.Filter)
     */
    @Override
    public void removeFeatures(final Filter filter) throws IOException {
        Preconditions.checkNotNull(filter);

        checkTransaction();

        FeatureCollection removed = getFeatures(filter);
        try {
            Name typeName = getSchema().getName();
            VersioningTransactionState versioningState = getVersioningState();
            versioningState.stageDelete(typeName, filter, removed);
        } catch (Exception e) {
            Throwables.propagate(e);
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
        final Repository repository = dataStore.getRepository();
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
     * @see org.geotools.data.FeatureStore#setFeatures(org.geotools.data.FeatureReader)
     */
    @Override
    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader)
            throws IOException {
        throw new UnsupportedOperationException("do versioning!");
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

        final Object key = GeoGitDataStore.class;
        VersioningTransactionState state = (VersioningTransactionState) transaction.getState(key);
        if (state == null) {
            Repository repository = dataStore.getRepository();
            state = new VersioningTransactionState(new GeoGIT(repository));
            transaction.putState(key, state);
        }

        return state;
    }

    /**
     * @see org.geotools.data.simple.SimpleFeatureStore#modifyFeatures(java.lang.String,
     *      java.lang.Object, org.opengis.filter.Filter)
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {

        Name attributeName = new NameImpl(name);
        modifyFeatures(attributeName, attributeValue, filter);
    }

    /**
     * @see org.geotools.data.simple.SimpleFeatureStore#modifyFeatures(java.lang.String[],
     *      java.lang.Object[], org.opengis.filter.Filter)
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException {

        Name[] attributeNames = new Name[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            attributeNames[i] = new NameImpl(name);
        }
        modifyFeatures(attributeNames, attributeValues, filter);
    }

    /**
     * @see org.geotools.data.FeatureStore#modifyFeatures(org.opengis.feature.type.AttributeDescriptor[],
     *      java.lang.Object[], org.opengis.filter.Filter)
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
     * @see org.geotools.data.FeatureStore#modifyFeatures(org.opengis.feature.type.Name,
     *      java.lang.Object, org.opengis.filter.Filter)
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {

        modifyFeatures(new Name[] { attributeName }, new Object[] { attributeValue }, filter);
    }

    /**
     * @see org.geotools.data.FeatureStore#modifyFeatures(org.opengis.feature.type.AttributeDescriptor,
     *      java.lang.Object, org.opengis.filter.Filter)
     * @see #modifyFeatures(Name[], Object[], Filter)
     */
    @Override
    public void modifyFeatures(AttributeDescriptor type, Object value, Filter filter)
            throws IOException {

        modifyFeatures(new Name[] { type.getName() }, new Object[] { value }, filter);

    }

}
