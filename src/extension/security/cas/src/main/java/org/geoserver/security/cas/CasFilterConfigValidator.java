/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.springframework.util.StringUtils;

/**
 * Validator for Cas filter configurations
 *
 * @author mcr
 */
public class CasFilterConfigValidator extends FilterConfigValidator {

    public CasFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {

        if (config instanceof CasAuthenticationFilterConfig) {
            validateCASFilterConfig((CasAuthenticationFilterConfig) config);
        } else {
            super.validateFilterConfig(config);
        }
    }

    public void validateCASFilterConfig(CasAuthenticationFilterConfig casConfig)
            throws FilterConfigException {

        if (StringUtils.hasLength(casConfig.getUrlInCasLogoutPage())) {
            try {
                new URL(casConfig.getUrlInCasLogoutPage());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        CasFilterConfigException.CAS_URL_IN_LOGOUT_PAGE_MALFORMED);
            }
        }
        super.validateFilterConfig((PreAuthenticatedUserNameFilterConfig) casConfig);

        if (StringUtils.hasLength(casConfig.getCasServerUrlPrefix()) == false)
            throw createFilterException(CasFilterConfigException.CAS_SERVER_URL_REQUIRED);

        try {
            new URL(casConfig.getCasServerUrlPrefix());
        } catch (MalformedURLException ex) {
            throw createFilterException(CasFilterConfigException.CAS_SERVER_URL_MALFORMED);
        }

        if (StringUtils.hasLength(casConfig.getProxyCallbackUrlPrefix())) {
            URL callBackUrl = null;
            try {
                callBackUrl = new URL(casConfig.getProxyCallbackUrlPrefix());
            } catch (MalformedURLException ex) {
                throw createFilterException(CasFilterConfigException.CAS_PROXYCALLBACK_MALFORMED);
            }
            if ("https".equalsIgnoreCase(callBackUrl.getProtocol()) == false)
                throw createFilterException(CasFilterConfigException.CAS_PROXYCALLBACK_NOT_HTTPS);
        }
    }

    protected CasFilterConfigException createFilterException(String errorid, Object... args) {
        return new CasFilterConfigException(errorid, args);
    }
}
