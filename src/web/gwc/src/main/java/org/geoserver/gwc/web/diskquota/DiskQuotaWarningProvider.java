/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.diskquota;

import org.apache.wicket.Component;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerHomePageContentProvider;

/**
 * Adds a error message to the home page in case the current user is the administrator and the disk
 * quota store failed to load
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DiskQuotaWarningProvider implements GeoServerHomePageContentProvider {

    @Override
    public Component getPageBodyComponent(String id) {
        GeoServerSecurityManager secMgr = GeoServerApplication.get().getSecurityManager();
        if (secMgr.checkAuthenticationForAdminRole()
                && DiskQuotaWarningPanel.getException() != null) {
            return new DiskQuotaWarningPanel(id);
        } else {
            return null;
        }
    }
}
