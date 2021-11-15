/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout.web;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ServiceInfo;
import org.geoserver.nsg.timeout.TimeoutCallback;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MapModel;

/** Panel to set the WFS timeout value */
public class NSGTimeoutPanel extends AdminPagePanel {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public NSGTimeoutPanel(final String id, final IModel<ServiceInfo> model) {
        super(id, model);

        PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "metadata");
        MapModel timeoutModel = new MapModel(metadata, TimeoutCallback.TIMEOUT_CONFIG_KEY);
        if (timeoutModel.getObject() == null) {
            timeoutModel.setObject(TimeoutCallback.TIMEOUT_CONFIG_DEFAULT);
        }
        TextField<Integer> timeoutField =
                new TextField<>("timeoutSeconds", timeoutModel, Integer.class);
        timeoutField.setRequired(true);
        timeoutField.add(RangeValidator.minimum(0));
        add(timeoutField);
    }
}
