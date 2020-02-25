/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

/**
 * Pluggable extension that can be used to filter out an extension point implementation before it
 * gets returned by {@link GeoServerExtensions#extensions(Class)}
 *
 * @author Andrea Aime - OpenGeo
 */
public interface ExtensionFilter {

    /**
     * If any registered {@link ExtensionFilter} returns {@code true} the bean in question will be
     * removed from the list returned by {@link GeoServerExtensions#extensions(Class)}
     *
     * @param beanId The bean id as registered in the Spring context, or {@code null} if the bean is
     *     coming from the GeoTools SPI bridge
     * @param bean The bean itself
     * @return true to exclude
     */
    boolean exclude(String beanId, Object bean);
}
