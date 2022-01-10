/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import org.geoserver.web.ComponentInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geotools.data.DataAccessFactory;
import org.opengis.coverage.grid.Format;

/**
 * Used to declare a data store panel information and its icon. Both are optional, you can specify
 * the configuration panel but not the icon, or the opposite.
 *
 * @author aaime
 */
@SuppressWarnings("serial")
public class DataStorePanelInfo extends ComponentInfo<StoreEditPanel> {
    Class<?> factoryClass;

    String icon;

    Class<?> iconBase;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Class<?> getIconBase() {
        return iconBase;
    }

    /**
     * Used as the reference class to locate the datastore icon (since the component might not be
     * there)
     */
    public void setIconBase(Class<?> iconBase) {
        this.iconBase = iconBase;
    }

    public Class<?> getFactoryClass() {
        return factoryClass;
    }

    /** @param factoryClassName either a {@link DataAccessFactory} or {@link Format} subclass */
    public void setFactoryClass(Class<?> factoryClassName) {
        this.factoryClass = factoryClassName;
    }
}
