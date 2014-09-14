/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFileWatcher<T> {
    private static final Log LOG = LogFactory.getLog(AbstractFileWatcher.class);
    final long CHECK_PERIOD;
    long lastModTime = 0;
    long nextCheckTime = 0;
    T data;
    boolean oldDataAcceptable = true; // Whether failures reloading data are fatal or a warning
    
    URL u;

    public AbstractFileWatcher(long checkPeriod, T initData, URL u) {
        CHECK_PERIOD = checkPeriod;
        data = initData;
        this.u = u;
    }
    
    public AbstractFileWatcher(long checkPeriod, URL u) throws IOException {
        this(checkPeriod, null, u);
        data = doLoad(u.openConnection());
    }
    
    public void setOldDataAcceptable(boolean oldDataAcceptable) {
        this.oldDataAcceptable = oldDataAcceptable;
    }
    
    public boolean isOldDataAcceptable() {
        return oldDataAcceptable;
    }

    public T load() throws IOException {
        if(shouldCheck()) {
            URLConnection conn = u.openConnection();
            nextCheckTime = System.currentTimeMillis() + CHECK_PERIOD;
            if(shouldUpdate(conn)) {
                T newData;
                try {
                    newData = doLoad(conn);
                    lastModTime = conn.getLastModified(); // Don't update until successful
                } catch(IOException e) {
                    if(data == null || !oldDataAcceptable) {
                        // Check right away if we're throwing exceptions
                        nextCheckTime = 0;
                        throw e;
                    }
                    newData = data; // Use old data, but warn
                    LOG.warn("Loading new data failed, using old data", e);
                }
                return data = newData;
            }
        }

        return data;
    }

    public URL getURL() {
        return u;
    }
    
    protected T getData() {
        return data;
    }
    
    public long getLastModTime() {
        return lastModTime;
    }
    
    protected boolean shouldCheck() {
        return System.currentTimeMillis() > nextCheckTime;
    }
    
    protected boolean shouldUpdate(URLConnection conn) {
        return conn.getLastModified() != lastModTime || conn.getLastModified() == 0;
    }

    protected abstract T doLoad(URLConnection conn) throws IOException;
}