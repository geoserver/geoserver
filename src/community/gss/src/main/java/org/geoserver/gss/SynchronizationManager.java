/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import static org.geoserver.gss.GSSCore.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gss.GSSInfo.GSSMode;
import org.geoserver.wfsv.VersioningTransactionConverter;
import org.geotools.data.DefaultQuery;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureDiffReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.VersioningDataStore;
import org.geotools.data.VersioningFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * This object gets periodically invoked to perform all outstanding layer synchronisations with the
 * units
 * 
 * @author Andrea Aime - OpenGeo
 */
public class SynchronizationManager extends TimerTask {

    static final Logger LOGGER = Logging.getLogger(DefaultGeoServerSynchronizationService.class);

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    Catalog catalog;

    GSSCore core;

    GSSClientFactory clientFactory;

    public SynchronizationManager(GeoServer geoServer, GSSClientFactory clientFactory) {
        this.catalog = geoServer.getCatalog();
        this.core = new GSSCore(geoServer);
        this.clientFactory = clientFactory;
    }

    /**
     * Runs the synchronisation. To be used by {@link Timer}, if you need to manually run the
     * synchronization please invoke {@link #synchronizeOustandlingLayers()} instead.
     */
    @Override
    public void run() {
        try {
            synchronizeOustandlingLayers();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while running the scheduled synchronisation",
                    e);
        }
    }

    /**
     * Runs the synchronisation on all unit layers that require it (all the ones that haven't
     * synchronised according to the requested frequency and that are inside the call window)
     * 
     * @throws IOException
     */
    public void synchronizeOustandlingLayers() throws IOException {
        // make sure we're properly configured
        core.ensureEnabled();
        
        if (core.getMode() != GSSMode.Central) {
            return;
        }

        FeatureIterator<SimpleFeature> fi = null;
        FeatureIterator<SimpleFeature> li = null;
        try {
            // grab the layers to be synchronised
            VersioningDataStore ds = core.getVersioningStore();
            FeatureSource<SimpleFeatureType, SimpleFeature> outstanding = ds
                    .getFeatureSource(SYNCH_OUTSTANDING);
            DefaultQuery q = new DefaultQuery(SYNCH_OUTSTANDING);
            q.setSortBy(new SortBy[] { ff.sort("last_synchronization", SortOrder.ASCENDING) });

            LOGGER.info("Performing scheduled synchronisation");

            fi = outstanding.getFeatures(q).features();

            // the set of units we failed to synchronize with. The problem might be a connection
            // timeout, we don't really want to multiply the timeout by the number of layers
            Set<Integer> unitBlacklist = new HashSet<Integer>();

            while (fi.hasNext()) {
                // extract relevant attributes
                SimpleFeature layer = fi.next();
                int unitId = (Integer) layer.getAttribute("unit_id");
                int tableId = (Integer) layer.getAttribute("table_id");
                String unitName = (String) layer.getAttribute("unit_name");
                String tableName = (String) layer.getAttribute("table_name");
                String address = (String) layer.getAttribute("unit_address");
                String user = (String) layer.getAttribute("synch_user");
                String password = (String) layer.getAttribute("synch_password");
                Long getDiffCentralRevision = (Long) layer.getAttribute("getdiff_central_revision");
                Long lastUnitRevision = (Long) layer.getAttribute("last_unit_revision");

                // avoid the unit that already failed this run, we'll try next run
                if (unitBlacklist.contains(unitId)) {
                    LOGGER.log(Level.INFO, "Unit " + unitName + " is blacklisted "
                            + "for this run, skipping " + tableName);
                    continue;
                }

                Transaction transaction = null;
                try {
                    // build the transaction with the proper author and commit message
                    transaction = new DefaultTransaction();

                    // get the last central revision the client knows about
                    GSSClient client = getClient(address, user, password);
                    QName layerName = getLayerName(tableName);
                    long clientCentralRevision = client.getCentralRevision(layerName);

                    // compute the diff that we have to send the client. Notice that we have
                    // to skip over the local change occurred when we last performed a GetDiff
                    // against the client
                    VersioningFeatureStore fs = (VersioningFeatureStore) ds
                            .getFeatureSource(tableName);
                    fs.setTransaction(transaction);
                    String fromRevision = clientCentralRevision == -1 ? "FIRST" : String
                            .valueOf(clientCentralRevision);
                    TransactionType centralChanges;
                    LOGGER.log(Level.INFO, "About to compute PostDiff changes. Last central revision known to client " + clientCentralRevision + ", last GetDiff central revision " + getDiffCentralRevision);
                    if (getDiffCentralRevision == null || clientCentralRevision >= getDiffCentralRevision) {
                        // either first time or we don't need to make jumps
                        LOGGER.log(Level.INFO, "First PostDiff or clientRevion same as the last central one, computing diff from " + fromRevision +  " to LAST");
                        FeatureDiffReader fdr = fs.getDifferences(fromRevision, "LAST", null, null);
                        centralChanges = new VersioningTransactionConverter().convert(fdr,
                                TransactionType.class);
                    } else  {
                        // we need to jump over the last local changes
                        String before = String.valueOf(getDiffCentralRevision - 1);
                        String after = String.valueOf(getDiffCentralRevision);
                        LOGGER.log(Level.INFO, "Client revision lower than the server one, computing diff from " + fromRevision +  " to " + before + " and merging with diffs from " + after + " to LAST");
                        FeatureDiffReader fdr1 = fs.getDifferences(fromRevision, before, null, null);
                        FeatureDiffReader fdr2 = fs.getDifferences(after, "LAST", null, null);
                        FeatureDiffReader[] fdr = new FeatureDiffReader[] { fdr1, fdr2 };
                        centralChanges = new VersioningTransactionConverter().convert(fdr,
                                TransactionType.class);
                    }

                    // what is the latest change on this layer? (worst case it's the last GetDiff
                    // from this Unit)
                    long lastCentralRevision = clientCentralRevision;
                    li = fs.getLog("LAST", fromRevision, null, null, 1).features();
                    if (li.hasNext()) {
                        lastCentralRevision = (Long) li.next().getAttribute("revision");
                    }
                    li.close();

                    // finally run the PostDiff
                    PostDiffType postDiff = new PostDiffType();
                    postDiff.setTypeName(layerName);
                    postDiff.setFromVersion(clientCentralRevision);
                    postDiff.setToVersion(lastCentralRevision);
                    postDiff.setTransaction(centralChanges);
                    client.postDiff(postDiff);

                    // grab the changes from the client and apply them locally
                    GetDiffType getDiff = new GetDiffType();
                    getDiff.setFromVersion(lastUnitRevision == null ? -1 : lastUnitRevision);
                    getDiff.setTypeName(layerName);
                    GetDiffResponseType gdr = client.getDiff(getDiff);
                    TransactionType unitChanges = gdr.getTransaction();
                    core.applyChanges(unitChanges, fs);
                    
                    // mark down this layer as succesfully synchronised
                    FeatureStore<SimpleFeatureType, SimpleFeature> tuMetadata = (FeatureStore<SimpleFeatureType, SimpleFeature>) ds
                            .getFeatureSource(SYNCH_UNIT_TABLES);
                    tuMetadata.setTransaction(transaction);
                    SimpleFeatureType tuSchema = tuMetadata.getSchema();
                    int unitChangeCount = core.countChanges(unitChanges);
                    int centralChangeCount = core.countChanges(centralChanges);
                    if (unitChangeCount == 0 && centralChangeCount == 0) {
                        // just update the last_synch marker, as nothing else happened and
                        // this way we can avoid eating away central revision number (which
                        // might go up very rapidly otherwise)
                        AttributeDescriptor[] atts = new AttributeDescriptor[] { tuSchema
                                .getDescriptor("last_synchronization") };
                        Object[] values = new Object[] { new Date() };
                        Filter filter = ff.and(ff.equals(ff.property("table_id"), ff
                                .literal(tableId)), ff.equals(ff.property("unit_id"), ff
                                .literal(unitId)));
                        tuMetadata.modifyFeatures(atts, values, filter);
                    } else {
                        AttributeDescriptor[] atts = new AttributeDescriptor[] {
                                tuSchema.getDescriptor("last_synchronization"),
                                tuSchema.getDescriptor("getdiff_central_revision"),
                                tuSchema.getDescriptor("last_unit_revision") };
                        Object[] values = new Object[] { new Date(),
                                Long.parseLong(fs.getVersion()), gdr.getToVersion() };
                        Filter filter = ff.and(ff.equals(ff.property("table_id"), ff
                                .literal(tableId)), ff.equals(ff.property("unit_id"), ff
                                .literal(unitId)));
                        tuMetadata.modifyFeatures(atts, values, filter);
                    }

                    // mark the unit as succeffully updated
                    updateUnitStatus(ds, transaction, unitId, false);
                    
                    // the the commit log
                    transaction.putProperty(VersioningDataStore.AUTHOR, "gss");
                    transaction.putProperty(VersioningDataStore.MESSAGE, "Synchronizing with Unit '" 
                            + unitName + "' on table '" + tableName + "': " + centralChangeCount 
                            + " changes sent and " + unitChangeCount + " changes received");

                    // close up
                    transaction.commit();
                    LOGGER.log(Level.INFO, "Successfull synchronisation of table " + tableName
                            + " for unit " + unitName + "(" + centralChangeCount
                            + " changes sent to the Unit, " + unitChangeCount
                            + " change incoming from the Unit)");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Synchronisation of table " + tableName + " for unit "
                            + unitName + " failed", e);

                    // rollback all current changes
                    transaction.rollback();

                    // if anything at all went bad mark the layer synch as failed
                    FeatureStore<SimpleFeatureType, SimpleFeature> tuMetadata = (FeatureStore<SimpleFeatureType, SimpleFeature>) ds
                            .getFeatureSource(SYNCH_UNIT_TABLES);
                    SimpleFeatureType tuSchema = tuMetadata.getSchema();
                    AttributeDescriptor[] atts = new AttributeDescriptor[] { tuSchema
                            .getDescriptor("last_failure"), };
                    Object[] values = new Object[] { new Date() };
                    Filter filter = ff.and(ff.equals(ff.property("table_id"), ff.literal(tableId)),
                            ff.equals(ff.property("unit_id"), ff.literal(unitId)));
                    tuMetadata.modifyFeatures(atts, values, filter);

                    // mark the unit as failed
                    updateUnitStatus(ds, Transaction.AUTO_COMMIT, unitId, true);

                    // blacklist the unit, we'll retry later
                    unitBlacklist.add(unitId);
                } finally {
                    if (transaction != null) {
                        transaction.close();
                    }
                }
            }
        } finally {
            if (fi != null) {
                fi.close();
            }
            if (li != null) {
                li.close();
            }
        }

    }

    /**
     * Updates the "errors" flag in the specified unit
     * @param ds
     * @param transaction
     * @param unitId
     * @param success
     * @throws IOException
     */
    void updateUnitStatus(VersioningDataStore ds, Transaction transaction, int unitId,
            boolean errors) throws IOException {
        FeatureStore<SimpleFeatureType, SimpleFeature> fs = (FeatureStore<SimpleFeatureType, SimpleFeature>) ds
                .getFeatureSource(SYNCH_UNITS);

        SimpleFeatureType tuSchema = fs.getSchema();
        AttributeDescriptor[] atts = new AttributeDescriptor[] { tuSchema.getDescriptor("errors") };
        Object[] values = new Object[] { errors };
        Filter filter = ff.id(Collections.singleton(ff.featureId(SYNCH_UNITS + "." + unitId)));

        fs.setTransaction(transaction);
        fs.modifyFeatures(atts, values, filter);
    }

    /**
     * Turns a table name into a fully qualified layer name
     * 
     * @param tableName
     * @return
     */
    QName getLayerName(String tableName) {
        String wsName = core.getVersioningStoreInfo().getWorkspace().getName();
        NamespaceInfo ns = catalog.getNamespaceByPrefix(wsName);
        return new QName(ns.getURI(), tableName, ns.getPrefix());
    }

    protected GSSClient getClient(String address, String username, String password)
            throws MalformedURLException {
        return clientFactory.createClient(new URL(address), username, password);
    }

}
