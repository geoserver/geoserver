/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.raster;

import org.apache.wicket.markup.html.form.Form;

@SuppressWarnings("serial")
public class NetCDFRasterEditPanel extends AbstractRasterFileEditPanel {

    public NetCDFRasterEditPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm, new String[] { ".nc", ".cdf", ".netcdf", ".nc3", ".nc4" });
    }

}