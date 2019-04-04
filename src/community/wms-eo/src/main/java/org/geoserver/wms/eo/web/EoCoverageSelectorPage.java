/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SingleGridCoverage2DReader;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.store.StoreChoiceRenderer;
import org.geoserver.web.data.store.StoreNameComparator;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.eo.EoLayerType;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;

public class EoCoverageSelectorPage extends GeoServerSecuredPage {

    static final Logger LOGGER = Logging.getLogger(EoCoverageSelectorPage.class);

    List<EoCoverageSelection> selections =
            new ArrayList<EoCoverageSelectorPage.EoCoverageSelection>();

    CoverageStoreInfo selectedStore;

    private DropDownChoice<CoverageStoreInfo> stores;

    private List<EoLayerGroupEntry> layerGroupEntries;

    private String groupName;

    public EoCoverageSelectorPage(
            EoLayerGroupAbstractPage owner, String layerGroupName, String coverageStoreId) {
        this(owner, layerGroupName);

        this.groupName = layerGroupName;
        CoverageStoreInfo store = getCatalog().getStore(coverageStoreId, CoverageStoreInfo.class);
        stores.setDefaultModelObject(store);
        stores.setVisible(false);
        updateCoveragesList(true);
    }

    public EoCoverageSelectorPage(EoLayerGroupAbstractPage returnPage, String groupName) {
        setReturnPage(returnPage);
        this.groupName = groupName;
        layerGroupEntries = returnPage.lgEntryPanel.entryProvider.getItems();
        Form form = new Form("form");
        add(form);

        List<CoverageStoreInfo> coverageStores =
                new ArrayList<CoverageStoreInfo>(getCatalog().getCoverageStores());
        Collections.sort(coverageStores, new StoreNameComparator());
        stores =
                new DropDownChoice<CoverageStoreInfo>(
                        "store", coverageStores, new StoreChoiceRenderer());
        stores.setModel(new PropertyModel(this, "selectedStore"));
        stores.setRequired(true);
        form.add(stores);

        final WebMarkupContainer coveragesContainer = new WebMarkupContainer("coveragesContainer");
        coveragesContainer.setOutputMarkupId(true);
        form.add(coveragesContainer);

        GeoServerTablePanel<EoCoverageSelection> coverages =
                new GeoServerTablePanel<EoCoverageSelection>(
                        "coverages", new EoCoverageSelectionProvider(selections)) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<EoCoverageSelection> itemModel,
                            Property<EoCoverageSelection> property) {
                        if ("type".equals(property.getName())) {
                            DropDownChoice<EoLayerType> layerTypes =
                                    new DropDownChoice<EoLayerType>(
                                            "type",
                                            (IModel<EoLayerType>) property.getModel(itemModel),
                                            EoLayerType.getRasterTypes(true),
                                            new EoLayerTypeRenderer());
                            Fragment fragment =
                                    new Fragment(id, "typeFragment", EoCoverageSelectorPage.this);
                            fragment.add(layerTypes);
                            return fragment;
                        }

                        return null;
                    }
                };
        coverages.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        coveragesContainer.add(coverages);

        // link the store dropdown to the coverages table
        stores.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        updateCoveragesList(true);
                        target.add(coveragesContainer);
                        addFeedbackPanels(target);
                    }
                });

        form.add(addToLayerGroupLink());
        form.add(cancelLink());
    }

    protected void updateCoveragesList(boolean reportSkippedLayers) {
        selections.clear();
        if (stores.getModelObject() != null) {
            try {
                CoverageStoreInfo store = (CoverageStoreInfo) stores.getModelObject();
                GridCoverageReader reader = store.getGridCoverageReader(null, null);
                String[] names = reader.getGridCoverageNames();
                Arrays.sort(names);
                for (String name : names) {
                    LayerInfo li = getPreExistingLayer(name, store);
                    if (li == null) {
                        selections.add(new EoCoverageSelection(name, EoLayerType.IGNORE));
                    } else if (reportSkippedLayers) {
                        info(
                                "Skipping coverage "
                                        + name
                                        + " as it's already part of the group as "
                                        + li.getName());
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to list coverages in the specified store", e);
                error("Failed to list coverages in the specified store: " + e.getMessage());
            }
        }
    }

    private LayerInfo getPreExistingLayer(String coverageName, StoreInfo si) {
        for (EoLayerGroupEntry entry : layerGroupEntries) {
            if (entry.getLayer() instanceof LayerInfo) {
                LayerInfo li = (LayerInfo) entry.getLayer();
                if (li.getResource() instanceof CoverageInfo) {
                    CoverageInfo ci = (CoverageInfo) li.getResource();
                    // if same store, check the native name
                    if (ci.getStore().getId().equals(si.getId())) {
                        if (ci.getNativeName().equals(coverageName)) {
                            return li;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Component cancelLink() {
        return new AjaxLink<String>("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn();
            }
        };
    }

    private SubmitLink addToLayerGroupLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                CatalogBuilder builder = new CatalogBuilder(getCatalog());
                builder.setStore(selectedStore);

                EoLayerGroupAbstractPage layerGroupPage = (EoLayerGroupAbstractPage) returnPage;
                List<EoLayerGroupEntry> items =
                        layerGroupPage.lgEntryPanel.entryProvider.getItems();
                boolean error = false;
                for (EoCoverageSelection item : selections) {
                    if (item.getType() != EoLayerType.IGNORE) {
                        try {
                            LayerInfo layer = createLayer(groupName, item, builder);
                            if (layer == null) {
                                error = true;
                            } else {
                                items.add(
                                        new EoLayerGroupEntry(
                                                layer,
                                                layer.getDefaultStyle(),
                                                layerGroupPage.lgModel.getObject().getName()));
                                info(
                                        "Layer "
                                                + layer.getName()
                                                + " successfully created and added to the EO layer group");
                            }
                        } catch (Exception e) {
                            error = true;
                            error(
                                    "Failed to create layer from covearge "
                                            + item.getCoverageName()
                                            + ": "
                                            + e.getMessage());
                        }
                    }
                }

                if (!error) {
                    setResponsePage(returnPage);
                } else {
                    updateCoveragesList(true);
                }
            }
        };
    }

    LayerInfo createLayer(String groupName, EoCoverageSelection selection, CatalogBuilder builder) {
        String coverageName = selection.getCoverageName();
        EoLayerType layerType = selection.getType();
        String name = coverageName;
        if (groupName != null) {
            name = groupName + "_" + name;
        }
        try {
            // build the coverage and enable its dimensions
            CoverageInfo resource = builder.buildCoverage(coverageName);
            boolean dimensionsPresent = enableDimensions(resource, layerType);
            if (!dimensionsPresent) {
                if (layerType == EoLayerType.BAND_COVERAGE) {
                    error(
                            new ParamResourceModel(
                                            "EoLayerGroupError.invalidBandCoverage",
                                            null,
                                            coverageName)
                                    .getString());
                } else {
                    error(
                            new ParamResourceModel(
                                            "EoLayerGroupError.invalidLayer", null, coverageName)
                                    .getString());
                }
                return null;
            }

            // update the name and save the coverage
            resource.setName(name);
            resource.setTitle(name);
            getCatalog().add(resource);

            // save the layer too
            LayerInfo layer = builder.buildLayer(resource);
            layer.setName(name);
            layer.setTitle(name);
            layer.setEnabled(true);
            layer.setQueryable(true);
            layer.setType(PublishedType.RASTER);
            layer.getMetadata().put(EoLayerType.KEY, layerType.name());
            if (layerType == EoLayerType.BITMASK) {
                StyleInfo red = getCatalog().getStyleByName("red");
                if (red != null) {
                    layer.setDefaultStyle(red);
                } else {
                    warn(
                            "Default style for bitmask layers 'red' was not found, the default 'raster' layer got assigned instead. Please fix");
                }
            }
            getCatalog().add(layer);

            return layer;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "The layer '"
                            + name
                            + "' could not be created. Failure message: "
                            + e.getMessage(),
                    e);
        }
    }

    /**
     * Check presence of TIME dimension and, if the layer is of band type, a custom dimension Enable
     * all dimensions found.
     */
    private boolean enableDimensions(CoverageInfo ci, EoLayerType type) {
        boolean timeDimension = false;
        boolean customDimension = false;
        GridCoverage2DReader reader = null;
        try {
            // acquire a reader
            reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
            if (reader == null) {
                throw new RuntimeException(
                        "Unable to acquire reader for this coverageinfo: " + ci.getName());
            }
            if (ci.getNativeCoverageName() != null) {
                reader = SingleGridCoverage2DReader.wrap(reader, ci.getNativeCoverageName());
            }

            // inspect dimensions
            final ReaderDimensionsAccessor ra = new ReaderDimensionsAccessor(reader);
            for (String domain : ra.getCustomDomains()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    boolean hasRange = ra.hasRange(domain);
                    boolean hasResolution = ra.hasResolution(domain);
                    LOGGER.fine(
                            ci.getName()
                                    + ": found "
                                    + domain
                                    + " dimension (hasRange: "
                                    + hasRange
                                    + ", hasResolution: "
                                    + hasResolution
                                    + ")");
                }

                DimensionInfo dimension = new DimensionInfoImpl();
                dimension.setEnabled(true);
                dimension.setPresentation(DimensionPresentation.LIST);
                ci.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX + domain, dimension);

                customDimension = true;
            }

            String elev = reader.getMetadataValue(GridCoverage2DReader.HAS_ELEVATION_DOMAIN);
            if (Boolean.parseBoolean(elev)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(ci.getName() + ": found ELEVATION dimension");
                }

                DimensionInfo dimension = new DimensionInfoImpl();
                dimension.setEnabled(true);
                dimension.setPresentation(DimensionPresentation.LIST);
                ci.getMetadata().put(ResourceInfo.ELEVATION, dimension);
            }

            String time = reader.getMetadataValue(GridCoverage2DReader.HAS_TIME_DOMAIN);
            if (Boolean.parseBoolean(time)) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(ci.getName() + ": found TIME dimension");
                }

                DimensionInfo dimension = new DimensionInfoImpl();
                dimension.setEnabled(true);
                dimension.setPresentation(DimensionPresentation.LIST);
                ci.getMetadata().put(ResourceInfo.TIME, dimension);

                timeDimension = true;
            }
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to access coverage reader custom dimensions", e);
            }
        }

        if (type == EoLayerType.BAND_COVERAGE) {
            return timeDimension && customDimension;
        } else {
            return timeDimension;
        }
    }

    static class EoCoverageSelection implements Serializable {
        String coverageName;

        EoLayerType type;

        public EoCoverageSelection(String coverageName, EoLayerType type) {
            super();
            this.coverageName = coverageName;
            this.type = type;
        }

        public String getCoverageName() {
            return coverageName;
        }

        public EoLayerType getType() {
            return type;
        }

        @Override
        public String toString() {
            return "EoCoverageSelection [coverageName=" + coverageName + ", type=" + type + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((coverageName == null) ? 0 : coverageName.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            EoCoverageSelection other = (EoCoverageSelection) obj;
            if (coverageName == null) {
                if (other.coverageName != null) return false;
            } else if (!coverageName.equals(other.coverageName)) return false;
            if (type != other.type) return false;
            return true;
        }
    }

    static class EoCoverageSelectionProvider extends GeoServerDataProvider<EoCoverageSelection> {

        List<EoCoverageSelection> selections;

        public EoCoverageSelectionProvider(List<EoCoverageSelection> selections) {
            super();
            this.selections = selections;
        }

        @Override
        protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<EoCoverageSelection>>
                getProperties() {
            List<GeoServerDataProvider.Property<EoCoverageSelection>> result =
                    new ArrayList<GeoServerDataProvider.Property<EoCoverageSelection>>();
            result.add(new BeanProperty("coverageName", "coverageName"));
            result.add(new BeanProperty("type", "type"));
            return result;
        }

        @Override
        protected List<EoCoverageSelection> getItems() {
            return selections;
        }
    }
}
