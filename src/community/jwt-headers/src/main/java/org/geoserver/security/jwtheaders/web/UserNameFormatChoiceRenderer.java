/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.web;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;

/** Wicket support class for the UserNameHeaderFormat displayed on the config page. */
public class UserNameFormatChoiceRenderer
        extends ChoiceRenderer<GeoServerJwtHeadersFilterConfig.UserNameHeaderFormat> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(GeoServerJwtHeadersFilterConfig.UserNameHeaderFormat rs) {
        String key = "UserNameHeaderFormat." + rs.toString();
        return Application.get().getResourceSettings().getLocalizer().getString(key, null);
    }

    @Override
    public String getIdValue(GeoServerJwtHeadersFilterConfig.UserNameHeaderFormat rs, int index) {
        return rs.toString();
    }
}
