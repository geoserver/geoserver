/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.geoserver.config.util.XStreamUtils.xStreamPersist;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.server.UID;
import java.security.InvalidKeyException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.security.auth.AuthenticationCache;
import org.geoserver.security.auth.GeoServerRootAuthenticationProvider;
import org.geoserver.security.auth.GuavaAuthenticationCacheImpl;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.concurrent.LockingKeyStoreProvider;
import org.geoserver.security.concurrent.LockingRoleService;
import org.geoserver.security.concurrent.LockingUserGroupService;
import org.geoserver.security.config.AnonymousAuthenticationFilterConfig;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.ExceptionTranslationFilterConfig;
import org.geoserver.security.config.FileBasedSecurityServiceConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RememberMeAuthenticationFilterConfig;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SSLFilterConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityConfig;
import org.geoserver.security.config.SecurityContextPersistenceFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.file.FileWatcher;
import org.geoserver.security.file.RoleFileWatcher;
import org.geoserver.security.file.UserGroupFileWatcher;
import org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.filter.GeoServerExceptionTranslationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerRememberMeAuthenticationFilter;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.filter.GeoServerSSLFilter;
import org.geoserver.security.filter.GeoServerSecurityContextPersistenceFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.GroupAdminProperty;
import org.geoserver.security.impl.RESTAccessRuleDAO;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.impl.Util;
import org.geoserver.security.password.ConfigurationPasswordEncryptionHelper;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.MasterPasswordChangeRequest;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.password.RandomPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;
import org.geoserver.security.rememberme.GeoServerTokenBasedRememberMeServices;
import org.geoserver.security.rememberme.RememberMeServicesConfig;
import org.geoserver.security.validation.MasterPasswordChangeException;
import org.geoserver.security.validation.MasterPasswordChangeValidator;
import org.geoserver.security.validation.MasterPasswordConfigValidator;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.xml.XMLConstants;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLRoleServiceConfig;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.security.xml.XMLUserGroupServiceConfig;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.memory.UserAttribute;
import org.springframework.security.core.userdetails.memory.UserAttributeEditor;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.util.StringUtils;

/**
 * Top level singleton/facade/dao for the security authentication/authorization subsystem.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerSecurityManager implements ApplicationContextAware, ApplicationListener {

    private static final String VERSION_PROPERTIES = "version.properties";

    private static final Version VERSION_2_1 = new Version("2.1");

    private static final Version VERSION_2_2 = new Version("2.2");

    private static final Version VERSION_2_3 = new Version("2.3");

    private static final Version VERSION_2_4 = new Version("2.4");

    private static final Version VERSION_2_5 = new Version("2.5");

    private static final Version BASE_VERSION = VERSION_2_1;

    private static final Version CURR_VERSION = VERSION_2_5;

    private static final String VERSION = "version";

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** default config file name */
    public static final String CONFIG_FILENAME = "config.xml";

    /** master password config file name */
    public static final String MASTER_PASSWD_CONFIG_FILENAME = "masterpw.xml";

    /** master password info file name */
    public static final String MASTER_PASSWD_INFO_FILENAME = "masterpw.info";

    /** master password digest file name */
    public static final String MASTER_PASSWD_DIGEST_FILENAME = "masterpw.digest";

    /** default master password */
    public static final char[] MASTER_PASSWD_DEFAULT = "geoserver".toCharArray();

    /** the core spring authentication provider manager */
    ProviderManager providerMgr;

    /** data directory file system access */
    GeoServerDataDirectory dataDir;

    /** app context for loading plugins */
    ApplicationContext appContext;

    /** the active role service */
    GeoServerRoleService activeRoleService;

    /** configured authentication providers */
    List<GeoServerAuthenticationProvider> authProviders;

    /** current security config */
    SecurityManagerConfig securityConfig = new SecurityManagerConfig();

    /** current master password config */
    MasterPasswordConfig masterPasswordConfig = new MasterPasswordConfig();

    /** digested master password */
    volatile String masterPasswdDigest;

    /** cached user groups */
    ConcurrentHashMap<String, GeoServerUserGroupService> userGroupServices =
            new ConcurrentHashMap<>();

    /** cached role services */
    ConcurrentHashMap<String, GeoServerRoleService> roleServices = new ConcurrentHashMap<>();

    /** cached password validators services */
    ConcurrentHashMap<String, PasswordValidator> passwordValidators = new ConcurrentHashMap<>();

    /** some helper instances for storing/loading service config */
    RoleServiceHelper roleServiceHelper = new RoleServiceHelper();

    UserGroupServiceHelper userGroupServiceHelper = new UserGroupServiceHelper();
    AuthProviderHelper authProviderHelper = new AuthProviderHelper();
    FilterHelper filterHelper = new FilterHelper();
    PasswordValidatorHelper passwordValidatorHelper = new PasswordValidatorHelper();
    MasterPasswordProviderHelper masterPasswordProviderHelper = new MasterPasswordProviderHelper();

    /** helper for encrypting store configuration parameters */
    ConfigurationPasswordEncryptionHelper configPasswordEncryptionHelper;

    /** listeners */
    List<SecurityManagerListener> listeners = new ArrayList<>();

    /** cached flag determining is strong cryptography is available */
    Boolean strongEncryptionAvaialble;

    /** flag set once the security manager has been fully initialized */
    boolean initialized = false;

    /** keystore provider, loaded lazily */
    volatile KeyStoreProvider keyStoreProvider;

    /** generator of random passwords */
    RandomPasswordProvider randomPasswdProvider = new RandomPasswordProvider();

    /** authentication cache */
    volatile AuthenticationCache authCache;

    /** rememmber me service */
    volatile RememberMeServices rememberMeService;

    private XStreamPersister xp;

    private XStreamPersister gxp;

    public static final String REALM = "GeoServer Realm";

    public GeoServerSecurityManager(GeoServerDataDirectory dataDir) throws Exception {
        this.dataDir = dataDir;

        /*
         * JD we have to ensure that the master password is initialized first thing, before the
         * catalog since we need to decrypt configuration the passwords, the rest of the security
         * initializes occurs at the end of startup
         */
        Resource masterpw = security().get(MASTER_PASSWD_CONFIG_FILENAME);
        if (masterpw.getType() == Type.RESOURCE) {
            init(loadMasterPasswordConfig());
        }
        // if it doesn't exist this must be a migration startup... and this case should be
        // handled during migration where all the datastore passwords are processed
        // explicitly

        configPasswordEncryptionHelper = new ConfigurationPasswordEncryptionHelper(this);
    }

    private AuthenticationManager authMgrProxy =
            new AuthenticationManager() {
                @Override
                public Authentication authenticate(Authentication authentication)
                        throws AuthenticationException {
                    return providerMgr.authenticate(authentication);
                }
            };

    private DefaultAuthenticationEventPublisher eventPublisher;

    public AuthenticationManager authenticationManager() {
        return authMgrProxy;
    }

    public Catalog getCatalog() {
        // have to look this up dynamically on demand on avoid circular dependency on application
        // context startup
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    public List<AuthenticationProvider> getProviders() {
        Preconditions.checkNotNull(providerMgr, "Provider manager has not yet been created");
        return providerMgr.getProviders();
    }

    @VisibleForTesting
    public void setProviders(List<AuthenticationProvider> providers) throws Exception {
        providerMgr = new ProviderManager(providers);
        providerMgr.setEraseCredentialsAfterAuthentication(true);
        // check to allow mock tests to work anyways
        if (eventPublisher != null) {
            providerMgr.setAuthenticationEventPublisher(this.eventPublisher);
        }
        providerMgr.afterPropertiesSet();
    }

    public ConfigurationPasswordEncryptionHelper getConfigPasswordEncryptionHelper() {
        return configPasswordEncryptionHelper;
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.appContext = appContext;
        this.xp = buildPersister();
        this.gxp = buildGlobalPersister();
        this.eventPublisher = new DefaultAuthenticationEventPublisher(appContext);
    }

    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextLoadedEvent) {
            reload();
        }
        if (event instanceof ContextClosedEvent) {
            try {
                destroy();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error destroying security manager", e);
            }
        }
    }

    /**
     * Reload the configuration which may have been updated in the meanwhile; after a restore as an
     * instance.
     */
    public void reload() {
        try {
            Resource masterPasswordInfo = security().get(MASTER_PASSWD_INFO_FILENAME);
            if (masterPasswordInfo.getType() != Type.UNDEFINED) {
                LOGGER.warning(
                        masterPasswordInfo.path()
                                + " is a security risk. Please read this file and remove it afterward");
            }
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // migrate from old security config
        try {
            Version securityVersion = getSecurityVersion();

            boolean migratedFrom21 = false;
            if (securityVersion.compareTo(VERSION_2_2) < 0) {
                migratedFrom21 = migrateFrom21();
            }
            if (securityVersion.compareTo(VERSION_2_3) < 0) {
                removeErroneousAccessDeniedPage();
                migrateFrom22(migratedFrom21);
            }
            if (securityVersion.compareTo(VERSION_2_4) < 0) {
                migrateFrom23();
            }
            if (securityVersion.compareTo(VERSION_2_5) < 0) {
                migrateFrom24();
            }
            if (securityVersion.compareTo(CURR_VERSION) < 0) {
                writeCurrentVersion();
            }
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // read config and initialize... we do this now since we can be ensured that the spring
        // context has been property initialized, and we can successfully look up security
        // plugins
        KeyStoreProvider keyStoreProvider = getKeyStoreProvider();
        try {
            // check for an outstanding masster password change
            keyStoreProvider.commitMasterPasswordChange();
            // check if there is an outstanding master password change in case of SPrin injection
            init();
            for (GeoServerSecurityProvider securityProvider :
                    GeoServerExtensions.extensions(GeoServerSecurityProvider.class)) {
                securityProvider.init(this);
            }
        } catch (Exception e) {
            throw new BeanCreationException("Error occured reading security configuration", e);
        }
    }

    private Version getSecurityVersion() throws IOException {
        Resource security = security();
        if (security.getType() == Type.UNDEFINED) {
            return BASE_VERSION;
        }
        Resource properties = security.get(VERSION_PROPERTIES);
        if (properties.getType() == Type.UNDEFINED) {
            return BASE_VERSION;
        }
        Properties p = new Properties();
        try (InputStream is = properties.in()) {
            p.load(is);
        }
        String version = p.getProperty(VERSION);
        if (version != null) {
            return new Version(version);
        } else {
            return BASE_VERSION;
        }
    }

    private void writeCurrentVersion() throws IOException {
        Resource security = security();
        security.dir();
        Resource properties = security.get(VERSION_PROPERTIES);
        Properties p = new Properties();
        p.put(VERSION, CURR_VERSION.toString());
        try (OutputStream os = properties.out()) {
            p.store(
                    os,
                    "Current version of the security directory. Do not remove or alter this file");
        }
    }

    void migrateFrom24() throws SecurityConfigException, IOException {
        // allows migration of RoleSource from PreAuthenticatedUserNameFilterConfig
        MigrationHelper mh =
                xp ->
                        xp.getXStream()
                                .registerConverter(
                                        new Converter() {

                                            @Override
                                            @SuppressWarnings("unchecked")
                                            public boolean canConvert(Class cls) {
                                                return cls.isAssignableFrom(RoleSource.class);
                                            }

                                            @Override
                                            public void marshal(
                                                    Object rs,
                                                    HierarchicalStreamWriter writer,
                                                    MarshallingContext ctx) {
                                                if (rs != null) {
                                                    writer.setValue(rs.toString());
                                                }
                                            }

                                            @Override
                                            public Object unmarshal(
                                                    HierarchicalStreamReader reader,
                                                    UnmarshallingContext ctx) {
                                                if (reader.getValue() != null) {
                                                    return J2EERoleSource.valueOf(
                                                            reader.getValue());
                                                }
                                                return null;
                                            }
                                        });
        for (String fName : listFilters()) {
            SecurityFilterConfig fConfig = loadFilterConfig(fName, mh);
            if (fConfig != null) {
                if (fConfig instanceof J2eeAuthenticationBaseFilterConfig) {
                    J2eeAuthenticationBaseFilterConfig j2eeConfig =
                            (J2eeAuthenticationBaseFilterConfig) fConfig;
                    // add default J2EE RoleSource that was the only one possible
                    // before 2.5
                    if (j2eeConfig.getRoleSource() == null) {
                        j2eeConfig.setRoleSource(J2EERoleSource.J2EE);
                    }
                } else if (fConfig instanceof PreAuthenticatedUserNameFilterConfig) {
                    PreAuthenticatedUserNameFilterConfig userNameConfig =
                            (PreAuthenticatedUserNameFilterConfig) fConfig;
                    RoleSource rs = userNameConfig.getRoleSource();
                    if (rs != null) {
                        // use the right RoleSource enum
                        userNameConfig.setRoleSource(
                                PreAuthenticatedUserNameRoleSource.valueOf(rs.toString()));
                    }
                }
                saveFilter(fConfig, mh);
            }
        }
    }

    public void destroy() throws Exception {
        for (GeoServerSecurityProvider securityProvider :
                GeoServerExtensions.extensions(GeoServerSecurityProvider.class)) {
            securityProvider.destroy(this);
        }
        userGroupServices.clear();
        roleServices.clear();

        userGroupServiceHelper.destroy();
        roleServiceHelper.destroy();

        rememberMeService = null;
        keyStoreProvider = null;

        listeners.clear();

        appContext = null;
    }

    /** Adds a listener to the security manager. */
    public void addListener(SecurityManagerListener listener) {
        listeners.add(listener);
    }

    /** Removes a listener to the security manager. */
    public void removeListener(SecurityManagerListener listener) {
        listeners.remove(listener);
    }

    /** List of active/configured authentication providers */
    public List<GeoServerAuthenticationProvider> getAuthenticationProviders() {
        return authProviders;
    }

    /*
     * loads configuration and initializes the security subsystem.
     */
    void init() throws Exception {
        init(loadMasterPasswordConfig());
        init(loadSecurityConfig());
        fireChanged();
    }

    synchronized void init(SecurityManagerConfig config) throws Exception {

        // load the master password provider

        //  prepare the keystore providing needed key material
        getKeyStoreProvider().reloadKeyStore();

        // load the role authority and ensure it is properly configured
        String roleServiceName = config.getRoleServiceName();
        GeoServerRoleService roleService = null;
        try {
            roleService = loadRoleService(roleServiceName);

            // TODO:
            // if (!roleService.isConfigured()) {
            //    roleService = null;
            // }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    String.format(
                            "Error occured loading role service %s, "
                                    + "falling back to default role service",
                            roleServiceName),
                    e);
        }

        if (roleService == null) {
            try {
                roleService = loadRoleService("default");
            } catch (Exception e) {
                throw new RuntimeException("Fatal error occurred loading default role service", e);
            }
        }

        // configure the user details instance
        setActiveRoleService(roleService);

        // set up authentication providers
        this.authProviders = new ArrayList<>();

        // first provider is for the root user
        GeoServerRootAuthenticationProvider rootAuthProvider =
                new GeoServerRootAuthenticationProvider();
        rootAuthProvider.setSecurityManager(this);
        rootAuthProvider.initializeFromConfig(null);
        this.authProviders.add(rootAuthProvider);

        // add the custom/configured ones
        if (!config.getAuthProviderNames().isEmpty()) {
            for (String authProviderName : config.getAuthProviderNames()) {
                // TODO: handle failure here... perhaps simply disabling when auth provider
                // fails to load?
                GeoServerAuthenticationProvider authProvider =
                        authProviderHelper.load(authProviderName);
                authProviders.add(authProvider);
            }
        }

        List<AuthenticationProvider> allAuthProviders = new ArrayList<>();
        allAuthProviders.addAll(authProviders);

        // anonymous, not needed  anymore
        //        if (config.isAnonymousAuth()) {
        //            AnonymousAuthenticationProvider aap = new AnonymousAuthenticationProvider();
        //            aap.setKey("geoserver");
        //            aap.afterPropertiesSet();
        //            allAuthProviders.add(aap);
        //        }

        // remember me
        RememberMeAuthenticationProvider rap =
                new RememberMeAuthenticationProvider(config.getRememberMeService().getKey());
        rap.afterPropertiesSet();
        allAuthProviders.add(rap);

        setProviders(allAuthProviders);

        this.securityConfig = new SecurityManagerConfig(config);
        this.initialized = true;
    }

    void init(MasterPasswordConfig config) {
        this.masterPasswordConfig = new MasterPasswordConfig(config);
    }

    public KeyStoreProvider getKeyStoreProvider() {
        if (keyStoreProvider == null) {
            synchronized (this) {
                if (keyStoreProvider == null) {
                    keyStoreProvider = lookupKeyStoreProvider();
                }
            }
        }
        return keyStoreProvider;
    }

    KeyStoreProvider lookupKeyStoreProvider() {
        KeyStoreProvider ksp = GeoServerExtensions.bean(KeyStoreProvider.class);
        if (ksp == null) {
            // use default key store provider
            ksp = new KeyStoreProviderImpl();
        }

        ksp.setSecurityManager(this);
        return new LockingKeyStoreProvider(ksp);
    }

    public RandomPasswordProvider getRandomPassworddProvider() {
        return randomPasswdProvider;
    }

    public AuthenticationCache getAuthenticationCache() {
        if (authCache == null) {
            synchronized (this) {
                if (authCache == null) {
                    authCache = lookupAuthenticationCache();
                }
            }
        }
        return authCache;
    }

    /**
     * Used to supply an authentication cache (for testing) in GeoServerSystemTestSupport
     *
     * @param authCache
     */
    public void setAuthenticationCache(AuthenticationCache authCache) {
        synchronized (this) {
            this.authCache = authCache;
        }
    }

    AuthenticationCache lookupAuthenticationCache() {
        AuthenticationCache authCache = GeoServerExtensions.bean(AuthenticationCache.class);
        return authCache != null ? authCache : new GuavaAuthenticationCacheImpl(1000);
    }

    public RememberMeServices getRememberMeService() {
        if (rememberMeService == null) {
            synchronized (this) {
                if (rememberMeService == null) {
                    rememberMeService = lookupRememberMeService();
                }
            }
        }
        return rememberMeService;
    }

    RememberMeServices lookupRememberMeService() {
        return (RememberMeServices) GeoServerExtensions.bean("rememberMeServices");
    }

    public DataAccessRuleDAO getDataAccessRuleDAO() {
        return DataAccessRuleDAO.get();
    }

    public ServiceAccessRuleDAO getServiceAccessRuleDAO() {
        return ServiceAccessRuleDAO.get();
    }

    public RESTAccessRuleDAO getRESTAccessRuleDAO() {
        return RESTAccessRuleDAO.get();
    }

    /**
     * Determines if the security manager has been initialized yet.
     *
     * <p>TODO: this is a temporary hack, perhaps we should think about initializing the security
     * subsystem as the very first thing on startup... but now we have dependencies on the catalog
     * so we cant.
     */
    public boolean isInitialized() {
        return initialized;
    }

    public Resource get(String path) {
        return dataDir.get(path);
    }

    /** Security configuration root directory. */
    public Resource security() {
        return get("security");
    }

    /** Role configuration root directory. */
    public Resource role() {
        return get("security/role");
    }

    /** Password policy configuration root directory */
    public Resource passwordPolicy() {
        return get("security/pwpolicy");
    }

    /** User/group configuration root directory. */
    public Resource userGroup() throws IOException {
        return get("security/usergroup");
    }

    /** Authentication configuration root directory. */
    public Resource auth() throws IOException {
        return get("security/auth");
    }

    /** Authentication filter root directory. */
    public Resource filterRoot() throws IOException {
        return get("security/filter");
    }

    /** Master password provider root */
    public Resource masterPasswordProvider() throws IOException {
        return get("security/masterpw");
    }

    /** Lists all available role service configurations. */
    public SortedSet<String> listRoleServices() throws IOException {
        return listFiles(role());
    }

    /**
     * Loads a role service from a named configuration.
     *
     * @param name The name of the role service configuration.
     */
    public GeoServerRoleService loadRoleService(String name) throws IOException {
        GeoServerRoleService roleService = roleServices.get(name);
        if (roleService == null) {
            synchronized (this) {
                roleService = roleServices.get(name);
                if (roleService == null) {
                    roleService = roleServiceHelper.load(name);
                    if (roleService != null) {
                        GeoServerRoleService previous = roleServices.putIfAbsent(name, roleService);
                        if (previous != null) {
                            roleService = previous;
                        }
                    }
                }
            }
        }

        return wrapRoleService(roleService);
    }

    GeoServerRoleService wrapRoleService(GeoServerRoleService roleService) throws IOException {
        if (!initialized) {
            // starting up
            return roleService;
        }

        // check for group administrator and wrap accordingly
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (checkAuthenticationForAdminRole(auth)) {
            // admin, no need to wrap
            return roleService;
        }

        // check for group admin
        if (checkAuthenticationForRole(auth, GeoServerRole.GROUP_ADMIN_ROLE)) {
            roleService =
                    new GroupAdminRoleService(
                            roleService, calculateAdminGroups((UserDetails) auth.getPrincipal()));
        }
        return roleService;
    }

    List<String> calculateAdminGroups(UserDetails userDetails) throws IOException {
        if (userDetails instanceof GeoServerUser) {
            Properties props = ((GeoServerUser) userDetails).getProperties();
            if (GroupAdminProperty.has(props)) {
                return Arrays.asList(GroupAdminProperty.get(props));
            }
        }

        // fall back on including every group the user is part of
        List<String> groupNames = new ArrayList<>();
        for (GeoServerUserGroupService ugService : loadUserGroupServices()) {
            GeoServerUser user = ugService.getUserByUsername(userDetails.getUsername());
            if (user != null) {
                for (GeoServerUserGroup group : ugService.getGroupsForUser(user)) {
                    groupNames.add(group.getGroupname());
                }
            }
        }
        return groupNames;
    }

    /**
     * Loads a role {@link SecurityRoleServiceConfig} from a named configuration. <code>null</code>
     * if not found
     *
     * @param name The name of the role service configuration.
     */
    public SecurityRoleServiceConfig loadRoleServiceConfig(String name) throws IOException {
        return roleServiceHelper.loadConfig(name, true);
    }

    /**
     * Loads a password validator from a named configuration.
     *
     * @param name The name of the password policy configuration.
     */
    public PasswordValidator loadPasswordValidator(String name) throws IOException {
        PasswordValidator validator = passwordValidators.get(name);
        if (validator == null) {
            synchronized (this) {
                validator = passwordValidators.get(name);
                if (validator == null) {
                    validator = passwordValidatorHelper.load(name);
                    if (validator != null) {
                        PasswordValidator previous =
                                passwordValidators.putIfAbsent(name, validator);
                        if (previous != null) {
                            validator = previous;
                        }
                    }
                }
            }
        }
        return validator;
    }

    /**
     * Loads a password {@link PasswordPolicyConfig} from a named configuration.
     * <code>null</a> if not found
     *
     * @param name The name of the password policy configuration.
     */
    public PasswordPolicyConfig loadPasswordPolicyConfig(String name) throws IOException {
        return passwordValidatorHelper.loadConfig(name, true);
    }

    /**
     * Loads a password encoder with the specified name.
     *
     * @return The password encoder, or <code>null</code> if non found matching the name.
     */
    public GeoServerPasswordEncoder loadPasswordEncoder(String name) {
        GeoServerPasswordEncoder encoder =
                (GeoServerPasswordEncoder) GeoServerExtensions.bean(name);
        if (encoder != null) {
            try {
                encoder.initialize(this);
            } catch (IOException e) {
                throw new RuntimeException("Error occurred initializing password encoder");
            }
        }
        return encoder;
    }

    /**
     * Loads the first password encoder that matches the specified class filter.
     *
     * <p>This method is shorthand for:
     *
     * <pre>
     *   loadPasswordEncoder(filter, null, null);
     * </pre>
     */
    public <T extends GeoServerPasswordEncoder> T loadPasswordEncoder(Class<T> filter) {
        return loadPasswordEncoder(filter, null, null);
    }

    /**
     * Loads the first password encoder that matches the specified criteria.
     *
     * @param filter Class used to filter password encoders.
     * @param reversible Flag indicating if a reversible encoder is required, true forces
     *     reversible, false forces irreversible, null means either.
     * @param strong Flag indicating if an encoder that supports strong encryption is required, true
     *     forces strong encryption, false forces weak encryption, null means either.
     * @return The first encoder matching, or null if none was found.
     */
    public <T extends GeoServerPasswordEncoder> T loadPasswordEncoder(
            Class<T> filter, Boolean reversible, Boolean strong) {
        List<T> pw = loadPasswordEncoders(filter, reversible, strong);
        return pw.isEmpty() ? null : pw.get(0);
    }

    /** Looks up all available password encoders. */
    public List<GeoServerPasswordEncoder> loadPasswordEncoders() {
        return loadPasswordEncoders(null);
    }

    /**
     * Looks up all available password encoders filtering out only those that are instances of the
     * specified class.
     *
     * <p>This method is convenience for:
     *
     * <pre>
     * loadPasswordEncoders(filter, null, null)
     * </pre>
     */
    public <T extends GeoServerPasswordEncoder> List<T> loadPasswordEncoders(Class<T> filter) {
        return loadPasswordEncoders(filter, null, null);
    }

    /**
     * Loads all the password encoders that match the specified criteria.
     *
     * @param filter Class used to filter password encoders.
     * @param reversible Flag indicating if a reversible encoder is required, true forces
     *     reversible, false forces irreversible, null means either.
     * @param strong Flag indicating if an encoder that supports strong encryption is required, true
     *     forces strong encryption, false forces weak encryption, null means either.
     * @return All matching encoders, or an empty list.
     */
    public <T extends GeoServerPasswordEncoder> List<T> loadPasswordEncoders(
            Class<T> filter, Boolean reversible, Boolean strong) {
        filter = defaultFilterClass(filter);

        List<T> list = GeoServerExtensions.extensions(filter);
        for (Iterator<T> it = list.iterator(); it.hasNext(); ) {
            boolean remove = false;
            T pw = it.next();
            if (reversible != null && !reversible.equals(pw.isReversible())) {
                remove = true;
            }
            if (!remove
                    && strong != null
                    && strong.equals(pw.isAvailableWithoutStrongCryptogaphy())) {
                remove = true;
            }

            if (remove) {
                it.remove();
            } else {
                try {
                    pw.initialize(this);
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error initializing password encoder " + pw.getName() + ", skipping",
                            e);
                    it.remove();
                }
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private <T extends GeoServerPasswordEncoder> Class<T> defaultFilterClass(Class<T> filter) {
        return (Class<T>) (filter != null ? filter : GeoServerPasswordEncoder.class);
    }

    /**
     * Determines if strong encryption is available.
     *
     * <p>This method does the determination by trying to encrypt a value with AES 256 Bit
     * encryption.
     *
     * @return True if strong encryption available, otherwise false.
     */
    public boolean isStrongEncryptionAvailable() {
        if (strongEncryptionAvaialble != null) return strongEncryptionAvaialble;

        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance("AES");
            kgen.init(256);
            SecretKey skey = kgen.generateKey();
            byte[] raw = skey.getEncoded();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            cipher.doFinal("This is just an example".getBytes());
            strongEncryptionAvaialble = true;
            LOGGER.info("Strong cryptography is available");
        } catch (InvalidKeyException e) {
            strongEncryptionAvaialble = false;
            LOGGER.warning(
                    "Strong cryptography is NOT available"
                            + "\nDownload and installation the of unlimted length policy files is recommended");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Strong cryptography is NOT available, unexpected error", ex);
            strongEncryptionAvaialble = false; // should not happen
        }
        return strongEncryptionAvaialble;
    }

    /** Saves/persists a role service configuration. */
    public void saveRoleService(SecurityRoleServiceConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerRoleService.class, config.getClassName());

        if (config.getId() == null) {
            config.initBeforeSave();
            validator.validateAddRoleService(config);
        } else {
            validator.validateModifiedRoleService(
                    config, roleServiceHelper.loadConfig(config.getName(), true));
        }

        roleServiceHelper.saveConfig(config);
        // remove from cache
        roleServices.remove(config.getName());

        // update active role service
        if (activeRoleService != null && config.getName().equals(activeRoleService.getName())) {
            synchronized (activeRoleService) {
                activeRoleService.initializeFromConfig(config);
            }
        }
    }

    /** Saves/persists a password policy configuration. */
    public void savePasswordPolicy(PasswordPolicyConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        PasswordValidator.class, config.getClassName());

        if (config.getId() == null) {
            config.initBeforeSave();
            validator.validateAddPasswordPolicy(config);
        } else {
            validator.validateModifiedPasswordPolicy(
                    config, passwordValidatorHelper.loadConfig(config.getName(), true));
        }

        passwordValidatorHelper.saveConfig(config);
    }

    /**
     * Removes a role service configuration.
     *
     * @param config The role service configuration.
     */
    public void removeRoleService(SecurityRoleServiceConfig config)
            throws IOException, SecurityConfigException {

        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerRoleService.class, config.getClassName());

        validator.validateRemoveRoleService(config);

        roleServices.remove(config.getName());
        roleServiceHelper.removeConfig(config.getName());
    }

    /**
     * Removes a password validator configuration.
     *
     * @param config The password validator configuration.
     */
    public void removePasswordValidator(PasswordPolicyConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        PasswordValidator.class, config.getClassName());

        validator.validateRemovePasswordPolicy(config);
        passwordValidators.remove(config.getName());
        passwordValidatorHelper.removeConfig(config.getName());
    }

    /** Lists all available user group service configurations. */
    public SortedSet<String> listUserGroupServices() throws IOException {
        return listFiles(userGroup());
    }

    /** Lists all available password Validators. */
    public SortedSet<String> listPasswordValidators() throws IOException {
        return listFiles(passwordPolicy());
    }

    /** Loads all available user group services. */
    public List<GeoServerUserGroupService> loadUserGroupServices() throws IOException {
        List<GeoServerUserGroupService> ugServices = new ArrayList<>();

        for (String ugServiceName : listUserGroupServices()) {
            try {
                GeoServerUserGroupService ugService = userGroupServiceHelper.load(ugServiceName);
                ugServices.add(ugService);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load user group service " + ugServiceName, e);
            }
        }

        return ugServices;
    }

    /**
     * Loads a user group service from a named configuration.
     *
     * @param name The name of the user group service configuration.
     */
    public GeoServerUserGroupService loadUserGroupService(String name) throws IOException {
        GeoServerUserGroupService ugService = userGroupServices.get(name);
        if (ugService == null) {
            synchronized (this) {
                ugService = userGroupServices.get(name);
                if (ugService == null) {
                    ugService = userGroupServiceHelper.load(name);
                    if (ugService != null) {
                        GeoServerUserGroupService previous =
                                userGroupServices.putIfAbsent(name, ugService);
                        if (previous != null) {
                            ugService = previous;
                        }
                    }
                }
            }
        }

        return wrapUserGroupService(ugService);
    }

    GeoServerUserGroupService wrapUserGroupService(GeoServerUserGroupService ugService)
            throws IOException {
        if (!initialized) {
            // starting up
            return ugService;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (checkAuthenticationForAdminRole(auth)) {
            // full admin, no need to wrap
            return ugService;
        }

        // check for group administrator and wrap accordingly
        if (checkAuthenticationForRole(auth, GeoServerRole.GROUP_ADMIN_ROLE)) {
            ugService =
                    new GroupAdminUserGroupService(
                            ugService, calculateAdminGroups((UserDetails) auth.getPrincipal()));
        }
        return ugService;
    }

    /**
     * Loads a user {@link SecurityUserGroupServiceConfig} from a named configuration. <code>null
     * </code> if not foun
     *
     * @param name The name of the user group service configuration.
     */
    public SecurityUserGroupServiceConfig loadUserGroupServiceConfig(String name)
            throws IOException {
        return userGroupServiceHelper.loadConfig(name, true);
    }

    /** Saves/persists a user group service configuration. */
    public void saveUserGroupService(SecurityUserGroupServiceConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerUserGroupService.class, config.getClassName());

        if (config.getId() == null) {
            config.initBeforeSave();
            validator.validateAddUserGroupService(config);
        } else {
            validator.validateModifiedUserGroupService(
                    config, userGroupServiceHelper.loadConfig(config.getName(), true));
        }

        userGroupServiceHelper.saveConfig(config);
        // remove from cache
        userGroupServices.remove(config.getName());
    }

    /**
     * Removes a user group service configuration.
     *
     * @param config The user group service configuration.
     */
    public void removeUserGroupService(SecurityUserGroupServiceConfig config)
            throws IOException, SecurityConfigException {

        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerUserGroupService.class, config.getClassName());

        validator.validateRemoveUserGroupService(config);

        userGroupServices.remove(config.getName());
        userGroupServiceHelper.removeConfig(config.getName());
    }

    /** Lists all available authentication provider configurations. */
    public SortedSet<String> listAuthenticationProviders() throws IOException {
        return listFiles(auth());
    }

    /**
     * Loads an authentication provider from a named configuration.
     *
     * @param name The name of the authentication provider service configuration.
     */
    public GeoServerAuthenticationProvider loadAuthenticationProvider(String name)
            throws IOException {
        return authProviderHelper.load(name);
    }

    /**
     * Loads an authentication provider config from a named configuration. <code>null</code> if not
     * found
     *
     * @param name The name of the authentication provider service configuration.
     */
    public SecurityAuthProviderConfig loadAuthenticationProviderConfig(String name)
            throws IOException {
        return authProviderHelper.loadConfig(name, true);
    }

    public void saveAuthenticationProvider(SecurityAuthProviderConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerAuthenticationProvider.class, config.getClassName());

        if (config.getId() == null) {
            config.initBeforeSave();
            validator.validateAddAuthProvider(config);
        } else {
            validator.validateModifiedAuthProvider(
                    config, authProviderHelper.loadConfig(config.getName(), true));
        }

        // update the running auth providers
        if (authProviders != null) {
            GeoServerAuthenticationProvider authProvider = null;
            for (GeoServerAuthenticationProvider ap : authProviders) {
                if (config.getName().equals(ap.getName())) {
                    authProvider = ap;
                    break;
                }
            }

            if (authProvider != null) {
                synchronized (authProvider) {
                    authProvider.initializeFromConfig(config);
                }
            }
        }

        authProviderHelper.saveConfig(config);
    }

    /**
     * Checks if the currently authenticated user has the administrator role.
     *
     * <p>This method is shorthand for: <code>
     * <pre>
     *   checkAuthenticationForAdminRole(SecurityContextHolder.getContext().getAuthentication())
     * </pre>
     * </code>
     */
    public boolean checkAuthenticationForAdminRole() {
        if (SecurityContextHolder.getContext() == null)
            return checkAuthenticationForAdminRole(null);
        else
            return checkAuthenticationForAdminRole(
                    SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Checks if the specified authentication has the administrator role.
     *
     * <p>This method is shorthand for: <code>
     * <pre>
     *   checkAuthenticationForRole(auth, GeoServerRole.ADMIN_ROLE)
     * </pre>
     * </code>
     */
    public boolean checkAuthenticationForAdminRole(Authentication auth) {
        return checkAuthenticationForRole(auth, GeoServerRole.ADMIN_ROLE);
    }

    /**
     * Checks if the specified authentication contains the specified role.
     *
     * If the current {@link HttpServletRequest} has security disabled,
     * this method always returns <code>true</code>.
     *
     * @return <code>true</code> if the authenticated contains the role, otherwise <code>false</false>
     */
    public boolean checkAuthenticationForRole(Authentication auth, GeoServerRole role) {

        if (GeoServerSecurityFilterChainProxy.isSecurityEnabledForCurrentRequest() == false)
            return true; // No security means any role is granted

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (role.getAuthority().equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the default password for the {@Link GeoServerUser#ADMIN_USERNAME} has not
     * been changed.
     */
    public boolean checkForDefaultAdminPassword() {
        Authentication token =
                new UsernamePasswordAuthenticationToken(
                        GeoServerUser.ADMIN_USERNAME, GeoServerUser.DEFAULT_ADMIN_PASSWD);

        try {
            token = providerMgr.authenticate(token);
        } catch (Exception e) {
            // ok
        }

        return token.isAuthenticated();
    }

    /** Lists all available filter configurations. */
    public SortedSet<String> listFilters() throws IOException {
        return listFiles(filterRoot());
    }

    /**
     * Lists all available pre authentication filter configurations whose implentation class is an
     * instance of the specified class.
     */
    public SortedSet<String> listFilters(Class<?> type) throws IOException {
        SortedSet<String> configs = new TreeSet<>();
        for (String name : listFilters()) {
            SecurityFilterConfig config = loadFilterConfig(name, true);
            if (config.getClassName() == null) {
                continue;
            }

            try {
                if (type.isAssignableFrom(Class.forName(config.getClassName()))) {
                    configs.add(config.getName());
                }
            } catch (ClassNotFoundException e) {
                // ignore and continue
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        return configs;
    }

    /**
     * Loads an authentication provider from a named configuration.
     *
     * @param name The name of the authentication provider service configuration.
     */
    public GeoServerSecurityFilter loadFilter(String name) throws IOException {
        return filterHelper.load(name);
    }

    /**
     * Loads an authentication provider config from a named configuration.
     * <code>null</a> if not found
     *
     * @param name The name of the authentication provider service configuration.
     * @param migrationHelper Optional helper used for migration purposes
     */
    public SecurityFilterConfig loadFilterConfig(String name, MigrationHelper migrationHelper)
            throws IOException {
        return filterHelper.loadConfig(name, migrationHelper, true);
    }

    /**
     * Loads an authentication provider config from a named configuration.
     * <code>null</a> if not found
     *
     * @param name The name of the authentication provider service configuration.
     */
    public SecurityFilterConfig loadFilterConfig(String name, boolean allowEnvParametrization)
            throws IOException {
        return filterHelper.loadConfig(name, allowEnvParametrization);
    }

    public void saveFilter(SecurityNamedServiceConfig config)
            throws IOException, SecurityConfigException {
        saveFilter(config, null);
    }

    public void saveFilter(SecurityNamedServiceConfig config, MigrationHelper migrationHelper)
            throws IOException, SecurityConfigException {

        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerSecurityFilter.class, config.getClassName());

        boolean fireChanged = false;
        if (config.getId() == null) {
            config.initBeforeSave();
            validator.validateAddFilter(config);
        } else {
            validator.validateModifiedFilter(
                    config, filterHelper.loadConfig(config.getName(), migrationHelper, true));
            // remove all cached authentications for this filter
            getAuthenticationCache().removeAll(config.getName());
            if (!securityConfig
                    .getFilterChain()
                    .patternsForFilter(config.getName(), true)
                    .isEmpty()) {
                fireChanged = true;
            }
        }

        filterHelper.saveConfig(config);
        if (fireChanged) {
            fireChanged();
        }
    }

    /**
     * Removes an authentication provider configuration.
     *
     * @param config The authentication provider configuration.
     */
    public void removeAuthenticationProvider(SecurityAuthProviderConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerAuthenticationProvider.class, config.getClassName());
        validator.validateRemoveAuthProvider(config);
        authProviderHelper.removeConfig(config.getName());
    }

    public void removeFilter(SecurityNamedServiceConfig config)
            throws IOException, SecurityConfigException {
        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        GeoServerSecurityFilter.class, config.getClassName());
        validator.validateRemoveFilter(config);
        getAuthenticationCache().removeAll(config.getName());
        filterHelper.removeConfig(config.getName());
    }

    /**
     * Returns the current security configuration.
     *
     * <p>In order to make changes to the security configuration client code may make changes to
     * this object directly, but must call {@link #saveSecurityConfig(SecurityManagerConfig)} in
     * order to persist changes.
     */
    public SecurityManagerConfig getSecurityConfig() {
        return new SecurityManagerConfig(this.securityConfig);
    }

    public boolean isEncryptingUrlParams() {
        if (this.securityConfig == null) return false;
        return this.securityConfig.isEncryptingUrlParams();
    }
    /*
     * saves the global security config
     * TODO: use read/write lock rather than full synchronied
     */
    public synchronized void saveSecurityConfig(SecurityManagerConfig config) throws Exception {

        SecurityManagerConfig oldConfig = new SecurityManagerConfig(this.securityConfig);

        SecurityConfigValidator validator = new SecurityConfigValidator(this);
        validator.validateManagerConfig(
                (SecurityManagerConfig) config.clone(true),
                (SecurityManagerConfig) oldConfig.clone(true));

        // save the current config to fall back to

        // The whole try block should run as a transaction, unfortunately
        // this is not possible with files.
        try {
            // set the new configuration
            init(config);
            if (config.getConfigPasswordEncrypterName()
                            .equals(oldConfig.getConfigPasswordEncrypterName())
                    == false) {
                updateConfigurationFilesWithEncryptedFields();
            }

            // save out new configuration
            xStreamPersist(security().get(CONFIG_FILENAME), config, globalPersister());
        } catch (IOException e) {
            // exception, revert back to known working config
            LOGGER.log(Level.SEVERE, "Error saving security config, reverting back to previous", e);
            init(oldConfig);
            return;
        }

        fireChanged();
    }

    /** Returns the master password configuration. */
    public MasterPasswordConfig getMasterPasswordConfig() {
        return new MasterPasswordConfig(masterPasswordConfig);
    }

    /**
     * Saves the master password configuration.
     *
     * @param config The new configuration.
     * @param currPasswd The current master password.
     * @param newPasswd The new password, may be null depending on strategy used.
     * @param newPasswdConfirm The confirmation password
     * @throws MasterPasswordChangeException If there is a validation error with the new config
     * @throws PasswordPolicyException If the new password violates the master password policy
     */
    public synchronized void saveMasterPasswordConfig(
            MasterPasswordConfig config,
            char[] currPasswd,
            char[] newPasswd,
            char[] newPasswdConfirm)
            throws Exception {

        // load the (possibly new) master password provider
        MasterPasswordProviderConfig mpProviderConfig =
                loadMasterPassswordProviderConfig(config.getProviderName());
        MasterPasswordProvider mpProvider = loadMasterPasswordProvider(config.getProviderName());

        if (mpProviderConfig.isReadOnly()) {
            // new password comes from the provider
            newPasswd = mpProvider.getMasterPassword();
        }

        // first validate the password change
        MasterPasswordChangeRequest req = new MasterPasswordChangeRequest();
        req.setCurrentPassword(currPasswd);
        req.setNewPassword(newPasswd);
        req.setConfirmPassword(newPasswdConfirm);

        MasterPasswordChangeValidator val = new MasterPasswordChangeValidator(this);
        val.validateChangeRequest(req);

        // validate the new config
        MasterPasswordConfigValidator validator = new MasterPasswordConfigValidator(this);
        validator.validateMasterPasswordConfig(config);

        // save the current config to fall back to
        MasterPasswordConfig oldConfig = new MasterPasswordConfig(this.masterPasswordConfig);
        String oldMasterPasswdDigest = masterPasswdDigest;

        KeyStoreProvider ksProvider = getKeyStoreProvider();
        synchronized (ksProvider) {
            ksProvider.prepareForMasterPasswordChange(currPasswd, newPasswdConfirm);
            try {
                if (!mpProviderConfig.isReadOnly()) {
                    // write it back first
                    try {
                        mpProvider.setMasterPassword(newPasswd);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }

                // save out the master password config
                saveMasterPasswordConfig(config);

                // redigest
                masterPasswdDigest = computeAndSaveMasterPasswordDigest(newPasswdConfirm);

                // commit the password change to the keystore
                ksProvider.commitMasterPasswordChange();

                // if (!config.getProviderName().equals(oldConfig.getProviderName())) {
                // TODO: reencrypt the keystore? restart the server?
                // updateConfigurationFilesWithEncryptedFields();
                // }
            } catch (IOException e) {
                // error occurred, roll back
                ksProvider.abortMasterPasswordChange();

                // revert to old master password config
                this.masterPasswordConfig = oldConfig;
                this.masterPasswdDigest = oldMasterPasswdDigest;
                saveMasterPasswordDigest(oldMasterPasswdDigest);

                throw e;
            }
        }
    }

    /** Saves master password config out directly, not during a password change. */
    public void saveMasterPasswordConfig(MasterPasswordConfig config) throws IOException {
        xStreamPersist(security().get(MASTER_PASSWD_CONFIG_FILENAME), config, globalPersister());
        this.masterPasswordConfig = new MasterPasswordConfig(config);
    }

    /** Checks the specified password against the master password. */
    public boolean checkMasterPassword(String passwd) {
        return checkMasterPassword(passwd.toCharArray(), true);
    }

    /** Checks the specified password against the master password. */
    public boolean checkMasterPassword(String passwd, boolean forLogin) {
        return checkMasterPassword(passwd.toCharArray(), forLogin);
    }

    /** Checks the specified password against the master password. */
    public boolean checkMasterPassword(char[] passwd) {
        return checkMasterPassword(passwd, true);
    }

    /** Checks the specified password against the master password. */
    public boolean checkMasterPassword(char[] passwd, boolean forLogin) {
        try {
            if (forLogin
                    && !this.masterPasswordProviderHelper
                            .loadConfig(this.masterPasswordConfig.getProviderName(), true)
                            .isLoginEnabled()) {
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to load master password provider config", e);
        }

        GeoServerDigestPasswordEncoder pwEncoder =
                loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
        if (masterPasswdDigest == null) {
            synchronized (this) {
                if (masterPasswdDigest == null) {
                    try {
                        // look for file
                        masterPasswdDigest = loadMasterPasswordDigest();
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to create master password digest", e);
                    }
                }
            }
        }
        return pwEncoder.isPasswordValid(masterPasswdDigest, passwd, null);
    }

    String loadMasterPasswordDigest() throws IOException {
        // look for file
        Resource pwDigestFile = security().get(MASTER_PASSWD_DIGEST_FILENAME);
        if (pwDigestFile.getType() == Type.RESOURCE) {
            try (InputStream fin = pwDigestFile.in()) {
                return IOUtils.toString(fin, StandardCharsets.UTF_8);
            }
        } else {
            // compute and store
            char[] masterPasswd = getMasterPassword();
            try {
                return computeAndSaveMasterPasswordDigest(masterPasswd);
            } finally {
                disposePassword(masterPasswd);
            }
        }
    }

    void saveMasterPasswordDigest(String masterPasswdDigest) throws IOException {
        try (OutputStream fout = security().get(MASTER_PASSWD_DIGEST_FILENAME).out()) {
            IOUtils.write(masterPasswdDigest, fout, "UTF-8");
        }
    }

    String computeAndSaveMasterPasswordDigest(char[] passwd) throws IOException {
        GeoServerDigestPasswordEncoder pwEncoder =
                loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
        String masterPasswdDigest = pwEncoder.encodePassword(passwd, null);
        saveMasterPasswordDigest(masterPasswdDigest);
        return masterPasswdDigest;
    }

    /**
     * Returns the master password in plain text.
     *
     * <p>This method is package protected and only allowed to be called by classes in this package.
     *
     * <p>The password is returned as a char array rather than string to allow for the scrambling of
     * the password after use. Since strings are immutable they can not be scrambled. All code that
     * calls this method should follow the following guidelines:
     *
     * <ol>
     *   <li>Never turn the result into a String object
     *   <li>Always call {@link #disposePassword(char[])} (ideally in a finally block) when done
     *       with the password.
     * </ol>
     *
     * <p>For example: <code>
     * <pre>
     *   char[] passwd = manager.getMasterPassword();
     *   try {
     *     //do something
     *   }
     *   finally {
     *     manager.disposeMasterPassword(passwd);
     *   }
     * </pre>
     * </code>
     */
    char[] getMasterPassword() {
        try {
            MasterPasswordProvider mpp =
                    loadMasterPasswordProvider(getMasterPasswordConfig().getProviderName());
            return mpp.getMasterPassword();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Disposes the char array containing the plain text password. */
    public void disposePassword(char[] passwd) {
        SecurityUtils.scramble(passwd);
    }

    /** Disposes the byte array containing the plain text password. */
    public void disposePassword(byte[] passwd) {
        SecurityUtils.scramble(passwd);
    }

    /**
     * Loads a user {@link MasterPasswordProviderConfig} from a named configuration.
     *
     * <p>This method returns <code>null</code> if the provider config is not found.
     *
     * @param name The name of the master password provider configuration.
     */
    public MasterPasswordProviderConfig loadMasterPassswordProviderConfig(String name)
            throws IOException {
        return masterPasswordProviderHelper.loadConfig(name, true);
    }

    /**
     * Loads a user {@link MasterPasswordProvider} from a named configuration.
     *
     * <p>This method returns <code>null</code> if the provider config is not found.
     *
     * @param name The name of the master password provider configuration.
     */
    protected MasterPasswordProvider loadMasterPasswordProvider(String name) throws IOException {
        return masterPasswordProviderHelper.load(name);
    }

    /** Saves/persists a master password provider configuration. */
    public void saveMasterPasswordProviderConfig(MasterPasswordProviderConfig config)
            throws IOException, SecurityConfigException {
        saveMasterPasswordProviderConfig(config, true);
    }

    /**
     * Saves master password provider configuration, optionally skipping validation.
     *
     * <p>Validation only skipped during migration.
     */
    void saveMasterPasswordProviderConfig(MasterPasswordProviderConfig config, boolean validate)
            throws IOException, SecurityConfigException {

        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        MasterPasswordProvider.class, config.getClassName());

        if (config.getId() == null) {
            config.initBeforeSave();
            if (validate) {
                validator.validateAddMasterPasswordProvider(config);
            }
        } else {
            if (validate) {
                validator.validateModifiedMasterPasswordProvider(
                        config, masterPasswordProviderHelper.loadConfig(config.getName(), true));
            }
        }

        masterPasswordProviderHelper.saveConfig(config);
    }

    /** Removes a master password provider configuration. */
    public void removeMasterPasswordProvder(MasterPasswordProviderConfig config)
            throws IOException, SecurityConfigException {

        SecurityConfigValidator validator =
                SecurityConfigValidator.getConfigurationValiator(
                        MasterPasswordProvider.class, config.getClassName());

        validator.validateRemoveMasterPasswordProvider(config);
        masterPasswordProviderHelper.removeConfig(config.getName());
    }

    /** Lists all available master password provider configurations. */
    public SortedSet<String> listMasterPasswordProviders() throws IOException {
        return listFiles(masterPasswordProvider());
    }

    void fireChanged() {
        for (SecurityManagerListener l : listeners) {
            l.handlePostChanged(this);
        }
    }

    /** @return the master password used for the migration */
    char[] extractMasterPasswordForMigration(Properties props) throws Exception {

        Map<String, String> candidates = new HashMap<>();
        String defaultPasswordAsString = new String(MASTER_PASSWD_DEFAULT);

        if (props != null) {
            // load user.properties populate the services

            UserAttributeEditor configAttribEd = new UserAttributeEditor();

            for (Object o : props.keySet()) {
                String username = (String) o;

                configAttribEd.setAsText(props.getProperty(username));
                UserAttribute attr = (UserAttribute) configAttribEd.getValue();
                if (attr == null) continue;

                // The master password policy is not yet available, the default is to
                // have a minimum of 8 chars --> all passwords shorter than 8 chars
                // are no candidates
                if (attr.getPassword() == null || attr.getPassword().length() < 8) continue;

                // The default password is not allowed
                if (defaultPasswordAsString.equals(attr.getPassword())) continue;

                // the  user named "admin" having a non default password is the primary candiate
                if (GeoServerUser.ADMIN_USERNAME.equals(username)) {
                    candidates.put(GeoServerUser.ADMIN_USERNAME, attr.getPassword());
                    continue;
                }

                // other users having the amin role are secondary candidates
                if (attr.getAuthorities().contains(GeoServerRole.ADMIN_ROLE)) {
                    candidates.put(username, attr.getPassword());
                }
            }
        }

        String username = GeoServerUser.ADMIN_USERNAME;
        String masterPW = candidates.get(username);
        if (masterPW == null && !candidates.isEmpty()) {
            username = candidates.keySet().iterator().next();
            masterPW = candidates.get(username);
        }

        String message = null;
        Resource info = security().get(MASTER_PASSWD_INFO_FILENAME);
        char[] masterPasswordArray = null;
        if (masterPW != null) {
            message = "Master password is identical to the password of user: " + username;
            masterPasswordArray = masterPW.toCharArray();
            writeMasterPasswordInfo(info, message, null);
        } else {
            message = "The generated master password is: ";
            masterPasswordArray = getRandomPassworddProvider().getRandomPassword(8);
            writeMasterPasswordInfo(info, message, masterPasswordArray);
        }

        LOGGER.info("Information regarding the master password is in: " + info.path());
        return masterPasswordArray;
    }

    /** Writes a file containing info about the master password. */
    void writeMasterPasswordInfo(Resource file, String message, char[] masterPasswordArray)
            throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(file.out()))) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            w.write("This file was created at " + dateFormat.format(new Date()));
            w.newLine();
            w.newLine();
            w.write(message);
            if (masterPasswordArray != null) {
                w.write(masterPasswordArray);
            }
            w.newLine();
            w.newLine();
            w.write("Test the master password by logging in as user \"root\"");
            w.newLine();
            w.newLine();
            w.write("This file should be removed after reading !!!.");
            w.newLine();
        }
    }

    /**
     * Method to dump master password to a file
     *
     * <p>The file name is the shared secret between the administrator and GeoServer.
     *
     * <p>The method inspects the stack trace to check for an authorized calling method. The
     * authenticated principal has to be an administrator
     *
     * <p>If authorization fails, a warning is written in the log and the return code is <code>false
     * </code>. On success, the return code is <code>true</code>.
     */
    public boolean dumpMasterPassword(Resource file) throws IOException {
        if (file.getType() != Resource.Type.UNDEFINED) {
            LOGGER.warning("Master password dump attempted to overwrite existing resource");
            return false;
        }
        if (checkAuthenticationForAdminRole() == false) {
            LOGGER.warning("Unautorized user tries to dump master password");
            return false;
        }

        String[][] allowedMethods = {
            {"org.geoserver.security.GeoServerSecurityManagerTest", "testMasterPasswordDump"},
            {"org.geoserver.security.web.passwd.MasterPasswordInfoPage", "dumpMasterPassword"}
        };

        String result = checkStackTrace(10, allowedMethods);

        if (result != null) {
            LOGGER.warning("Dump master password is called by an unautorized method\n" + result);
            return false;
        }

        String message = "The current master password is: ";
        writeMasterPasswordInfo(file, message, getMasterPassword());
        return true;
    }

    /**
     * Get master password for REST configuraton
     *
     * <p>The method inspects the stack trace to check for an authorized calling method. The
     * authenticated principal has to be an administrator
     *
     * <p>If authorization fails, an IOException is thrown
     */
    public char[] getMasterPasswordForREST() throws IOException {

        if (checkAuthenticationForAdminRole() == false) {
            throw new IOException("Unauthorized user tries to read master password");
        }

        String[][] allowedMethods = {
            {"org.geoserver.rest.security.MasterPasswordController", "masterPasswordGet"}
        };

        String result = checkStackTrace(10, allowedMethods);
        if (result != null) {
            throw new IOException("Unauthorized method wants to read master password\n" + result);
        }

        return getMasterPassword();
    }

    /**
     * Checks if the stack trace contains allowed methods. It it contains allowed methods, return
     * <code>null</code>, if not return a String listing the methods.
     */
    String checkStackTrace(int countMethodsToCheck, String[][] allowedMethods) {

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        boolean isAllowed = false;

        for (int i = 0; i < countMethodsToCheck; i++) {
            StackTraceElement element = stackTraceElements[i];
            for (String[] methodEntry : allowedMethods) {
                if (methodEntry[0].equals(element.getClassName())
                        && methodEntry[1].equals(element.getMethodName())) {
                    isAllowed = true;
                    break;
                }
            }
        }

        if (isAllowed) {
            return null;
        } else {
            StringBuffer buff = new StringBuffer();
            for (int i = 0; i < countMethodsToCheck; i++) {
                StackTraceElement element = stackTraceElements[i];
                buff.append(element.getClassName())
                        .append(" : ")
                        .append(element.getMethodName())
                        .append("\n");
            }
            return buff.toString();
        }
    }

    /**
     * converts an 2.1.x security configuration to 2.2.x
     *
     * @return <code>true</code> if migration has taken place
     */
    boolean migrateFrom21() throws Exception {

        if (role().getType() != Type.UNDEFINED) {
            Resource oldUserFile = security().get("users.properties.old");
            if (oldUserFile.getType() != Type.UNDEFINED) {
                LOGGER.warning(oldUserFile.path() + " could be removed manually");
            }
            return false; // already migrated
        }

        LOGGER.info("Start security migration");

        // master password configuration
        MasterPasswordProviderConfig mpProviderConfig =
                loadMasterPassswordProviderConfig("default");
        if (mpProviderConfig == null) {
            mpProviderConfig = new URLMasterPasswordProviderConfig();
            mpProviderConfig.setName("default");
            mpProviderConfig.setClassName(URLMasterPasswordProvider.class.getCanonicalName());
            mpProviderConfig.setReadOnly(false);

            ((URLMasterPasswordProviderConfig) mpProviderConfig).setURL(new URL("file:passwd"));
            ((URLMasterPasswordProviderConfig) mpProviderConfig).setEncrypting(true);
            saveMasterPasswordProviderConfig(mpProviderConfig, false);

            // save out the default master password
            MasterPasswordProvider mpProvider =
                    loadMasterPasswordProvider(mpProviderConfig.getName());
            Resource propFile = security().get("users.properties");
            Properties userprops = null;
            if (propFile.getType() == Type.RESOURCE) userprops = Util.loadPropertyFile(propFile);
            mpProvider.setMasterPassword(extractMasterPasswordForMigration(userprops));
        }

        MasterPasswordConfig mpConfig = new MasterPasswordConfig();
        mpConfig.setProviderName(mpProviderConfig.getName());
        saveMasterPasswordConfig(mpConfig);

        // check for services.properties, create if necessary
        Resource serviceFile = security().get("services.properties");
        if (serviceFile.getType() == Type.UNDEFINED) {
            org.geoserver.util.IOUtils.copy(
                    Util.class.getResourceAsStream("serviceTemplate.properties"),
                    serviceFile.out());
        }

        long checkInterval = 10000; // 10 secs

        // check for the default user group service, create if necessary
        GeoServerUserGroupService userGroupService =
                loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);

        KeyStoreProvider keyStoreProvider = getKeyStoreProvider();
        keyStoreProvider.reloadKeyStore();
        keyStoreProvider.setUserGroupKey(
                XMLUserGroupService.DEFAULT_NAME, randomPasswdProvider.getRandomPassword(32));
        keyStoreProvider.storeKeyStore();

        PasswordValidator validator = loadPasswordValidator(PasswordValidator.DEFAULT_NAME);
        if (validator == null) {
            // Policy allows any password except null, this is the default
            // at before migration
            PasswordPolicyConfig pwpconfig = new PasswordPolicyConfig();
            pwpconfig.setName(PasswordValidator.DEFAULT_NAME);
            pwpconfig.setClassName(PasswordValidatorImpl.class.getName());
            pwpconfig.setMinLength(0);
            savePasswordPolicy(pwpconfig);
            validator = loadPasswordValidator(PasswordValidator.DEFAULT_NAME);
        }

        validator = loadPasswordValidator(PasswordValidator.MASTERPASSWORD_NAME);
        if (validator == null) {
            // Policy requires a minimum of 8 chars for the master password
            PasswordPolicyConfig pwpconfig = new PasswordPolicyConfig();
            pwpconfig.setName(PasswordValidator.MASTERPASSWORD_NAME);
            pwpconfig.setClassName(PasswordValidatorImpl.class.getName());
            pwpconfig.setMinLength(8);
            savePasswordPolicy(pwpconfig);
            validator = loadPasswordValidator(PasswordValidator.MASTERPASSWORD_NAME);
        }

        if (userGroupService == null) {
            XMLUserGroupServiceConfig ugConfig = new XMLUserGroupServiceConfig();
            ugConfig.setName(XMLUserGroupService.DEFAULT_NAME);
            ugConfig.setClassName(XMLUserGroupService.class.getName());
            ugConfig.setCheckInterval(checkInterval);
            ugConfig.setFileName(XMLConstants.FILE_UR);
            ugConfig.setValidating(true);
            // start with weak encryption, plain passwords can be restored
            ugConfig.setPasswordEncoderName(
                    loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false).getName());
            ugConfig.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);
            saveUserGroupService(ugConfig);
            userGroupService = loadUserGroupService(XMLUserGroupService.DEFAULT_NAME);
        }

        // check for the default role service, create if necessary
        GeoServerRoleService roleService = loadRoleService(XMLRoleService.DEFAULT_NAME);

        if (roleService == null) {
            XMLRoleServiceConfig gaConfig = new XMLRoleServiceConfig();
            gaConfig.setName(XMLRoleService.DEFAULT_NAME);
            gaConfig.setClassName(XMLRoleService.class.getName());
            gaConfig.setCheckInterval(checkInterval);
            gaConfig.setFileName(XMLConstants.FILE_RR);
            gaConfig.setValidating(true);
            gaConfig.setAdminRoleName(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
            gaConfig.setGroupAdminRoleName(XMLRoleService.DEFAULT_LOCAL_GROUP_ADMIN_ROLE);
            saveRoleService(gaConfig);
            roleService = loadRoleService(XMLRoleService.DEFAULT_NAME);
        }

        String filterName = GeoServerSecurityFilterChain.BASIC_AUTH_FILTER;
        GeoServerSecurityFilter filter = loadFilter(filterName);
        if (filter == null) {
            BasicAuthenticationFilterConfig bfConfig = new BasicAuthenticationFilterConfig();
            bfConfig.setName(filterName);
            bfConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
            bfConfig.setUseRememberMe(true);
            saveFilter(bfConfig);
        }
        /*filterName = GeoServerSecurityFilterChain.BASIC_AUTH_NO_REMEMBER_ME_FILTER;
        filter = loadFilter(filterName);
        if (filter==null) {
            BasicAuthenticationFilterConfig bfConfig = new BasicAuthenticationFilterConfig();
            bfConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
            bfConfig.setName(filterName);
            bfConfig.setUseRememberMe(false);
            saveFilter(bfConfig);
        }*/
        filterName = GeoServerSecurityFilterChain.FORM_LOGIN_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            UsernamePasswordAuthenticationFilterConfig upConfig =
                    new UsernamePasswordAuthenticationFilterConfig();
            upConfig.setClassName(GeoServerUserNamePasswordAuthenticationFilter.class.getName());
            upConfig.setName(filterName);
            upConfig.setUsernameParameterName(
                    UsernamePasswordAuthenticationFilterConfig.DEFAULT_USERNAME_PARAM);
            upConfig.setPasswordParameterName(
                    UsernamePasswordAuthenticationFilterConfig.DEFAULT_PASSWORD_PARAM);
            saveFilter(upConfig);
        }
        filterName = GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            SecurityContextPersistenceFilterConfig pConfig =
                    new SecurityContextPersistenceFilterConfig();
            pConfig.setClassName(GeoServerSecurityContextPersistenceFilter.class.getName());
            pConfig.setName(filterName);
            pConfig.setAllowSessionCreation(true);
            saveFilter(pConfig);
        }
        filterName = GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            SecurityContextPersistenceFilterConfig pConfig =
                    new SecurityContextPersistenceFilterConfig();
            pConfig.setClassName(GeoServerSecurityContextPersistenceFilter.class.getName());
            pConfig.setName(filterName);
            pConfig.setAllowSessionCreation(false);
            saveFilter(pConfig);
        }
        filterName = GeoServerSecurityFilterChain.ANONYMOUS_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            AnonymousAuthenticationFilterConfig aConfig = new AnonymousAuthenticationFilterConfig();
            aConfig.setClassName(GeoServerAnonymousAuthenticationFilter.class.getName());
            aConfig.setName(filterName);
            saveFilter(aConfig);
        }
        filterName = GeoServerSecurityFilterChain.REMEMBER_ME_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            RememberMeAuthenticationFilterConfig rConfig =
                    new RememberMeAuthenticationFilterConfig();
            rConfig.setClassName(GeoServerRememberMeAuthenticationFilter.class.getName());
            rConfig.setName(filterName);
            saveFilter(rConfig);
        }
        filterName = GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
        filter = loadFilter(filterName);
        if (filter == null) {
            SecurityInterceptorFilterConfig siConfig = new SecurityInterceptorFilterConfig();
            siConfig.setClassName(GeoServerSecurityInterceptorFilter.class.getName());
            siConfig.setName(filterName);
            siConfig.setAllowIfAllAbstainDecisions(false);
            siConfig.setSecurityMetadataSource("geoserverMetadataSource");
            saveFilter(siConfig);
        }
        filterName = GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR;
        filter = loadFilter(filterName);
        if (filter == null) {
            SecurityInterceptorFilterConfig siConfig = new SecurityInterceptorFilterConfig();
            siConfig.setClassName(GeoServerSecurityInterceptorFilter.class.getName());
            siConfig.setName(filterName);
            siConfig.setAllowIfAllAbstainDecisions(false);
            siConfig.setSecurityMetadataSource("restFilterDefinitionMap");
            saveFilter(siConfig);
        }
        filterName = GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            LogoutFilterConfig loConfig = new LogoutFilterConfig();
            loConfig.setClassName(GeoServerLogoutFilter.class.getName());
            loConfig.setName(filterName);
            saveFilter(loConfig);
        }
        filterName = GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            ExceptionTranslationFilterConfig bfConfig = new ExceptionTranslationFilterConfig();
            bfConfig.setClassName(GeoServerExceptionTranslationFilter.class.getName());
            bfConfig.setName(filterName);
            bfConfig.setAuthenticationFilterName(null);
            bfConfig.setAccessDeniedErrorPage("/accessDenied.jsp");
            saveFilter(bfConfig);
        }
        filterName = GeoServerSecurityFilterChain.GUI_EXCEPTION_TRANSLATION_FILTER;
        filter = loadFilter(filterName);
        if (filter == null) {
            ExceptionTranslationFilterConfig bfConfig = new ExceptionTranslationFilterConfig();
            bfConfig.setClassName(GeoServerExceptionTranslationFilter.class.getName());
            bfConfig.setName(filterName);
            bfConfig.setAuthenticationFilterName(GeoServerSecurityFilterChain.FORM_LOGIN_FILTER);
            bfConfig.setAccessDeniedErrorPage("/accessDenied.jsp");
            saveFilter(bfConfig);
        }

        // check for the default auth provider, create if necessary
        GeoServerAuthenticationProvider authProvider =
                loadAuthenticationProvider(GeoServerAuthenticationProvider.DEFAULT_NAME);
        if (authProvider == null) {
            UsernamePasswordAuthenticationProviderConfig upAuthConfig =
                    new UsernamePasswordAuthenticationProviderConfig();
            upAuthConfig.setName(GeoServerAuthenticationProvider.DEFAULT_NAME);
            upAuthConfig.setClassName(UsernamePasswordAuthenticationProvider.class.getName());
            upAuthConfig.setUserGroupServiceName(userGroupService.getName());

            saveAuthenticationProvider(upAuthConfig);
            authProvider = loadAuthenticationProvider(GeoServerAuthenticationProvider.DEFAULT_NAME);
        }

        // save the top level config
        SecurityManagerConfig config = new SecurityManagerConfig();
        config.setRoleServiceName(roleService.getName());
        config.getAuthProviderNames().add(authProvider.getName());
        config.setEncryptingUrlParams(false);

        // start with weak encryption
        config.setConfigPasswordEncrypterName(
                loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, true, false).getName());

        // setup the default remember me service
        RememberMeServicesConfig rememberMeConfig = new RememberMeServicesConfig();
        rememberMeConfig.setClassName(GeoServerTokenBasedRememberMeServices.class.getName());
        config.setRememberMeService(rememberMeConfig);

        config.setFilterChain(GeoServerSecurityFilterChain.createInitialChain());
        saveSecurityConfig(config);

        // TODO: just call initializeFrom
        userGroupService.setSecurityManager(this);
        roleService.setSecurityManager(this);

        // populate the user group and role service
        GeoServerUserGroupStore userGroupStore = userGroupService.createStore();
        GeoServerRoleStore roleStore = roleService.createStore();

        // migrate from users.properties
        Resource usersFile = security().get("users.properties");
        if (usersFile.getType() == Type.RESOURCE) {
            // load user.properties populate the services
            Properties props = Util.loadPropertyFile(usersFile);

            UserAttributeEditor configAttribEd = new UserAttributeEditor();

            for (Object o : props.keySet()) {
                // the attribute editors parses the list of strings into password, username and
                // enabled
                // flag
                String username = (String) o;
                configAttribEd.setAsText(props.getProperty(username));

                // if the parsing succeeded turn that into a user object
                UserAttribute attr = (UserAttribute) configAttribEd.getValue();
                if (attr != null) {
                    GeoServerUser user =
                            userGroupStore.createUserObject(
                                    username, attr.getPassword(), attr.isEnabled());
                    userGroupStore.addUser(user);

                    for (GrantedAuthority auth : attr.getAuthorities()) {
                        String roleName =
                                GeoServerRole.ADMIN_ROLE.getAuthority().equals(auth.getAuthority())
                                        ? XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE
                                        : auth.getAuthority();
                        GeoServerRole role = roleStore.getRoleByName(roleName);
                        if (role == null) {
                            role = roleStore.createRoleObject(roleName);
                            roleStore.addRole(role);
                        }
                        roleStore.associateRoleToUser(role, username);
                    }
                }
            }
        } else {
            // no user.properties, populate with default user and roles
            if (userGroupService.getUserByUsername(GeoServerUser.ADMIN_USERNAME) == null) {
                userGroupStore.addUser(GeoServerUser.createDefaultAdmin());
                GeoServerRole localAdminRole =
                        roleStore.createRoleObject(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
                roleStore.addRole(localAdminRole);
                roleStore.associateRoleToUser(localAdminRole, GeoServerUser.ADMIN_USERNAME);
            }
        }

        // add the local group administrator role
        if (roleStore.getRoleByName(XMLRoleService.DEFAULT_LOCAL_GROUP_ADMIN_ROLE) == null) {
            roleStore.addRole(
                    roleStore.createRoleObject(XMLRoleService.DEFAULT_LOCAL_GROUP_ADMIN_ROLE));
        }

        // replace all occurrences of ROLE_ADMINISTRATOR  in the property files
        // TODO Justin, a little bit brute force, is this ok ?
        for (String filename :
                new String[] {"services.properties", "layers.properties", "rest.properties"}) {
            Resource file = security().get(filename);
            if (file.getType() == Type.UNDEFINED) {
                continue;
            }
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.in()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(
                            line.replace(
                                    GeoServerRole.ADMIN_ROLE.getAuthority(),
                                    XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE));
                }
            }
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(file.out()))) {
                for (String s : lines) {
                    writer.println(s);
                }
            }
        }

        // check for roles in services.properties but not in user.properties
        serviceFile = security().get("services.properties");
        if (serviceFile.getType() != Type.UNDEFINED) {
            Properties props = Util.loadPropertyFile(serviceFile);
            for (Entry<Object, Object> entry : props.entrySet()) {
                StringTokenizer tokenizer = new StringTokenizer((String) entry.getValue(), ",");
                while (tokenizer.hasMoreTokens()) {
                    String roleName = tokenizer.nextToken().trim();
                    if (roleName.length() > 0) {
                        if (roleStore.getRoleByName(roleName) == null) {
                            roleStore.addRole(roleStore.createRoleObject(roleName));
                        }
                    }
                }
            }
        }

        // check for  roles in data.properties but not in user.properties
        Resource dataFile = security().get("layers.properties");
        if (dataFile.getType() == Type.RESOURCE) {
            Properties props = Util.loadPropertyFile(dataFile);
            for (Entry<Object, Object> entry : props.entrySet()) {
                if ("mode".equals(entry.getKey().toString())) continue; // skip mode directive
                StringTokenizer tokenizer = new StringTokenizer((String) entry.getValue(), ",");
                while (tokenizer.hasMoreTokens()) {
                    String roleName = tokenizer.nextToken().trim();
                    if (roleName.length() > 0 && roleName.equals("*") == false) {
                        if (roleStore.getRoleByName(roleName) == null)
                            roleStore.addRole(roleStore.createRoleObject(roleName));
                    }
                }
            }
        }

        // persist the changes
        roleStore.store();
        userGroupStore.store();

        // first part of migration finished, rename old file
        if (usersFile.getType() != Type.UNDEFINED) {
            Resource oldUserFile = dataDir.get(usersFile.path() + ".old");
            usersFile.renameTo(oldUserFile);
            LOGGER.info("Renamed " + usersFile.path() + " to " + oldUserFile.path());
        }

        LOGGER.info("End security migration");
        return true;
    }

    /** migration from 2.2.x to 2.3.x return <code>true</code> if migration has taken place */
    boolean migrateFrom22(boolean migratedFrom21) throws Exception {

        String filterName = GeoServerSecurityFilterChain.ROLE_FILTER;
        GeoServerSecurityFilter filter = loadFilter(filterName);

        Resource logoutFilterDir =
                filterRoot().get(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        Resource oldLogoutFilterConfig = logoutFilterDir.get("config.xml.2.2.x");
        Resource oldSecManagerConfig = security().get("config.xml.2.2.x");

        if (filter != null) {
            if (oldLogoutFilterConfig.getType() == Type.RESOURCE)
                LOGGER.warning(oldLogoutFilterConfig.path() + " could be removed manually");
            if (oldSecManagerConfig.getType() == Type.RESOURCE)
                LOGGER.warning(oldSecManagerConfig.path() + " could be removed manually");
            return false; // already migrated
        }

        // add role filter
        RoleFilterConfig rfConfig = new RoleFilterConfig();
        rfConfig.setClassName(GeoServerRoleFilter.class.getName());
        rfConfig.setName(filterName);
        rfConfig.setHttpResponseHeaderAttrForIncludedRoles(
                GeoServerRoleFilter.DEFAULT_HEADER_ATTRIBUTE);
        rfConfig.setRoleConverterName(GeoServerRoleFilter.DEFAULT_ROLE_CONVERTER);
        saveFilter(rfConfig);

        // add ssl filter
        SSLFilterConfig sslConfig = new SSLFilterConfig();
        sslConfig.setClassName(GeoServerSSLFilter.class.getName());
        sslConfig.setName(GeoServerSecurityFilterChain.SSL_FILTER);
        sslConfig.setSslPort(443);
        saveFilter(sslConfig);

        // set redirect url after successful logout
        if (!migratedFrom21)
            org.geoserver.util.IOUtils.copy(
                    logoutFilterDir.get("config.xml").in(), oldLogoutFilterConfig.out());
        LogoutFilterConfig loConfig =
                (LogoutFilterConfig)
                        loadFilterConfig(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER, true);
        loConfig.setRedirectURL(GeoServerLogoutFilter.URL_AFTER_LOGOUT);
        saveFilter(loConfig);

        if (!migratedFrom21)
            org.geoserver.util.IOUtils.copy(
                    security().get("config.xml").in(), oldSecManagerConfig.out());
        SecurityManagerConfig config = loadSecurityConfig();
        for (RequestFilterChain chain : config.getFilterChain().getRequestChains()) {
            if (chain.getFilterNames()
                    .contains(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER)) {
                chain.setAllowSessionCreation(true);
                chain.getFilterNames()
                        .remove(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER);
            }
            if (chain.getFilterNames()
                    .contains(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER)) {
                chain.setAllowSessionCreation(false);
                chain.getFilterNames()
                        .remove(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER);
            }
            // prepare web chain
            if (GeoServerSecurityFilterChain.WEB_CHAIN_NAME.equals(chain.getName())) {
                // replace exception translation filter
                int index =
                        chain.getFilterNames()
                                .indexOf(
                                        GeoServerSecurityFilterChain
                                                .GUI_EXCEPTION_TRANSLATION_FILTER);
                if (index != -1)
                    chain.getFilterNames()
                            .set(
                                    index,
                                    GeoServerSecurityFilterChain
                                            .DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
                // inject form login filter if necessary
                if (chain.getFilterNames().indexOf(GeoServerSecurityFilterChain.FORM_LOGIN_FILTER)
                        == -1) {
                    index =
                            chain.getFilterNames()
                                    .indexOf(GeoServerSecurityFilterChain.ANONYMOUS_FILTER);
                    if (index == -1)
                        index =
                                chain.getFilterNames()
                                        .indexOf(
                                                GeoServerSecurityFilterChain
                                                        .FILTER_SECURITY_INTERCEPTOR);
                    if (index != -1)
                        chain.getFilterNames()
                                .add(index, GeoServerSecurityFilterChain.FORM_LOGIN_FILTER);
                }
            }

            // remove dynamic translation filter
            chain.getFilterNames()
                    .remove(GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
            chain.getFilterNames().remove(GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR);
            chain.getFilterNames()
                    .remove(GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR);
        }
        // gui filter not needed any more
        removeFilter(
                loadFilterConfig(
                        GeoServerSecurityFilterChain.GUI_EXCEPTION_TRANSLATION_FILTER, true));
        saveSecurityConfig(config);

        // load and store all filter configuration
        // some filter configurations may have their class name as top level xml element in
        // config.xml,
        // the alias should be used instead, this was bug fixed during GSIP 82
        if (!migratedFrom21) {
            for (String fName : listFilters()) {
                SecurityFilterConfig fConfig = loadFilterConfig(fName, true);
                if (fConfig != null) saveFilter(fConfig);
            }
        }

        return true;
    }

    /**
     * converts an 2.3.x security configuration to 2.4.x
     *
     * @return <code>true</code> if migration has taken place
     */
    boolean migrateFrom23() throws Exception {
        SecurityManagerConfig config = loadSecurityConfig();
        RequestFilterChain webChain =
                config.getFilterChain()
                        .getRequestChainByName(GeoServerSecurityFilterChain.WEB_CHAIN_NAME);

        boolean migrated = false;
        List<String> patterns = webChain.getPatterns();
        if (patterns.contains("/") == false) {
            patterns.add("/");
            saveSecurityConfig(config);
            migrated |= true;
        }
        return migrated;
    }

    /**
     * Remove erroneous access denied page (HTTP) 403 (see GEOS-4943) The page /accessDeniedPage
     * does not exist and would not work if it exists.
     */
    void removeErroneousAccessDeniedPage() throws Exception {

        ExceptionTranslationFilterConfig config =
                (ExceptionTranslationFilterConfig)
                        loadFilterConfig(
                                GeoServerSecurityFilterChain.DYNAMIC_EXCEPTION_TRANSLATION_FILTER,
                                true);
        if (config != null && "/accessDenied.jsp".equals(config.getAccessDeniedErrorPage())) {
            config.setAccessDeniedErrorPage(null);
            saveFilter(config);
        }

        config =
                (ExceptionTranslationFilterConfig)
                        loadFilterConfig(
                                GeoServerSecurityFilterChain.GUI_EXCEPTION_TRANSLATION_FILTER,
                                true);
        if (config != null && "/accessDenied.jsp".equals(config.getAccessDeniedErrorPage())) {
            config.setAccessDeniedErrorPage(null);
            saveFilter(config);
        }
    }

    /*
     * looks up security plugins
     */
    public List<GeoServerSecurityProvider> lookupSecurityProviders() {
        List<GeoServerSecurityProvider> list = new ArrayList<>();
        if (appContext != null) {
            for (GeoServerSecurityProvider provider :
                    GeoServerExtensions.extensions(GeoServerSecurityProvider.class, appContext)) {
                if (!provider.isAvailable()) {
                    continue;
                }
                list.add(provider);
            }
        }
        return list;
    }

    /*
     * list files in a directory.
     */
    SortedSet<String> listFiles(Resource dir) {
        SortedSet<String> result = new TreeSet<>();
        List<Resource> dirs = dir.list();
        for (Resource d : dirs) {
            if (d.getType() == Type.DIRECTORY
                    && d.get(CONFIG_FILENAME).getType() == Type.RESOURCE) {
                result.add(d.name());
            }
        }
        return result;
    }

    XStreamPersister globalPersister() throws IOException {
        if (gxp == null) {
            gxp = buildGlobalPersister();
        }
        return gxp;
    }

    private XStreamPersister buildGlobalPersister() {
        XStreamPersister xp = buildPersister();
        xp.getXStream().alias("security", SecurityManagerConfig.class);
        xp.getXStream().alias("masterPassword", MasterPasswordConfig.class);
        xp.getXStream()
                .registerLocalConverter(
                        SecurityManagerConfig.class,
                        "filterChain",
                        new FilterChainConverter(xp.getXStream().getMapper()));

        // The field anonymousAuth is deprecated
        xp.getXStream().omitField(SecurityManagerConfig.class, "anonymousAuth");
        return xp;
    }

    /*
     * creates the persister for security plugin configuration.
     */
    XStreamPersister persister() throws IOException {
        // we should build this when the Spring context is setup, but some tests are
        // not using Spring
        if (xp == null) {
            xp = buildPersister();
        }
        return xp;
    }

    private XStreamPersister buildPersister() {
        List<GeoServerSecurityProvider> all = lookupSecurityProviders();

        // create and configure an xstream persister to load the configuration files
        XStreamPersister xp = new XStreamPersisterFactory().createXMLPersister();
        xp.getXStream().alias("security", SecurityManagerConfig.class);

        for (GeoServerSecurityProvider roleService : all) {
            roleService.configure(xp);
        }
        return xp;
    }

    /*
     * loads the global security config
     */
    public SecurityManagerConfig loadSecurityConfig() throws IOException {
        return (SecurityManagerConfig) loadConfigFile(security(), globalPersister(), true);
    }

    /*
     * loads the master password config
     */
    public MasterPasswordConfig loadMasterPasswordConfig() throws IOException {
        Resource resource = security().get(MASTER_PASSWD_CONFIG_FILENAME);
        return loadConfig(MasterPasswordConfig.class, resource, globalPersister());
    }

    /** reads a config file from the specified directly using the specified xstream persister */
    <T extends SecurityConfig> T loadConfig(Class<T> config, Resource resource, XStreamPersister xp)
            throws IOException {
        try (InputStream in = resource.in()) {
            Object loaded = xp.load(in, SecurityConfig.class).clone(true);
            return config.cast(loaded);
        }
    }
    /** reads a config file from the specified directly using the specified xstream persister */
    SecurityConfig loadConfigFile(
            Resource directory,
            String filename,
            XStreamPersister xp,
            boolean allowEnvParametrization)
            throws IOException {
        try (InputStream fin = directory.get(filename).in()) {
            return xp.load(fin, SecurityConfig.class).clone(allowEnvParametrization);
        }
    }

    /**
     * reads a file named {@value #CONFIG_FILENAME} from the specified directly using the specified
     * xstream persister
     */
    SecurityConfig loadConfigFile(
            Resource directory, XStreamPersister xp, boolean allowEnvParametrization)
            throws IOException {
        return loadConfigFile(directory, CONFIG_FILENAME, xp, allowEnvParametrization);
    }

    /** saves a config file to the specified directly using the specified xstream persister */
    void saveConfigFile(
            SecurityConfig config, Resource directory, String filename, XStreamPersister xp)
            throws IOException {
        xStreamPersist(directory.get(filename), config, xp);
    }

    /**
     * saves a file named {@value #CONFIG_FILENAME} from the specified directly using the specified
     * xstream persister
     */
    void saveConfigFile(SecurityConfig config, Resource directory, XStreamPersister xp)
            throws IOException {

        saveConfigFile(config, directory, CONFIG_FILENAME, xp);
    }

    abstract class HelperBase<T, C extends SecurityNamedServiceConfig> {
        /*
         * list of file watchers
         * TODO: we should probably manage these better rather than just throwing them in a
         * list, repeated loads will cause this list to fill up with threads
         */
        protected List<FileWatcher> fileWatchers = new ArrayList<>();

        public abstract T load(String name) throws IOException;

        /** loads the named entity config from persistence */
        public C loadConfig(
                String name, MigrationHelper migrationHelper, boolean allowEnvParametrization)
                throws IOException {
            Resource dir = getRoot().get(name);
            if (dir.getType() != Type.DIRECTORY) {
                return null;
            }

            XStreamPersister xp = persister();
            if (migrationHelper != null) {
                migrationHelper.migrationPersister(xp);
            }
            @SuppressWarnings("unchecked")
            C config = (C) loadConfigFile(dir, xp, allowEnvParametrization);
            return config;
        }

        /** loads the named entity config from persistence */
        public C loadConfig(String name, boolean allowEnvParametrization) throws IOException {
            return loadConfig(name, null, allowEnvParametrization);
        }

        /** saves the user group service config to persistence */
        public void saveConfig(SecurityNamedServiceConfig config) throws IOException {
            Resource dir = getRoot().get(config.getName());

            boolean isNew = config.getId() == null;
            if (isNew) {
                config.setId(newId());
            }
            try {
                saveConfigFile(config, dir, persister());
            } catch (Exception e) {
                // catch exception, if the config was new, clear out the id since it was not added
                if (isNew) {
                    config.setId(null);
                }
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException(e);
            }
        }

        String newId() {
            return new UID().toString();
        }

        /** removes the user group service config from persistence */
        public void removeConfig(String name) throws IOException {
            getRoot().get(name).delete();
        }

        public void destroy() {
            for (FileWatcher fw : fileWatchers) {
                fw.setTerminate(true);
            }
        }

        /** config root */
        protected abstract Resource getRoot() throws IOException;
    }

    class UserGroupServiceHelper
            extends HelperBase<GeoServerUserGroupService, SecurityUserGroupServiceConfig> {
        @Override
        public GeoServerUserGroupService load(String name) throws IOException {

            SecurityNamedServiceConfig config = loadConfig(name, true);
            if (config == null) {
                // no such config
                return null;
            }

            // look up the service for this config
            GeoServerUserGroupService service = null;

            for (GeoServerSecurityProvider p : lookupSecurityProviders()) {
                if (p.getUserGroupServiceClass() == null) {
                    continue;
                }
                if (p.getUserGroupServiceClass().getName().equals(config.getClassName())) {
                    service = p.createUserGroupService(config);
                    break;
                }
            }

            if (service == null) {
                throw new IOException("No user group service matching config: " + config);
            }

            service.setSecurityManager(GeoServerSecurityManager.this);
            if (config instanceof SecurityUserGroupServiceConfig) {
                boolean needsLockProtection =
                        GeoServerSecurityProvider.getProvider(
                                        GeoServerUserGroupService.class, config.getClassName())
                                .roleServiceNeedsLockProtection();
                if (needsLockProtection) service = new LockingUserGroupService(service);
            }
            service.setName(name);
            service.initializeFromConfig(config);

            if (config instanceof FileBasedSecurityServiceConfig) {
                FileBasedSecurityServiceConfig fileConfig = (FileBasedSecurityServiceConfig) config;
                if (fileConfig.getCheckInterval() > 0) {
                    Resource resource = getConfigFile(fileConfig.getFileName());
                    if (resource == null) {
                        String path =
                                Paths.path("security/usergroup", name, fileConfig.getFileName());
                        resource = get(path);
                    }

                    UserGroupFileWatcher watcher = new UserGroupFileWatcher(resource, service);
                    service.registerUserGroupLoadedListener(watcher);
                    watcher.start();

                    // register the watcher so we can kill it later on disposale
                    fileWatchers.add(watcher);
                }
            }

            return service;
        }

        @Override
        protected Resource getRoot() throws IOException {
            return userGroup();
        }
    }

    class RoleServiceHelper extends HelperBase<GeoServerRoleService, SecurityRoleServiceConfig> {

        /** Loads the role service for the named config from persistence. */
        @Override
        public GeoServerRoleService load(String name) throws IOException {

            SecurityNamedServiceConfig config = loadConfig(name, true);
            if (config == null) {
                // no such config
                return null;
            }

            // look up the service for this config
            GeoServerRoleService service = null;

            for (GeoServerSecurityProvider p : lookupSecurityProviders()) {
                if (p.getRoleServiceClass() == null) {
                    continue;
                }
                if (p.getRoleServiceClass().getName().equals(config.getClassName())) {
                    service = p.createRoleService(config);
                    break;
                }
            }

            if (service == null) {
                throw new IOException("No authority service matching config: " + config);
            }
            service.setSecurityManager(GeoServerSecurityManager.this);

            if (config instanceof SecurityRoleServiceConfig) {
                boolean needsLockProtection =
                        GeoServerSecurityProvider.getProvider(
                                        GeoServerRoleService.class, config.getClassName())
                                .roleServiceNeedsLockProtection();
                if (needsLockProtection) {
                    service = new LockingRoleService(service);
                }
            }

            service.setName(name);

            // TODO: do we need this anymore?
            service.initializeFromConfig(config);

            if (config instanceof FileBasedSecurityServiceConfig) {
                FileBasedSecurityServiceConfig fileConfig = (FileBasedSecurityServiceConfig) config;
                if (fileConfig.getCheckInterval() > 0) {
                    Resource resource = getConfigFile(fileConfig.getFileName());
                    if (resource == null) {
                        String path = Paths.path("security/role", name, fileConfig.getFileName());
                        resource = get(path);
                    }

                    RoleFileWatcher watcher =
                            new RoleFileWatcher(resource, service, resource.lastmodified());
                    service.registerRoleLoadedListener(watcher);
                    watcher.start();

                    // register the watcher so we can kill it later
                    fileWatchers.add(watcher);
                }
            }

            return service;
        }

        @Override
        protected Resource getRoot() throws IOException {
            return role();
        }
    }

    /**
     * Alternative to {@link GeoServerResourceLoader#find(String)} that supports absolute paths for
     * use in test cases.
     *
     * <p>If an absolute path is used the Resource implementation is provided by {@link
     * Files#asResource(File)}.
     *
     * @return resource
     */
    Resource getConfigFile(String configFileLocation) throws IOException {
        File file = new File(configFileLocation);
        if (file.isAbsolute()) {
            if (file.canRead()) {
                return Files.asResource(file); // used by test cases
            } else {
                throw new IOException("Cannot read file: " + file.getCanonicalPath());
            }
        }
        return null;
    }

    class PasswordValidatorHelper extends HelperBase<PasswordValidator, PasswordPolicyConfig> {

        /** Loads the password policy for the named config from persistence. */
        @Override
        public PasswordValidator load(String name) throws IOException {

            PasswordPolicyConfig config = loadConfig(name, true);
            if (config == null) {
                // no such config
                return null;
            }

            // look up the validator for this config
            PasswordValidator validator = null;

            for (GeoServerSecurityProvider p : lookupSecurityProviders()) {
                if (p.getPasswordValidatorClass() == null) {
                    continue;
                }
                if (p.getPasswordValidatorClass().getName().equals(config.getClassName())) {
                    validator = p.createPasswordValidator(config, GeoServerSecurityManager.this);
                    break;
                }
            }
            if (validator == null) {
                throw new IOException("No password policy matching config: " + config);
            }

            validator.setConfig(config);
            return validator;
        }

        @Override
        protected Resource getRoot() throws IOException {
            return passwordPolicy();
        }
    }

    class MasterPasswordProviderHelper
            extends HelperBase<MasterPasswordProvider, MasterPasswordProviderConfig> {

        @Override
        public MasterPasswordProvider load(String name) throws IOException {
            MasterPasswordProviderConfig config = loadConfig(name, true);
            if (config == null) {
                return null;
            }

            // look up the provider for this config
            MasterPasswordProvider provider = null;

            for (GeoServerSecurityProvider p : lookupSecurityProviders()) {
                if (p.getMasterPasswordProviderClass() == null) {
                    continue;
                }
                if (p.getMasterPasswordProviderClass().getName().equals(config.getClassName())) {
                    provider = p.createMasterPasswordProvider(config);
                    break;
                }
            }
            if (provider == null) {
                throw new IOException("No master password provider matching config: " + config);
            }

            // ensure that the provider is a final class
            if (!Modifier.isFinal(provider.getClass().getModifiers())) {
                throw new RuntimeException(
                        "Master password provider class: "
                                + provider.getClass().getCanonicalName()
                                + " is not final");
            }

            provider.setName(config.getName());
            provider.setSecurityManager(GeoServerSecurityManager.this);
            provider.initializeFromConfig(config);
            return provider;
        }

        @Override
        protected Resource getRoot() throws IOException {
            return masterPasswordProvider();
        }
    }

    /** @return the active {@link GeoServerRoleService} */
    public GeoServerRoleService getActiveRoleService() {
        try {
            return wrapRoleService(activeRoleService);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** set the active {@link GeoServerRoleService} */
    public void setActiveRoleService(GeoServerRoleService activeRoleService) {
        this.activeRoleService = activeRoleService;
    }

    /**
     * rewrites configuration files with encrypted fields. Candidates: {@link StoreInfo} from the
     * {@link Catalog} {@link SecurityNamedServiceConfig} objects from the security directory
     */
    public void updateConfigurationFilesWithEncryptedFields() throws IOException {
        // rewrite stores in catalog
        LOGGER.info(
                "Start encrypting configuration passwords using "
                        + getSecurityConfig().getConfigPasswordEncrypterName());

        Catalog catalog = getCatalog();
        List<StoreInfo> stores = catalog.getStores(StoreInfo.class);
        for (StoreInfo info : stores) {
            if (!configPasswordEncryptionHelper.getEncryptedFields(info).isEmpty()) {
                catalog.save(info);
            }
        }

        Set<Class<?>> configClasses = new HashSet<>();

        // filter the interesting classes ones
        for (GeoServerSecurityProvider prov : lookupSecurityProviders()) {
            configClasses.addAll(prov.getFieldsForEncryption().keySet());
        }

        for (String name : listPasswordValidators()) {
            PasswordPolicyConfig config = passwordValidatorHelper.loadConfig(name, true);
            for (Class<?> classWithEncryption : configClasses) {
                if (config.getClass().isAssignableFrom(classWithEncryption)) {
                    passwordValidatorHelper.saveConfig(config);
                    break;
                }
            }
        }
        for (String name : listRoleServices()) {
            SecurityNamedServiceConfig config = roleServiceHelper.loadConfig(name, true);
            for (Class<?> classWithEncryption : configClasses) {
                if (config.getClass().isAssignableFrom(classWithEncryption)) {
                    roleServiceHelper.saveConfig(config);
                    break;
                }
            }
        }
        for (String name : listUserGroupServices()) {
            SecurityNamedServiceConfig config = userGroupServiceHelper.loadConfig(name, true);
            for (Class<?> classWithEncryption : configClasses) {
                if (config.getClass().isAssignableFrom(classWithEncryption)) {
                    userGroupServiceHelper.saveConfig(config);
                    break;
                }
            }
        }

        for (String name : listAuthenticationProviders()) {
            SecurityNamedServiceConfig config = authProviderHelper.loadConfig(name, true);
            for (Class<?> classWithEncryption : configClasses) {
                if (config.getClass().isAssignableFrom(classWithEncryption)) {
                    authProviderHelper.saveConfig(config);
                    break;
                }
            }
        }

        for (String name : listFilters()) {
            SecurityNamedServiceConfig config = filterHelper.loadConfig(name, true);
            for (Class<?> classWithEncryption : configClasses) {
                if (config.getClass().isAssignableFrom(classWithEncryption)) {
                    filterHelper.saveConfig(config);
                    break;
                }
            }
        }
        LOGGER.info("End encrypting configuration passwords");
    }

    /**
     * Interface that can be used to assist migration phases, adding XStream behaviours to be used
     * only during migration of configurations from previous versions. A specific implementation can
     * be passed to {@link FilterHelper#loadConfig(String)} and/or {@link
     * FilterHelper#saveConfig(SecurityNamedServiceConfig)} to change XStream mappings and
     * conversions to allow loading of old (incompatible) configuration files that need to be
     * updated to a new format. The implementation should implement the migrationPersister method to
     * add aliases, converters or other XStream behaviours needed only when migrating old
     * configurations.
     *
     * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
     */
    interface MigrationHelper {
        /**
         * Implement here XStream mappings and conversion behaviours needed to read incompatible
         * configurations during migration.
         */
        public void migrationPersister(XStreamPersister xp);
    }

    class AuthProviderHelper
            extends HelperBase<GeoServerAuthenticationProvider, SecurityAuthProviderConfig> {

        /** Loads the auth provider for the named config from persistence. */
        @Override
        public GeoServerAuthenticationProvider load(String name) throws IOException {

            SecurityNamedServiceConfig config = loadConfig(name, true);
            if (config == null) {
                // no such config
                return null;
            }

            // look up the service for this config
            GeoServerAuthenticationProvider authProvider = null;

            for (GeoServerSecurityProvider p : lookupSecurityProviders()) {
                if (p.getAuthenticationProviderClass() == null) {
                    continue;
                }
                if (p.getAuthenticationProviderClass().getName().equals(config.getClassName())) {
                    authProvider = p.createAuthenticationProvider(config);
                    break;
                }
            }

            if (authProvider == null) {
                throw new IOException("No authentication provider matching config: " + config);
            }

            authProvider.setName(name);
            authProvider.setSecurityManager(GeoServerSecurityManager.this);
            authProvider.initializeFromConfig(config);

            return authProvider;
        }

        @Override
        protected Resource getRoot() throws IOException {
            return auth();
        }
    }

    class FilterHelper extends HelperBase<GeoServerSecurityFilter, SecurityFilterConfig> {
        /** Loads the filter for the named config from persistence. */
        @Override
        public GeoServerSecurityFilter load(String name) throws IOException {

            SecurityNamedServiceConfig config = loadConfig(name, true);
            if (config == null) {
                // no such config
                return null;
            }

            // look up the service for this config
            GeoServerSecurityFilter filter = null;

            for (GeoServerSecurityProvider p : lookupSecurityProviders()) {
                if (p.getFilterClass() == null) {
                    continue;
                }
                if (p.getFilterClass().getName().equals(config.getClassName())) {
                    filter = p.createFilter(config);
                    break;
                }
            }

            if (filter == null) {
                throw new IOException("No authentication provider matching config: " + config);
            }

            filter.setName(name);
            filter.setSecurityManager(GeoServerSecurityManager.this);
            filter.initializeFromConfig(config);

            return filter;
        }

        @Override
        protected Resource getRoot() throws IOException {
            return filterRoot();
        }
    }

    /** custom converter for filter chain */
    class FilterChainConverter extends AbstractCollectionConverter {

        public FilterChainConverter(Mapper mapper) {
            super(mapper);
        }

        @Override
        public boolean canConvert(Class type) {
            return GeoServerSecurityFilterChain.class.isAssignableFrom(type);
        }

        @Override
        public void marshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

            GeoServerSecurityFilterChain filterChain = (GeoServerSecurityFilterChain) source;
            for (RequestFilterChain requestChain : filterChain.getRequestChains()) {
                // <filterChain>
                //  <filters path="..." index="...">
                //    <filter>name1</filter>
                //    <filter>name2</filter>
                //    ...
                writer.startNode("filters");

                StringBuilder sb = new StringBuilder();
                for (String s : requestChain.getPatterns()) {
                    sb.append(s).append(",");
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                }

                if (requestChain.getName() != null) {
                    writer.addAttribute("name", requestChain.getName());
                }
                writer.addAttribute("class", requestChain.getClass().getName());
                if (StringUtils.hasLength(requestChain.getRoleFilterName()))
                    writer.addAttribute("roleFilterName", requestChain.getRoleFilterName());

                if (requestChain instanceof VariableFilterChain) {
                    if (StringUtils.hasLength(
                            ((VariableFilterChain) requestChain).getInterceptorName()))
                        writer.addAttribute(
                                "interceptorName",
                                ((VariableFilterChain) requestChain).getInterceptorName());
                    if (StringUtils.hasLength(
                            ((VariableFilterChain) requestChain).getExceptionTranslationName()))
                        writer.addAttribute(
                                "exceptionTranslationName",
                                ((VariableFilterChain) requestChain).getExceptionTranslationName());
                }

                writer.addAttribute("path", sb.toString());
                writer.addAttribute("disabled", Boolean.toString(requestChain.isDisabled()));
                writer.addAttribute(
                        "allowSessionCreation",
                        Boolean.toString(requestChain.isAllowSessionCreation()));
                writer.addAttribute("ssl", Boolean.toString(requestChain.isRequireSSL()));
                writer.addAttribute(
                        "matchHTTPMethod", Boolean.toString(requestChain.isMatchHTTPMethod()));
                if (requestChain.getHttpMethods() != null
                        && requestChain.getHttpMethods().size() > 0) {
                    writer.addAttribute(
                            "httpMethods",
                            StringUtils.collectionToCommaDelimitedString(
                                    requestChain.getHttpMethods()));
                }

                for (String filterName : requestChain.getFilterNames()) {
                    writer.startNode("filter");
                    writer.setValue(filterName);
                    writer.endNode();
                }

                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            GeoServerSecurityFilterChain filterChain = new GeoServerSecurityFilterChain();

            while (reader.hasMoreChildren()) {

                // <filters name="..." path="..."
                reader.moveDown();

                String path = reader.getAttribute("path");
                String name = reader.getAttribute("name");
                String classname = reader.getAttribute("class");
                String roleFilterName = reader.getAttribute("roleFilterName");
                String disabledString = reader.getAttribute("disabled");
                String allowSessionCreationString = reader.getAttribute("allowSessionCreation");
                String interceptorName = reader.getAttribute("interceptorName");
                String exceptionTranslationName = reader.getAttribute("exceptionTranslationName");
                String sslString = reader.getAttribute("ssl");
                String matchHTTPMethodString = reader.getAttribute("matchHTTPMethod");
                String httpMethodsString = reader.getAttribute("httpMethods");

                if (name == null) {
                    // first version of the serialization did not contain name attribute, if not
                    // available try to look up well known chain, if not found just use the path
                    // as the name
                    RequestFilterChain requestChain =
                            GeoServerSecurityFilterChain.lookupRequestChainByPattern(
                                    path, GeoServerSecurityManager.this);
                    if (requestChain != null) {
                        name = requestChain.getName();
                    } else {
                        name = path;
                    }
                }

                // this is nasty but no other chance to migrate from GeoServer 2.2.0
                if (classname == null) {
                    if (GeoServerSecurityFilterChain.WEB_CHAIN_NAME.equals(name)) {
                        classname = HtmlLoginFilterChain.class.getName();
                        allowSessionCreationString = "true";
                        interceptorName = GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
                    }
                    if (GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME.equals(name)) {
                        classname = ConstantFilterChain.class.getName();
                        allowSessionCreationString = "true";
                        interceptorName = GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
                    }
                    if (GeoServerSecurityFilterChain.WEB_LOGOUT_CHAIN_NAME.equals(name)) {
                        classname = LogoutFilterChain.class.getName();
                        allowSessionCreationString = "true";
                        interceptorName = GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
                    }
                    if (GeoServerSecurityFilterChain.REST_CHAIN_NAME.equals(name)) {
                        classname = ServiceLoginFilterChain.class.getName();
                        allowSessionCreationString = "false";
                        interceptorName =
                                GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR;
                    }
                    if (GeoServerSecurityFilterChain.GWC_CHAIN_NAME.equals(name)) {
                        classname = ServiceLoginFilterChain.class.getName();
                        allowSessionCreationString = "false";
                        interceptorName =
                                GeoServerSecurityFilterChain.FILTER_SECURITY_REST_INTERCEPTOR;
                    }
                    if (GeoServerSecurityFilterChain.DEFAULT_CHAIN_NAME.equals(name)) {
                        classname = ServiceLoginFilterChain.class.getName();
                        allowSessionCreationString = "false";
                        interceptorName = GeoServerSecurityFilterChain.FILTER_SECURITY_INTERCEPTOR;
                    }
                }

                // <filter
                ArrayList<String> filterNames = new ArrayList<>();
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    filterNames.add(reader.getValue());
                    reader.moveUp();
                }

                RequestFilterChain requestChain = null;
                try {
                    Class<?> chainClass = Class.forName(classname);
                    Constructor<?> cons = chainClass.getConstructor(new Class[] {String[].class});
                    String[] args = path.split(",");
                    requestChain = (RequestFilterChain) cons.newInstance(new Object[] {args});
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                requestChain.setName(name);
                if (StringUtils.hasLength(disabledString)) {
                    requestChain.setDisabled(Boolean.parseBoolean(disabledString));
                }
                if (StringUtils.hasLength(allowSessionCreationString)) {
                    requestChain.setAllowSessionCreation(
                            Boolean.parseBoolean(allowSessionCreationString));
                }
                if (StringUtils.hasLength(sslString)) {
                    requestChain.setRequireSSL(Boolean.parseBoolean(sslString));
                }
                if (StringUtils.hasLength(matchHTTPMethodString)) {
                    requestChain.setMatchHTTPMethod(Boolean.parseBoolean(matchHTTPMethodString));
                }
                if (StringUtils.hasLength(httpMethodsString)) {
                    for (String method : httpMethodsString.split(",")) {
                        requestChain.getHttpMethods().add(HTTPMethod.fromString(method));
                    }
                }

                requestChain.setRoleFilterName(roleFilterName);

                if (requestChain instanceof VariableFilterChain) {
                    ((VariableFilterChain) requestChain).setInterceptorName(interceptorName);
                    if (StringUtils.hasLength(exceptionTranslationName))
                        ((VariableFilterChain) requestChain)
                                .setExceptionTranslationName(exceptionTranslationName);
                    else
                        ((VariableFilterChain) requestChain)
                                .setExceptionTranslationName(
                                        GeoServerSecurityFilterChain
                                                .DYNAMIC_EXCEPTION_TRANSLATION_FILTER);
                }
                requestChain.setFilterNames(filterNames);
                filterChain.getRequestChains().add(requestChain);

                reader.moveUp();
            }

            // no good idea, how to split them from the gui ???
            // filterChain.simplify();
            return filterChain;
        }
    }

    /**
     * Calculates the union of roles from all role services and adds {@link
     * GeoServerRole#ANONYMOUS_ROLE} and {@link GeoServerRole#AUTHENTICATED_ROLE}
     */
    public SortedSet<GeoServerRole> getRolesForAccessControl() throws IOException {

        SortedSet<GeoServerRole> allRoles = new TreeSet<>();
        for (String serviceName : listRoleServices()) {
            // catch the IOException for each role service.
            // As an example, it does not make sense to throw an IOException if
            // a jdbc connection cannot be established.
            try {
                allRoles.addAll(loadRoleService(serviceName).getRoles());
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
        allRoles.add(GeoServerRole.AUTHENTICATED_ROLE);
        allRoles.add(GeoServerRole.ANONYMOUS_ROLE);
        return allRoles;
    }
}
