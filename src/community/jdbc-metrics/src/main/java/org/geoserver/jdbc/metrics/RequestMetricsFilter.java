/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbc.metrics;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.GeoServerNodeData;
import org.geoserver.filters.GeoServerFilter;
import org.geoserver.ows.util.ResponseUtils;

/**
 * Servlet filter that adds headers providing access to jdbc request metrics.
 *
 * <p>Since headers need to be written before the start of the response body we need to store the
 * metric values into a cache to be later retrieved. See {@link RequestMetricsController} for
 * details.
 */
public class RequestMetricsFilter implements GeoServerFilter {

    AtomicLong requestId = new AtomicLong(System.currentTimeMillis());

    GeoServerNodeData nodeData = GeoServerNodeData.createFromEnvironment();

    @Override
    public void init(FilterConfig config) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRsp = (HttpServletResponse) rsp;

        // set a header that represents the request id
        String reqId = String.valueOf(requestId.getAndIncrement());
        httpRsp.setHeader("x-geoserver-request", reqId);
        httpRsp.setHeader(
                "x-geoserver-jdbc-metrics",
                ResponseUtils.baseURL(httpReq) + "jdbc-metrics/" + reqId);
        httpRsp.setHeader("x-geoserver-node-info", nodeData.getId());

        // do the request
        filterChain.doFilter(req, rsp);

        // write the metrics out to the cache
        Optional.ofNullable(RequestMetricsCallback.metrics.get())
                .ifPresent(
                        map -> {
                            RequestMetricsController.METRICS.put(reqId, map);
                            RequestMetricsCallback.metrics.remove();
                        });
    }

    @Override
    public void destroy() {}
}
