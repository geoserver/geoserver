package org.geoserver.monitor;

import java.util.List;

import org.geoserver.monitor.MonitorConfig.Mode;

/**
 * The GeoServer request monitor and primary entry point into the monitor api.
 * <p>
 * For each request submitted to a GeoServer instance the monitor maintains state about
 * the request and makes operations available that control the life cycle of the request.
 * The life cycle of a monitored request advances through the following states:
 * <ul>
 *  <li>the request is STARTED
 *  <li>the request is UPDATED any number of times.
 * </ul>
 * </p>
 * 
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class Monitor {

    /**  
     * thread local request object.
     */
    static ThreadLocal<RequestData> REQUEST = new ThreadLocal<RequestData>();
    
    /**
     * default page size when executing queries
     */
    static long PAGE_SIZE = 1000;
    
    MonitorConfig config;
    MonitorDAO dao;
    
    public Monitor(MonitorConfig config) {
        this.config = config;
        this.dao = config.createDAO();
    }
    
    public Monitor(MonitorDAO dao) {
        this.config = new MonitorConfig();
        this.dao = dao;
    }
    
    public RequestData start() {
        RequestData req = new RequestData();
        req = dao.init(req);
        REQUEST.set(req);
        
        if (config.getMode() != Mode.HISTORY) {
            dao.add(req);
        }
        
        return req;
    }

    public RequestData current() {
        return REQUEST.get();
    }

    public void update() {
        if (config.getMode() != Mode.HISTORY) {
            dao.update(REQUEST.get());
        }
    }

    public void complete() {
        dao.save(REQUEST.get());
        REQUEST.remove();
    }

    public void dispose() {
        dao.dispose();
        dao = null;
    }
    
    public MonitorDAO getDAO() {
        return dao;
    }
    
    public void query(MonitorQuery q, RequestDataVisitor visitor) {
        dao.getRequests(q, visitor);
    }

}
