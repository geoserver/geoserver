/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.LiveCollectionModel;

/** Configures {@link LayerInfo} WMS specific attributes */
public class WMSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = -2895136226805357532L;

    public WMSLayerConfig(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        add(new CheckBox("queryableEnabled", new PropertyModel<Boolean>(layerModel, "queryable")));
        add(new CheckBox("opaqueEnabled", new PropertyModel<Boolean>(layerModel, "opaque")));

        // styles block container
        WebMarkupContainer styleContainer = new WebMarkupContainer("styles");
        add(styleContainer);
        ResourceInfo resource = layerModel.getObject().getResource();
        styleContainer.setVisible(
                resource instanceof CoverageInfo || resource instanceof FeatureTypeInfo);

        // default style chooser. A default style is required
        StylesModel styles = new StylesModel();
        final PropertyModel<StyleInfo> defaultStyleModel =
                new PropertyModel<StyleInfo>(layerModel, "defaultStyle");
        final DropDownChoice<StyleInfo> defaultStyle =
                new DropDownChoice<StyleInfo>(
                        "defaultStyle", defaultStyleModel, styles, new StyleChoiceRenderer());
        defaultStyle.setRequired(true);
        styleContainer.add(defaultStyle);

        final Image defStyleImg = new NonCachingImage("defaultStyleLegendGraphic");
        defStyleImg.setOutputMarkupId(true);
        styleContainer.add(defStyleImg);

        // the wms url is build without qualification to allow usage of global styles,
        // the style name and layer name will be ws qualified instead
        String wmsURL = RequestCycle.get().getUrlRenderer().renderContextRelativeUrl("wms") + "?";

        final LegendGraphicAjaxUpdater defaultStyleUpdater;
        defaultStyleUpdater = new LegendGraphicAjaxUpdater(wmsURL, defStyleImg, defaultStyleModel);

        defaultStyle.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = -4098934889965471248L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        defaultStyleUpdater.updateStyleImage(target);
                    }
                });

        // build a palette with no reordering allowed, since order doesn't affect anything
        LiveCollectionModel stylesModel =
                LiveCollectionModel.set(new PropertyModel<Set<StyleInfo>>(layerModel, "styles"));
        Palette<StyleInfo> extraStyles =
                new Palette<StyleInfo>(
                        "extraStyles", stylesModel, styles, new StyleNameRenderer(), 10, false) {
                    private static final long serialVersionUID = -3494299396410932090L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("ExtraStylesPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("ExtraStylesPalette.availableHeader"));
                    }
                };
        extraStyles.add(new DefaultTheme());
        styleContainer.add(extraStyles);

        TextField<Integer> renderingBuffer =
                new TextField<Integer>(
                        "renderingBuffer",
                        new MapModel(new PropertyModel(layerModel, "metadata"), LayerInfo.BUFFER),
                        Integer.class);
        renderingBuffer.add(RangeValidator.minimum(0));
        styleContainer.add(renderingBuffer);

        add(new TextField<String>("wmsPath", new PropertyModel<String>(layerModel, "path")));

        List<WMSInterpolation> interpolChoices = Arrays.asList(WMSInterpolation.values());

        PropertyModel<WMSInterpolation> defaultInterpolModel =
                new PropertyModel<WMSInterpolation>(layerModel, "defaultWMSInterpolationMethod");
        DropDownChoice<WMSInterpolation> interpolDropDown =
                new DropDownChoice<WMSInterpolation>(
                        "defaultInterpolationMethod",
                        defaultInterpolModel,
                        interpolChoices,
                        new InterpolationRenderer(this));
        interpolDropDown.setNullValid(true);
        add(interpolDropDown);
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
}
