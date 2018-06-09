/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.StyleType;

@SuppressWarnings("serial")
public class StyleTypeChoiceRenderer extends ChoiceRenderer<StyleType> {

    public Object getDisplayValue(StyleType object) {
        return object.toString();
    }

    public String getIdValue(StyleType object, int index) {
        return object.toString();
    }
}
