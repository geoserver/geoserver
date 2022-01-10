/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.WorkspaceInfo;

/** Simple choice renderer for {@link WorkspaceInfo} */
public class WorkspaceChoiceRenderer extends ChoiceRenderer<WorkspaceInfo> {

    private static final long serialVersionUID = 9065816461497078542L;

    public Object getDisplayValue(WorkspaceInfo object) {
        return object.getName();
    }

    public String getIdValue(WorkspaceInfo object, int index) {
        return object.getId();
    }
}
