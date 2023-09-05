/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.faker;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.DataLinkInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInfo.WebUIMode;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.JAIInfo.PngEncoderType;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.JAIEXTInfoImpl;
import org.geoserver.config.impl.JAIInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.api.coverage.SampleDimensionType;
import org.geotools.api.util.InternationalString;
import org.geotools.util.GrowableInternationalString;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.Version;
import org.springframework.util.Assert;

public class CatalogFaker {

    private final Faker faker;
    private Supplier<Catalog> catalog;
    private Supplier<GeoServer> geoserver;

    public CatalogFaker(/* @NonNull */ Catalog catalog, /* @NonNull */ GeoServer geoserver) {
        this(() -> catalog, () -> geoserver);
    }

    public CatalogFaker(
            /* @NonNull */ Catalog catalog, /* @NonNull */
            GeoServer geoserver, /* @NonNull */
            Locale locale) {
        this(() -> catalog, () -> geoserver, locale);
    }

    public CatalogFaker(Supplier<Catalog> catalog, Supplier<GeoServer> geoserver) {
        this(catalog, geoserver, Locale.ENGLISH);
    }

    public CatalogFaker(
            /* @NonNull */ Supplier<Catalog> catalog, /* @NonNull */
            Supplier<GeoServer> geoserver, /* @NonNull */
            Locale locale) {
        this.catalog = catalog;
        this.geoserver = geoserver;
        this.faker = new Faker(locale);
    }

    public Faker faker() {
        return this.faker;
    }

    public CatalogFaker italian() {
        return to(Locale.ITALIAN);
    }

    public CatalogFaker german() {
        return to(Locale.GERMAN);
    }

    public CatalogFaker to(/* @NonNull */ Locale locale) {
        return new CatalogFaker(catalog, geoserver, locale);
    }

    private Catalog catalog() {
        return catalog.get();
    }

    private CatalogFactory catalogFactory() {
        return catalog().getFactory();
    }

    public LayerGroupInfo layerGroupInfo(
            String id, WorkspaceInfo workspace, String name, PublishedInfo layer, StyleInfo style) {
        // not using factory cause SecuredCatalog would return SecuredLayerGroupInfo
        // which has no id
        // setter
        LayerGroupInfo lg = new LayerGroupInfoImpl();
        OwsUtils.set(lg, "id", id);
        lg.setName(name);
        lg.setWorkspace(workspace);
        lg.getLayers().add(layer);
        lg.getStyles().add(style);
        OwsUtils.resolveCollections(lg);
        return lg;
    }

    public LayerInfo layerInfo(ResourceInfo resource, StyleInfo defaultStyle) {

        return layerInfo(
                resource.getName() + "-layer-id",
                resource,
                resource.getName() + " title",
                true,
                defaultStyle);
    }

    public LayerInfo layerInfo(
            String id,
            ResourceInfo resource,
            String title,
            boolean enabled,
            StyleInfo defaultStyle,
            StyleInfo... additionalStyles) {
        LayerInfo lyr = catalogFactory().createLayer();
        OwsUtils.set(lyr, "id", id);
        lyr.setResource(resource);
        lyr.setEnabled(enabled);
        lyr.setDefaultStyle(defaultStyle);
        lyr.setTitle(title);
        for (int i = 0; null != additionalStyles && i < additionalStyles.length; i++) {
            lyr.getStyles().add(additionalStyles[i]);
        }
        OwsUtils.resolveCollections(lyr);
        return lyr;
    }

    public StyleInfo styleInfo() {
        return styleInfo(name());
    }

    public StyleInfo styleInfo(/* @NonNull */ String name) {
        return styleInfo(name, (WorkspaceInfo) null);
    }

    public StyleInfo styleInfo(/* @NonNull */ String name, WorkspaceInfo workspace) {
        return styleInfo(name + "-id", workspace, name, name + ".sld");
    }

    public StyleInfo styleInfo(String id, WorkspaceInfo workspace, String name, String fileName) {
        StyleInfo st = catalogFactory().createStyle();
        OwsUtils.set(st, "id", id);
        st.setWorkspace(workspace);
        st.setName(name);
        st.setFilename(fileName);
        OwsUtils.resolveCollections(st);
        return st;
    }

    public WMTSLayerInfo wmtsLayerInfo(
            String id, StoreInfo store, NamespaceInfo namespace, String name, boolean enabled) {
        WMTSLayerInfo wmtsl = catalogFactory().createWMTSLayer();
        OwsUtils.set(wmtsl, "id", id);
        wmtsl.setStore(store);
        wmtsl.setNamespace(namespace);
        wmtsl.setName(name);
        wmtsl.setEnabled(enabled);
        OwsUtils.resolveCollections(wmtsl);
        return wmtsl;
    }

    public WMTSStoreInfo wmtsStoreInfo(
            String id, WorkspaceInfo workspace, String name, String url, boolean enabled) {
        WMTSStoreInfo wmtss = catalogFactory().createWebMapTileServer();
        OwsUtils.set(wmtss, "id", id);
        wmtss.setWorkspace(workspace);
        wmtss.setName(name);
        wmtss.setType("WMTS");
        wmtss.setCapabilitiesURL(url);
        wmtss.setEnabled(enabled);
        OwsUtils.resolveCollections(wmtss);
        return wmtss;
    }

    public WMSLayerInfo wmsLayerInfo(
            String id, StoreInfo store, NamespaceInfo namespace, String name, boolean enabled) {
        WMSLayerInfo wmsl = catalogFactory().createWMSLayer();
        OwsUtils.set(wmsl, "id", id);
        wmsl.setStore(store);
        wmsl.setNamespace(namespace);
        wmsl.setName(name);
        wmsl.setEnabled(enabled);
        OwsUtils.resolveCollections(wmsl);
        return wmsl;
    }

    public WMSStoreInfo wmsStoreInfo(
            String id, WorkspaceInfo wspace, String name, String url, boolean enabled) {
        WMSStoreInfo wms = catalogFactory().createWebMapServer();
        OwsUtils.set(wms, "id", id);
        wms.setName(name);
        wms.setType("WMS");
        wms.setCapabilitiesURL(url);
        wms.setWorkspace(wspace);
        wms.setEnabled(enabled);
        OwsUtils.resolveCollections(wms);
        return wms;
    }

    public CoverageInfo coverageInfo(String id, CoverageStoreInfo cstore, String name) {
        CoverageInfo coverage = catalogFactory().createCoverage();
        OwsUtils.set(coverage, "id", id);
        coverage.setName(name);
        coverage.setStore(cstore);
        OwsUtils.resolveCollections(coverage);
        return coverage;
    }

    public CoverageStoreInfo coverageStoreInfo(
            String id, WorkspaceInfo ws, String name, String coverageType, String uri) {
        CoverageStoreInfo cstore = catalogFactory().createCoverageStore();
        OwsUtils.set(cstore, "id", id);
        cstore.setName(name);
        cstore.setType(coverageType);
        cstore.setURL(uri);
        cstore.setWorkspace(ws);
        OwsUtils.resolveCollections(cstore);
        return cstore;
    }

    public FeatureTypeInfo featureTypeInfo(DataStoreInfo ds) {
        String prefix = ds.getWorkspace().getName();
        NamespaceInfo ns = catalog().getNamespaceByPrefix(prefix);
        Objects.requireNonNull(ns, "Namespace " + prefix + " does not exist");

        String id = "FeatureType." + id();
        String name = name();
        String abstracT = faker().company().bs();
        String description = faker().company().buzzword();
        boolean enabled = true;
        return featureTypeInfo(id, ds, ns, name, abstracT, description, enabled);
    }

    public FeatureTypeInfo featureTypeInfo(
            String id,
            DataStoreInfo ds,
            NamespaceInfo ns,
            String name,
            String ftAbstract,
            String ftDescription,
            boolean enabled) {
        FeatureTypeInfo fttype = catalogFactory().createFeatureType();
        OwsUtils.set(fttype, "id", id);
        fttype.setEnabled(true);
        fttype.setName(name);
        fttype.setAbstract(ftAbstract);
        fttype.setDescription(ftDescription);
        fttype.setStore(ds);
        fttype.setNamespace(ns);
        OwsUtils.resolveCollections(fttype);
        return fttype;
    }

    public DataStoreInfo dataStoreInfo(WorkspaceInfo ws) {
        return dataStoreInfo(name(), ws);
    }

    public DataStoreInfo dataStoreInfo(String name, WorkspaceInfo ws) {
        return dataStoreInfo("DataStoreInfo." + id(), ws, name, name + " description", true);
    }

    public DataStoreInfo dataStoreInfo(
            String id, WorkspaceInfo ws, String name, String description, boolean enabled) {
        DataStoreInfoImpl dstore = (DataStoreInfoImpl) catalogFactory().createDataStore();
        OwsUtils.set(dstore, "id", id);
        dstore.setEnabled(enabled);
        dstore.setName(name);
        dstore.setDescription(description);
        dstore.setWorkspace(ws);
        dstore.setConnectionParameters(new HashMap<>());
        // note: using only string param values to avoid assertEquals() failures due to
        // serialization/deserialization losing type of parameter values
        dstore.getConnectionParameters().put("param1", "test value");
        dstore.getConnectionParameters().put("param2", "1000");
        OwsUtils.resolveCollections(dstore);
        return dstore;
    }

    public WorkspaceInfo workspaceInfo() {
        return workspaceInfo(name());
    }

    public String name() {
        return faker.internet().domainName() + "_" + faker.random().hex();
    }

    public WorkspaceInfo workspaceInfo(String name) {
        return workspaceInfo("WorkspaceInfo." + id(), name);
    }

    public WorkspaceInfo workspaceInfo(String id, String name) {
        WorkspaceInfo workspace = catalogFactory().createWorkspace();
        OwsUtils.set(workspace, "id", id);
        workspace.setName(name);
        OwsUtils.resolveCollections(workspace);
        return workspace;
    }

    public String url() {
        return faker().company().url() + "/" + faker.random().hex();
    }

    private String id() {
        return faker().idNumber().valid();
    }

    public NamespaceInfo namespace() {
        return namespace(id(), faker().letterify("ns-????"), url());
    }

    public NamespaceInfo namespace(String name) {
        return namespace(id(), name, url());
    }

    public NamespaceInfo namespace(String id, String name, String uri) {
        NamespaceInfo namesapce = catalogFactory().createNamespace();
        OwsUtils.set(namesapce, "id", id);
        namesapce.setPrefix(name);
        namesapce.setURI(uri);
        OwsUtils.resolveCollections(namesapce);
        return namesapce;
    }

    public GeoServerInfo geoServerInfo() {
        GeoServerInfoImpl g = new GeoServerInfoImpl();

        g.setId("GeoServer.global");
        g.setSettings(settingsInfo(null));
        g.setAdminPassword("geoserver");
        g.setAdminUsername("admin");
        g.setAllowStoredQueriesPerWorkspace(true);
        g.setCoverageAccess(createCoverageAccessInfo());
        g.setFeatureTypeCacheSize(1000);
        g.setGlobalServices(true);
        g.setId("GeoServer.global");
        g.setJAI(jaiInfo());
        // don't set lock provider to avoid a warning stack trace that the bean does not
        // exist
        // g.setLockProviderName("testLockProvider");
        g.setMetadata(metadataMap("k1", Integer.valueOf(1), "k2", "2", "k3", Boolean.FALSE));
        g.setResourceErrorHandling(ResourceErrorHandling.OGC_EXCEPTION_REPORT);
        g.setUpdateSequence(faker().random().nextLong(1000L));
        g.setWebUIMode(WebUIMode.DO_NOT_REDIRECT);
        g.setXmlExternalEntitiesEnabled(Boolean.TRUE);
        g.setXmlPostRequestLogBufferSize(1024);

        return g;
    }

    public MetadataMap metadataMap(Serializable... kvps) {
        Assert.isTrue(kvps == null || kvps.length % 2 == 0, "expected even number");
        MetadataMap m = new MetadataMap();
        if (kvps != null) {
            for (int i = 0; i < kvps.length; i += 2) {
                m.put((String) kvps[i], kvps[i + 1]);
            }
        }
        return m;
    }

    public JAIInfo jaiInfo() {
        JAIInfo jai = new JAIInfoImpl();
        jai.setAllowInterpolation(true);
        jai.setAllowNativeMosaic(true);
        jai.setAllowNativeWarp(true);

        JAIEXTInfoImpl jaiext = new JAIEXTInfoImpl();
        jaiext.setJAIEXTOperations(Collections.singleton("categorize"));
        jaiext.setJAIOperations(Collections.singleton("band"));
        jai.setJAIEXTInfo(jaiext);

        jai.setJpegAcceleration(true);
        jai.setMemoryCapacity(4096);
        jai.setMemoryThreshold(0.75);
        jai.setPngEncoderType(PngEncoderType.PNGJ);
        jai.setRecycling(true);
        jai.setTilePriority(1);
        jai.setTileThreads(7);
        return jai;
    }

    private CoverageAccessInfo createCoverageAccessInfo() {
        CoverageAccessInfoImpl c = new CoverageAccessInfoImpl();
        c.setCorePoolSize(9);
        c.setImageIOCacheThreshold(11);
        c.setKeepAliveTime(1000);
        c.setMaxPoolSize(18);
        c.setQueueType(QueueType.UNBOUNDED);
        return c;
    }

    public ContactInfo contactInfo() {
        ContactInfoImpl c = new ContactInfoImpl();

        Address fakeAddress = faker.address();

        c.setId(faker.idNumber().valid());
        c.setAddress(fakeAddress.fullAddress());
        c.setAddressCity(fakeAddress.cityName());
        c.setAddressCountry(fakeAddress.country());
        c.setAddressDeliveryPoint(fakeAddress.secondaryAddress());
        c.setAddressPostalCode(fakeAddress.zipCode());
        c.setAddressState(fakeAddress.state());

        // c.setContactEmail(faker.hacker().);
        c.setContactFacsimile(faker.phoneNumber().phoneNumber());
        c.setContactOrganization(faker.company().name());
        c.setContactPerson(faker.name().fullName());
        c.setContactVoice(faker.phoneNumber().cellPhone());
        c.setOnlineResource(faker.company().url());

        Address it = italian().faker().address();
        Address de = german().faker().address();
        c.setInternationalAddress(
                internationalString(
                        Locale.ITALIAN, it.fullAddress(), Locale.GERMAN, de.fullAddress()));
        return c;
    }

    public LoggingInfo loggingInfo() {
        LoggingInfoImpl l = new LoggingInfoImpl();
        l.setId("weird-this-has-id");
        l.setLevel("super");
        l.setLocation("there");
        l.setStdOutLogging(true);
        return l;
    }

    public SettingsInfo settingsInfo(WorkspaceInfo workspace) {
        SettingsInfo s = new SettingsInfoImpl();
        s.setWorkspace(workspace);
        String id = workspace == null ? "global-settings-id" : workspace.getName() + "-settings-id";
        OwsUtils.set(s, "id", id);

        s.setTitle(workspace == null ? "Global Settings" : workspace.getName() + " Settings");
        s.setCharset("UTF-8");
        s.setContact(contactInfo());
        s.getMetadata()
                .putAll(metadataMap("k1", Integer.valueOf(1), "k2", "2", "k3", Boolean.FALSE));
        s.setNumDecimals(9);
        s.setOnlineResource("http://geoserver.org");
        s.setProxyBaseUrl("http://test.geoserver.org");
        s.setSchemaBaseUrl("file:data/schemas");
        s.setVerbose(true);
        s.setVerboseExceptions(true);
        return s;
    }

    public <S extends ServiceInfo> S serviceInfo(
            WorkspaceInfo workspace, String serviceName, Supplier<S> factory) {
        S s = serviceInfo(serviceName, factory);
        s.setWorkspace(workspace);
        return s;
    }

    public <S extends ServiceInfo> S serviceInfo(String name, Supplier<S> factory) {
        S s = factory.get();
        OwsUtils.set(s, "id", name + "-" + this.id());
        s.setName(name);
        s.setTitle(name + " Title");
        s.setAbstract(name + " Abstract");
        s.setInternationalTitle(
                internationalString(
                        Locale.ENGLISH,
                        name + " english title",
                        Locale.CANADA_FRENCH,
                        name + "titre anglais"));
        s.setInternationalAbstract(
                internationalString(
                        Locale.ENGLISH,
                        name + " english abstract",
                        Locale.CANADA_FRENCH,
                        name + "résumé anglais"));
        s.setAccessConstraints("NONE");
        s.setCiteCompliant(true);
        s.setEnabled(true);
        s.getExceptionFormats().add("fake-" + name + "-exception-format");
        s.setFees("NONE");
        s.getKeywords().add(keywordInfo());
        s.setMaintainer("Claudious whatever");
        s.getMetadata().putAll(metadataMap(name, "something"));
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

    public KeywordInfo keywordInfo() {
        Keyword k1 = new Keyword(faker().chuckNorris().fact());
        k1.setLanguage("eng");
        k1.setVocabulary("watchit");
        return k1;
    }

    public AuthorityURLInfo authorityURLInfo() {
        AuthorityURL a1 = new AuthorityURL();
        a1.setHref(faker().company().url());
        a1.setName(faker().numerify("test-auth-url-####"));
        return a1;
    }

    public List<AuthorityURLInfo> authUrls(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> this.authorityURLInfo())
                .collect(Collectors.toList());
    }

    public InternationalString internationalString(String val) {
        return new SimpleInternationalString(val);
    }

    public GrowableInternationalString internationalString(Locale l, String val) {
        GrowableInternationalString s = new GrowableInternationalString();
        s.add(l, val);
        return s;
    }

    public GrowableInternationalString internationalString(
            Locale l1, String val1, Locale l2, String val2) {
        GrowableInternationalString s = new GrowableInternationalString();
        s.add(l1, val1);
        s.add(l2, val2);
        return s;
    }

    public AttributionInfo attributionInfo() throws Exception {
        AttributionInfoImpl attinfo = new AttributionInfoImpl();
        attinfo.setId(faker.idNumber().valid());
        attinfo.setHref(faker.company().url());
        attinfo.setLogoWidth(faker.number().numberBetween(128, 512));
        attinfo.setLogoHeight(faker.number().numberBetween(128, 512));
        attinfo.setTitle(faker.company().bs());
        return attinfo;
    }

    public MetadataLinkInfo metadataLink() {
        MetadataLinkInfoImpl link = new MetadataLinkInfoImpl();
        link.setId(faker().idNumber().valid());
        link.setAbout(faker().company().buzzword());
        link.setContent(faker().company().url());
        link.setMetadataType("metadataType");
        link.setType("type");
        return link;
    }

    public CoverageDimensionInfo coverageDimensionInfo() {
        CoverageDimensionImpl c = new CoverageDimensionImpl();
        c.setDescription(faker().company().bs());
        c.setDimensionType(SampleDimensionType.UNSIGNED_1BIT);
        c.setId(faker.idNumber().valid());
        c.setName(faker.internet().domainName());
        c.setNullValues(Lists.newArrayList(0.0)); // , Double.NEGATIVE_INFINITY,
        // Double.POSITIVE_INFINITY));
        c.setRange(NumberRange.create(0.0, 255.0));
        c.setUnit("unit");
        return c;
    }

    public DimensionInfo dimensionInfo() {
        DimensionInfoImpl di = new DimensionInfoImpl();
        di.setAcceptableInterval("searchRange");
        di.setAttribute("attribute");
        DimensionDefaultValueSetting defaultValue = new DimensionDefaultValueSetting();
        defaultValue.setReferenceValue("referenceValue");
        defaultValue.setStrategyType(Strategy.MAXIMUM);
        di.setDefaultValue(defaultValue);
        di.setEnabled(faker.bool().bool());
        di.setNearestMatchEnabled(faker.bool().bool());
        di.setResolution(BigDecimal.valueOf(faker.number().randomDouble(4, 0, 1000)));
        di.setUnits("metre");
        di.setUnitSymbol("m");
        di.setPresentation(DimensionPresentation.DISCRETE_INTERVAL);
        return di;
    }

    public DataLinkInfo dataLinkInfo() {
        DataLinkInfoImpl dl = new DataLinkInfoImpl();
        dl.setAbout(faker.yoda().quote());
        dl.setContent(faker.internet().url());
        dl.setId(faker.idNumber().valid());
        dl.setType(faker.internet().domainWord());
        return dl;
    }

    public LayerIdentifier layerIdentifierInfo() {
        org.geoserver.catalog.impl.LayerIdentifier li = new LayerIdentifier();
        li.setAuthority(faker.company().url());
        li.setIdentifier(faker.idNumber().valid());
        return li;
    }

    public LegendInfo legendInfo() {
        LegendInfoImpl l = new LegendInfoImpl();
        l.setFormat("image/png");
        l.setHeight(faker.number().numberBetween(10, 20));
        l.setWidth(faker.number().numberBetween(10, 20));
        l.setOnlineResource(faker.internet().url());
        l.setId(faker.idNumber().valid());
        return l;
    }
}
