package org.geoserver.wfsv;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import net.opengis.wfs.InsertedFeatureType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfsv.RollbackType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.TransactionElementHandler;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionListener;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTransactionException;
import org.geotools.data.FeatureDiff;
import org.geotools.data.FeatureDiffReader;
import org.geotools.data.VersioningFeatureSource;
import org.geotools.data.VersioningFeatureStore;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Handles the extended rollback elements
 * 
 * @author Andrea Aime - TOPP
 */
public class RollbackElementHandler implements TransactionElementHandler {

    private WFSInfo wfs;

    private FilterFactory filterFactory;

    public RollbackElementHandler(GeoServer gs, FilterFactory filterFactory) {
        this.wfs = gs.getService( WFSInfo.class );
        this.filterFactory = filterFactory;
    }

    public void checkValidity(EObject element, Map featureTypeInfos) throws WFSTransactionException {
        // let's check we can perfom inserts, updates and deletes
        if (!wfs.getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_INSERT)) {
            throw new WFSException("Transaction INSERT support is not enabled "
                    + "(required for rollback)");
        }

        if (!wfs.getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_UPDATE)) {
            throw new WFSException("Transaction UPDATE support is not enabled "
                    + "(required for rollback)");
        }

        if (!wfs.getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_DELETE)) {
            throw new WFSException("Transaction DELETE support is not enabled "
                    + "(required for rollback)");
        }

        // then, make sure we're hitting a versioning datastore
        RollbackType rollback = (RollbackType) element;
        FeatureTypeInfo info = (FeatureTypeInfo) featureTypeInfos.get(rollback.getTypeName());
        

        try {
            if (!(info.getFeatureSource(null,null) instanceof VersioningFeatureSource)) {
                throw new WFSTransactionException("Cannot perform a rollback on "
                        + info.getName() + " since the backing data store is not versioning",
                        "", rollback.getHandle());
            }
        } catch (IOException e) {
            throw new WFSTransactionException("Cannot get the feature source for feature type "
                    + info.getName(), e, rollback.getHandle());
        }

        // TODO: we should check the user attribute, but for the moment
        // we don't have an authentication subsystem
    }

    public void execute(EObject element, TransactionType request, Map featureStores,
            TransactionResponseType response, TransactionListener listener)
            throws WFSTransactionException {
        RollbackType rollback = (RollbackType) element;
        final QName layerName = rollback.getTypeName();
        VersioningFeatureStore vstore = (VersioningFeatureStore) featureStores.get(layerName);
        if(vstore == null)
            throw new WFSTransactionException("Could not locate feature type " + layerName);
        
        long inserted = response.getTransactionSummary().getTotalInserted().longValue();
        long updated = response.getTransactionSummary().getTotalUpdated().longValue();
        long deleted = response.getTransactionSummary().getTotalDeleted().longValue();

        FeatureDiffReader reader = null;

        try {
            // we use the difference to compute the number of inserted,
            // updated and deleted features, but we can't use these to
            // actually perform the rollback, since we would be unable to
            // preserve the fids of the ones that were deleted and need to
            // be re-inserted
            Filter filter = (rollback.getFilter() != null) ? (Filter) rollback.getFilter()
                    : Filter.INCLUDE;
            String version = rollback.getToFeatureVersion();
            String user = rollback.getUser();
            String[] users = ((user != null) && !user.trim().equals("")) ? new String[] { user }
                    : null;
            reader = vstore.getDifferences("LAST", version, filter, users);

            Set insertedIds = new HashSet();
            Set updatedIds = new HashSet();
            Set deletedIds = new HashSet();
            while (reader.hasNext()) {
                FeatureDiff fd = reader.next();

                if (fd.getState() == FeatureDiff.INSERTED) {
                    inserted++;

                    InsertedFeatureType insertedFeature = WfsFactory.eINSTANCE
                            .createInsertedFeatureType();
                    insertedFeature.setHandle(rollback.getHandle());
                    insertedFeature.getFeatureId().add(filterFactory.featureId(fd.getID()));
                    response.getInsertResults().getFeature().add(insertedFeature);
                    // accumulate fids for transaction event handling
                    insertedIds.add(filterFactory.featureId(fd.getID()));
                } else if (fd.getState() == FeatureDiff.UPDATED) {
                    updated++;
                    // accumulate fids for transaction event handling
                    updatedIds.add(filterFactory.featureId(fd.getID()));
                } else if (fd.getState() == FeatureDiff.DELETED) {
                    deleted++;
                    // accumulate fids for transaction event handling
                    deletedIds.add(filterFactory.featureId(fd.getID()));
                }
            }

            // build filters
            Filter insertedFilter = filterFactory.id(insertedIds);
            Filter updatedFilter = filterFactory.id(updatedIds);
            Filter deletedFilter = filterFactory.id(deletedIds);

            // notify pre-update and pre-delete
            
            listener.dataStoreChange(new TransactionEvent(TransactionEventType.PRE_UPDATE, request,
                    layerName, vstore.getFeatures(updatedFilter), rollback));
            listener.dataStoreChange(new TransactionEvent(TransactionEventType.PRE_DELETE, request,
                    layerName, vstore.getFeatures(deletedFilter), rollback));

            // now do the actual rollback
            try {
                vstore.rollback(version, (Filter) rollback.getFilter(), users);
            } catch (Exception e) {
                throw new WFSTransactionException("Could not perform the rollback", e, rollback
                        .getHandle());
            }

            // notify post update and post insert
            listener.dataStoreChange(new TransactionEvent(TransactionEventType.POST_INSERT,
                    request, layerName, vstore.getFeatures(insertedFilter)));
            listener.dataStoreChange(new TransactionEvent(TransactionEventType.POST_UPDATE,
                    request, layerName, vstore.getFeatures(updatedFilter)));

            // update summary information
            response.getTransactionSummary().setTotalInserted(BigInteger.valueOf(inserted));
            response.getTransactionSummary().setTotalUpdated(BigInteger.valueOf(updated));
            response.getTransactionSummary().setTotalDeleted(BigInteger.valueOf(deleted));
        } catch (IOException e) {
            throw new WFSTransactionException("Could not perform the rollback", e, rollback
                    .getHandle());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }

    public Class getElementClass() {
        return RollbackType.class;
    }

    public QName[] getTypeNames(EObject element) throws WFSTransactionException {
        RollbackType rollback = (RollbackType) element;

        return new QName[] { (QName) rollback.getTypeName() };
    }
}
