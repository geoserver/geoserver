/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.geoserver.cluster.impl.handlers.catalog.CatalogUtils;

/**
 * This class implements a set of function inspired by the Apache BeanUtils defining wrappers which
 * are designed to work with the GeoServer catalog and configuration
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class BeanUtils {

    /**
     * This is a 'smart' (perform checks for some special cases) update function which should be
     * used to copy of the properties for objects of the catalog and configuration.
     *
     * @param <T> the type of the bean to update
     * @param info the bean instance to update
     * @param properties the list of string of properties to update
     * @param values the list of new values to update
     */
    public static <T> void smartUpdate(
            final T info, final List<String> properties, final List<Object> values)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final Iterator<String> itPropertyName = properties.iterator();
        final Iterator<Object> itValue = values.iterator();
        while (itPropertyName.hasNext() && itValue.hasNext()) {
            String propertyName = itPropertyName.next();
            final Object value = itValue.next();

            PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(info, propertyName);
            // return null if there is no such descriptor
            if (pd == null) {
                // this is a special case used by the NamespaceInfoImpl setURI
                // the propertyName coming from the ModificationProxy is set to 'uRI'
                // lets set it to uri
                propertyName = propertyName.toUpperCase();
                pd = PropertyUtils.getPropertyDescriptor(info, propertyName);
                if (pd == null) {
                    return;
                }
            }
            if (pd.getWriteMethod() != null) {
                PropertyUtils.setProperty(info, propertyName, value);
            } else {
                // T interface do not declare setter method for this property
                // lets use getter methods to get the property reference
                final Object property = PropertyUtils.getProperty(info, propertyName);

                // check type of property to apply new value
                if (Collection.class.isAssignableFrom(pd.getPropertyType())) {
                    final Collection<?> liveCollection = (Collection<?>) property;
                    liveCollection.clear();
                    liveCollection.addAll((Collection) value);
                } else if (Map.class.isAssignableFrom(pd.getPropertyType())) {
                    final Map<?, ?> liveMap = (Map<?, ?>) property;
                    liveMap.clear();
                    liveMap.putAll((Map) value);
                } else {
                    if (CatalogUtils.LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                        CatalogUtils.LOGGER.severe(
                                "Skipping unwritable property "
                                        + propertyName
                                        + " with property type "
                                        + pd.getPropertyType());
                }
            }
        }
    }
}
