/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
        return GeoServerApplication.get().getCatalog().getStyle( id );
    }

    @Override
    protected void onDetach() {
        // TODO Auto-generated method stub
        super.onDetach();
    }

}
