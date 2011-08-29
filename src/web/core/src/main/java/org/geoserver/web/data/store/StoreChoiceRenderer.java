/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.StoreInfo;

/**
 * Simple choice renderer for {@link StoreInfo}
 */
@SuppressWarnings("serial")
public class StoreChoiceRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return ((StoreInfo) object).getName();
    }

    public String getIdValue(Object object, int index) {
        return ((StoreInfo) object).getId();
    }

}
