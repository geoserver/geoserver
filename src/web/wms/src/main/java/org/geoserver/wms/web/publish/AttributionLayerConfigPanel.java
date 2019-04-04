/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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
import org.apache.wicket.validation.validator.RangeValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.publish.PublishedConfigurationPanel;

/** Configures a {@link LayerInfo} geo-search related metadata */
public class AttributionLayerConfigPanel extends PublishedConfigurationPanel<PublishedInfo> {

    private static final long serialVersionUID = -5229831547353122190L;

    public AttributionLayerConfigPanel(String id, IModel<? extends PublishedInfo> model) {
        super(id, model);

        PublishedInfo layer = model.getObject();

        if (layer.getAttribution() == null) {
            layer.setAttribution(
                    GeoServerApplication.get().getCatalog().getFactory().createAttribution());
        }

        add(
                new TextField<String>(
                        "wms.attribution.title",
                        new PropertyModel<String>(model, "attribution.title")));

        final TextField<String> href =
                new TextField<String>(
                        "wms.attribution.href",
                        new PropertyModel<String>(model, "attribution.href"));
        href.add(new UrlValidator());
        href.setOutputMarkupId(true);
        add(href);

        final TextField<String> logo =
                new TextField<String>(
                        "wms.attribution.logo",
                        new PropertyModel<String>(model, "attribution.logoURL"));
        logo.add(new UrlValidator());
        logo.setOutputMarkupId(true);
        add(logo);

        final TextField<String> type =
                new TextField<String>(
                        "wms.attribution.type",
                        new PropertyModel<String>(model, "attribution.logoType"));
        type.setOutputMarkupId(true);
        add(type);

        final TextField<Integer> height =
                new TextField<Integer>(
                        "wms.attribution.height",
                        new PropertyModel<Integer>(model, "attribution.logoHeight"),
                        Integer.class);
        height.add(RangeValidator.minimum(0));
        height.setOutputMarkupId(true);
        add(height);

        final TextField<Integer> width =
                new TextField<Integer>(
                        "wms.attribution.width",
                        new PropertyModel<Integer>(model, "attribution.logoWidth"),
                        Integer.class);
        width.add(RangeValidator.minimum(0));
        width.setOutputMarkupId(true);
        add(width);

        add(
                new AjaxSubmitLink("verifyImage") {
                    private static final long serialVersionUID = 6814575194862084111L;

                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (logo.getDefaultModelObjectAsString() != null) {
                            try {
                                URL url = new URL(logo.getDefaultModelObjectAsString());
                                URLConnection conn = url.openConnection();
                                type.getModel().setObject(conn.getContentType());
                                BufferedImage image = ImageIO.read(conn.getInputStream());
                                height.setModelValue(new String[] {"" + image.getHeight()});
                                width.setModelValue(new String[] {"" + image.getWidth()});
                            } catch (Exception e) {
                            }
                        }

                        target.add(type);
                        target.add(height);
                        target.add(width);
                    }
                });
    }
}
