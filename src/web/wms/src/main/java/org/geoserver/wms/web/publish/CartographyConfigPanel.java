/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
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
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geotools.util.logging.Logging;

/**
 * Configures {@link LayerInfo} WMS specific attributes.
 *
 * <ul>
 *   <li>defaultStyle
 *   <li>styles
 *   <li>opaqueEnabled
 *   <li>renderingBuffer
 *   <li>defaultWMSInterpolationMethod
 * </ul>
 */
public class CartographyConfigPanel extends PublishedConfigurationPanel<LayerInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(CartographyConfigPanel.class);

    @Override
    public void renderHead(org.apache.wicket.markup.head.IHeaderResponse response) {
        super.renderHead(response);
        // if the panel-specific CSS file contains actual css then have the browser load the css
        if (!isCssEmpty) {
            response.render(org.apache.wicket.markup.head.CssHeaderItem.forReference(
                    new org.apache.wicket.request.resource.PackageResourceReference(
                            getClass(), getClass().getSimpleName() + ".css")));
        }
    }

    static final Logger LOGGER = Logging.getLogger(CartographyConfigPanel.class);

    @Serial
    private static final long serialVersionUID = -2895136226805357532L;

    public CartographyConfigPanel(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);
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
            @Serial
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
                    @Serial
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

        add(new CheckBox("opaqueEnabled", new PropertyModel<>(layerModel, "opaque")));

        TextField<Integer> renderingBuffer = new TextField<>(
                "renderingBuffer",
                new MapModel<>(new PropertyModel<>(layerModel, "metadata"), LayerInfo.BUFFER),
                Integer.class);
        renderingBuffer.add(RangeValidator.minimum(0));
        styleContainer.add(renderingBuffer);
        add(renderingBuffer);

        List<WMSInterpolation> interpolChoices = Arrays.asList(WMSInterpolation.values());
        PropertyModel<WMSInterpolation> defaultInterpolModel =
                new PropertyModel<>(layerModel, "defaultWMSInterpolationMethod");
        DropDownChoice<WMSInterpolation> interpolDropDown = new DropDownChoice<>(
                "defaultInterpolationMethod", defaultInterpolModel, interpolChoices, new InterpolationRenderer(this));
        interpolDropDown.setNullValid(true);
        add(interpolDropDown);
    }

    private static class InterpolationRenderer extends ChoiceRenderer<WMSInterpolation> {

        @Serial
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
