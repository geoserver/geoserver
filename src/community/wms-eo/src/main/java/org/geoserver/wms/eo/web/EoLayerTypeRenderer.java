/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.eo.EoLayerType;

/**
 * Renders internationalized strings for the {@link EoLayerType} enum items
 *
 * @author Andrea Aime - GeoSolutions
 */
public class EoLayerTypeRenderer extends ChoiceRenderer<EoLayerType> {
    private static final long serialVersionUID = -6351905251612872379L;

    @Override
    public Object getDisplayValue(EoLayerType object) {
        return new ParamResourceModel("EoLayerType." + object.name(), null).getString();
    }

    @Override
    public String getIdValue(EoLayerType object, int index) {
        return object.name();
    }
}
