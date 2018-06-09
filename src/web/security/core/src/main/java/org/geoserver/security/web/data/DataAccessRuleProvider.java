/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.util.Arrays;
import java.util.List;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.wicket.GeoServerDataProvider;

/** Page listing the rules contained in the layers.properties file */
@SuppressWarnings("serial")
public class DataAccessRuleProvider extends GeoServerDataProvider<DataAccessRule> {

    public static final Property<DataAccessRule> RULEKEY =
            new BeanProperty<DataAccessRule>("key", "key");

    public static final Property<DataAccessRule> ROLES =
            new BeanProperty<DataAccessRule>("roles", "value");

    @Override
    protected List<DataAccessRule> getItems() {
        return DataAccessRuleDAO.get().getRules();
    }

    @Override
    protected List<Property<DataAccessRule>> getProperties() {
        return Arrays.asList(RULEKEY, ROLES);
    }
}
