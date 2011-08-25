/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.StyleInfo;

/**
 * Style name rendered, displays the style, uses the id as the select id
 */
@SuppressWarnings("serial")
public class StyleNameRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return (object != null ? ((StyleInfo) object).getName() : null);
    }

    public String getIdValue(Object object, int index) {
        return (object != null ? ((StyleInfo) object).getId() : null);
    }

}
