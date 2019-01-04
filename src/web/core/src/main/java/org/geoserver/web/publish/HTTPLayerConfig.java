/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

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

    private static final long serialVersionUID = -907171664833447962L;

    public HTTPLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);
        add(
                new CheckBox(
                        ResourceInfo.CACHING_ENABLED,
                        new MapModel<>(
                                new PropertyModel(model, "resource.metadata"), "cachingEnabled")));

        TextField<Long> maxAge =
                new TextField<Long>(
                        ResourceInfo.CACHE_AGE_MAX,
                        new MapModel(new PropertyModel(model, "resource.metadata"), "cacheAgeMax"),
                        Long.class);
        maxAge.add(RangeValidator.range(0l, Long.MAX_VALUE));
        add(maxAge);
    }
}
