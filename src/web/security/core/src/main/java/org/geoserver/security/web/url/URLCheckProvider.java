/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.url;

import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.geoserver.security.urlchecks.AbstractURLCheck;
import org.geoserver.web.wicket.GeoServerDataProvider;

class URLCheckProvider extends GeoServerDataProvider<AbstractURLCheck> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static Property<AbstractURLCheck> NAME = new BeanProperty<>("name", "name");
    public static Property<AbstractURLCheck> DESCRIPTION =
            new BeanProperty<>("description", "description");
    public static Property<AbstractURLCheck> CONFIGURATION =
            new BeanProperty<>("configuration", "configuration");
    public static Property<AbstractURLCheck> ENABLED = new BeanProperty<>("enabled", "enabled");

    List<AbstractURLCheck> checks;

    public URLCheckProvider(List<AbstractURLCheck> checks) {
        setSort(new SortParam<>(NAME.getName(), true));
        this.checks = checks;
    }

    @Override
    protected List<Property<AbstractURLCheck>> getProperties() {
        return List.of(NAME, DESCRIPTION, CONFIGURATION, ENABLED);
    }

    @Override
    protected List<AbstractURLCheck> getItems() {
        return checks;
    }
}
