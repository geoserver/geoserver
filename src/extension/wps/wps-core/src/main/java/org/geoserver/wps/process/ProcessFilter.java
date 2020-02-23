/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import org.geotools.process.ProcessFactory;

/**
 * Implementors of this interface can filter out processes that must not be exposed in the current
 * request and allow for wrapping the processes in case extra scrutiny needs to be applied.
 *
 * <p>Use cases for this interface are static configuration of available processes from the existing
 * factories, security restrictions based on the current user, and general verification and
 * manipulation of process inputs and outputs. The implementation must be registered as a bean in
 * the Spring context for GeoServer to pick it up and use it.
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface ProcessFilter {

    /**
     * Returns the process factory (eventually wrapped) if the factory is allowed, null if factory
     * is to be filtered out.
     *
     * <p>The factory provided by this method might have been wrapped already by another filter, so
     * don't assume it's possible to make an instance-of check against it to recognize a particular
     * factory. In case {@link DelegatingProcessFactory} is used the {@link
     * DelegatingProcessFactory#getInnermostDelegate()} method can be used to reach the original
     * factory
     */
    ProcessFactory filterFactory(ProcessFactory pf);
}
