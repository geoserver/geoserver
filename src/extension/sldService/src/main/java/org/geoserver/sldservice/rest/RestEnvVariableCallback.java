/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.rest.DispatcherCallbackAdapter;
import org.geoserver.rest.RestException;
import org.geotools.filter.function.EnvFunction;
import org.springframework.http.HttpStatus;

/** Parses and sets the environment variables, clears them at the end of the request */
public class RestEnvVariableCallback extends DispatcherCallbackAdapter {

    static final FormatOptionsKvpParser PARSER = new FormatOptionsKvpParser("env");

    /**
     * Parses and sets the environment variables from their "var1:value1;v2:value;..." syntax, that
     * a {@link org.springframework.stereotype.Controller} retrieved from the request some way
     * (ideally via a "env" KVP parameter, but we don't want to be prescriptive about it, in REST
     * "env" could be used for something else
     */
    public static void setOptions(String unparsedOptions) {
        try {
            Map<String, Object> localEnvVars = (Map<String, Object>) PARSER.parse(unparsedOptions);
            EnvFunction.setLocalValues(localEnvVars);
        } catch (Exception e) {
            throw new RestException(
                    "Invalid syntax for environment variables", HttpStatus.BAD_REQUEST, e);
        }
    }

    @Override
    public void finished(HttpServletRequest request, HttpServletResponse response) {
        EnvFunction.clearLocalValues();
    }
}
