/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static org.geoserver.gss.GSSCore.*;
import static org.geotools.data.DataUtilities.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gss.CentralRevisionsType.LayerRevision;
import org.geoserver.gss.GSSException.GSSExceptionCode;
import org.geoserver.gss.xml.GSSConfiguration;
import org.geoserver.wfsv.VersioningTransactionConverter;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultQuery;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureDiff;
import org.geotools.data.FeatureDiffReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.VersioningFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.gml3.GML;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.geotools.xml.EMFUtils;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.xml.sax.SAXException;

/**
 * The GSS implementation
 */
public class DefaultGeoServerSynchronizationService implements GeoServerSynchronizationService {

    // exception locator constant
    static final String TO_VERSION = "toVersion";

    static final String TYPE_NAME = "typeName";

    static final String FROM_VERSION = "fromVersion";

    static final Logger LOGGER = Logging.getLogger(DefaultGeoServerSynchronizationService.class);

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    Catalog catalog;

    GSSConfiguration configuration;

    GSSCore core;

    public DefaultGeoServerSynchronizationService(GeoServer geoServer,
            GSSConfiguration configuration) {
        this.catalog = geoServer.getCatalog();
        this.configuration = configuration;
        this.core = new GSSCore(geoServer);
    }

    /**
     * Responds a GetCentralRevision reuest
     */
    public CentralRevisionsType getCentralRevision(GetCentralRevisionType request) {
        core.ensureUnitEnabled();

        try {
            CentralRevisionsType cr = new CentralRevisionsType();
            for (QName typeName : request.getTypeNames()) {
                checkSyncronized(typeName);
                SimpleFeature record = getLastSynchronizationRecord(typeName);
                long revision = -1;
                if (record != null) {
                    revision = ((Number) record.getAttribute("central_revision")).longValue();
                }
                cr.getLayerRevisions().add(new LayerRevision(typeName, revision));
            }

            return cr;
        } catch (IOException e) {
            throw new GSSException("Could not compute the response", e);
        }
    }

    /**
     * Checks the feature type exists, it's actually synchronized, and returns the last known
     * central revision
     * 
     * @param typeName
     * @return
     */
    SimpleFeature getLastSynchronizationRecord(QName typeName) throws IOException {
        FeatureIterator<SimpleFeature> fi = null;
        try {
            // get the versioning data store
            VersioningDataStore ds = core.getVersioningStore();

            // gather the record from the synch history table
            DefaultQuery q = new DefaultQuery();
            q.setFilter(ff.equal(ff.property("table_name"), ff.literal(typeName.getLocalPart()),
                    true));
            q.setSortBy(new SortBy[] { ff.sort("central_revision", SortOrder.DESCENDING),
                    ff.sort("local_revision", SortOrder.DESCENDING)});
            q.setMaxFeatures(1);
            fi = ds.getFeatureSource(SYNCH_HISTORY).getFeatures(q).features();

            if (fi.hasNext()) {
                return fi.next();
            } else {
                return null;
            }
        } finally {
            if (fi != null) {
                fi.close();
            }
        }
    }

    /**
     * Check the typeName exists and is one of the synchronised tables
     * 
     * @param typeName
     * @throws IOException
     */
    void checkSyncronized(QName typeName) throws IOException {
        NamespaceInfo ns = catalog.getNamespaceByURI(typeName.getNamespaceURI());
        if (ns == null) {
            ns = catalog.getNamespaceByPrefix(typeName.getPrefix());
        }
        if (ns == null) {
            throw new GSSException("Could not locate typeName: " + typeName,
                    GSSExceptionCode.InvalidParameterValue, TYPE_NAME);
        }

        FeatureTypeInfo fti = catalog.getFeatureTypeByName(ns, typeName.getLocalPart());
        if (fti == null) {
            throw new GSSException("Could not locate typeName: " + typeName,
                    GSSExceptionCode.InvalidParameterValue, TYPE_NAME);
        }

        // get the versioning data store
        VersioningDataStore ds = core.getVersioningStore();

        // check the table is actually synch-ed
        DefaultQuery q = new DefaultQuery();
        q.setFilter(ff.equal(ff.property("table_name"), ff.literal(fti.getName()), true));
        int count = ds.getFeatureSource(SYNCH_TABLES).getCount(q);
        if (count == 0) {
            throw new GSSException(fti.getName() + " is not a synchronized layer",
                    GSSExceptionCode.InvalidParameterValue, TYPE_NAME);
        }
    }

    public PostDiffResponseType postDiff(PostDiffType request) {
        core.ensureUnitEnabled();

        Transaction transaction = new DefaultTransaction();
        try {
            // check the layer is actually shared and get the latest known central revision
            checkSyncronized(request.getTypeName());
            SimpleFeature record = getLastSynchronizationRecord(request.getTypeName());
            long lastCentralRevision = -1;
            long lastLocalRevision = -1;
            if (record != null) {
                lastCentralRevision = ((Number) record.getAttribute("central_revision"))
                        .longValue();
                lastLocalRevision = ((Number) record.getAttribute("local_revision")).longValue();
            }

            if (request.getFromVersion() != lastCentralRevision) {
                throw new GSSException("Invalid fromVersion, it should be " + lastCentralRevision,
                        GSSExceptionCode.InvalidParameterValue, FROM_VERSION);
            }

            if (request.getFromVersion() > request.getToVersion()) {
                throw new GSSException("Invalid toVersion " + request.getToVersion() + ", it should be higher than fromVersion: "
                        + request.getFromVersion(), GSSExceptionCode.InvalidParameterValue,
                        TO_VERSION);
            }

            // make sure all of the changes are applied in one hit, or none
            // very important, make sure all versioning writes use the same transaction or they
            // will deadlock each other
            VersioningDataStore ds = core.getVersioningStore();
            
            // see if there is anything at all to do, if both sides have no changes there
            // is no point eating away a revision number (this avoid the local revision number to
            // skyrocket for nothing if there are frequent synchronisations)
            String tableName = request.getTypeName().getLocalPart();
            VersioningFeatureStore fs = (VersioningFeatureStore) ds.getFeatureSource(tableName);
            FeatureStore history = (FeatureStore) ds.getFeatureSource(SYNCH_HISTORY);
            PropertyIsEqualTo ftSyncRecord = ff.equals(ff.property("table_name"), ff.literal(tableName));
            TransactionType changes = request.getTransaction();
            int changesCount = core.countChanges(changes);
            // ... if we have no changes from remote
            if(changesCount == 0) {
                // ... and we have no changes locally
                String lastLocalRevisionId = lastLocalRevision != -1 ? String.valueOf(lastLocalRevision) : "FIRST";
                if(fs.getLog(lastLocalRevisionId, "LAST", null, null, 1).size() == 0) {
                    // add a new record without the need to grab a new local revision
                    // (if necessary, that is, if at least the Central revision changed or if
                    // we don't have a synch history at all)
                    long newCentralRevision = request.getToVersion();
                    if(lastCentralRevision != newCentralRevision || record == null) {
                        SimpleFeatureType hft = (SimpleFeatureType) history.getSchema();
                        SimpleFeature f = SimpleFeatureBuilder.build(hft, new Object[] { tableName,
                                lastLocalRevision, newCentralRevision }, null);
                        history.addFeatures(DataUtilities.collection(f));
                    }
                    
                    // ... let's just return directly, no need to store or do anything
                    return new PostDiffResponseType();
                }
            }
            
            // setup the commit message and author
            transaction.putProperty(VersioningDataStore.AUTHOR, "gss");
            transaction.putProperty(VersioningDataStore.MESSAGE, "Applying " + changesCount 
                    + " changes coming from Central on layer '" + tableName + "'");

            // grab the feature stores and bind them all to the same transaction
            VersioningFeatureStore conflicts = (VersioningFeatureStore) ds
                    .getFeatureSource(SYNCH_CONFLICTS);
            conflicts.setTransaction(transaction);
            history.setTransaction(transaction);
            fs.setTransaction(transaction);

            // get a hold on a revision number early so that we don't get concurrent changes
            // from the user (the datastore will make it so that no new revision numbers will
            // be generated until we commit or rollback this transaction
            long newLocalRevision = Long.parseLong(conflicts.getVersion());

            // apply changes
            LOGGER.info("About to apply " + core.countChanges(changes)
                    + " changes coming from Central");
            if (core.countChanges(changes) > 0) {
                List<DeleteElementType> deletes = changes.getDelete();
                List<UpdateElementType> updates = changes.getUpdate();

                // We need to find conflicts: local changes occurred since last synchronisation
                // that hit the same features contained in this changeset. For those we need
                // to create a conflict record and revert the local changes so that we
                // can apply the central ones
                Set<FeatureId> deletedFids = getEObjectFids(deletes);
                Set<FeatureId> updatedFids = getEObjectFids(updates);
                Set<FeatureId> changedFids = new HashSet<FeatureId>();
                changedFids.addAll(deletedFids);
                changedFids.addAll(updatedFids);

                // any possibility of conflict? If empty grabbing the corresponding local changes
                // will fail
                if (changedFids.size() > 0) {
                    // limit the changeset to the window between the last and the current
                    // synchronization
                    String newLocalRevisionId = String.valueOf(newLocalRevision);
                    String lastLocalRevisionId = lastLocalRevision != -1 ? String.valueOf(lastLocalRevision) : "FIRST";
                    FeatureDiffReader localChanges = fs.getDifferences(lastLocalRevisionId,
                            newLocalRevisionId, ff.id(changedFids), null);
                    while (localChanges.hasNext()) {
                        FeatureDiff fd = localChanges.next();
                        FeatureId diffFeatureId = ff.featureId(fd.getID());
                        if (fd.getState() == FeatureDiff.INSERTED) {
                            throw new GSSException(
                                    "A new locally inserted feature has the same "
                                            + "id as a modified feature coming from Central, this is impossible, "
                                            + "there is either a bug in ID generation or someone manually tampered with it!");
                        } else if (fd.getState() == FeatureDiff.DELETED) {
                            if (deletedFids.contains(diffFeatureId)) {
                                saveCleanMergeMarker(fs, conflicts, lastLocalRevisionId,
                                        newLocalRevision, fd.getID());
                            } else {
                                handleDeletionConflict(fs, conflicts, lastLocalRevisionId,
                                        newLocalRevision, fd.getID());
                            }
                        } else {
                            if (updatedFids.contains(diffFeatureId)) {
                                if (isSameUpdate(fd, findUpdate(fd.getID(), updates))) {
                                    saveCleanMergeMarker(fs, conflicts, lastLocalRevisionId,
                                            newLocalRevision, fd.getID());
                                } else {
                                    handleUpdateConflict(fs, conflicts, lastLocalRevisionId,
                                            newLocalRevision, fd.getID());
                                }
                            } else {
                                handleUpdateConflict(fs, conflicts, lastLocalRevisionId,
                                        newLocalRevision, fd.getID());
                            }
                        }
                    }
                    localChanges.close();
                }

                // now that conflicting local changes have been moved out of the way, apply the
                // central ones
                core.applyChanges(changes, fs);

            }

            // save/update the synchronisation metadata
            long newCentralRevision = request.getToVersion();
            SimpleFeatureType hft = (SimpleFeatureType) history.getSchema();
            SimpleFeature f = SimpleFeatureBuilder.build(hft, new Object[] { tableName,
                    newLocalRevision, newCentralRevision }, null);
            history.addFeatures(DataUtilities.collection(f));

            // commit all the changes
            transaction.commit();

            LOGGER.info(core.countChanges(changes)
                    + " changes coming from Central succesfully applied");
        } catch (Throwable t) {
            // make sure we rollback the transaction in case of _any_ exception
            try {
                transaction.rollback();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Rollback failed. This is unexpected", e);
            }

            if (t instanceof GSSException) {
                throw (GSSException) t;
            } else {
                throw new GSSException("Error occurred while applyling the diff", t);
            }
        } finally {
            // very important to close transaction, as it holds a connection to the db
            try {
                transaction.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Transaction close failed. This is unexpected", e);
            }

        }

        return new PostDiffResponseType();
    }

    public GetDiffResponseType getDiff(GetDiffType request) {
        core.ensureUnitEnabled();

        FeatureIterator<SimpleFeature> fi = null;
        try {
            checkSyncronized(request.getTypeName());
            SimpleFeature record = getLastSynchronizationRecord(request.getTypeName());
            if (record == null) {
                throw new GSSException(
                        "Out of order invocation, no PostDiff was called before GetDiff",
                        GSSExceptionCode.InvalidParameterValue, FROM_VERSION);
            }

            // the local revision at the last server update
            long lastPostRevision = (Long) record.getAttribute("local_revision");
            if (request.getFromVersion() > lastPostRevision) {
                throw new GSSException(
                        "Invalid fromVersion, " + request.getFromVersion() + ", it is more recent than " +
                        		"the latest PostDiff synchronisation: " + lastPostRevision,
                        GSSExceptionCode.InvalidParameterValue, FROM_VERSION);
            }

            // ok, we need to find what revisions we have to jump over (the synch ones)
            VersioningDataStore ds = core.getVersioningStore();

            // gather all records in the synch history that happened after the requested revision
            DefaultQuery q = new DefaultQuery();
            String tableName = request.getTypeName().getLocalPart();
            Filter tableFilter = ff.equal(ff.property("table_name"), ff.literal(tableName), true);
            Filter revisionFilter = ff.greater(ff.property("local_revision"), ff.literal(request
                    .getFromVersion()));
            q.setFilter(ff.and(tableFilter, revisionFilter));
            q.setSortBy(new SortBy[] { ff.sort("local_revision", SortOrder.ASCENDING) });
            fi = ds.getFeatureSource(SYNCH_HISTORY).getFeatures(q).features();

            // build a list so that taking elements pair-wise we get the intervals we need to query
            // (we won't sent local changes happened after the last PostDiff as there is no way
            // to know if they would conflict with Central or not)
            List<Long> intervals = new ArrayList<Long>();
            intervals.add(request.getFromVersion());
            while (fi.hasNext()) {
                intervals.add((Long) fi.next().getAttribute("local_revision"));
            }
            fi.close();

            TransactionType transaction;
            if(intervals.size() > 1) {
                // gather the ids of the features still under conflict, we don't want to load their
                // diffs
                Filter nonConflictingFilter = getFidConflictFilter(tableName,
                        getActiveConflicts(tableName));
    
                // gather all of the diff readers for the non conflicting features
                VersioningFeatureSource fs = (VersioningFeatureSource) ds.getFeatureSource(tableName);
                FeatureDiffReader[] readers = new FeatureDiffReader[intervals.size() - 1];
                for (int i = 1; i < intervals.size(); i++) {
                    // mind we need to skip the actual synch points, so we subtract 1
                    // from the revision number
                    String fromVersion = String.valueOf(intervals.get(i - 1));
                    String toVersion = String.valueOf(intervals.get(i) - 1);
    
                    Filter filter = nonConflictingFilter;
                    // skip over all the clean merges
                    Filter cleanMerges = getFidConflictFilter(tableName, getCleanMerges(tableName,
                            intervals.get(i)));
                    if (cleanMerges != Filter.INCLUDE) {
                        if (filter != Filter.INCLUDE) {
                            filter = ff.and(cleanMerges, filter);
                        } else {
                            filter = cleanMerges;
                        }
                    }
    
                    readers[i - 1] = fs.getDifferences(fromVersion, toVersion, filter, null);
                }
    
                // now we need to merge the readers into a global set of changes, skip
                // the changes on the conflicting features, and turn everything into a
                // transaction (easy no?)
                FeatureDiffReader differences = new MergingFeatureDiffReader(readers);
                transaction = new VersioningTransactionConverter().convert(differences,
                        TransactionType.class);
            } else {
                // no local changes to return, it happens only if we never had local changes
                // in the current history
                transaction = WfsFactory.eINSTANCE.createTransactionType();
            }
            transaction.setReleaseAction(null);
            transaction.setVersion(null);
            transaction.setService(null);
            LOGGER.info("Sending back to Central " + core.countChanges(transaction) + " changes");

            GetDiffResponseType response = new GetDiffResponseType();
            response.setFromVersion(request.getFromVersion());
            response.setToVersion(lastPostRevision);
            response.setTypeName(request.getTypeName());
            response.setTransaction(transaction);
            return response;
        } catch (IOException e) {
            throw new GSSException("Failed to compute the GetDiff response", e);
        } finally {
            if (fi != null) {
                fi.close();
            }
        }
    }

    /**
     * Gathers all of the conflicts ids and builds a fid filter excluding all of them
     * 
     * @return
     */
    Filter getFidConflictFilter(String tableName,
            FeatureCollection<SimpleFeatureType, SimpleFeature> conflicts) {
        FeatureIterator<SimpleFeature> fi = null;
        try {
            fi = conflicts.features();
            Set<FeatureId> conflictIds = new HashSet<FeatureId>();
            while (fi.hasNext()) {
                conflictIds.add(ff
                        .featureId(tableName + "." + fi.next().getAttribute("feature_id")));
            }
            fi.close();
            return conflictIds.size() > 0 ? ff.not(ff.id(conflictIds)) : Filter.INCLUDE;
        } finally {
            if (fi != null) {
                fi.close();
            }
        }
    }

    /**
     * Returns true if the feature diff and the update element would apply the same change
     */
    boolean isSameUpdate(FeatureDiff fd, UpdateElementType update) {
        List<PropertyType> updateProperties = update.getProperty();
        Set<String> fdAttributes = new HashSet<String>(fd.getChangedAttributes());
        if (updateProperties.size() != fdAttributes.size()) {
            return false;
        }

        for (PropertyType pt : updateProperties) {
            String attName = pt.getName().getLocalPart();
            if (!fdAttributes.contains(attName)) {
                return false;
            }

            // compare the values (mind, the upValue comes from a parser, might
            // not be the right type, use converters)
            Object fdValue = fd.getFeature().getAttribute(attName);
            Object upValue = pt.getValue();

            if (fdValue == null && upValue == null) {
                continue;
            } else if (fdValue != null && upValue != null) {
                Class target = fd.getFeature().getType().getDescriptor(attName).getType()
                        .getBinding();
                upValue = Converters.convert(upValue, target);
                if (upValue == null) {
                    // could not perform a conversion to the target type, evidently not equal
                    return false;
                }
                // ok, same type, they should be comparable now
                if (!fdValue.equals(upValue)) {
                    return false;
                }
            } else {
                // one is null, the other is not
                return false;
            }
        }

        // did we really manage to go thru all those checks? Wow, it's the same change all right
        return true;
    }

    /**
     * Finds the update element that's modifying a certain id (assuming the updates are using ID
     * filters)
     */
    UpdateElementType findUpdate(String featureId, List<UpdateElementType> updates) {
        for (UpdateElementType update : updates) {
            Set<Identifier> ids = ((Id) update.getFilter()).getIdentifiers();
            for (Identifier id : ids) {
                if (id.toString().equals(featureId)) {
                    return update;
                }
            }
        }
        return null;
    }

    /**
     * Rolls back the locally deleted feature and store a conflict record
     * 
     * @param lastLocalRevisionId
     * @param id
     */
    void handleDeletionConflict(VersioningFeatureStore layer, VersioningFeatureStore conflicts,
            String lastLocalRevisionId, long newLocalRevision, String id) throws IOException {
        // create a conflict feature. For local deletions we just don't store the feature
        SimpleFeature conflict = SimpleFeatureBuilder.build(conflicts.getSchema(), new Object[] {
                layer.getSchema().getTypeName(), // table_name
                id.substring(id.lastIndexOf(".") + 1), // feature uuid
                newLocalRevision, // local revision
                new Date(), // date created
                'c', // not resolved, conflict
                null, // date fixed
                null // local feature, none since it was locally removed
                }, null);
        conflicts.addFeatures(collection(conflict));

        // roll back the local changes
        Id filter = ff.id(Collections.singleton(ff.featureId(id)));
        layer.rollback(lastLocalRevisionId, filter, null);
    }

    /**
     * Stores a clean merge marker
     * 
     * @param lastLocalRevisionId
     * @param id
     */
    void saveCleanMergeMarker(VersioningFeatureStore layer, VersioningFeatureStore conflicts,
            String lastLocalRevisionId, long newLocalRevision, String id) throws IOException {
        // create a conflict feature. For local deletions we just don't store the feature
        SimpleFeature conflict = SimpleFeatureBuilder.build(conflicts.getSchema(), new Object[] {
                layer.getSchema().getTypeName(), // table_name
                id.substring(id.lastIndexOf(".") + 1), // feature uuid
                newLocalRevision, // local revision
                new Date(), // date created
                'm', // clean merge marker
                null, // date fixed
                null // local feature, none since it was locally removed
                }, null);
        conflicts.addFeatures(collection(conflict));
    }

    /**
     * Rolls back the locally modified feature and store a conflict record
     * 
     * @param lastLocalRevisionId
     * @param id
     */
    void handleUpdateConflict(VersioningFeatureStore layer, VersioningFeatureStore conflicts,
            String lastLocalRevisionId, long newLocalRevision, String id) throws IOException {
        // create a conflict feature. For local deletions we just don't store the feature
        SimpleFeature conflict = SimpleFeatureBuilder.build(conflicts.getSchema(), new Object[] {
                layer.getSchema().getTypeName(), // table_name
                id.substring(id.lastIndexOf(".") + 1), // feature uuid
                newLocalRevision, // local revision
                new Date(), // date created
                'c', // resolved?
                null, // date fixed
                toGML3(getFeatureById(layer, id)) // local feature value
                }, null);
        conflicts.addFeatures(collection(conflict));

        // roll back the local changes
        Id filter = ff.id(Collections.singleton(ff.featureId(id)));
        layer.rollback(lastLocalRevisionId, filter, null);
    }

    /**
     * Converts a simple feature to a GML3 representation
     * 
     * @param featureById
     * @return
     * @throws IOException
     */
    String toGML3(SimpleFeature feature) throws IOException {
        Encoder encoder = new Encoder(configuration, configuration.getXSD().getSchema());
        NamespaceInfo nsi = catalog
                .getNamespaceByURI(feature.getType().getName().getNamespaceURI());
        if (nsi != null) {
            encoder.getNamespaces().declarePrefix(nsi.getPrefix(), nsi.getURI());
        }
        encoder.setEncoding(Charset.forName("UTF-8"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encoder.encode(feature, GML._Feature, bos);
        return bos.toString("UTF-8");
    }

    /**
     * Parses the representation of a GML3 feature back into a {@link SimpleFeature}
     * 
     * @param gml3
     * @return
     * @throws IOException
     */
    SimpleFeature fromGML3(String gml3) throws IOException {
        try {
            Parser parser = new Parser(configuration);
            parser.setStrict(false);
            return (SimpleFeature) parser.parse(new StringReader(gml3));
        } catch (ParserConfigurationException e) {
            throw (IOException) new IOException("Failure parsing the feature").initCause(e);
        } catch (SAXException e) {
            throw (IOException) new IOException("Failure parsing the feature").initCause(e);
        }
    }

    /**
     * Returns the fids of the features being modified by this {@link UpdateElementType} or by this
     * {@link DeleteElementType} The code assumes changes only contain fid filters (we know that
     * since the versioning datastore only generates that kind of filter in diff transactions)
     */
    Set<FeatureId> getEObjectFids(List<? extends EObject> objects) {
        Set<FeatureId> ids = new HashSet<FeatureId>();
        for (EObject object : objects) {
            Filter f = (Filter) EMFUtils.get(object, "filter");
            if (!(f instanceof Id)) {
                throw new GSSException("Unexpected filter type, GSS can only handle FID filters");
            }

            for (Identifier id : ((Id) f).getIdentifiers()) {
                ids.add((FeatureId) id);
            }
        }

        return ids;
    }

    /**
     * Retrieves a single feature by id from the feature source. Returns null if no feature with
     * that id is found
     */
    SimpleFeature getFeatureById(FeatureSource<SimpleFeatureType, SimpleFeature> fs, String id)
            throws IOException {
        Filter filter = ff.id(Collections.singleton(ff.featureId(id)));
        FeatureIterator<SimpleFeature> fi = null;
        try {
            fi = fs.getFeatures(filter).features();
            if (fi.hasNext()) {
                return fi.next();
            } else {
                return null;
            }
        } finally {
            fi.close();
        }
    }

    /**
     * Returns all the active conflicts for the specified table
     */
    FeatureCollection<SimpleFeatureType, SimpleFeature> getActiveConflicts(String tableName)
            throws IOException {
        VersioningDataStore ds = core.getVersioningStore();
        VersioningFeatureSource conflicts = (VersioningFeatureSource) ds
                .getFeatureSource(SYNCH_CONFLICTS);

        Filter unresolved = ff.equals(ff.property("state"), ff.literal(String.valueOf('c')));
        Filter tableFilter = ff.equals(ff.property("table_name"), ff.literal(tableName));
        return conflicts.getFeatures(ff.and(unresolved, tableFilter));
    }

    /**
     * Returns the clean merges occurred at the specified revision
     */
    FeatureCollection<SimpleFeatureType, SimpleFeature> getCleanMerges(String tableName,
            long revision) throws IOException {
        VersioningDataStore ds = core.getVersioningStore();
        VersioningFeatureSource conflicts = (VersioningFeatureSource) ds
                .getFeatureSource(SYNCH_CONFLICTS);

        Filter unresolved = ff.equals(ff.property("state"), ff.literal(String.valueOf('m')));
        Filter tableFilter = ff.equals(ff.property("table_name"), ff.literal(tableName));
        Filter version = ff.equals(ff.property("local_revision"), ff.literal(revision));
        return conflicts.getFeatures(ff.and(ff.and(unresolved, tableFilter), version));
    }

}
