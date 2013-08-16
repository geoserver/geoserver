/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;


/**
 * Simple choice renderer for {@link LayerGroupInfo.Mode}
 */
@SuppressWarnings("serial")
public class LayerGroupModeChoiceRenderer implements IChoiceRenderer<Mode> {

    @Override
    public Object getDisplayValue(Mode mode) {
        return mode.getName();
    }

    @Override
    public String getIdValue(Mode mode, int index) {
        return mode.getCode().toString();
    }
}
