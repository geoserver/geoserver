/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.List;

/**
 * Provider of GeoServer extensions.
 *
 * <p>Implementations of this interface must be registered in a spring context.
 *
 * <pre>
 * &lt;bean id="myExtensionProvider" class="com.xyz.MyExtensionProvider"/&gt;
 * </pre>
 *
 * <p>After which the extension lookup methods in {@link GeoServerExtensions} will use any instances
 * of this interface as a supplementary lookup to the initial spring context lookup.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface ExtensionProvider<T> {

    /** The extension point this provider handles. */
    Class<T> getExtensionPoint();

    /**
     * Returns a list of extensions that implement the specified class.
     *
     * @param extensionPoint The class for which implementations are being looked up.
     * @return A list of objects implementing <tt>extensionPoint</tt>, or an empty list if no such
     *     objects are available.
     */
    List<T> getExtensions(Class<T> extensionPoint);
}
