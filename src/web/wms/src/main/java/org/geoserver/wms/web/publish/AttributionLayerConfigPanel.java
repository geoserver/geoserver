/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import java.awt.image.BufferedImage;
import java.io.Serial;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
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
// TODO WICKET8 - Verify this page works OK
public class AttributionLayerConfigPanel extends PublishedConfigurationPanel<PublishedInfo> {

    @Serial
    private static final long serialVersionUID = -5229831547353122190L;

    public AttributionLayerConfigPanel(String id, IModel<? extends PublishedInfo> model) {
        super(id, model);

        PublishedInfo layer = model.getObject();

        if (layer.getAttribution() == null) {
            layer.setAttribution(
                    GeoServerApplication.get().getCatalog().getFactory().createAttribution());
        }

        add(new TextField<>("wms.attribution.title", new PropertyModel<>(model, "attribution.title")));

        final TextField<String> href =
                new TextField<>("wms.attribution.href", new PropertyModel<>(model, "attribution.href"));
        href.add(new UrlValidator());
        href.setOutputMarkupId(true);
        add(href);

        final TextField<String> logo =
                new TextField<>("wms.attribution.logo", new PropertyModel<>(model, "attribution.logoURL"));
        logo.add(new UrlValidator());
        logo.setOutputMarkupId(true);
        add(logo);

        final TextField<String> type =
                new TextField<>("wms.attribution.type", new PropertyModel<>(model, "attribution.logoType"));
        type.setOutputMarkupId(true);
        add(type);

        final TextField<Integer> height = new TextField<>(
                "wms.attribution.height", new PropertyModel<>(model, "attribution.logoHeight"), Integer.class);
        height.add(RangeValidator.minimum(0));
        height.setOutputMarkupId(true);
        add(height);

        final TextField<Integer> width = new TextField<>(
                "wms.attribution.width", new PropertyModel<>(model, "attribution.logoWidth"), Integer.class);
        width.add(RangeValidator.minimum(0));
        width.setOutputMarkupId(true);
        add(width);

        add(new AjaxSubmitLink("verifyImage") {
            @Serial
            private static final long serialVersionUID = 6814575194862084111L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
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
