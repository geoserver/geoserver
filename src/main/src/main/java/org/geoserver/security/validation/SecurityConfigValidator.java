/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import static org.geoserver.security.validation.SecurityConfigException.AUTH_FILTER_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.SecurityConfigException.AUTH_FILTER_NOT_FOUND_$1;
import static org.geoserver.security.validation.SecurityConfigException.AUTH_PROVIDER_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.SecurityConfigException.AUTH_PROVIDER_NOT_FOUND_$1;
import static org.geoserver.security.validation.SecurityConfigException.CLASSNAME_REQUIRED;
import static org.geoserver.security.validation.SecurityConfigException.PASSWD_POLICY_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.SecurityConfigException.PASSWD_POLICY_NOT_FOUND_$1;
import static org.geoserver.security.validation.SecurityConfigException.ROLE_SERVICE_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.SecurityConfigException.ROLE_SERVICE_NOT_FOUND_$1;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_01;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_02;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_03;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_04;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_05;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_06;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_07;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_20;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_21;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_22;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_24b;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_24d;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_25;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_30;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_31;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_32;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_33;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_34;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_35;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_40;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_41;
import static org.geoserver.security.validation.SecurityConfigException.SEC_ERR_42;
import static org.geoserver.security.validation.SecurityConfigException.USERGROUP_SERVICE_ALREADY_EXISTS_$1;
import static org.geoserver.security.validation.SecurityConfigException.USERGROUP_SERVICE_NOT_FOUND_$1;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.PasswordValidator;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.util.StringUtils;


public class SecurityConfigValidator extends AbstractSecurityValidator{

    public SecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    /**
     * Get the proper {@link SecurityConfigValidator} object
     * 
     * @param serviceClass
     * @param className
     * @return
     */
    static public SecurityConfigValidator getConfigurationValiator(Class <?> serviceClass, String className) 
            throws SecurityConfigException {
        GeoServerSecurityProvider prov = GeoServerSecurityProvider.getProvider(serviceClass, className);
        if (className == null)
            throw new SecurityConfigException(CLASSNAME_REQUIRED,new Object[]{});
        
        //TODO: remove the call to extensions, have teh security manager be passed in
        return prov.createConfigurationValidator(GeoServerExtensions.bean(GeoServerSecurityManager.class));
    }
    
    
    /**
     * Checks the {@link SecurityManagerConfig} object
     * 
     * @param config
     * @throws SecurityConfigException
     */
    public void validateManagerConfig(SecurityManagerConfig config) throws SecurityConfigException{
        
        String encrypterName =config.getConfigPasswordEncrypterName();
        if (isNotEmpty(encrypterName)==false) {
            throw createSecurityException(SEC_ERR_07);
        }
        
        GeoServerPasswordEncoder encoder = null;
        try {
            encoder = manager.loadPasswordEncoder(config.getConfigPasswordEncrypterName());
        } catch (NoSuchBeanDefinitionException ex) {
            throw createSecurityException(SEC_ERR_01, encrypterName);
        }
        if (encoder == null) {
            throw createSecurityException(SEC_ERR_01, encrypterName);
        }

        if (!encoder.isReversible()) {
            throw createSecurityException(SEC_ERR_01, encrypterName);
        }

        if (!manager.isStrongEncryptionAvailable()) {
            if (encoder!=null && encoder.isAvailableWithoutStrongCryptogaphy()==false) {
                throw createSecurityException(SEC_ERR_05);
            }
        }
        
        String roleServiceName = config.getRoleServiceName();
        if (roleServiceName==null) 
            roleServiceName="";
        
        try {
            if (manager.listRoleServices().contains(roleServiceName)==false)
                throw createSecurityException(SEC_ERR_02, roleServiceName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        SortedSet<String> authProviders=null;
        try{
            authProviders =manager.listAuthenticationProviders();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (String authProvName : config.getAuthProviderNames()) {
            if (authProviders.contains(authProvName)==false)
                throw createSecurityException(SEC_ERR_03, authProvName);
        }
        
        // check the filter chain
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        Set<String> keys = chain.getFilterMap().keySet();
        if (keys.size()!=chain.getAntPatterns().size())
            throw createSecurityException(SecurityConfigException.FILTER_CHAIN_CONFIG_ERROR);
        for (String pattern : chain.getAntPatterns()) {
            if (keys.contains(pattern)==false)
                throw createSecurityException(SecurityConfigException.FILTER_CHAIN_CONFIG_ERROR);
        }        
    }
    
    protected void checkExtensionPont(Class<?> extensionPoint, String className) throws SecurityConfigException{
        if (isNotEmpty(className)==false) {
            throw createSecurityException(SEC_ERR_25);
        }
        Class<?> aClass = null;
        try {
            aClass=Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw createSecurityException(SEC_ERR_20, className);
        } 
        
        if (extensionPoint.isAssignableFrom(aClass)==false) {
            throw createSecurityException(SEC_ERR_21, extensionPoint,
                    className);
        }
    }
    
    protected void checkServiceName(Class<?> extensionPoint,String name) throws SecurityConfigException{
        if (name==null || name.isEmpty())
                throw createSecurityException(SEC_ERR_22);                        
    }
    
    protected  SortedSet<String> getNamesFor(Class<?> extensionPoint) {
        try {
            if (extensionPoint==GeoServerUserGroupService.class)
                return manager.listUserGroupServices();
            if (extensionPoint==GeoServerRoleService.class)
                return manager.listRoleServices();
            if (extensionPoint==GeoServerAuthenticationProvider.class)
                return manager.listAuthenticationProviders();
            if (extensionPoint==AuthenticationProvider.class)
                return manager.listAuthenticationProviders();
            if (extensionPoint==GeoServerSecurityFilter.class)
                return manager.listFilters();
            if (extensionPoint==PasswordValidator.class)
                return manager.listPasswordValidators();
            if (extensionPoint==MasterPasswordProvider.class) {
                return  manager.listMasterPasswordProviders();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        throw new RuntimeException("Unkwnown extension point: "+extensionPoint.getName());
    }
    
    public void validateAddNamedService(Class<?> extensionPoint,SecurityNamedServiceConfig config) throws SecurityConfigException{
        checkExtensionPont(extensionPoint, config.getClassName());
        checkServiceName(extensionPoint, config.getName());
        SortedSet<String> names= getNamesFor(extensionPoint);
        if (names.contains(config.getName()))
            throw createSecurityException(alreadyExistsErrorCode(extensionPoint), config.getName());
        
    }
    
    public void validateModifiedNamedService(Class<?> extensionPoint,SecurityNamedServiceConfig config) throws SecurityConfigException{
        checkExtensionPont(extensionPoint, config.getClassName());
        checkServiceName(extensionPoint, config.getName());
        SortedSet<String> names= getNamesFor(extensionPoint);
        if (names.contains(config.getName())==false)
            throw createSecurityException(notFoundErrorCode(extensionPoint),config.getName());
        
    }

        
    public void validateRemoveNamedService(Class<?> extensionPoint,SecurityNamedServiceConfig config) throws SecurityConfigException{
        checkServiceName(extensionPoint, config.getName());
    }
    
    public void validateAddUserGroupService(SecurityUserGroupServiceConfig config) throws SecurityConfigException{
        validateAddNamedService(GeoServerUserGroupService.class, config);        
        validate(config);
    }
    
    public void validateAddRoleService(SecurityRoleServiceConfig config) throws SecurityConfigException{
        validateAddNamedService(GeoServerRoleService.class, config);
        validate(config);
    }

    public void validateAddPasswordPolicy(PasswordPolicyConfig config) throws SecurityConfigException{
        validateAddNamedService(PasswordValidator.class, config);
        validate(config);
    }
    
    
    public void validateAddAuthProvider(SecurityAuthProviderConfig config) throws SecurityConfigException{
        validateAddNamedService(GeoServerAuthenticationProvider.class, config);
        validate(config);
    }

    public void validateAddFilter(SecurityNamedServiceConfig config) throws SecurityConfigException{
        validateAddNamedService(GeoServerSecurityFilter.class, config);        
    }

    public void validateAddMasterPasswordProvider(MasterPasswordProviderConfig config) throws SecurityConfigException {
        validateAddNamedService(MasterPasswordProvider.class, config);
        validate(config);
    }

    public void validateModifiedUserGroupService(SecurityUserGroupServiceConfig config,SecurityUserGroupServiceConfig oldConfig) throws SecurityConfigException{
        validateModifiedNamedService(GeoServerUserGroupService.class, config);
        validate(config);
    }
    
    public void validateModifiedRoleService(SecurityRoleServiceConfig config,SecurityRoleServiceConfig oldConfig) throws SecurityConfigException{
        validateModifiedNamedService(GeoServerRoleService.class, config);
        validate(config);
    }

    public void validateModifiedPasswordPolicy(PasswordPolicyConfig config,PasswordPolicyConfig oldConfig) throws SecurityConfigException{
        validateModifiedNamedService(PasswordValidator.class, config);
        validate(config);
    }
    
    public void validateModifiedAuthProvider(SecurityAuthProviderConfig config,SecurityAuthProviderConfig oldconfig) throws SecurityConfigException{
        validateModifiedNamedService(GeoServerAuthenticationProvider.class, config);
        validate(config);        
    }

    public void validateModifiedFilter(SecurityNamedServiceConfig config,SecurityNamedServiceConfig oldConfig) throws SecurityConfigException{
        validateModifiedNamedService(GeoServerSecurityFilter.class, config);        
    }

    public void validateModifiedMasterPasswordProvider(MasterPasswordProviderConfig config, 
        MasterPasswordProviderConfig oldConfig) throws SecurityConfigException {
        validateModifiedNamedService(MasterPasswordProvider.class, config);
        validate(config);
    }

    public void validateRemoveUserGroupService(SecurityUserGroupServiceConfig config) throws SecurityConfigException{
        validateRemoveNamedService(GeoServerUserGroupService.class, config);
        try {
            for (String name: manager.listAuthenticationProviders()) {
                SecurityAuthProviderConfig authConfig = 
                        manager.loadAuthenticationProviderConfig(name);
                String userGroupService=authConfig.getUserGroupServiceName();
                if (isNotEmpty(userGroupService)) {
                    if (authConfig.getUserGroupServiceName().equals(config.getName()))
                        throw createSecurityException(SEC_ERR_35, config.getName(),authConfig.getName());
                }    
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
    }
    
    public void validateRemoveRoleService(SecurityRoleServiceConfig config) throws SecurityConfigException{
        validateRemoveNamedService(GeoServerRoleService.class, config); 
        if (manager.getActiveRoleService().getName().equals(config.getName())) {
                    throw createSecurityException(SEC_ERR_30, config.getName());
                }
    }

    public void validateRemovePasswordPolicy(PasswordPolicyConfig config) throws SecurityConfigException{
        validateRemoveNamedService(PasswordValidator.class, config);
        
        if (PasswordValidator.MASTERPASSWORD_NAME.equals(config.getName()))
                throw createSecurityException(SEC_ERR_42);
                
        try {
            for (String name: manager.listUserGroupServices()) {
                SecurityUserGroupServiceConfig ugConfig = 
                        manager.loadUserGroupServiceConfig(name);
                if (ugConfig.getPasswordPolicyName().equals(config.getName()))
                    throw createSecurityException(SEC_ERR_34, config.getName(),ugConfig.getName());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void validateRemoveAuthProvider(SecurityAuthProviderConfig config) throws SecurityConfigException{
        validateRemoveNamedService(GeoServerAuthenticationProvider.class, config);        
        for (GeoServerAuthenticationProvider prov :manager.getAuthenticationProviders()) {
            if (prov.getName().equals(config.getName()))
                throw createSecurityException(SEC_ERR_31, config.getName());
        }
    }

    public void validateRemoveFilter(SecurityNamedServiceConfig config) throws SecurityConfigException{
        validateRemoveNamedService(GeoServerSecurityFilter.class, config);
        
        List<String> patterns = manager.getSecurityConfig().getFilterChain().
            patternsContainingFilter(config.getClassName());
        if (patterns.isEmpty()==false) {
            throw createSecurityException(SecurityConfigException.FILTER_STILL_USED, 
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
            if (getNamesFor(GeoServerUserGroupService.class).
                    contains(config.getUserGroupServiceName())==false)
                    throw createSecurityException(SEC_ERR_24d,
                            config.getUserGroupServiceName() );
        }        
    }
    
    public void validate(SecurityRoleServiceConfig config) throws SecurityConfigException {
//        if (GeoserverRole.ADMIN_ROLE.getAuthority().equals(config.getAdminRoleName())==false) {
//            throw createSecurityException(SEC_ERR_50, GeoserverRole.ADMIN_ROLE.getAuthority());
//        }
    }

    public void validate(SecurityUserGroupServiceConfig config) throws SecurityConfigException {
        String encoderName =config.getPasswordEncoderName();
        GeoServerPasswordEncoder encoder = null;
        if (isNotEmpty(encoderName)) {
            try {
                encoder = manager.loadPasswordEncoder(encoderName);
            } catch (NoSuchBeanDefinitionException ex) {
                throw createSecurityException(SEC_ERR_04, encoderName);
            }
            if (encoder == null) {
                throw createSecurityException(SEC_ERR_04, encoderName);
            }
        } else {
            throw createSecurityException(SEC_ERR_32, config.getName());
        }
        
        if (!manager.isStrongEncryptionAvailable()) {
            if (encoder!=null && encoder.isAvailableWithoutStrongCryptogaphy()==false) {
                throw createSecurityException(SEC_ERR_06);
            }
        }
        
        String policyName= config.getPasswordPolicyName();
        if (isNotEmpty(policyName)==false) {
            throw createSecurityException(SEC_ERR_33, config.getName());
        }
        
        if (getNamesFor(PasswordValidator.class).contains(policyName)==false) {
            throw createSecurityException(SEC_ERR_24b,policyName);
        }
    }
    
    public void validate(PasswordPolicyConfig config) throws SecurityConfigException {
        if (config.getMinLength() < 0)
            throw createSecurityException(SEC_ERR_40);
        if (config.getMaxLength() !=- 1) {
            if (config.getMinLength()>config.getMaxLength())
                throw createSecurityException(SEC_ERR_41);
        }
    }

    public void validate(MasterPasswordProviderConfig config) throws SecurityConfigException {
    }

    protected String alreadyExistsErrorCode(Class<?> extPoint) {
        if (GeoServerAuthenticationProvider.class==extPoint)
            return AUTH_PROVIDER_ALREADY_EXISTS_$1;
        if (PasswordValidator.class==extPoint)
            return PASSWD_POLICY_ALREADY_EXISTS_$1;
        if (GeoServerRoleService.class==extPoint)
            return ROLE_SERVICE_ALREADY_EXISTS_$1;
        if (GeoServerUserGroupService.class==extPoint)
            return USERGROUP_SERVICE_ALREADY_EXISTS_$1;
        if (GeoServerSecurityFilter.class==extPoint)
            return AUTH_FILTER_ALREADY_EXISTS_$1;
        throw new RuntimeException("Unkonw extension point: "+extPoint.getName());
    }

    protected String notFoundErrorCode(Class<?> extPoint) {
        if (GeoServerAuthenticationProvider.class==extPoint)
            return AUTH_PROVIDER_NOT_FOUND_$1;
        if (PasswordValidator.class==extPoint)
            return PASSWD_POLICY_NOT_FOUND_$1;
        if (GeoServerRoleService.class==extPoint)
            return ROLE_SERVICE_NOT_FOUND_$1;
        if (GeoServerUserGroupService.class==extPoint)
            return USERGROUP_SERVICE_NOT_FOUND_$1;
        if (GeoServerSecurityFilter.class==extPoint)
            return AUTH_FILTER_NOT_FOUND_$1;
        throw new RuntimeException("Unkonw extension point: "+extPoint.getName());
    }

    /**
     * Helper method for creating a proper
     * {@link SecurityConfigException} object
     */
    protected SecurityConfigException createSecurityException (String errorid, Object ...args) {
        return new SecurityConfigException(errorid,args);
    }
}
