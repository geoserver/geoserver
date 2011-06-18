package org.geoserver.web;

import org.springframework.security.Authentication;;


public class DefaultPageAuthorizer extends AdminComponentAuthorizer implements PageAuthorizer {
    @Override
    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        if (GeoServerSecuredPage.class.isAssignableFrom(componentClass)) {
            return super.isAccessAllowed(componentClass, authentication);
        }
        return true;
    }
}
