/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.util.Locale.CANADA_FRENCH;
import static java.util.Locale.ENGLISH;

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServicePersister;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.SecureCatalogImpl;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.Version;
import org.springframework.lang.Nullable;

/**
 * Support to configure some concrete {@link ServiceInfo} classes and xstream loaders for tests, since there're no
 * implementations in gs-main
 */
class DataDirectoryLoaderTestSupport {
    public static interface TestService1 extends ServiceInfo {

        @SuppressWarnings("serial")
        static class TestService1Impl extends ServiceInfoImpl implements TestService1 {
            @Override
            public boolean equals(Object o) {
                return o instanceof TestService1Impl && super.equals(o);
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass(), this);
            }
        }

        static final class Loader extends XStreamServiceLoader<TestService1> {

            public Loader(GeoServerResourceLoader resourceLoader) {
                super(resourceLoader, "service1");
            }

            @Override
            public Class<TestService1> getServiceClass() {
                return TestService1.class;
            }

            @Override
            protected TestService1 createServiceFromScratch(GeoServer gs) {
                return new TestService1Impl();
            }
        }
    }

    public static interface TestService2 extends ServiceInfo {

        @SuppressWarnings("serial")
        static class TestService2Impl extends ServiceInfoImpl implements TestService2 {
            @Override
            public boolean equals(Object o) {
                return o instanceof TestService2Impl && super.equals(o);
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass(), this);
            }
        }

        static final class Loader extends XStreamServiceLoader<TestService2> {

            public Loader(GeoServerResourceLoader resourceLoader) {
                super(resourceLoader, "service2");
            }

            @Override
            public Class<TestService2> getServiceClass() {
                return TestService2.class;
            }

            @Override
            protected TestService2 createServiceFromScratch(GeoServer gs) {
                return new TestService2Impl();
            }
        }
    }

    public TestService1.Loader serviceLoader1;
    public TestService2.Loader serviceLoader2;
    private Catalog catalog;

    public DataDirectoryLoaderTestSupport(Catalog catalog) {
        this.catalog = catalog;
    }

    public void setUpServiceLoaders(final GeoServer geoServer) {
        serviceLoader1 = new TestService1.Loader(geoServer.getCatalog().getResourceLoader());
        serviceLoader2 = new TestService2.Loader(geoServer.getCatalog().getResourceLoader());
        GeoServerExtensionsHelper.singleton("testServiceLoader1", serviceLoader1, XStreamServiceLoader.class);
        GeoServerExtensionsHelper.singleton("testServiceLoader2", serviceLoader2, XStreamServiceLoader.class);

        geoServer.removeListener(geoServer.getListeners().stream()
                .filter(ServicePersister.class::isInstance)
                .findFirst()
                .orElse(null));

        final List<XStreamServiceLoader<ServiceInfo>> loaders = findServiceLoaders();

        geoServer.addListener(new ServicePersister(loaders, geoServer));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<XStreamServiceLoader<ServiceInfo>> findServiceLoaders() {
        return (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);
    }

    public void tearDown() {
        GeoServerExtensionsHelper.clear();
    }

    public TestService1 serviceInfo1(WorkspaceInfo workspace, String name, GeoServer geoServer) {
        return serviceInfo(workspace, name, () -> serviceLoader1.create(geoServer));
    }

    public TestService2 serviceInfo2(WorkspaceInfo workspace, String name, GeoServer geoServer) {
        return serviceInfo(workspace, name, () -> serviceLoader2.create(geoServer));
    }

    public <S extends ServiceInfo> S serviceInfo(WorkspaceInfo workspace, String name, Supplier<S> factory) {
        S s = factory.get();
        String id = String.format("%s:%s-id", workspace == null ? null : workspace.getName(), name);
        OwsUtils.set(s, "id", id);
        s.setName(name);
        s.setWorkspace(workspace);
        s.setTitle(name + " Title");
        s.setAbstract(name + " Abstract");
        s.setInternationalTitle(
                internationalString(ENGLISH, name + " english title", CANADA_FRENCH, name + "titre anglais"));
        s.setInternationalAbstract(
                internationalString(ENGLISH, name + " english abstract", CANADA_FRENCH, name + "résumé anglais"));
        s.setAccessConstraints("NONE");
        s.setCiteCompliant(true);
        s.setEnabled(true);
        s.getExceptionFormats().add("fake-" + name + "-exception-format");
        s.setFees("NONE");
        s.setMaintainer("Claudious whatever");
        MetadataLinkInfoImpl metadataLink = new MetadataLinkInfoImpl();
        metadataLink.setAbout("about");
        metadataLink.setContent("content");
        metadataLink.setId("medatata-link-" + name);
        metadataLink.setMetadataType("fake");
        metadataLink.setType("void");
        s.setMetadataLink(metadataLink);
        s.setOnlineResource("http://geoserver.org/" + name);
        s.setOutputStrategy("SPEED");
        s.setSchemaBaseURL("file:data/" + name);
        s.setVerbose(true);
        List<Version> versions = Lists.newArrayList(new Version("1.0.0"), new Version("2.0.0"));
        s.getVersions().addAll(versions);
        return s;
    }

    private GrowableInternationalString internationalString(Locale l1, String val1, Locale l2, String val2) {
        GrowableInternationalString s = new GrowableInternationalString();
        s.add(l1, val1);
        s.add(l2, val2);
        return s;
    }

    public WorkspaceInfoImpl createWorkspace(String name) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo ws = factory.createWorkspace();
        ws = (WorkspaceInfo) SecureCatalogImpl.unwrap(ws);
        ws.setName(name);
        return (WorkspaceInfoImpl) ws;
    }

    public WorkspaceInfo addWorkspace(String name) {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo ws = createWorkspace(name);

        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(ws.getName());
        ns.setURI("http://" + name + ".test.com");
        catalog.add(ws);
        catalog.add(ns);
        return catalog.getWorkspaceByName(name);
    }

    public DataStoreInfoImpl createPostgisStore(WorkspaceInfo ws) {
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds = (DataStoreInfo) SecureCatalogImpl.unwrap(ds);
        ds.setWorkspace(ws);
        ds.setName("postgis");
        ds.setType(new PostgisNGDataStoreFactory().getDisplayName());
        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.getDefaultValue());
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "test");
        params.put(JDBCDataStoreFactory.USER.key, "test");
        params.put(JDBCDataStoreFactory.PASSWD.key, "s3cr3t");
        return (DataStoreInfoImpl) ds;
    }

    public SettingsInfo createSettings() {
        return createSettings(null);
    }

    public SettingsInfo createSettings(@Nullable WorkspaceInfo workspace) {
        SettingsInfo s = new SettingsInfoImpl();
        s.setWorkspace(workspace);
        String id = workspace == null ? "global-settings-id" : workspace.getName() + "-settings-id";
        OwsUtils.set(s, "id", id);

        s.setTitle(workspace == null ? "Global Settings" : workspace.getName() + " Settings");
        s.setCharset("UTF-8");
        s.setNumDecimals(9);
        s.setOnlineResource("http://geoserver.org");
        s.setProxyBaseUrl("http://test.geoserver.org");
        s.setSchemaBaseUrl("file:data/schemas");
        s.setVerbose(true);
        s.setVerboseExceptions(true);
        return s;
    }

    public LayerGroupInfoImpl createLayerGroup(String name, List<? extends PublishedInfo> layers) {
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg = (LayerGroupInfo) SecureCatalogImpl.unwrap(lg);
        lg.setName(name);
        lg.setTitle(name);
        lg.getLayers().addAll(layers);
        return (LayerGroupInfoImpl) lg;
    }

    public WMSStoreInfoImpl createWmsStore(WorkspaceInfo ws) {
        WMSStoreInfo wms = catalog.getFactory().createWebMapServer();
        wms = (WMSStoreInfo) SecureCatalogImpl.unwrap(wms);
        wms.setName("wms-store");
        wms.setWorkspace(ws);
        wms.setCapabilitiesURL("http://test.com/wms?");
        return (WMSStoreInfoImpl) wms;
    }
}
