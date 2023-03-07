/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.web.data.settings.SettingsPluginPanel;

/** Configuration panel for OGC API links attached to either a workspace or a global settings */
public class OGCAPILinksSettingsPanel extends SettingsPluginPanel {

    public OGCAPILinksSettingsPanel(String id, IModel<SettingsInfo> model) {
        super(id, model);
        this.setOutputMarkupId(true);

        final PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "metadata");
        IModel<List<LinkInfo>> linksModel = new LinksMetadataMapModel(metadata);
        LinkInfoEditor linkEditor = new LinkInfoEditor("linksEditor", linksModel, this);
        linkEditor.setOutputMarkupId(true);
        add(linkEditor);
    }
}
