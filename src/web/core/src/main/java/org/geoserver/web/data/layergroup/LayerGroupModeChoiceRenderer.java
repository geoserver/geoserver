/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;

/** Simple choice renderer for {@link LayerGroupInfo.Mode} */
public class LayerGroupModeChoiceRenderer extends ChoiceRenderer<Mode> {

    private static final long serialVersionUID = -4853272187576451891L;

    @Override
    public Object getDisplayValue(Mode mode) {
        return mode.getName();
    }

    @Override
    public String getIdValue(Mode mode, int index) {
        return mode.getCode().toString();
    }
}
