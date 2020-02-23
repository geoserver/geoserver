/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.WrapperPolicy;

/**
 * Builds secured versions of various catalog and data access object used by GeoServer. Acts as an
 * extension point for users in need to preserve the original interface of the wrapped object.
 *
 * @author Andrea Aime - TOPP
 */
public interface SecuredObjectFactory extends ExtensionPriority {

    /**
     * Returns true if this factory can properly wrap the specified objects of the specified class
     */
    boolean canSecure(Class clazz);

    /**
     * Wraps the data access object into a secured wrapper
     *
     * @param object The object to be wrapped, <code>canWrap(object.getClass())</code> must return
     *     true, otherwise an {@link IllegalArgumentException} will be thrown
     * @param policy The secure object handling policy the wrapper should abide to
     * @return a read only wrapper for the specified object
     */
    Object secure(Object object, WrapperPolicy policy);
}
