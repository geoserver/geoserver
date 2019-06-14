/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.data.store.panel.FileModel;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.FileExistsValidator;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SRSListTextArea;
import org.geoserver.web.wicket.browser.GeoServerFileChooser;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.web.publish.LayerAuthoritiesAndIdentifiersPanel;

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
            new ArrayList<String>(Arrays.asList(WMS.DISPOSAL_METHODS));

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

    protected Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }

    @SuppressWarnings("unchecked")
    protected void build(IModel info, Form form) {
        // popups support
        form.add(modal = new ModalWindow("modal"));

        // new text field for the title of the root node
        form.add(new TextField<String>("rootLayerTitle"));
        form.add(new TextArea<String>("rootLayerAbstract"));

        PropertyModel metadataModel = new PropertyModel(info, "metadata");
        MapModel rootLayerEnabled =
                defaultedModel(
                        metadataModel,
                        WMS.ROOT_LAYER_IN_CAPABILITIES_KEY,
                        WMS.ROOT_LAYER_IN_CAPABILITIES_DEFAULT);
        CheckBox rootLayerEnabledField = new CheckBox("rootLayerEnabled", rootLayerEnabled);
        form.add(rootLayerEnabledField);

        // authority URLs and Identifiers for the root layer
        LayerAuthoritiesAndIdentifiersPanel authAndIds;
        authAndIds = new LayerAuthoritiesAndIdentifiersPanel("authoritiesAndIds", true, info);
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
        TextField<Integer> maxMemory = new TextField<Integer>("maxRequestMemory");
        maxMemory.add(RangeValidator.minimum(0));
        form.add(maxMemory);
        TextField<Integer> maxTime = new TextField<Integer>("maxRenderingTime");
        maxTime.add(RangeValidator.minimum(0));
        form.add(maxTime);
        TextField<Integer> maxErrors = new TextField<Integer>("maxRenderingErrors");
        maxErrors.add(RangeValidator.minimum(0));
        form.add(maxErrors);
        // max buffer
        TextField<Integer> maxBuffer = new TextField<Integer>("maxBuffer");
        maxBuffer.add(RangeValidator.minimum(0));
        form.add(maxBuffer);
        // max dimension values
        TextField<Integer> maxRequestedDimensionValues =
                new TextField<Integer>("maxRequestedDimensionValues");
        maxRequestedDimensionValues.add(RangeValidator.minimum(0));
        form.add(maxRequestedDimensionValues);
        // watermark
        form.add(new CheckBox("watermark.enabled"));
        TextField watermarkUrlField =
                new TextField(
                        "watermark.uRL",
                        new FileModel(new PropertyModel<String>(form.getModel(), "watermark.URL")));
        watermarkUrlField.add(new FileExistsValidator(true));
        watermarkUrlField.setOutputMarkupId(true);
        form.add(watermarkUrlField);
        form.add(
                chooserButton(
                        "chooser",
                        new ParamResourceModel("chooseWatermark", this).getString(),
                        watermarkUrlField));
        TextField<Integer> transparency = new TextField<Integer>("watermark.transparency");
        transparency.add(new RangeValidator<Integer>(0, 100));
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
                new TextField<Integer>("png.compression", pngCompression, Integer.class);
        pngCompressionField.add(new RangeValidator<Integer>(0, 100));
        form.add(pngCompressionField);
        // jpeg compression levels
        MapModel jpegCompression =
                defaultedModel(metadataModel, WMS.JPEG_COMPRESSION, WMS.JPEG_COMPRESSION_DEFAULT);
        TextField<Integer> jpegCompressionField =
                new TextField<Integer>("jpeg.compression", jpegCompression, Integer.class);
        jpegCompressionField.add(new RangeValidator<Integer>(0, 100));
        form.add(jpegCompressionField);
        // GIF animated
        // MAX_ALLOWED_FRAMES
        MapModel maxAllowedFrames =
                defaultedModel(
                        metadataModel, WMS.MAX_ALLOWED_FRAMES, WMS.MAX_ALLOWED_FRAMES_DEFAULT);
        TextField<Integer> maxAllowedFramesField =
                new TextField<Integer>("anim.maxallowedframes", maxAllowedFrames, Integer.class);
        maxAllowedFramesField.add(new RangeValidator<Integer>(0, Integer.MAX_VALUE));
        form.add(maxAllowedFramesField);
        // MAX_RENDERING_TIME
        MapModel maxRenderingTime = defaultedModel(metadataModel, WMS.MAX_RENDERING_TIME, null);
        TextField<Integer> maxRenderingTimeField =
                new TextField<Integer>("anim.maxrenderingtime", maxRenderingTime, Integer.class);
        form.add(maxRenderingTimeField);
        // MAX_RENDERING_SIZE
        MapModel maxRenderingSize = defaultedModel(metadataModel, WMS.MAX_RENDERING_SIZE, null);
        TextField<Integer> maxRenderingSizeField =
                new TextField<Integer>("anim.maxrenderingsize", maxRenderingSize, Integer.class);
        form.add(maxRenderingSizeField);
        // FRAMES_DELAY
        MapModel framesDelay =
                defaultedModel(metadataModel, WMS.FRAMES_DELAY, WMS.FRAMES_DELAY_DEFAULT);
        TextField<Integer> framesDelayField =
                new TextField<Integer>("anim.framesdelay", framesDelay, Integer.class);
        framesDelayField.add(new RangeValidator<Integer>(0, Integer.MAX_VALUE));
        form.add(framesDelayField);
        // DISPOSAL_METHOD
        MapModel disposalMethod =
                defaultedModel(metadataModel, WMS.DISPOSAL_METHOD, WMS.DISPOSAL_METHOD_DEFAULT);
        form.add(new DropDownChoice("anim.disposalmethod", disposalMethod, DISPOSAL_METHODS));
        // LOOP_CONTINUOUSLY
        MapModel loopContinuously =
                defaultedModel(metadataModel, WMS.LOOP_CONTINUOUSLY, WMS.LOOP_CONTINUOUSLY_DEFAULT);
        CheckBox loopContinuouslyField = new CheckBox("anim.loopcontinuously", loopContinuously);
        form.add(loopContinuouslyField);

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
        TextField<Integer> kmScoreField =
                new TextField<Integer>("kml.kmscore", kmScore, Integer.class);
        kmScoreField.add(new RangeValidator<Integer>(0, 100));
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
        getMapAvailable = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (GetMapOutputFormat format : GeoServerExtensions.extensions(GetMapOutputFormat.class)) {
            getMapAvailable.add(format.getMimeType());
        }

        List<String> getMapSelected = new ArrayList<String>();
        getMapSelected.addAll(new PropertyModel<Set<String>>(info, "getMapMimeTypes").getObject());
        List<String> getMapChoices = new ArrayList<String>();
        getMapChoices.addAll(getMapAvailable);

        form.add(
                getMapMimeTypesComponent =
                        new MimeTypesFormComponent(
                                "getMapMimeTypes",
                                new ListModel<String>(getMapSelected),
                                new CollectionModel<String>(getMapChoices),
                                new PropertyModel<Boolean>(info, "getMapMimeTypeCheckingEnabled")
                                        .getObject()));

        // mime types for GetFeatueInfo
        getFeatureInfoAvailable = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (GetFeatureInfoOutputFormat format :
                GeoServerExtensions.extensions(GetFeatureInfoOutputFormat.class)) {
            getFeatureInfoAvailable.add(format.getContentType());
        }

        List<String> getFeatureInfoSelected = new ArrayList<String>();
        getFeatureInfoSelected.addAll(
                new PropertyModel<Set<String>>(info, "getFeatureInfoMimeTypes").getObject());
        List<String> getFeatureInfoChoices = new ArrayList<String>();
        getFeatureInfoChoices.addAll(getFeatureInfoAvailable);

        form.add(
                getFeatureInfoMimeTypesComponent =
                        new MimeTypesFormComponent(
                                "getFeatureInfoMimeTypes",
                                new ListModel<String>(getFeatureInfoSelected),
                                new CollectionModel<String>(getFeatureInfoChoices),
                                new PropertyModel<Boolean>(
                                                info, "getFeatureInfoMimeTypeCheckingEnabled")
                                        .getObject()));

        // dynamicStylingDisabled
        form.add(
                new CheckBox(
                        "dynamicStyling.disabled",
                        new PropertyModel<Boolean>(info, WMS.DYNAMIC_STYLING_DISABLED)));

        // disable the reprojection of GetFeatureInfo results
        form.add(
                new CheckBox(
                        "disableFeaturesReproject",
                        new PropertyModel<>(info, WMS.FEATURES_REPROJECTION_DISABLED)));
        TextField<Integer> cacheMaxExtries =
                new TextField<Integer>("cacheConfiguration.maxEntries");
        cacheMaxExtries.add(RangeValidator.minimum(1));
        form.add(cacheMaxExtries);

        TextField<Long> cacheEntrySize = new TextField<Long>("cacheConfiguration.maxEntrySize");
        cacheEntrySize.add(new RangeValidator<Long>(1L, Long.MAX_VALUE));
        form.add(cacheEntrySize);

        form.add(new CheckBox("cacheConfiguration.enabled"));
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
                                new GeoServerFileChooser(modal.getContentId(), new Model(file)) {
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

    MapModel defaultedModel(IModel baseModel, String key, Object defaultValue) {
        MapModel model = new MapModel(baseModel, key);
        if (model.getObject() == null) model.setObject(defaultValue);
        return model;
    }

    protected String getServiceName() {
        return "WMS";
    }

    private class WatermarkPositionRenderer extends ChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(((Position) object).name(), WMSAdminPage.this, null)
                    .getString();
        }

        public String getIdValue(Object object, int index) {
            return ((Position) object).name();
        }
    }

    private class InterpolationRenderer extends ChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel(
                            ((WMSInterpolation) object).name(), WMSAdminPage.this, null)
                    .getString();
        }

        public String getIdValue(Object object, int index) {
            return ((WMSInterpolation) object).name();
        }
    }

    private class SVGMethodRenderer extends ChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return new StringResourceModel("svg." + object, WMSAdminPage.this, null).getString();
        }

        public String getIdValue(Object object, int index) {
            return (String) object;
        }
    }
}
