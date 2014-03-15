/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.style;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

public class StyleDetachableModel extends LoadableDetachableModel {

    String id;
    
    public StyleDetachableModel(StyleInfo style) {
        this.id = style.getId();
    }
    
    @Override
    protected Object load() {
        StyleInfo style = GeoServerApplication.get().getCatalog().getStyle( id );

        // Make sure the legend object isn't null
        if (null == style.getLegend()) {
            style.setLegend(GeoServerApplication.get().getCatalog().getFactory().createLegend());
        }
        return style;
    }

}
