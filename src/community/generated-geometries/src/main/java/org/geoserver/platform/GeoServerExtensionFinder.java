/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.List;

// TODO: should we move it to "platform" module?
/** Abstraction layer for removing unwanted static dependencies. */
@FunctionalInterface
public interface GeoServerExtensionFinder extends Serializable {

    /**
     * (Description borrowed from {@link
     * org.geoserver.platform.GeoServerExtensions#extensions(Class)})
     *
     * <p>Loads all extensions implementing or extending <code>extensionPoint</code>.
     *
     * <p>This method uses the "default" application context to perform the lookup. See {@link
     * #setApplicationContext(ApplicationContext)}.
     *
     * @param extensionPoint The class or interface of the extensions.
     * @return A collection of the extensions, or an empty collection.
     *     <p>{@link org.geoserver.platform.GeoServerExtensions#extensions(Class)}
     */
    <T> List<T> find(Class<T> extensionPoint);
}
