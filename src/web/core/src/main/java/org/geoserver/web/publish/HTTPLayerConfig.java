/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.util.MapModel;

/**
 * Configures the HTTP caching parameters 
 */
@SuppressWarnings("serial")
public class HTTPLayerConfig extends LayerConfigurationPanel {
    public HTTPLayerConfig(String id, IModel model){
        super(id, model);
        add(new CheckBox(ResourceInfo.CACHING_ENABLED, new MapModel(new PropertyModel(model, "resource.metadata"), "cachingEnabled")));
        TextField maxAge = new TextField(ResourceInfo.CACHE_AGE_MAX, new MapModel(new PropertyModel(model, "resource.metadata"), "cacheAgeMax"), Long.class);
        maxAge.add(NumberValidator.range(0, Long.MAX_VALUE));
        add(maxAge);
    }
}

