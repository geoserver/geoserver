/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.web.publish.PublishedEditTabPanel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.logging.Logging;

/**
 * Plugs into the layer page a time/elevation selector for vector data
 *
 * @author Alessio
 */
public class ResourceDimensionsTabPanelInfo extends PublishedEditTabPanel<LayerInfo> implements MetadataMapValidator {

    private static final long serialVersionUID = 4702596541385329270L;

    static final Logger LOGGER = Logging.getLogger(ResourceDimensionsTabPanelInfo.class);

    @SuppressWarnings("unchecked")
    public ResourceDimensionsTabPanelInfo(String id, IModel<LayerInfo> model) {
        super(id, model);

        final LayerInfo layer = model.getObject();
        final ResourceInfo resource = layer.getResource();

        final PropertyModel<MetadataMap> metadata = new PropertyModel<>(model, "resource.metadata");

        // time
        IModel<DimensionInfo> time = new MetadataMapModel<>(metadata, ResourceInfo.TIME, DimensionInfo.class);
        if (time.getObject() == null) {
            time.setObject(new DimensionInfoImpl());
        }
        add(new DimensionEditor("time", time, resource, Date.class, true, true));

        // elevation
        IModel<DimensionInfo> elevation = new MetadataMapModel<>(metadata, ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevation.getObject() == null) {
            elevation.setObject(new DimensionInfoImpl());
        }
        add(new DimensionEditor("elevation", elevation, resource, Number.class));

        // handle raster data custom dimensions
        final List<RasterDimensionModel> customDimensionModels = new ArrayList<>();
        if (resource instanceof CoverageInfo) {
            CoverageInfo ci = (CoverageInfo) resource;
            try {
                GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
                ReaderDimensionsAccessor ra = new ReaderDimensionsAccessor(reader);

                for (String domain : ra.getCustomDomains()) {
                    boolean hasRange = ra.hasRange(domain);
                    boolean hasResolution = ra.hasResolution(domain);
                    RasterDimensionModel mm =
                            new RasterDimensionModel<>(metadata, domain, DimensionInfo.class, hasRange, hasResolution);
                    if (mm.getObject() == null) {
                        mm.setObject(new DimensionInfoImpl());
                    }
                    customDimensionModels.add(mm);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to access coverage reader custom dimensions", e);
            }
        }
        RefreshingView customDimensionsEditor = new RefreshingView("customDimensions") {

            @Override
            protected Iterator getItemModels() {
                return customDimensionModels.iterator();
            }

            @Override
            protected void populateItem(Item item) {
                RasterDimensionModel model = (RasterDimensionModel) item.getModel();
                ParamResourceModel customDimension =
                        new ParamResourceModel("customDimension", ResourceDimensionsTabPanelInfo.this);
                item.add(new Label("dimensionName", customDimension.getString() + ": " + model.getExpression()));
                DimensionEditor editor = new DimensionEditor("dimension", model, resource, String.class);
                editor.disablePresentationMode(DimensionPresentation.CONTINUOUS_INTERVAL);
                if (!model.hasRange && !model.hasResolution) {
                    editor.disablePresentationMode(DimensionPresentation.DISCRETE_INTERVAL);
                }
                item.add(editor);
            }
        };
        add(customDimensionsEditor);
        customDimensionsEditor.setVisible(!customDimensionModels.isEmpty());

        // vector custom dimensions panel
        buildVectorCustomDimensionsPanel(model, resource);
    }

    private void buildVectorCustomDimensionsPanel(IModel<LayerInfo> model, final ResourceInfo resource) {
        WebMarkupContainer vectorCustomDimPanel;
        // vector custom dimensions panel
        if (resource instanceof FeatureTypeInfo) {
            final PropertyModel<FeatureTypeInfo> typeInfoModel = new PropertyModel<>(model, "resource");
            vectorCustomDimPanel = new VectorCustomDimensionsPanel("vectorCustomDimPanel", typeInfoModel);
        } else {
            vectorCustomDimPanel = new WebMarkupContainer("vectorCustomDimPanel");
            vectorCustomDimPanel.setVisible(false);
        }
        this.add(vectorCustomDimPanel);
    }

    @Override
    public void validate(MetadataMap map) {
        if (metadataContainsRepeatedDimension("time", map) || metadataContainsRepeatedDimension("elevation", map)) {
            throw new IllegalArgumentException("Repeated dimensions names not allowed.");
        }
    }

    private boolean metadataContainsRepeatedDimension(String name, MetadataMap map) {
        return isDimensionEnabled(name, map) && isDimensionEnabled("dim_" + name, map);
    }

    private boolean isDimensionEnabled(String name, MetadataMap map) {
        Serializable object = map.get(name);
        if (object == null || !(object instanceof DimensionInfo)) return false;
        DimensionInfo info = (DimensionInfo) object;
        return info.isEnabled();
    }

    static class RasterDimensionModel<T> extends MetadataMapModel<T> {
        private static final long serialVersionUID = 4734439907138483817L;

        boolean hasRange;

        boolean hasResolution;

        public RasterDimensionModel(
                IModel<MetadataMap> model,
                String expression,
                Class<T> target,
                boolean hasRange,
                boolean hasResolution) {
            super(model, expression, target);
        }

        @Override
        @SuppressWarnings("unchecked")
        public T getObject() {
            return (T) (model.getObject()).get(ResourceInfo.CUSTOM_DIMENSION_PREFIX + expression, target);
        }

        @Override
        public void setObject(T object) {
            (model.getObject()).put(ResourceInfo.CUSTOM_DIMENSION_PREFIX + expression, (Serializable) object);
        }
    }
}
