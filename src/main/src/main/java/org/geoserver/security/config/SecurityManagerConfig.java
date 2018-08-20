/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.rememberme.RememberMeServicesConfig;

/**
 * {@link GeoServerSecurityManager} configuration object.
 *
 * @author christian
 */
public class SecurityManagerConfig implements SecurityConfig {

    private static final long serialVersionUID = 1L;

    private String roleServiceName;
    private List<String> authProviderNames = new ArrayList<String>();
    private String configPasswordEncrypterName;
    private boolean encryptingUrlParams;

    private GeoServerSecurityFilterChain filterChain = new GeoServerSecurityFilterChain();
    private RememberMeServicesConfig rememberMeService = new RememberMeServicesConfig();
    private BruteForcePreventionConfig bruteForcePrevention = new BruteForcePreventionConfig();

    public SecurityManagerConfig() {}

    public SecurityManagerConfig(SecurityManagerConfig config) {
        this.roleServiceName = config.getRoleServiceName();
        this.authProviderNames =
                config.getAuthProviderNames() != null
                        ? new ArrayList<String>(config.getAuthProviderNames())
                        : null;
        this.filterChain =
                config.getFilterChain() != null
                        ? new GeoServerSecurityFilterChain(config.getFilterChain())
                        : null;
        this.rememberMeService = new RememberMeServicesConfig(config.getRememberMeService());
        this.bruteForcePrevention =
                new BruteForcePreventionConfig(config.getBruteForcePrevention());
        this.encryptingUrlParams = config.isEncryptingUrlParams();
        this.configPasswordEncrypterName = config.getConfigPasswordEncrypterName();
        // this.masterPasswordURL=config.getMasterPasswordURL();
        // this.masterPasswordStrategy=config.getMasterPasswordStrategy();
    }

    private Object readResolve() {
        authProviderNames = authProviderNames != null ? authProviderNames : new ArrayList<String>();
        filterChain = filterChain != null ? filterChain : new GeoServerSecurityFilterChain();
        rememberMeService =
                rememberMeService != null ? rememberMeService : new RememberMeServicesConfig();
        bruteForcePrevention =
                bruteForcePrevention != null
                        ? bruteForcePrevention
                        : new BruteForcePreventionConfig();
        return this;
    }

    /** Name of {@link GeoServerRoleService} object. */
    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    /** @return list of names for {@link GeoServerAuthenticationProvider} objects */
    public List<String> getAuthProviderNames() {
        return authProviderNames;
    }

    /** The security filter chain. */
    public GeoServerSecurityFilterChain getFilterChain() {
        return filterChain;
    }

    public void setFilterChain(GeoServerSecurityFilterChain filterChain) {
        this.filterChain = filterChain;
    }

    /** The remember me service. */
    public RememberMeServicesConfig getRememberMeService() {
        return rememberMeService;
    }

    public void setRememberMeService(RememberMeServicesConfig rememberMeService) {
        this.rememberMeService = rememberMeService;
    }

    public BruteForcePreventionConfig getBruteForcePrevention() {
        return bruteForcePrevention;
    }

    /** The brute force attack prevention */
    public void setBruteForcePrevention(BruteForcePreventionConfig bruteForcePrevention) {
        this.bruteForcePrevention = bruteForcePrevention;
    }

    /** Flag controlling if web admin should encrypt url parameters. */
    public boolean isEncryptingUrlParams() {
        return encryptingUrlParams;
    }

    public void setEncryptingUrlParams(boolean encryptingUrlParams) {
        this.encryptingUrlParams = encryptingUrlParams;
    }

    /** The name of the password encrypter for encrypting password in configuration files. */
    public String getConfigPasswordEncrypterName() {
        return configPasswordEncrypterName;
    }

    public void setConfigPasswordEncrypterName(String configPasswordEncrypterName) {
        this.configPasswordEncrypterName = configPasswordEncrypterName;
    }

    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {

        final GeoServerEnvironment gsEnvironment =
                GeoServerExtensions.bean(GeoServerEnvironment.class);

        SecurityManagerConfig target = (SecurityManagerConfig) SerializationUtils.clone(this);

        if (target != null) {
            if (allowEnvParametrization
                    && gsEnvironment != null
                    && GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
                target.setConfigPasswordEncrypterName(
                        (String) gsEnvironment.resolveValue(configPasswordEncrypterName));
                target.setRoleServiceName((String) gsEnvironment.resolveValue(roleServiceName));
            }
        }

        return target;
    }
}
