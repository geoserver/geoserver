/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * @author christian
 *     <p>Validates {@link AuthenticationKeyFilterConfig} objects.
 */
public class AuthenticationKeyFilterConfigValidator extends FilterConfigValidator {

    public AuthenticationKeyFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {

        if (config instanceof AuthenticationKeyFilterConfig)
            validateFilterConfig((AuthenticationKeyFilterConfig) config);
        else super.validateFilterConfig(config);
    }

    public void validateFilterConfig(AuthenticationKeyFilterConfig config)
            throws FilterConfigException {

        checkExistingUGService(config.getUserGroupServiceName());

        if (isNotEmpty(config.getAuthKeyParamName()) == false) {
            throw createFilterException(
                    AuthenticationKeyFilterConfigException.AUTH_KEY_PARAM_NAME_REQUIRED);
        }
        if (isNotEmpty(config.getAuthKeyMapperName()) == false) {
            throw createFilterException(
                    AuthenticationKeyFilterConfigException.AUTH_KEY_MAPPER_NAME_REQUIRED);
        }

        try {
            lookupBean(config.getAuthKeyMapperName());
        } catch (NoSuchBeanDefinitionException ex) {
            throw createFilterException(
                    AuthenticationKeyFilterConfigException.AUTH_KEY_MAPPER_NOT_FOUND_$1,
                    config.getAuthKeyMapperName());
        }

        // validates mapper parameters
        AuthenticationKeyMapper mapper =
                (AuthenticationKeyMapper) lookupBean(config.getAuthKeyMapperName());
        for (String paramName : config.getMapperParameters().keySet()) {
            if (!mapper.getAvailableParameters().contains(paramName)) {
                throw createFilterException(
                        AuthenticationKeyFilterConfigException.INVALID_AUTH_KEY_MAPPER_PARAMETER_$3,
                        paramName);
            }
            mapper.validateParameter(paramName, config.getMapperParameters().get(paramName));
        }
        mapper.configureMapper(config.getMapperParameters());

        GeoServerUserGroupService service = null;
        try {
            service = manager.loadUserGroupService(config.getUserGroupServiceName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (mapper.supportsReadOnlyUserGroupService() == false
                && service.canCreateStore() == false) {
            throw createFilterException(
                    AuthenticationKeyFilterConfigException.INVALID_AUTH_KEY_MAPPER_$2,
                    config.getAuthKeyMapperName(),
                    config.getUserGroupServiceName());
        }
    }

    protected AuthenticationKeyFilterConfigException createFilterException(
            String errorid, Object... args) {
        return new AuthenticationKeyFilterConfigException(errorid, args);
    }
}
