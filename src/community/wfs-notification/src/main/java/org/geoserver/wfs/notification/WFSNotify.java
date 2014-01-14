/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionListener;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.notification.TriggerManager.TriggerCallback;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.Identifier;
import org.springframework.beans.factory.DisposableBean;

/**
 * <p>
 * A {@link TransactionPlugin} and {@link TransactionListener} that receives
 * updates from an ongoing transaction and performs some post-processing on
 * those updates to send out related notifications. This class binds a
 * {@link TransactionStatus} to the the transaction that contains some
 * bookkeeping information. This is only possible due to some extensions to
 * GeoServer.... (TODO: contribute this back in a form acceptable to OpenGeo.)
 * <p>
 * This happens in two phases: pre-delete and post-insert. Pre-delete, related
 * notifications need to be found right then (related feature types queried)
 * because related information might be stored in views. Post-delete, that
 * information will already be gone from the database view.
 * <p>
 * This situation could also potentially happen with inserts, but only the
 * post-insert case is handled right now. TODO ...
 * <p>
 * "Related notifications" to be sent over WSN are accumulated in
 * {@link TransactionStatus} during pre-delete events. Features that need to be
 * processed are also accumulated there during post-insert events. Both types
 * are finally handled during {@link #beforeCommit}.
 * <p>
 * This class is meant to be used in a managed-object container, like Spring.
 * {@link #setTriggerManager(TriggerManager)} must be called after construction with a non-null
 * {@link TriggerManager}, followed by a call to {@link #init()}, at the very least,
 * for objects of this class to minimally functional.
 * <p>
 * To-do items:
 * <ul>
 * <li>TODO: GeoServer doesn't send post-delete notifications, which could be
 * useful.</li>
 * <li>TODO: Handle pre-insert, post-delete cases here.</li>
 * <li>TODO: Contribute extended {@link TransactionPlugin},
 * {@link TransactionListener} classes back to OpenGeo.</li>
 * </ul>
 *
 * @author Dustin Parker &lt;dparker@forwardslope.com&gt;
 */
public class WFSNotify implements TransactionPlugin, TransactionListener, DisposableBean {
    private static final Log LOG = LogFactory.getLog(WFSNotify.class);
    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory(null);

    /**
     * Class that lets code 'spy' on features as they're removed from a {@link FeatureIterator}.
     * 
     * @author dparker
     */
    private static class FeatureIteratorWrapper<F extends Feature> implements FeatureIterator<F> {
        protected final FeatureIterator<F> fi;
        
        public FeatureIteratorWrapper(FeatureIterator<F> fi) {
            this.fi = fi;
        }

        @Override
        public boolean hasNext() {
            return fi.hasNext();
        }

        @Override
        public F next() throws NoSuchElementException {
            return fi.next();
        }

        @Override
        public void close() {
            fi.close();
        }
    }
    
    private static class CollectionsFeatureIterator<F extends Feature> implements FeatureIterator<F> {
        protected final Iterator<F> fi;
        
        public CollectionsFeatureIterator(Iterator<F> fi) {
            this.fi = fi;
        }

        @Override
        public boolean hasNext() {
            return fi.hasNext();
        }

        @Override
        public F next() {
            return fi.next();
        }
        
        @Override
        public void close() {
        }
    }

    protected Catalog catalog;
    protected TriggerManager tm;
    protected GMLNotificationSerializer cb;
    protected NotificationSerializer serializer;
    protected Collection<NotificationPublisher> publishers;
    protected final boolean publishDebug = Boolean.parseBoolean(System
        .getProperty("com.fsi.c2rpc.geoserver.wsn.PublishCallback.debug"));

    private final Map<Object, TransactionStatus> statuses = Collections.synchronizedMap(new WeakHashMap());


    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public void setTriggerManager(TriggerManager tm) {
        this.tm = tm;
    }

    public void setCallback(GMLNotificationSerializer cb) {
        this.cb = cb;
    }

    public void init() {
        if(tm == null) {
            throw new IllegalStateException("TriggerManager was not set, which is required.");
        }
        List<NotificationSerializer> serializers = GeoServerExtensions.extensions(NotificationSerializer.class);
        if(serializers.isEmpty()) {
            LOG.warn("No WFS notification serializers found.");
        } else {
            if(serializers.size() > 1) {
                StringBuilder msg = new StringBuilder("Multiple WFS notification serializers found:\n");
                for(NotificationSerializer serializer : serializers) {
                    msg.append('\t').append(serializer.getClass().getName()).append('\n');
                }
                msg.append("Using first.");
                LOG.warn(msg);
            }
            serializer = serializers.get(0);
        }
        
        publishers = GeoServerExtensions.extensions(NotificationPublisher.class);
        
        if(publishers.isEmpty()) {
            LOG.info("No notification publishers found.");
        } else {
            StringBuilder msg = new StringBuilder("Using the following WFS notification publisher(s):\n");
            for(NotificationPublisher publisher : publishers) {
                msg.append('\t').append(publisher.getClass().getName()).append('\n');
            }
            LOG.info(msg);
        }
    }

    public void dataStoreChange(TransactionEvent event) throws WFSException {
        try {
            // don't notify if we don't have to.
            boolean ready = false;
            for(NotificationPublisher publisher : publishers) {
                ready |= publisher.isReady();
            }
            if(!(ready || publishDebug)) {
                LOG.debug("No publishers ready, skipping notification.");
                return;
            }
            
            if(serializer == null) {
                return;
            }

            final TransactionStatus ts = statuses.get(event.getRequest());
            
            if(ts == null)
                return;

            ts.setTransaction(event.getTransaction());
            
            if(event.getType() == TransactionEventType.PRE_INSERT) {
                preInsert(event, ts);
            } else if(event.getType() == TransactionEventType.PRE_DELETE) {
                preDelete(event, ts);
            } else if(event.getType() == TransactionEventType.POST_INSERT) {
                postInsert(event, ts);
            } else if(event.getType() == TransactionEventType.PRE_UPDATE) {
                preUpdate(event, ts);
            }
        } catch(Throwable t) {
            handleException(t);
        }
    }

    private void handleException(Throwable t) {
        final String message = "Caught throwable during notification attempt, won't rethrow.";
        if(LOG.isDebugEnabled())
            LOG.debug(message, t);
        else
            LOG.warn(message);
    }

    protected void preUpdate(TransactionEvent event, TransactionStatus ts) throws Exception {}

    protected void preInsert(TransactionEvent event, final TransactionStatus ts) throws Exception {
        FeatureIterator<SimpleFeature> affected = getAffectedFeatures(event);
        try {
            affected = new FeatureIteratorWrapper<SimpleFeature>(affected) {
                @Override
                public SimpleFeature next() throws NoSuchElementException {
                    SimpleFeature next = super.next();
                    ts.affected(next);
                    return next;
                }
            };
            
            tm.triggerEvent(affected, getLayerName(event), new TriggerCallback() {
    
                @Override
                public void triggerEvent(Feature f) {
                    ts.modified(f);
                }
            }, ts.getTransaction());
            
        } finally {
            affected.close();
        }
    }

    protected void postInsert(TransactionEvent event, TransactionStatus ts) throws Exception {
    }

    protected void preDelete(final TransactionEvent event, final TransactionStatus ts) throws Exception {
        FeatureIterator<SimpleFeature> affected = getAffectedFeatures(event);
        try {
            affected = new FeatureIteratorWrapper<SimpleFeature>(affected) {
                @Override
                public SimpleFeature next() throws NoSuchElementException {
                    SimpleFeature next = super.next();
                    ts.affected(next);
                    return next;
                }
            };
            
            tm.triggerEvent(affected, getLayerName(event), new TriggerCallback() {
                @Override
                public void triggerEvent(Feature f) {
                    ts.modified(f);
                }
            }, ts.getTransaction());
        } finally {
            affected.close();
        }
    }

    /**
     * Find all features that are about to be inserted, deleted, or updated by this event.
     */
    protected static FeatureIterator<SimpleFeature> getAffectedFeatures(TransactionEvent event) {
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = event.getAffectedFeatures();
        return collection.features();
    }

    protected final QName getLayerName(TransactionEvent event) {
        final QName layerName;
        QName name = event.getLayerName();
        // Normalize the QName if needed
        if(name.getPrefix() == null || name.getPrefix().length() == 0) {
            NamespaceInfo info = catalog.getNamespaceByURI(name.getNamespaceURI());
            if(info == null) {
                throw new NoSuchElementException("No such namespace in our catalog: " + name.getNamespaceURI());
            }
            name = new QName(name.getNamespaceURI(), name.getLocalPart(), info.getPrefix());
        }
        layerName = name;
        return layerName;
    }

    public void destroy() throws Exception {
        LOG.info("GeoServer WSN producer is being destroyed.");
    }

    @Override
    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        try {
            statuses.put(request, (TransactionStatus) new TransactionStatus());
        } catch(Throwable t) {
            handleException(t);
        }
        return request;
    }

    /*
    public void beginTransaction(TransactionType request, Object userData, Transaction transaction) {
        try {
            TransactionStatus ts = (TransactionStatus) userData;
            ts.transaction = transaction;
        } catch(Throwable t) {
            handleException(t);
        }
    }
    */
    
    @Override
    public void afterTransaction(TransactionType request, TransactionResponseType result, boolean committed) {
        try {
            TransactionStatus ts = statuses.remove(request);
            if(ts != null) {
                ts.destroy();
            }
        } catch(Throwable t) {
            handleException(t);
        }
    }

    @Override
    public void beforeCommit(TransactionType request) throws WFSException {
        try {
            TransactionStatus ts = statuses.get(request);
            if(ts != null) {
                tryBeforeCommit(request, ts);
            }
        } catch(Throwable t) {
            handleException(t);
        }
    }
    
    public void tryBeforeCommit(TransactionType request, final TransactionStatus ts) throws WFSException {

        // Rerun each query to be checked
        for(Entry<Name, Set<Identifier>> ent : ts.getAffected().entrySet()) {
            Collection<Feature> features = new ArrayList(ent.getValue().size());
            for(Identifier id : ent.getValue()) {
                features.add(ts.getFidMap().get(id));
            }
            try {
                tm.triggerEvent(new CollectionsFeatureIterator<Feature>(features.iterator()), new QName(ent.getKey().getNamespaceURI(), ent.getKey().getLocalPart()), new TriggerCallback() {
                    @Override
                    public void triggerEvent(Feature f) {
                        if(ts.checkFeature(f)) {
                            WFSNotify.this.triggerEvent(f);
                        }
                    }
                }, ts.getTransaction());
            } catch(IOException e) {
                LOG.debug("Error checking modified features, notifications will be inaccurate:", e);
            }
        }

        // Check the remaining features in the ts map:
        for(Entry<Name, Set<Identifier>> ent : ts.getPotentiallyModified().entrySet()) {
            try {
                if(ent.getValue().isEmpty()) {
                    continue; // Nothing to do...
                }
                FeatureTypeInfo info = catalog.getFeatureTypeByName(ent.getKey());
                if(info == null) {
                    continue;
                }


                // Create a FID query for each type that's not empty, do the same as the above
                Filter filter = FF.id(ent.getValue());

                FeatureSource<? extends FeatureType, ? extends Feature> source = info.getFeatureSource(null, null);
                FeatureCollection<? extends FeatureType, ? extends Feature> coll = source.getFeatures(filter);
                FeatureIterator<? extends Feature> i = coll.features();
                
                try {
                    while(i.hasNext()) {
                        Feature f = i.next();
                        if(ts.checkFeature(f)) {
                            triggerEvent(f);
                        }
                    }
                } finally {
                    i.close();
                }

            } catch(IOException e) {}
        }

        // Delete anything left in the ts map
        for(Entry<Name, Set<Identifier>> ent : ts.getPotentiallyModified().entrySet()) {
            Name typeName = ent.getKey();
            for(Identifier id : ent.getValue()) {
                triggerDeleteEvent(typeName, id);
            }
        }

    }

    private void triggerEvent(Feature f) {
        if(serializer != null) {
            publishEvent(serializer.serializeInsertOrUpdate(f));
        }
    }

    private void triggerDeleteEvent(Name typeName, Identifier id) {
        if(serializer != null) {
            publishEvent(serializer.serializeDelete(typeName, id));
        }
    }
    
    private void publishEvent(String msg) {
        for(NotificationPublisher publisher : publishers) {
            if(publisher.isReady()) {
                publisher.publish(msg);
            }
        }
    }

    public int getPriority() {
        return 0;
    }
    
    public static Name getTypeName(Feature f) {
        if(f.getDescriptor() != null)
            return f.getDescriptor().getName();
        if(f.getType() != null)
            return f.getType().getName();
        return null;
    }
}