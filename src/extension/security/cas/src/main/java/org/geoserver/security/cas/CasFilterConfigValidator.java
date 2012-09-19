/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
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
 *
 */
public class CasFilterConfigValidator extends FilterConfigValidator {

    public CasFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
        
    }

    public void validateFilterConfig(SecurityNamedServiceConfig config) throws FilterConfigException {
        
        if (config instanceof CasAuthenticationFilterConfig)
            validateFilterConfig((CasAuthenticationFilterConfig)config);
        if (config instanceof CasProxiedAuthenticationFilterConfig)
            validateFilterConfig((CasProxiedAuthenticationFilterConfig)config);

        super.validateFilterConfig(config);
    }

    public void validateFilterConfig(CasAuthenticationProperties config) throws FilterConfigException {
        
        URL serviceUrl=null;;
        if (StringUtils.hasLength(config.getService())==false)
            throw  createFilterException(CasFilterConfigException.CAS_SERVICE_URL_REQUIRED);        
        try {
            serviceUrl= new URL(config.getService());
        } catch (MalformedURLException ex) {
            throw  createFilterException(CasFilterConfigException.CAS_SERVICE_URL_MALFORMED);
        }
        
        if (StringUtils.hasLength(config.getCasServerUrlPrefix())==false)
            throw  createFilterException(CasFilterConfigException.CAS_SERVER_URL_REQUIRED);
    
        try {
            new URL(config.getCasServerUrlPrefix());
        } catch (MalformedURLException ex) {
            throw  createFilterException(CasFilterConfigException.CAS_SERVER_URL_MALFORMED);
        }        
        
        if (StringUtils.hasLength(config.getProxyCallbackUrlPrefix())) {
            URL callBackUrl=null;
            try {
                callBackUrl=new URL(config.getProxyCallbackUrlPrefix());
            } catch (MalformedURLException ex) {
                throw  createFilterException(CasFilterConfigException.CAS_PROXYCALLBACK_MALFORMED);
            }
            if ("https".equalsIgnoreCase(callBackUrl.getProtocol())==false)
                throw  createFilterException(CasFilterConfigException.CAS_PROXYCALLBACK_NOT_HTTPS);
            
            if (callBackUrl.getHost().equals(serviceUrl.getHost())==false)
                throw  createFilterException(CasFilterConfigException.CAS_PROXYCALLBACK_HOST_UNEQUAL_SERVICE_HOST);            
        }
    }

    public void validateFilterConfig(CasProxiedAuthenticationFilterConfig config) throws FilterConfigException {
        validateFilterConfig((PreAuthenticatedUserNameFilterConfig) config);
        validateFilterConfig((CasAuthenticationProperties) config);
    }
    
    public void validateFilterConfig(CasAuthenticationFilterConfig config) throws FilterConfigException {

        if (StringUtils.hasLength(config.getUrlInCasLogoutPage())) {    
            try {
                new URL(config.getCasServerUrlPrefix());
            } catch (MalformedURLException ex) {
                throw  createFilterException(CasFilterConfigException.CAS_URL_IN_LOGOUT_PAGE_MALFORMED);
            }
        }
        
        checkExistingUGService(config.getUserGroupServiceName());
        
        validateFilterConfig((CasAuthenticationProperties) config);                  
    }

    protected CasFilterConfigException createFilterException (String errorid, Object ...args) {
        return new CasFilterConfigException(errorid,args);
    }
    
}
