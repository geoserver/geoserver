/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.Resource;

/**
 * A spring placeholder configurer that loads properties from the data directory.
 *
 * <p>This class is used by declaring an instance in a spring context:
 *
 * <pre>
 *  &lt;bean id="myPropertyConfigurer" class="org.geoserver.config.GeoServerPropertyConfigurer">
 *    &lt;constructor-arg ref="dataDirectory"/>
 *    &lt;property name="location" value="file:myDirectory/myFile.properties"/>
 *    &lt;property name="properties">
 *      &lt;props>
 *        &lt;prop key="prop1">value1&lt;/prop>
 *        &lt;prop key="prop2">value2&lt;/prop>
 *      &lt;/props>
 *    &lt;/property>
 *  &lt;/bean>
 * </pre>
 *
 * The location <tt>myDirectory/myFile.properties</tt> will be resolved relative to the data
 * directory.
 *
 * <p>In the same spring context the configurer is used as follows:
 *
 * <pre>
 *  &lt;bean id="myBean" class="com.xyz.MyClass">
 *    &lt;property name="someProperty" value="${prop1}"/>
 *    &lt;property name="someOtherProperty" value="${prop2}"/>
 *  &lt;/bean>
 * </pre>
 *
 * If the file <tt>myDirectory/myFile.properties</tt> exists then the property values will be loaded
 * from it, otherwise the defaults declared on the property configurer will be used. By default when
 * the resource is not found it will be copied out into the data directory. However {@link
 * #setCopyOutTemplate(boolean)} can be used to control this.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerPropertyConfigurer extends PropertyPlaceholderConfigurer {

    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    org.geoserver.platform.resource.Resource configFile;
    protected GeoServerDataDirectory data;
    boolean copyOutTemplate = true;
    String comments;

    public GeoServerPropertyConfigurer(GeoServerDataDirectory data) {
        this.data = data;
    }

    public void setCopyOutTemplate(boolean copyOutTemplate) {
        this.copyOutTemplate = copyOutTemplate;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    /** @return the configLocation */
    public org.geoserver.platform.resource.Resource getConfigFile() {
        return configFile;
    }

    @Override
    public void setLocation(Resource location) {
        try {
            location = SpringResourceAdaptor.relative(location, data.getResourceStore());
            if (location instanceof SpringResourceAdaptor) {
                configFile = ((SpringResourceAdaptor) location).getResource();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading resource " + location, e);
        }

        super.setLocation(location);
    }

    @Override
    public void setLocations(Resource[] locations) {
        throw new UnsupportedOperationException("Only a single location is supported");
    }

    @Override
    protected void loadProperties(Properties props) throws IOException {
        try {
            super.loadProperties(props);
        } catch (FileNotFoundException e) {
            // location was not found, create
            if (configFile != null && copyOutTemplate) {
                try (OutputStream fout = configFile.out()) {
                    props.store(fout, comments);
                    fout.flush();
                }
            }
        }
    }

    /**
     * Force reloading the properties which may have been updated in the meanwhile; after a restore
     * as an instance.
     */
    public void reload() throws IOException {
        if (localProperties != null) {
            for (Properties props : localProperties) {
                loadProperties(props);
            }
        }
    }

    @Override
    protected String convertPropertyValue(String property) {
        return property.replace("${GEOSERVER_DATA_DIR}", data.root().getPath());
    }
}
