/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.s3.ui;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

/**
 * Just a basic data store info panel that skips the file based validation present in the GeoServer
 * data store
 */
public class S3InfoPanel extends StoreEditPanel {
    public S3InfoPanel(String componentId, Form storeEditForm) {
        super(componentId, storeEditForm);

        IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        IModel paramsModel = new PropertyModel(model, "connectionParameters");
        TextParamPanel urlPanel =
                new TextParamPanel(
                        "url",
                        new PropertyModel(model, "URL"),
                        new ResourceModel("url", "URL"),
                        true,
                        null);
        add(urlPanel);
    }
}
