/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.geoserver.cluster.configuration.EmbeddedBrokerConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.config.SpringResourceAdaptor;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;

public class JMSPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer
        implements InitializingBean {

    private final JMSConfiguration config;

    private final Resource defaults;

    public JMSPropertyPlaceholderConfigurer(Resource defaultFile, JMSConfiguration config)
            throws IOException {
        if (!defaultFile.exists()) {
            throw new IOException("Unable to locate the default properties file at:" + defaultFile);
        }
        this.defaults = defaultFile;
        this.config = config;
    }

    public Properties[] getProperties() {
        return localProperties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        org.geoserver.platform.resource.Resource properties =
                Resources.fromPath(
                        config.getConfiguration(
                                        EmbeddedBrokerConfiguration.EMBEDDED_BROKER_PROPERTIES_KEY)
                                .toString(),
                        JMSConfiguration.getConfigPathDir());
        if (properties.getType() != Type.RESOURCE) {
            // copy the defaults
            try (InputStream inputStream = defaults.getInputStream()) {
                IOUtils.copy(inputStream, properties.out());
            }
        }
        super.setLocation(new SpringResourceAdaptor(properties));

        // make sure the activemq.base is set to a valuable default
        final Properties props = new Properties();
        props.setProperty("activemq.base", (String) config.getConfiguration("CLUSTER_CONFIG_DIR"));
        props.setProperty("instanceName", (String) config.getConfiguration("instanceName"));
        setProperties(props);
    }
}
