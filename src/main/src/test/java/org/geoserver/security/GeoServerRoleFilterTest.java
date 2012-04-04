package org.geoserver.security;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.GeoServerRoleFilterConfig;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.impl.GeoServerRole;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoServerRoleFilterTest extends GeoServerSecurityTestSupport {

    
    public void testFilterChainWithEnabled() throws Exception {
        
        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerRoleFilterConfig config = new GeoServerRoleFilterConfig();
        config.setName("roleConverter");
        config.setClassName(GeoServerRoleFilter.class.getName());
        config.setRoleConverterName("roleConverter");
        config.setHttpResponseHeaderAttrForIncludedRoles("ROLES");
        secMgr.saveFilter(config);

        
        MockHttpServletRequest request = createRequest("/foo");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        chain.addFilter(getSecurityManager().loadFilter("roleConverter"));
        
        GeoServerSecurityFilterChainProxy filterChainProxy = 
            GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);
        assertEquals(GeoServerRole.ANONYMOUS_ROLE.getAuthority(),response.getHeader("ROLES"));        
    }

    public void testFilterChainWithDisabled() throws Exception {

        MockHttpServletRequest request = createRequest("/foo");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        GeoServerSecurityFilterChainProxy filterChainProxy = 
            GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);
        assertNull(response.getHeader("ROLES"));
        
    }



}
