package org.geoserver.data.gss;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.UpdateElementType;

import org.eclipse.emf.common.util.EList;
import org.geoserver.bxml.Context;
import org.geoserver.bxml.FeatureTypeProvider;
import org.geoserver.data.geogit.GeoToolsCommitStateResolver;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.Hints;
import org.geotools.feature.NameImpl;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;
import org.springframework.util.Assert;

import com.google.common.base.Preconditions;

@SuppressWarnings("rawtypes")
public class ReplicationReceiver {

    public EntryImpl receive(final FeedImpl replicationFeed,
            final FeatureTypeProvider geosyncStoreResolver) throws Exception {
        return receive(replicationFeed, geosyncStoreResolver, new NullProgressListener());
    }

    /**
     * Grabs the list of a GSS replication feed entries and applies them to the repository.
     * 
     * @param replicationFeed
     * @param targetRepo
     * @return the last entry processed
     * @throws Exception
     */
    public EntryImpl receive(final FeedImpl replicationFeed,
            final FeatureTypeProvider geosyncStoreResolver, final ProgressListener progress)
            throws Exception {

        try {
            Context.put(FeatureTypeProvider.class, geosyncStoreResolver);
            progress.started();
            EntryImpl lastEntry = receiveInternal(replicationFeed, geosyncStoreResolver, progress);
            progress.complete();
            return lastEntry;
        } finally {
            Context.clear();
        }
    }

    private EntryImpl receiveInternal(FeedImpl replicationFeed,
            FeatureTypeProvider geosyncStoreResolver, ProgressListener progress) throws Exception {

        Logger logger = Logging.getLogger(ReplicationReceiver.class);
        logger.fine("Processing replication entries...");
        final long startTime = System.currentTimeMillis();

        final Iterator<EntryImpl> entries = replicationFeed.getEntry();

        EntryImpl lastEntry = null;
        EntryImpl latestCommitted = null;

        Transaction transaction = new DefaultTransaction();
        Map<Name, FeatureStore> stores = new HashMap<Name, FeatureStore>();

        Date previousUpdated = null;

        int count = 0;

        int uncommitted = 0;
        try {
            while (entries.hasNext()) {
                EntryImpl entry = entries.next();
                progress.setDescription(String.valueOf(++count));

                if (progress.isCanceled()) {
                    transaction.rollback();
                    return latestCommitted;
                }

                uncommitted++;
                if (lastEntry == null) {
                    lastEntry = entry;
                    previousUpdated = entry.getUpdated();
                }
                Date currentUpdated = entry.getUpdated();
                if (!currentUpdated.equals(previousUpdated)) {
                    transaction = commit(lastEntry, transaction);
                    previousUpdated = currentUpdated;
                    uncommitted = 1;// the current entry
                    latestCommitted = lastEntry;
                }

                FeatureStore store = findStore(entry, geosyncStoreResolver, stores);
                store.setTransaction(transaction);
                apply(store, entry);

                lastEntry = entry;
                if (uncommitted > 0 && !entries.hasNext()) {
                    transaction = commit(lastEntry, transaction);
                    uncommitted = 0;
                    latestCommitted = lastEntry;
                }
            }
        } finally {
            transaction.close();
        }

        final long endTime = System.currentTimeMillis();
        logger.fine(count + " entries processed in " + (endTime - startTime) + "ms");
        if (count > 0) {
            progress.setDescription("Replication complete after " + count + " entries.");
        } else {
            progress.setDescription("Found no changes");
        }
        return lastEntry;
    }

    /**
     * @param lastEntry
     * @param transaction
     * @return
     * @throws IOException
     * @see {@link GeoToolsCommitStateResolver}
     */
    protected Transaction commit(EntryImpl lastEntry, Transaction transaction) throws IOException {
        String user = author(lastEntry);
        String committer = committer(lastEntry);
        String commitMessage = lastEntry.getSummary();
        Date updated = lastEntry.getUpdated();
        transaction.putProperty(GeoToolsCommitStateResolver.GEOGIT_AUTHOR, user);
        transaction.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMITTER, committer);
        transaction.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_MESSAGE, commitMessage);
        transaction.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_TIMESTAMP, updated);

        try {
            transaction.commit();
        } catch (IOException e) {
            transaction.rollback();
            throw e;
        } catch (RuntimeException e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
        }
        transaction = new DefaultTransaction();
        return transaction;
    }

    private FeatureStore findStore(final EntryImpl entry,
            final FeatureTypeProvider geosyncStoreResolver, final Map<Name, FeatureStore> storeCache)
            throws IOException {

        final Name typeName = typeName(entry.getContent().getValue());

        FeatureStore store = storeCache.get(typeName);
        if (store == null) {
            store = geosyncStoreResolver.resolveFeatureStore(typeName);
            storeCache.put(typeName, store);
        }
        return store;
    }

    private Name typeName(Object value) {
        Assert.notNull(value);
        if (value instanceof InsertElementType) {
            InsertElementType insertElement = (InsertElementType) value;
            Feature feature = (Feature) insertElement.getFeature().get(0);
            return feature.getType().getName();
        }

        if (value instanceof UpdateElementType) {
            QName qName = ((UpdateElementType) value).getTypeName();
            return new NameImpl(qName);
        }

        if (value instanceof DeleteElementType) {
            QName qName = ((DeleteElementType) value).getTypeName();
            return new NameImpl(qName);
        }

        throw new IllegalArgumentException(
                "Entry's content is not a WFS Insert, Update, or Delete (" + value + ")");
    }

    private void apply(FeatureStore store, EntryImpl entry) throws IOException {
        ContentImpl content = entry.getContent();
        if (content.getValue() instanceof UpdateElementType) {
            update(store, (UpdateElementType) content.getValue());
        }

        if (content.getValue() instanceof DeleteElementType) {
            delete(store, (DeleteElementType) content.getValue());
        }

        if (content.getValue() instanceof InsertElementType) {
            insert(store, (InsertElementType) content.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private void insert(FeatureStore store, InsertElementType insertElement) throws IOException {
        if (!store.getQueryCapabilities().isUseProvidedFIDSupported()) {
            throw new UnsupportedOperationException(
                    "The underlying data store '"
                            + store.getSchema().getName().getLocalPart()
                            + "' does not support the USE_PROVIDED_FID Hint and inserts won't preserve Feature IDs");
        }
        List<SimpleFeature> features = insertElement.getFeature();
        for (SimpleFeature f : features) {
            f.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
        }
        SimpleFeatureCollection fc = DataUtilities.collection(features);
        List addedFeatures = store.addFeatures(fc);
        int size = fc.size();
        Preconditions.checkState(size == addedFeatures.size());
        store.addFeatures(fc);
    }

    private void delete(FeatureStore store, DeleteElementType deleteElement) throws IOException {
        store.removeFeatures(deleteElement.getFilter());
    }

    private void update(FeatureStore store, UpdateElementType updateElement) throws IOException {
        EList properties = updateElement.getProperty();

        Name[] attributeNames = new NameImpl[properties.size()];
        Object[] attributeValues = new Object[properties.size()];
        int i = 0;
        for (Object object : properties) {
            PropertyType propertyType = (PropertyType) object;
            attributeNames[i] = new NameImpl(propertyType.getName().getLocalPart());
            attributeValues[i] = propertyType.getValue();
            i++;
        }

        Filter filter = updateElement.getFilter();
        FeatureType schema = store.getSchema();
        schema.getDescriptor(attributeNames[0]);
        store.modifyFeatures(attributeNames, attributeValues, filter);
    }

    private String committer(EntryImpl entry) {
        return name(entry.getContributor());
    }

    private String author(EntryImpl entry) {
        return name(entry.getAuthor());
    }

    private String name(List<PersonImpl> person) {
        if (person != null && person.size() > 0) {
            return person.get(0).getName();
        }
        return null;
    }

}