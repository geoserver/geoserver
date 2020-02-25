/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.geoserver.data.test.CiteTestData.CDF_PREFIX;
import static org.geoserver.data.test.CiteTestData.CDF_TYPENAMES;
import static org.geoserver.data.test.CiteTestData.CDF_URI;
import static org.geoserver.data.test.CiteTestData.CGF_PREFIX;
import static org.geoserver.data.test.CiteTestData.CGF_TYPENAMES;
import static org.geoserver.data.test.CiteTestData.CGF_URI;
import static org.geoserver.data.test.CiteTestData.CITE_PREFIX;
import static org.geoserver.data.test.CiteTestData.CITE_TYPENAMES;
import static org.geoserver.data.test.CiteTestData.CITE_URI;
import static org.geoserver.data.test.CiteTestData.COVERAGES;
import static org.geoserver.data.test.CiteTestData.DEFAULT_PREFIX;
import static org.geoserver.data.test.CiteTestData.DEFAULT_RASTER_STYLE;
import static org.geoserver.data.test.CiteTestData.DEFAULT_URI;
import static org.geoserver.data.test.CiteTestData.DEFAULT_VECTOR_STYLE;
import static org.geoserver.data.test.CiteTestData.SF_PREFIX;
import static org.geoserver.data.test.CiteTestData.SF_TYPENAMES;
import static org.geoserver.data.test.CiteTestData.SF_URI;
import static org.geoserver.data.test.CiteTestData.WCS_PREFIX;
import static org.geoserver.data.test.CiteTestData.WCS_TYPENAMES;
import static org.geoserver.data.test.CiteTestData.WCS_URI;
import static org.geoserver.security.SecurityUtils.toBytes;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.data.test.MockCatalogBuilder.Callback;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerEmptyPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.springframework.context.ApplicationContext;

/**
 * Helper class used to creat mock objects during GeoServer testing.
 *
 * <p>Utility methods are provided to create many common configuration and resource access objects.
 */
public class MockCreator implements Callback {

    /**
     * Creates GeoServerResouceLoader around provided test data.
     *
     * <p>Created bean is registered with GeoServerExtensions as the singleton resourceLoader.
     *
     * @param testData Used to access base directory
     * @return GeoServerResourceLoader (registered with GeoServerExtensions)
     */
    public GeoServerResourceLoader createResourceLoader(MockTestData testData) throws Exception {
        File data = testData.getDataDirectoryRoot();
        GeoServerResourceLoader loader = new GeoServerResourceLoader(data);

        GeoServerExtensionsHelper.singleton("resourceLoader", loader); // treat as singleton

        return loader;
    }

    public Catalog createCatalog(MockTestData testData) throws Exception {
        GeoServerResourceLoader loader = createResourceLoader(testData);

        final Catalog catalog = createMock(Catalog.class);
        expect(catalog.getFactory()).andReturn(new CatalogFactoryImpl(catalog)).anyTimes();
        expect(catalog.getResourceLoader()).andReturn(loader).anyTimes();

        catalog.removeListeners((Class) EasyMock.anyObject());
        expectLastCall().anyTimes();

        catalog.addListener((CatalogListener) EasyMock.anyObject());
        expectLastCall().anyTimes();

        expect(catalog.getResourcePool())
                .andAnswer(
                        new IAnswer<ResourcePool>() {
                            @Override
                            public ResourcePool answer() throws Throwable {
                                return ResourcePool.create(catalog);
                            }
                        })
                .anyTimes();
        MockCatalogBuilder b = new MockCatalogBuilder(catalog, loader.getBaseDirectory());
        b.setCallback(this);

        b.style(DEFAULT_VECTOR_STYLE);
        b.style("generic");

        createWorkspace(DEFAULT_PREFIX, DEFAULT_URI, null, b);
        createWorkspace(CGF_PREFIX, CGF_URI, CGF_TYPENAMES, b);
        createWorkspace(CDF_PREFIX, CDF_URI, CDF_TYPENAMES, b);
        createWorkspace(SF_PREFIX, SF_URI, SF_TYPENAMES, b);
        createWorkspace(CITE_PREFIX, CITE_URI, CITE_TYPENAMES, b);

        if (testData.isInludeRaster()) {
            b.style(DEFAULT_RASTER_STYLE);

            createWorkspace(WCS_PREFIX, WCS_URI, null, WCS_TYPENAMES, b);
        }

        addToCatalog(catalog, b);
        b.commit();
        return catalog;
    }

    protected void addToCatalog(Catalog catalog, MockCatalogBuilder b) {}

    void createWorkspace(String wsName, String nsURI, QName[] typeNames, MockCatalogBuilder b) {
        createWorkspace(wsName, nsURI, typeNames, null, b);
    }

    void createWorkspace(
            String wsName,
            String nsURI,
            QName[] ftTypeNames,
            QName[] covTypeNames,
            MockCatalogBuilder b) {
        b.workspace(wsName, nsURI);

        if (ftTypeNames != null && ftTypeNames.length > 0) {
            b.dataStore(wsName);
            for (QName typeName : ftTypeNames) {
                String local = typeName.getLocalPart();
                b.style(local);
                b.featureType(local);
            }
            b.commit().commit();
        }
        if (covTypeNames != null && covTypeNames.length > 0) {
            for (QName typeName : covTypeNames) {
                String local = typeName.getLocalPart();
                String[] fileNameAndFormat = COVERAGES.get(typeName);

                b.coverageStore(local, fileNameAndFormat[0], fileNameAndFormat[1]);
                b.coverage(typeName, fileNameAndFormat[0], null, null);
                b.commit();
            }
            b.commit();
        }
    }

    @Override
    public void onWorkspace(String name, WorkspaceInfo ws, MockCatalogBuilder b) {}

    @Override
    public void onStore(String name, StoreInfo st, WorkspaceInfo ws, MockCatalogBuilder b) {}

    @Override
    public void onResource(String name, ResourceInfo r, StoreInfo s, MockCatalogBuilder b) {}

    @Override
    public void onLayer(String name, LayerInfo l, MockCatalogBuilder b) {}

    @Override
    public void onStyle(String name, StyleInfo s, MockCatalogBuilder b) {}

    @Override
    public void onLayerGroup(String name, LayerGroupInfo lg, MockCatalogBuilder b) {}

    public GeoServerSecurityManager createSecurityManager(MockTestData testData) throws Exception {
        final GeoServerSecurityManager secMgr = createNiceMock(GeoServerSecurityManager.class);

        // application context
        ApplicationContext appContext = createNiceMock(ApplicationContext.class);
        expect(secMgr.getApplicationContext()).andReturn(appContext).anyTimes();

        // master password provider
        MasterPasswordProvider masterPasswdProvider = createNiceMock(MasterPasswordProvider.class);
        expect(masterPasswdProvider.getName())
                .andReturn(MasterPasswordProvider.DEFAULT_NAME)
                .anyTimes();
        expect(secMgr.listMasterPasswordProviders())
                .andReturn(new TreeSet<String>(Arrays.asList(MasterPasswordProvider.DEFAULT_NAME)))
                .anyTimes();

        final File mrPwdFolder = new File(testData.getDataDirectoryRoot(), "master-pwd");
        expect(secMgr.masterPasswordProvider()).andReturn(Files.asResource(mrPwdFolder)).anyTimes();

        // password validators
        PasswordValidator passwdValidator = createNiceMock(PasswordValidator.class);
        expect(secMgr.loadPasswordValidator(PasswordValidator.DEFAULT_NAME))
                .andReturn(passwdValidator)
                .anyTimes();

        PasswordPolicyConfig masterPasswdPolicyConfig = createNiceMock(PasswordPolicyConfig.class);
        expect(masterPasswdPolicyConfig.getMinLength()).andReturn(8).anyTimes();
        expect(masterPasswdPolicyConfig.getMaxLength()).andReturn(-1).anyTimes();

        PasswordValidatorImpl masterPasswdValidator = new PasswordValidatorImpl(secMgr);
        masterPasswdValidator.setConfig(masterPasswdPolicyConfig);

        expect(secMgr.loadPasswordValidator(PasswordValidator.MASTERPASSWORD_NAME))
                .andReturn(masterPasswdValidator)
                .anyTimes();
        expect(secMgr.listPasswordValidators())
                .andReturn(
                        new TreeSet<String>(
                                Arrays.asList(
                                        PasswordValidator.DEFAULT_NAME,
                                        PasswordValidator.MASTERPASSWORD_NAME)))
                .anyTimes();
        ;

        // default user group store
        GeoServerUserGroupStore ugStore =
                createUserGroupStore(XMLUserGroupService.DEFAULT_NAME, secMgr);
        expect(secMgr.listUserGroupServices())
                .andReturn(new TreeSet<String>(Arrays.asList(XMLUserGroupService.DEFAULT_NAME)))
                .anyTimes();

        SecurityUserGroupServiceConfig ugConfig =
                createNiceMock(SecurityUserGroupServiceConfig.class);
        expect(ugConfig.getName()).andReturn(XMLUserGroupService.DEFAULT_NAME).anyTimes();
        expect(ugConfig.getPasswordPolicyName())
                .andReturn(PasswordValidator.DEFAULT_NAME)
                .anyTimes();
        expect(secMgr.loadUserGroupServiceConfig(XMLUserGroupService.DEFAULT_NAME))
                .andReturn(ugConfig)
                .anyTimes();

        // default role store
        GeoServerRoleStore roleStore = createRoleStore(XMLRoleService.DEFAULT_NAME, secMgr);
        expect(secMgr.listRoleServices())
                .andReturn(new TreeSet<String>(Arrays.asList(XMLRoleService.DEFAULT_NAME)))
                .anyTimes();
        expect(secMgr.getActiveRoleService()).andReturn(roleStore).anyTimes();

        // auth providers
        SecurityAuthProviderConfig authProviderConfig =
                createNiceMock(SecurityAuthProviderConfig.class);
        expect(authProviderConfig.getName())
                .andReturn(GeoServerAuthenticationProvider.DEFAULT_NAME)
                .anyTimes();
        expect(authProviderConfig.getUserGroupServiceName())
                .andReturn(XMLUserGroupService.DEFAULT_NAME)
                .anyTimes();
        expect(
                        secMgr.loadAuthenticationProviderConfig(
                                GeoServerAuthenticationProvider.DEFAULT_NAME))
                .andReturn(authProviderConfig)
                .anyTimes();

        GeoServerAuthenticationProvider authProvider =
                createNiceMock(GeoServerAuthenticationProvider.class);
        expect(authProvider.getName())
                .andReturn(GeoServerAuthenticationProvider.DEFAULT_NAME)
                .anyTimes();
        expect(secMgr.loadAuthenticationProvider(GeoServerAuthenticationProvider.DEFAULT_NAME))
                .andReturn(authProvider)
                .anyTimes();
        expect(secMgr.listAuthenticationProviders())
                .andReturn(
                        new TreeSet<String>(
                                Arrays.asList(GeoServerAuthenticationProvider.DEFAULT_NAME)))
                .anyTimes();
        expect(secMgr.getAuthenticationProviders())
                .andReturn(Arrays.asList(authProvider))
                .anyTimes();

        // security filters
        SecurityInterceptorFilterConfig filterConfig =
                createNiceMock(SecurityInterceptorFilterConfig.class);
        expect(secMgr.loadFilterConfig(GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR))
                .andReturn(filterConfig)
                .anyTimes();

        GeoServerAnonymousAuthenticationFilter authFilter =
                createNiceMock(GeoServerAnonymousAuthenticationFilter.class);
        expect(authFilter.applicableForServices()).andReturn(true).anyTimes();
        expect(authFilter.applicableForHtml()).andReturn(true).anyTimes();
        expect(secMgr.loadFilter(GeoServerSecurityFilterChain.ANONYMOUS_FILTER))
                .andReturn(authFilter)
                .anyTimes();

        GeoServerRoleFilter roleFilter = createNiceMock(GeoServerRoleFilter.class);
        expect(secMgr.loadFilter(GeoServerSecurityFilterChain.ROLE_FILTER))
                .andReturn(roleFilter)
                .anyTimes();

        GeoServerUserNamePasswordAuthenticationFilter formFilter =
                createNiceMock(GeoServerUserNamePasswordAuthenticationFilter.class);
        expect(formFilter.applicableForHtml()).andReturn(true).anyTimes();
        expect(secMgr.loadFilter(GeoServerSecurityFilterChain.FORM_LOGIN_FILTER))
                .andReturn(formFilter)
                .anyTimes();

        GeoServerBasicAuthenticationFilter basicFilter =
                createNiceMock(GeoServerBasicAuthenticationFilter.class);
        expect(basicFilter.applicableForServices()).andReturn(true).anyTimes();
        expect(secMgr.loadFilter(GeoServerSecurityFilterChain.BASIC_AUTH_FILTER))
                .andReturn(basicFilter)
                .anyTimes();

        // password encoders
        expect(secMgr.loadPasswordEncoder(GeoServerEmptyPasswordEncoder.class))
                .andAnswer(
                        new IAnswer<GeoServerEmptyPasswordEncoder>() {
                            @Override
                            public GeoServerEmptyPasswordEncoder answer() throws Throwable {
                                return createEmptyPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder("emptyPasswordEncoder"))
                .andAnswer(
                        new IAnswer<GeoServerPasswordEncoder>() {
                            @Override
                            public GeoServerPasswordEncoder answer() throws Throwable {
                                return createEmptyPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class))
                .andAnswer(
                        new IAnswer<GeoServerPlainTextPasswordEncoder>() {
                            @Override
                            public GeoServerPlainTextPasswordEncoder answer() throws Throwable {
                                return createPlainTextPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder("plainTextPasswordEncoder"))
                .andAnswer(
                        new IAnswer<GeoServerPasswordEncoder>() {
                            @Override
                            public GeoServerPasswordEncoder answer() throws Throwable {
                                return createPlainTextPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();

        expect(secMgr.loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false))
                .andAnswer(
                        new IAnswer<GeoServerPBEPasswordEncoder>() {
                            @Override
                            public GeoServerPBEPasswordEncoder answer() throws Throwable {
                                return createPbePasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder("pbePasswordEncoder"))
                .andAnswer(
                        new IAnswer<GeoServerPasswordEncoder>() {
                            @Override
                            public GeoServerPasswordEncoder answer() throws Throwable {
                                return createPbePasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();

        expect(secMgr.loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, true))
                .andAnswer(
                        new IAnswer<GeoServerPBEPasswordEncoder>() {
                            @Override
                            public GeoServerPBEPasswordEncoder answer() throws Throwable {
                                return createStrongPbePasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder("strongPbePasswordEncoder"))
                .andAnswer(
                        new IAnswer<GeoServerPasswordEncoder>() {
                            @Override
                            public GeoServerPasswordEncoder answer() throws Throwable {
                                return createStrongPbePasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder(GeoServerDigestPasswordEncoder.class, null, true))
                .andAnswer(
                        new IAnswer<GeoServerDigestPasswordEncoder>() {
                            @Override
                            public GeoServerDigestPasswordEncoder answer() throws Throwable {
                                return createDigestPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder(GeoServerDigestPasswordEncoder.class))
                .andAnswer(
                        new IAnswer<GeoServerDigestPasswordEncoder>() {
                            @Override
                            public GeoServerDigestPasswordEncoder answer() throws Throwable {
                                return createDigestPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoder("digestPasswordEncoder"))
                .andAnswer(
                        new IAnswer<GeoServerPasswordEncoder>() {
                            @Override
                            public GeoServerPasswordEncoder answer() throws Throwable {
                                return createDigestPasswordEncoder(secMgr);
                            }
                        })
                .anyTimes();
        expect(secMgr.loadPasswordEncoders())
                .andAnswer(
                        new IAnswer<List<GeoServerPasswordEncoder>>() {
                            @Override
                            public List<GeoServerPasswordEncoder> answer() throws Throwable {
                                return (List)
                                        Arrays.asList(
                                                createEmptyPasswordEncoder(secMgr),
                                                createPlainTextPasswordEncoder(secMgr),
                                                createPbePasswordEncoder(secMgr),
                                                createStrongPbePasswordEncoder(secMgr),
                                                createDigestPasswordEncoder(secMgr));
                            }
                        })
                .anyTimes();

        // keystore provider
        KeyStoreProvider keyStoreProvider = createNiceMock(KeyStoreProvider.class);
        expect(keyStoreProvider.isKeyStorePassword(aryEq("geoserver".toCharArray())))
                .andReturn(true)
                .anyTimes();
        expect(keyStoreProvider.containsAlias(KeyStoreProviderImpl.CONFIGPASSWORDKEY))
                .andReturn(true)
                .anyTimes();
        ;
        expect(keyStoreProvider.getSecretKey(KeyStoreProviderImpl.CONFIGPASSWORDKEY))
                .andReturn(new SecretKeySpec(toBytes("geoserver".toCharArray()), "PBE"))
                .anyTimes();
        expect(keyStoreProvider.hasUserGroupKey(XMLUserGroupService.DEFAULT_NAME))
                .andReturn(true)
                .anyTimes();

        String alias = "ugServiceAlias";
        expect(keyStoreProvider.aliasForGroupService(XMLUserGroupService.DEFAULT_NAME))
                .andReturn(alias)
                .anyTimes();
        expect(keyStoreProvider.containsAlias(alias)).andReturn(true).anyTimes();
        ;
        expect(keyStoreProvider.getSecretKey(alias))
                .andReturn(new SecretKeySpec(toBytes("geoserver".toCharArray()), "PBE"))
                .anyTimes();
        expect(secMgr.getKeyStoreProvider()).andReturn(keyStoreProvider).anyTimes();

        replay(
                keyStoreProvider,
                masterPasswdProvider,
                ugStore,
                ugConfig,
                roleStore,
                authProvider,
                authProviderConfig,
                filterConfig,
                passwdValidator,
                masterPasswdPolicyConfig,
                appContext,
                secMgr,
                roleFilter,
                formFilter,
                authFilter,
                basicFilter);
        return secMgr;
    }

    protected GeoServerEmptyPasswordEncoder createEmptyPasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {
        GeoServerEmptyPasswordEncoder emptyPwe = new GeoServerEmptyPasswordEncoder();
        emptyPwe.setBeanName("emptyPasswordEncoder");
        emptyPwe.setPrefix("empty");
        return emptyPwe;
    }

    protected GeoServerDigestPasswordEncoder createDigestPasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {
        GeoServerDigestPasswordEncoder digestPwe = new GeoServerDigestPasswordEncoder();
        digestPwe.setBeanName("digestPasswordEncoder");
        digestPwe.setPrefix("digest1");
        return digestPwe;
    }

    protected GeoServerPBEPasswordEncoder createStrongPbePasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {
        GeoServerPBEPasswordEncoder strongPbePwe = new GeoServerPBEPasswordEncoder();
        strongPbePwe.setBeanName("strongPbePasswordEncoder");
        strongPbePwe.setPrefix("crypt2");
        strongPbePwe.setProviderName("BC");
        strongPbePwe.setAvailableWithoutStrongCryptogaphy(false);
        strongPbePwe.initialize(secMgr);
        return strongPbePwe;
    }

    protected GeoServerPBEPasswordEncoder createPbePasswordEncoder(GeoServerSecurityManager secMgr)
            throws IOException {
        GeoServerPBEPasswordEncoder pbePwe = new GeoServerPBEPasswordEncoder();
        pbePwe.setBeanName("pbePasswordEncoder");
        pbePwe.setPrefix("crypt1");
        pbePwe.setAlgorithm("PBEWITHMD5ANDDES");
        pbePwe.initialize(secMgr);
        return pbePwe;
    }

    protected GeoServerPlainTextPasswordEncoder createPlainTextPasswordEncoder(
            GeoServerSecurityManager secMgr) throws IOException {

        GeoServerPlainTextPasswordEncoder plainPwe = new GeoServerPlainTextPasswordEncoder();
        plainPwe.setBeanName("plainTextPasswordEncoder");
        plainPwe.setPrefix("plain");
        plainPwe.initialize(secMgr);
        return plainPwe;
    }

    protected GeoServerUserGroupStore createUserGroupStore(
            String name, GeoServerSecurityManager secMgr) throws IOException {
        GeoServerUserGroupStore ugStore = createNiceMock(GeoServerUserGroupStore.class);
        expect(ugStore.getName()).andReturn(name).anyTimes();

        expect(secMgr.loadUserGroupService(name)).andReturn(ugStore).anyTimes();
        return ugStore;
    }

    protected void addUsers(GeoServerUserGroupStore ugStore, String... up) throws IOException {
        for (int i = 0; i < up.length; i += 2) {
            GeoServerUser user = new GeoServerUser(up[i]);
            user.setPassword(up[i + 1]);

            expect(ugStore.getUserByUsername(up[i])).andReturn(user).anyTimes();
        }
    }

    protected void addGroups(GeoServerUserGroupStore ugStore, String... groupNames)
            throws IOException {
        for (String groupName : groupNames) {
            GeoServerUserGroup grp = new GeoServerUserGroup(groupName);
            expect(ugStore.getGroupByGroupname(groupName)).andReturn(grp).anyTimes();
        }
    }

    protected GeoServerRoleStore createRoleStore(
            String name, GeoServerSecurityManager secMgr, String... roleNames) throws IOException {

        GeoServerRoleStore roleStore = createNiceMock(GeoServerRoleStore.class);
        expect(roleStore.getSecurityManager()).andReturn(secMgr).anyTimes();
        expect(roleStore.getName()).andReturn(name).anyTimes();

        for (String roleName : roleNames) {
            expect(roleStore.getRoleByName(roleName))
                    .andReturn(new GeoServerRole(roleName))
                    .anyTimes();
        }

        for (GeoServerRole role : GeoServerRole.SystemRoles) {
            String roleName = role.getAuthority();
            expect(roleStore.createRoleObject(roleName))
                    .andReturn(new GeoServerRole(roleName))
                    .anyTimes();
        }

        expect(secMgr.loadRoleService(name)).andReturn(roleStore).anyTimes();
        return roleStore;
    }

    protected void addRolesToCreate(GeoServerRoleStore roleStore, String... roleNames)
            throws IOException {
        for (String roleName : roleNames) {
            expect(roleStore.createRoleObject(roleName))
                    .andReturn(new GeoServerRole(roleName))
                    .anyTimes();
        }
    }
}
