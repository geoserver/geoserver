/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Type;


/**
 * Simple choice renderer for {@link LayerGroupInfo.Type}
 */
@SuppressWarnings("serial")
public class LayerGroupTypeChoiceRenderer implements IChoiceRenderer<Type> {

    @Override
    public Object getDisplayValue(Type type) {
        return type.getName();
    }

    @Override
    public String getIdValue(Type type, int index) {
        return type.getCode().toString();
    }
}