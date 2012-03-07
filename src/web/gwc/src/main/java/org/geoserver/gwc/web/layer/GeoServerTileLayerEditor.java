/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.geoserver.gwc.GWC.tileLayerName;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.CatalogLayerEventListener;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.filter.parameters.FloatParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.RegexParameterFilter;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;

import com.google.common.base.Preconditions;

/**
 * Edit panel for a {@link GeoServerTileLayerInfo} (used to edit caching options for both
 * {@link LayerInfo} and {@link LayerGroupInfo}.
 * <p>
 * If a tile layer existed for the given layer or layer group and the "create a cached layer"
 * checkbox is unchecked, then the metadata properties defining the cached layers will be removed
 * from the layer/group {@link MetadataMap}. Once the save button is pressed on the enclosing page,
 * the {@link CatalogLayerEventListener} will caught the modification to the layer or layer group
 * and delete the cache for the layer.
 * </p>
 * 
 * @author groldan
 * 
 * @see GridSubsetsEditor
 * @see LayerCacheOptionsTabPanel
 * @see LayerGroupCacheOptionsPanel
 */
class GeoServerTileLayerEditor extends FormComponentPanel<GeoServerTileLayerInfo> {

    private static final long serialVersionUID = 7870938096047218989L;

    /**
     * Flag to indicate whether a cached layer initially existed for the given layer/group info so
     * that the cache for the layer is deleted at {@link #convertInput()}
     */
    private final boolean cachedLayerExistedInitially;

    /**
     * the confirm removal of existing tile layer and its associated cache dialog
     */
    private final GeoServerDialog confirmRemovalDialog;

    /**
     * Whether to create a {@link TileLayer} for this {@link LayerInfo} or {@link LayerGroupInfo}
     */
    private final FormComponent<Boolean> createLayer;

    /**
     * Whether the cached layer is enabled (like in {@link TileLayer#isEnabled()}
     */
    private final FormComponent<Boolean> enabled;

    /**
     * Container for {@link #configs}
     */
    private final WebMarkupContainer container;

    /**
     * Container for everything but {@link #createLayer}
     */
    private final WebMarkupContainer configs;

    private final FormComponent<Integer> metaTilingX;

    private final FormComponent<Integer> metaTilingY;

    private final FormComponent<Integer> gutter;

    private final CheckGroup<String> cacheFormats;

    private final FormComponent<Boolean> cacheExtraStyles;

    private final FormComponent<Boolean> createTimeParameterFilter;

    private final FormComponent<Boolean> createElevationParameterFilter;

    private final GridSubsetsEditor gridSubsets;

    private final String originalLayerName;

    private IModel<? extends CatalogInfo> layerModel;

    /**
     * @param id
     * @param layerModel
     * @param tileLayerModel
     *            must be a {@link GeoServerTileLayerInfoModel}
     */
    public GeoServerTileLayerEditor(final String id,
            final IModel<? extends CatalogInfo> layerModel,
            final IModel<GeoServerTileLayerInfo> tileLayerModel) {
        super(id);
        checkArgument(tileLayerModel instanceof GeoServerTileLayerInfoModel);
        this.layerModel = layerModel;
        setModel(tileLayerModel);

        final GWC mediator = GWC.get();
        final IModel<String> createTileLayerLabelModel;

        final CatalogInfo info = layerModel.getObject();
        final GeoServerTileLayerInfo tileLayerInfo = tileLayerModel.getObject();

        if (info instanceof LayerInfo) {
            createTileLayerLabelModel = new ResourceModel("createTileLayerForLayer");
            ResourceInfo resource = ((LayerInfo) info).getResource();
            // we need the _current_ name, regardless of it's name is being changed
            resource = ModificationProxy.unwrap(resource);
            originalLayerName = resource.getPrefixedName();
        } else if (info instanceof LayerGroupInfo) {
            createTileLayerLabelModel = new ResourceModel("createTileLayerForLayerGroup");
            // we need the _current_ name, regardless of if it's name is being changed
            LayerGroupInfo lgi = ModificationProxy.unwrap((LayerGroupInfo) info);
            originalLayerName = tileLayerName(lgi);
        } else {
            throw new IllegalArgumentException(
                    "Provided model does not target a LayerInfo nor a LayerGroupInfo: " + info);
        }

        TileLayer tileLayer = null;
        if (originalLayerName != null) {
            try {
                tileLayer = mediator.getTileLayerByName(originalLayerName);
            } catch (IllegalArgumentException notFound) {
                //
            }
        }
        cachedLayerExistedInitially = tileLayer != null;

        // UI construction phase
        add(confirmRemovalDialog = new GeoServerDialog("confirmRemovalDialog"));
        confirmRemovalDialog.setInitialWidth(360);
        confirmRemovalDialog.setInitialHeight(180);

        add(new Label("createTileLayerLabel", createTileLayerLabelModel));

        boolean doCreateTileLayer;
        if (tileLayerInfo.getId() != null) {
            doCreateTileLayer = true;
        } else if (isNew() && mediator.getConfig().isCacheLayersByDefault()) {
            doCreateTileLayer = true;
        } else {
            doCreateTileLayer = false;
        }
        add(createLayer = new CheckBox("createTileLayer", new Model<Boolean>(doCreateTileLayer)));
        createLayer.add(new AttributeModifier("title", true, new ResourceModel(
                "createTileLayer.title")));

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        container.add(configs);

        add(enabled = new CheckBox("enabled", new PropertyModel<Boolean>(getModel(), "enabled")));
        enabled.add(new AttributeModifier("title", true, new ResourceModel("enabled.title")));
        configs.add(enabled);

        List<Integer> metaTilingChoices = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IModel<Integer> metaTilingXModel = new PropertyModel<Integer>(getModel(), "metaTilingX");
        metaTilingX = new DropDownChoice<Integer>("metaTilingX", metaTilingXModel,
                metaTilingChoices);
        configs.add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<Integer>(getModel(), "metaTilingY");
        metaTilingY = new DropDownChoice<Integer>("metaTilingY", metaTilingYModel,
                metaTilingChoices);
        configs.add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<Integer>(getModel(), "gutter");
        List<Integer> gutterChoices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50);
        gutter = new DropDownChoice<Integer>("gutter", gutterModel, gutterChoices);
        configs.add(gutter);

        IModel<Set<String>> mimeFormatsModel = new PropertyModel<Set<String>>(getModel(),
                "mimeFormats");

        cacheFormats = new CheckGroup<String>("cacheFormatsGroup", mimeFormatsModel);
        cacheFormats.setLabel(new ResourceModel("cacheFormats"));
        configs.add(cacheFormats);

        final List<String> formats = Arrays.asList("image/png", "image/png8", "image/jpeg",
                "image/gif");

        ListView<String> cacheFormatsList = new ListView<String>("cacheFormats", formats) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Check<String>("cacheFormatsOption", item.getModel()));
                item.add(new Label("name", item.getModel()));
            }
        };
        cacheFormatsList.setReuseItems(true);// otherwise it looses state on invalid form state
                                             // submits
        cacheFormats.add(cacheFormatsList);

        cacheExtraStyles = cacheExtraStylesParameterFilter(info, getModel());
        createTimeParameterFilter = createTimeParameterFilter(info, getModel());
        createElevationParameterFilter = createElevationParameterFilter(info, getModel());

        IModel<Set<XMLGridSubset>> gridSubsetsModel;
        gridSubsetsModel = new PropertyModel<Set<XMLGridSubset>>(getModel(), "gridSubsets");
        gridSubsets = new GridSubsetsEditor("cachedGridsets", gridSubsetsModel);
        configs.add(gridSubsets);

        // behavior phase
        configs.setVisible(createLayer.getModelObject());
        setValidating(createLayer.getModelObject());

        createLayer.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                final boolean createTileLayer = createLayer.getModelObject().booleanValue();

                if (!createTileLayer && cachedLayerExistedInitially) {
                    confirmRemovalOfExistingTileLayer(target);
                } else {
                    updateConfigsVisibility(target);
                }

            }
        });
    }

    private boolean isNew() {
        GeoServerTileLayerInfoModel model = (GeoServerTileLayerInfoModel) super.getModel();
        return model.isNew();
    }

    public void save() {
        final GWC gwc = GWC.get();

        final CatalogInfo layer = layerModel.getObject();
        final GeoServerTileLayerInfo tileLayerInfo = getModelObject();
        final boolean tileLayerExists = gwc.hasTileLayer(layer);
        final boolean createLayer = this.createLayer.getModelObject().booleanValue();

        if (!createLayer) {
            if (tileLayerExists) {
                String tileLayerName = tileLayerInfo.getName();
                gwc.removeTileLayers(Arrays.asList(tileLayerName));
            }
            return;
        }

        // if we're creating a new layer, at this point the layer has already been created and hence
        // has an id
        Preconditions.checkState(layer.getId() != null);
        tileLayerInfo.setId(layer.getId());

        final String name;
        final GridSetBroker gridsets = gwc.getGridSetBroker();
        GeoServerTileLayer tileLayer;
        if (layer instanceof LayerGroupInfo) {
            LayerGroupInfo groupInfo = (LayerGroupInfo) layer;
            name = tileLayerName(groupInfo);
            tileLayer = new GeoServerTileLayer(groupInfo, gridsets, tileLayerInfo);
        } else {
            LayerInfo layerInfo = (LayerInfo) layer;
            name = tileLayerName(layerInfo);
            tileLayer = new GeoServerTileLayer(layerInfo, gridsets, tileLayerInfo);
        }

        tileLayerInfo.setName(name);

        if (tileLayerExists) {
            gwc.save(tileLayer);
        } else {
            gwc.add(tileLayer);
        }
    }

    private FormComponent<Boolean> createElevationParameterFilter(CatalogInfo info,
            IModel<GeoServerTileLayerInfo> model) {

        boolean hasElevationDimension = false;
        if (info instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) info;
            ResourceInfo resource = layerInfo.getResource();
            MetadataMap resourceMetadata = resource.getMetadata();
            DimensionInfo elevationDimension = resourceMetadata.get(ResourceInfo.ELEVATION,
                    DimensionInfo.class);
            if (elevationDimension != null && elevationDimension.isEnabled()) {
                hasElevationDimension = true;
            }
        }

        GeoServerTileLayerInfo layerInfo = model.getObject();
        Set<? extends ParameterFilter> parameterFilters = layerInfo.getParameterFilters();
        boolean hasElevationParameterFilter = false;
        if (hasElevationDimension) {
            for (ParameterFilter p : parameterFilters) {
                if ("ELEVATION".equalsIgnoreCase(p.getKey())) {
                    hasElevationParameterFilter = true;
                    break;
                }
            }
        }

        Label elevationParameterFilterLabel = new Label("elevationParameterFilterLabel",
                new ResourceModel("elevationParameterFilter"));
        configs.add(elevationParameterFilterLabel);

        IModel<Boolean> elevationParameterFilterModel = new Model<Boolean>(
                Boolean.valueOf(hasElevationParameterFilter));

        CheckBox elevationParameterFilter = new CheckBox("elevationParameterFilter",
                elevationParameterFilterModel);
        elevationParameterFilter.setEnabled(hasElevationDimension);
        if (!hasElevationDimension) {
            elevationParameterFilter.add(new AttributeModifier("title", true, new ResourceModel(
                    "elevationParameterFilterDisabled")));
        }
        configs.add(elevationParameterFilter);

        // only visible for LayerInfos, non sense for LayerGroups
        elevationParameterFilterLabel.setVisible(info instanceof LayerInfo);
        elevationParameterFilter.setVisible(info instanceof LayerInfo);

        return elevationParameterFilter;
    }

    private FormComponent<Boolean> createTimeParameterFilter(CatalogInfo info,
            IModel<GeoServerTileLayerInfo> model) {

        boolean hasTimeDimension = false;
        if (info instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) info;
            ResourceInfo resource = layerInfo.getResource();
            MetadataMap resourceMetadata = resource.getMetadata();
            DimensionInfo elevationDimension = resourceMetadata.get(ResourceInfo.TIME,
                    DimensionInfo.class);
            if (elevationDimension != null && elevationDimension.isEnabled()) {
                hasTimeDimension = true;
            }
        }

        GeoServerTileLayerInfo layerInfo = model.getObject();
        Set<? extends ParameterFilter> parameterFilters = layerInfo.getParameterFilters();
        boolean hasTimeParameterFilter = false;
        if (hasTimeDimension) {
            for (ParameterFilter p : parameterFilters) {
                if ("TIME".equalsIgnoreCase(p.getKey())) {
                    hasTimeParameterFilter = true;
                    break;
                }
            }
        }

        Label timeParameterFilterLabel = new Label("timeParameterFilterLabel", new ResourceModel(
                "timeParameterFilter"));
        configs.add(timeParameterFilterLabel);

        IModel<Boolean> timeParameterFilterModel = new Model<Boolean>(
                Boolean.valueOf(hasTimeParameterFilter));

        CheckBox timeParameterFilter = new CheckBox("timeParameterFilter", timeParameterFilterModel);
        timeParameterFilter.setEnabled(hasTimeDimension);
        if (!hasTimeDimension) {
            timeParameterFilter.add(new AttributeModifier("title", true, new ResourceModel(
                    "timeParameterFilterDisabled")));
        }

        configs.add(timeParameterFilter);

        // only visible for LayerInfos, non sense for LayerGroups
        timeParameterFilterLabel.setVisible(info instanceof LayerInfo);
        timeParameterFilter.setVisible(info instanceof LayerInfo);

        return timeParameterFilter;
    }

    private FormComponent<Boolean> cacheExtraStylesParameterFilter(CatalogInfo info,
            IModel<GeoServerTileLayerInfo> model) {

        Label cacheNonDefaultStylesLabel = new Label("cacheNonDefaultStylesLabel",
                new ResourceModel("cacheNonDefaultStyles"));
        configs.add(cacheNonDefaultStylesLabel);

        CheckBox cacheExtraStyles = new CheckBox("cacheNonDefaultStyles",
                new PropertyModel<Boolean>(model, "autoCacheStyles"));
        configs.add(cacheExtraStyles);

        // only visible for LayerInfos, non sense for LayerGroups
        cacheNonDefaultStylesLabel.setVisible(info instanceof LayerInfo);
        cacheExtraStyles.setVisible(info instanceof LayerInfo);

        return cacheExtraStyles;
    }

    private void updateConfigsVisibility(AjaxRequestTarget target) {
        final boolean createTileLayer = createLayer.getModelObject().booleanValue();
        setValidating(createTileLayer);
        configs.setVisible(createTileLayer);
        target.addComponent(container);
    }

    private void confirmRemovalOfExistingTileLayer(final AjaxRequestTarget origTarget) {
        // show confirm cache removal dialog for this layer
        confirmRemovalDialog.setTitle(new Model<String>("Confirm removal of cached contents?"));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        confirmRemovalDialog.showOkCancel(origTarget, new GeoServerDialog.DialogDelegate() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Component getContents(final String id) {
                // show a confirmation panel for all the objects we have to remove
                GWC gwc = GWC.get();
                Quota usedQuota = gwc.getUsedQuota(originalLayerName);
                if (usedQuota == null) {
                    usedQuota = new Quota();
                }
                String usedQuotaStr = usedQuota.toNiceString();
                return new Label(id, new ParamResourceModel("confirmTileLayerRemoval",
                        GeoServerTileLayerEditor.this, usedQuotaStr));
            }

            @Override
            protected boolean onSubmit(final AjaxRequestTarget target, final Component contents) {
                return true;
            }

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.addComponent(createLayer);
                updateConfigsVisibility(target);
            }

            @Override
            protected boolean onCancel(final AjaxRequestTarget target) {
                createLayer.setModelObject(Boolean.TRUE);
                final boolean closeWindow = true;
                return closeWindow;
            }
        });

    }

    private void setValidating(final boolean validate) {
        gridSubsets.setValidating(validate);
        cacheFormats.setRequired(validate);
    }

    /**
     * @see org.apache.wicket.markup.html.form.FormComponent#convertInput()
     */
    @Override
    protected void convertInput() {
        createLayer.processInput();
        final boolean createTileLayer = createLayer.getModelObject().booleanValue();

        GeoServerTileLayerInfo tileLayerInfo = getModelObject();

        if (createTileLayer) {
            enabled.processInput();
            metaTilingX.processInput();
            metaTilingY.processInput();
            gutter.processInput();
            cacheFormats.processInput();
            // process the cacheExtraStyles flag and let the CatalogLayerEventListener update the
            // allowed values when saved
            cacheExtraStyles.processInput();
            createTimeParameterFilter.processInput();
            createElevationParameterFilter.processInput();
            gridSubsets.processInput();

            tileLayerInfo.setId(layerModel.getObject().getId());
            updateAcceptAllRegExParameterFilter(tileLayerInfo, "TIME", createTimeParameterFilter
                    .getModelObject().booleanValue());

            updateAcceptAllFloatParameterFilter(tileLayerInfo, "ELEVATION",
                    createElevationParameterFilter.getModelObject().booleanValue());
            setConvertedInput(tileLayerInfo);
        } else {
            tileLayerInfo.setId(null);
            setConvertedInput(tileLayerInfo);
        }
        setModelObject(tileLayerInfo);
    }

    private void updateAcceptAllRegExParameterFilter(GeoServerTileLayerInfo tileLayerInfo,
            String paramKey, boolean createParam) {

        Set<ParameterFilter> parameterFilters = tileLayerInfo.getParameterFilters();
        for (Iterator<? extends ParameterFilter> it = parameterFilters.iterator(); it.hasNext();) {
            if (paramKey.equalsIgnoreCase(it.next().getKey())) {
                it.remove();
                break;
            }
        }
        if (createParam) {
            RegexParameterFilter filter = new RegexParameterFilter();
            filter.setKey(paramKey);
            filter.setDefaultValue("");
            filter.setRegex(".*");
            tileLayerInfo.getParameterFilters().add(filter);
        }
    }

    private void updateAcceptAllFloatParameterFilter(GeoServerTileLayerInfo tileLayerInfo,
            String paramKey, boolean createParam) {

        Set<ParameterFilter> parameterFilters = tileLayerInfo.getParameterFilters();
        for (Iterator<? extends ParameterFilter> it = parameterFilters.iterator(); it.hasNext();) {
            if (paramKey.equalsIgnoreCase(it.next().getKey())) {
                it.remove();
                break;
            }
        }
        if (createParam) {
            FloatParameterFilter filter = new FloatParameterFilter();
            filter.setKey(paramKey);
            filter.setDefaultValue("");
            tileLayerInfo.getParameterFilters().add(filter);
        }
    }

    /**
     * @see org.apache.wicket.Component#onBeforeRender()
     */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }

}
