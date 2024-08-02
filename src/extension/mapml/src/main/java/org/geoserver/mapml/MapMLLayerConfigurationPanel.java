/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
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
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.WMS;
import org.geotools.util.logging.Logging;
import org.geowebcache.layer.TileLayer;

/**
 * Resource configuration panel for MapML
 *
 * @author Chris Hodgson
 * @author prushforth
 */
public class MapMLLayerConfigurationPanel extends PublishedConfigurationPanel<LayerInfo> {
    static final Logger LOGGER = Logging.getLogger(MapMLLayerConfigurationPanel.class);

    private static final long serialVersionUID = 1L;
    public static final String PNG_MIME_TYPE = "image/png";
    ListMultipleChoice<String> featureCaptionAttributes;

    private static final String MIME_PATTERN = "png|jpeg";

    public static final Pattern mimePattern =
            Pattern.compile(MIME_PATTERN, Pattern.CASE_INSENSITIVE);

    DropDownChoice<String> mime;

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
        useTiles.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        ajaxRequestTarget.add(mime);
                        boolean useTilesChecked = useTiles.getConvertedInput();
                        mime.setChoices(getAvailableMimeTypes(model.getObject(), useTilesChecked));
                    }
                });
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
        useFeatures.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(mime);
                        // if we are using features, we don't use a default mime type
                        mime.setEnabled(!useFeatures.getConvertedInput());
                    }
                });
        add(useFeatures);

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

        MapModel<String> mimeModel =
                new MapModel<>(
                        new PropertyModel<MetadataMap>(model, MapMLConstants.RESOURCE_METADATA),
                        MapMLConstants.MAPML_MIME);
        boolean useTilesFromModel =
                Boolean.TRUE.equals(
                        model.getObject()
                                .getResource()
                                .getMetadata()
                                .get(MAPML_USE_TILES, Boolean.class));
        mime =
                new DropDownChoice<>(
                        MapMLConstants.MIME,
                        mimeModel,
                        getAvailableMimeTypes(model.getObject(), useTilesFromModel));
        mime.setOutputMarkupId(true);
        mime.setNullValid(false);
        // if we are using features, we don't use a mime type
        if (useFeaturesModel.getObject() != null) {
            String useFeaturesString = String.valueOf(useFeaturesModel.getObject());
            boolean useFeaturesBoolean = Boolean.parseBoolean(useFeaturesString);
            mime.setEnabled(!useFeaturesBoolean);
        }
        add(mime);

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
     * Get the available mime types for the layer
     *
     * @param layer the layer to get the mime types for
     * @return a list of strings of mime types
     */
    public static List<String> getAvailableMimeTypes(PublishedInfo layer, boolean useTiles) {
        List<String> mimeTypes = new ArrayList<>();
        if (useTiles) {
            GWC gwc = GWC.get();
            if (gwc != null) {
                try {
                    TileLayer tileLayer = gwc.getTileLayerByName(layer.prefixedName());
                    // if the useTiles flag is set and the cache is enabled we get cache mime types
                    if (tileLayer instanceof GeoServerTileLayer && tileLayer.isEnabled()) {
                        GeoServerTileLayer geoServerTileLayer = (GeoServerTileLayer) tileLayer;
                        GeoServerTileLayerInfo info = geoServerTileLayer.getInfo();
                        mimeTypes.addAll(
                                info.getMimeFormats().stream()
                                        .filter(mimeType -> mimePattern.matcher(mimeType).find())
                                        .collect(Collectors.toList()));
                        return mimeTypes;
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.fine("No tile layer found for " + layer.prefixedName());
                }
            }
        }
        // if the useTiles flag is not set or the tile cache is not enabled we get WMS mime types
        WMS wms = WMS.get();
        if (wms != null) {
            mimeTypes.addAll(
                    wms.getAllowedMapFormatNames().stream()
                            .filter(mimeType -> mimePattern.matcher(mimeType).find())
                            .collect(Collectors.toList()));
        }

        return mimeTypes;
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
