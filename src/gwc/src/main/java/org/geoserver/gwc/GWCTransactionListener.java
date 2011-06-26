/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geoserver.gwc;

import static org.geoserver.wfs.TransactionEventType.POST_UPDATE;
import static org.geoserver.wfs.TransactionEventType.PRE_DELETE;
import static org.geoserver.wfs.TransactionEventType.PRE_INSERT;
import static org.geoserver.wfs.TransactionEventType.PRE_UPDATE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.springframework.util.Assert;

/**
 * Listens to transactions (so far only issued by WFS) and truncates the cache for the affected area
 * of the layers involved in the transaction.
 * <p>
 * A Spring bean singleton of this class needs to be declared in order for GeoServer transactions to
 * pick it up automatically and forward transaction events to it.
 * </p>
 * <p>
 * TODO: upon deletion, only truncate if feature count > 0
 * </p>
 * 
 * @author Arne Kepp
 * @author Gabriel Roldan
 * @version $Id$
 * 
 */
public class GWCTransactionListener implements TransactionPlugin {

    private static Logger log = Logging.getLogger("org.geoserver.gwc.GWCTransactionListener");

    final private Catalog catalog;

    final private GWC gwc;

    /**
     * Keeps track of the pre-transaction affected bounds on a per
     * {@link TransactionEvent#getSource() transaction request} basis, so that the
     * {@code POST_UPDATE|INSERT|DELETE} bounds are aggregated to these ones before issuing a cache
     * truncation.
     */
    private final Map<EObject, ReferencedEnvelope> affectedBounds;

    private final Map<EObject, Set<String>> affectedLayers;

    public GWCTransactionListener(final Catalog cat, final GWC gwc) {
        this.catalog = cat;
        this.gwc = gwc;
        this.affectedBounds = new ConcurrentHashMap<EObject, ReferencedEnvelope>();
        this.affectedLayers = new ConcurrentHashMap<EObject, Set<String>>();
    }

    /**
     * Not used, we're interested in the {@link #dataStoreChange} and {@link #afterTransaction}
     * hooks
     * 
     * @see org.geoserver.wfs.TransactionPlugin#beforeTransaction(net.opengis.wfs.TransactionType)
     */
    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        // nothing to do
        return request;
    }

    /**
     * Not used, we're interested in the {@link #dataStoreChange} and {@link #afterTransaction}
     * hooks
     * 
     * @see org.geoserver.wfs.TransactionPlugin#beforeCommit(net.opengis.wfs.TransactionType)
     */
    public void beforeCommit(TransactionType request) throws WFSException {
        // nothing to do
    }

    /**
     * If transaction's succeeded then truncate the affected layers at the transaction affected
     * bounds
     * 
     * @see org.geoserver.wfs.TransactionPlugin#afterTransaction(net.opengis.wfs.TransactionType,
     *      boolean)
     */
    public void afterTransaction(TransactionType request, TransactionResponseType result,
            boolean committed) {
        try {
            afterTransactionInternal(request, committed);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error trying to truncate the transaction affected area", e);
        }
    }

    private void afterTransactionInternal(final TransactionType request, boolean committed) {
        final List<EObject> transactionElements = getTransactionElements(request);

        ReferencedEnvelope affectedBounds;
        Set<String> affectedLayers;

        for (EObject transactionElement : transactionElements) {
            affectedBounds = this.affectedBounds.remove(transactionElement);
            affectedLayers = this.affectedLayers.remove(transactionElement);

            if (committed && affectedBounds != null) {
                Assert.notNull(affectedLayers);
                if (affectedBounds.isEmpty()) {
                    continue;
                }
                for (String layerName : affectedLayers) {
                    try {
                        gwc.truncate(layerName, affectedBounds);
                    } catch (GeoWebCacheException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<EObject> getTransactionElements(TransactionType request) {
        List<InsertElementType> insert = request.getInsert();
        List<UpdateElementType> update = request.getUpdate();
        List<DeleteElementType> delete = request.getDelete();
        List<EObject> allTransactionElements = new ArrayList<EObject>();
        allTransactionElements.addAll(insert);
        allTransactionElements.addAll(update);
        allTransactionElements.addAll(delete);
        return allTransactionElements;
    }

    /**
     * @return {@code 0}, we don't need any special treatment
     * @see org.geoserver.wfs.TransactionPlugin#getPriority()
     */
    public int getPriority() {
        return 0;
    }

    /**
     * 
     * @see org.geoserver.wfs.TransactionListener#dataStoreChange(org.geoserver.wfs.TransactionEvent)
     */
    public void dataStoreChange(final TransactionEvent event) throws WFSException {

        try {
            dataStoreChangeInternal(event);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error pre computing the transaction's affected area", e);
        }

    }

    private void dataStoreChangeInternal(final TransactionEvent event) {
        final Object source = event.getSource();
        Assert.isTrue(source instanceof InsertElementType || source instanceof UpdateElementType
                || source instanceof DeleteElementType);

        final EObject originatingTransactionRequest = (EObject) source;

        Assert.notNull(originatingTransactionRequest);

        final TransactionEventType type = event.getType();
        final SimpleFeatureCollection affectedFeatures = event.getAffectedFeatures();

        if (isIgnorablePostEvent(originatingTransactionRequest, type)) {
            // if its a post event and there's no corresponding pre event bbox no need to
            // proceed(Saves some cpu cycles and a catalog lookup for findAffectedCachedLayers).
            return;
        }

        final Set<String> affectedLayers = findAffectedCachedLayers(event);
        if (affectedLayers.isEmpty()) {
            // event didn't touch a cached layer
            return;
        }

        if (PRE_INSERT == type || PRE_UPDATE == type || PRE_DELETE == type) {
            ReferencedEnvelope preBounds = affectedFeatures.getBounds();

            this.affectedLayers.put(originatingTransactionRequest, affectedLayers);
            this.affectedBounds.put(originatingTransactionRequest, preBounds);

        } else if (POST_UPDATE == type && affectedFeatures != null) {

            final ReferencedEnvelope bounds = affectedBounds.get(originatingTransactionRequest);

            // only truncate if the request didn't fail
            ReferencedEnvelope postBounds = affectedFeatures.getBounds();
            Assert.isTrue(bounds.getCoordinateReferenceSystem().equals(
                    postBounds.getCoordinateReferenceSystem()));
            bounds.expandToInclude(postBounds);

        } else {
            throw new IllegalArgumentException("Unrecognized transaction event type: " + type);
        }
    }

    private boolean isIgnorablePostEvent(final Object originatingTransactionRequest,
            final TransactionEventType type) {

        if (POST_UPDATE == type) {
            if (!affectedBounds.containsKey(originatingTransactionRequest)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds out which cached layers are affected by the given transaction event and returns their
     * names, or an empty set if no cached layer is affected by the transaction.
     * <p>
     * NOTE: so far it will always include a plain layer and any LayerGroup the layer is part of,
     * since the geoserver/gwc integration works by automatically making all geoserver layers
     * cacheable. But this might change in the near future, having the options to opt-out of caching
     * on a per layer basis, so beware this method may need to get smarter.
     * </p>
     */
    private Set<String> findAffectedCachedLayers(final TransactionEvent event) {

        final String layerName = getQualifiedLayerName(event);

        Set<String> affectedLayers = findLayerGroupsOf(layerName);

        affectedLayers.add(layerName);

        return affectedLayers;
    }

    private Set<String> findLayerGroupsOf(String layerName) {
        Set<String> affectedLayerGroups = new HashSet<String>();

        for (LayerGroupInfo lgi : catalog.getLayerGroups()) {
            for (LayerInfo li : lgi.getLayers()) {
                if (li.getResource().getPrefixedName().equals(layerName)) {
                    affectedLayerGroups.add(lgi.getName());
                    break;
                }
            }
        }

        return affectedLayerGroups;
    }

    private String getQualifiedLayerName(final TransactionEvent event) {
        final String layerName;

        final QName name = event.getLayerName();
        final String namespaceURI = name.getNamespaceURI();
        final String localName = name.getLocalPart();
        if (!XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
            NamespaceInfo namespaceInfo = catalog.getNamespaceByURI(namespaceURI);
            if (namespaceInfo == null) {
                log.info("Can't find namespace info for layer " + name + ". Cache not truncated");
                throw new NoSuchElementException("Layer not found: " + name);
            }
            String prefix = namespaceInfo.getPrefix();
            layerName = prefix + ":" + localName;
        } else {
            LayerInfo layerInfo = catalog.getLayerByName(localName);
            if (layerInfo == null) {
                log.info("Can't find layer " + localName + ". Cache not truncated");
                throw new NoSuchElementException("Layer not found: " + localName);
            }
            ResourceInfo resource = layerInfo.getResource();
            layerName = resource.getPrefixedName();
        }

        return layerName;
    }

}
