/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbc.metrics;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * Simple endpoint to expose JDBC request metrics.
 *
 * <p>This class utilizes a cache of request metric values, keyed by request id. The cache entries
 * are short lived as it is assumed that client code requesting metrics will do shortly after the
 * original request. That said the cache expiry time is configurable with a default value of 1
 * minute.
 */
public class RequestMetricsController extends AbstractController {

    static Cache<String, Map<String, Object>> METRICS =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(Long.getLong("jdbc-metrics.cacheExpiry", 1), TimeUnit.MINUTES)
                    .maximumSize(Long.getLong("jdbc-metrics.cacheMaxSize", 2000))
                    .build();

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse rsp)
            throws Exception {

        String[] split = req.getPathInfo().split("/");
        String reqId =
                StringUtils.stripEnd(StringUtils.stripStart(split[split.length - 1], "/"), "/");

        JSONObject obj = new JSONObject();
        obj.put("request", reqId);

        Map<String, Object> m = METRICS.getIfPresent(reqId);
        if (m != null) {
            obj.put("metrics", m);
            rsp.setStatus(HttpServletResponse.SC_OK);
        } else {
            rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            obj.put("message", "metrics unavailable");
        }

        rsp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try (OutputStreamWriter w = new OutputStreamWriter(rsp.getOutputStream(), Charsets.UTF_8)) {
            obj.write(w);
            w.flush();
        }

        return null;
    }
}
