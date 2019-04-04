/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.StoreInfo;

/** Simple choice renderer for {@link StoreInfo} */
@SuppressWarnings("serial")
public class StoreChoiceRenderer extends ChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((StoreInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((StoreInfo) object).getId();
    }
}
