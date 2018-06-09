/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

/**
 * A specific exception class for unknown execution id
 *
 * @author Andrea Aime - GeoSolutions
 */
public class UnknownExecutionIdException extends WPSException {
    private static final long serialVersionUID = 3886845200543307484L;

    public UnknownExecutionIdException(String executionId) {
        super(
                "Unknown execution id "
                        + executionId
                        + ", either the execution was never submitted, was dismissed, or too much time "
                        + "passed since the process completed");
    }
}
