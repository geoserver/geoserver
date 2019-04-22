/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.xml;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Collections;
import org.easymock.EasyMock;
import org.eclipse.emf.common.util.URI;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.util.PropertyRule;
import org.geoserver.wfs.xml.WFSURIHandler.InitStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class WFSURIHandlerTest {

    @Rule
    public PropertyRule aliases =
            PropertyRule.system("org.geoserver.wfs.xml.WFSURIHandler.additionalHostnames");

    InitStrategy strategy;

    private GeoServer gs;

    private GeoServerInfo config;
    private SettingsInfo settings;

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        WFSURIHandler.ADDITIONAL_HOSTNAMES.clear();
        WFSURIHandler.ADDRESSES.clear();

        // Suppress the network interface interrogation so it doesn't interfere with other tests
        strategy =
                new InitStrategy() {
                    public Collection<NetworkInterface> getNetworkInterfaces() {
                        return Collections.emptyList();
                    }
                };

        gs = EasyMock.createMock(GeoServer.class);
        config = EasyMock.createMock(GeoServerInfo.class);
        settings = EasyMock.createMock(SettingsInfo.class);
        expect(gs.getGlobal()).andStubReturn(config);
        expect(settings.getProxyBaseUrl()).andStubReturn(null);
        expect(config.getSettings()).andStubReturn(settings);
        replay(gs, settings, config);
    }

    @After
    public void tearDown() {
        verify(gs, config);
    }

    @Test
    @Ignore
    public void testFromNetworkInterfaces() {
        // Was unable to mock NetworkInterfaces
    }

    @Test
    public void testDefaultAliases() {

        WFSURIHandler.init(strategy);

        assertThat(WFSURIHandler.ADDRESSES, empty());
        assertThat(WFSURIHandler.ADDITIONAL_HOSTNAMES, contains("localhost"));
    }

    @Test
    public void testOverrideAliasesComma() {

        aliases.setValue("foo,bar , baz");

        WFSURIHandler.init(strategy);

        assertThat(WFSURIHandler.ADDRESSES, empty());
        assertThat(WFSURIHandler.ADDITIONAL_HOSTNAMES, containsInAnyOrder("foo", "bar", "baz"));
    }

    @Test
    public void testOverrideAliasesSpace() {

        aliases.setValue("foo bar  baz ");

        WFSURIHandler.init(strategy);

        assertThat(WFSURIHandler.ADDRESSES, empty());
        assertThat(WFSURIHandler.ADDITIONAL_HOSTNAMES, containsInAnyOrder("foo", "bar", "baz"));
    }

    protected void setProxyBase(String url) {
        reset(settings);
        expect(settings.getProxyBaseUrl()).andStubReturn(url);
        replay(settings);
    }

    @Test
    public void testRecognizeReflexiveSimple() {

        WFSURIHandler.init(strategy);

        WFSURIHandler handler = new WFSURIHandler(gs);

        final URI wrongHost =
                URI.createURI(
                        "http://example.com/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI notDFT =
                URI.createURI(
                        "http://localhost/geoserver/wfs?service=wfs&version=2.0.0&request=GetCapabilities");
        final URI localDFT =
                URI.createURI(
                        "http://localhost/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        assertThat(handler.canHandle(wrongHost), is(false));
        assertThat(handler.canHandle(notDFT), is(false));
        assertThat(handler.canHandle(localDFT), is(true));
    }

    @Test
    public void testRecognizeReflexiveUserAliases() {

        aliases.setValue("foo bar baz");

        WFSURIHandler.init(strategy);

        WFSURIHandler handler = new WFSURIHandler(gs);

        final URI wrongHost =
                URI.createURI(
                        "http://example.com/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI notDFT =
                URI.createURI(
                        "http://foo/geoserver/wfs?service=wfs&version=2.0.0&request=GetCapabilities");
        final URI fooDFT =
                URI.createURI(
                        "http://foo/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI barDFT =
                URI.createURI(
                        "http://bar/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI bazDFT =
                URI.createURI(
                        "http://baz/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI localhostDFT =
                URI.createURI(
                        "http://localhost/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        assertThat(handler.canHandle(wrongHost), is(false));
        assertThat(handler.canHandle(notDFT), is(false));
        assertThat(handler.canHandle(fooDFT), is(true));
        assertThat(handler.canHandle(barDFT), is(true));
        assertThat(handler.canHandle(bazDFT), is(true));
        assertThat(handler.canHandle(localhostDFT), is(false));
    }

    @Test
    public void testRecognizeReflexiveProxy() {

        this.setProxyBase("http://foo/geoserver");

        WFSURIHandler.init(strategy);

        WFSURIHandler handler = new WFSURIHandler(gs);

        final URI wrongHost =
                URI.createURI(
                        "http://example.com/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI notDFT =
                URI.createURI(
                        "http://foo/geoserver/wfs?service=wfs&version=2.0.0&request=GetCapabilities");
        final URI fooDFT =
                URI.createURI(
                        "http://foo/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        final URI uppercaseFooDFT =
                URI.createURI(
                        "http://FOO/geoserver/wfs?service=wfs&version=2.0.0&request=DescribeFeatureType");
        assertThat(handler.canHandle(wrongHost), is(false));
        assertThat(handler.canHandle(notDFT), is(false));
        assertThat(handler.canHandle(fooDFT), is(true));
        assertThat(handler.canHandle(uppercaseFooDFT), is(true));
    }
}
