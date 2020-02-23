/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.qos.util.XstreamQosFactory;
import org.geoserver.qos.xml.QosMainConfiguration;

public abstract class BaseConfigurationLoader<T extends ServiceInfo> {

    protected GeoServerDataDirectory dataDirectory;

    public BaseConfigurationLoader() {}

    public BaseConfigurationLoader(GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * Loads the configuration file in workspace or global
     *
     * @return configuration
     */
    public QosMainConfiguration getConfiguration(ServiceInfo info) {
        Resource resource = getResource(info);
        // if doesn't exists, return a deactivated default config object
        if (resource.getType().equals(Resource.Type.UNDEFINED)) {
            return buildDeactivatedConfig();
        }
        QosMainConfiguration conf = null;
        XStream xstream = XstreamQosFactory.getInstance();
        try (InputStream is = resource.in()) {
            conf = (QosMainConfiguration) xstream.fromXML(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return conf;
    }

    protected Resource getResource(ServiceInfo info) {
        if (info.getWorkspace() == null) {
            return dataDirectory.getRoot(getFileName());
        }
        String wsName = info.getWorkspace().getName();
        return dataDirectory.getWorkspaces(wsName + "/" + getFileName());
    }

    protected QosMainConfiguration buildDeactivatedConfig() {
        QosMainConfiguration config = new QosMainConfiguration();
        config.setActivated(false);
        return config;
    }

    public GeoServerDataDirectory getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(GeoServerDataDirectory dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    protected abstract String getFileName();

    protected abstract void validate(QosMainConfiguration config);

    /** Saves QoS configuration to file in workspace folder */
    public void setConfiguration(T info, QosMainConfiguration config) {
        Resource resource = getResource(info);
        setConfiguration(resource, config);
    }

    public void setConfiguration(String workspace, QosMainConfiguration config) {
        Resource resource = dataDirectory.getWorkspaces(workspace + "/" + getFileName());
        setConfiguration(resource, config);
    }

    public void setConfiguration(Resource resource, QosMainConfiguration config) {
        validate(config);
        XStream xstream = XstreamQosFactory.getInstance();
        try (OutputStream out = resource.out()) {
            xstream.toXML(config, out);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
