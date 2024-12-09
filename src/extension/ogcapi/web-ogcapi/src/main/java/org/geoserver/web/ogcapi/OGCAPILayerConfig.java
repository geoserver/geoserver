/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MetadataMapModel;

/** Configuration panel for OGC API links attached to a given layer/layer group */
public class OGCAPILayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private final LinkInfoEditor linkEditor;
    private final MetadataMapModel<List<LinkInfo>> linksModel;

    @SuppressWarnings("unchecked")
    public OGCAPILayerConfig(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        this.setOutputMarkupId(true);
        PropertyModel<MetadataMap> metadata = new PropertyModel<>(layerModel, "resource.metadata");
        linksModel = new LinksMetadataMapModel(metadata);
        linkEditor = new LinkInfoEditor("linksEditor", this.linksModel, this);
        linkEditor.setOutputMarkupId(true);
        add(linkEditor);
    }
}
