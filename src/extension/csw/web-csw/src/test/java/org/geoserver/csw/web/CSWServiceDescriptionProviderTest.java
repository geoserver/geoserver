/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.CSWInfoImpl;
import org.geoserver.csw.WebCatalogService;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.Service;
import org.geoserver.web.ServiceDescription;
import org.geoserver.web.ServiceLinkDescription;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CSWServiceDescriptionProviderTest {

    private static final String SERVICE_TYPE = "CSW";

    private static final Version VERSION = new Version("2.0.2");

    private static final List<String> OPERATIONS = Collections.singletonList("GetCapabilities");

    private static final String TITLE = "Test CSW Title";

    private static final String DESCRIPTION = "Test CSW Abstract";

    @Mock private GeoServer geoserver;

    @InjectMocks private CSWServiceDescriptionProvider provider;

    @Mock private WebCatalogService csw;

    private AutoCloseable mocks;

    private GeoServerInfo global;

    private CSWInfo info;

    @Before
    public void setUp() {
        this.mocks = MockitoAnnotations.openMocks(this);
        this.global = new GeoServerInfoImpl();
        this.info = new CSWInfoImpl();
        this.info.setTitle(TITLE);
        this.info.setAbstract(DESCRIPTION);
        when(this.geoserver.getGlobal()).thenReturn(this.global);
        Service service = new Service(SERVICE_TYPE, this.csw, VERSION, OPERATIONS);
        GeoServerExtensionsHelper.singleton("cswService", service, Service.class);
    }

    @After
    public void tearDown() throws Exception {
        GeoServerExtensionsHelper.init(null);
        if (this.mocks != null) {
            this.mocks.close();
            this.mocks = null;
        }
    }

    @Test
    public void testGetServicesGlobalDisabled() {
        this.global.setGlobalServices(false);
        List<ServiceDescription> list = this.provider.getServices(null, null);
        assertThat(list, empty());
    }

    @Test
    public void testGetServicesGlobalEnabled() {
        when(this.geoserver.getService(CSWInfo.class)).thenReturn(this.info);
        List<ServiceDescription> list = this.provider.getServices(null, null);
        assertThat(list, hasSize(1));
        ServiceDescription actual = list.get(0);
        assertEquals(SERVICE_TYPE, actual.getServiceType());
        assertEquals(new GrowableInternationalString(TITLE), actual.getTitle());
        assertEquals(new GrowableInternationalString(DESCRIPTION), actual.getDescription());
        assertTrue(actual.isAvailable());
        assertFalse(actual.isAdmin());
        assertNull(actual.getWorkspace());
        assertNull(actual.getLayer());
    }

    @Test
    public void testGetServicesWithWorkspace() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ws.setName("foo");
        when(this.geoserver.getService(ws, CSWInfo.class)).thenReturn(this.info);
        List<ServiceDescription> list = this.provider.getServices(ws, null);
        assertThat(list, hasSize(1));
        ServiceDescription actual = list.get(0);
        assertEquals(SERVICE_TYPE, actual.getServiceType());
        assertEquals(new GrowableInternationalString(TITLE), actual.getTitle());
        assertEquals(new GrowableInternationalString(DESCRIPTION), actual.getDescription());
        assertTrue(actual.isAvailable());
        assertFalse(actual.isAdmin());
        assertEquals("foo", actual.getWorkspace());
        assertNull(actual.getLayer());
    }

    @Test
    public void testGetServiceLinksGlobalDisabled() {
        this.global.setGlobalServices(false);
        List<ServiceLinkDescription> list = this.provider.getServiceLinks(null, null);
        assertThat(list, empty());
    }

    @Test
    public void testGetServiceLinksGlobalEnabled() {
        List<ServiceLinkDescription> list = this.provider.getServiceLinks(null, null);
        assertThat(list, hasSize(1));
        ServiceLinkDescription actual = list.get(0);
        assertEquals(SERVICE_TYPE, actual.getServiceType());
        assertEquals(SERVICE_TYPE, actual.getProtocol());
        assertEquals(VERSION, actual.getVersion());
        assertEquals("../ows?service=CSW&version=2.0.2&request=GetCapabilities", actual.getLink());
        assertNull(actual.getWorkspace());
        assertNull(actual.getLayer());
    }

    @Test
    public void testGetServiceLinksWithWorkspace() {
        WorkspaceInfo ws = new WorkspaceInfoImpl();
        ws.setName("foo");
        List<ServiceLinkDescription> list = this.provider.getServiceLinks(ws, null);
        assertThat(list, hasSize(1));
        ServiceLinkDescription actual = list.get(0);
        assertEquals(SERVICE_TYPE, actual.getServiceType());
        assertEquals(SERVICE_TYPE, actual.getProtocol());
        assertEquals(VERSION, actual.getVersion());
        assertEquals(
                "../foo/ows?service=CSW&version=2.0.2&request=GetCapabilities", actual.getLink());
        assertEquals("foo", actual.getWorkspace());
        assertNull(actual.getLayer());
    }
}
