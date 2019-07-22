/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;

/** Resource configuration panel for MapML */
public class MapMLLayerConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 1L;

    /** Adds MapML configuration panel */
    public MapMLLayerConfigurationPanel(final String panelId, final IModel<LayerInfo> model) {
        super(panelId, model);

        MapModel<String> licenseTitleModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.licenseTitle");
        TextField<String> licenseTitle = new TextField<String>("licenseTitle", licenseTitleModel);
        add(licenseTitle);

        MapModel<String> licenseLinkModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.licenseLink");
        TextField<String> licenseLink = new TextField<String>("licenseLink", licenseLinkModel);
        add(licenseLink);

        // add the checkbox to select tiled or not
        MapModel<Boolean> useTilesModel =
                new MapModel<Boolean>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.useTiles");
        CheckBox useTiles = new CheckBox("useTiles", useTilesModel);
        add(useTiles);

        // add the checkbox to enable sharding or not
        MapModel<Boolean> enableShardingModel =
                new MapModel<Boolean>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.enableSharding");
        CheckBox enableSharding = new CheckBox("enableSharding", enableShardingModel);
        add(enableSharding);

        MapModel<String> shardListModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.shardList");
        TextField<String> shardList = new TextField<String>("shardList", shardListModel);
        add(shardList);

        MapModel<String> shardServerPatternModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.shardServerPattern");
        TextField<String> shardServerPattern =
                new TextField<String>("shardServerPattern", shardServerPatternModel);
        add(shardServerPattern);

        MapModel<String> dimensionModel =
                new MapModel<String>(
                        new PropertyModel<MetadataMap>(model, "resource.metadata"),
                        "mapml.dimension");
        DropDownChoice<String> dimension =
                new DropDownChoice<String>(
                        "dimension", dimensionModel, getEnabledDimensionNames(model.getObject()));
        dimension.setNullValid(true);
        add(dimension);
    }

    List<String> getEnabledDimensionNames(LayerInfo layer) {
        List<String> dimensionNames = new ArrayList<String>();
        for (Map.Entry<String, Serializable> entry : layer.getResource().getMetadata().entrySet()) {
            String key = entry.getKey();
            Serializable md = entry.getValue();
            if (md instanceof DimensionInfo) {
                // skip disabled dimensions
                DimensionInfo di = (DimensionInfo) md;
                if (!di.isEnabled()) {
                    continue;
                }

                // get the dimension name
                String dimensionName;
                if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                    dimensionName = key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                } else {
                    dimensionName = key;
                }
                dimensionNames.add(dimensionName);
            }
        }
        return dimensionNames;
    }
}
