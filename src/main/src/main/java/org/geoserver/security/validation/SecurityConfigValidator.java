/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import static org.geoserver.security.validation.SecurityConfigException.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.HtmlLoginFilterChain;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.ServiceLoginFilterChain;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.PasswordValidator;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.util.StringUtils;

public class SecurityConfigValidator extends AbstractSecurityValidator {

    public SecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    /** Get the proper {@link SecurityConfigValidator} object */
    public static SecurityConfigValidator getConfigurationValiator(
            Class<?> serviceClass, String className) throws SecurityConfigException {
        if (className == null)
            throw new SecurityConfigException(CLASSNAME_REQUIRED, new Object[] {});

        GeoServerSecurityProvider prov =
                GeoServerSecurityProvider.getProvider(serviceClass, className);

        if (prov == null) {
            throw new SecurityConfigException(CLASS_NOT_FOUND, new Object[] {className});
        }

        // TODO: remove the call to extensions, have teh security manager be passed in
        return prov.createConfigurationValidator(
                GeoServerExtensions.bean(GeoServerSecurityManager.class));
    }

    /** Checks the {@link SecurityManagerConfig} object */
    public void validateManagerConfig(SecurityManagerConfig config, SecurityManagerConfig oldConfig)
            throws SecurityConfigException {

        String encrypterName = config.getConfigPasswordEncrypterName();
        if (isNotEmpty(encrypterName) == false) {
            throw createSecurityException(PASSWORD_ENCODER_REQUIRED);
        }

        GeoServerPasswordEncoder encoder = null;
        try {
            encoder = manager.loadPasswordEncoder(config.getConfigPasswordEncrypterName());
        } catch (NoSuchBeanDefinitionException ex) {
            throw createSecurityException(INVALID_PASSWORD_ENCODER_$1, encrypterName);
        }
        if (encoder == null) {
            throw createSecurityException(INVALID_PASSWORD_ENCODER_$1, encrypterName);
        }

        if (!encoder.isReversible()) {
            throw createSecurityException(INVALID_PASSWORD_ENCODER_$1, encrypterName);
        }

        if (!manager.isStrongEncryptionAvailable()) {
            if (encoder != null && encoder.isAvailableWithoutStrongCryptogaphy() == false) {
                throw createSecurityException(INVALID_STRONG_CONFIG_PASSWORD_ENCODER);
            }
        }

        String roleServiceName = config.getRoleServiceName();
        if (roleServiceName == null) roleServiceName = "";

        try {
            if (manager.listRoleServices().contains(roleServiceName) == false)
                throw createSecurityException(ROLE_SERVICE_NOT_FOUND_$1, roleServiceName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SortedSet<String> authProviders = null;
        try {
            authProviders = manager.listAuthenticationProviders();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String authProvName : config.getAuthProviderNames()) {
            if (authProviders.contains(authProvName) == false)
                throw createSecurityException(AUTH_PROVIDER_NOT_FOUND_$1, authProvName);
        }

        // check the filter chain

        GeoServerSecurityFilterChain chain = config.getFilterChain();
        GeoServerSecurityFilterChain oldChain = oldConfig.getFilterChain();
        if (chain == null) {
            throw createSecurityException(SecurityConfigException.FILTER_CHAIN_NULL_ERROR);
        }

        // check for remove
        for (RequestFilterChain oldRequestChain : oldChain.getRequestChains()) {
            if (chain.getRequestChainByName(oldRequestChain.getName()) == null) {
                if (oldRequestChain.canBeRemoved() == false) {
                    throw createSecurityException(
                            SecurityConfigException.FILTER_CHAIN_NOT_REMOVEABLE_$1,
                            oldRequestChain.getName());
                }
            }
        }
        // check for unique chain names
        for (RequestFilterChain requestChain : chain.getRequestChains()) {
            Set<String> chainNames = new HashSet<String>();
            // valid name
            if (isNotEmpty(requestChain.getName()) == false) {
                throw createSecurityException(SecurityConfigException.FILTER_CHAIN_NAME_MANDATORY);
            }
            if (chainNames.contains(requestChain.getName())) {
                throw createSecurityException(
                        SecurityConfigException.FILTER_CHAIN_NAME_NOT_UNIQUE_$1,
                        requestChain.getName());
            }
            chainNames.add(requestChain.getName());
        }

        for (RequestFilterChain requestChain : chain.getRequestChains()) {
            validateRequestFilterChain(requestChain);
        }
    }

    public void validateRequestFilterChain(RequestFilterChain requestChain)
            throws SecurityConfigException {
        if (isNotEmpty(requestChain.getName()) == false) {
            throw createSecurityException(SecurityConfigException.FILTER_CHAIN_NAME_MANDATORY);
        }

        if (requestChain.getPatterns().isEmpty()) {
            throw createSecurityException(
                    SecurityConfigException.PATTERN_LIST_EMPTY_$1, requestChain.getName());
        }

        GeoServerSecurityFilterChainProxy proxy =
                GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);

        String roleFilterName = requestChain.getRoleFilterName();
        if (StringUtils.hasLength(roleFilterName)) {
            try {
                if (proxy.lookupFilter(roleFilterName) == null) {
                    throw createSecurityException(
                            SecurityConfigException.UNKNOWN_ROLE_FILTER_$2,
                            requestChain.getName(),
                            roleFilterName);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        if (requestChain instanceof VariableFilterChain) {
            if (requestChain.isDisabled() == false && requestChain.getFilterNames().isEmpty())
                throw createSecurityException(
                        SecurityConfigException.FILTER_CHAIN_EMPTY_$1, requestChain.getName());

            String interceptorFilterName =
                    ((VariableFilterChain) requestChain).getInterceptorName();
            if (StringUtils.hasLength(interceptorFilterName)) {
                try {
                    if (proxy.lookupFilter(interceptorFilterName) == null) {
                        throw createSecurityException(
                                SecurityConfigException.UNKNOWN_INTERCEPTOR_FILTER_$2,
                                requestChain.getName(),
                                interceptorFilterName);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw createSecurityException(
                        SecurityConfigException.INTERCEPTOR_FILTER_MANDATORY_$1,
                        requestChain.getName());
            }

            String exceptionTranslationName =
                    ((VariableFilterChain) requestChain).getExceptionTranslationName();
            if (StringUtils.hasLength(exceptionTranslationName)) {
                try {
                    if (proxy.lookupFilter(exceptionTranslationName) == null) {
                        throw createSecurityException(
                                SecurityConfigException.UNKNOWN_EXCEPTION_FILTER_$2,
                                requestChain.getName(),
                                exceptionTranslationName);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                throw createSecurityException(
                        SecurityConfigException.EXCEPTION_FILTER_MANDATORY_$1,
                        requestChain.getName());
            }

            int index =
                    requestChain
                            .getFilterNames()
                            .indexOf(GeoServerSecurityFilterChain.ANONYMOUS_FILTER);
            if (index != -1 && index != requestChain.getFilterNames().size() - 1)
                throw createSecurityException(
                        SecurityConfigException.ANONYMOUS_NOT_LAST_$1, requestChain.getName());

            for (String filterName : requestChain.getFilterNames()) {
                GeoServerSecurityFilter filter = null;
                try {
                    filter = (GeoServerSecurityFilter) proxy.lookupFilter(filterName);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                if (filter == null)
                    throw createSecurityException(
                            SecurityConfigException.UNKNOWN_FILTER_$2,
                            requestChain.getName(),
                            filterName);
                if (filter instanceof GeoServerAuthenticationFilter == false)
                    throw createSecurityException(
                            SecurityConfigException.NOT_AN_AUTHENTICATION_FILTER_$2,
                            requestChain.getName(),
                            filterName);
                GeoServerAuthenticationFilter authFilter = (GeoServerAuthenticationFilter) filter;

                if (requestChain instanceof HtmlLoginFilterChain
                        && authFilter.applicableForHtml() == false) {
                    throw createSecurityException(
                            SecurityConfigException.NOT_A_HTML_AUTHENTICATION_FILTER_$2,
                            requestChain.getName(),
                            filterName);
                }
                if (requestChain instanceof ServiceLoginFilterChain
                        && authFilter.applicableForServices() == false) {
                    throw createSecurityException(
                            SecurityConfigException.NOT_A_SERVICE_AUTHENTICATION_FILTER_$2,
                            requestChain.getName(),
                            filterName);
                }
            }
        }
    }

    protected void checkExtensionPont(Class<?> extensionPoint, String className)
            throws SecurityConfigException {
        if (isNotEmpty(className) == false) {
            throw createSecurityException(CLASSNAME_REQUIRED);
        }
        Class<?> aClass = null;
        try {
            aClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw createSecurityException(CLASS_NOT_FOUND_$1, className);
        }

        if (extensionPoint.isAssignableFrom(aClass) == false) {
            throw createSecurityException(CLASS_WRONG_TYPE_$2, extensionPoint, className);
        }
    }

    protected void checkServiceName(Class<?> extensionPoint, String name)
            throws SecurityConfigException {
        if (name == null || name.isEmpty()) throw createSecurityException(NAME_REQUIRED);
    }

    protected SortedSet<String> getNamesFor(Class<?> extensionPoint) {
        try {
            if (extensionPoint == GeoServerUserGroupService.class)
                return manager.listUserGroupServices();
            if (extensionPoint == GeoServerRoleService.class) return manager.listRoleServices();
            if (extensionPoint == GeoServerAuthenticationProvider.class)
                return manager.listAuthenticationProviders();
            if (extensionPoint == AuthenticationProvider.class)
                return manager.listAuthenticationProviders();
            if (extensionPoint == GeoServerSecurityFilter.class) return manager.listFilters();
            if (extensionPoint == PasswordValidator.class) return manager.listPasswordValidators();
            if (extensionPoint == MasterPasswordProvider.class) {
                return manager.listMasterPasswordProviders();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Unkwnown extension point: " + extensionPoint.getName());
    }

    public void validateAddNamedService(Class<?> extensionPoint, SecurityNamedServiceConfig config)
            throws SecurityConfigException {
        checkExtensionPont(extensionPoint, config.getClassName());
        checkServiceName(extensionPoint, config.getName());
        SortedSet<String> names = getNamesFor(extensionPoint);
        if (names.contains(config.getName()))
            throw createSecurityException(alreadyExistsErrorCode(extensionPoint), config.getName());
    }

    public void validateModifiedNamedService(
            Class<?> extensionPoint, SecurityNamedServiceConfig config)
            throws SecurityConfigException {
        checkExtensionPont(extensionPoint, config.getClassName());
        checkServiceName(extensionPoint, config.getName());
        SortedSet<String> names = getNamesFor(extensionPoint);
        if (names.contains(config.getName()) == false)
            throw createSecurityException(notFoundErrorCode(extensionPoint), config.getName());
    }

    public void validateRemoveNamedService(
            Class<?> extensionPoint, SecurityNamedServiceConfig config)
            throws SecurityConfigException {
        checkServiceName(extensionPoint, config.getName());
    }

    public void validateAddUserGroupService(SecurityUserGroupServiceConfig config)
            throws SecurityConfigException {
        validateAddNamedService(GeoServerUserGroupService.class, config);
        validate(config);
    }

    public void validateAddRoleService(SecurityRoleServiceConfig config)
            throws SecurityConfigException {
        validateAddNamedService(GeoServerRoleService.class, config);
        validate(config);
    }

    public void validateAddPasswordPolicy(PasswordPolicyConfig config)
            throws SecurityConfigException {
        validateAddNamedService(PasswordValidator.class, config);
        validate(config);
    }

    public void validateAddAuthProvider(SecurityAuthProviderConfig config)
            throws SecurityConfigException {
        validateAddNamedService(GeoServerAuthenticationProvider.class, config);
        validate(config);
    }

    public void validateAddFilter(SecurityNamedServiceConfig config)
            throws SecurityConfigException {
        validateAddNamedService(GeoServerSecurityFilter.class, config);
    }

    public void validateAddMasterPasswordProvider(MasterPasswordProviderConfig config)
            throws SecurityConfigException {
        validateAddNamedService(MasterPasswordProvider.class, config);
        validate(config);
    }

    public void validateModifiedUserGroupService(
            SecurityUserGroupServiceConfig config, SecurityUserGroupServiceConfig oldConfig)
            throws SecurityConfigException {
        validateModifiedNamedService(GeoServerUserGroupService.class, config);
        validate(config);
    }

    public void validateModifiedRoleService(
            SecurityRoleServiceConfig config, SecurityRoleServiceConfig oldConfig)
            throws SecurityConfigException {
        validateModifiedNamedService(GeoServerRoleService.class, config);
        validate(config);
    }

    public void validateModifiedPasswordPolicy(
            PasswordPolicyConfig config, PasswordPolicyConfig oldConfig)
            throws SecurityConfigException {
        validateModifiedNamedService(PasswordValidator.class, config);
        validate(config);
    }

    public void validateModifiedAuthProvider(
            SecurityAuthProviderConfig config, SecurityAuthProviderConfig oldconfig)
            throws SecurityConfigException {
        validateModifiedNamedService(GeoServerAuthenticationProvider.class, config);
        validate(config);
    }

    public void validateModifiedFilter(
            SecurityNamedServiceConfig config, SecurityNamedServiceConfig oldConfig)
            throws SecurityConfigException {
        validateModifiedNamedService(GeoServerSecurityFilter.class, config);
    }

    public void validateModifiedMasterPasswordProvider(
            MasterPasswordProviderConfig config, MasterPasswordProviderConfig oldConfig)
            throws SecurityConfigException {
        validateModifiedNamedService(MasterPasswordProvider.class, config);
        validate(config);
    }

    public void validateRemoveUserGroupService(SecurityUserGroupServiceConfig config)
            throws SecurityConfigException {
        validateRemoveNamedService(GeoServerUserGroupService.class, config);
        try {
            for (String name : manager.listAuthenticationProviders()) {
                SecurityAuthProviderConfig authConfig =
                        manager.loadAuthenticationProviderConfig(name);
                String userGroupService = authConfig.getUserGroupServiceName();
                if (isNotEmpty(userGroupService)) {
                    if (authConfig.getUserGroupServiceName().equals(config.getName()))
                        throw createSecurityException(
                                USERGROUP_SERVICE_ACTIVE_$2,
                                config.getName(),
                                authConfig.getName());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void validateRemoveRoleService(SecurityRoleServiceConfig config)
            throws SecurityConfigException {
        validateRemoveNamedService(GeoServerRoleService.class, config);
        if (manager.getActiveRoleService().getName().equals(config.getName())) {
            throw createSecurityException(ROLE_SERVICE_ACTIVE_$1, config.getName());
        }
    }

    public void validateRemovePasswordPolicy(PasswordPolicyConfig config)
            throws SecurityConfigException {
        validateRemoveNamedService(PasswordValidator.class, config);

        if (PasswordValidator.MASTERPASSWORD_NAME.equals(config.getName()))
            throw createSecurityException(PASSWD_POLICY_MASTER_DELETE);

        try {
            for (String name : manager.listUserGroupServices()) {
                SecurityUserGroupServiceConfig ugConfig = manager.loadUserGroupServiceConfig(name);
                if (ugConfig.getPasswordPolicyName().equals(config.getName()))
                    throw createSecurityException(
                            PASSWD_POLICY_ACTIVE_$2, config.getName(), ugConfig.getName());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void validateRemoveAuthProvider(SecurityAuthProviderConfig config)
            throws SecurityConfigException {
        validateRemoveNamedService(GeoServerAuthenticationProvider.class, config);
        for (GeoServerAuthenticationProvider prov : manager.getAuthenticationProviders()) {
            if (prov.getName().equals(config.getName()))
                throw createSecurityException(AUTH_PROVIDER_ACTIVE_$1, config.getName());
        }
    }

    public void validateRemoveFilter(SecurityNamedServiceConfig config)
            throws SecurityConfigException {
        validateRemoveNamedService(GeoServerSecurityFilter.class, config);

        List<String> patterns =
                manager.getSecurityConfig()
                        .getFilterChain()
                        .patternsForFilter(config.getClassName(), false);
        if (patterns.isEmpty() == false) {
            throw createSecurityException(
                    SecurityConfigException.FILTER_STILL_USED,
                    config.getName(),
                    StringUtils.arrayToCommaDelimitedString(patterns.toArray()));
        }
    }

    public void validateRemoveMasterPasswordProvider(MasterPasswordProviderConfig config)
            throws SecurityConfigException {
        validateRemoveNamedService(MasterPasswordProvider.class, config);
    }

    public void validate(SecurityAuthProviderConfig config) throws SecurityConfigException {
        if (isNotEmpty(config.getUserGroupServiceName())) {
            if (getNamesFor(GeoServerUserGroupService.class)
                            .contains(config.getUserGroupServiceName())
                    == false)
                throw createSecurityException(
                        USERGROUP_SERVICE_NOT_FOUND_$1, config.getUserGroupServiceName());
        }
    }

    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
        for (GeoServerRole systemRole : GeoServerRole.SystemRoles) {
            if (systemRole.getAuthority().equals(config.getAdminRoleName()))
                throw createSecurityException(RESERVED_ROLE_NAME, systemRole.getAuthority());
            if (systemRole.getAuthority().equals(config.getGroupAdminRoleName()))
                throw createSecurityException(RESERVED_ROLE_NAME, systemRole.getAuthority());
        }
    }

    public void validate(SecurityUserGroupServiceConfig config) throws SecurityConfigException {
        String encoderName = config.getPasswordEncoderName();
        GeoServerPasswordEncoder encoder = null;
        if (isNotEmpty(encoderName)) {
            try {
                encoder = manager.loadPasswordEncoder(encoderName);
            } catch (NoSuchBeanDefinitionException ex) {
                throw createSecurityException(INVALID_CONFIG_PASSWORD_ENCODER_$1, encoderName);
            }
            if (encoder == null) {
                throw createSecurityException(INVALID_CONFIG_PASSWORD_ENCODER_$1, encoderName);
            }
        } else {
            throw createSecurityException(PASSWD_ENCODER_REQUIRED_$1, config.getName());
        }

        if (!manager.isStrongEncryptionAvailable()) {
            if (encoder != null && encoder.isAvailableWithoutStrongCryptogaphy() == false) {
                throw createSecurityException(INVALID_STRONG_PASSWORD_ENCODER);
            }
        }

        String policyName = config.getPasswordPolicyName();
        if (isNotEmpty(policyName) == false) {
            throw createSecurityException(PASSWD_POLICY_REQUIRED_$1, config.getName());
        }

        if (getNamesFor(PasswordValidator.class).contains(policyName) == false) {
            throw createSecurityException(PASSWD_POLICY_NOT_FOUND_$1, policyName);
        }
    }

    public void validate(PasswordPolicyConfig config) throws SecurityConfigException {
        if (config.getMinLength() < 0) throw createSecurityException(INVALID_MIN_LENGTH);
        if (config.getMaxLength() != -1) {
            if (config.getMinLength() > config.getMaxLength())
                throw createSecurityException(INVALID_MAX_LENGTH);
        }
    }

    public void validate(MasterPasswordProviderConfig config) throws SecurityConfigException {}

    protected String alreadyExistsErrorCode(Class<?> extPoint) {
        if (GeoServerAuthenticationProvider.class == extPoint)
            return AUTH_PROVIDER_ALREADY_EXISTS_$1;
        if (PasswordValidator.class == extPoint) return PASSWD_POLICY_ALREADY_EXISTS_$1;
        if (GeoServerRoleService.class == extPoint) return ROLE_SERVICE_ALREADY_EXISTS_$1;
        if (GeoServerUserGroupService.class == extPoint) return USERGROUP_SERVICE_ALREADY_EXISTS_$1;
        if (GeoServerSecurityFilter.class == extPoint) return AUTH_FILTER_ALREADY_EXISTS_$1;
        throw new RuntimeException("Unknown extension point: " + extPoint.getName());
    }

    protected String notFoundErrorCode(Class<?> extPoint) {
        if (GeoServerAuthenticationProvider.class == extPoint) return AUTH_PROVIDER_NOT_FOUND_$1;
        if (PasswordValidator.class == extPoint) return PASSWD_POLICY_NOT_FOUND_$1;
        if (GeoServerRoleService.class == extPoint) return ROLE_SERVICE_NOT_FOUND_$1;
        if (GeoServerUserGroupService.class == extPoint) return USERGROUP_SERVICE_NOT_FOUND_$1;
        if (GeoServerSecurityFilter.class == extPoint) return AUTH_FILTER_NOT_FOUND_$1;
        throw new RuntimeException("Unknown extension point: " + extPoint.getName());
    }

    /** Helper method for creating a proper {@link SecurityConfigException} object */
    protected SecurityConfigException createSecurityException(String errorid, Object... args) {
        return new SecurityConfigException(errorid, args);
    }
}
