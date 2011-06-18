/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.geoserver.monitor.hib.HibernateMonitorDAO2;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.PropertyFileWatcher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Configuration object for monitor subsystem.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class MonitorConfig implements ApplicationContextAware {

    public static enum Mode {
        LIVE, HISTORY, HYBRID;
    }
    
    public static enum Sync {
        SYNC, ASYNC, ASYNC_UPDATE;
    }
    
    Properties props;
    PropertyFileWatcher fw;
    ApplicationContext context;
    boolean enabled = true;
    Exception error;
    
    public MonitorConfig() {
        props = new Properties();
        props.put("mode", "history");
        props.put("sync", "async");
    }
    
    public MonitorConfig(GeoServerResourceLoader loader) throws IOException {
        File f = loader.find("monitoring", "monitor.properties");
        if (f == null) {
            f = loader.createFile("monitoring", "monitor.properties");
            loader.copyFromClassPath("monitor.properties", f, MonitorConfig.class);
        }
        
        fw = new PropertyFileWatcher(f);
    }
    
    public Mode getMode() {
        return Mode.valueOf(props().getProperty("mode", "history").toUpperCase());
    }
    
    public Sync getSync() {
        return Sync.valueOf(props().getProperty("sync", "async").toUpperCase());
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Exception getError() {
        return error;
    }
    
    public void setError(Exception error) {
        this.error = error;
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
    
    public MonitorDAO createDAO() {
        Mode m = getMode();
        
        if (m == Mode.LIVE) {
            return new MemoryMonitorDAO();
        }
        
        HibernateMonitorDAO2 dao = (HibernateMonitorDAO2) context.getBean("hibMonitorDAO");
        dao.setMode(m);
        dao.setSync(getSync());
        
        return dao;
    }
    
    Properties props() {
        if (fw != null && fw.isModified()) {
            synchronized (this) {
                if (fw.isModified()) {
                    try {
                        props = fw.read();
                    } 
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return props;
    }
}
