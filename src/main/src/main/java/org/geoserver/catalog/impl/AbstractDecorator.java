/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.Wrapper;

/**
 * Generic delegating base class. Provides the following features:
 *
 * <ul>
 *   <li>null check for the delegate object
 *   <li>direct forwarding of {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()}
 *       to the delegate
 *   <li>implements the Wrapper interface for programmatic extraction
 * </ul>
 *
 * @deprecated use org.geotools.decorate.AbstractDecorator
 */
public abstract class AbstractDecorator<D> extends org.geotools.decorate.AbstractDecorator<D>
        implements Wrapper {

    public AbstractDecorator(D delegate) {
        super(delegate);
    }
}
