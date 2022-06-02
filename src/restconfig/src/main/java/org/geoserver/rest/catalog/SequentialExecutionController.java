/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

/**
 * A marker interface. Any controller class implementing this interface will not allow parallel
 * execution for REST APIs
 */
public interface SequentialExecutionController {}
