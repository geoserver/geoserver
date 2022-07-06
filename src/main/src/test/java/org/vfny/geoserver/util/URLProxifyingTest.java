/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.ows.HTTPHeadersCollector;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class URLProxifyingTest {

    ProxifyingURLMangler mangler;

    void createAppContext(
            String proxyBaseUrl,
            Boolean useHeadersProxyURLIn,
            String forwardedHeader,
            String forwardedProtoHeader,
            String host,
            String forwardedHost,
            String forwardedPath,
            String forwarded,
            boolean localWorkspaceSettingsEnabled,
            boolean useWorkspaceHeadersProxyUrl) {

        Map<String, String> headers = new HashMap<>();
        headers.put(
                ProxifyingURLMangler.Headers.FORWARDED.asString().toLowerCase(), forwardedHeader);
        headers.put(
                ProxifyingURLMangler.Headers.FORWARDED_PROTO.asString().toLowerCase(),
                forwardedProtoHeader);
        headers.put(ProxifyingURLMangler.Headers.HOST.asString().toLowerCase(), host);
        headers.put(
                ProxifyingURLMangler.Headers.FORWARDED_HOST.asString().toLowerCase(),
                forwardedHost);
        headers.put(
                ProxifyingURLMangler.Headers.FORWARDED_PATH.asString().toLowerCase(),
                forwardedPath);
        headers.put(ProxifyingURLMangler.Headers.FORWARDED.asString().toLowerCase(), forwarded);

        HttpServletRequest servletRequest = createNiceMock(HttpServletRequest.class);

        expect(
                        servletRequest.getHeader(
                                ProxifyingURLMangler.Headers.FORWARDED.asString().toLowerCase()))
                .andReturn(
                        headers.get(
                                ProxifyingURLMangler.Headers.FORWARDED.asString().toLowerCase()))
                .anyTimes();
        expect(
                        servletRequest.getHeader(
                                ProxifyingURLMangler.Headers.FORWARDED_PROTO
                                        .asString()
                                        .toLowerCase()))
                .andReturn(
                        headers.get(
                                ProxifyingURLMangler.Headers.FORWARDED_PROTO
                                        .asString()
                                        .toLowerCase()))
                .anyTimes();
        expect(servletRequest.getHeader(ProxifyingURLMangler.Headers.HOST.asString().toLowerCase()))
                .andReturn(headers.get(ProxifyingURLMangler.Headers.HOST.asString().toLowerCase()))
                .anyTimes();
        expect(
                        servletRequest.getHeader(
                                ProxifyingURLMangler.Headers.FORWARDED_HOST
                                        .asString()
                                        .toLowerCase()))
                .andReturn(
                        headers.get(
                                ProxifyingURLMangler.Headers.FORWARDED_HOST
                                        .asString()
                                        .toLowerCase()))
                .anyTimes();
        expect(
                        servletRequest.getHeader(
                                ProxifyingURLMangler.Headers.FORWARDED_PATH
                                        .asString()
                                        .toLowerCase()))
                .andReturn(
                        headers.get(
                                ProxifyingURLMangler.Headers.FORWARDED_PATH
                                        .asString()
                                        .toLowerCase()))
                .anyTimes();

        expect(servletRequest.getHeaderNames())
                .andReturn(Collections.enumeration(headers.keySet()))
                .anyTimes();
        replay(servletRequest);

        HTTPHeadersCollector filter = new HTTPHeadersCollector();
        filter.collectHeaders(servletRequest);
        GeoServer geoServer = createNiceMock(GeoServer.class);
        expect(geoServer.getGlobal()).andReturn(new GeoServerInfoImpl()).anyTimes();

        SettingsInfo settings = createNiceMock(SettingsInfo.class);
        expect(settings.getProxyBaseUrl()).andReturn(proxyBaseUrl).anyTimes();
        if (localWorkspaceSettingsEnabled) {
            expect(settings.isUseHeadersProxyURL())
                    .andReturn(useWorkspaceHeadersProxyUrl)
                    .anyTimes();
        } else {
            expect(settings.isUseHeadersProxyURL()).andReturn(useHeadersProxyURLIn).anyTimes();
        }
        replay(settings);

        expect(geoServer.getSettings()).andReturn(settings).anyTimes();
        replay(geoServer);

        ProxifyingURLMangler mangler = new ProxifyingURLMangler(geoServer);
        ApplicationContext appContext = createNiceMock(ApplicationContext.class);

        expect(appContext.getBeanNamesForType(URLMangler.class))
                .andReturn(new String[] {"mangler"});
        expect(appContext.getBean("mangler")).andReturn(mangler).anyTimes();
        replay(appContext);
        GeoServerExtensionsHelper.init(appContext);
        this.mangler = new ProxifyingURLMangler(geoServer);
    }

    @After
    public void clearAppContext() {
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testNullFlag() throws Exception {
        createAppContext(null, null, null, null, null, null, null, null, false, false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testNoProxyBaseURL() throws Exception {
        createAppContext(null, false, null, null, null, null, null, null, false, false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURL() throws Exception {
        createAppContext(
                "http://foo.org/geoserver",
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://foo.org/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetNoTemplate() throws Exception {
        createAppContext(
                "http://foo.org/geoserver", true, null, null, null, null, null, null, false, false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://foo.org/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateNoHeaders() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplatePartialHeaders() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                null,
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateEmptyBaseURL() throws Exception {
        createAppContext("", true, null, null, null, "example.com:8080", null, null, false, false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplate() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                null,
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateForLocalWorkspaceWithTemplate()
            throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                null,
                null,
                true,
                true);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateForLocalWorkspaceWithoutTemplate()
            throws Exception {
        createAppContext(
                "http://foo.org/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                null,
                null,
                true,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://foo.org/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateForwardedHost() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                "public",
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/public/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateHost() throws Exception {
        createAppContext(
                "http://${Host}/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                "example.com:8080",
                null,
                "public",
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/public/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateForwarded() throws Exception {
        createAppContext(
                "${Forwarded.proto}://${Forwarded.host}/geoserver",
                true,
                null,
                null,
                null,
                null,
                null,
                "for=192.0.2.60; proto=http; by=203.0.113.43; host=example.com:8080",
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateForwardedPath() throws Exception {
        createAppContext(
                "${Forwarded.proto}://${Forwarded.host}/${Forwarded.path}/geoserver",
                true,
                null,
                null,
                null,
                null,
                null,
                "proto=http; host=example.com:8080; path=public",
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/public/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTwoTemplates() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver"
                        + ProxifyingURLMangler.TEMPLATE_SEPARATOR
                        + "http://${X-Forwarded-Host}/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                "public",
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/public/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTwoTemplates2() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver"
                        + ProxifyingURLMangler.TEMPLATE_SEPARATOR
                        + "http://example.org/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                null,
                null,
                "public",
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.org/public/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateMixedCase() throws Exception {
        createAppContext(
                "http://${X-ForwarDED-HoST}/${x-forwarded-PATH}/geoserver",
                true,
                null,
                null,
                null,
                "example.com:8080",
                "public",
                null,
                false,
                false);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL, new StringBuilder(), new HashMap<>(), URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/public/geoserver", baseURL.toString());
    }
}
