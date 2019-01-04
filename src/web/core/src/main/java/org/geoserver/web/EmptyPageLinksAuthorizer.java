/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.Collections;
import java.util.List;
import org.springframework.security.core.Authentication;

/**
 * Shows the tools link only if there is at least one tool around
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EmptyPageLinksAuthorizer extends DefaultPageAuthorizer {

    List<Class> linkClasses = Collections.emptyList();

    public List<Class> getLinkClasses() {
        return linkClasses;
    }

    public void setLinkClasses(List<Class> linkClasses) {
        this.linkClasses = linkClasses;
    }

    @Override
    public boolean isAccessAllowed(Class<?> componentClass, Authentication authentication) {
        // if not admin just say no
        if (!super.isAccessAllowed(componentClass, authentication)) {
            return false;
        }

        // hide the page if there is demo around
        GeoServerApplication app = GeoServerApplication.get();
        for (Class<?> linkClass : linkClasses) {
            if (app.getBeansOfType(linkClass).size() > 0) {
                return true;
            }
        }
        return false;
    }
}
