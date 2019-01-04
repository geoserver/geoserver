/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/**
 * Interface for GeoServer Pages which can simply ignore ConfigurationLock since they are safe.
 *
 * <p>The developer *MUST* know what he is doing and manage the configuration read/write safely!
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public interface GeoServerUnlockablePage {}
