/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.LayerGroupInfo;


/**
 * Simple choice renderer for {@link LayerGroupInfo}
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
@SuppressWarnings("serial")
public class LayerGroupInfoChoiceRenderer implements IChoiceRenderer<LayerGroupInfo> {

    public Object getDisplayValue(LayerGroupInfo group) {
        return group.prefixedName();
    }

    public String getIdValue(LayerGroupInfo group, int index) {
        return group.getId();
    }
}