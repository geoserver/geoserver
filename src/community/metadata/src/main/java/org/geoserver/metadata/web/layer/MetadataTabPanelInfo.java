/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.layer;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.PublishedEditTabPanelInfo;

public class MetadataTabPanelInfo extends PublishedEditTabPanelInfo<LayerInfo> {

    private static final long serialVersionUID = 7092956796960461825L;

    @Override
    public Class<LayerInfo> getPublishedInfoClass() {
        return LayerInfo.class;
    }

    @Override
    public IModel<?> createOwnModel(IModel<? extends LayerInfo> model, boolean isNew) {
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);

        List<MetadataTemplate> selectedTemplates = new ArrayList<>();
        for (MetadataTemplate template : service.list()) {
            if (template.getLinkedLayers().contains(model.getObject().getResource().getId())) {
                selectedTemplates.add(template);
            }
        }
        return new ListModel<MetadataTemplate>(selectedTemplates);
    }
}
