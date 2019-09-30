/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.springframework.web.util.UrlPathHelper;

public class LocalWorkspaceURLPathHelper extends UrlPathHelper {

    public LocalWorkspaceURLPathHelper() {
        this.setAlwaysUseFullPath(true);
    }

    @Override
    public String getRequestUri(HttpServletRequest request) {
        String uri = super.getRequestUri(request);
        WorkspaceInfo ws = LocalWorkspace.get();
        if (ws == null) {
            return uri;
        }

        String localRequestPrefix = request.getContextPath() + "/" + ws.getName();
        if (uri.startsWith(localRequestPrefix)) {
            uri = request.getContextPath() + uri.substring(localRequestPrefix.length());
        }

        return uri;
    }

    public String toLocalPath(String path) {
        WorkspaceInfo ws = LocalWorkspace.get();
        if (ws == null) {
            return path;
        }

        if (path.startsWith("/" + ws.getName())) {
            path = path.substring(ws.getName().length() + 1);
        }

        return path;
    }

    @Override
    public String getPathWithinApplication(HttpServletRequest request) {
        String path = super.getPathWithinApplication(request);
        return toLocalPath(path);
    }
}
