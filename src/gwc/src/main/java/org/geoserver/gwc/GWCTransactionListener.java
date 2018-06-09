/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs.*;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.*;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Listens to transactions (so far only issued by WFS) and truncates the cache for the affected area
 * of the layers involved in the transaction.
 *
 * <p>A Spring bean singleton of this class needs to be declared in order for GeoServer transactions
 * to pick it up automatically and forward transaction events to it.
 *
 * <p>TODO: upon deletion, only truncate if feature count > 0
 *
 * @author Arne Kepp
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GWCTransactionListener implements TransactionCallback {

    private static Logger log = Logging.getLogger(GWCTransactionListener.class);

    private final GWC gwc;

    static final String GWC_TRANSACTION_INFO_PLACEHOLDER = "GWC_TRANSACTION_INFO_PLACEHOLDER";

    /** @param gwc */
    public GWCTransactionListener(final GWC gwc) {
        this.gwc = gwc;
    }

    /**
     * Not used, we're interested in the {@link #dataStoreChange} and {@link #afterTransaction}
     * hooks
     */
    public TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException {
        // nothing to do
        return request;
    }

    /**
     * Not used, we're interested in the {@link #dataStoreChange} and {@link #afterTransaction}
     * hooks
     *
     * @see org.geoserver.wfs.TransactionPlugin#beforeCommit(net.opengis.wfs.TransactionRequest)
     */
    public void beforeCommit(TransactionRequest request) throws WFSException {
        // nothing to do
    }

    /**
     * If transaction's succeeded then truncate the affected layers at the transaction affected
     * bounds
     *
     * @see org.geoserver.wfs.TransactionPlugin#afterTransaction
     */
    public void afterTransaction(
            final TransactionRequest request, TransactionResponse result, boolean committed) {
        if (!committed) {
            return;
        }
        try {
            afterTransactionInternal(request, committed);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error trying to truncate the transaction affected area", e);
        }
    }

    private void afterTransactionInternal(final TransactionRequest transaction, boolean committed) {

        final Map<String, List<ReferencedEnvelope>> byLayerDirtyRegions =
                getByLayerDirtyRegions(transaction);
        if (byLayerDirtyRegions.isEmpty()) {
            return;
        }
        for (String tileLayerName : byLayerDirtyRegions.keySet()) {
            List<ReferencedEnvelope> dirtyList = byLayerDirtyRegions.get(tileLayerName);
            ReferencedEnvelope dirtyRegion;
            try {
                dirtyRegion = merge(tileLayerName, dirtyList);
            } catch (Exception e) {
                log.log(Level.WARNING, e.getMessage(), e);
                continue;
            }
            if (dirtyRegion == null) {
                continue;
            }
            try {
                gwc.truncate(tileLayerName, dirtyRegion);
            } catch (GeoWebCacheException e) {
                log.warning(
                        "Error truncating tile layer "
                                + tileLayerName
                                + " for transaction affected bounds "
                                + dirtyRegion);
            }
        }
    }

    private ReferencedEnvelope merge(
            final String tileLayerName, final List<ReferencedEnvelope> dirtyList)
            throws TransformException, FactoryException {
        if (dirtyList.size() == 0) {
            return null;
        }

        final CoordinateReferenceSystem declaredCrs =
                CRS.getHorizontalCRS(gwc.getDeclaredCrs(tileLayerName));
        ReferencedEnvelope merged = new ReferencedEnvelope(declaredCrs);
        for (ReferencedEnvelope env : dirtyList) {
            if (env instanceof ReferencedEnvelope3D) {
                env =
                        new ReferencedEnvelope(
                                env, CRS.getHorizontalCRS(env.getCoordinateReferenceSystem()));
            }
            ReferencedEnvelope transformedDirtyRegion = env.transform(declaredCrs, true, 1000);
            merged.expandToInclude(transformedDirtyRegion);
        }
        return merged;
    }

    /**
     * @return {@code 0}, we don't need any special treatment
     * @see org.geoserver.wfs.TransactionPlugin#getPriority()
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Collects the per TileLayer affected bounds
     *
     * @see
     *     org.geoserver.wfs.TransactionListener#dataStoreChange(org.geoserver.wfs.TransactionEvent)
     */
    public void dataStoreChange(final TransactionEvent event) throws WFSException {
        log.info("DataStoreChange: " + event.getLayerName() + " " + event.getType());
        try {
            dataStoreChangeInternal(event);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error pre computing the transaction's affected area", e);
        }
    }

    private void dataStoreChangeInternal(final TransactionEvent event) {
        final Object source = event.getSource();
        if (!(source instanceof InsertElementType
                || source instanceof UpdateElementType
                || source instanceof DeleteElementType)) {
            return;
        }

        final EObject originatingTransactionRequest = (EObject) source;
        checkNotNull(originatingTransactionRequest, "No original transaction request exists");
        final TransactionEventType type = event.getType();
        if (TransactionEventType.POST_INSERT.equals(type)) {
            // no need to compute the bounds, they're the same than for PRE_INSERT
            return;
        }
        final QName featureTypeName = event.getLayerName();
        final Set<String> affectedTileLayers =
                gwc.getTileLayersByFeatureType(
                        featureTypeName.getNamespaceURI(), featureTypeName.getLocalPart());
        if (affectedTileLayers.isEmpty()) {
            // event didn't touch a cached layer
            return;
        }

        final SimpleFeatureCollection affectedFeatures = event.getAffectedFeatures();
        final ReferencedEnvelope affectedBounds = affectedFeatures.getBounds();

        final TransactionType transaction = event.getRequest();
        TransactionRequest request = TransactionRequest.adapt(transaction);

        for (String tileLayerName : affectedTileLayers) {
            addLayerDirtyRegion(request, tileLayerName, affectedBounds);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<ReferencedEnvelope>> getByLayerDirtyRegions(
            final TransactionRequest transaction) {

        final Map<Object, Object> extendedProperties = transaction.getExtendedProperties();
        Map<String, List<ReferencedEnvelope>> byLayerDirtyRegions;
        byLayerDirtyRegions =
                (Map<String, List<ReferencedEnvelope>>)
                        extendedProperties.get(GWC_TRANSACTION_INFO_PLACEHOLDER);
        if (byLayerDirtyRegions == null) {
            byLayerDirtyRegions = new HashMap<String, List<ReferencedEnvelope>>();
            extendedProperties.put(GWC_TRANSACTION_INFO_PLACEHOLDER, byLayerDirtyRegions);
        }
        return byLayerDirtyRegions;
    }

    private void addLayerDirtyRegion(
            final TransactionRequest transaction,
            final String tileLayerName,
            final ReferencedEnvelope affectedBounds) {

        Map<String, List<ReferencedEnvelope>> byLayerDirtyRegions =
                getByLayerDirtyRegions(transaction);

        List<ReferencedEnvelope> layerDirtyRegion = byLayerDirtyRegions.get(tileLayerName);
        if (layerDirtyRegion == null) {
            layerDirtyRegion = new ArrayList<ReferencedEnvelope>(2);
            byLayerDirtyRegions.put(tileLayerName, layerDirtyRegion);
        }
        layerDirtyRegion.add(affectedBounds);
    }
}
