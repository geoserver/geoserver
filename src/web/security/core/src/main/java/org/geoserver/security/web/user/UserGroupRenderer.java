/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.security.impl.GeoServerUserGroup;

/**
 * Choice renderer for {@link GeoServerUserGroup}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UserGroupRenderer extends ChoiceRenderer<GeoServerUserGroup> {

    @Override
    public String getIdValue(GeoServerUserGroup object, int index) {
        return object.getGroupname();
    }

    @Override
    public Object getDisplayValue(GeoServerUserGroup object) {
        return getIdValue(object, -1);
    }
}
