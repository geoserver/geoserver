/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * Base class for {@link SecurityNamedServiceConfig} providers.
 *
 * <p>This class is responsible for loading all configuration objects for a certain class of named
 * security service.
 *
 * @author Christian Mueller
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class SecurityNamedServiceProvider<T extends SecurityNamedServiceConfig>
        extends GeoServerDataProvider<T> {

    private static final long serialVersionUID = 1L;

    /** name of the config */
    public static final Property<SecurityNamedServiceConfig> NAME =
            new BeanProperty<SecurityNamedServiceConfig>("name", "name");

    /** type/implementation of the config */
    public static final Property<SecurityNamedServiceConfig> TYPE =
            new AbstractProperty<SecurityNamedServiceConfig>("type") {

                @Override
                public Object getPropertyValue(SecurityNamedServiceConfig item) {
                    // do a resource lookup
                    return new ResourceModel(item.getClassName() + ".title", item.getClassName())
                            .getObject();
                }
            };

    @Override
    protected List<Property<T>> getProperties() {
        List result = new ArrayList();
        result.add(NAME);
        result.add(TYPE);
        return result;
    }

    protected GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    /** Bean property in which the value is looked up as resource key in the i18n file. */
    public static class ResourceBeanProperty<T extends SecurityNamedServiceConfig>
            extends BeanProperty<T> {

        public ResourceBeanProperty(String key, String propertyPath) {
            super(key, propertyPath);
        }
    }
}
