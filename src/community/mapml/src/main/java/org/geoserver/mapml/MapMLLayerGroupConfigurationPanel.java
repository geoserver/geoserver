/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

/** LayerGroup configuration panel for MapML */
public class MapMLLayerGroupConfigurationPanel extends PublishedConfigurationPanel<LayerGroupInfo> {

    private static final long serialVersionUID = 1L;

    /** Adds MapML configuration panel */
    public MapMLLayerGroupConfigurationPanel(
            final String panelId, final IModel<LayerGroupInfo> model) {
        super(panelId, model);

        MapModel<String> licenseTitleModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "metadata"), "mapml.licenseTitle");
        TextField<String> licenseTitle = new TextField<String>("licenseTitle", licenseTitleModel);
        add(licenseTitle);

        MapModel<String> licenseLinkModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "metadata"), "mapml.licenseLink");
        TextField<String> licenseLink = new TextField<String>("licenseLink", licenseLinkModel);
        add(licenseLink);

        // add the checkbox to select tiled or not
        MapModel<Boolean> useTilesModel =
                new MapModel<Boolean>(
                        new PropertyModel<MetadataMap>(model, "metadata"), "mapml.useTiles");
        CheckBox useTiles = new CheckBox("useTiles", useTilesModel);
        add(useTiles);

        // add the checkbox to enable sharding or not
        MapModel<Boolean> enableShardingModel =
                new MapModel<Boolean>(
                        new PropertyModel<MetadataMap>(model, "metadata"), "mapml.enableSharding");
        CheckBox enableSharding = new CheckBox("enableSharding", enableShardingModel);
        add(enableSharding);

        MapModel<String> shardListModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "metadata"), "mapml.shardList");
        TextField<String> shardList = new TextField<String>("shardList", shardListModel);
        add(shardList);

        MapModel<String> shardServerPatternModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "metadata"),
                        "mapml.shardServerPattern");
        TextField<String> shardServerPattern =
                new TextField<String>("shardServerPattern", shardServerPatternModel);
        add(shardServerPattern);
    }
}
