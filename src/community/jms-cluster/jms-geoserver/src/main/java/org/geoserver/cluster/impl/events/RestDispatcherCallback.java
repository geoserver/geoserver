/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.events;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.rest.DispatcherCallback;
import org.geotools.util.logging.Logging;

public class RestDispatcherCallback implements DispatcherCallback {
    static final java.util.logging.Logger LOGGER = Logging.getLogger(RestDispatcherCallback.class);

    private static final ThreadLocal<Map<String, String>> parameters = new ThreadLocal<>();

    public static Map<String, String> getParameters() {
        return parameters.get();
    }

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        // get request parameters
        Map<String, String> parameters =
                request.getParameterMap()
                        .entrySet()
                        .stream()
                        .map(
                                entry -> {
                                    String[] values = entry.getValue();
                                    String value =
                                            values == null || values.length == 0 ? null : values[0];
                                    return new SimpleEntry<>(entry.getKey(), value);
                                })
                        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
        // log request parameters
        if (LOGGER.isLoggable(Level.FINE)) {
            parameters
                    .keySet()
                    .forEach(
                            parameter ->
                                    LOGGER.info("Registering incoming parameter: " + parameter));
        }
        // set local parameters
        this.parameters.set(parameters);
    }

    @Override
    public void dispatched(
            HttpServletRequest request, HttpServletResponse response, Object handler) {}

    @Override
    public void exception(
            HttpServletRequest request, HttpServletResponse response, Exception error) {}

    @Override
    public void finished(HttpServletRequest request, HttpServletResponse response) {}
}
