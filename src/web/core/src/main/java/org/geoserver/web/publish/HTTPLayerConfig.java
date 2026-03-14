/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import static org.geoserver.web.util.WebUtils.IsWicketCssFileEmpty;

import java.io.Serial;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.util.MapModel;

/** Configures the HTTP caching parameters */
public class HTTPLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final boolean isCssEmpty = IsWicketCssFileEmpty(HTTPLayerConfig.class);

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

    @Serial
    private static final long serialVersionUID = -907171664833447962L;

    public HTTPLayerConfig(String id, IModel<LayerInfo> model, String metadataPropertyName) {
        super(id, model);
        add(new CheckBox(
                ResourceInfo.CACHING_ENABLED,
                new MapModel<>(new PropertyModel<>(model, metadataPropertyName), "cachingEnabled")));

        TextField<Long> maxAge = new TextField<>(
                ResourceInfo.CACHE_AGE_MAX,
                new MapModel<>(new PropertyModel<>(model, metadataPropertyName), "cacheAgeMax"),
                Long.class);
        maxAge.add(RangeValidator.range(0l, Long.MAX_VALUE));
        add(maxAge);
    }
}
