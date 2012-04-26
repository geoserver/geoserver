package org.geoserver.task.web;

import org.apache.wicket.Component;
import org.geoserver.web.GeoServerHomePageContentProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class LongTasksHomePagePanelProvider implements GeoServerHomePageContentProvider {

    /**
     * @see org.geoserver.web.GeoServerHomePageContentProvider#getPageBodyComponent(java.lang.String)
     */
    public Component getPageBodyComponent(final String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return new LongTasksPanel(id, new LongTasksMonitorDetachableModel());
    }

}
