/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.geoserver.platform.GeoServerExtensions;
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
        HISTORY, LIVE,

        @Deprecated // use live
        HYBRID;
    }

    public static enum BboxMode {
        NONE, NO_WFS, FULL;
    }
    
    protected Properties props;
    PropertyFileWatcher fw;
    ApplicationContext context;
    boolean enabled = true;
    Exception error;
    
    public MonitorConfig() {
        props = new Properties();
        props.put("storage", "memory");
        props.put("mode", "history");
        props.put("maxBodySize", "1024");
        props.put("bboxMode", "no_wfs");
        props.put("bboxCrs", "EPSG:4326");

        //for backwards compatibility include the hibernate config options
        props.put("hibernate.sync", "async");
    }
    
    public MonitorConfig(GeoServerResourceLoader loader) throws IOException {
        File f = loader.find("monitoring", "monitor.properties");
        if (f == null) {
            f = loader.createFile("monitoring", "monitor.properties");
            loader.copyFromClassPath("monitor.properties", f, MonitorConfig.class);
        }
        
        fw = new PropertyFileWatcher(f);
    }

    public String getStorage() {
        return props().getProperty("storage");
    }

    public Properties getProperties() {
        return props();
    }

    public Mode getMode() {
        Mode m = Mode.valueOf(props().getProperty("mode", "history").toUpperCase());
        if (m == Mode.HYBRID) {
            m = Mode.LIVE;
        }
        return m;
    }
    
    public long getMaxBodySize() {
        return Long.parseLong(props().getProperty("maxBodySize", String.valueOf(1024)));
    }
    
    public CoordinateReferenceSystem getBboxCrs() {
        Properties props = props();
        String srs = props.getProperty("bboxCrs");
        if (srs == null) {
            //old property name
            srs = props.getProperty("bboxLogCrs", "EPSG:4326");
        }
        try {
            return CRS.decode(srs);
        } catch (NoSuchAuthorityCodeException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (FactoryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
        return null;
    }
    
    public BboxMode getBboxMode() {
        Properties props = props();
        String mode = props.getProperty("bboxMode");
        if (mode == null) {
            //old property name
            mode = props.getProperty("bboxLogLevel", "no_wfs");
        }
        if (mode == null) {
            return null;
        }
        return BboxMode.valueOf(mode.toUpperCase());
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
        MonitorDAO dao = null;

        String storage = getStorage();
        if (storage == null) {
            //storage key not found, for backward compatibility look up mode
            Mode mode = getMode();
            if (mode == Mode.HISTORY || mode == Mode.HYBRID) {
                storage = "hibernate";
            }
        }

        if (storage == null) {
            storage = MemoryMonitorDAO.NAME;
        }

        //match storage to plugin in context
        for (MonitorDAO d : GeoServerExtensions.extensions(MonitorDAO.class)) {
            if (storage.equalsIgnoreCase(d.getName())) {
                dao = d;
                break;
            }
        }
        if (dao == null) {
            LOGGER.warning("monitoring storage "+storage+" not found, falling back to '"
                + MemoryMonitorDAO.NAME +"'");
            dao = new MemoryMonitorDAO();
        }

        dao.init(this);
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

                        //backward compatibility hack for sync -> hibernate.sync
                        if (props.getProperty("sync") != null) {
                            props.setProperty("hibernate.sync", props.getProperty("sync"));
                        }
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
