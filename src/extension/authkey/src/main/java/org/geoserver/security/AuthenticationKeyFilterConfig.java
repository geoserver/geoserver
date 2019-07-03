/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang.SerializationUtils;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityConfig;
import org.geoserver.security.config.SecurityFilterConfig;

/**
 * {@link GeoServerAuthenticationKeyFilter} configuration object.
 *
 * <p>{@link #authKeyParamName} is the name of the URL parameter, default is {@link
 * KeyAuthenticationToken#DEFAULT_URL_PARAM}
 *
 * <p>{@link #authKeyMapperName} is the bean name of an {@link AuthenticationKeyMapper}
 * implementation.
 *
 * @author mcr
 */
public class AuthenticationKeyFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;
    private String authKeyMapperName;
    private String authKeyParamName = KeyAuthenticationToken.DEFAULT_URL_PARAM;
    private String userGroupServiceName;
    private Map<String, String> mapperParameters;

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    public String getAuthKeyMapperName() {
        return authKeyMapperName;
    }

    public void setAuthKeyMapperName(String authKeyMapperName) {
        this.authKeyMapperName = authKeyMapperName;
    }

    public String getAuthKeyParamName() {
        return authKeyParamName;
    }

    public void setAuthKeyParamName(String authKeyParamName) {
        this.authKeyParamName = authKeyParamName;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    /** Returns the mapper parameters. */
    public Map<String, String> getMapperParameters() {
        if (mapperParameters == null) {
            mapperParameters = new HashMap<String, String>();
        }
        return mapperParameters;
    }

    /**
     * Sets the mapper parameters.
     *
     * @param mapperParameters mapper parameters
     */
    public void setMapperParameters(Map<String, String> mapperParameters) {
        this.mapperParameters = mapperParameters;
    }

    @Override
    public SecurityConfig clone(boolean allowEnvParametrization) {
        AuthenticationKeyFilterConfig target =
                (AuthenticationKeyFilterConfig) SerializationUtils.clone(this);
        if (target != null) {
            // Resolve GeoServer Environment placeholders
            final GeoServerEnvironment gsEnvironment =
                    GeoServerExtensions.bean(GeoServerEnvironment.class);
            if (target.getMapperParameters() != null && !target.getMapperParameters().isEmpty()) {
                if (allowEnvParametrization) {
                    for (Entry<String, String> param : target.getMapperParameters().entrySet()) {
                        String key = param.getKey();
                        Object value = param.getValue();

                        if (gsEnvironment != null
                                && GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
                            value = gsEnvironment.resolveValue(value);
                        }

                        target.getMapperParameters().put(key, (String) value);
                    }
                }
            }
        }

        return target;
    }
}
