/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.geoserver.gwc.GWC.tileLayerName;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.gwc.ConfigurableBlobStore;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.CatalogLayerEventListener;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.config.BlobStoreInfo;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.blobstore.memory.CacheProvider;

/**
 * Edit panel for a {@link GeoServerTileLayerInfo} (used to edit caching options for both {@link
 * LayerInfo} and {@link LayerGroupInfo}.
 *
 * <p>If a tile layer existed for the given layer or layer group and the "create a cached layer"
 * checkbox is unchecked, then the metadata properties defining the cached layers will be removed
 * from the layer/group {@link MetadataMap}. Once the save button is pressed on the enclosing page,
 * the {@link CatalogLayerEventListener} will caught the modification to the layer or layer group
 * and delete the cache for the layer.
 *
 * @author groldan
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

    /** the confirm removal of existing tile layer and its associated cache dialog */
    private final GeoServerDialog confirmRemovalDialog;

    /**
     * Whether to create a {@link TileLayer} for this {@link LayerInfo} or {@link LayerGroupInfo}
     */
    private final FormComponent<Boolean> createLayer;

    /** Whether the cached layer is enabled (like in {@link TileLayer#isEnabled()} */
    private final FormComponent<Boolean> enabled;

    /** The blobstoreId */
    private final DropDownChoice<String> blobStoreId;

    /** Container for {@link #configs} */
    private final WebMarkupContainer container;

    /** Container for everything but {@link #createLayer} */
    private final WebMarkupContainer configs;

    private final FormComponent<Integer> metaTilingX;

    private final FormComponent<Integer> metaTilingY;

    private final FormComponent<Integer> gutter;

    private final CheckGroup<String> cacheFormats;

    private final FormComponent<Integer> expireCache;

    private final FormComponent<Integer> expireClients;

    private final GridSubsetsEditor gridSubsets;
    private final ParameterFilterEditor parameterFilters;

    private final String originalLayerName;

    private IModel<? extends CatalogInfo> layerModel;

    private CheckBox enableInMemoryCaching;

    /** @param tileLayerModel must be a {@link GeoServerTileLayerInfoModel} */
    public GeoServerTileLayerEditor(
            final String id,
            final IModel<? extends PublishedInfo> layerModel,
            final IModel<GeoServerTileLayerInfo> tileLayerModel) {
        super(id);
        checkArgument(tileLayerModel instanceof GeoServerTileLayerInfoModel);
        this.layerModel = layerModel;
        setModel(tileLayerModel);

        final GWC mediator = GWC.get();
        final IModel<String> createTileLayerLabelModel;

        final PublishedInfo info = layerModel.getObject();
        final GeoServerTileLayerInfo tileLayerInfo = tileLayerModel.getObject();

        if (info instanceof LayerInfo) {
            createTileLayerLabelModel = new ResourceModel("createTileLayerForLayer");
            ResourceInfo resource = ((LayerInfo) info).getResource();
            // we need the _current_ name, regardless of it's name is being changed
            resource = ModificationProxy.unwrap(resource);
            originalLayerName = resource.prefixedName();
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

        // Get the model and check if the Enabled parameter has been defined
        GeoServerTileLayerInfoModel model = ((GeoServerTileLayerInfoModel) tileLayerModel);

        boolean undefined = model.getEnabled() == null;

        boolean doCreateTileLayer;
        if (tileLayerInfo.getId() != null) {
            doCreateTileLayer = true;
        } else if (isNew() && mediator.getConfig().isCacheLayersByDefault()) {
            doCreateTileLayer = true;
        } else {
            doCreateTileLayer = false;
        }
        // Add the enabled/disabled parameter depending on the doCreateTileLayer variable if not
        // already set
        if (undefined) {
            model.setEnabled(doCreateTileLayer);
        }
        add(createLayer = new CheckBox("createTileLayer", new Model<Boolean>(doCreateTileLayer)));
        createLayer.add(new AttributeModifier("title", new ResourceModel("createTileLayer.title")));

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        container.add(configs);

        add(enabled = new CheckBox("enabled", new PropertyModel<Boolean>(getModel(), "enabled")));
        enabled.add(new AttributeModifier("title", new ResourceModel("enabled.title")));
        configs.add(enabled);

        ChoiceRenderer<String> blobStoreRenderer =
                new ChoiceRenderer<String>() {
                    private static final long serialVersionUID = 1L;

                    final String defaultStore = getDefaultBlobStoreId();

                    @Override
                    public String getIdValue(String object, int index) {
                        return object;
                    }

                    @Override
                    public Object getDisplayValue(String object) {
                        String value = object;
                        if (object.equals(defaultStore)) {
                            value += " (*)";
                        }
                        return value;
                    }
                };
        PropertyModel<String> blobStoreModel = new PropertyModel<String>(getModel(), "blobStoreId");
        List<String> blobStoreChoices = getBlobStoreIds();
        configs.add(
                blobStoreId =
                        new DropDownChoice<String>(
                                "blobStoreId",
                                blobStoreModel,
                                blobStoreChoices,
                                blobStoreRenderer));
        blobStoreId.setNullValid(true);
        blobStoreId.add(new AttributeModifier("title", new ResourceModel("blobStoreId.title")));

        add(
                new IValidator<GeoServerTileLayerInfo>() {
                    private static final long serialVersionUID = 5240602030478856537L;

                    @Override
                    public void validate(IValidatable<GeoServerTileLayerInfo> validatable) {
                        final Boolean createVal = createLayer.getConvertedInput();
                        final Boolean enabledVal = enabled.getConvertedInput();
                        final String blobStoreIdVal = blobStoreId.getConvertedInput();

                        if (createVal && enabledVal && !isBlobStoreEnabled(blobStoreIdVal)) {
                            error(
                                    new ParamResourceModel(
                                                    "enabledError", GeoServerTileLayerEditor.this)
                                            .getString());
                        }
                    }
                });

        // CheckBox for enabling/disabling inner caching for the layer
        enableInMemoryCaching =
                new CheckBox(
                        "inMemoryCached", new PropertyModel<Boolean>(getModel(), "inMemoryCached"));
        ConfigurableBlobStore store = GeoServerExtensions.bean(ConfigurableBlobStore.class);
        if (store != null && store.getCache() != null) {
            enableInMemoryCaching.setEnabled(
                    mediator.getConfig().isInnerCachingEnabled()
                            && !store.getCache().isImmutable());
        }

        configs.add(enableInMemoryCaching);

        List<Integer> metaTilingChoices =
                Arrays.asList(
                        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
        IModel<Integer> metaTilingXModel = new PropertyModel<Integer>(getModel(), "metaTilingX");
        metaTilingX =
                new DropDownChoice<Integer>("metaTilingX", metaTilingXModel, metaTilingChoices);
        configs.add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<Integer>(getModel(), "metaTilingY");
        metaTilingY =
                new DropDownChoice<Integer>("metaTilingY", metaTilingYModel, metaTilingChoices);
        configs.add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<Integer>(getModel(), "gutter");
        List<Integer> gutterChoices =
                Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50, 100);
        gutter = new DropDownChoice<Integer>("gutter", gutterModel, gutterChoices);
        configs.add(gutter);

        IModel<Set<String>> mimeFormatsModel =
                new PropertyModel<Set<String>>(getModel(), "mimeFormats");

        cacheFormats = new CheckGroup<String>("cacheFormatsGroup", mimeFormatsModel);
        cacheFormats.setLabel(new ResourceModel("cacheFormats"));
        configs.add(cacheFormats);

        final List<String> formats;
        formats = Lists.newArrayList(GWC.getAdvertisedCachedFormats(info.getType()));
        mergeExisting(formats, mimeFormatsModel.getObject());

        ListView<String> cacheFormatsList =
                new ListView<String>("cacheFormats", formats) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new Check<String>("cacheFormatsOption", item.getModel()));
                        item.add(new Label("name", item.getModel()));
                    }
                };
        cacheFormatsList.setReuseItems(true); // otherwise it looses state on invalid form state
        // submits
        cacheFormats.add(cacheFormatsList);

        IModel<Integer> expireCacheModel = new PropertyModel<Integer>(getModel(), "expireCache");
        expireCache = new TextField<Integer>("expireCache", expireCacheModel);
        configs.add(expireCache);

        IModel<Integer> expireClientsModel =
                new PropertyModel<Integer>(getModel(), "expireClients");
        expireClients = new TextField<Integer>("expireClients", expireClientsModel);
        configs.add(expireClients);

        IModel<Set<XMLGridSubset>> gridSubsetsModel;
        gridSubsetsModel = new PropertyModel<Set<XMLGridSubset>>(getModel(), "gridSubsets");
        gridSubsets = new GridSubsetsEditor("cachedGridsets", gridSubsetsModel);
        configs.add(gridSubsets);

        IModel<Set<ParameterFilter>> parameterFilterModel;
        parameterFilterModel =
                new PropertyModel<Set<ParameterFilter>>(getModel(), "parameterFilters");
        parameterFilters =
                new ParameterFilterEditor("parameterFilters", parameterFilterModel, layerModel);
        configs.add(parameterFilters);

        // behavior phase
        configs.setVisible(createLayer.getModelObject());
        setValidating(createLayer.getModelObject());

        createLayer.add(
                new OnChangeAjaxBehavior() {
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

    private List<String> getBlobStoreIds() {
        List<String> blobStoreIds = new ArrayList<String>();
        for (BlobStoreInfo blobStore : GWC.get().getBlobStores()) {
            blobStoreIds.add(blobStore.getName());
        }
        return blobStoreIds;
    }

    private boolean isBlobStoreEnabled(String blobStoreId) {
        if (blobStoreId == null) {
            return true;
        }
        for (BlobStoreInfo blobStore : GWC.get().getBlobStores()) {
            if (blobStore.getName().equals(blobStoreId)) {
                return blobStore.isEnabled();
            }
        }
        return false;
    }

    private boolean isNew() {
        GeoServerTileLayerInfoModel model = (GeoServerTileLayerInfoModel) super.getModel();
        return model.isNew();
    }

    private String getDefaultBlobStoreId() {
        BlobStoreInfo defaultBlobStore = GWC.get().getDefaultBlobStore();
        return defaultBlobStore == null ? null : defaultBlobStore.getName();
    }

    public void save() {
        final GWC gwc = GWC.get();

        final CatalogInfo layer = layerModel.getObject();
        final GeoServerTileLayerInfo tileLayerInfo = getModelObject();
        final boolean tileLayerExists = gwc.hasTileLayer(layer);
        GeoServerTileLayerInfoModel model = (GeoServerTileLayerInfoModel) getModel();
        final boolean createLayer =
                model.getEnabled() == null
                        ? GWC.get().getConfig().isCacheLayersByDefault()
                        : model.getEnabled();

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

        // Remove the Layer from the cache if it is present
        ConfigurableBlobStore store = GeoServerExtensions.bean(ConfigurableBlobStore.class);
        if (store != null) {
            CacheProvider cache = store.getCache();
            if (cache != null) {
                if (enableInMemoryCaching.getModelObject()) {
                    cache.removeUncachedLayer(name);
                } else {
                    cache.addUncachedLayer(name);
                }
            }
        }

        if (tileLayerExists) {
            gwc.save(tileLayer);
        } else {
            gwc.add(tileLayer);
        }
    }

    private void updateConfigsVisibility(AjaxRequestTarget target) {
        final boolean createTileLayer = createLayer.getModelObject().booleanValue();
        setValidating(createTileLayer);
        configs.setVisible(createTileLayer);
        target.add(container);
    }

    private void confirmRemovalOfExistingTileLayer(final AjaxRequestTarget origTarget) {
        // show confirm cache removal dialog for this layer
        confirmRemovalDialog.setTitle(new Model<String>("Confirm removal of cached contents?"));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        confirmRemovalDialog.showOkCancel(
                origTarget,
                new GeoServerDialog.DialogDelegate() {
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
                        return new Label(
                                id,
                                new ParamResourceModel(
                                        "confirmTileLayerRemoval",
                                        GeoServerTileLayerEditor.this,
                                        usedQuotaStr));
                    }

                    @Override
                    protected boolean onSubmit(
                            final AjaxRequestTarget target, final Component contents) {
                        return true;
                    }

                    @Override
                    public void onClose(final AjaxRequestTarget target) {
                        target.add(createLayer);
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

    /** @see org.apache.wicket.markup.html.form.FormComponent#convertInput() */
    @Override
    public void convertInput() {
        createLayer.processInput();
        final boolean createTileLayer = createLayer.getModelObject().booleanValue();
        GeoServerTileLayerInfoModel model = ((GeoServerTileLayerInfoModel) getModel());
        model.setEnabled(createTileLayer);
        GeoServerTileLayerInfo tileLayerInfo = getModelObject();

        if (createTileLayer) {
            enabled.processInput();
            expireCache.processInput();
            expireClients.processInput();
            metaTilingX.processInput();
            metaTilingY.processInput();
            gutter.processInput();
            cacheFormats.processInput();
            parameterFilters.processInput();
            gridSubsets.processInput();

            //            // Remove add the Layer to the cache if it is present
            //            ConfigurableBlobStore store =
            // GeoServerExtensions.bean(ConfigurableBlobStore.class);
            //            if(store != null){
            //                CacheProvider cache = store.getCache();
            //                if (cache != null) {
            //                    if (enableInMemoryCaching.getModelObject()) {
            //                        cache.removeUncachedLayer(getModel().getObject().getName());
            //                    } else {
            //                        cache.addUncachedLayer(getModel().getObject().getName());
            //                    }
            //                }
            //            }

            tileLayerInfo.setId(layerModel.getObject().getId());
            setConvertedInput(tileLayerInfo);
        } else {
            tileLayerInfo.setId(null);
            setConvertedInput(tileLayerInfo);
        }
        setModelObject(tileLayerInfo);
    }

    /** @see org.apache.wicket.Component#onBeforeRender() */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }

    /** Merges the elements of existingFormats missing from formats into formats */
    private void mergeExisting(List<String> formats, Collection<String> existingFormats) {
        for (String x : existingFormats) {
            if (!formats.contains(x)) formats.add(x);
        }
    }
}
