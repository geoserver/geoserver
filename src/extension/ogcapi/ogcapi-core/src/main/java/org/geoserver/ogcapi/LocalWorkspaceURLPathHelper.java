/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.springframework.web.util.UrlPathHelper;

/** Adapt paths to local workspace. */
public class LocalWorkspaceURLPathHelper extends UrlPathHelper {

    public LocalWorkspaceURLPathHelper() {
        this.setAlwaysUseFullPath(true);
    }

    @Override
    public String getRequestUri(HttpServletRequest request) {
        String uri = super.getRequestUri(request);
        WorkspaceInfo ws = LocalWorkspace.get();
        PublishedInfo published = LocalPublished.get();
        if (ws == null && published == null) {
            return uri;
        }

        // handle all local services cases
        String localRequestPrefix;
        if (ws == null) {
            localRequestPrefix = request.getContextPath() + "/" + published.getName();
        } else if (published == null) {
            localRequestPrefix = request.getContextPath() + "/" + ws.getName();
        } else {
            localRequestPrefix =
                    request.getContextPath() + "/" + ws.getName() + "/" + published.getName();
        }
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
