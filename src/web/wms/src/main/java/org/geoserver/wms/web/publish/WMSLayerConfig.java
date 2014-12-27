/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.LiveCollectionModel;

/**
 * Configures {@link LayerInfo} WMS specific attributes
 */
@SuppressWarnings("serial")
public class WMSLayerConfig extends LayerConfigurationPanel {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public WMSLayerConfig(String id, IModel layerModel) {
        super(id, layerModel);
        
        add(new CheckBox("queryableEnabled", new PropertyModel(layerModel,"queryable")));
        add(new CheckBox("opaqueEnabled", new PropertyModel(layerModel,"opaque")));
        
        // styles block container
        WebMarkupContainer styleContainer = new WebMarkupContainer("styles");
        add(styleContainer);
        ResourceInfo resource = ((LayerInfo) layerModel.getObject()).getResource();
        styleContainer.setVisible(resource instanceof CoverageInfo || resource instanceof FeatureTypeInfo); 

        // default style chooser. A default style is required
        StylesModel styles = new StylesModel();
        final PropertyModel defaultStyleModel = new PropertyModel(layerModel, "defaultStyle");
        final DropDownChoice defaultStyle = new DropDownChoice("defaultStyle", defaultStyleModel,
                styles, new StyleChoiceRenderer());
        defaultStyle.setRequired(true);
        styleContainer.add(defaultStyle);

        final Image defStyleImg = new Image("defaultStyleLegendGraphic");
        defStyleImg.setOutputMarkupId(true);
        styleContainer.add(defStyleImg);

        // the wms url is build without qualification to allow usage of global styles,
        // the style name and layer name will be ws qualified instead
        String wmsURL = getRequest().getRelativePathPrefixToContextRoot();
        wmsURL += wmsURL.endsWith("/") ? "wms?" : "/wms?";

        final LegendGraphicAjaxUpdater defaultStyleUpdater;
        defaultStyleUpdater = new LegendGraphicAjaxUpdater(wmsURL, defStyleImg, defaultStyleModel);

        defaultStyle.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                defaultStyleUpdater.updateStyleImage(target);
            }
        });

        // build a palette with no reordering allowed, since order doesn't affect anything
        IModel stylesModel = LiveCollectionModel.set(new PropertyModel(layerModel, "styles"));
        Palette extraStyles = new Palette("extraStyles", stylesModel, styles,
                new StyleNameRenderer(), 10, false) {
            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newSelectedHeader(final String componentId) {
                return new Label(componentId,
                        new ResourceModel("ExtraStylesPalette.selectedHeader"));
            }

            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newAvailableHeader(final String componentId) {
                return new Label(componentId, new ResourceModel(
                        "ExtraStylesPalette.availableHeader"));
            }
        };
        styleContainer.add(extraStyles);
        
        TextField renderingBuffer = new TextField("renderingBuffer", new MapModel(new PropertyModel(layerModel, "metadata"), LayerInfo.BUFFER), Integer.class);
        renderingBuffer.add(NumberValidator.minimum(0));
        styleContainer.add(renderingBuffer);
        
        add(new TextField("wmsPath", new PropertyModel(layerModel, "path")));

        // authority URLs and identifiers for this layer
        LayerAuthoritiesAndIdentifiersPanel authAndIds;
        authAndIds = new LayerAuthoritiesAndIdentifiersPanel("authoritiesAndIds", false, layerModel);
        add(authAndIds);
        
    }
}
