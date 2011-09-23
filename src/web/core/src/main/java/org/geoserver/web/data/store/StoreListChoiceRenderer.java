/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.StoreInfo;

/**
 * Renders a StoreInfo into a public name
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public class StoreListChoiceRenderer implements IChoiceRenderer<StoreInfo> {

    public Object getDisplayValue(StoreInfo info) {
        return new StringBuilder(info.getWorkspace().getName()).append(':').append(info.getName());
    }

    public String getIdValue(StoreInfo store, int arg1) {
        return store.getId();
    }

}