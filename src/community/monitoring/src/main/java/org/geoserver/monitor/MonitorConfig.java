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
import org.geotools.factory.Hints;
import org.geotools.referencing.CRS;
import org.geotools.util.ConverterFactory;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Configuration object for monitor subsystem.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class MonitorConfig implements ApplicationContextAware {

    private static final Logger LOGGER = Logging.getLogger(MonitorConfig.class);
    
    public static enum Mode {
        LIVE, HISTORY, HYBRID;
    }
    
    public static enum Sync {
        SYNC, ASYNC, ASYNC_UPDATE;
    }
    
    public static enum BBoxLogLevel {
        NONE, NO_WFS, FULL;
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
        props.put("maxBodySize", "1024");
        props.put("bboxLogCrs", "EPSG:4326");
        props.put("bboxLogLevel", "no_wfs");
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
    
    public long getMaxBodySize() {
        return Long.parseLong(props.getProperty("maxBodySize", String.valueOf(1024)));
    }
    
    public CoordinateReferenceSystem getBboxLogCrs() {
        try {
            return CRS.decode(props.getProperty("bboxLogCrs", "EPSG:4326"));
        } catch (NoSuchAuthorityCodeException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (FactoryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }
    
    public BBoxLogLevel getBboxLogLevel() {
        return BBoxLogLevel.valueOf(props().getProperty("bboxLogLevel", "no_wfs").toUpperCase());
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
    
    /**
     * Allows to retrieve a generic property from the configuration. Extensions and plugins are
     * supposed to use the plugin.property naming convention, passing both a prefix and a name
     * 
     * @param <T>
     * @param prefix
     * @param name
     * @param target
     * @return
     */
    public <T> T getProperty(String prefix, String name, Class<T> target) {
        String key = prefix == null ? name : prefix + "." + name;
        Object value = props().get(key);
        if (value != null) {
            T converted = Converters.convert(value, target, new Hints(
                    ConverterFactory.SAFE_CONVERSION, true));
            if (converted == null) {
                throw new IllegalArgumentException("Object " + value
                        + " could not be converted to the target class " + target);
            }
            return converted;
        } else {
            return null;
        }
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
