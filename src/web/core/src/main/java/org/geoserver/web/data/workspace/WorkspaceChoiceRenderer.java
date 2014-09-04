/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.WorkspaceInfo;

/**
 * Simple choice renderer for {@link WorkspaceInfo}
 */
@SuppressWarnings("serial")
public class WorkspaceChoiceRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((WorkspaceInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((WorkspaceInfo) object).getId();
    }

}
