/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.raster;

import org.apache.wicket.markup.html.form.Form;

/**
 * A generic raster file panel that won't filter files by extension. Good when there are no file
 * naming rules
 */
@SuppressWarnings("serial")
public class FileRasterEditPanel extends AbstractRasterFileEditPanel {

    public FileRasterEditPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);
    }
}
