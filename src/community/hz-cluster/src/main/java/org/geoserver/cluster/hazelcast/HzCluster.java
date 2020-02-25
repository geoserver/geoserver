/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 Boundless
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast;

import com.google.common.base.Optional;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.cluster.ClusterConfig;
import org.geoserver.cluster.ClusterConfigWatcher;
import org.geoserver.config.GeoServerPluginConfigurator;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/** @author Kevin Smith, OpenGeo */
public class HzCluster implements GeoServerPluginConfigurator, DisposableBean, InitializingBean {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.cluster.hazelcast");

    static final String CONFIG_DIRECTORY = "cluster";
    static final String CONFIG_FILENAME = "cluster.properties";
    static final String HAZELCAST_FILENAME = "hazelcast.xml";

    HazelcastInstance hz;
    ResourceStore rl;
    ClusterConfigWatcher watcher;

    private Catalog rawCatalog;

    private HzResourceNotificationDispatcher rnd;

    private static HzCluster CLUSTER;

    /**
     * Get a file from the cluster config directory. Create it by copying a template from the
     * classpath if it doesn't exist.
     *
     * @param fileName Name of the file
     * @param scope Scope for looking up a default if the file doesn't exist.
     */
    public Resource getConfigFile(String fileName, Class<?> scope) throws IOException {
        return getConfigFile(fileName, scope, this.rl);
    }

    protected Resource getConfigFile(String fileName, Class<?> scope, ResourceStore rl)
            throws IOException {
        Resource dir = rl.get(CONFIG_DIRECTORY);
        Resource file = dir.get(fileName);
        if (!Resources.exists(file)) {
            IOUtils.copy(scope.getResourceAsStream(fileName), file.out());
        }
        return file;
    }

    static Optional<HzCluster> getInstanceIfAvailable() {
        return Optional.fromNullable(CLUSTER);
    }

    /** Is clustering enabled */
    public boolean isEnabled() {
        return hz != null;
    }

    public boolean isRunning() {
        return isEnabled() && hz.getLifecycleService().isRunning();
    }

    /** Is session sharing enabled. Only true if clustering in general is enabled. */
    public boolean isSessionSharing() {
        return isEnabled()
                && Boolean.parseBoolean(getClusterConfig().getProperty("session_sharing", "true"));
    }

    /** Is session sharing sticky. See Hazelcast documentation for details. */
    public boolean isStickySession() {
        return Boolean.parseBoolean(getClusterConfig().getProperty("session_sticky", "false"));
    }

    /**
     * @return milliseconds to wait for node ack notifications upon sending a config change event.
     *     Defaults to 2000ms.
     */
    public int getAckTimeoutMillis() {
        return getClusterConfig().getAckTimeoutMillis();
    }

    /**
     * Get the HazelcastInstance being used for clustering
     *
     * @throws IllegalStateException if clustering is not enabled
     */
    public HazelcastInstance getHz() {
        if (!isEnabled())
            throw new IllegalStateException("Hazelcast Clustering has not been enabled.");
        return hz;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Assert.notNull(this.rl);

        watcher = loadConfig(this.rl);
        if (watcher.get().isEnabled()) {
            hz = Hazelcast.newHazelcastInstance(loadHazelcastConfig(this.rl));
            CLUSTER = this;
        }

        rnd.setCluster(this);
    }

    @Override
    public void destroy() throws Exception {
        if (hz != null) {
            LOGGER.info("HzCluster.destroy() invoked, shutting down Hazelcast instance...");
            CLUSTER = null;
            hz.getLifecycleService().shutdown();
            hz = null;
            LOGGER.info("HzCluster.destroy(): Hazelcast instance shut down complete");
        }
    }

    private Config loadHazelcastConfig(ResourceStore rl) throws IOException {
        Resource hzf = getConfigFile(HAZELCAST_FILENAME, HzCluster.class, rl);
        try (InputStream hzIn = hzf.in()) {
            return new XmlConfigBuilder(hzIn).build();
        }
    }

    /** For Spring initialisation, don't call otherwise. */
    public void setResourceStore(ResourceStore dd) throws IOException {
        rl = dd;
    }

    public void setResourceNotificationDispatcher(HzResourceNotificationDispatcher rnd) {
        this.rnd = rnd;
    }

    /** For Spring initialisation, don't call otherwise. */
    public void setRawCatalog(Catalog rawCatalog) throws IOException {
        this.rawCatalog = rawCatalog;
    }

    public Catalog getRawCatalog() {
        return rawCatalog;
    }

    ClusterConfigWatcher loadConfig(ResourceStore rl) throws IOException {
        Resource f = getConfigFile(HzCluster.CONFIG_FILENAME, HzCluster.class, rl);

        return new ClusterConfigWatcher(f);
    }

    ClusterConfigWatcher getConfigWatcher() {
        return watcher;
    }

    ClusterConfig getClusterConfig() {
        return watcher.get();
    }

    @Override
    public List<Resource> getFileLocations() throws IOException {
        List<Resource> configurationFiles = new ArrayList<>();
        configurationFiles.add(getConfigFile(HzCluster.CONFIG_FILENAME, HzCluster.class));
        configurationFiles.add(getConfigFile(HAZELCAST_FILENAME, HzCluster.class));
        return configurationFiles;
    }

    @Override
    public void saveConfiguration(GeoServerResourceLoader resourceLoader) throws IOException {
        for (Resource configFile : getFileLocations()) {
            Resource targetDir =
                    Files.asResource(
                            resourceLoader.findOrCreateDirectory(
                                    Paths.convert(rl.get("/").dir(), configFile.parent().dir())));

            Resources.copy(configFile.file(), targetDir);
        }
    }

    @Override
    public void loadConfiguration(GeoServerResourceLoader resourceLoader) throws IOException {
        synchronized (hz) {
            try {
                destroy();
            } catch (Exception e) {
                throw new IOException(e);
            }

            watcher = loadConfig(resourceLoader);
            if (watcher.get().isEnabled()) {
                hz = Hazelcast.newHazelcastInstance(loadHazelcastConfig(resourceLoader));
                CLUSTER = this;
            }
        }
    }
}
