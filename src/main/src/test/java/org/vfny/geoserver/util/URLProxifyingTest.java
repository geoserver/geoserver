/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.ProxifyingURLMangler;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

public class URLProxifyingTest {

    ProxifyingURLMangler mangler;

    void createAppContext(
            String proxyBaseUrl,
            Boolean useHeadersProxyURLIn,
            String forwardedHeader,
            String forwardedProtoHeader,
            String forwardedHost,
            String forwardedPath) {

        Request request = createNiceMock(Request.class);
        expect(request.getHttpRequest())
                .andReturn(
                        new HttpServletRequestWrapper(new MockHttpServletRequest()) {
                            public String getHeader(String name) {
                                switch (name) {
                                    case ProxifyingURLMangler.FORWARDED_HEADER:
                                        return forwardedHeader;
                                    case ProxifyingURLMangler.FORWARDED_PROTO_HEADER:
                                        return forwardedProtoHeader;
                                    case ProxifyingURLMangler.FORWARDED_HOST_HEADER:
                                        return forwardedHost;
                                    case ProxifyingURLMangler.FORWARDED_PATH_HEADER:
                                        return forwardedPath;
                                    case ProxifyingURLMangler.HOST_HEADER:
                                        return forwardedHost;
                                }
                                return null;
                            }
                        })
                .anyTimes();
        replay(request);
        Dispatcher.REQUEST.set(request);

        GeoServer geoServer = createNiceMock(GeoServer.class);
        expect(geoServer.getGlobal())
                .andReturn(
                        new GeoServerInfoImpl() {
                            @Override
                            public Boolean isUseHeadersProxyURL() {
                                return useHeadersProxyURLIn;
                            }
                        })
                .anyTimes();

        SettingsInfo settings = createNiceMock(SettingsInfo.class);
        expect(settings.getProxyBaseUrl()).andReturn(proxyBaseUrl).anyTimes();
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
    public void testNoProxyBaseURL() throws Exception {
        createAppContext(null, false, null, null, null, null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURL() throws Exception {
        createAppContext("http://foo.org/geoserver", false, null, null, null, null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("http://foo.org/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetNoTemplate() throws Exception {
        createAppContext("http://foo.org/geoserver", true, null, null, null, null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
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
                null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplatePartialHeaders() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                "example.com:8080",
                null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateEmptyBaseURL() throws Exception {
        createAppContext("", true, null, null, "example.com:8080", null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplate() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/geoserver", true, null, null, "example.com:8080", null);
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplate2() throws Exception {
        createAppContext(
                "http://${X-Forwarded-Host}/${X-Forwarded-Path}/geoserver",
                true,
                null,
                null,
                "example.com:8080",
                "public");
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
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
                "example.com:8080",
                "public");
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
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
                "public");
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("http://example.org/public/geoserver", baseURL.toString());
    }

    @Test
    public void testProxyBaseURLFlagSetWithTemplateMixedCase() throws Exception {
        createAppContext(
                "http://${X-ForwarDED-HoST}/${x-forwarded-PATH}/geoserver",
                true,
                null,
                null,
                "example.com:8080",
                "public");
        StringBuilder baseURL = new StringBuilder();
        this.mangler.mangleURL(
                baseURL,
                new StringBuilder(),
                new HashMap<String, String>(),
                URLMangler.URLType.SERVICE);
        assertEquals("http://example.com:8080/public/geoserver", baseURL.toString());
    }
}
