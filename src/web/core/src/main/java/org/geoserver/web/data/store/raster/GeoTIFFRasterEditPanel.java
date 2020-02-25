/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.raster;

import org.apache.wicket.markup.html.form.Form;

@SuppressWarnings("serial")
public class GeoTIFFRasterEditPanel extends AbstractRasterFileEditPanel {

    public GeoTIFFRasterEditPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm, new String[] {".tiff", ".tif"});
    }
}
