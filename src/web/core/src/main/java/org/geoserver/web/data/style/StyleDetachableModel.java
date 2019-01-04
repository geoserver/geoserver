/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.style;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;

public class StyleDetachableModel extends LoadableDetachableModel<StyleInfo> {

    private static final long serialVersionUID = 1429186780251933703L;

    String id;

    public StyleDetachableModel(StyleInfo style) {
        this.id = style.getId();
    }

    @Override
    protected StyleInfo load() {
        StyleInfo style = GeoServerApplication.get().getCatalog().getStyle(id);

        // Make sure the legend object isn't null
        if (null == style.getLegend()) {
            style.setLegend(GeoServerApplication.get().getCatalog().getFactory().createLegend());
        }
        return style;
    }

    @Override
    protected void onDetach() {
        super.onDetach();
    }
}
