/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ncwms;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.services.AdminPagePanel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.wms.ncwms.NcWMSInfoImpl;
import org.geoserver.wms.ncwms.NcWmsInfo;
import org.geoserver.wms.ncwms.NcWmsService;

public class NcWmsAdminPanel extends AdminPagePanel {

    public NcWmsAdminPanel(String id, IModel<?> model) {
        super(id, model);

        IModel<NcWmsInfo> ncWmsModel = new MetadataMapModel<>(
                new PropertyModel<>(model, "metadata"), NcWmsService.WMS_CONFIG_KEY, NcWmsInfo.class);
        if (ncWmsModel.getObject() == null) ncWmsModel.setObject(new NcWMSInfoImpl());
        add(new TextField<>("timeSeriesPoolSize", new PropertyModel<>(ncWmsModel, "timeSeriesPoolSize")));
        add(new TextField<>("maxTimeSeriesValues", new PropertyModel<>(ncWmsModel, "maxTimeSeriesValues")));
    }
}
