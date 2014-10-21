/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.Application;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.security.config.RoleSource;

/**
 * ChoiceRenderer for RoleSource enums
 * 
 * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
 */
public class RoleSourceChoiceRenderer implements IChoiceRenderer<RoleSource> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    @Override
    public Object getDisplayValue(RoleSource rs) {
        String key = "RoleSource." + rs.toString();
        return Application.get().getResourceSettings().getLocalizer()
                .getString(key, null);
    }

    @Override
    public String getIdValue(RoleSource rs, int index) {
        return rs.toString();
    }

}
