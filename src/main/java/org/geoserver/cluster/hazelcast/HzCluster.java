package org.geoserver.cluster.hazelcast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.ClusterConfig;
import org.geoserver.cluster.ClusterConfigWatcher;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
public class HzCluster implements DisposableBean, InitializingBean {
    
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");
    
    static final String CONFIG_DIRECTORY = "cluster";
    static final String CONFIG_FILENAME = "cluster.properties";
    static final String HAZELCAST_FILENAME = "hazelcast.xml";
    
    HazelcastInstance hz;
    GeoServerResourceLoader rl;
    ClusterConfigWatcher watcher;

    private Catalog rawCatalog;

    /**
     * Get a file from the cluster config directory. Create it by copying a template from the
     * classpath if it doesn't exist.
     * @param fileName Name of the file
     * @param scope Scope for looking up a default if the file doesn't exist.
     * @return
     * @throws IOException
     */
    public File getConfigFile(String fileName, Class<?> scope) throws IOException {
        File dir = rl.findOrCreateDirectory(CONFIG_DIRECTORY);
        File file = rl.find(dir, fileName);
        if (file == null) {
            
            file = rl.createFile(dir, fileName);
            
            rl.copyFromClassPath(fileName, file, scope);
        }
        return file;
    }
    
    /**
     * Is clustering enabled
     * @return
     */
    public boolean isEnabled() {
        return hz!=null;
    }
    
    /**
     * Is session sharing enabled.  Only true if clustering in general is enabled.
     * @return
     */
    public boolean isSessionSharing() {
        return isEnabled() &&
                Boolean.parseBoolean(getClusterConfig().getProperty("session_sharing", "true"));
    }
    
    /**
     * Is session sharing sticky.  See Hazelcast documentation for details.
     * @return
     */
    public boolean isStickySession() {
        return Boolean.parseBoolean(getClusterConfig().getProperty("session_sticky", "false"));
    }
    
    /**
     * Get the HazelcastInstance being used for clustering
     * @return
     * @throws IllegalStateException if clustering is not enabled
     */
    public HazelcastInstance getHz() {
        if(!isEnabled()) throw new IllegalStateException("Hazelcast Clustering has not been enabled.");
        return hz;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        watcher = loadConfig();
        if(watcher.get().isEnabled())
            hz = Hazelcast.newHazelcastInstance(loadHazelcastConfig(rl));
    }

    @Override
    public void destroy() throws Exception {
        if(hz!=null) {
            hz.getLifecycleService().shutdown();
            hz=null;
        }
    }
    
    private Config loadHazelcastConfig(GeoServerResourceLoader rl) throws IOException{
        File hzf = getConfigFile(HAZELCAST_FILENAME, HzCluster.class);
        InputStream hzIn = new FileInputStream(hzf);
        try {
            return new XmlConfigBuilder(new FileInputStream(hzf)).build();
        } finally {
            hzIn.close();
        }
    }
    
    /**
     * For Spring initialisation, don't call otherwise.
     * @param dd
     * @throws IOException
     */
    public void setDataDirectory(GeoServerDataDirectory dd) throws IOException {
        rl=dd.getResourceLoader();
    }
    
    /**
     * For Spring initialisation, don't call otherwise.
     */
    public void setRawCatalog(Catalog rawCatalog) throws IOException {
        this.rawCatalog = rawCatalog;
    }

    public Catalog getRawCatalog() {
        return rawCatalog;
    }

    ClusterConfigWatcher loadConfig() throws IOException {
        File f = getConfigFile(HzCluster.CONFIG_FILENAME, HzCluster.class);
        
        return new ClusterConfigWatcher(f);
    }
    
    ClusterConfigWatcher getConfigWatcher() {
        return watcher;
    }
    
    ClusterConfig getClusterConfig() {
        return watcher.get();
    }
}
