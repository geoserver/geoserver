/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.Delete;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureLockException;
import org.geotools.api.data.FeatureLocking;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.FeatureWriter;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureLocking;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.jts.geom.Envelope;

/**
 * Processes standard Delete elements
 *
 * @author Andrea Aime - TOPP
 */
public class DeleteElementHandler extends AbstractTransactionElementHandler {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    FilterFactory filterFactory;

    public DeleteElementHandler(GeoServer gs) {
        super(gs);
        filterFactory = CommonFactoryFinder.getFilterFactory(null);
    }

    @Override
    public Class<Delete> getElementClass() {
        return Delete.class;
    }

    /** @see org.geoserver.wfs.TransactionElementHandler#getTypeNames(TransactionRequest, TransactionElement) */
    @Override
    public QName[] getTypeNames(TransactionRequest request, TransactionElement element) throws WFSTransactionException {
        return new QName[] {element.getTypeName()};
    }

    @Override
    public void checkValidity(TransactionElement delete, Map featureTypeInfos) throws WFSTransactionException {
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_DELETE)) {
            throw new WFSException(delete, "Transaction Delete support is not enabled");
        }

        Filter f = delete.getFilter();

        if ((f == null) || Filter.INCLUDE.equals(f)) {
            throw new WFSTransactionException("Must specify filter for delete", "MissingParameterValue");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(
            TransactionElement delete,
            TransactionRequest request,
            Map featureStores,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException {

        QName elementName = delete.getTypeName();
        String handle = delete.getHandle();

        long deleted = response.getTotalDeleted().longValue();
        String msg = "Could not locate FeatureStore for '" + elementName + "'";
        if (!featureStores.containsKey(elementName)) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("failed to find " + elementName + " in:");
                for (QName key : (Set<QName>) featureStores.keySet()) {
                    LOGGER.finer("\t" + key.toString());
                }
            }
            throw new WFSTransactionException(msg, ServiceException.INVALID_PARAMETER_VALUE, handle);
        }
        SimpleFeatureStore store = DataUtilities.simple((FeatureStore) featureStores.get(elementName));

        if (store == null) {
            throw new WFSTransactionException(msg, ServiceException.INVALID_PARAMETER_VALUE, handle);
        }

        String typeName = store.getSchema().getTypeName();
        LOGGER.finer("Transaction Delete:" + delete);

        try {
            Filter filter = delete.getFilter();

            // make sure all geometric elements in the filter have a crs, and that the filter
            // is reprojected to store's native crs as well
            CoordinateReferenceSystem declaredCRS =
                    WFSReprojectionUtil.getDeclaredCrs(store.getSchema(), request.getVersion());
            filter = WFSReprojectionUtil.normalizeFilterCRS(filter, store.getSchema(), declaredCRS);

            // notify listeners
            TransactionEvent event = new TransactionEvent(
                    TransactionEventType.PRE_DELETE, request, elementName, store.getFeatures(filter));
            event.setSource(Delete.WFS11.unadapt((Delete) delete));
            listener.dataStoreChange(event);

            // compute damaged area
            Envelope damaged = store.getBounds(new Query(elementName.getLocalPart(), filter));

            if (damaged == null) {
                damaged = store.getFeatures(filter).getBounds();
            }

            if ((request.getLockId() != null) && store instanceof FeatureLocking && (request.isReleaseActionSome())) {
                SimpleFeatureLocking locking = (SimpleFeatureLocking) store;

                // TODO: Revisit Lock/Delete interaction in gt2
                // This a bit better and what should be done, we
                // will need to rework the gt2 locking api to work
                // with fids or something
                //
                // The only other thing that would work
                // would be to specify that FeatureLocking is
                // required to remove locks when removing Features.
                //
                // While that sounds like a good idea, it
                // would be extra work when doing release mode ALL.
                //
                DataStore data = (DataStore) store.getDataStore();
                try (FeatureWriter<SimpleFeatureType, SimpleFeature> writer =
                        data.getFeatureWriter(typeName, filter, store.getTransaction())) {
                    while (writer.hasNext()) {
                        String fid = writer.next().getID();
                        Set<FeatureId> featureIds = new HashSet<>();
                        featureIds.add(filterFactory.featureId(fid));
                        locking.unLockFeatures(filterFactory.id(featureIds));
                        writer.remove();
                        deleted++;
                    }
                }

                store.removeFeatures(filter);
            } else {
                // We don't have to worry about locking right now
                int deletedCount = store.getFeatures(filter).size();
                if (deletedCount > 0) deleted += deletedCount;
                store.removeFeatures(filter);
            }
        } catch (IOException e) {
            String eHandle = delete.getHandle();
            String code = null;

            // check case of feature lock exception and set appropriate exception
            // code
            if (e instanceof FeatureLockException) {
                code = "MissingParameterValue";
            }
            throw exceptionFactory.newWFSTransactionException(
                    "Delete error: " + e.getMessage(), e, code, eHandle, handle);
        }

        // update deletion count
        response.setTotalDeleted(BigInteger.valueOf(deleted));
    }
}
