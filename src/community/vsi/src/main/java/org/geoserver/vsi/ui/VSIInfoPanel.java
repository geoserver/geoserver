/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.vsi.ui;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

/**
 * Basic data store panel for VSI sources.
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSIInfoPanel extends StoreEditPanel {
    public VSIInfoPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);

        IModel model = storeEditForm.getModel();
        setDefaultModel(model);

        add(
                new TextParamPanel(
                        "location",
                        new PropertyModel(model, "url"),
                        new ResourceModel("location", "Location"),
                        true,
                        null));
    }
}
