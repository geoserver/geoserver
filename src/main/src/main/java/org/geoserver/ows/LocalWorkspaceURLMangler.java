/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.geoserver.ows.util.ResponseUtils.stripRemainingPath;

import java.util.Map;

/**
 * Mangles service URL's based on teh presence of a {@link LocalWorkspace} and {@link
 * LocalPublished}.
 *
 * <p>When the local workspace and layer are present this mangler will turns urls of the form:
 *
 * <pre>
 *   /geoserver/wfs?...
 * </pre>
 *
 * into:
 *
 * <pre>
 *   /geoserver/&lt;localWorkspace&gt;/&lt;localLayer&gt;/wfs?...
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class LocalWorkspaceURLMangler implements URLMangler {

    /** the name/identifier of the ows: wfs, wms, wcs, etc... */
    String ows;

    public LocalWorkspaceURLMangler(String ows) {
        this.ows = ows;
    }

    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {

        if (type == URLType.SERVICE && stripRemainingPath(path.toString()).equalsIgnoreCase(ows)) {
            if (LocalWorkspace.get() != null) {
                path.insert(0, LocalWorkspace.get().getName() + "/");

                if (LocalPublished.get() != null) {
                    int i = LocalWorkspace.get().getName().length() + 1;
                    path.insert(i, LocalPublished.get().getName() + "/");
                }
            }
        }
    }
}
