/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.markup.html.form.IChoiceRenderer;

/**
 * Renders layer group entries in drop down choice components
 * @author Andrea Aime - GeoSolutions
 *
 */
class LayerGroupEntryRenderer implements IChoiceRenderer<EoLayerGroupEntry> {
    private static final long serialVersionUID = 4073120717494762957L;

    @Override
    public Object getDisplayValue(EoLayerGroupEntry entry) {
        return entry.getLayer().prefixedName();
    }

    @Override
    public String getIdValue(EoLayerGroupEntry entry, int index) {
        return entry.getLayer().getId();
    }

}
