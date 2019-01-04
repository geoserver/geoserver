/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.StoreInfo;

/**
 * Renders a StoreInfo into a public name
 *
 * @author Andrea Aime - GeoSolutions
 */
public class StoreListChoiceRenderer extends ChoiceRenderer<StoreInfo> {

    public Object getDisplayValue(StoreInfo info) {
        return new StringBuilder(info.getWorkspace().getName()).append(':').append(info.getName());
    }

    public String getIdValue(StoreInfo store, int arg1) {
        return store.getId();
    }
}
