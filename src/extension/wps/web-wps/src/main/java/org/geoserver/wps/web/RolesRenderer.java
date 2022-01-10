/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.web;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteRenderer;
import org.apache.wicket.request.Response;

/** Renders roles as string */
public class RolesRenderer extends AbstractAutoCompleteRenderer<String> {

    private static final long serialVersionUID = 3407675669346346083L;
    private StringBuilder selectedRoles;

    public RolesRenderer(StringBuilder selectedRoles) {
        this.selectedRoles = selectedRoles;
    }

    @Override
    protected void renderChoice(String object, Response response, String criteria) {
        response.write(object);
    }

    @Override
    protected String getTextValue(String object) {
        return selectedRoles.toString() + object;
    }
}
