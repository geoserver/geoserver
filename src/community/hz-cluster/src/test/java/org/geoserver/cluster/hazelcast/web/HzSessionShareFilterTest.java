/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.hazelcast.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.geoserver.cluster.hazelcast.HzCluster;
import org.junit.Before;
import org.junit.Test;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.context.WebApplicationContext;

public class HzSessionShareFilterTest {

    private HzSessionShareFilter filter;
    private ServletContext servletContext;
    private HzCluster cluster;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        filter = new HzSessionShareFilter();
        servletContext = mock(ServletContext.class);
        WebApplicationContext applicationContext = mock(WebApplicationContext.class);
        cluster = mock(HzCluster.class);
        // session sharing is enabled by default when no configuration disables it
        HazelcastInstance hz = mock(HazelcastInstance.class);
        IMap<Object, Object> sessionMap = mock(IMap.class);

        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(applicationContext);
        when(applicationContext.getBean("hzCluster", HzCluster.class)).thenReturn(cluster);
        when(cluster.getHz()).thenReturn(hz);
        when(hz.getConfig()).thenReturn(new Config());
        when(hz.getMap(HzSessionShareFilter.SESSION_MAP_NAME)).thenReturn(sessionMap);
    }

    @Test
    public void testLazyCreationAndEnabled() throws Exception {
        when(cluster.isSessionSharing()).thenReturn(true);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        filter.init(filterConfig);

        // Initially no delegate
        assertNull(filter.getDelegate());

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Delegate should be created
        SessionRepositoryFilter<?> delegate = filter.getDelegate();
        assertNotNull(delegate);

        // Subsequent calls should reuse the same delegate
        filter.doFilter(request, response, chain);
        assertSame(delegate, filter.getDelegate());

        verify(cluster, atLeastOnce()).isSessionSharing();
        verify(cluster, times(1)).getHz();
    }

    @Test
    public void testDisabledBehavior() throws Exception {
        when(cluster.isSessionSharing()).thenReturn(false);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        filter.init(filterConfig);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // No delegate created because session sharing is disabled
        assertNull(filter.getDelegate());
        verify(chain).doFilter(request, response);
    }

    @Test
    public void testResetDelegate() throws Exception {
        when(cluster.isSessionSharing()).thenReturn(true);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        filter.init(filterConfig);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        assertNotNull(filter.getDelegate());

        // Reset the delegate
        filter.resetDelegate();
        assertNull(filter.getDelegate());

        // Next request should recreate it if still enabled
        filter.doFilter(request, response, chain);
        assertNotNull(filter.getDelegate());
    }

    @Test
    public void testDestroy() throws Exception {
        when(cluster.isSessionSharing()).thenReturn(true);

        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        filter.init(filterConfig);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);
        assertNotNull(filter.getDelegate());

        filter.destroy();
        assertNull(filter.getDelegate());
    }
}
