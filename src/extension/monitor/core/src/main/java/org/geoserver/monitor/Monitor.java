/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.monitor.MonitorConfig.Mode;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * The GeoServer request monitor and primary entry point into the monitor api.
 *
 * <p>For each request submitted to a GeoServer instance the monitor maintains state about the
 * request and makes operations available that control the life cycle of the request. The life cycle
 * of a monitored request advances through the following states:
 *
 * <ul>
 *   <li>the request is STARTED
 *   <li>the request is UPDATED any number of times.
 * </ul>
 *
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
public class Monitor implements ApplicationListener<ApplicationEvent> {

    /** thread local request object. */
    static ThreadLocal<RequestData> REQUEST = new ThreadLocal<RequestData>();

    /** default page size when executing queries */
    static long PAGE_SIZE = 1000;

    MonitorConfig config;
    MonitorDAO dao;

    // info about monitored server
    // JD: look up lazily, using constructor injection causes failure to load main Geoserver
    // configuration, TODO: take another look at the initialization
    GeoServer server;

    /** The set of listeners for the monitor */
    List<RequestDataListener> listeners = new ArrayList<RequestDataListener>();

    public Monitor(MonitorConfig config) {
        this.config = config;
        this.dao = config.createDAO();
    }

    public Monitor(MonitorDAO dao) {
        this.config = new MonitorConfig();
        this.dao = dao;
    }

    public MonitorConfig getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public RequestData start() {
        RequestData req = new RequestData();
        req = dao.init(req);
        REQUEST.set(req);

        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestUpdated(req);
        }
        // have the DAO persist/propagate the change
        if (config.getMode() != Mode.HISTORY) {
            dao.add(req);
        }

        return req;
    }

    public RequestData current() {
        return REQUEST.get();
    }

    public void update() {
        RequestData data = REQUEST.get();
        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestUpdated(data);
        }
        // have the DAO persist/propagate the change
        if (config.getMode() != Mode.HISTORY) {
            dao.update(data);
        }
    }

    public void complete() {
        RequestData data = REQUEST.get();
        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestCompleted(data);
        }
        // have the DAO persist/propagate the change
        dao.save(data);
        REQUEST.remove();
    }

    public void postProcessed(RequestData rd) {
        // notify listeners
        for (RequestDataListener listener : listeners) {
            listener.requestPostProcessed(rd);
        }
        // have the DAO persist/propagate the change
        dao.update(rd);
    }

    public void dispose() {
        dao.dispose();
        dao = null;
    }

    public MonitorDAO getDAO() {
        return dao;
    }

    public GeoServer getServer() {
        return server;
    }

    public void setServer(GeoServer server) {
        this.server = server;
    }

    public void query(Query q, RequestDataVisitor visitor) {
        dao.getRequests(q, visitor);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            listeners = GeoServerExtensions.extensions(RequestDataListener.class);
        }
    }
}
