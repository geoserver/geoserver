/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.util.Arrays;
import java.util.List;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.web.wicket.GeoServerDataProvider;

/** Page listing the rules contained in the service.properties file */
@SuppressWarnings("serial")
public class ServiceAccessRuleProvider extends GeoServerDataProvider<ServiceAccessRule> {

    public static final Property<ServiceAccessRule> RULEKEY =
            new BeanProperty<ServiceAccessRule>("key", "key");
    public static final Property<ServiceAccessRule> ROLES =
            new BeanProperty<ServiceAccessRule>("roles", "value");

    @Override
    protected List<ServiceAccessRule> getItems() {
        return ServiceAccessRuleDAO.get().getRules();
    }

    @Override
    protected List<Property<ServiceAccessRule>> getProperties() {
        return Arrays.asList(RULEKEY, ROLES);
    }
}
