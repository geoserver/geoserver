/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import static com.google.common.base.Preconditions.checkArgument;
import static org.geoserver.gwc.GWC.tileLayerName;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.coverage.CachingGridCoverageReaderCallback;
import org.geoserver.coverage.configuration.CoverageConfiguration;
import org.geoserver.coverage.layer.CoverageTileLayer;
import org.geoserver.coverage.layer.CoverageTileLayerInfo;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.InterpolationType;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.SeedingPolicy;
import org.geoserver.coverage.layer.CoverageTileLayerInfo.TiffCompression;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;

import com.google.common.base.Preconditions;

/**
 * 
 */
public class RasterCachingLayerEditor extends FormComponentPanel<GeoServerTileLayerInfo> {

    private static final long serialVersionUID = 7870938096047218989L;

    /**
     * Flag to indicate whether a cached layer initially existed for the given layer info so that the cache for the layer is deleted at
     * {@link #convertInput()}
     */
    private final boolean cachedLayerExistedInitially;

    /**
     * the confirm removal of existing tile layer and its associated cache dialog
     */
    private final GeoServerDialog confirmRemovalDialog;

    /**
     * Whether to create a {@link TileLayer} for this {@link LayerInfo} or {@link LayerGroupInfo}
     */
    private final FormComponent<Boolean> enabled;

    /**
     * Container for {@link #configs}
     */
    private final WebMarkupContainer container;

    /**
     * Container for everything but {@link #enabled}
     */
    private final WebMarkupContainer configs;

    private final FormComponent<Integer> metaTilingX;

    private final FormComponent<Integer> metaTilingY;

    private final FormComponent<Integer> gutter;

    private final GridSubsetsEditor gridSubsets;

    private final RasterParameterFilterEditor parameterFilters;

    private final String originalLayerId;
    private final String originalLayerName;

    private IModel<? extends CatalogInfo> layerModel;

    private final DropDownChoice<InterpolationType> interpolationPolicy;

    private final DropDownChoice<SeedingPolicy> seedingPolicy;

    private DropDownChoice<TiffCompression> tiffCompression;

    private DropDownChoice<OverviewPolicy> overviewPolicy;

    /**
     * @param id
     * @param layerModel
     * @param tileLayerModel must be a {@link GeoServerTileLayerInfoModel}
     */
    public RasterCachingLayerEditor(final String id,
            final IModel<? extends CatalogInfo> layerModel,
            final IModel<GeoServerTileLayerInfo> tileLayerModel) {
        super(id);
        checkArgument(tileLayerModel instanceof CoverageTileLayerInfoModel);
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
            originalLayerId = resource.getId();
            originalLayerName = resource.getPrefixedName();
            if (!(resource instanceof CoverageInfo)) {
                throw new IllegalArgumentException(
                        "Provided Layer does not target a CoverageInfo: " + resource);
            }
        } else {
            throw new IllegalArgumentException("Provided model does not target a LayerInfo: "
                    + info);
        }

        TileLayer tileLayer = null;
        if (originalLayerId != null) {
            try {

                Iterable<? extends TileLayer> layers = mediator
                        .getTileLayers();

                for (TileLayer layer : layers) {
                    if (layer instanceof CoverageTileLayer
                            && layer.getId().equalsIgnoreCase(originalLayerId)) {
                        tileLayer = (CoverageTileLayer) layer;
                        break;
                    }
                }
                // tileLayer = mediator.getTileLayerByName(originalLayerName);
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

        CoverageTileLayerInfoModel model = ((CoverageTileLayerInfoModel)tileLayerModel);
        
        boolean undefined = model.isEnabled() == null;
        
        boolean doCreateTileLayer;
        if (tileLayerInfo.getId() != null) {
            doCreateTileLayer = true;
        } else if (isNew()) {
            doCreateTileLayer = true;
        } else {
            doCreateTileLayer = false;
        }
        if(undefined){
            model.setEnabled(doCreateTileLayer);
        }
        add(enabled = new CheckBox("createTileLayer", new Model<Boolean>(doCreateTileLayer)));
        enabled.add(new AttributeModifier("title", true, new ResourceModel("createTileLayer.title")));

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        container.add(configs);

        List<Integer> metaTilingChoices = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13,
                14, 15, 16, 17, 18, 19, 20);
        IModel<Integer> metaTilingXModel = new PropertyModel<Integer>(getModel(), "metaTilingX");
        metaTilingX = new DropDownChoice<Integer>("metaTilingX", metaTilingXModel,
                metaTilingChoices);
        metaTilingX.add(new MetaTilingValidator("RasterCachingLayerEditor.metaTilingXError"));
        configs.add(metaTilingX);

        IModel<Integer> metaTilingYModel = new PropertyModel<Integer>(getModel(), "metaTilingY");
        metaTilingY = new DropDownChoice<Integer>("metaTilingY", metaTilingYModel,
                metaTilingChoices);
        metaTilingY.add(new MetaTilingValidator("RasterCachingLayerEditor.metaTilingYError"));
        configs.add(metaTilingY);

        IModel<Integer> gutterModel = new PropertyModel<Integer>(getModel(), "gutter");
        List<Integer> gutterChoices = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 50,
                100);
        gutter = new DropDownChoice<Integer>("gutter", gutterModel, gutterChoices);
        configs.add(gutter);
        
        IModel<SeedingPolicy> policyModel = new PropertyModel<SeedingPolicy>(getModel(), "seedingPolicy");
        seedingPolicy = new DropDownChoice<SeedingPolicy>("seedingPolicy", policyModel, Arrays.asList(SeedingPolicy.values()));
        configs.add(seedingPolicy);

        IModel<InterpolationType> interpolationModel = new PropertyModel<InterpolationType>(getModel(), "interpolationType");

        interpolationPolicy = new DropDownChoice<InterpolationType>("interpolationType",
                interpolationModel, Arrays.asList(InterpolationType.values()));
        configs.add(interpolationPolicy);

        IModel<OverviewPolicy> overviewModel = new PropertyModel<OverviewPolicy>(getModel(), "overviewPolicy");

        overviewPolicy = new DropDownChoice<OverviewPolicy>("overviewPolicy",
                overviewModel, Arrays.asList(OverviewPolicy.values()));
        configs.add(overviewPolicy);

        IModel<TiffCompression> compressionModel = new PropertyModel<TiffCompression>(getModel(), "tiffCompression");

        tiffCompression = new DropDownChoice<TiffCompression>("tiffCompression", compressionModel,
                Arrays.asList(TiffCompression.values()));
        configs.add(tiffCompression);
        
        IModel<Set<XMLGridSubset>> gridSubsetsModel;
        gridSubsetsModel = new PropertyModel<Set<XMLGridSubset>>(getModel(), "gridSubsets");
        gridSubsets = new GridSubsetsEditor("cachedGridsets", gridSubsetsModel);
        configs.add(gridSubsets);

        IModel<Set<ParameterFilter>> parameterFilterModel;
        parameterFilterModel = new PropertyModel<Set<ParameterFilter>>(getModel(),
                "parameterFilters");
        parameterFilters = new RasterParameterFilterEditor("parameterFilters",
                parameterFilterModel, layerModel);
        configs.add(parameterFilters);

        // Button for the seeding      
        final String name = originalLayerName + CoverageConfiguration.COVERAGE_LAYER_SUFFIX;//tileLayerModel.getObject().getName();
        final String href = "../gwc/rest/seed/" + name;

        // Seeding task
        final ExternalLink externalLink = new ExternalLink("seedLink", href, new ResourceModel(
                "RasterCachingLayerEditor.seed").getObject());
        externalLink.setEnabled(cachedLayerExistedInitially).setOutputMarkupId(true);
        configs.add(externalLink);
        
        Button test = new Button("button"){
            @Override
            public void onSubmit() {
              final boolean enableTileLayer = enabled.getModelObject().booleanValue();
              if (enableTileLayer) {
                  save();
                  externalLink.setEnabled(true);
              }
            }
        };
        configs.add(test);
        
        // behavior phase
        configs.setVisible(enabled.getModelObject());
        setValidating(enabled.getModelObject());

        enabled.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                final boolean enableTileLayer = enabled.getModelObject().booleanValue();
                // TODO CHANGE HERE
                if (!enableTileLayer && cachedLayerExistedInitially) {
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

        final CoverageTileLayerInfo tileLayerInfo = (CoverageTileLayerInfo) getModelObject();
        boolean tileLayerExists = false;// TODO CHANGE HERE

        Iterable<? extends TileLayer> layers = gwc.getTileLayers();

        for (TileLayer l : layers) {
            if (l.getId().equalsIgnoreCase(originalLayerId)) {
                tileLayerExists = true;
                break;
            }
        }

        CoverageTileLayerInfoModel model = (CoverageTileLayerInfoModel) getModel();
        final boolean enableTileLayer = model.isEnabled() == null ? true : model.isEnabled();//enabled.getModelObject().booleanValue();

        LayerInfo layerInfo = (LayerInfo) layer;
        CoverageInfo res = null;
        if(tileLayerExists){
            res = (CoverageInfo) layerInfo.getResource();
            gwc.getCatalog().getResourcePool().clear(res.getStore());
        }

        if (!enableTileLayer) {
            if (tileLayerExists) {

                String tileLayerName = tileLayerInfo.getName();
                gwc.removeTileLayers(Arrays.asList(tileLayerName));

                MetadataMap metadata = res.getMetadata();
                if (metadata != null
                        && metadata
                                .containsKey(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY)) {
                    // Avoid Proxy issues
                    res = gwc.getCatalog().getCoverage(res.getId());
                    res.getMetadata().remove(
                            CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY);
                    gwc.getCatalog().save(res);

                }
            }
            return;
        }

        // if we're creating a new layer, at this point the layer has already been created and hence
        // has an id
        Preconditions.checkState(layer.getId() != null);
        tileLayerInfo.setId(layer.getId());

        final String name;
        final GridSetBroker gridsetBroker = gwc.getGridSetBroker();

        name = tileLayerName(layerInfo);

        tileLayerInfo.setName(name);
        List<GridSubset> subSets = CoverageConfiguration.parseGridSubsets(gridsetBroker, tileLayerInfo);
        
        CoverageInfo info = gwc.getCatalog().getCoverageByName(layerInfo.getName());
        CoverageTileLayer coverageTileLayer = null;
        try {
            coverageTileLayer = new CoverageTileLayer(info, gridsetBroker, subSets, tileLayerInfo, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (tileLayerExists) {
            gwc.save(coverageTileLayer);
        } else {
            gwc.add(coverageTileLayer);
        }
        
        // Get the resource
        CoverageInfo coverage = gwc.getCatalog().getCoverageByName(((LayerInfo) layer).getName());

        // Add the WCSLayerInfo to the CoverageInfo Metadata Map
        if(coverage == null){
            return;
        }
        MetadataMap metadata = coverage.getMetadata();
        if(metadata != null){
            metadata.put(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY, tileLayerInfo);
        } else if(coverage instanceof CoverageInfoImpl){
            metadata = new MetadataMap();
            metadata.put(CachingGridCoverageReaderCallback.COVERAGETILELAYERINFO_KEY, tileLayerInfo);
            ((CoverageInfoImpl)coverage).setMetadata(metadata);
        }
        
        // Saving catalog
        gwc.getCatalog().save(coverage);
    }

    private void updateConfigsVisibility(AjaxRequestTarget target) {
        final boolean createTileLayer = enabled.getModelObject().booleanValue();
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
                        RasterCachingLayerEditor.this, usedQuotaStr));
            }

            @Override
            protected boolean onSubmit(final AjaxRequestTarget target, final Component contents) {
                return true;
            }

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.addComponent(enabled);
                updateConfigsVisibility(target);
            }

            @Override
            protected boolean onCancel(final AjaxRequestTarget target) {
                enabled.setModelObject(Boolean.TRUE);
                final boolean closeWindow = true;
                return closeWindow;
            }
        });

    }

    private void setValidating(final boolean validate) {
        gridSubsets.setValidating(validate);
    }

    /**
     * @see org.apache.wicket.markup.html.form.FormComponent#convertInput()
     */
    @Override
    protected void convertInput() {
        enabled.processInput();
        final boolean enableTileLayer = enabled.getModelObject().booleanValue();
        CoverageTileLayerInfoModel model = (CoverageTileLayerInfoModel) getModel();
        model.setEnabled(enableTileLayer);
        GeoServerTileLayerInfo tileLayerInfo = getModelObject();

        if (enableTileLayer) {
            metaTilingX.processInput();
            metaTilingY.processInput();
            gutter.processInput();
            parameterFilters.processInput();
            gridSubsets.processInput();
            interpolationPolicy.processInput();
            seedingPolicy.processInput();
            tiffCompression.processInput();
            overviewPolicy.processInput();

            tileLayerInfo.setId(layerModel.getObject().getId());// TODO CHANGE HERE
            setConvertedInput(tileLayerInfo);
        } else {
            tileLayerInfo.setId(null);
            setConvertedInput(tileLayerInfo);
        }
        setModelObject(tileLayerInfo);
    }

    /**
     * @see org.apache.wicket.Component#onBeforeRender()
     */
    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
    }
    
    static class MetaTilingValidator extends AbstractValidator<Integer> {
        
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;
        private final String name;

        public MetaTilingValidator(String name){
            this.name = name;
        }
        
        @Override
        public boolean validateOnNullValue() {
            return true;
        }

        @Override
        protected void onValidate(IValidatable<Integer> validatable) {
            if(validatable == null || validatable.getValue() == null){
                ValidationError error = new ValidationError();
                error.setMessage(new ParamResourceModel(name, null,
                "").getObject());
                validatable.error(error);
            }
        }
    }
}
