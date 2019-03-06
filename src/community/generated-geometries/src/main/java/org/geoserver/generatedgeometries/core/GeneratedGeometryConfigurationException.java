/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.generatedgeometries.core;

/** Thrown when there is an error in geometry strategy configuration. */
public class GeneratedGeometryConfigurationException extends RuntimeException {

    public GeneratedGeometryConfigurationException(Exception e) {
        super(e);
    }

    public GeneratedGeometryConfigurationException(String message) {
        super(message);
    }
}
