/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletResponse;

/**
 * Factory used to create an {@link ServiceStrategy} for a request.
 *
 * <p>An {@link ServiceStrategy} is used to provide different modes of writing output.
 *
 * <p>Examples include a "safe mode", in which the entire response is buffered before being output,
 * as to ensure no errors occur during the writing of a response. Another example may include a
 * "speed mode" where data is written directly to the output with no buffering.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public interface OutputStrategyFactory {
    /**
     * Creates an output strategy for a response.
     *
     * @param response The response which contains the live output stream in which to write data to.
     * @return An output strategy to write output to.
     */
    ServiceStrategy createOutputStrategy(HttpServletResponse response);
}
