/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.StyleInfo;

@SuppressWarnings("serial")
public class StyleChoiceRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((StyleInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((StyleInfo) object).getId();
    }

}
