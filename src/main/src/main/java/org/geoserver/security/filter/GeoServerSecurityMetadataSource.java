/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.AntPathRequestMatcher;
import org.springframework.security.web.util.RequestMatcher;

/**
 * Justin, nasty hack to get rid of the spring bean
 * "filterSecurityInterceptor";
 * I think, there is a better was to solve this.
 * 
 * 
 * @author mcr
 *
 */
public class GeoServerSecurityMetadataSource extends DefaultFilterInvocationSecurityMetadataSource {

    
    static LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>> requestMap;
    static {
        
        requestMap= new LinkedHashMap<RequestMatcher, Collection<ConfigAttribute>>();
        
        RequestMatcher matcher = new AntPathRequestMatcher("/config/**");                
        List<ConfigAttribute> list = new ArrayList<ConfigAttribute>();
        list.add(new SecurityConfig("ROLE_ADMINISTRATOR"));
        requestMap.put(matcher,list);

        matcher = new AntPathRequestMatcher("/**");
        list = new ArrayList<ConfigAttribute>();
        list.add(new SecurityConfig("IS_AUTHENTICATED_ANONYMOUSLY"));
        requestMap.put(matcher,list);                
    };
    
    public GeoServerSecurityMetadataSource() {
        super(requestMap);
        /*
        <sec:intercept-url pattern="/config/**" access="ROLE_ADMINISTRATOR"/>
        <sec:intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY"/>
        */
        
    }    
}
