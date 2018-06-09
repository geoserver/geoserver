/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.resource;

/**
 * A resource managed during a WPS process execution. It can be anything, from a feature type, a
 * file, a directory, an embedded database table. The interface makes sure the resource can be
 * cleaned up when the request execution is terminated, or, if the process is asynchronous, when the
 * results availability expires.
 *
 * <p>Normally all resources linked to a process have to be kept around as long as one of the
 * resulting feature collections or coverages is eligible to be collected: there is no way to know
 * if the processes are computing the result once, or if they are generating streaming outputs that
 * will recompute the results every time they are queried (the JAI efficient case for coverages
 * would be the latter)
 *
 * @author Andrea Aime - OpenGeo
 */
public interface WPSResource {

    /** Deletes the resource permanently */
    void delete() throws Exception;

    /** The resource name, used for error reporting */
    String getName();
}
