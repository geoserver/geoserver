/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;

/** Simple choice renderer for {@link LayerInfo} */
@SuppressWarnings("serial")
public class WorkspaceChoiceRenderer extends ChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((WorkspaceInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((WorkspaceInfo) object).getName();
    }
}
