/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

/**
 * Resource configuration panel for MapML
 *
 * @author Chris Hodgson
 * @author prushforth
 */
public class MapMLLayerConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {
    static final Logger LOGGER = Logging.getLogger(MapMLLayerConfigurationPanel.class);

    private static final long serialVersionUID = 1L;
    ListMultipleChoice<String> featureCaptionAttributes;

    /**
     * Adds MapML configuration panel
     *
     * @param panelId
     * @param model
     */
    public MapMLLayerConfigurationPanel(final String panelId, final IModel<LayerInfo> model) {
        super(panelId, model);

        MapModel<String> licenseTitleModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.LICENSE_TITLE);
        TextField<String> licenseTitle =
                new TextField<>(MapMLConstants.LICENSE_TITLE2, licenseTitleModel);
        add(licenseTitle);

        MapModel<String> licenseLinkModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.LICENSE_LINK);
        TextField<String> licenseLink = new TextField<>(MapMLConstants.LICENSE, licenseLinkModel);
        add(licenseLink);

        // add the checkbox to select tiled or not
        MapModel<Boolean> useTilesModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_USE_TILES);
        CheckBox useTiles = new CheckBox(MapMLConstants.USE_TILES, useTilesModel);
        add(useTiles);

        // add the checkbox to select features or not
        MapModel<Boolean> useFeaturesModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_USE_FEATURES);
        CheckBox useFeatures = new CheckBox(MapMLConstants.USE_FEATURES, useFeaturesModel);
        if (model.getObject() != null && model.getObject() instanceof PublishedInfo) {
            if (((PublishedInfo) model.getObject()).getType() == PublishedType.RASTER) {
                useFeatures.setEnabled(false);
            }
        }
        add(useFeatures);

        // add the checkbox to enable sharding or not
        MapModel<Boolean> enableShardingModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_PREFIX + MapMLConstants.ENABLE_SHARDING);
        CheckBox enableSharding = new CheckBox(MapMLConstants.ENABLE_SHARDING, enableShardingModel);
        add(enableSharding);

        MapModel<String> shardListModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_PREFIX + MapMLConstants.SHARD_LIST);
        TextField<String> shardList = new TextField<>(MapMLConstants.SHARD_LIST, shardListModel);
        add(shardList);

        MapModel<String> shardServerPatternModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_PREFIX + MapMLConstants.SHARD_SERVER_PATTERN);
        TextField<String> shardServerPattern =
                new TextField<>(MapMLConstants.SHARD_SERVER_PATTERN, shardServerPatternModel);
        add(shardServerPattern);

        MapModel<String> dimensionModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_DIMENSION);
        DropDownChoice<String> dimension =
                new DropDownChoice<>(
                        MapMLConstants.DIMENSION,
                        dimensionModel,
                        getEnabledDimensionNames(model.getObject()));
        dimension.setNullValid(true);
        add(dimension);

        featureCaptionAttributes =
                new ListMultipleChoice<>(
                        MapMLConstants.FEATURE_CAPTION_ATTRIBUTES,
                        new Model<ArrayList<String>>(),
                        getAttributeNames(model.getObject()));
        featureCaptionAttributes.setOutputMarkupId(false);
        add(featureCaptionAttributes);

        MapModel<String> featureCaptionModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.FEATURE_CAPTION);
        TextArea<String> featureCaptionTemplate =
                new TextArea<>(MapMLConstants.FEATURE_CAPTION_TEMPLATE, featureCaptionModel);
        add(featureCaptionTemplate);
    }

    /**
     * @param layer a LayerInfo for the layer
     * @return a list of strings of dimension names from the layer info
     */
    List<String> getEnabledDimensionNames(LayerInfo layer) {
        List<String> dimensionNames = new ArrayList<>();
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

    /**
     * Process the layer and return all the attribute names or band/dimension names
     *
     * @param layer
     * @return A list of attribtes or band/dimension names as strings.
     */
    private List<String> getAttributeNames(LayerInfo layer) {
        List<String> attributeNames = new ArrayList<>();
        ResourceInfo res = layer.getResource();
        if (!(res instanceof FeatureTypeInfo || res instanceof CoverageInfo)) {
            return Collections.emptyList();
        }
        try {
            if (res instanceof FeatureTypeInfo) {
                FeatureTypeInfo typeInfo = (FeatureTypeInfo) res;
                Catalog catalog = GeoServerApplication.get().getCatalog();
                final ResourcePool resourcePool = catalog.getResourcePool();
                // using loadAttributes to dodge the ResourcePool caches, the
                // feature type structure might have been modified (e.g., SQL view editing)
                for (AttributeTypeInfo a : resourcePool.loadAttributes(typeInfo)) {
                    attributeNames.add(a.getName());
                }
            } else {
                CoverageInfo covInfo = (CoverageInfo) res;
                List<CoverageDimensionInfo> dimensions = covInfo.getDimensions();
                Iterator<CoverageDimensionInfo> cdi = dimensions.iterator();
                while (cdi.hasNext()) {
                    CoverageDimensionInfo cd = cdi.next();
                    // aargh coverage dimension names can have spaces in them
                    attributeNames.add(cd.getName());
                }
            }
            return attributeNames;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, MapMLConstants.LIST_FAILED, e);
            String error =
                    new ParamResourceModel(
                                    MapMLConstants.ATTRIBUTE_LIST_FAILED, this, e.getMessage())
                            .getString();
            this.getPage().error(error);
            return Collections.emptyList();
        }
    }
}
