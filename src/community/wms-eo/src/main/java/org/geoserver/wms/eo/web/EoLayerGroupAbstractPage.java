/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.layergroup.LayerGroupDetachableModel;
import org.geoserver.web.data.layergroup.LayerInfoConverter;
import org.geoserver.web.data.layergroup.RootLayerEntryPanel;
import org.geoserver.web.data.layergroup.StyleInfoConverter;
import org.geoserver.web.data.store.CoverageStoreNewPage;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.EnvelopePanel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.eo.EoCatalogBuilder;
import org.geoserver.wms.eo.EoLayerType;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** Handles layer group */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public abstract class EoLayerGroupAbstractPage extends GeoServerSecuredPage {

    public static final String GROUP = "group";
    IModel<LayerGroupInfo> lgModel;
    EnvelopePanel envelopePanel;
    EoLayerGroupEntryPanel lgEntryPanel;
    String layerGroupId;
    protected RootLayerEntryPanel rootLayerPanel;
    TextField<String> name;
    ModalWindow popupWindow;
    String groupName;
    private GeoServerDialog dialog;

    /** Subclasses must call this method to initialize the UI for this page */
    protected void initUI(LayerGroupInfo layerGroup) {
        this.returnPageClass = EoLayerGroupPage.class;
        lgModel = new LayerGroupDetachableModel(layerGroup);
        layerGroupId = layerGroup.getId();

        add(popupWindow = new ModalWindow("popup"));
        add(dialog = new GeoServerDialog("dialog"));

        Form form =
                new Form("form", new CompoundPropertyModel(lgModel)) {
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (LayerInfo.class.isAssignableFrom(type)) {
                            return (IConverter<C>) new LayerInfoConverter();
                        } else if (StyleInfo.class.isAssignableFrom(type)) {
                            return (IConverter<C>) new StyleInfoConverter();
                        } else {
                            return super.getConverter(type);
                        }
                    }
                };

        add(form);

        name = new TextField<String>("name");
        name.setRequired(true);
        groupName = layerGroup.getName();
        form.add(name);

        form.add(new TextField("title"));
        form.add(new TextArea("abstract"));

        final DropDownChoice<WorkspaceInfo> wsChoice =
                new DropDownChoice(
                        "workspace", new WorkspacesModel(), new WorkspaceChoiceRenderer());
        wsChoice.setNullValid(true);
        if (!isAuthenticatedAsAdmin()) {
            wsChoice.setNullValid(false);
            wsChoice.setRequired(true);
        }

        form.add(wsChoice);

        // bounding box
        form.add(envelopePanel = new EnvelopePanel("bounds") /*.setReadOnly(true)*/);
        envelopePanel.setRequired(true);
        envelopePanel.setCRSFieldVisible(true);
        envelopePanel.setCrsRequired(true);
        envelopePanel.setOutputMarkupId(true);

        form.add(
                new GeoServerAjaxFormLink("generateBounds") {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form form) {
                        // build a layer group with the current contents of the group
                        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
                        for (EoLayerGroupEntry entry : lgEntryPanel.getEntries()) {
                            lg.getLayers().add(entry.getLayer());
                            lg.getStyles().add(entry.getStyle());
                        }

                        try {
                            // grab the eventually manually inserted
                            CoordinateReferenceSystem crs =
                                    envelopePanel.getCoordinateReferenceSystem();

                            if (crs != null) {
                                // ensure the bounds calculated in terms of the user specified crs
                                new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg, crs);
                            } else {
                                // calculate from scratch
                                new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
                            }

                            envelopePanel.setModelObject(lg.getBounds());
                            target.add(envelopePanel);

                        } catch (Exception e) {
                            throw new WicketRuntimeException(e);
                        }
                    }
                });

        form.add(lgEntryPanel = new EoLayerGroupEntryPanel("layers", layerGroup, popupWindow));
        lgEntryPanel.setOutputMarkupId(true);

        EoLayerTypeRenderer eoLayerTypeRenderer = new EoLayerTypeRenderer();
        final DropDownChoice<EoLayerType> layerTypes =
                new DropDownChoice<EoLayerType>(
                        "layerType", EoLayerType.getRegularTypes(), eoLayerTypeRenderer);
        layerTypes.setModel(new Model<EoLayerType>(null));
        layerTypes.setOutputMarkupId(true);
        form.add(layerTypes);

        final GeoServerAjaxFormLink createStoreLink =
                new GeoServerAjaxFormLink("createStore") {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form form) {
                        final String layerGroupName = getNonNullGroupName(target);
                        if (layerGroupName != null) {
                            CoverageStoreNewPage coverageStoreCreator =
                                    new CoverageStoreNewPage(new ImageMosaicFormat().getName()) {
                                        protected void onSuccessfulSave(
                                                org.geoserver.catalog.CoverageStoreInfo info,
                                                org.geoserver.catalog.Catalog catalog,
                                                org.geoserver.catalog.CoverageStoreInfo
                                                        savedStore) {
                                            EoCoverageSelectorPage page =
                                                    new EoCoverageSelectorPage(
                                                            EoLayerGroupAbstractPage.this,
                                                            layerGroupName,
                                                            savedStore.getId());
                                            setResponsePage(page);
                                        };
                                    };
                            setResponsePage(coverageStoreCreator);
                        } else {
                            dialog.showInfo(
                                    target,
                                    null,
                                    new ParamResourceModel(
                                            "layerInfoTitle", EoLayerGroupAbstractPage.this),
                                    new ParamResourceModel(
                                            "provideGroupName", EoLayerGroupAbstractPage.this));
                        }
                    }
                };
        createStoreLink.setOutputMarkupId(true);
        form.add(createStoreLink);

        final GeoServerAjaxFormLink addFromStoreLink =
                new GeoServerAjaxFormLink("addFromStore") {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form form) {
                        final String layerGroupName = getNonNullGroupName(target);
                        if (layerGroupName != null) {
                            EoCoverageSelectorPage page =
                                    new EoCoverageSelectorPage(
                                            EoLayerGroupAbstractPage.this, layerGroupName);
                            setResponsePage(page);
                        } else {
                            dialog.showInfo(
                                    target,
                                    null,
                                    new ParamResourceModel(
                                            "layerInfoTitle", EoLayerGroupAbstractPage.this),
                                    new ParamResourceModel(
                                            "provideGroupName", EoLayerGroupAbstractPage.this));
                        }
                    }
                };
        addFromStoreLink.setOutputMarkupId(true);
        form.add(addFromStoreLink);

        final GeoServerAjaxFormLink addLayerLink =
                new GeoServerAjaxFormLink("addLayer") {
                    @Override
                    public void onClick(AjaxRequestTarget target, Form form) {
                        popupWindow.setInitialHeight(375);
                        popupWindow.setInitialWidth(525);
                        popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                        layerTypes.processInput();
                        final EoLayerType layerType = layerTypes.getModelObject();
                        popupWindow.setContent(
                                new EoLayerListPanel(
                                        popupWindow.getContentId(),
                                        layerType,
                                        lgEntryPanel.entryProvider) {
                                    @Override
                                    protected void handleLayer(
                                            LayerInfo layer, AjaxRequestTarget target) {
                                        popupWindow.close(target);

                                        layer.getMetadata().put(EoLayerType.KEY, layerType);
                                        lgEntryPanel
                                                .entryProvider
                                                .getItems()
                                                .add(
                                                        new EoLayerGroupEntry(
                                                                layer,
                                                                layer.getDefaultStyle(),
                                                                groupName));

                                        target.add(lgEntryPanel);
                                        layerTypes.setDefaultModelObject(
                                                layerTypes.getDefaultModelObject());
                                        target.add(layerTypes);
                                    }
                                });

                        popupWindow.show(target);
                    }
                };
        addLayerLink.setEnabled(false);
        form.add(addLayerLink);

        final DropDownChoice<EoLayerGroupEntry> outlinesEntryChooser =
                new DropDownChoice<EoLayerGroupEntry>(
                        "sourceLayer",
                        new OutlineSourceModel(lgEntryPanel.items),
                        new LayerGroupEntryRenderer());
        outlinesEntryChooser.setModel(new Model<EoLayerGroupEntry>(null));
        outlinesEntryChooser.setOutputMarkupId(true);
        outlinesEntryChooser.setEnabled(!outlinesPresent(lgEntryPanel.items));
        form.add(outlinesEntryChooser);

        outlinesEntryChooser.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        wsChoice.processInput();
                        WorkspaceInfo ws = (WorkspaceInfo) wsChoice.getDefaultModelObject();
                        outlinesEntryChooser.processInput();
                        EoLayerGroupEntry entry = outlinesEntryChooser.getModelObject();
                        try {
                            EoCatalogBuilder builder = new EoCatalogBuilder(getCatalog());
                            CoverageInfo coverage =
                                    (CoverageInfo) ((LayerInfo) entry.getLayer()).getResource();
                            CoverageStoreInfo store = coverage.getStore();
                            String url = store.getURL();
                            StructuredGridCoverage2DReader reader =
                                    (StructuredGridCoverage2DReader)
                                            coverage.getGridCoverageReader(null, null);
                            LayerInfo layer =
                                    builder.createEoOutlineLayer(
                                            url,
                                            ws,
                                            groupName,
                                            coverage.getNativeCoverageName(),
                                            reader);
                            lgEntryPanel.items.add(
                                    new EoLayerGroupEntry(
                                            layer, layer.getDefaultStyle(), groupName));
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to create outlines layer", e);
                            String layerName = entry.getLayer().prefixedName();
                            error(
                                    new ParamResourceModel(
                                                    "outlinesCreationError",
                                                    EoLayerGroupAbstractPage.this,
                                                    layerName,
                                                    e.getMessage())
                                            .getString());
                        } finally {
                            outlinesEntryChooser.setDefaultModelObject(null);
                        }
                        target.add(lgEntryPanel);
                        addFeedbackPanels(target);
                        target.add(outlinesEntryChooser);
                    }
                });

        layerTypes.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        layerTypes.processInput();
                        boolean input = layerTypes.getModelObject() != null;
                        addLayerLink.setEnabled(input);
                        target.add(addLayerLink);
                    }
                });

        name.add(
                new AjaxFormComponentUpdatingBehavior("blur") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        groupName = name.getInput();
                        boolean nameAvailable = groupName != null && !"".equals(groupName.trim());
                        if (!nameAvailable) {
                            info(
                                    new ParamResourceModel(
                                                    "provideGroupName",
                                                    EoLayerGroupAbstractPage.this)
                                            .getString());
                        } else {
                            // inform the user about possible layer renames
                            if (isLayerRenameRequired(groupName)) {
                                info(
                                        new ParamResourceModel(
                                                        "layerRenameWarning",
                                                        EoLayerGroupAbstractPage.this,
                                                        groupName)
                                                .getString());
                            }
                        }
                        target.add(createStoreLink);
                        target.add(addFromStoreLink);
                        addFeedbackPanels(target);
                    }
                });
        if (name.getDefaultModelObject() == null || "".equals(name.getDefaultModelObject())) {
            info(new ParamResourceModel("provideGroupName", this).getString());
        }

        form.add(saveLink());
        form.add(cancelLink());
    }

    /** True if we already have an outline layer, false otherwise */
    private boolean outlinesPresent(List<EoLayerGroupEntry> items) {
        for (EoLayerGroupEntry entry : items) {
            if (entry.getLayerType() == EoLayerType.COVERAGE_OUTLINE) {
                return true;
            }
        }

        return false;
    }

    private boolean isLayerRenameRequired(String layerGroupName) {
        String prefix = layerGroupName + "_";
        for (EoLayerGroupEntry entry : lgEntryPanel.entryProvider.getItems()) {
            String expectedName = prefix + entry.getLayerSubName();
            if (!expectedName.equals(entry.getLayer().getName())) {
                return true;
            }
        }

        return false;
    }

    protected String getNonNullGroupName(AjaxRequestTarget target) {
        if (groupName == null || "".equals(groupName.trim())) {
            error("Please given a name to the layer grup before adding layers into it");
            return null;
        }

        return groupName;
    }

    private Component cancelLink() {
        return new AjaxLink<String>("cancel") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                doReturn();
            }
        };
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                // validation
                if (lgEntryPanel.getEntries().size() == 0) {
                    error(
                            (String)
                                    new ParamResourceModel("oneLayerMinimum", getPage())
                                            .getObject());
                    return;
                }

                LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();

                // update the layer group entries
                lg.getLayers().clear();
                lg.getStyles().clear();
                for (EoLayerGroupEntry entry : lgEntryPanel.getEntries()) {
                    PublishedInfo pi = entry.getLayer();
                    if (pi instanceof LayerInfo) {
                        LayerInfo li = getCatalog().getLayer(pi.getId());
                        String expectedName = lg.getName() + "_" + entry.getLayerSubName();
                        String actualName = li.getName();
                        if (!expectedName.equals(actualName)) {
                            ResourceInfo resource = li.getResource();
                            li.setName(expectedName);
                            resource.setName(expectedName);
                            getCatalog().save(resource);
                            getCatalog().save(li);
                        }
                        lg.getLayers().add(li);
                        lg.getStyles().add(entry.getStyle());
                    }
                }

                try {
                    EoLayerGroupAbstractPage.this.save();
                } catch (Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Error adding/modifying layer group.", e);
                }
            }
        };
    }

    private final void save() {
        LayerGroupInfo lg = (LayerGroupInfo) lgModel.getObject();
        if (validateLayerGroupContents(lg)) {
            onSubmit(lg);
        }
    }

    private boolean validateLayerGroupContents(LayerGroupInfo lg) {
        boolean valid = true;
        List<EoLayerGroupEntry> items = lgEntryPanel.items;
        int browseCount = 0;
        LayerInfo browseLayer = null;
        StyleInfo browseLayerStyle = null;
        int bandsCount = 0;
        for (EoLayerGroupEntry entry : items) {

            if (!(entry.getLayer() instanceof LayerInfo)) {
                error(new ParamResourceModel("nestedLayerGroupInvalid", this));
            } else {
                EoLayerType type = entry.getLayerType();

                // count band and browse layers
                switch (type) {
                    case BAND_COVERAGE:
                        bandsCount++;
                        break;
                    case BROWSE_IMAGE:
                        browseCount++;
                        browseLayer = (LayerInfo) entry.getLayer();
                        browseLayerStyle = entry.getStyle();
                        break;
                    default:
                        break;
                }

                if (!checkDimensions(entry)) {
                    valid = false;
                }
            }
        }

        if (browseCount != 1) {
            error(new ParamResourceModel("invalidBrowseCount", this, browseCount).getString());
            valid = false;
        }

        if (bandsCount > 1) {
            error(new ParamResourceModel("invalidBandsCount", this, bandsCount).getString());
            valid = false;
        }

        if (valid) {
            // set the layer root
            lg.setRootLayer(browseLayer);
            lg.setRootLayerStyle(browseLayerStyle);
            lg.setMode(Mode.EO);
        }

        return valid;
    }

    private boolean checkDimensions(EoLayerGroupEntry entry) {
        LayerInfo layer = (LayerInfo) entry.getLayer();
        MetadataMap metadata = layer.getResource().getMetadata();
        DimensionInfo timeDimension = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
        boolean timeAvaiable = timeDimension != null && timeDimension.isEnabled();
        if (!timeAvaiable) {
            error(
                    new ParamResourceModel("EoLayerGroupError.invalidLayer", null, layer.getName())
                            .getString());
            return false;
        } else if (entry.getLayerType() != EoLayerType.BAND_COVERAGE) {
            return true;
        }

        // ok, has time, and it's a band layer. Does it have any extra dimension that can be
        // imagined to be used as a band selector?
        DimensionInfo elevationDimension =
                metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
        if (elevationDimension != null && elevationDimension.isEnabled()) {
            return true;
        }

        // look for custom dimensions
        for (String key : metadata.keySet()) {
            if (key != null && key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                DimensionInfo di = metadata.get(key, DimensionInfo.class);
                if (di != null && di.isEnabled()) {
                    return true;
                }
            }
        }

        // found nothing, the layer is not valid
        error(
                new ParamResourceModel(
                                "EoLayerGroupError.invalidBandCoverage", null, layer.getName())
                        .getString());
        return false;
    }

    /** Subclasses */
    protected abstract void onSubmit(LayerGroupInfo lg);

    class GroupNameValidator implements IValidator<String> {

        @Override
        public void validate(IValidatable<String> iv) {
            String name = (String) iv.getValue();
            LayerGroupInfo other = getCatalog().getLayerGroupByName(name);
            if (other != null && (layerGroupId == null || !other.getId().equals(layerGroupId))) {
                iv.error(new ValidationError("duplicateGroupNameError").setVariable("name", name));
            }
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
