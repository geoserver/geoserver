/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.core.log.LogMessage;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * {@link SecurityMetadataSource} implementation for GeoServer web UI, provides {@link ConfigAttribute}s with
 * authentication and authorization constraints for evaluation through {@link GeoServerSecurityInterceptorFilter}.
 *
 * @author mcr
 */
@SuppressWarnings({"deprecation", "removal"})
public class GeoServerSecurityMetadataSource implements SecurityMetadataSource {

    /**
     * Should match
     *
     * <p>/web/?wicket:bookmarkablePage=:org.geoserver.web.GeoServerLoginPage&error=false
     *
     * @author christian
     */
    static class LoginPageRequestMatcher implements RequestMatcher {

        RequestMatcher webChainMatcher1 =
                PathPatternRequestMatcher.withDefaults().matcher("/" + GeoServerSecurityFilterChain.WEB_CHAIN_NAME);

        RequestMatcher webChainMatcher2 = PathPatternRequestMatcher.withDefaults()
                .matcher("/" + GeoServerSecurityFilterChain.WEB_CHAIN_NAME + "/");

        @Override
        public boolean matches(HttpServletRequest request) {

            // check if we are on the "web" chain
            boolean isOnWebChain = webChainMatcher1.matches(request) || webChainMatcher2.matches(request);
            if (!isOnWebChain) return false;

            Map<String, String[]> params = request.getParameterMap();
            if (params.size() != 2) return false;

            String[] pageClass = params.get("wicket:bookmarkablePage");
            if (pageClass == null || pageClass.length != 1) return false;

            if (!":org.geoserver.web.GeoServerLoginPage".equals(pageClass[0])) return false;

            String[] error = params.get("error");
            if (error == null || error.length != 1) return false;

            return true;
        }
    }

    static final Map<RequestMatcher, Collection<ConfigAttribute>> requestMap;

    static {
        LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> map = new LinkedHashMap<>();

        // the login page is a public resource
        map.put(new LoginPageRequestMatcher(), new ArrayList<>());
        // images,java script,... are public resources
        map.put(PathPatternRequestMatcher.withDefaults().matcher("/web/resources/**"), new ArrayList<>());

        RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher("/config/**");
        List<ConfigAttribute> list = new ArrayList<>();
        list.add(new SecurityConfig(GeoServerRole.ADMIN_ROLE.getAuthority()));
        map.put(matcher, list);

        matcher = PathPatternRequestMatcher.withDefaults().matcher("/**");
        list = new ArrayList<>();
        list.add(new SecurityConfig("IS_AUTHENTICATED_ANONYMOUSLY"));
        map.put(matcher, list);

        requestMap = Collections.unmodifiableMap(map);
    }

    private final Logger logger = Logging.getLogger(GeoServerSecurityMetadataSource.class);

    public GeoServerSecurityMetadataSource() {
        super();
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        Set<ConfigAttribute> allAttributes = new HashSet<>();
        requestMap.values().forEach(allAttributes::addAll);
        return allAttributes;
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) {
        final HttpServletRequest request = (HttpServletRequest) object;
        int count = 0;
        for (Map.Entry<RequestMatcher, Collection<ConfigAttribute>> entry : requestMap.entrySet()) {
            if (entry.getKey().matches(request)) {
                return entry.getValue();
            } else {
                if (this.logger.isLoggable(Level.FINEST)) {
                    String msg = LogMessage.format(
                                    "Did not match request to %s - %s (%d/%d)",
                                    entry.getKey(), entry.getValue(), ++count, requestMap.size())
                            .toString();
                    this.logger.finest(msg);
                }
            }
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return HttpServletRequest.class.isAssignableFrom(clazz);
    }
}
