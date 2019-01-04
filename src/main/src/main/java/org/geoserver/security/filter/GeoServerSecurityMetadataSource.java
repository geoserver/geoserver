/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Justin, nasty hack to get rid of the spring bean "filterSecurityInterceptor"; I think, there is a
 * better was to solve this.
 *
 * @author mcr
 */
public class GeoServerSecurityMetadataSource extends DefaultFilterInvocationSecurityMetadataSource {

    /**
     * Should match
     *
     * <p>/web/?wicket:bookmarkablePage=:org.geoserver.web.GeoServerLoginPage&error=false
     *
     * @author christian
     */
    static class LoginPageRequestMatcher implements RequestMatcher {

        RequestMatcher webChainMatcher1 =
                new AntPathRequestMatcher("/" + GeoServerSecurityFilterChain.WEB_CHAIN_NAME);

        RequestMatcher webChainMatcher2 =
                new AntPathRequestMatcher("/" + GeoServerSecurityFilterChain.WEB_CHAIN_NAME + "/");

        @Override
        public boolean matches(HttpServletRequest request) {

            // check if we are on the "web" chain
            boolean isOnWebChain =
                    webChainMatcher1.matches(request) || webChainMatcher2.matches(request);
            if (isOnWebChain == false) return false;

            Map params = request.getParameterMap();
            if (params.size() != 2) return false;

            String[] pageClass = (String[]) params.get("wicket:bookmarkablePage");
            if (pageClass == null || pageClass.length != 1) return false;

            if (":org.geoserver.web.GeoServerLoginPage".equals(pageClass[0]) == false) return false;

            String error[] = (String[]) params.get("error");
            if (error == null || error.length != 1) return false;

            return true;
        }
    };

    static LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap;

    static {
        requestMap = new LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>>();

        // the login page is a public resource
        requestMap.put(new LoginPageRequestMatcher(), new ArrayList<ConfigAttribute>());
        // images,java script,... are public resources
        requestMap.put(
                new AntPathRequestMatcher("/web/resources/**"), new ArrayList<ConfigAttribute>());

        RequestMatcher matcher = new AntPathRequestMatcher("/config/**");
        List<ConfigAttribute> list = new ArrayList<ConfigAttribute>();
        list.add(new SecurityConfig(GeoServerRole.ADMIN_ROLE.getAuthority()));
        requestMap.put(matcher, list);

        matcher = new AntPathRequestMatcher("/**");
        list = new ArrayList<ConfigAttribute>();
        list.add(new SecurityConfig("IS_AUTHENTICATED_ANONYMOUSLY"));
        requestMap.put(matcher, list);
    };

    public GeoServerSecurityMetadataSource() {
        super(requestMap);
        /*
        <sec:intercept-url pattern="/config/**" access="ROLE_ADMINISTRATOR"/>
        <sec:intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>
        */

    }
}
