/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.servlet.ServletRequest;
import org.geoserver.util.LoggerRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@SuppressWarnings({"PMD.AvoidUsingHardCodedIP", "PMD.AvoidXForwardedForHeader"})
public class RemoteIpFilterTest {

    @Rule
    public LoggerRule logging = new LoggerRule(RemoteIpFilter.LOGGER, Level.WARNING);

    private static final List<IpAddressMatcher> DEFAULTS =
            RemoteIpFilter.buildMatchers(RemoteIpFilter.DEFAULT_TRUSTED_PROXIES);

    /** The private ranges Tomcat's RemoteIpValve trusts by default, for proxies on another host. */
    private static final List<IpAddressMatcher> PRIVATE =
            RemoteIpFilter.buildMatchers("127.0.0.0/8,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16");

    private static MockHttpServletRequest request(String peer, String... forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(peer);
        for (String value : forwardedFor) {
            request.addHeader("X-Forwarded-For", value);
        }
        return request;
    }

    @Test
    public void testPeerNotTrustedIgnoresForwardedFor() {
        // peer is a public address, not a trusted proxy, so the header is ignored
        MockHttpServletRequest request = request("203.0.113.7", "10.0.0.5");
        assertEquals("203.0.113.7", RemoteIpFilter.resolve(request, DEFAULTS));
    }

    @Test
    public void testPeerNotTrustedNoHeader() {
        MockHttpServletRequest request = request("203.0.113.7");
        assertEquals("203.0.113.7", RemoteIpFilter.resolve(request, DEFAULTS));
    }

    @Test
    public void testTrustedPeerSingleClient() {
        MockHttpServletRequest request = request("10.0.0.1", "198.51.100.23");
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testTrustedPeerNoHeaderKeepsPeer() {
        MockHttpServletRequest request = request("10.0.0.1");
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testTrustedPeerReturnsRightmostUntrusted() {
        // the client is the right-most hop that is not a trusted proxy
        MockHttpServletRequest request = request("10.0.0.1", "198.51.100.23, 172.16.0.9");
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testAllHopsTrustedReturnsLeftmost() {
        MockHttpServletRequest request = request("10.0.0.1", "192.168.1.5, 172.16.0.9");
        assertEquals("192.168.1.5", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testLoopbackHopSkipped() {
        // two proxies on the same host: the loopback hop is the first proxy, the client is on its left
        MockHttpServletRequest request = request("127.0.0.1", "198.51.100.23, 127.0.0.1");
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request, DEFAULTS));
    }

    @Test
    public void testLoopbackOnlyHeaderReturnsPeer() {
        // a request made on the proxy host itself comes from the proxy
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request("10.0.0.1", "127.0.0.1"), PRIVATE));
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request("10.0.0.1", "::1"), PRIVATE));
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request("10.0.0.1", "127.0.0.1, 127.0.0.2"), PRIVATE));
    }

    @Test
    public void testUntrustedLoopbackHopStopsWalk() {
        // loopback is not in the custom list, so the hop on its left may come from a local process
        List<IpAddressMatcher> trusted = RemoteIpFilter.buildMatchers("10.1.2.3/32");
        MockHttpServletRequest request = request("10.1.2.3", "198.51.100.23, 127.0.0.1");
        assertEquals("10.1.2.3", RemoteIpFilter.resolve(request, trusted));
    }

    @Test
    public void testHopPortAndBracketsStripped() {
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request("10.0.0.1", "198.51.100.23:5555"), PRIVATE));
        assertEquals("2001:db8::1", RemoteIpFilter.resolve(request("10.0.0.1", "[2001:db8::1]:1234"), PRIVATE));
        assertEquals("2001:db8::1", RemoteIpFilter.resolve(request("10.0.0.1", "[2001:db8::1]"), PRIVATE));
        assertEquals("2001:db8::1", RemoteIpFilter.resolve(request("10.0.0.1", "2001:db8::1"), PRIVATE));
    }

    @Test
    public void testInvalidHopStopsAtLastTrustedHop() {
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request("10.0.0.1", "198.51.100.23, unknown"), PRIVATE));
        MockHttpServletRequest request = request("10.0.0.1", "198.51.100.23, proxy.example, 172.16.0.9");
        assertEquals("172.16.0.9", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testMultipleForwardedForHeadersMerged() {
        MockHttpServletRequest request = request("10.0.0.1", "198.51.100.23", "172.16.0.9");
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testForwardedForWithBlankTokens() {
        MockHttpServletRequest request = request("10.0.0.1", "198.51.100.23, ");
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request, PRIVATE));
    }

    @Test
    public void testNarrowTrustList() {
        List<IpAddressMatcher> trusted = RemoteIpFilter.buildMatchers("10.1.2.3/32");
        // the proxy address is not in the narrow list, so the header is ignored
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request("10.0.0.1", "198.51.100.23"), trusted));
        // exact proxy match, the forwarded client is used
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request("10.1.2.3", "198.51.100.23"), trusted));
    }

    @Test
    public void testDefaultsTrustLoopbackOnly() {
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request("127.0.0.1", "198.51.100.23"), DEFAULTS));
        assertEquals("198.51.100.23", RemoteIpFilter.resolve(request("0:0:0:0:0:0:0:1", "198.51.100.23"), DEFAULTS));
        // a proxy in a private range is not trusted unless listed
        assertEquals("10.0.0.1", RemoteIpFilter.resolve(request("10.0.0.1", "198.51.100.23"), DEFAULTS));
        assertEquals("172.17.0.1", RemoteIpFilter.resolve(request("172.17.0.1", "10.0.0.5"), DEFAULTS));
    }

    @Test
    public void testBuildMatchersSkipsInvalidMask() {
        List<IpAddressMatcher> matchers = RemoteIpFilter.buildMatchers("10.0.0.0/8, not-an-ip, 192.168.0.0/16");
        assertEquals(2, matchers.size());
    }

    @Test
    public void testFilterWrapsRemoteAddr() throws Exception {
        ServletRequest request = run(filter(null), request("127.0.0.1", "198.51.100.23"));
        assertEquals("198.51.100.23", request.getRemoteAddr());
        assertEquals("198.51.100.23", request.getRemoteHost());
    }

    @Test
    public void testFilterReadsTrustedProxiesFromContext() throws Exception {
        RemoteIpFilter filter = filter("203.0.113.1");
        ServletRequest listed = run(filter, request("203.0.113.1", "198.51.100.23"));
        assertEquals("198.51.100.23", listed.getRemoteAddr());
        // the configured list replaces the default
        ServletRequest unlisted = run(filter, request("10.0.0.1", "198.51.100.23"));
        assertEquals("10.0.0.1", unlisted.getRemoteAddr());
    }

    @Test
    public void testIgnoredHeaderLoggedOnce() throws Exception {
        RemoteIpFilter filter = filter(null);
        run(filter, request("127.0.0.1", "198.51.100.23"));
        run(filter, request("203.0.113.7"));
        assertEquals(0, logging.records().size());

        run(filter, request("203.0.113.7", "198.51.100.23"));
        run(filter, request("203.0.113.8", "198.51.100.24"));
        List<LogRecord> records = logging.records();
        assertEquals(1, records.size());
        assertEquals(Level.WARNING, records.get(0).getLevel());
        assertEquals(List.of("203.0.113.7"), List.of(records.get(0).getParameters()));
    }

    /** Builds a filter initialized like a servlet container does, with an optional trusted proxy context param. */
    private static RemoteIpFilter filter(String trustedProxies) throws Exception {
        MockServletContext context = new MockServletContext();
        if (trustedProxies != null) {
            context.addInitParameter(RemoteIpFilter.TRUSTED_PROXIES_PROPERTY, trustedProxies);
        }
        RemoteIpFilter filter = new RemoteIpFilter();
        filter.init(new MockFilterConfig(context));
        return filter;
    }

    /** Runs the request through the filter and returns the request the rest of the chain sees. */
    private static ServletRequest run(RemoteIpFilter filter, MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain.getRequest();
    }
}
