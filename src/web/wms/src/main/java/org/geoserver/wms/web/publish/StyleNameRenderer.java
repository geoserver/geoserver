/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.StyleInfo;

/** Style name rendered, displays the style, uses the id as the select id */
@SuppressWarnings("serial")
public class StyleNameRenderer extends ChoiceRenderer<StyleInfo> {

    public Object getDisplayValue(StyleInfo object) {
        return object.prefixedName();
    }

    public String getIdValue(StyleInfo object, int index) {
        return object.getId();
    }
}
