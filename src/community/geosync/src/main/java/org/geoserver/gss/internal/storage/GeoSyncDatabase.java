package org.geoserver.gss.internal.storage;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.sleepycat.je.Environment;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;

public class GeoSyncDatabase {

    private final Environment env;

    private EntityStore store;

    // private PrimaryIndex<String, ServerSubscription> serverSubscriptions;

    private PrimaryIndex<String, Subscription> clientSubscriptions;

    public GeoSyncDatabase(final Environment env) {
        this.env = env;
    }

    public void create() {
        StoreConfig config = new StoreConfig();
        config.setAllowCreate(true);
        config.setTransactional(true);
        store = new EntityStore(env, "GeoSync Database", config);
        // serverSubscriptions = store.getPrimaryIndex(String.class, ServerSubscription.class);
        clientSubscriptions = store.getPrimaryIndex(String.class, Subscription.class);
    }

    public void close() {
        try {
            store.close();
        } finally {
            env.close();
        }
    }

    /**
     * @return the list of subscriptions to a target server
     */
    // public List<ServerSubscription> getServerSubscriptions() {
    // List<ServerSubscription> subscriptions = new LinkedList<ServerSubscription>();
    // EntityCursor<ServerSubscription> entities = serverSubscriptions.entities();
    // try {
    // subscriptions.add(entities.next());
    // } finally {
    // entities.close();
    // }
    // return subscriptions;
    // }
    //
    // public String put(ServerSubscription subscription) {
    // Preconditions.checkNotNull(subscription);
    // ServerSubscription put = serverSubscriptions.put(subscription);
    // return put.getSid();
    // }

    /**
     * @return the list of subscriptions clients have been subscribed to on this server
     */
    public List<Subscription> getClientSubscriptions() {
        List<Subscription> subscriptions = new LinkedList<Subscription>();
        EntityCursor<Subscription> entities = clientSubscriptions.entities();
        try {
            subscriptions.add(entities.next());
        } finally {
            entities.close();
        }
        return subscriptions;
    }

    public String put(Subscription subscription) {
        Preconditions.checkNotNull(subscription);
        Subscription put = clientSubscriptions.put(subscription);
        return put.getSid();
    }
}
