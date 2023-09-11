/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.resource.LocalesDropdown;
import org.geoserver.web.data.resource.TitleAndAbstractPanel;
import org.geoserver.web.data.store.panel.FileModel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.HTTPURLsListTextArea;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SRSListTextArea;
import org.geoserver.web.wicket.SimpleChoiceRenderer;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.map.MarkFactoryHintsInjector;
import org.geoserver.wms.web.publish.LayerAuthoritiesAndIdentifiersPanel;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;

/** Edits the WMS service details */
@SuppressWarnings("serial")
public class WMSAdminPage extends BaseServiceAdminPage<WMSInfo> {

    static final List<String> SVG_RENDERERS =
            Arrays.asList(new String[] {WMS.SVG_BATIK, WMS.SVG_SIMPLE});

    static final List<String> KML_REFLECTOR_MODES =
            Arrays.asList(
                    new String[] {
                        WMS.KML_REFLECTOR_MODE_REFRESH,
                        WMS.KML_REFLECTOR_MODE_SUPEROVERLAY,
                        WMS.KML_REFLECTOR_MODE_DOWNLOAD
                    });

    static final List<String> KML_SUPEROVERLAY_MODES =
            Arrays.asList(
                    new String[] {
                        WMS.KML_SUPEROVERLAY_MODE_AUTO,
                        WMS.KML_SUPEROVERLAY_MODE_RASTER,
                        WMS.KML_SUPEROVERLAY_MODE_OVERVIEW,
                        WMS.KML_SUPEROVERLAY_MODE_HYBRID,
                        WMS.KML_SUPEROVERLAY_MODE_CACHED
                    });

    static final List<String> DISPOSAL_METHODS =
            new ArrayList<>(Arrays.asList(WMS.DISPOSAL_METHODS));

    ModalWindow modal;
    MimeTypesFormComponent getMapMimeTypesComponent, getFeatureInfoMimeTypesComponent;
    TreeSet<String> getMapAvailable;
    TreeSet<String> getFeatureInfoAvailable;

    public WMSAdminPage() {
        super();
    }

    public WMSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public WMSAdminPage(WMSInfo service) {
        super(service);
    }

    @Override
    protected Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void build(IModel info, Form form) {

        // popups support
        form.add(modal = new ModalWindow("modal"));

        // new text field for the title of the root node
        form.add(
                new TitleAndAbstractPanel(
                        "rootLayerTitleAndAbstract",
                        info,
                        "rootLayerTitle",
                        "internationalRootLayerTitle",
                        "rootLayerAbstract",
                        "internationalRootLayerAbstract",
                        "rootLayerTitle",
                        "rootLayerAbstract",
                        this));
        PropertyModel<Map<String, ?>> metadataModel = new PropertyModel(info, "metadata");
        MapModel rootLayerEnabled =
                defaultedModel(
                        metadataModel,
                        WMS.ROOT_LAYER_IN_CAPABILITIES_KEY,
                        WMS.ROOT_LAYER_IN_CAPABILITIES_DEFAULT);
        CheckBox rootLayerEnabledField = new CheckBox("rootLayerEnabled", rootLayerEnabled);
        form.add(rootLayerEnabledField);

        // authority URLs and Identifiers for the root layer
        LayerAuthoritiesAndIdentifiersPanel authAndIds =
                new LayerAuthoritiesAndIdentifiersPanel("authoritiesAndIds", true, info);
        form.add(authAndIds);

        // limited srs list
        TextArea srsList =
                new SRSListTextArea(
                        "srs", LiveCollectionModel.list(new PropertyModel(info, "sRS")));
        form.add(srsList);

        form.add(new CheckBox("bBOXForEachCRS"));
        form.add(
                new AjaxLink("bBOXForEachCRSHelp") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        dialog.showInfo(
                                target,
                                new StringResourceModel(
                                        "bboxForEachCRSHelp.title", WMSAdminPage.this, null),
                                new StringResourceModel(
                                        "bboxForEachCRSHelp.message", WMSAdminPage.this, null));
                    }
                });
        // advanced projection handling
        MapModel aphEnabled =
                defaultedModel(
                        metadataModel, WMS.ADVANCED_PROJECTION_KEY, WMS.ENABLE_ADVANCED_PROJECTION);
        CheckBox aphEnabledField = new CheckBox("aph.enabled", aphEnabled);
        form.add(aphEnabledField);
        MapModel aphWrap =
                defaultedModel(metadataModel, WMS.MAP_WRAPPING_KEY, WMS.ENABLE_MAP_WRAPPING);
        CheckBox aphWrapField = new CheckBox("aph.wrap", aphWrap);
        form.add(aphWrapField);
        MapModel aphDensify =
                defaultedModel(
                        metadataModel,
                        WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY,
                        WMS.ENABLE_ADVANCED_PROJECTION_DENSIFICATION);
        CheckBox aphDensifyField = new CheckBox("aph.densify", aphDensify);
        form.add(aphDensifyField);
        MapModel aphHeuristic =
                defaultedModel(
                        metadataModel,
                        WMS.DATELINE_WRAPPING_HEURISTIC_KEY,
                        WMS.DISABLE_DATELINE_WRAPPING_HEURISTIC);
        CheckBox aphHeuristicField = new CheckBox("aph.dlh", aphHeuristic);
        form.add(aphHeuristicField);

        // general
        form.add(
                new DropDownChoice(
                        "interpolation",
                        Arrays.asList(WMSInfo.WMSInterpolation.values()),
                        new InterpolationRenderer()));
        // resource limits
        TextField<Integer> maxMemory = new TextField<>("maxRequestMemory");
        maxMemory.add(RangeValidator.minimum(0));
        form.add(maxMemory);
        TextField<Integer> maxTime = new TextField<>("maxRenderingTime");
        maxTime.add(RangeValidator.minimum(0));
        form.add(maxTime);
        TextField<Integer> maxErrors = new TextField<>("maxRenderingErrors");
        maxErrors.add(RangeValidator.minimum(0));
        form.add(maxErrors);
        // max buffer
        TextField<Integer> maxBuffer = new TextField<>("maxBuffer");
        maxBuffer.add(RangeValidator.minimum(0));
        form.add(maxBuffer);
        // max dimension values
        TextField<Integer> maxRequestedDimensionValues =
                new TextField<>("maxRequestedDimensionValues");
        maxRequestedDimensionValues.add(RangeValidator.minimum(0));
        form.add(maxRequestedDimensionValues);
        // watermark
        form.add(new CheckBox("watermark.enabled"));
        TextField watermarkUrlField =
                new TextField(
                        "watermark.uRL",
                        new FileModel(new PropertyModel<>(form.getModel(), "watermark.URL")));
        watermarkUrlField.add(new FileExistsValidator(true));
        watermarkUrlField.setOutputMarkupId(true);
        form.add(watermarkUrlField);
        form.add(
                chooserButton(
                        "chooser",
                        new ParamResourceModel("chooseWatermark", this).getString(),
                        watermarkUrlField));
        TextField<Integer> transparency = new TextField<>("watermark.transparency");
        transparency.add(new RangeValidator<>(0, 100));
        form.add(transparency);
        form.add(
                new DropDownChoice(
                        "watermark.position",
                        Arrays.asList(Position.values()),
                        new WatermarkPositionRenderer()));
        // svg
        form.add(new CheckBox("svg.antialias", new MapModel(metadataModel, "svgAntiAlias")));
        form.add(
                new DropDownChoice(
                        "svg.producer",
                        new MapModel(metadataModel, "svgRenderer"),
                        SVG_RENDERERS,
                        new SVGMethodRenderer()));
        // png compression levels
        MapModel pngCompression =
                defaultedModel(metadataModel, WMS.PNG_COMPRESSION, WMS.PNG_COMPRESSION_DEFAULT);
        TextField<Integer> pngCompressionField =
                new TextField<>("png.compression", pngCompression, Integer.class);
        pngCompressionField.add(new RangeValidator<>(0, 100));
        form.add(pngCompressionField);
        // jpeg compression levels
        MapModel jpegCompression =
                defaultedModel(metadataModel, WMS.JPEG_COMPRESSION, WMS.JPEG_COMPRESSION_DEFAULT);
        TextField<Integer> jpegCompressionField =
                new TextField<>("jpeg.compression", jpegCompression, Integer.class);
        jpegCompressionField.add(new RangeValidator<>(0, 100));
        form.add(jpegCompressionField);

        // kml handling
        MapModel kmlReflectorMode =
                defaultedModel(
                        metadataModel, WMS.KML_REFLECTOR_MODE, WMS.KML_REFLECTOR_MODE_DEFAULT);
        form.add(
                new DropDownChoice(
                        "kml.defaultReflectorMode", kmlReflectorMode, KML_REFLECTOR_MODES));

        MapModel kmlSuperoverlayMode =
                defaultedModel(
                        metadataModel,
                        WMS.KML_SUPEROVERLAY_MODE,
                        WMS.KML_SUPEROVERLAY_MODE_DEFAULT);
        form.add(
                new DropDownChoice(
                        "kml.superoverlayMode", kmlSuperoverlayMode, KML_SUPEROVERLAY_MODES));

        form.add(
                new CheckBox(
                        "kml.kmattr",
                        defaultedModel(metadataModel, WMS.KML_KMLATTR, WMS.KML_KMLATTR_DEFAULT)));
        form.add(
                new CheckBox(
                        "kml.kmlplacemark",
                        defaultedModel(
                                metadataModel,
                                WMS.KML_KMLPLACEMARK,
                                WMS.KML_KMLPLACEMARK_DEFAULT)));

        MapModel kmScore = defaultedModel(metadataModel, WMS.KML_KMSCORE, WMS.KML_KMSCORE_DEFAULT);
        TextField<Integer> kmScoreField = new TextField<>("kml.kmscore", kmScore, Integer.class);
        kmScoreField.add(new RangeValidator<>(0, 100));
        form.add(kmScoreField);

        // scalehint
        form.add(
                new CheckBox(
                        "scalehint.mapunitsPixel",
                        defaultedModel(
                                metadataModel,
                                WMS.SCALEHINT_MAPUNITS_PIXEL,
                                WMS.SCALEHINT_MAPUNITS_PIXEL_DEFAULT)));

        // mime types for GetMap
        getMapAvailable = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (GetMapOutputFormat format : GeoServerExtensions.extensions(GetMapOutputFormat.class)) {
            getMapAvailable.add(format.getMimeType());
        }

        List<String> getMapSelected = new ArrayList<>();
        getMapSelected.addAll(new PropertyModel<Set<String>>(info, "getMapMimeTypes").getObject());
        List<String> getMapChoices = new ArrayList<>();
        getMapChoices.addAll(getMapAvailable);

        form.add(
                getMapMimeTypesComponent =
                        new MimeTypesFormComponent(
                                "getMapMimeTypes",
                                new ListModel<>(getMapSelected),
                                new CollectionModel<>(getMapChoices),
                                new PropertyModel<Boolean>(info, "getMapMimeTypeCheckingEnabled")
                                        .getObject()));

        // mime types for GetFeatueInfo
        getFeatureInfoAvailable = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (GetFeatureInfoOutputFormat format :
                GeoServerExtensions.extensions(GetFeatureInfoOutputFormat.class)) {
            getFeatureInfoAvailable.add(format.getContentType());
        }

        List<String> getFeatureInfoSelected = new ArrayList<>();
        getFeatureInfoSelected.addAll(
                new PropertyModel<Set<String>>(info, "getFeatureInfoMimeTypes").getObject());
        List<String> getFeatureInfoChoices = new ArrayList<>();
        getFeatureInfoChoices.addAll(getFeatureInfoAvailable);

        form.add(
                getFeatureInfoMimeTypesComponent =
                        new MimeTypesFormComponent(
                                "getFeatureInfoMimeTypes",
                                new ListModel<>(getFeatureInfoSelected),
                                new CollectionModel<>(getFeatureInfoChoices),
                                new PropertyModel<Boolean>(
                                                info, "getFeatureInfoMimeTypeCheckingEnabled")
                                        .getObject()));

        // dynamicStylingDisabled
        form.add(
                new CheckBox(
                        "dynamicStyling.disabled",
                        new PropertyModel<>(info, WMS.DYNAMIC_STYLING_DISABLED)));

        // disable the reprojection of GetFeatureInfo results
        form.add(
                new CheckBox(
                        "disableFeaturesReproject",
                        new PropertyModel<>(info, WMS.FEATURES_REPROJECTION_DISABLED)));
        form.add(
                new CheckBox(
                        "disableTransformFeatureInfo",
                        new PropertyModel<>(info, "transformFeatureInfoDisabled")));
        form.add(
                new CheckBox(
                        "autoEscapeTemplateValues",
                        new PropertyModel<>(info, "autoEscapeTemplateValues")));
        TextField<Integer> cacheMaxExtries = new TextField<>("cacheConfiguration.maxEntries");
        cacheMaxExtries.add(RangeValidator.minimum(1));
        form.add(cacheMaxExtries);

        TextField<Long> cacheEntrySize = new TextField<>("cacheConfiguration.maxEntrySize");
        cacheEntrySize.add(new RangeValidator<>(1L, Long.MAX_VALUE));
        form.add(cacheEntrySize);

        form.add(new CheckBox("cacheConfiguration.enabled"));

        // Remote style time settings
        TextField<Integer> remoteStylesTimeout = new TextField<>("remoteStyleTimeout");
        remoteStylesTimeout.add(RangeValidator.minimum(1));
        form.add(remoteStylesTimeout);
        TextField<Integer> remoteStylesMaxRequestTime =
                new TextField<>("remoteStyleMaxRequestTime");
        remoteStylesMaxRequestTime.add(RangeValidator.minimum(1));
        form.add(remoteStylesMaxRequestTime);

        // limited srs list
        TextArea allowedRemoteSLDUrlsForAuthorizationForwarding =
                new HTTPURLsListTextArea(
                        "allowedURLsForAuthForwarding",
                        LiveCollectionModel.list(
                                new PropertyModel(info, "allowedURLsForAuthForwarding")));
        form.add(allowedRemoteSLDUrlsForAuthorizationForwarding);

        form.add(new CheckBox("defaultGroupStyleEnabled"));
        form.add(new LocalesDropdown("defaultLocale", new PropertyModel<>(info, "defaultLocale")));
        // add mark factory optimization
        addMarkFactoryLoadOptimizationPanel(metadataModel, form);
    }

    /** Adds the MarkFactory performance optimization panel. */
    private void addMarkFactoryLoadOptimizationPanel(
            PropertyModel<Map<String, ?>> metadataModel, Form<?> form) {
        checkAndInitializeMapData(metadataModel);
        final MapModel<String> mapMarkFactoryList =
                new MapModel<>(metadataModel, MarkFactoryHintsInjector.MARK_FACTORY_LIST);
        IModel<List<String>> markFactoryList = buildMarkFactoryListModel(mapMarkFactoryList);
        Collection<String> liveCollection = new ListModelCollection(markFactoryList);
        if (mapMarkFactoryList.getObject() == null) {
            mapMarkFactoryList.setObject("");
        }
        IModel<Collection<String>> collectionModel =
                new IModel<Collection<String>>() {

                    @Override
                    public void detach() {}

                    @Override
                    public void setObject(Collection<String> object) {
                        markFactoryList.setObject(new ArrayList<>(object));
                    }

                    @Override
                    public Collection<String> getObject() {
                        return liveCollection;
                    }
                };
        LiveCollectionModel<String, List<String>> markFactoriesLiveCollectionModel =
                LiveCollectionModel.list(collectionModel);
        Palette<String> factoriesSetupPallete =
                buildMarkFactoryPalleteComponent(markFactoriesLiveCollectionModel);
        factoriesSetupPallete.setOutputMarkupPlaceholderTag(true);
        factoriesSetupPallete.add(new DefaultTheme());
        form.add(factoriesSetupPallete);
        // add the boolean activator
        IModel<Boolean> enableModel = buildEnableModel(markFactoriesLiveCollectionModel);
        // add the label
        Label label =
                new Label(
                        "enableMarkFactoryLabel",
                        new ResourceModel("WMSAdminPage.markFactorySetup").getObject()) {
                    @Override
                    public boolean isVisible() {
                        return enableModel.getObject();
                    }
                };
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        form.add(label);

        AjaxCheckBox enableCheckBox =
                buildMarkFactoryEnableCheck(label, factoriesSetupPallete, enableModel);
        enableCheckBox.setOutputMarkupId(true);
        form.add(enableCheckBox);
    }

    private IModel<List<String>> buildMarkFactoryListModel(MapModel<String> mapMarkFactoryList) {
        return new IModel<List<String>>() {

            @Override
            public void detach() {}

            @Override
            public List<String> getObject() {
                String value = mapMarkFactoryList.getObject();
                if (StringUtils.isNotBlank(value)) {
                    return new ArrayList<>(Arrays.asList(value.split(",")));
                }
                return new ArrayList<>();
            }

            @Override
            public void setObject(List<String> object) {
                if (object == null) {
                    mapMarkFactoryList.setObject("");
                    return;
                }
                StringBuilder builder = new StringBuilder();
                boolean started = false;
                for (String value : object) {
                    if (started) builder.append(",");
                    builder.append(value);
                    started = true;
                }
                mapMarkFactoryList.setObject(builder.toString());
            }
        };
    }

    private AjaxCheckBox buildMarkFactoryEnableCheck(
            Label label, Palette<String> factoriesSetupPallete, IModel<Boolean> enableModel) {
        return new AjaxCheckBox("enableMarkFactory", enableModel) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (enableModel.getObject()) {
                    factoriesSetupPallete.setVisible(true);
                } else {
                    factoriesSetupPallete.setVisible(false);
                }
                target.add(factoriesSetupPallete);
                target.add(label);
            }
        };
    }

    private IModel<Boolean> buildEnableModel(
            LiveCollectionModel<String, List<String>> markFactoriesLiveCollectionModel) {
        return new IModel<Boolean>() {

            @Override
            public void detach() {}

            @Override
            public void setObject(Boolean object) {
                if (Boolean.TRUE.equals(object)) {
                    List<String> identifiers = new ArrayList<>(getMarkFactoryModelsIdentifiers());
                    markFactoriesLiveCollectionModel.setObject(identifiers);
                } else {
                    markFactoriesLiveCollectionModel.setObject(new ArrayList<>());
                }
            }

            @Override
            public Boolean getObject() {
                return CollectionUtils.isNotEmpty(markFactoriesLiveCollectionModel.getObject());
            }
        };
    }

    private Palette<String> buildMarkFactoryPalleteComponent(
            LiveCollectionModel<String, List<String>> markFactoriesLiveCollectionModel) {
        return new Palette<String>(
                "MarkFactoryPalette",
                markFactoriesLiveCollectionModel,
                new MarkFactoriesModel(),
                new SimpleChoiceRenderer<>(),
                10,
                true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onBeforeRender() {
                if (CollectionUtils.isEmpty(markFactoriesLiveCollectionModel.getObject())) {
                    this.setVisible(false);
                }
                super.onBeforeRender();
            }
            /** Override otherwise the header is not i18n'ized */
            @Override
            public Component newSelectedHeader(final String componentId) {
                return new Label(
                        componentId, new ResourceModel("MarkFactoryPalette.selectedHeader"));
            }

            /** Override otherwise the header is not i18n'ized */
            @Override
            public Component newAvailableHeader(final String componentId) {
                return new Label(
                        componentId, new ResourceModel("MarkFactoryPalette.availableHeader"));
            }
        };
    }

    private void checkAndInitializeMapData(PropertyModel<Map<String, ?>> metadataModel) {
        @SuppressWarnings("unchecked")
        PropertyModel<Map<String, Object>> mapModel = (PropertyModel) metadataModel;
        Object object = mapModel.getObject().get(MarkFactoryHintsInjector.MARK_FACTORY_LIST);
        if (!(object instanceof String)) {
            mapModel.getObject().put(MarkFactoryHintsInjector.MARK_FACTORY_LIST, "");
        }
    }

    private static class MarkFactoriesModel extends LoadableDetachableModel<List<String>> {
        @Override
        protected List<String> load() {
            return getMarkFactoryModelsIdentifiers();
        }
    }

    private static List<String> getMarkFactoryModelsIdentifiers() {
        return IteratorUtils.toList(DynamicSymbolFactoryFinder.getMarkFactories()).stream()
                .map(mf -> mf.getClass().getSimpleName())
                .collect(Collectors.toList());
    }

    @Override
    protected void handleSubmit(WMSInfo info) {

        info.setGetMapMimeTypeCheckingEnabled(getMapMimeTypesComponent.isMimeTypeCheckingEnabled());
        if (info.isGetMapMimeTypeCheckingEnabled())
            info.getGetMapMimeTypes()
                    .addAll(getMapMimeTypesComponent.getPalette().getModelCollection());
        else info.getGetMapMimeTypes().clear();

        info.setGetFeatureInfoMimeTypeCheckingEnabled(
                getFeatureInfoMimeTypesComponent.isMimeTypeCheckingEnabled());
        if (info.isGetFeatureInfoMimeTypeCheckingEnabled())
            info.getGetFeatureInfoMimeTypes()
                    .addAll(getFeatureInfoMimeTypesComponent.getPalette().getModelCollection());
        else info.getGetFeatureInfoMimeTypes().clear();

        super.handleSubmit(info);
    }

    protected Component chooserButton(
            String linkId, final String windowTitle, final TextField<String> textField) {
        AjaxSubmitLink link =
                new AjaxSubmitLink(linkId) {

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form form) {
                        File file = null;
                        textField.processInput();
                        String input = textField.getConvertedInput();
                        if (input != null && !input.equals("")) {
                            file = new File(input);
                        }

                        GeoServerFileChooser chooser =
                                new GeoServerFileChooser(modal.getContentId(), new Model<>(file)) {
                                    @Override
                                    protected void fileClicked(
                                            File file, AjaxRequestTarget target) {
                                        // clear the raw input of the field won't show the new model
                                        // value
                                        textField.clearInput();
                                        textField.setModelObject(file.getAbsolutePath());

                                        target.add(textField);
                                        dialog.close(target);
                                    };
                                };
                        chooser.setFileTableHeight(null);
                        modal.setContent(chooser);
                        modal.setTitle(windowTitle);
                        modal.show(target);
                    }
                };
        return link;
    }

    <T> MapModel<T> defaultedModel(IModel<Map<String, ?>> baseModel, String key, T defaultValue) {
        MapModel<T> model = new MapModel<>(baseModel, key);
        if (model.getObject() == null) model.setObject(defaultValue);
        return model;
    }

    @Override
    protected String getServiceName() {
        return "WMS";
    }

    private class WatermarkPositionRenderer extends ChoiceRenderer {

        @Override
        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((Position) object).name(), WMSAdminPage.this, null)
                    .getString();
        }

        @Override
        public String getIdValue(Object object, int index) {
            return ((Position) object).name();
        }
    }

    private class InterpolationRenderer extends ChoiceRenderer {

        @Override
        public Object getDisplayValue(Object object) {
            return new StringResourceModel(
                            ((WMSInterpolation) object).name(), WMSAdminPage.this, null)
                    .getString();
        }

        @Override
        public String getIdValue(Object object, int index) {
            return ((WMSInterpolation) object).name();
        }
    }

    private class SVGMethodRenderer extends ChoiceRenderer {

        @Override
        public Object getDisplayValue(Object object) {
            return new StringResourceModel("svg." + object, WMSAdminPage.this, null).getString();
        }

        @Override
        public String getIdValue(Object object, int index) {
            return (String) object;
        }
    }

    @Override
    protected boolean supportInternationalContent() {
        return true;
    }
}
