/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.framework;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.LayerInfo;

/**
 * Simple choice renderer for {@link org.geoserver.catalog.WorkspaceInfo}
 */
@SuppressWarnings("serial")
public class AssociatedWMSChoiceRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((LayerInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((LayerInfo) object).getId();
    }

}
