/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template.editor.web;

import org.geoserver.web.ComponentInfo;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * Extension hook for template editor pages (should not show in menu)
 *
 * @author Jean Pommier <jean.pommier@pi-geosolutions.fr>
 */
@SuppressWarnings("serial")
public class TemplateEditorPageInfo extends ComponentInfo<GeoServerSecuredPage> {
    // inherit everything from ComponentInfo
}
