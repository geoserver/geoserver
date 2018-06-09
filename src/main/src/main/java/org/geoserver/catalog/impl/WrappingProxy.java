/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.InvocationHandler;

/**
 * Interface which exposes an underlying object being proxied.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface WrappingProxy extends InvocationHandler {

    /** The underlying object being proxied. */
    Object getProxyObject();
}
