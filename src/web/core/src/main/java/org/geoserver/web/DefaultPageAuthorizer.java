package org.geoserver.web;

import org.springframework.security.core.Authentication;;


public class DefaultPageAuthorizer extends AdminComponentAuthorizer implements ComponentAuthorizer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    @Override
    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        if (GeoServerSecuredPage.class.isAssignableFrom(componentClass)) {
            return super.isAccessAllowed(componentClass, authentication);
        }
        return true;
    }
}
