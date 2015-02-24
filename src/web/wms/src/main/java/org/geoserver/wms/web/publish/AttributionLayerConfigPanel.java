/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.LayerConfigurationPanel;

/**
 * Configures a {@link LayerInfo} geo-search related metadata
 */
@SuppressWarnings("serial")
public class AttributionLayerConfigPanel extends LayerConfigurationPanel{
    public AttributionLayerConfigPanel(String id, IModel model){
        super(id, model);

        LayerInfo layer = (LayerInfo) model.getObject();

        if (layer.getAttribution() == null) {
            layer.setAttribution(
                GeoServerApplication.get().getCatalog().getFactory().createAttribution()
            );
        }

        AttributionInfo attr = layer.getAttribution();

        add(new TextField("wms.attribution.title", 
            new PropertyModel(model, "attribution.title")
        ));

        final TextField href = new TextField("wms.attribution.href", 
            new PropertyModel(model, "attribution.href")
        );
        href.add(new UrlValidator());
        href.setOutputMarkupId(true);
        add(href);

        final TextField logo = new TextField("wms.attribution.logo", 
            new PropertyModel(model, "attribution.logoURL")
        );
        logo.add(new UrlValidator());
        logo.setOutputMarkupId(true);
        add(logo);

        final TextField type = new TextField("wms.attribution.type",
            new PropertyModel(model, "attribution.logoType")
        );
        type.setOutputMarkupId(true);
        add(type);

        final TextField height = new TextField("wms.attribution.height", 
            new PropertyModel(model, "attribution.logoHeight"),
            Integer.class
        );
        height.add(NumberValidator.minimum(0));
        height.setOutputMarkupId(true);
        add(height);

        final TextField width = new TextField("wms.attribution.width",
            new PropertyModel(model, "attribution.logoWidth"),
            Integer.class
        );
        width.add(NumberValidator.minimum(0));
        width.setOutputMarkupId(true);
        add(width);

        add(new AjaxSubmitLink("verifyImage") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                if (logo.getDefaultModelObjectAsString() != null) {
                    try { 
                        URL url = new URL(logo.getDefaultModelObjectAsString());
                        URLConnection conn = url.openConnection();
                        type.getModel().setObject(conn.getContentType());
                        BufferedImage image = ImageIO.read(conn.getInputStream());
                        height.setModelValue("" + image.getHeight());
                        width.setModelValue("" + image.getWidth());
                    } catch (Exception e) {
                    }
                }

                target.addComponent(type);
                target.addComponent(height);
                target.addComponent(width);
            }
        });
    }
}
