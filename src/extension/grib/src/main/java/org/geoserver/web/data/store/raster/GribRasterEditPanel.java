/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store.raster;

import org.apache.wicket.markup.html.form.Form;

@SuppressWarnings("serial")
public class GribRasterEditPanel extends AbstractRasterFileEditPanel {

    public GribRasterEditPanel(String componentId, Form storeEditForm) {
        super(
                componentId,
                storeEditForm,
                new String[] {
                    ".gr", ".gr1", ".grb", ".grib", ".grb1", ".grib1", ".gr2", ".grib2", ".grb2"
                });
    }
}
