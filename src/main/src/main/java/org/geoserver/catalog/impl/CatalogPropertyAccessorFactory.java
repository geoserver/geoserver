/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.Info;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.factory.Hints;

/** Property accessor for GeoServer {@link Info} configuration objects. */
public class CatalogPropertyAccessorFactory implements PropertyAccessorFactory {

    private static final CatalogPropertyAccessor INSTANCE = new CatalogPropertyAccessor();

    @Override
    public PropertyAccessor createPropertyAccessor(
            Class<?> type, String xpath, Class<?> target, Hints hints) {
        if (Info.class.isAssignableFrom(type)) {
            return INSTANCE;
        }
        return null;
    }
}
