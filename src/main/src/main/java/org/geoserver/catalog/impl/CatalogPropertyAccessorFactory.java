/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.Info;
import org.geotools.factory.Hints;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;

/**
 * Property accessor for GeoServer {@link Info} configuration objects.
 * 
 */
public class CatalogPropertyAccessorFactory implements PropertyAccessorFactory {

    private static final CatalogPropertyAccessor INSTANCE = new CatalogPropertyAccessor();

    @Override
    public PropertyAccessor createPropertyAccessor(Class<?> type, String xpath, Class<?> target,
            Hints hints) {
        if (Info.class.isAssignableFrom(type)) {
            return INSTANCE;
        }
        return null;
    }
}
