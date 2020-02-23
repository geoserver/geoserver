/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

/** Base class for admin panel extensions. */
public abstract class NetCDFExtensionPanel extends Panel {

    public NetCDFExtensionPanel(String id, IModel<?> model) {
        super(id, model);
    }

    /**
     * Writes its input into the provided settings, as participation in {@link NetCDFPanel} {@link
     * FormComponentPanel#convertInput()}
     */
    public abstract void convertInput(NetCDFSettingsContainer settings);
}
