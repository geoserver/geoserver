/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.CollectionModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geoserver.web.wicket.SimpleChoiceRenderer;
import org.geotools.util.logging.Logging;

/** Configures {@link LayerInfo} WMS specific attributes */
public class WMSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    static final Logger LOGGER = Logging.getLogger(WMSLayerConfig.class);

    private static final long serialVersionUID = -2895136226805357532L;

    public WMSLayerConfig(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        add(new CheckBox("queryableEnabled", new PropertyModel<>(layerModel, "queryable")));
        add(new CheckBox("opaqueEnabled", new PropertyModel<>(layerModel, "opaque")));

        // styles block container
        WebMarkupContainer styleContainer = new WebMarkupContainer("styles");
        add(styleContainer);
        ResourceInfo resource = layerModel.getObject().getResource();
        styleContainer.setVisible(resource instanceof CoverageInfo || resource instanceof FeatureTypeInfo);

        // default style chooser. A default style is required
        StylesModel styles = new StylesModel();
        final PropertyModel<StyleInfo> defaultStyleModel = new PropertyModel<>(layerModel, "defaultStyle");
        final Select2DropDownChoice<StyleInfo> defaultStyle =
                new Select2DropDownChoice<>("defaultStyle", defaultStyleModel, styles, new StyleChoiceRenderer());
        defaultStyle.setRequired(true);
        styleContainer.add(defaultStyle);
        final Image defStyleImg = new NonCachingImage("defaultStyleLegendGraphic");
        defStyleImg.setOutputMarkupId(true);
        styleContainer.add(defStyleImg);

        final LegendGraphicAjaxUpdater defaultStyleUpdater =
                new LegendGraphicAjaxUpdater(defStyleImg, defaultStyleModel);

        defaultStyle.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = -4098934889965471248L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                defaultStyleUpdater.updateStyleImage(target);
            }
        });

        // build a palette with no reordering allowed, since order doesn't affect anything
        LiveCollectionModel<StyleInfo, Set<StyleInfo>> stylesModel =
                LiveCollectionModel.set(new PropertyModel<>(layerModel, "styles"));
        Palette<StyleInfo> extraStyles =
                new Palette<>("extraStyles", stylesModel, styles, new StyleNameRenderer(), 10, false) {
                    private static final long serialVersionUID = -3494299396410932090L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("ExtraStylesPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("ExtraStylesPalette.availableHeader"));
                    }
                };
        extraStyles.add(new DefaultTheme());
        styleContainer.add(extraStyles);

        TextField<Integer> renderingBuffer = new TextField<>(
                "renderingBuffer",
                new MapModel<>(new PropertyModel<>(layerModel, "metadata"), LayerInfo.BUFFER),
                Integer.class);
        renderingBuffer.add(RangeValidator.minimum(0));
        styleContainer.add(renderingBuffer);

        add(new TextField<>("wmsPath", new PropertyModel<>(layerModel, "path")));

        List<WMSInterpolation> interpolChoices = Arrays.asList(WMSInterpolation.values());

        PropertyModel<WMSInterpolation> defaultInterpolModel =
                new PropertyModel<>(layerModel, "defaultWMSInterpolationMethod");
        DropDownChoice<WMSInterpolation> interpolDropDown = new DropDownChoice<>(
                "defaultInterpolationMethod", defaultInterpolModel, interpolChoices, new InterpolationRenderer(this));
        interpolDropDown.setNullValid(true);
        add(interpolDropDown);

        initWMSCascadedUI(layerModel);
    }

    private class InterpolationRenderer extends ChoiceRenderer<WMSInterpolation> {

        private static final long serialVersionUID = 4230274692882585457L;

        private Component parent;

        public InterpolationRenderer(Component parent) {
            this.parent = parent;
        }

        @Override
        public Object getDisplayValue(WMSInterpolation interpolationMethod) {
            return new StringResourceModel(interpolationMethod.name(), parent).getString();
        }

        @Override
        public String getIdValue(WMSInterpolation object, int index) {
            return object.name();
        }
    }

    private void initWMSCascadedUI(IModel<LayerInfo> layerModel) {

        // styles block container
        WebMarkupContainer styleContainer = new WebMarkupContainer("remotestyles");
        // remote formats
        WebMarkupContainer remoteForamtsContainer = new WebMarkupContainer("remoteformats");
        WebMarkupContainer metaDataCheckBoxContainer = new WebMarkupContainer("metaDataCheckBoxContainer");
        WebMarkupContainer scaleDenominatorContainer = new WebMarkupContainer("scaleDenominatorContainer");
        WebMarkupContainer vendorParametersContainer = new WebMarkupContainer("vendorParametersContainer");

        add(styleContainer);
        add(remoteForamtsContainer);
        add(metaDataCheckBoxContainer);
        add(scaleDenominatorContainer);
        add(vendorParametersContainer);

        if (!(layerModel.getObject().getResource() instanceof WMSLayerInfo)) {
            styleContainer.setVisible(false);
            remoteForamtsContainer.setVisible(false);
            metaDataCheckBoxContainer.setVisible(false);
            scaleDenominatorContainer.setVisible(false);
            vendorParametersContainer.setVisible(false);
            return;
        }

        WMSLayerInfo wmsLayerInfo = (WMSLayerInfo) layerModel.getObject().getResource();
        // for new only
        try {
            if (layerModel.getObject().getId() == null) wmsLayerInfo.reset();
            else {
                // pull latest styles from remote WMS
                wmsLayerInfo.getAllAvailableRemoteStyles().clear();
                wmsLayerInfo.getAllAvailableRemoteStyles().addAll(wmsLayerInfo.getRemoteStyleInfos());
            }
        } catch (Exception e) {
            error("unable to fetch remote styles for " + wmsLayerInfo.getNativeName());
            LOGGER.log(
                    Level.SEVERE,
                    e.getMessage() + ":unable to fetch remote styles for " + wmsLayerInfo.getNativeName(),
                    e);
        }
        // empty string to use whatever default remote server has
        List<String> remoteSyles = new ArrayList<>();
        remoteSyles.add("");
        remoteSyles.addAll(getRemoteStyleNames(wmsLayerInfo.getAllAvailableRemoteStyles()));
        DropDownChoice<String> remotStyles = new DropDownChoice<>(
                "remoteStylesDropDown", new PropertyModel<>(wmsLayerInfo, "forcedRemoteStyle"), remoteSyles);

        styleContainer.add(remotStyles);

        LiveCollectionModel<String, Set<String>> stylesModel =
                LiveCollectionModel.set(new PropertyModel<>(wmsLayerInfo, "selectedRemoteStyles"));
        Palette<String> extraRemoteStyles = new Palette<>(
                "extraRemoteStyles",
                stylesModel,
                new CollectionModel<>(getRemoteStyleNames(wmsLayerInfo.getAllAvailableRemoteStyles())),
                new SimpleChoiceRenderer<>(),
                10,
                true);

        extraRemoteStyles.add(new DefaultTheme());
        styleContainer.add(extraRemoteStyles);

        DropDownChoice<String> remoteForamts = new DropDownChoice<>(
                "remoteFormatsDropDown",
                new PropertyModel<>(wmsLayerInfo, "preferredFormat"),
                wmsLayerInfo.availableFormats());

        remoteForamtsContainer.add(remoteForamts);
        // add format pallete

        LiveCollectionModel<String, Set<String>> remoteFormatsModel =
                LiveCollectionModel.set(new PropertyModel<>(wmsLayerInfo, "selectedRemoteFormats"));

        Palette<String> remoteFormatsPalette = new Palette<>(
                "remoteFormatsPalette",
                remoteFormatsModel,
                new CollectionModel<>(wmsLayerInfo.availableFormats()),
                new SimpleChoiceRenderer<>(),
                10,
                true);

        remoteFormatsPalette.add(new DefaultTheme());
        remoteForamtsContainer.add(remoteFormatsPalette);
        metaDataCheckBoxContainer.add(
                new CheckBox("respectMetadataBBoxChkBox", new PropertyModel<>(wmsLayerInfo, "metadataBBoxRespected")));
        // scale denominators
        TextField<Double> minScale =
                new TextField<>("minScale", new PropertyModel<>(wmsLayerInfo, "minScale"), Double.class);
        scaleDenominatorContainer.add(minScale);
        TextField<Double> maxScale =
                new TextField<>("maxScale", new PropertyModel<>(wmsLayerInfo, "maxScale"), Double.class);
        scaleDenominatorContainer.add(maxScale);

        minScale.add(new ScalesValidator(minScale, maxScale));

        TextArea<Object> vendorParameters =
                new TextArea<>("vendorParameters", new PropertyModel<>(wmsLayerInfo, "vendorParameters")) {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <C> IConverter<C> getConverter(Class<C> type) {
                        if (Map.class.isAssignableFrom(type)) {
                            // The cast is safe because we explicitly check that the type is assignable from Map.
                            return (IConverter<C>) new VendorParametersConvertor();
                        }
                        return super.getConverter(type);
                    }
                };
        vendorParameters.setConvertEmptyInputStringToNull(false);
        vendorParametersContainer.add(vendorParameters);
    }

    private Set<String> getRemoteStyleNames(final List<StyleInfo> styleInfoList) {
        return styleInfoList.stream().map(s -> s.getName()).collect(Collectors.toSet());
    }

    // validator to make sure min scale smaller than max scale and vice-versa
    private class ScalesValidator implements IValidator<Double> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1349568700386246273L;

        TextField<Double> minScale;
        TextField<Double> maxScale;

        public ScalesValidator(TextField<Double> minScale, TextField<Double> maxScale) {
            this.minScale = minScale;
            this.maxScale = maxScale;
        }

        private Double safeGet(String input, Double defaultValue) {
            if (input == null || input.isEmpty()) return defaultValue;
            else return Double.valueOf(input);
        }

        @Override
        public void validate(IValidatable validatable) {
            if (this.minScale.getInput() != null && this.maxScale.getInput() != null) {
                // negative check
                if (Double.valueOf(minScale.getInput()) < 0 || Double.valueOf(maxScale.getInput()) < 0) {
                    validatable.error(new ValidationError("Scale denominator cannot be Negative"));
                }
                // if both are set perform check min < max

                if (safeGet(minScale.getInput(), 0d) >= safeGet(maxScale.getInput(), Double.MAX_VALUE)) {
                    validatable.error(new ValidationError("Minimum Scale cannot be greater than Maximum Scale"));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    static class VendorParametersConvertor implements IConverter<Map<String, String>> {

        @Override
        public Map<String, String> convertToObject(String text, Locale locale) throws ConversionException {
            Properties properties = new Properties();
            if (text != null && !text.isEmpty()) {
                try (StringReader reader = new StringReader(text)) {
                    properties.load(reader); // Load properties from the string
                } catch (IOException e) {
                    throw new ConversionException(e);
                }
            }

            // No empty keys as they break the url protocol when mapped to url parameters
            return properties.entrySet().stream()
                    .filter(e -> e.getKey() != null)
                    .map(e -> Map.entry((String) e.getKey(), e.getValue() != null ? (String) e.getValue() : ""))
                    .filter(e -> !Strings.isEmpty(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @Override
        public String convertToString(Map<String, String> parameters, Locale locale) {
            if (parameters != null && !parameters.isEmpty()) {
                try (StringWriter writer = new StringWriter()) {
                    parameters.forEach((key, value) -> writer.write(key + "=" + value + "\n"));
                    return writer.toString();
                } catch (IOException e) {
                    throw new ConversionException(e);
                }
            }
            return "";
        }
    }
}
